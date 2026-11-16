// Custom server code. json-server loads this file with --middlewares and runs it
// after its own static files, CORS and body parsing, but before its router.
const fs = require('node:fs')
const path = require('node:path')
const express = require('express')
const multer = require('multer')

// Simulation settings from the environment, both off by default
const latencyMs = Number(process.env.LATENCY_MS ?? 0)
const failureRate = Number(process.env.FAILURE_RATE ?? 0)

// Photo uploads larger than this are refused with 413
const maxPhotoBytes = 10 * 1024 * 1024
// Uploaded photos live under public/, which json-server serves as static files
const publicDir = path.join(__dirname, 'public')
// A comment line this often keeps idle event streams open through proxies
const heartbeatMs = 20_000

// Each handler below either answers the request or calls next() to pass it on
const router = express.Router()

// Sends an RFC 9457 Problem Details error, the format the app's ProblemParser reads
function problem(res, status, code, title, extra = {}) {
  res.status(status).type('application/problem+json').json({ type: 'about:blank', title, status, code, ...extra })
}

// region every request
router.use(async (req, res, next) => {
  // 1. Slow every request down when LATENCY_MS is set
  if (latencyMs > 0) await new Promise((resolve) => setTimeout(resolve, latencyMs))
  // 2. Fail a share of requests when FAILURE_RATE is set, so the app shows retries
  if (Math.random() < failureRate) return problem(res, 503, 'SERVICE_UNAVAILABLE', 'Simulated outage')
  // 3. Tell the app which database it is talking to, so it resyncs after a reset
  res.set('X-Database-Id', req.app.db.get('meta.databaseId').value())
  // 4. Carry on to the routes below, or to json-server's router
  next()
})
// endregion

// region GET /reference
// Answers here instead of json-server: one call returns all reference data
router.get('/reference', (req, res) => {
  // req.app.db is json-server's lowdb database, read with lodash-style chains
  const db = req.app.db
  res.json({
    sites: db.get('sites').value(),
    buildings: db.get('buildings').value(),
    floors: db.get('floors').value(),
    rooms: db.get('rooms').value(),
    assets: db.get('assets').value(),
    // Technicians are the users with that role, with their passwords removed
    technicians: db.get('users').filter({ role: 'TECHNICIAN' }).map(({ password, ...user }) => user).value(),
  })
})
// endregion

// region GET /events
// Open event streams, one response object per connected app
const eventClients = new Set()

// Server-sent events: the response stays open and the server writes hints into it
router.get('/events', (req, res) => {
  // 1. Event stream headers; no-transform stops json-server's gzip from buffering the events
  res.set({
    'Content-Type': 'text/event-stream',
    'Cache-Control': 'no-cache, no-transform',
    Connection: 'keep-alive',
  })
  // 2. Send the headers now, which the app sees as "connected"
  res.flushHeaders()
  // 3. Remember this stream so writes can hint to it
  eventClients.add(res)
  // 4. Send a comment line now and then so the connection is not dropped as idle
  const heartbeat = setInterval(() => res.write(': heartbeat\n\n'), heartbeatMs)
  // 5. Forget the stream when the app disconnects
  req.on('close', () => {
    clearInterval(heartbeat)
    eventClients.delete(res)
  })
})

// Tells every connected app that incidents changed up to this changeSeq; the app then pulls
function broadcastChange(changeSeq) {
  for (const client of eventClients) client.write(`event: incidents-changed\nid: ${changeSeq}\ndata: ${changeSeq}\n\n`)
}

// Sends the hint once json-server's router has saved the write and answered with success
function hintAfterWrite(res, changeSeq) {
  res.on('finish', () => {
    if (res.statusCode < 300) broadcastChange(changeSeq)
  })
}
// endregion

// region incident rules
// Statuses in which the technician's app may still edit or delete an incident
const editableStatuses = ['OPEN', 'ASSIGNED']
const deletableStatuses = ['OPEN']
// Fields only an operator sets; the app never sends them
const operatorFields = ['status', 'assignedTo', 'dueDate', 'rootCause', 'correctiveActions', 'regulatoryReportable', 'supervisorAcknowledged']

// Looks an incident up by id, or returns undefined
const findIncident = (req, id) => req.app.db.get('incidents').find({ id }).value()

// Sends the 410 the app shows as "deleted remotely", with who deleted it and when
function deleted(res, incident) {
  const { deletedAt, deletedBy, referenceNumber } = incident
  problem(res, 410, 'INCIDENT_DELETED', 'Incident was deleted', { deletedAt, deletedBy, referenceNumber })
}

// Adds one to a counter in meta, saves it to db.json and returns the new value
function nextCounter(db, key) {
  const value = db.get(`meta.${key}`).value() + 1
  db.set(`meta.${key}`, value).write()
  return value
}
// endregion

// region GET /incidents/:id
router.get('/incidents/:id', (req, res, next) => {
  const incident = findIncident(req, req.params.id)
  // A deleted incident answers 410 instead of its tombstone
  if (incident?.deletedAt) return deleted(res, incident)
  // Otherwise json-server's router returns it, or its own 404
  next()
})
// endregion

