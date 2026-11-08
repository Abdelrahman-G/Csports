import type { AuthenticatedFetch } from '../auth/authContext'
import type { ApiErrorResponse } from '../auth/types'
import type { ScheduleResponse } from './schedule'

export async function getMySchedule(
  authenticatedFetch: AuthenticatedFetch, from: string, to: string, signal: AbortSignal,
): Promise<ScheduleResponse> {
  const query = new URLSearchParams({ from, to })
  const response = await authenticatedFetch(`/api/v1/bookings/me/schedule?${query}`, { signal })
  const body = await response.json().catch(() => null)
  if (!response.ok) {
    throw new Error((body as ApiErrorResponse | null)?.message ?? 'Your schedule could not be loaded.')
  }
  return body as ScheduleResponse
}
