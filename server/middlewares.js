const express = require('express')

const latencyMs = Number(process.env.LATENCY_MS ?? 0)
const failureRate = Number(process.env.FAILURE_RATE ?? 0)

const router = express.Router()

function problem(res, status, code, title, extra = {}) {
  res.status(status).type('application/problem+json').json({ type: 'about:blank', title, status, code, ...extra })
}

router.use(async (req, res, next) => {
  if (latencyMs > 0) await new Promise((resolve) => setTimeout(resolve, latencyMs))
  if (Math.random() < failureRate) return problem(res, 503, 'SERVICE_UNAVAILABLE', 'Simulated outage')
  res.set('X-Database-Id', req.app.db.get('meta.databaseId').value())
  next()
})

router.get('/reference', (req, res) => {
  const db = req.app.db
  res.json({
    sites: db.get('sites').value(),
    buildings: db.get('buildings').value(),
    floors: db.get('floors').value(),
    rooms: db.get('rooms').value(),
    assets: db.get('assets').value(),
    technicians: db.get('users').filter({ role: 'TECHNICIAN' }).map(({ password, ...user }) => user).value(),
  })
})

module.exports = router
