// The slice of the API contract the dashboard reads; field names match the server and the app

export type IncidentStatus = 'OPEN' | 'ASSIGNED' | 'IN_PROGRESS' | 'RESOLVED' | 'CLOSED'

export interface Incident {
  id: string
  referenceNumber: string
  reporterId: string
  title: string
  severity: string
  siteId: string
  status: IncidentStatus
  assignedTo: string | null
  rootCause: string | null
  correctiveActions: string | null
  version: number
  changeSeq: number
  updatedAt: string
  deletedAt: string | null
  deletedBy: string | null
}

export interface Technician {
  id: string
  displayName: string
}

export interface Site {
  id: string
  name: string
}

// The parts of GET /reference the dashboard uses
export interface Reference {
  sites: Site[]
  technicians: Technician[]
}

// RFC 9457 Problem Details, as the middlewares file sends them
export interface Problem {
  title?: string
  status?: number
  code?: string
}
