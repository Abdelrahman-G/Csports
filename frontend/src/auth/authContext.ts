import { createContext, useContext } from 'react'
import type { AuthSession } from './types'

export type AuthContextValue = {
  session: AuthSession | null
  signIn: (session: AuthSession) => void
  clearSession: () => void
}

export const AuthContext = createContext<AuthContextValue | undefined>(
  undefined,
)

export function useAuth() {
  const context = useContext(AuthContext)

  if (!context) {
    throw new Error('useAuth must be used inside AuthProvider.')
  }

  return context
}
