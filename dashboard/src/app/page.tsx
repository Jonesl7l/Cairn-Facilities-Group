'use client'

import { useCallback, useEffect, useState } from 'react'
import { apiUrl, deleteIncident, fetchIncidents, fetchTechnicians, updateIncident } from '@/lib/api'
import type { Incident, IncidentStatus, Problem, Technician } from '@/lib/types'

const statuses: IncidentStatus[] = ['OPEN', 'ASSIGNED', 'IN_PROGRESS', 'RESOLVED', 'CLOSED']

// One page: a table of every incident with the operator actions that make the app react
export default function DashboardPage() {
  const [incidents, setIncidents] = useState<Incident[]>([])
  const [technicians, setTechnicians] = useState<Technician[]>([])
  const [error, setError] = useState<string | null>(null)
  const [isLive, setIsLive] = useState(false)

  // Reloads the table from the server
  const refresh = useCallback(async () => {
    setIncidents(await fetchIncidents())
  }, [])

  // 1. Load the technicians and the incidents once
  useEffect(() => {
    fetchTechnicians().then(setTechnicians)
    refresh()
  }, [refresh])

  // 2. Listen to the server's change hints, the same stream the app uses, and reload on each one
  useEffect(() => {
    const events = new EventSource(`${apiUrl}/events`)
    events.onopen = () => setIsLive(true)
    events.onerror = () => setIsLive(false)
    events.addEventListener('incidents-changed', refresh)
    return () => events.close()
  }, [refresh])

  // 3. Runs one write and shows the server's problem if it refuses, for example a 412 when the app changed it first
  async function run(write: Promise<Problem | null>) {
    const problem = await write
    setError(problem ? `${problem.status} ${problem.code}: ${problem.title}` : null)
    refresh()
  }

  // Resolving needs the two operator fields, asked for with plain prompts
  function resolve(incident: Incident) {
    const rootCause = window.prompt('Root cause', incident.rootCause ?? '')
    if (rootCause === null) return
    const correctiveActions = window.prompt('Corrective actions', incident.correctiveActions ?? '')
    if (correctiveActions === null) return
    run(updateIncident(incident, { status: 'RESOLVED', rootCause, correctiveActions }))
  }

  const nameOf = (id: string | null) => technicians.find((technician) => technician.id === id)?.displayName ?? id ?? ''

  return (
    <main>
      <header>
        <h1>Operator dashboard</h1>
        <span className="live">{isLive ? 'Live' : 'Reconnecting…'}</span>
        <button onClick={refresh}>Refresh</button>
      </header>
      {error && <p className="error">{error}</p>}
      <table>
        <thead>
          <tr>
            <th>Reference</th>
            <th>Title</th>
            <th>Reporter</th>
            <th>Severity</th>
            <th>Status</th>
            <th>Assigned to</th>
            <th>Version</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {incidents.map((incident) => {
            const isDeleted = incident.deletedAt !== null
            return (
              <tr key={incident.id} className={isDeleted ? 'deleted' : undefined}>
                <td>{incident.referenceNumber}</td>
                <td>{incident.title}</td>
                <td>{nameOf(incident.reporterId)}</td>
                <td>{incident.severity}</td>
                <td>
                  {/* Any status change is an operator write; moving past Assigned locks the report in the app */}
                  <select
                    value={incident.status}
                    disabled={isDeleted}
                    onChange={(event) => run(updateIncident(incident, { status: event.target.value as IncidentStatus }))}
                  >
                    {statuses.map((status) => (
                      <option key={status} value={status}>
                        {status}
                      </option>
                    ))}
                  </select>
                </td>
                <td>
                  <select
                    value={incident.assignedTo ?? ''}
                    disabled={isDeleted}
                    onChange={(event) => run(updateIncident(incident, { assignedTo: event.target.value || null }))}
                  >
                    <option value="">Unassigned</option>
                    {technicians.map((technician) => (
                      <option key={technician.id} value={technician.id}>
                        {technician.displayName}
                      </option>
                    ))}
                  </select>
                </td>
                <td>{incident.version}</td>
                <td>
                  <button disabled={isDeleted} onClick={() => resolve(incident)}>
                    Resolve
                  </button>{' '}
                  <button
                    disabled={isDeleted}
                    onClick={() => window.confirm(`Delete ${incident.referenceNumber}?`) && run(deleteIncident(incident))}
                  >
                    Delete
                  </button>
                </td>
              </tr>
            )
          })}
        </tbody>
      </table>
    </main>
  )
}
