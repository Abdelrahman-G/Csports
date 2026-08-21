import { useState, type PropsWithChildren } from 'react'
import { AuthContext } from './authContext'
import type { AuthSession } from './types'

const STORAGE_KEY = 'csports.auth.session'

function readStoredSession(): AuthSession | null {
  const storedValue = sessionStorage.getItem(STORAGE_KEY)

  if (!storedValue) {
    return null
  }

  try {
    const session = JSON.parse(storedValue) as Partial<AuthSession>
    const validRole =
      session.role === 'USER' ||
      session.role === 'TRAINER' ||
      session.role === 'ADMIN'

    if (
      typeof session.accessToken === 'string' &&
      typeof session.refreshToken === 'string' &&
      validRole
    ) {
      return session as AuthSession
    }
  } catch {
    // Invalid browser data is removed below.
  }

  sessionStorage.removeItem(STORAGE_KEY)
  return null
}

export default function AuthProvider({ children }: PropsWithChildren) {
  const [session, setSession] = useState<AuthSession | null>(readStoredSession)

  function signIn(newSession: AuthSession) {
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(newSession))
    setSession(newSession)
  }

  function clearSession() {
    sessionStorage.removeItem(STORAGE_KEY)
    setSession(null)
  }

  return (
    <AuthContext.Provider value={{ session, signIn, clearSession }}>
      {children}
    </AuthContext.Provider>
  )
}
