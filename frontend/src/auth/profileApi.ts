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

async function readProfile<T>(response: Response): Promise<T> {
  const body: unknown = await response.json().catch(() => null)

  if (!response.ok) {
    const errorResponse = body as ApiErrorResponse | null

    if (errorResponse?.message) {
      throw new ApiError(errorResponse)
    }

    throw new Error('The profile could not be loaded.')
  }

  return body as T
}

export async function getMyUserProfile(
  authenticatedFetch: AuthenticatedFetch,
): Promise<UserProfile> {
  const response = await authenticatedFetch('/api/v1/users/me')
  return readProfile<UserProfile>(response)
}

export async function getMyTrainerProfile(
  authenticatedFetch: AuthenticatedFetch,
): Promise<TrainerProfile> {
  const response = await authenticatedFetch('/api/v1/trainers/me')
  return readProfile<TrainerProfile>(response)
}
