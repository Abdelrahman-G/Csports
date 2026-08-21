import type { ApiErrorResponse, AuthSession, LoginRequest } from './types'

export class ApiError extends Error {
  readonly status: number
  readonly code: string
  readonly fieldErrors: Record<string, string>

  constructor(response: ApiErrorResponse) {
    super(response.message)
    this.name = 'ApiError'
    this.status = response.status
    this.code = response.code
    this.fieldErrors = response.fieldErrors ?? {}
  }
}

function isAuthSession(value: unknown): value is AuthSession {
  if (typeof value !== 'object' || value === null) {
    return false
  }

  const response = value as Partial<AuthSession>
  const validRole =
    response.role === 'USER' ||
    response.role === 'TRAINER' ||
    response.role === 'ADMIN'

  return (
    typeof response.accessToken === 'string' &&
    typeof response.refreshToken === 'string' &&
    validRole
  )
}

export async function loginAccount(request: LoginRequest): Promise<AuthSession> {
  let response: Response

  try {
    response = await fetch('/api/v1/auth/login', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(request),
    })
  } catch {
    throw new Error('Could not reach the Csports server. Please try again.')
  }

  const body: unknown = await response.json().catch(() => null)

  if (!response.ok) {
    const errorResponse = body as ApiErrorResponse | null

    if (errorResponse?.message) {
      throw new ApiError(errorResponse)
    }

    throw new Error('Login failed. Please try again.')
  }

  if (!isAuthSession(body)) {
    throw new Error('The server returned an invalid login response.')
  }

  return body
}
