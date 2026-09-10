import { ApiError } from '../auth/authApi'
import type { AuthenticatedFetch } from '../auth/authContext'
import type { ApiErrorResponse } from '../auth/types'
import type {
  CreateTrainingSessionRequest,
  ParticipantPage,
  Region,
  SessionPage,
  TrainingSession,
  UpdateTrainingSessionRequest,
  ResolvedGoogleMapsLocation,
} from './types'

async function readResponse<T>(
  response: Response,
  fallbackMessage: string,
): Promise<T> {
  const body: unknown = await response.json().catch(() => null)

  if (!response.ok) {
    const error = body as ApiErrorResponse | null
    if (error?.message) {
      throw new ApiError(error)
    }
    throw new Error(fallbackMessage)
  }

  return body as T
}

export async function getRegions(signal?: AbortSignal): Promise<Region[]> {
  const response = await fetch('/api/v1/regions', { signal })
  return readResponse<Region[]>(response, 'Areas could not be loaded.')
}

export async function resolveGoogleMapsLocation(
  authenticatedFetch: AuthenticatedFetch,
  url: string,
): Promise<ResolvedGoogleMapsLocation> {
  const response = await authenticatedFetch('/api/v1/locations/google-maps/resolve', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ url }),
  })
  return readResponse<ResolvedGoogleMapsLocation>(
    response,
    'The Google Maps link could not be verified.',
  )
}

export async function getMySessions(
  authenticatedFetch: AuthenticatedFetch,
  signal?: AbortSignal,
): Promise<SessionPage> {
  const response = await authenticatedFetch('/api/v1/trainers/sessions?size=50', {
    signal,
  })
  return readResponse<SessionPage>(response, 'Your sessions could not be loaded.')
}

export async function createSession(
  authenticatedFetch: AuthenticatedFetch,
  request: CreateTrainingSessionRequest,
): Promise<TrainingSession> {
  const response = await authenticatedFetch('/api/v1/sessions', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  })
  return readResponse<TrainingSession>(response, 'The session could not be created.')
}

export async function updateSession(
  authenticatedFetch: AuthenticatedFetch,
  sessionId: number,
  request: UpdateTrainingSessionRequest,
): Promise<TrainingSession> {
  const response = await authenticatedFetch(`/api/v1/sessions/${sessionId}`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  })
  return readResponse<TrainingSession>(response, 'The session could not be updated.')
}

export async function cancelSession(
  authenticatedFetch: AuthenticatedFetch,
  sessionId: number,
  reason: string,
): Promise<void> {
  const response = await authenticatedFetch(
    `/api/v1/sessions/${sessionId}/cancel`,
    {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ reason }),
    },
  )
  if (!response.ok) {
    await readResponse<never>(response, 'The session could not be cancelled.')
  }
}

export async function restoreSession(
  authenticatedFetch: AuthenticatedFetch,
  sessionId: number,
): Promise<void> {
  const response = await authenticatedFetch(
    `/api/v1/sessions/${sessionId}/restore`,
    { method: 'PATCH' },
  )
  if (!response.ok) {
    await readResponse<never>(response, 'The session could not be restored.')
  }
}

export async function getParticipants(
  authenticatedFetch: AuthenticatedFetch,
  sessionId: number,
  signal?: AbortSignal,
): Promise<ParticipantPage> {
  const response = await authenticatedFetch(
    `/api/v1/sessions/${sessionId}/participants?size=100`,
    { signal },
  )
  return readResponse<ParticipantPage>(response, 'Participants could not be loaded.')
}
