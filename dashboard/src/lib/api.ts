import type { Incident, Problem, Technician } from './types'

// The mock server; 127.0.0.1 rather than localhost because the server listens on IPv4 only
export const apiUrl = process.env.NEXT_PUBLIC_API_URL ?? 'http://127.0.0.1:3000'

// The operator account from the seed, used as deletedBy
const operatorId = 'u3'

// Every incident, newest change first, tombstones included
export async function fetchIncidents(): Promise<Incident[]> {
  const response = await fetch(`${apiUrl}/incidents?_sort=changeSeq&_order=desc`, { cache: 'no-store' })
  return response.json()
}

// Technicians come from the same /reference call the app uses
export async function fetchTechnicians(): Promise<Technician[]> {
  const response = await fetch(`${apiUrl}/reference`, { cache: 'no-store' })
  return (await response.json()).technicians
}

// Sends operator fields with the version the dashboard last saw; returns the problem if the server refuses
export async function updateIncident(incident: Incident, changes: Partial<Incident>): Promise<Problem | null> {
  const response = await fetch(`${apiUrl}/incidents/${incident.id}`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json', 'If-Match': `"${incident.version}"` },
    body: JSON.stringify(changes),
  })
  return response.ok ? null : response.json()
}

// A soft delete; the unchanged status marks it as an operator write, so the server skips the technician status lock
export function deleteIncident(incident: Incident): Promise<Problem | null> {
  return updateIncident(incident, {
    status: incident.status,
    deletedAt: new Date().toISOString(),
    deletedBy: operatorId,
  })
}
