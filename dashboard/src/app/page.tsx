'use client'

import { useCallback, useEffect, useMemo, useState } from 'react'
import { apiUrl, deleteIncident, fetchIncidents, fetchReference, updateIncident } from '@/lib/api'
import type { Incident, IncidentStatus, Problem, Reference } from '@/lib/types'

const statuses: IncidentStatus[] = ['OPEN', 'ASSIGNED', 'IN_PROGRESS', 'RESOLVED', 'CLOSED']
const severities = ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW']

// Needs triage is the operator's default: open reports nobody has picked up yet
type Tab = 'TRIAGE' | 'ALL' | IncidentStatus
const tabs: { tab: Tab; label: string }[] = [
  { tab: 'TRIAGE', label: 'Needs triage' },
  { tab: 'ALL', label: 'All' },
  ...statuses.map((status) => ({ tab: status, label: status.replace('_', ' ') })),
]

// Higher is more severe, used to sort the triage queue
const severityRank = (severity: string) => severities.length - severities.indexOf(severity)

// Whether an incident belongs in a tab; deleted reports only show under All
function isInTab(incident: Incident, tab: Tab): boolean {
  if (tab === 'ALL') return true
  if (incident.deletedAt !== null) return false
  if (tab === 'TRIAGE') return incident.status === 'OPEN' && !incident.assignedTo
  return incident.status === tab
}

// One page: triage, filter, manage and resolve every incident, the operator actions that make the app react
export default function DashboardPage() {
  const [incidents, setIncidents] = useState<Incident[]>([])
  const [reference, setReference] = useState<Reference>({ sites: [], technicians: [] })
  const [error, setError] = useState<string | null>(null)
  const [isLive, setIsLive] = useState(false)
  const [tab, setTab] = useState<Tab>('TRIAGE')
  const [severity, setSeverity] = useState('')
  const [assignee, setAssignee] = useState('')
  const [site, setSite] = useState('')
  const [search, setSearch] = useState('')

  // Reloads the table from the server
  const refresh = useCallback(async () => {
    setIncidents(await fetchIncidents())
  }, [])

  // 1. Load the reference data and the incidents once
  useEffect(() => {
    fetchReference().then(setReference)
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

  // 3. Apply the filters, then the tab; the triage queue is most severe first, then newest
  const matchesFilters = useCallback(
    (incident: Incident) => {
      const query = search.trim().toLowerCase()
      return (
        (!severity || incident.severity === severity) &&
        (!site || incident.siteId === site) &&
        (!assignee || (assignee === 'UNASSIGNED' ? !incident.assignedTo : incident.assignedTo === assignee)) &&
        (!query || incident.title.toLowerCase().includes(query) || incident.referenceNumber.toLowerCase().includes(query))
      )
    },
    [severity, site, assignee, search],
  )
  const filtered = useMemo(() => incidents.filter(matchesFilters), [incidents, matchesFilters])
  const shown = useMemo(() => {
    const inTab = filtered.filter((incident) => isInTab(incident, tab))
    if (tab !== 'TRIAGE') return inTab
    return [...inTab].sort((a, b) => severityRank(b.severity) - severityRank(a.severity) || b.updatedAt.localeCompare(a.updatedAt))
  }, [filtered, tab])

  // 4. Runs one write and shows the server's problem if it refuses, for example a 412 when the app changed it first
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

  const technicianName = (id: string | null) => reference.technicians.find((technician) => technician.id === id)?.displayName ?? id ?? ''
  const siteName = (id: string) => reference.sites.find((candidate) => candidate.id === id)?.name ?? id

  return (
    <main>
      <header>
        <h1>Operator dashboard</h1>
        <span className="live">{isLive ? 'Live' : 'Reconnecting…'}</span>
        <button onClick={refresh}>Refresh</button>
      </header>

      {/* Tabs with counts after the filters, so the operator sees where the work is */}
      <nav className="tabs">
        {tabs.map(({ tab: value, label }) => (
          <button key={value} className={value === tab ? 'selected' : undefined} onClick={() => setTab(value)}>
            {label} ({filtered.filter((incident) => isInTab(incident, value)).length})
          </button>
        ))}
      </nav>

      <div className="filters">
        <input type="search" placeholder="Search title or reference" value={search} onChange={(event) => setSearch(event.target.value)} />
        <select value={severity} onChange={(event) => setSeverity(event.target.value)}>
          <option value="">Any severity</option>
          {severities.map((value) => (
            <option key={value} value={value}>
              {value}
            </option>
          ))}
        </select>
        <select value={site} onChange={(event) => setSite(event.target.value)}>
          <option value="">Any site</option>
          {reference.sites.map((value) => (
            <option key={value.id} value={value.id}>
              {value.name}
            </option>
          ))}
        </select>
        <select value={assignee} onChange={(event) => setAssignee(event.target.value)}>
          <option value="">Anyone</option>
          <option value="UNASSIGNED">Unassigned</option>
          {reference.technicians.map((technician) => (
            <option key={technician.id} value={technician.id}>
              {technician.displayName}
            </option>
          ))}
        </select>
      </div>

      {error && <p className="error">{error}</p>}
      <table>
        <thead>
          <tr>
            <th>Reference</th>
            <th>Title</th>
            <th>Site</th>
            <th>Reporter</th>
            <th>Severity</th>
            <th>Status</th>
            <th>Assigned to</th>
            <th>Version</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {shown.map((incident) => {
            const isDeleted = incident.deletedAt !== null
            return (
              <tr key={incident.id} className={isDeleted ? 'deleted' : undefined}>
                <td>{incident.referenceNumber}</td>
                <td>{incident.title}</td>
                <td>{siteName(incident.siteId)}</td>
                <td>{technicianName(incident.reporterId)}</td>
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
                    {reference.technicians.map((technician) => (
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
          {shown.length === 0 && (
            <tr>
              <td colSpan={9}>No incidents match.</td>
            </tr>
          )}
        </tbody>
      </table>
    </main>
  )
}
