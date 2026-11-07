import assert from 'node:assert/strict'
import { test } from 'node:test'
import {
  addDays, dateInCairo, startOfWeek, entriesOnDay, entryStatus, hasOverlap,
  eventMinutesOnDay, minutesInCairo, type ScheduleEntry,
} from '../src/schedule/schedule.ts'

const entry: ScheduleEntry = {
  bookingId: 1, courseId: 1, title: 'Football', sport: 'Football', trainerName: 'Coach',
  locationName: 'Club', regionName: 'Cairo', startsAt: '2026-11-06T23:00:00+02:00', endsAt: '2026-11-07T01:00:00+02:00',
}

test('calendar positions use Cairo wall time and clip overnight training to each day', () => {
  assert.equal(minutesInCairo(new Date('2026-11-07T16:30:00Z')), 18 * 60 + 30)
  assert.deepEqual(eventMinutesOnDay(entry, '2026-11-06'), { start: 1380, end: 1440 })
  assert.deepEqual(eventMinutesOnDay(entry, '2026-11-07'), { start: 0, end: 60 })
  const repeatedHour = { ...entry, startsAt: '2026-10-29T23:45:00+03:00', endsAt: '2026-10-29T23:15:00+02:00' }
  assert.deepEqual(eventMinutesOnDay(repeatedHour, '2026-10-29'), { start: 1425, end: 1426 })
})

test('week dates use Saturday and remain independent of the computer time zone', () => {
  assert.equal(startOfWeek('2026-11-07'), '2026-11-07')
  assert.equal(startOfWeek('2026-11-13'), '2026-11-07')
  assert.equal(addDays('2026-12-28', 7), '2027-01-04')
  assert.equal(dateInCairo(new Date('2026-11-06T23:30:00Z')), '2026-11-07')
})

test('overnight entries appear on both days, but an exclusive midnight end does not spill', () => {
  assert.equal(entriesOnDay([entry], '2026-11-06').length, 1)
  assert.equal(entriesOnDay([entry], '2026-11-07').length, 1)
  assert.equal(entriesOnDay([{ ...entry, endsAt: '2026-11-07T00:00:00+02:00' }], '2026-11-07').length, 0)
})

test('status advances only at actual start and end instants', () => {
  assert.equal(entryStatus(entry, Date.parse(entry.startsAt) - 1), 'Upcoming')
  assert.equal(entryStatus(entry, Date.parse(entry.startsAt)), 'In progress')
  assert.equal(entryStatus(entry, Date.parse(entry.endsAt)), 'Past')
})

test('overlap detection includes partial overlaps and permits back-to-back training', () => {
  const later = { ...entry, bookingId: 2, startsAt: '2026-11-07T00:30:00+02:00', endsAt: '2026-11-07T02:00:00+02:00' }
  assert.equal(hasOverlap(entry, [entry]), false)
  assert.equal(hasOverlap(entry, [entry, later]), true)
  assert.equal(hasOverlap(entry, [entry, { ...later, startsAt: entry.endsAt }]), false)
})
