export type NotificationType = 'COURSE_UPDATED' | 'COURSE_CANCELLED' | 'COURSE_ANNOUNCEMENT'

export type UserNotification = {
  id: number
  courseId: number
  announcementId: number | null
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
