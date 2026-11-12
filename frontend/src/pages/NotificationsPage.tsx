import AppNavigation from '../components/AppNavigation'
import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/authContext'
import {
  getNotifications,
  markNotificationAsRead,
} from '../notifications/notificationApi'
import type { NotificationPage } from '../notifications/types'

const PAGE_SIZE = 10

const notificationDateFormatter = new Intl.DateTimeFormat('en-EG', {
  day: 'numeric',
  month: 'short',
  year: 'numeric',
  hour: 'numeric',
  minute: '2-digit',
})

function NotificationsPage() {
  const navigate = useNavigate()
  const { authenticatedFetch } = useAuth()
  const [pageNumber, setPageNumber] = useState(0)
  const [notificationPage, setNotificationPage] =
    useState<NotificationPage | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [openingNotificationId, setOpeningNotificationId] = useState<
    number | null
  >(null)
  const [error, setError] = useState('')

  useEffect(() => {
    const controller = new AbortController()

    async function loadNotifications() {
      setIsLoading(true)
      setError('')

      try {
        const loadedPage = await getNotifications(
          authenticatedFetch,
          pageNumber,
          PAGE_SIZE,
          controller.signal,
        )
        setNotificationPage(loadedPage)
      } catch (requestError) {
        if (!controller.signal.aborted) {
          setNotificationPage(null)
          setError(
            requestError instanceof Error
              ? requestError.message
              : 'Your notifications could not be loaded.',
          )
        }
      } finally {
        if (!controller.signal.aborted) {
          setIsLoading(false)
        }
      }
    }

    void loadNotifications()
    return () => controller.abort()
  }, [authenticatedFetch, pageNumber])

  async function openCourse(
    notificationId: number,
    courseId: number,
    isRead: boolean,
    announcementId: number | null,
  ) {
    if (openingNotificationId !== null) {
      return
    }

    setOpeningNotificationId(notificationId)
    setError('')

    try {
      if (!isRead) {
        await markNotificationAsRead(authenticatedFetch, notificationId)
        setNotificationPage((currentPage) =>
          currentPage
            ? {
                ...currentPage,
                content: currentPage.content.map((notification) =>
                  notification.id === notificationId
                    ? { ...notification, read: true }
                    : notification,
                ),
              }
            : currentPage,
        )
      }

      navigate(`/user/courses/${courseId}${announcementId ? `?announcement=${announcementId}#announcement-${announcementId}` : ''}`)
    } catch (requestError) {
      setError(
        requestError instanceof Error
          ? requestError.message
          : 'The notification could not be opened.',
      )
      setOpeningNotificationId(null)
    }
  }

  const notifications = notificationPage?.content ?? []

  return (
    <>
      <AppNavigation />
      <main className="notifications-page">
        <header className="notifications-header">
          <p className="eyebrow">Course updates</p>
          <h1>Notifications</h1>
          <p>Important changes to courses you have booked appear here.</p>
        </header>

        {error && (
          <p className="bookings-error" role="alert">
            {error}
          </p>
        )}

        <section className="notification-list" aria-live="polite">
          {isLoading ? (
            <p className="booking-list-state">Loading notifications...</p>
          ) : notifications.length === 0 ? (
            <div className="booking-list-state">
              <h2>No notifications yet</h2>
              <p>Updates to your booked courses will appear here.</p>
            </div>
          ) : (
            notifications.map((notification) => (
              <article
                className={`notification-card ${notification.read ? '' : 'unread'}`}
                key={notification.id}
              >
                <div>
                  <div className="notification-card-heading">
                    <h2>{notification.title}</h2>
                    {!notification.read && <span>New</span>}
                  </div>
                  <p>{notification.message}</p>
                  <time dateTime={notification.createdAt}>
                    {notificationDateFormatter.format(
                      new Date(notification.createdAt),
                    )}
                  </time>
                </div>

                <button
                  type="button"
                  onClick={() =>
                    void openCourse(
                      notification.id,
                      notification.courseId,
                      notification.read,
                      notification.type === 'COURSE_CANCELLED' ? null : notification.announcementId,
                    )
                  }
                  disabled={openingNotificationId !== null}
                >
                  {openingNotificationId === notification.id
                    ? 'Opening...'
                    : 'View course'}
                </button>
              </article>
            ))
          )}
        </section>

        {notificationPage && notificationPage.totalPages > 1 && (
          <nav className="course-pagination" aria-label="Notification pages">
            <button
              type="button"
              onClick={() => setPageNumber((current) => current - 1)}
              disabled={notificationPage.first || isLoading}
            >
              Previous
            </button>
            <span>
              Page {notificationPage.page + 1} of {notificationPage.totalPages}
            </span>
            <button
              type="button"
              onClick={() => setPageNumber((current) => current + 1)}
              disabled={notificationPage.last || isLoading}
            >
              Next
            </button>
          </nav>
        )}
      </main>
    </>
  )
}

export default NotificationsPage
