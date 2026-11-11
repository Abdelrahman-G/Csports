import { ApiError } from '../auth/authApi'
import type { AuthenticatedFetch } from '../auth/authContext'
import type { ApiErrorResponse } from '../auth/types'
import type {
  CreateTrainingCourseRequest,
  ParticipantPage,
  Region,
  CoursePage,
  TrainingCourse,
  UpdateTrainingCourseRequest,
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

export async function getMyCourses(
  authenticatedFetch: AuthenticatedFetch,
  signal?: AbortSignal,
): Promise<CoursePage> {
  const response = await authenticatedFetch('/api/v1/trainers/courses?size=50', {
    signal,
  })
  return readResponse<CoursePage>(response, 'Your courses could not be loaded.')
}

export async function createCourse(
  authenticatedFetch: AuthenticatedFetch,
  request: CreateTrainingCourseRequest,
): Promise<TrainingCourse> {
  const response = await authenticatedFetch('/api/v1/courses', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  })
  return readResponse<TrainingCourse>(response, 'The course could not be created.')
}

export async function updateCourse(
  authenticatedFetch: AuthenticatedFetch,
  courseId: number,
  request: UpdateTrainingCourseRequest,
): Promise<TrainingCourse> {
  const response = await authenticatedFetch(`/api/v1/courses/${courseId}`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  })
  return readResponse<TrainingCourse>(response, 'The course could not be updated.')
}

export async function cancelCourse(
  authenticatedFetch: AuthenticatedFetch,
  courseId: number,
  reason: string,
): Promise<void> {
  const response = await authenticatedFetch(
    `/api/v1/courses/${courseId}/cancel`,
    {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ reason }),
    },
  )
  if (!response.ok) {
    await readResponse<never>(response, 'The course could not be cancelled.')
  }
}

export async function restoreCourse(
  authenticatedFetch: AuthenticatedFetch,
  courseId: number,
): Promise<void> {
  const response = await authenticatedFetch(
    `/api/v1/courses/${courseId}/restore`,
    { method: 'PATCH' },
  )
  if (!response.ok) {
    await readResponse<never>(response, 'The course could not be restored.')
  }
}

export async function getParticipants(
  authenticatedFetch: AuthenticatedFetch,
  courseId: number,
  signal?: AbortSignal,
): Promise<ParticipantPage> {
  const response = await authenticatedFetch(
    `/api/v1/courses/${courseId}/participants?size=100`,
    { signal },
  )
  return readResponse<ParticipantPage>(response, 'Participants could not be loaded.')
}
