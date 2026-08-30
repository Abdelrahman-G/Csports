import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../auth/authContext'
import { cancelBooking, getMyBookings } from '../booking/bookingApi'
import type {
  BookedSession,
  BookingPage,
  BookingView,
} from '../booking/types'
import UserNavigation from '../components/UserNavigation'
import { orderedDayLabels } from '../discovery/sessionFormatting'

const PAGE_SIZE = 6

const bookingDateFormatter = new Intl.DateTimeFormat('en-EG', {
  weekday: 'short',
  day: 'numeric',
  month: 'short',
  year: 'numeric',
})

const bookingTimeFormatter = new Intl.DateTimeFormat('en-EG', {
  hour: 'numeric',
  minute: '2-digit',
})

function formatPrice(price: number) {
  return price === 0 ? 'Free' : `${price.toLocaleString('en-EG')} EGP`
}

function bookingStatusLabel(booking: BookedSession) {
  if (booking.bookingStatus === 'CANCELLED_BY_USER') {
    return 'Cancelled by you'
  }
  if (booking.bookingStatus === 'CANCELLED_BY_TRAINER') {
    return 'Cancelled by trainer'
  }
  if (booking.sessionStatus === 'COMPLETED') {
    return 'Completed'
  }
  if (booking.sessionStatus === 'CANCELLED') {
    return 'Session cancelled'
  }
  return 'Confirmed'
}