// region POST /incidents
router.post('/incidents', (req, res, next) => {
  // 1. The app picks the id, so an id we already have means the app is retrying
  const existing = findIncident(req, req.body.id)
  // 2. A retry for an incident deleted since then is refused
  if (existing?.deletedAt) return deleted(res, existing)
  // 3. A retry for a live incident gets the stored copy back, not a duplicate
  if (existing) return res.status(200).json(existing)
  // 4. A new incident gets its server-owned fields stamped onto the body
  const db = req.app.db
  const now = new Date()
  const referenceCounter = nextCounter(db, 'referenceCounter')
  Object.assign(req.body, {
    referenceNumber: `INC-${now.getUTCFullYear()}-${String(referenceCounter).padStart(5, '0')}`,
    status: 'OPEN',
    createdAt: now.toISOString(),
    updatedAt: now.toISOString(),
    version: 1,
    changeSeq: nextCounter(db, 'changeSeq'),
    deletedAt: null,
    deletedBy: null,
  })
  // 5. Once saved, hint the new change to connected apps
  hintAfterWrite(res, req.body.changeSeq)
  // 6. json-server's router saves the stamped body and answers 201
  next()
})
// endregion

// region PATCH /incidents/:id
// Used for edits and for deletes (a body with deletedAt and deletedBy)
router.patch('/incidents/:id', (req, res, next) => {
  // 1. Every change must say which version it was based on
  const ifMatch = req.get('If-Match')
  if (!ifMatch) return problem(res, 428, 'PRECONDITION_REQUIRED', 'If-Match header is required')
  // 2. The incident must exist and not be deleted
  const incident = findIncident(req, req.params.id)
  if (!incident) return problem(res, 404, 'NOT_FOUND', 'Incident not found')
  if (incident.deletedAt) return deleted(res, incident)
  // 3. The version must match; If-Match arrives quoted ("4"), so strip the quotes
  if (Number(ifMatch.replaceAll('"', '')) !== incident.version) {
    // The current copy goes back so the app can merge without another request
    return problem(res, 412, 'VERSION_CONFLICT', 'Incident was changed by someone else', { current: incident })
  }
  // 4. Technician changes must suit the status; operator changes skip this check
  const isOperatorWrite = operatorFields.some((field) => field in req.body)
  const allowedStatuses = req.body.deletedAt ? deletableStatuses : editableStatuses
  if (!isOperatorWrite && !allowedStatuses.includes(incident.status)) {
    return problem(res, 409, 'STATUS_LOCKED', 'Incident status no longer allows this change', { currentStatus: incident.status })
  }
  // 5. All checks passed, so stamp the new version, change number and time
  Object.assign(req.body, {
    version: incident.version + 1,
    changeSeq: nextCounter(req.app.db, 'changeSeq'),
    updatedAt: new Date().toISOString(),
  })
  // 6. Once saved, hint the new change to connected apps
  hintAfterWrite(res, req.body.changeSeq)
  // 7. json-server's router merges the body into the stored incident and answers 200
  next()
})
// endregion

// region PUT /incidents/:id/photos/:photoId
// Reads one multipart file part named "file" into memory, refusing big or non-JPEG files
const upload = multer({
  storage: multer.memoryStorage(),
  limits: { fileSize: maxPhotoBytes },
  fileFilter: (req, file, accept) => {
    if (file.mimetype === 'image/jpeg') return accept(null, true)
    accept(Object.assign(new Error('Photo is not a JPEG'), { code: 'NOT_JPEG' }))
  },
})

// Ids become folder and file names, so only letters, digits, - and _ are allowed
const safeId = /^[\w-]+$/

router.put('/incidents/:id/photos/:photoId', (req, res) => {
  const { id, photoId } = req.params
  // 1. Refuse ids that could escape the uploads folder
  if (!safeId.test(id) || !safeId.test(photoId)) return problem(res, 422, 'VALIDATION_FAILED', 'Invalid incident or photo id')
  // 2. The app uploads photos before it creates a new incident, so a missing incident is fine
  const incident = findIncident(req, id)
  // 3. A deleted incident takes no photos
  if (incident?.deletedAt) return deleted(res, incident)
  // 4. Photos follow the edit rule: only while Open or Assigned
  if (incident && !editableStatuses.includes(incident.status)) {
    return problem(res, 409, 'STATUS_LOCKED', 'Incident status no longer allows photos', { currentStatus: incident.status })
  }
  // 5. Read the file part, turning multer's errors into Problem Details
  upload.single('file')(req, res, (error) => {
    if (error?.code === 'LIMIT_FILE_SIZE') return problem(res, 413, 'PAYLOAD_TOO_LARGE', 'Photo is too large')
    if (error?.code === 'NOT_JPEG') return problem(res, 415, 'UNSUPPORTED_MEDIA_TYPE', 'Photo must be a JPEG')
    if (error || !req.file) return problem(res, 422, 'VALIDATION_FAILED', 'A JPEG file part named file is required')
    // 6. Write the file where json-server serves it; the same photoId overwrites, so retries are safe
    const url = `uploads/incidents/${id}/${photoId}.jpg`
    const target = path.join(publicDir, url)
    const isReplacement = fs.existsSync(target)
    fs.mkdirSync(path.dirname(target), { recursive: true })
    fs.writeFileSync(target, req.file.buffer)
    // 7. 201 for a new photo, 200 for a replaced one, with the relative url the app stores
    res.status(isReplacement ? 200 : 201).json({ id: photoId, url })
  })
})
// endregion

module.exports = router
