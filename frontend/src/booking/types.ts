export type BookingStatus =
  | 'CONFIRMED'
  | 'CANCELLED_BY_USER'
  | 'CANCELLED_BY_TRAINER'

export type CourseStatus = 'SCHEDULED' | 'CANCELLED' | 'COMPLETED'

export type BookingView = 'UPCOMING' | 'HISTORY'

export type BookingSearch = {
  view: BookingView
  page: number
  size: number
}

export type BookedCourse = {
  bookingId: number
  bookingStatus: BookingStatus
  bookedAt: string
  cancelledAt: string | null
  courseId: number
  title: string
  trainerId: number
  trainerName: string
  sportId: number
  sport: string
  locationName: string
  regionId: number
  regionName: string
  city: string
  country: string
  googleMapsUrl: string | null
  startDate: string
  endDate: string
  startTime: string
  durationMinutes: number
  days: string[]
  price: number
  currentParticipants: number
  maxParticipants: number
  remainingSeats: number
  bookingClosesAt: string
  bookingOpen: boolean
  courseStatus: CourseStatus
  courseCancellationReason: string | null
}

export type BookingPage = {
  content: BookedCourse[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
}
