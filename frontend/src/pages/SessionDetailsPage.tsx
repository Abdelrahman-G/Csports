import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useAuth } from '../auth/authContext'
import {
  bookSession,
  getConfirmedBookingForSession,
} from '../booking/bookingApi'
import type { BookedSession } from '../booking/types'
import { getSessionDetails } from '../discovery/discoveryApi'
import { orderedDayLabels } from '../discovery/sessionFormatting'
import type { TrainingSessionDetails } from '../discovery/types'
import UserNavigation from '../components/UserNavigation'

const longDateFormatter = new Intl.DateTimeFormat('en-EG', {
  weekday: 'long',
  day: 'numeric',
  month: 'long',
  year: 'numeric',
})

const timeFormatter = new Intl.DateTimeFormat('en-EG', {
  hour: 'numeric',
  minute: '2-digit',
})

function formatPrice(price: number) {
  return price === 0 ? 'Free' : `${price.toLocaleString('en-EG')} EGP`
}

function SessionDetailsPage() {
  const { authenticatedFetch } = useAuth()
  const { sessionId } = useParams()
  const numericSessionId = Number(sessionId)
  const hasValidSessionId =
    Number.isInteger(numericSessionId) && numericSessionId > 0
  const [session, setSession] = useState<TrainingSessionDetails | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')
  const [booking, setBooking] = useState<BookedSession | null>(null)
  const [isLoadingBooking, setIsLoadingBooking] = useState(true)
  const [bookingLookupError, setBookingLookupError] = useState('')
  const [isBooking, setIsBooking] = useState(false)
  const [bookingActionError, setBookingActionError] = useState('')

  useEffect(() => {
    if (!hasValidSessionId) {
      return
    }

    const controller = new AbortController()

    async function loadSession() {
      setIsLoading(true)
      setError('')

      try {
        const loadedSession = await getSessionDetails(
          numericSessionId,
          controller.signal,
        )
        setSession(loadedSession)
      } catch (requestError) {
        if (!controller.signal.aborted) {
          setError(
            requestError instanceof Error
              ? requestError.message
              : 'Session details could not be loaded.',
          )
        }
      } finally {
        if (!controller.signal.aborted) {
          setIsLoading(false)
        }
      }
    }

    void loadSession()
    return () => controller.abort()
  }, [hasValidSessionId, numericSessionId])

  useEffect(() => {
    if (!hasValidSessionId) {
      return
    }

    const controller = new AbortController()

    async function loadBooking() {
      setIsLoadingBooking(true)
      setBooking(null)
      setBookingLookupError('')
      setBookingActionError('')

      try {
        const loadedBooking = await getConfirmedBookingForSession(
          authenticatedFetch,
          numericSessionId,
          controller.signal,
        )
        setBooking(loadedBooking)
      } catch (requestError) {
        if (!controller.signal.aborted) {
          setBooking(null)
          setBookingLookupError(
            requestError instanceof Error
              ? requestError.message
              : 'Your booking status could not be loaded.',
          )
        }
      } finally {
        if (!controller.signal.aborted) {
          setIsLoadingBooking(false)
        }
      }
    }

    void loadBooking()
    return () => controller.abort()
  }, [authenticatedFetch, hasValidSessionId, numericSessionId])

  if (!hasValidSessionId) {
    return (
      <main className="session-details-page">
        <section className="session-details-state">
          <h1>We could not open this session.</h1>
          <p>This session address is invalid.</p>
          <Link to="/user/home">Back to session discovery</Link>
        </section>
      </main>
    )
  }

  if (isLoading) {
    return (
      <main className="session-details-page">
        <p className="session-details-state">Loading session details...</p>
      </main>
    )
  }

  if (error || !session) {
    return (
      <main className="session-details-page">
        <section className="session-details-state">
          <h1>We could not open this session.</h1>
          <p>{error}</p>
          <Link to="/user/home">Back to session discovery</Link>
        </section>
      </main>
    )
  }

  async function reservePlace() {
    if (
      !session ||
      booking ||
      isBooking ||
      isLoadingBooking ||
      bookingLookupError ||
      !session.bookingOpen
    ) {
      return
    }

    setIsBooking(true)
    setBookingActionError('')

    try {
      const confirmedBooking = await bookSession(
        authenticatedFetch,
        numericSessionId,
      )

      setBooking(confirmedBooking)
      setSession((currentSession) => {
        if (!currentSession) {
          return currentSession
        }

        return {
          ...currentSession,
          currentParticipants: confirmedBooking.currentParticipants,
          remainingSeats: confirmedBooking.remainingSeats,
          bookingOpen: confirmedBooking.bookingOpen,
        }
      })
    } catch (requestError) {
      setBookingActionError(
        requestError instanceof Error
          ? requestError.message
          : 'This session could not be booked.',
      )
    } finally {
      setIsBooking(false)
    }
  }

  const firstTraining = new Date(session.bookingClosesAt)
  const trainingDays = orderedDayLabels(session.days)
  const availability = session.bookingOpen
    ? `${session.remainingSeats} places left`
    : session.remainingSeats === 0
      ? 'Fully booked'
      : 'Booking closed'
  const bookingStatusClassName = booking
    ? 'session-booking-status confirmed'
    : bookingLookupError
      ? 'session-booking-status error'
      : 'session-booking-status'
  const bookingButtonLabel = isLoadingBooking
    ? 'Checking booking...'
    : booking
      ? 'Booked'
      : isBooking
        ? 'Booking...'
        : session.remainingSeats === 0
          ? 'Fully booked'
          : !session.bookingOpen
            ? 'Booking closed'
            : 'Reserve my place'
  const bookingButtonDisabled =
    isLoadingBooking ||
    isBooking ||
    booking !== null ||
    bookingLookupError !== '' ||
    !session.bookingOpen

  return (
    <>
      <UserNavigation />
      <main className="session-details-page">
      <Link className="session-details-back" to="/user/home">
        Back to sessions
      </Link>

      <section className="session-details-hero">
        <div>
          <span className="session-sport">{session.sport}</span>
          <h1>{session.title}</h1>
          <p>Coached by {session.trainerName}</p>
        </div>
        <span
          className={
            session.bookingOpen
              ? 'session-availability open'
              : 'session-availability closed'
          }
        >
          {availability}
        </span>
      </section>

      <div className="session-details-layout">
        <section className="session-details-main">
          <div className="details-section">
            <p className="details-label">About this training</p>
            <p>{session.description || 'The trainer has not added a description yet.'}</p>
          </div>

          <div className="details-section">
            <p className="details-label">Your coach</p>
            <h2>{session.trainerName}</h2>
            <p>
              {session.trainerExperienceYears !== null
                ? `${session.trainerExperienceYears} years of coaching experience.`
                : 'Coaching experience has not been provided.'}
            </p>
            <p>{session.trainerBio || 'This trainer has not added a coaching story yet.'}</p>
          </div>
        </section>

        <aside className="session-booking-summary">
          <div className="details-first-session">
            <span>First session</span>
            <strong>{longDateFormatter.format(firstTraining)}</strong>
            <p>{timeFormatter.format(firstTraining)}</p>
          </div>

          <dl>
            <div>
              <dt>Training days</dt>
              <dd>{trainingDays.join(', ')}</dd>
            </div>
            <div>
              <dt>Duration</dt>
              <dd>{session.durationMinutes} minutes</dd>
            </div>
            <div>
              <dt>Runs until</dt>
              <dd>{longDateFormatter.format(new Date(`${session.endDate}T12:00:00`))}</dd>
            </div>
            <div>
              <dt>Location</dt>
              <dd>
                {session.locationName}, {session.regionName}
              </dd>
            </div>
          </dl>

          <div className="session-detail-price">
            <span>Price</span>
            <strong>{formatPrice(session.price)}</strong>
          </div>

          <div className={bookingStatusClassName} aria-live="polite">
            {isLoadingBooking
              ? 'Checking your booking...'
              : booking
                ? 'Your place is confirmed.'
                : bookingLookupError || 'You have not booked this session yet.'}
          </div>

          <button
            className="session-booking-button"
            type="button"
            onClick={reservePlace}
            disabled={bookingButtonDisabled}
          >
            {bookingButtonLabel}
          </button>

          {bookingActionError && (
            <p className="session-booking-action-error" role="alert">
              {bookingActionError}
            </p>
          )}

          {booking?.googleMapsUrl && (
            <div className="session-booked-location">
              <span>Exact location</span>
              <a
                href={booking.googleMapsUrl}
                target="_blank"
                rel="noreferrer"
              >
                Open in Google Maps
              </a>
            </div>
          )}
        </aside>
      </div>
      </main>
    </>
  )
}

export default SessionDetailsPage
