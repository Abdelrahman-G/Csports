import type { Region, CoursePage, TrainingCourse } from '../discovery/types'

export type { Region, CoursePage, TrainingCourse }

export type ResolvedGoogleMapsLocation = {
  latitude: number
  longitude: number
  normalizedGoogleMapsUrl: string
}

export type VerifiedVenueLocation = ResolvedGoogleMapsLocation & {
  locationName: string
}

export type CreateTrainingCourseRequest = {
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

export type UpdateTrainingCourseRequest = Partial<
  Omit<CreateTrainingCourseRequest, 'price'>
> & {
  reason: string
}

export type CourseParticipant = {
  userId: number
  name: string
  email: string
  phoneNumber: string
}

export type ParticipantPage = {
  content: CourseParticipant[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
}
