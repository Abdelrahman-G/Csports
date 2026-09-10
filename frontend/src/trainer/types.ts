import type { Region, SessionPage, TrainingSession } from '../discovery/types'

export type { Region, SessionPage, TrainingSession }

export type ResolvedGoogleMapsLocation = {
  latitude: number
  longitude: number
  normalizedGoogleMapsUrl: string
}

export type VerifiedVenueLocation = ResolvedGoogleMapsLocation & {
  locationName: string
}

export type CreateTrainingSessionRequest = {
  title: string
  description: string
  locationName: string
  regionId: number
  latitude: number
  longitude: number
  startDate: string
  endDate: string
  startTime: string
  durationMinutes: number
  days: string[]
  maxParticipants: number
  price: number
}

export type UpdateTrainingSessionRequest = Partial<
  Omit<CreateTrainingSessionRequest, 'price'>
> & {
  reason: string
}

export type SessionParticipant = {
  userId: number
  name: string
  email: string
  phoneNumber: string
}

export type ParticipantPage = {
  content: SessionParticipant[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
}
