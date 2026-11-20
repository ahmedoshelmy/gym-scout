// Typed client for the Gym Scout API. Same-origin: Vite proxies /api in dev, nginx in compose.

export type LatLon = { lat: number; lon: number }
export type TravelMode = 'DRIVE' | 'WALK'

export type ScoutResult = {
  gymId: string
  name: string
  lat: number
  lon: number
  distanceMeters: number
  score: number
  pricePoints: number
  travelPoints: number
  amenityPoints: number
  perVisitMinor?: number
  currency?: string
  bestPlan?: string
  travelMinutes: number
  travelEstimated: boolean
  amenities: string[]
  reasons: string[]
}

export type ScoutResponse = { travelEstimated: boolean; results: ScoutResult[] }

export type MembershipPlan = {
  id: string
  name: string
  priceMinor: number
  currency: string
  durationDays: number
  visitsIncluded?: number
}

export type Gym = {
  id: string
  name: string
  location: LatLon
  address?: string
  amenities: string[]
  openingHours?: string
  source: string
  plans: MembershipPlan[]
}

export type NearbyGym = { gym: Gym; distanceMeters: number }

export type SetInput = { exercise: string; weightKg: number; reps: number }

export type PersonalRecord = {
  sessionId: string
  on: string
  exercise: string
  kind: 'HEAVIEST_WEIGHT' | 'ESTIMATED_ONE_REP_MAX'
  value: number
  previous: number
}

export type LoggedSession = { id: string; performedOn: string; records: PersonalRecord[] }

export type Progress = {
  weeklyVolume: { weekStart: string; volumeKg: number; sets: number }[]
  bests: { exercise: string; heaviestKg: number; bestOneRepMax: number; sessions: number }[]
  recentRecords: PersonalRecord[]
}

export type SessionSummary = {
  id: string
  performedOn: string
  gymId?: string
  notes?: string
  sets: number
  volumeKg: number
}

export type MembershipView = {
  membership: {
    id: string
    gymId: string
    planName: string
    priceMinor: number
    currency: string
    startsOn: string
    endsOn: string
  }
  gymName: string
  stats: {
    visits: number
    daysElapsed: number
    totalDays: number
    costPerVisitMinor: number
    projectedVisits: number
    projectedCostPerVisitMinor: number
    visitsPerWeek: number
  }
  visits: string[]
}

export class ApiError extends Error {}

async function call<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(path, {
    ...init,
    headers: { 'Content-Type': 'application/json', ...init?.headers },
  })
  if (!res.ok) {
    let detail = `${res.status} ${res.statusText}`
    try {
      const problem = (await res.json()) as { detail?: string }
      if (problem.detail) detail = problem.detail
    } catch {
      // not a problem-details body; keep the status line
    }
    throw new ApiError(detail)
  }
  if (res.status === 204) return undefined as T
  return (await res.json()) as T
}

const post = <T>(path: string, body: unknown) =>
  call<T>(path, { method: 'POST', body: JSON.stringify(body) })

export const api = {
  scout: (query: string) => call<ScoutResponse>(`/api/scout${query}`),
  nearbyGyms: (at: LatLon, radius = 50000) =>
    call<NearbyGym[]>(`/api/places/gyms?lat=${at.lat}&lon=${at.lon}&radius=${radius}&limit=100`),
  addPlan: (gymId: string, plan: Omit<MembershipPlan, 'id'>) =>
    post<MembershipPlan>(`/api/places/gyms/${gymId}/plans`, plan),
  logSession: (body: { performedOn?: string; gymId?: string; notes?: string; sets: SetInput[] }) =>
    post<LoggedSession>('/api/training/sessions', body),
  sessions: () => call<SessionSummary[]>('/api/training/sessions?limit=10'),
  progress: (weeks = 8) => call<Progress>(`/api/training/progress?weeks=${weeks}`),
  currentMembership: () => call<MembershipView | undefined>('/api/memberships/current'),
  startMembership: (body: {
    gymId: string
    planName: string
    priceMinor: number
    currency: string
    startsOn: string
    endsOn: string
  }) => post<MembershipView>('/api/memberships', body),
  addVisit: (id: string) => post<MembershipView>(`/api/memberships/${id}/visits`, {}),
}
