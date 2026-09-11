import { ApiError } from './authApi'
import type { AuthenticatedFetch } from './authContext'
import type { ApiErrorResponse, AuthRole } from './types'

export type UserProfile = {
  id: number
  name: string
  email: string
  phoneNumber: string
  age: number
  role: AuthRole
  photoUrl: string | null
  regionId: number | null
  regionName: string | null
  city: string | null
  country: string | null
  latitude: number | null
  longitude: number | null
}

export type TrainerProfile = {
  id: number
  name: string
  photoUrl: string | null
  bio: string | null
  experienceYears: number
  sportId: number
  sport: string
  regionId: number | null
  regionName: string | null
  city: string | null
  country: string | null
}

export type UpdateUserProfileRequest = {
  name?: string
  phoneNumber?: string
  age?: number
}

export type UpdateTrainerProfileRequest = {
  bio?: string
  experienceYears?: number
}

async function readProfile<T>(
  response: Response,
  fallbackMessage: string,
): Promise<T> {
  const body: unknown = await response.json().catch(() => null)

  if (!response.ok) {
    const errorResponse = body as ApiErrorResponse | null

    if (errorResponse?.message) {
      throw new ApiError(errorResponse)
    }

    throw new Error(fallbackMessage)
  }

  return body as T
}

export async function getMyUserProfile(
  authenticatedFetch: AuthenticatedFetch,
  signal?: AbortSignal,
): Promise<UserProfile> {
  const response = await authenticatedFetch('/api/v1/users/me', { signal })
  return readProfile<UserProfile>(response, 'The profile could not be loaded.')
}

export async function getMyTrainerProfile(
  authenticatedFetch: AuthenticatedFetch,
  signal?: AbortSignal,
): Promise<TrainerProfile> {
  const response = await authenticatedFetch('/api/v1/trainers/me', { signal })
  return readProfile<TrainerProfile>(response, 'The coaching profile could not be loaded.')
}

export async function updateMyUserProfile(
  authenticatedFetch: AuthenticatedFetch,
  request: UpdateUserProfileRequest,
): Promise<UserProfile> {
  const response = await authenticatedFetch('/api/v1/users/me', {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  })
  return readProfile<UserProfile>(response, 'The account details could not be updated.')
}

export async function updateMyTrainerProfile(
  authenticatedFetch: AuthenticatedFetch,
  request: UpdateTrainerProfileRequest,
): Promise<TrainerProfile> {
  const response = await authenticatedFetch('/api/v1/trainers/me', {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  })
  return readProfile<TrainerProfile>(response, 'The coaching profile could not be updated.')
}
