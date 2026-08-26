export type Region = {
  id: number
  name: string
  city: string
  country: string
}

export type Sport = {
  id: number
  name: string
}

export type TrainingSession = {
  id: number
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
  status: string
  distanceMeters: number | null
}

export type TrainingSessionDetails = Omit<TrainingSession, 'distanceMeters'> & {
  description: string | null
  trainerBio: string | null
  trainerExperienceYears: number | null
  cancelledAt: string | null
  lastUpdateReason: string | null
  cancellationReason: string | null
}

export type SessionPage = {
  content: TrainingSession[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
}

export type NearbyCoordinates = {
  latitude: number
  longitude: number
  accuracy: number
}
