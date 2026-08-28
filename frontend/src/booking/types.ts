export type BookingStatus =
  | 'CONFIRMED'
  | 'CANCELLED_BY_USER'
  | 'CANCELLED_BY_TRAINER'

export type SessionStatus = 'SCHEDULED' | 'CANCELLED' | 'COMPLETED'

export type BookedSession = {
  bookingId: number
  bookingStatus: BookingStatus
  bookedAt: string
  cancelledAt: string | null
  sessionId: number
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
  googleMapsUrl: string
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
  sessionStatus: SessionStatus
  sessionCancellationReason: string | null
}

export type BookingPage = {
  content: BookedSession[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
}
