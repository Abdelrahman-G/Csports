export type AuthRole = 'USER' | 'TRAINER' | 'ADMIN'

export type LoginRequest = {
  identifier: string
  password: string
}

export type UserRegistrationRequest = {
  name: string
  email: string
  phoneNumber: string
  password: string
  age: number
}

export type TrainerRegistrationRequest = UserRegistrationRequest & {
  bio: string
  experienceYears: number
  sportId: number
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

export type Sport = {
  id: number
  name: string
}
