import type { AuthRole } from './types'

export function getHomePath(role: AuthRole) {
  if (role === 'TRAINER') {
    return '/trainer/home'
  }

  if (role === 'USER') {
    return '/user/home'
  }

  return '/'
}
