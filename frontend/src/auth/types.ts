export type AuthRole = 'USER' | 'TRAINER' | 'ADMIN'

export type LoginRequest = {
  identifier: string
  password: string
}

export type AuthSession = {
  accessToken: string
  refreshToken: string
  role: AuthRole
}

export type ApiErrorResponse = {
  status: number
  code: string
  error: string
  message: string
  path: string
  timestamp: string
  fieldErrors: Record<string, string>
}
