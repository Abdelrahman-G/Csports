import { ApiError } from '../auth/authApi'
import type { AuthenticatedFetch } from '../auth/authContext'
import type { ApiErrorResponse } from '../auth/types'
import type { BookedCourse, BookingPage, BookingSearch } from './types'

async function readBookingJson<T>(
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

export async function getConfirmedBookingForCourse(
  authenticatedFetch: AuthenticatedFetch,
  courseId: number,
  signal?: AbortSignal,
): Promise<BookedCourse | null> {
  const parameters = new URLSearchParams({
    view: 'ALL',
    status: 'CONFIRMED',
    courseId: String(courseId),
    page: '0',
    size: '1',
  })

  const response = await authenticatedFetch(
    `/api/v1/bookings/me?${parameters}`,
    { signal },
  )
  const bookingPage = await readBookingJson<BookingPage>(
    response,
    'Your booking status could not be loaded.',
  )

  return (
    bookingPage.content.find(
      (booking) =>
        booking.courseId === courseId &&
        booking.bookingStatus === 'CONFIRMED',
    ) ?? null
  )
}

export async function bookCourse(
  authenticatedFetch: AuthenticatedFetch,
  courseId: number,
): Promise<BookedCourse> {
  const response = await authenticatedFetch(`/api/v1/bookings/${courseId}`, {
    method: 'POST',
  })

  const booking = await readBookingJson<BookedCourse>(
    response,
    'This course could not be booked.',
  )

  if (
    booking.courseId !== courseId ||
    booking.bookingStatus !== 'CONFIRMED'
  ) {
    throw new Error('The server returned an unexpected booking response.')
  }

  return booking
}

export async function getMyBookings(
  authenticatedFetch: AuthenticatedFetch,
  search: BookingSearch,
  signal?: AbortSignal,
): Promise<BookingPage> {
  const parameters = new URLSearchParams({
    view: search.view,
    page: String(search.page),
    size: String(search.size),
  })

  const response = await authenticatedFetch(
    `/api/v1/bookings/me?${parameters}`,
    { signal },
  )

  return readBookingJson<BookingPage>(
    response,
    'Your bookings could not be loaded.',
  )
}

export async function cancelBooking(
  authenticatedFetch: AuthenticatedFetch,
  courseId: number,
): Promise<BookedCourse> {
  const response = await authenticatedFetch(`/api/v1/bookings/${courseId}`, {
    method: 'DELETE',
  })

  const booking = await readBookingJson<BookedCourse>(
    response,
    'This booking could not be cancelled.',
  )

  if (
    booking.courseId !== courseId ||
    booking.bookingStatus !== 'CANCELLED_BY_USER'
  ) {
    throw new Error('The server returned an unexpected cancellation response.')
  }

  return booking
}
