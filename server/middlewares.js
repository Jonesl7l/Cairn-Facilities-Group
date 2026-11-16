// Custom server code. json-server loads this file with --middlewares and runs it
// after its own static files, CORS and body parsing, but before its router.
const express = require('express')

// Simulation settings from the environment, both off by default
const latencyMs = Number(process.env.LATENCY_MS ?? 0)
const failureRate = Number(process.env.FAILURE_RATE ?? 0)

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
  // 5. json-server's router saves the stamped body and answers 201
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
  // 6. json-server's router merges the body into the stored incident and answers 200
  next()
})
// endregion

module.exports = router
