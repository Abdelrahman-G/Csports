export const SCHEDULE_TIME_ZONE = 'Africa/Cairo'

export type ScheduleEntry = {
  bookingId: number
  courseId: number
  title: string
  sport: string
  locationName: string
  startsAt: string
  endsAt: string
}

export type ScheduleResponse = {
  from: string
  to: string
  timeZone: string
  serverNow: string
  entries: ScheduleEntry[]
}

const cairoDate = new Intl.DateTimeFormat('en-CA', {
  timeZone: SCHEDULE_TIME_ZONE, year: 'numeric', month: '2-digit', day: '2-digit',
})

export function dateInCairo(date: Date): string {
  const parts = cairoDate.formatToParts(date)
  return ['year', 'month', 'day'].map((type) => parts.find((part) => part.type === type)!.value).join('-')
}

export function addDays(date: string, days: number): string {
  const value = new Date(`${date}T12:00:00Z`)
  value.setUTCDate(value.getUTCDate() + days)
  return value.toISOString().slice(0, 10)
}

export function startOfWeek(date: string): string {
  const weekday = new Date(`${date}T12:00:00Z`).getUTCDay()
  return addDays(date, -((weekday + 1) % 7))
}

export function entriesOnDay(entries: ScheduleEntry[], day: string): ScheduleEntry[] {
  return entries.filter((entry) => dateInCairo(new Date(entry.startsAt)) <= day
    && dateInCairo(new Date(Date.parse(entry.endsAt) - 1)) >= day)
}

export function entryStatus(entry: ScheduleEntry, now: number): 'Upcoming' | 'In progress' | 'Past' {
  if (Date.parse(entry.endsAt) <= now) return 'Past'
  if (Date.parse(entry.startsAt) <= now) return 'In progress'
  return 'Upcoming'
}

export function hasOverlap(entry: ScheduleEntry, entries: ScheduleEntry[]): boolean {
  return entries.some((other) => other !== entry
    && Date.parse(entry.startsAt) < Date.parse(other.endsAt)
    && Date.parse(other.startsAt) < Date.parse(entry.endsAt))
}

const cairoClock = new Intl.DateTimeFormat('en-GB', { timeZone: SCHEDULE_TIME_ZONE, hour: '2-digit', minute: '2-digit', hourCycle: 'h23' })

export function minutesInCairo(date: Date): number {
  const parts = cairoClock.formatToParts(date)
  return Number(parts.find((part) => part.type === 'hour')!.value) * 60
    + Number(parts.find((part) => part.type === 'minute')!.value)
}

export function eventMinutesOnDay(entry: ScheduleEntry, day: string): { start: number; end: number } {
  const startsAt = new Date(entry.startsAt)
  const endsAt = new Date(entry.endsAt)
  const start = dateInCairo(startsAt) < day ? 0 : minutesInCairo(startsAt)
  const end = dateInCairo(endsAt) > day ? 1440 : minutesInCairo(endsAt)
  // Repeated DST clock times still need a positive visual interval; labels retain the real times.
  return { start, end: Math.max(start + 1, end) }
}
