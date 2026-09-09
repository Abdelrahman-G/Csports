export type NotificationType = 'SESSION_UPDATED' | 'SESSION_CANCELLED'

export type UserNotification = {
  id: number
  sessionId: number
  type: NotificationType
  title: string
  message: string
  read: boolean
  createdAt: string
}

export type NotificationPage = {
  content: UserNotification[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
}

export type UnreadNotificationCount = {
  unreadCount: number
}