function MyBookingsPage() {
  const { authenticatedFetch } = useAuth()
  const [view, setView] = useState<BookingView>('UPCOMING')
  const [pageNumber, setPageNumber] = useState(0)
  const [bookingPage, setBookingPage] = useState<BookingPage | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')
  const [confirmationSessionId, setConfirmationSessionId] = useState<
    number | null
  >(null)
  const [cancellingSessionId, setCancellingSessionId] = useState<number | null>(
    null,
  )
  const [refreshVersion, setRefreshVersion] = useState(0)

  useEffect(() => {
    const controller = new AbortController()

    async function loadBookings() {
      setIsLoading(true)
      setError('')

      try {
        const loadedPage = await getMyBookings(
          authenticatedFetch,
          {
            view,
            page: pageNumber,
            size: PAGE_SIZE,
          },
          controller.signal,
        )

        setBookingPage(loadedPage)
      } catch (requestError) {
        if (!controller.signal.aborted) {
          setBookingPage(null)
          setError(
            requestError instanceof Error
              ? requestError.message
              : 'Your bookings could not be loaded.',
          )
        }
      } finally {
        if (!controller.signal.aborted) {
          setIsLoading(false)
        }
      }
    }

    void loadBookings()
    return () => controller.abort()
  }, [authenticatedFetch, pageNumber, refreshVersion, view])

  function selectView(nextView: BookingView) {
    if (nextView === view) {
      return
    }

    setView(nextView)
    setPageNumber(0)
    setConfirmationSessionId(null)
    setError('')
  }

  async function confirmCancellation(sessionId: number) {
    if (cancellingSessionId !== null) {
      return
    }

    setCancellingSessionId(sessionId)
    setError('')

    try {
      await cancelBooking(authenticatedFetch, sessionId)
      setConfirmationSessionId(null)
      setBookingPage((currentPage) => {
        if (!currentPage) {
          return currentPage
        }

        return {
          ...currentPage,
          content: currentPage.content.filter(
            (booking) => booking.sessionId !== sessionId,
          ),
          totalElements: Math.max(0, currentPage.totalElements - 1),
        }
      })

      if (bookingPage?.content.length === 1 && pageNumber > 0) {
        setPageNumber((current) => current - 1)
      } else {
        setRefreshVersion((current) => current + 1)
      }
    } catch (requestError) {
      setError(
        requestError instanceof Error
          ? requestError.message
          : 'This booking could not be cancelled.',
      )
    } finally {
      setCancellingSessionId(null)
    }
  }

  const bookings = bookingPage?.content ?? []

  return (
    <>
      <UserNavigation />
      <main className="bookings-page">
        <header className="bookings-header">
          <p className="eyebrow">Your training schedule</p>
          <h1>My bookings</h1>
          <p>Keep track of your upcoming sessions and booking history.</p>
        </header>

        <div className="booking-view-switch" aria-label="Booking views">
          <button
            type="button"
            className={view === 'UPCOMING' ? 'active' : ''}
            onClick={() => selectView('UPCOMING')}
            aria-pressed={view === 'UPCOMING'}
          >
            Upcoming
          </button>
          <button
            type="button"
            className={view === 'HISTORY' ? 'active' : ''}
            onClick={() => selectView('HISTORY')}
            aria-pressed={view === 'HISTORY'}
          >
            History
          </button>
        </div>

        {error && (
          <p className="bookings-error" role="alert">
            {error}
          </p>
        )}

        <section className="booking-list" aria-live="polite">
          {isLoading ? (
            <p className="booking-list-state">Loading your bookings...</p>
          ) : bookings.length === 0 ? (
            <div className="booking-list-state">
              <h2>
                {view === 'UPCOMING'
                  ? 'No upcoming bookings'
                  : 'No booking history yet'}
              </h2>
              <p>
                {view === 'UPCOMING'
                  ? 'Discover a session and reserve your next place.'
                  : 'Completed and cancelled bookings will appear here.'}
              </p>
              {view === 'UPCOMING' && (
                <Link to="/user/home">Discover sessions</Link>
              )}
            </div>
          ) : (
            bookings.map((booking) => {
              const firstTraining = new Date(booking.bookingClosesAt)
              const isAwaitingConfirmation =
                confirmationSessionId === booking.sessionId
              const isCancelling = cancellingSessionId === booking.sessionId
              const canCancel =
                view === 'UPCOMING' &&
                booking.bookingStatus === 'CONFIRMED' &&
                booking.sessionStatus === 'SCHEDULED'

              return (
                <article className="booking-card" key={booking.bookingId}>
                  <div className="booking-card-main">
                    <div className="booking-card-heading">
                      <span className="session-sport">{booking.sport}</span>
                      <span
                        className={`booking-state ${booking.bookingStatus.toLowerCase()}`}
                      >
                        {bookingStatusLabel(booking)}
                      </span>
                    </div>

                    <h2>{booking.title}</h2>
                    <p className="session-trainer">
                      with {booking.trainerName}
                    </p>

                    <div className="booking-card-details">
                      <div>
                        <span>First session</span>
                        <strong>
                          {bookingDateFormatter.format(firstTraining)}
                        </strong>
                        <p>{bookingTimeFormatter.format(firstTraining)}</p>
                      </div>
                      <div>
                        <span>Schedule</span>
                        <strong>
                          {orderedDayLabels(booking.days).join(', ')}
                        </strong>
                        <p>{booking.durationMinutes} minutes</p>
                      </div>
                      <div>
                        <span>Location</span>
                        <strong>{booking.locationName}</strong>
                        <p>
                          {booking.regionName}, {booking.city}
                        </p>
                      </div>
                      <div>
                        <span>Price</span>
                        <strong>{formatPrice(booking.price)}</strong>
                      </div>
                    </div>
                  </div>

                  <footer className="booking-card-actions">
                    <Link to={`/user/sessions/${booking.sessionId}`}>
                      View session
                    </Link>

                    {canCancel && booking.googleMapsUrl && (
                      <a
                        href={booking.googleMapsUrl}
                        target="_blank"
                        rel="noreferrer"
                      >
                        Open location
                      </a>
                    )}

                    {canCancel && !isAwaitingConfirmation && (
                      <button
                        className="booking-cancel-button"
                        type="button"
                        onClick={() =>
                          setConfirmationSessionId(booking.sessionId)
                        }
                        disabled={cancellingSessionId !== null}
                      >
                        Cancel booking
                      </button>
                    )}

                    {canCancel && isAwaitingConfirmation && (
                      <div className="booking-cancel-confirmation">
                        <p>Cancel your place in this session?</p>
                        <div>
                          <button
                            className="confirm-cancellation-button"
                            type="button"
                            onClick={() =>
                              void confirmCancellation(booking.sessionId)
                            }
                            disabled={isCancelling}
                          >
                            {isCancelling ? 'Cancelling...' : 'Yes, cancel'}
                          </button>
                          <button
                            type="button"
                            onClick={() => setConfirmationSessionId(null)}
                            disabled={isCancelling}
                          >
                            Keep booking
                          </button>
                        </div>
                      </div>
                    )}
                  </footer>
                </article>
              )
            })
          )}
        </section>

        {bookingPage && bookingPage.totalPages > 1 && (
          <nav className="session-pagination" aria-label="Booking pages">
            <button
              type="button"
              onClick={() => setPageNumber((current) => current - 1)}
              disabled={bookingPage.first || isLoading}
            >
              Previous
            </button>
            <span>
              Page {bookingPage.page + 1} of {bookingPage.totalPages}
            </span>
            <button
              type="button"
              onClick={() => setPageNumber((current) => current + 1)}
              disabled={bookingPage.last || isLoading}
            >
              Next
            </button>
          </nav>
        )}
      </main>
    </>
  )
}

export default MyBookingsPage
