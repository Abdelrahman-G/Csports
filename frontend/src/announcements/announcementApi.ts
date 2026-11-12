import type { AuthenticatedFetch } from '../auth/authContext'

export type Announcement = {
  id: number
  courseId: number
  authorId: number
  authorName: string
  type: 'TRAINER_MESSAGE' | 'COURSE_UPDATED' | 'COURSE_CANCELLED'
  title: string
  content: string
  createdAt: string
}

export type Noticeboard = {
  canPublish: boolean
  announcements: { content: Announcement[]; page: number; totalPages: number; totalElements: number; first: boolean; last: boolean }
}

async function read<T>(response: Response): Promise<T> {
  const body = await response.json().catch(() => null)
  if (!response.ok) throw new Error(body?.message ?? 'The noticeboard could not be loaded.')
  return body as T
}

export async function getNoticeboard(fetch: AuthenticatedFetch, courseId: number, page: number, signal: AbortSignal): Promise<Noticeboard | null> {
  const response = await fetch(`/api/v1/courses/${courseId}/announcements?page=${page}&size=10`, { signal })
  if (response.status === 403) return null
  return read<Noticeboard>(response)
}

export async function getAnnouncement(fetch: AuthenticatedFetch, courseId: number, id: number, signal: AbortSignal) {
  return read<Announcement>(await fetch(`/api/v1/courses/${courseId}/announcements/${id}`, { signal }))
}

export async function publishAnnouncement(fetch: AuthenticatedFetch, courseId: number, title: string, content: string) {
  return read<Announcement>(await fetch(`/api/v1/courses/${courseId}/announcements`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ title: title.trim(), content: content.trim() }),
  }))
}
