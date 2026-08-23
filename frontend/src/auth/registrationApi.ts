import { ApiError } from './authApi'
import type {
  ApiErrorResponse,
  Region,
  Sport,
  TrainerRegistrationRequest,
  UserRegistrationRequest,
} from './types'

async function throwRegistrationError(response: Response): Promise<never> {
  const body: unknown = await response.json().catch(() => null)
  const errorResponse = body as ApiErrorResponse | null

  if (errorResponse?.message) {
    throw new ApiError(errorResponse)
  }

  throw new Error('Registration failed. Please try again.')
}

async function postRegistration(
  endpoint: string,
  request: UserRegistrationRequest | TrainerRegistrationRequest,
): Promise<void> {
  let response: Response

  try {
    response = await fetch(endpoint, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(request),
    })
  } catch {
    throw new Error('Could not reach the Csports server. Please try again.')
  }

  if (!response.ok) {
    await throwRegistrationError(response)
  }
}

async function getReferenceData<T>(endpoint: string): Promise<T> {
  let response: Response

  try {
    response = await fetch(endpoint)
  } catch {
    throw new Error('Could not load the signup options.')
  }

  if (!response.ok) {
    throw new Error('Could not load the signup options.')
  }

  return response.json() as Promise<T>
}

export function registerUser(request: UserRegistrationRequest): Promise<void> {
  return postRegistration('/api/v1/auth/register/user', request)
}

export function registerTrainer(
  request: TrainerRegistrationRequest,
): Promise<void> {
  return postRegistration('/api/v1/auth/register/trainer', request)
}

export function getRegions(): Promise<Region[]> {
  return getReferenceData<Region[]>('/api/v1/regions')
}

export function getSports(): Promise<Sport[]> {
  return getReferenceData<Sport[]>('/api/v1/sports/list')
}
