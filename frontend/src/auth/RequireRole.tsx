import type { ReactNode } from 'react'
import { Navigate } from 'react-router-dom'
import { useAuth } from './authContext'
import { getHomePath } from './routePaths'
import type { AuthRole } from './types'

type RequireRoleProps = {
  allowedRole: AuthRole
  children: ReactNode
}

export default function RequireRole({
  allowedRole,
  children,
}: RequireRoleProps) {
  const { session } = useAuth()

  if (!session) {
    return <Navigate to="/login" replace />
  }

  if (session.role !== allowedRole) {
    return <Navigate to={getHomePath(session.role)} replace />
  }

  return children
}
