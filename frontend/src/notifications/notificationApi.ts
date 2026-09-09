import { ApiError } from '../auth/authApi'
import type { AuthenticatedFetch } from '../auth/authContext'
import type { ApiErrorResponse } from '../auth/types'
import type {
  NotificationPage,
  UnreadNotificationCount,
  UserNotification,
} from './types'

async function readNotificationJson<T>(
  response: Response,
  fallbackMessage: string,
): Promise<T> {
  const body: unknown = await response.json().catch(() => null)

  if (!response.ok) {
    const errorResponse = body as ApiErrorResponse | null

    if (errorResponse?.message) {
      throw new ApiError(errorResponse)
    }

    throw new Error(fallbackMessage)
  }

  return body as T
}

export async function getNotifications(
  authenticatedFetch: AuthenticatedFetch,
  page: number,
  size: number,
  signal?: AbortSignal,
): Promise<NotificationPage> {
  const parameters = new URLSearchParams({
    page: String(page),
    size: String(size),
  })
  const response = await authenticatedFetch(
    `/api/v1/notifications?${parameters}`,
    { signal },
  )

  return readNotificationJson<NotificationPage>(
    response,
    'Your notifications could not be loaded.',
  )
}

export async function getUnreadNotificationCount(
  authenticatedFetch: AuthenticatedFetch,
  signal?: AbortSignal,
): Promise<UnreadNotificationCount> {
  const response = await authenticatedFetch(
    '/api/v1/notifications/unread-count',
    { signal },
  )

  return readNotificationJson<UnreadNotificationCount>(
    response,
    'Your unread notification count could not be loaded.',
  )
}

export async function markNotificationAsRead(
  authenticatedFetch: AuthenticatedFetch,
  notificationId: number,
): Promise<UserNotification> {
  const response = await authenticatedFetch(
    `/api/v1/notifications/${notificationId}/read`,
    { method: 'PATCH' },
  )

  return readNotificationJson<UserNotification>(
    response,
    'The notification could not be opened.',
  )
}
