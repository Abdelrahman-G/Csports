import AppNavigation from '../components/AppNavigation'
import BackLink from '../components/BackLink'
import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { useAuth } from '../auth/authContext'
import {
  bookCourse,
  getConfirmedBookingForCourse,
} from '../booking/bookingApi'
import type { BookedCourse } from '../booking/types'
import { getCourseDetails } from '../discovery/discoveryApi'
import { orderedDayLabels } from '../discovery/courseFormatting'
import type { TrainingCourseDetails } from '../discovery/types'
import CourseNoticeboard from '../announcements/CourseNoticeboard'

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

function CourseDetailsPage() {
  const { authenticatedFetch } = useAuth()
  const { courseId } = useParams()
  const numericCourseId = Number(courseId)
  const hasValidCourseId =
    Number.isInteger(numericCourseId) && numericCourseId > 0
  const [course, setCourse] = useState<TrainingCourseDetails | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')
  const [booking, setBooking] = useState<BookedCourse | null>(null)
  const [isLoadingBooking, setIsLoadingBooking] = useState(true)
  const [bookingLookupError, setBookingLookupError] = useState('')
  const [isBooking, setIsBooking] = useState(false)
  const [bookingActionError, setBookingActionError] = useState('')

  useEffect(() => {
    if (!hasValidCourseId) {
      return
    }

    const controller = new AbortController()

    async function loadCourse() {
      setIsLoading(true)
      setError('')

      try {
        const loadedCourse = await getCourseDetails(
          numericCourseId,
          controller.signal,
        )
        setCourse(loadedCourse)
      } catch (requestError) {
        if (!controller.signal.aborted) {
          setError(
            requestError instanceof Error
              ? requestError.message
              : 'Course details could not be loaded.',
          )
        }
      } finally {
        if (!controller.signal.aborted) {
          setIsLoading(false)
        }
      }
    }

    void loadCourse()
    return () => controller.abort()
  }, [hasValidCourseId, numericCourseId])

  useEffect(() => {
    if (!hasValidCourseId) {
      return
    }

    const controller = new AbortController()

    async function loadBooking() {
      setIsLoadingBooking(true)
      setBooking(null)
      setBookingLookupError('')
      setBookingActionError('')

      try {
        const loadedBooking = await getConfirmedBookingForCourse(
          authenticatedFetch,
          numericCourseId,
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
  }, [authenticatedFetch, hasValidCourseId, numericCourseId])

  if (!hasValidCourseId) {
    return (
      <><AppNavigation />
      <main className="course-details-page">
        <section className="course-details-state">
          <h1>We could not open this course.</h1>
          <p>This course address is invalid.</p>
          <BackLink to="/user/home">Back to courses</BackLink>
        </section>
      </main></>
    )
  }

  if (isLoading) {
    return (
      <><AppNavigation />
      <main className="course-details-page">
        <p className="course-details-state">Loading course details...</p>
      </main></>
    )
  }

  if (error || !course) {
    return (
      <><AppNavigation />
      <main className="course-details-page">
        <section className="course-details-state">
          <h1>We could not open this course.</h1>
          <p>{error}</p>
          <BackLink to="/user/home">Back to courses</BackLink>
        </section>
      </main></>
    )
  }

  async function reservePlace() {
    if (
      !course ||
      booking ||
      isBooking ||
      isLoadingBooking ||
      bookingLookupError ||
      !course.bookingOpen
    ) {
      return
    }

    setIsBooking(true)
    setBookingActionError('')

    try {
      const confirmedBooking = await bookCourse(
        authenticatedFetch,
        numericCourseId,
      )

      setBooking(confirmedBooking)
      setCourse((currentCourse) => {
        if (!currentCourse) {
          return currentCourse
        }

        return {
          ...currentCourse,
          currentParticipants: confirmedBooking.currentParticipants,
          remainingSeats: confirmedBooking.remainingSeats,
          bookingOpen: confirmedBooking.bookingOpen,
        }
      })
    } catch (requestError) {
      setBookingActionError(
        requestError instanceof Error
          ? requestError.message
          : 'This course could not be booked.',
      )
    } finally {
      setIsBooking(false)
    }
  }

  const firstTraining = new Date(course.bookingClosesAt)
  const trainingDays = orderedDayLabels(course.days)
  const isCancelled = course.status === 'CANCELLED'
  const availability = isCancelled
    ? 'Course cancelled'
    : course.bookingOpen
      ? `${course.remainingSeats} places left`
      : course.remainingSeats === 0
        ? 'Fully booked'
        : 'Booking closed'
  const bookingStatusClassName = isCancelled
    ? 'course-booking-status cancelled'
    : booking
      ? 'course-booking-status confirmed'
      : bookingLookupError
        ? 'course-booking-status error'
        : 'course-booking-status'
  const bookingButtonLabel = isCancelled
    ? 'Course cancelled'
    : isLoadingBooking
      ? 'Checking booking...'
      : booking
        ? 'Booked'
        : isBooking
          ? 'Booking...'
          : course.remainingSeats === 0
            ? 'Fully booked'
            : !course.bookingOpen
              ? 'Booking closed'
              : 'Reserve my place'
  const bookingButtonDisabled =
    isLoadingBooking ||
    isBooking ||
    booking !== null ||
    bookingLookupError !== '' ||
    !course.bookingOpen

  return (
    <>
      <AppNavigation />
      <main className="course-details-page">
      <BackLink to="/user/home">
        Back to courses
      </BackLink>

      <section className="course-details-hero">
        <div>
          <span className="course-sport">{course.sport}</span>
          <h1>{course.title}</h1>
          <p>Coached by {course.trainerName}</p>
        </div>
        <span
          className={
            course.bookingOpen
              ? 'course-availability open'
              : 'course-availability closed'
          }
        >
          {availability}
        </span>
      </section>

      {isCancelled && (
        <section className="course-cancellation-notice" role="status">
          <strong>This course was cancelled by its trainer.</strong>
          <p>
            Reason: {course.cancellationReason || 'No reason was provided.'}
          </p>
        </section>
      )}

      <div className="course-details-layout">
        <section className="course-details-main">
          <div className="details-section">
            <p className="details-label">About this training</p>
            <p>{course.description || 'The trainer has not added a description yet.'}</p>
          </div>

          <div className="details-section">
            <p className="details-label">Your coach</p>
            <h2>{course.trainerName}</h2>
            <p>
              {course.trainerExperienceYears !== null
                ? `${course.trainerExperienceYears} years of coaching experience.`
                : 'Coaching experience has not been provided.'}
            </p>
            <p>{course.trainerBio || 'This trainer has not added a coaching story yet.'}</p>
          </div>
          <CourseNoticeboard key={`${course.id}-${booking?.bookingId ?? 'visitor'}`} courseId={course.id} />
        </section>

        <aside className="course-booking-summary">
          <div className="course-capacity">
            <span>{course.remainingSeats} of {course.maxParticipants} places available</span>
            <div className="capacity-track" aria-hidden="true">
              <span style={{ width: `${course.currentParticipants / course.maxParticipants * 100}%` }} />
            </div>
            <meter className="motion-sr-only" min={0} max={course.maxParticipants} value={course.currentParticipants}
              aria-label="Booked places" />
          </div>
          <div className="details-first-course">
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
              <dd>{course.durationMinutes} minutes</dd>
            </div>
            <div>
              <dt>Runs until</dt>
              <dd>{longDateFormatter.format(new Date(`${course.endDate}T12:00:00`))}</dd>
            </div>
            <div>
              <dt>Location</dt>
              <dd>
                {course.locationName}, {course.regionName}
              </dd>
            </div>
          </dl>

          <div className="course-detail-price">
            <span>Price</span>
            <strong>{formatPrice(course.price)}</strong>
          </div>

          <div className={bookingStatusClassName} aria-live="polite">
            {booking && !isCancelled && <span className="booking-check" aria-hidden="true">✓</span>}
            {isCancelled
              ? 'This course is no longer available for booking.'
              : isLoadingBooking
              ? 'Checking your booking...'
              : booking
                ? 'Your place is confirmed.'
                : bookingLookupError || 'You have not booked this course yet.'}
          </div>

          <button
            className="course-booking-button"
            type="button"
            onClick={reservePlace}
            disabled={bookingButtonDisabled}
          >
            {isBooking && <span className="booking-spinner" aria-hidden="true" />}
            {bookingButtonLabel}
          </button>

          {bookingActionError && (
            <p className="course-booking-action-error" role="alert">
              {bookingActionError}
            </p>
          )}

          {booking?.googleMapsUrl && (
            <div className="course-booked-location">
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

export default CourseDetailsPage
