import type { ApiErrorResponse } from '../auth/types'
import type {
  NearbyCoordinates,
  Region,
  SessionPage,
  Sport,
  TrainingSessionDetails,
} from './types'

type SearchByRegion = {
  mode: 'region'
  query?: string
  regionId?: number
  sportId?: number
}

type SearchNearby = {
  mode: 'nearby'
  query?: string
  sportId?: number
  coordinates: NearbyCoordinates
  radiusKm: number
}

export type SessionSearch = SearchByRegion | SearchNearby

async function readJson<T>(response: Response, fallback: string): Promise<T> {
  const body: unknown = await response.json().catch(() => null)

  if (!response.ok) {
    const apiError = body as ApiErrorResponse | null
    throw new Error(apiError?.message ?? fallback)
  }

  return body as T
}

export async function getRegions(signal?: AbortSignal): Promise<Region[]> {
  const response = await fetch('/api/v1/regions', { signal })
  return readJson<Region[]>(response, 'Regions could not be loaded.')
}

export async function getSports(signal?: AbortSignal): Promise<Sport[]> {
  const response = await fetch('/api/v1/sports/list', { signal })
  return readJson<Sport[]>(response, 'Sports could not be loaded.')
}

export async function searchSessions(
  search: SessionSearch,
  page: number,
  signal?: AbortSignal,
): Promise<SessionPage> {
  const parameters = new URLSearchParams({
    page: String(page),
    size: '12',
  })

  if (search.query) {
    parameters.set('q', search.query)
  }
  if (search.sportId !== undefined) {
    parameters.set('sportId', String(search.sportId))
  }

  if (search.mode === 'region') {
    if (search.regionId !== undefined) {
      parameters.set('regionId', String(search.regionId))
    }
  } else {
    parameters.set('latitude', String(search.coordinates.latitude))
    parameters.set('longitude', String(search.coordinates.longitude))
    parameters.set('radiusKm', String(search.radiusKm))
    parameters.set('sortBy', 'distance')
  }

  const response = await fetch(`/api/v1/sessions?${parameters}`, { signal })
  return readJson<SessionPage>(response, 'Sessions could not be loaded.')
}

export async function getSessionDetails(
  sessionId: number,
  signal?: AbortSignal,
): Promise<TrainingSessionDetails> {
  const response = await fetch(`/api/v1/sessions/${sessionId}`, { signal })
  return readJson<TrainingSessionDetails>(
    response,
    'Session details could not be loaded.',
  )
}
