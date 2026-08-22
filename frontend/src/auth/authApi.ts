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

export async function refreshAccount(
  refreshToken: string,
): Promise<AuthSession> {
  const response = await fetch('/api/v1/auth/refresh', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ refreshToken }),
  })

  const body: unknown = await response.json().catch(() => null)

  if (!response.ok) {
    const errorResponse = body as ApiErrorResponse | null

    if (errorResponse?.message) {
      throw new ApiError(errorResponse)
    }

    throw new Error('Your session could not be refreshed.')
  }

  if (!isAuthSession(body)) {
    throw new Error('The server returned an invalid refresh response.')
  }

  return body
}

export async function logoutAccount(session: AuthSession): Promise<void> {
  const response = await fetch('/api/v1/auth/logout', {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${session.accessToken}`,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ refreshToken: session.refreshToken }),
  })

  if (!response.ok) {
    const body: unknown = await response.json().catch(() => null)
    const errorResponse = body as ApiErrorResponse | null

    if (errorResponse?.message) {
      throw new ApiError(errorResponse)
    }

    throw new Error('The server could not complete logout.')
  }
}
