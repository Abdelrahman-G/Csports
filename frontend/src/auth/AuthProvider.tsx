import { useCallback, useRef, useState, type PropsWithChildren } from 'react'
import { logoutAccount, refreshAccount } from './authApi'
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
  const sessionRef = useRef<AuthSession | null>(session)
  const refreshPromiseRef = useRef<Promise<AuthSession> | null>(null)

  const signIn = useCallback((newSession: AuthSession) => {
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(newSession))
    sessionRef.current = newSession
    setSession(newSession)
  }, [])

  const clearSession = useCallback(() => {
    sessionStorage.removeItem(STORAGE_KEY)
    sessionRef.current = null
    setSession(null)
  }, [])

  const logout = useCallback(async () => {
    const activeSession = sessionRef.current

    try {
      if (activeSession) {
        await logoutAccount(activeSession)
      }
    } finally {
      clearSession()
    }
  }, [clearSession])

  const authenticatedFetch = useCallback(
    async (input: RequestInfo | URL, init: RequestInit = {}) => {
      let activeSession = sessionRef.current

      if (!activeSession) {
        throw new Error('You must log in before making this request.')
      }

      const sendRequest = (accessToken: string) => {
        const headers = new Headers(init.headers)
        headers.set('Authorization', `Bearer ${accessToken}`)

        return fetch(input, { ...init, headers })
      }

      let response = await sendRequest(activeSession.accessToken)

      if (response.status !== 401) {
        return response
      }

      const latestSession = sessionRef.current

      if (
        latestSession &&
        latestSession.accessToken !== activeSession.accessToken
      ) {
        return sendRequest(latestSession.accessToken)
      }

      try {
        if (!refreshPromiseRef.current) {
          refreshPromiseRef.current = refreshAccount(activeSession.refreshToken)
        }

        const refreshPromise = refreshPromiseRef.current
        activeSession = await refreshPromise
        signIn(activeSession)
        response = await sendRequest(activeSession.accessToken)
        return response
      } catch {
        clearSession()
        throw new Error('Your session expired. Please log in again.')
      } finally {
        if (refreshPromiseRef.current) {
          refreshPromiseRef.current = null
        }
      }
    },
    [clearSession, signIn],
  )

  return (
    <AuthContext.Provider
      value={{ session, signIn, clearSession, logout, authenticatedFetch }}
    >
      {children}
    </AuthContext.Provider>
  )
}
