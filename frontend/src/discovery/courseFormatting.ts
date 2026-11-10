const DAY_ORDER = [
  'MONDAY',
  'TUESDAY',
  'WEDNESDAY',
  'THURSDAY',
  'FRIDAY',
  'SATURDAY',
  'SUNDAY',
]

const DAY_LABELS: Record<string, string> = {
  MONDAY: 'Monday',
  TUESDAY: 'Tuesday',
  WEDNESDAY: 'Wednesday',
  THURSDAY: 'Thursday',
  FRIDAY: 'Friday',
  SATURDAY: 'Saturday',
  SUNDAY: 'Sunday',
}

export function orderedDayLabels(days: string[]): string[] {
  return [...days]
    .sort((first, second) => DAY_ORDER.indexOf(first) - DAY_ORDER.indexOf(second))
    .map((day) => DAY_LABELS[day] ?? day)
}
