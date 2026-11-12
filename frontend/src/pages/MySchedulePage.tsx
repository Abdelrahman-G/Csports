import AppNavigation from '../components/AppNavigation'
import { useEffect, useState, type CSSProperties } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../auth/authContext'
import { getMySchedule } from '../schedule/scheduleApi'
import { addDays, dateInCairo, entriesOnDay, entryStatus, hasOverlap, startOfWeek, minutesInCairo, eventMinutesOnDay, SCHEDULE_TIME_ZONE, type ScheduleResponse } from '../schedule/schedule'
import '../schedule/schedule.css'

const monthLabel = new Intl.DateTimeFormat('en-GB', { month: 'long', year: 'numeric', timeZone: 'UTC' })
const shortDate = new Intl.DateTimeFormat('en-GB', { day: 'numeric', month: 'short', timeZone: 'UTC' })
const weekday = new Intl.DateTimeFormat('en-GB', { weekday: 'short', timeZone: 'UTC' })
const clockTime = new Intl.DateTimeFormat('en-GB', { hour: '2-digit', minute: '2-digit', timeZone: SCHEDULE_TIME_ZONE })
const dateValue = (day: string) => new Date(`${day}T12:00:00Z`)

function Chevron({ forward = false }: { forward?: boolean }) {
  return <svg viewBox="0 0 24 24" aria-hidden="true"><path d={forward ? 'm9 6 6 6-6 6' : 'm15 6-6 6 6 6'} /></svg>
}

export default function MySchedulePage() {
  const { authenticatedFetch } = useAuth()
  const [week, setWeek] = useState(() => startOfWeek(dateInCairo(new Date())))
  const [schedule, setSchedule] = useState<ScheduleResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [refresh, setRefresh] = useState(0)
  const [now, setNow] = useState(Date.now)
  const [clockOffset, setClockOffset] = useState(0)

  useEffect(() => {
    const controller = new AbortController()
    async function loadSchedule() {
      setLoading(true)
      setError('')
      try {
        const response = await getMySchedule(authenticatedFetch, week, addDays(week, 7), controller.signal)
        if (controller.signal.aborted) return
        setSchedule(response)
        setClockOffset(Date.parse(response.serverNow) - Date.now())
        setNow(Date.now())
      } catch (requestError) {
        if (!controller.signal.aborted) {
          setError(requestError instanceof Error ? requestError.message : 'Your schedule could not be loaded.')
        }
      } finally {
        if (!controller.signal.aborted) setLoading(false)
      }
    }
    void loadSchedule()
    return () => controller.abort()
  }, [authenticatedFetch, week, refresh])

  useEffect(() => {
    const timer = window.setInterval(() => setNow(Date.now()), 30_000)
    function refreshOnReturn() {
      if (!document.hidden) setRefresh((value) => value + 1)
    }
    document.addEventListener('visibilitychange', refreshOnReturn)
    return () => {
      window.clearInterval(timer)
      document.removeEventListener('visibilitychange', refreshOnReturn)
    }
  }, [])


  const currentTime = now + clockOffset
  const today = dateInCairo(new Date(currentTime))
  const days = Array.from({ length: 7 }, (_, index) => addDays(week, index))
  const entries = schedule?.from === week ? schedule.entries : []
  const columns = days.map((day) => ({ day, events: entriesOnDay(entries, day).map((entry) => ({ entry, ...eventMinutesOnDay(entry, day) })) }))
  const bounds = columns.flatMap(({ events }) => events)
  const firstHour = bounds.length ? Math.max(0, Math.floor(Math.min(...bounds.map((event) => event.start)) / 60) - 1) : 9
  const lastHour = bounds.length ? Math.min(24, Math.max(firstHour + 6, Math.ceil(Math.max(...bounds.map((event) => event.end)) / 60) + 1)) : 18
  const hours = Array.from({ length: lastHour - firstHour }, (_, index) => firstHour + index)
  const nowMinute = minutesInCairo(new Date(currentTime))
  const sports = [...new Set(entries.map((entry) => entry.sport))]

  return (
    <>
      <AppNavigation />
      <main className="schedule-page">
        <header className="schedule-heading">
          <h1>My schedule</h1>
        </header>
        <section className="schedule-calendar" aria-label="My schedule">
          <header className="schedule-toolbar">
            <div className="schedule-period">
              <h2>{monthLabel.format(dateValue(week))}</h2>
              <div className="schedule-week-controls">
                <button type="button" aria-label="Previous week" onClick={() => setWeek(addDays(week, -7))}><Chevron /></button>
                <span aria-live="polite">{shortDate.format(dateValue(week))} – {shortDate.format(dateValue(addDays(week, 6)))}</span>
                <button type="button" aria-label="Next week" onClick={() => setWeek(addDays(week, 7))}><Chevron forward /></button>
              </div>
            </div>
            <button type="button" className="schedule-current-week" onClick={() => setWeek(startOfWeek(today))}>This week</button>
          </header>
          {error ? <div className="schedule-message" role="alert"><p>{error}</p><button type="button" onClick={() => setRefresh((value) => value + 1)}>Try again</button></div>
            : <div className="schedule-scroll" role="region" aria-label="Weekly calendar, times in Cairo" tabIndex={0} aria-busy={loading}>
              <div className="schedule-sheet" key={week} style={{ '--hours': hours.length } as CSSProperties}>
                <div className="schedule-day-heading schedule-gutter"><span aria-hidden="true">24h</span></div>
                {columns.map(({ day, events }) => <div key={day} className={`schedule-day-heading${day === today ? ' is-today' : ''}`}>
                  <span>{weekday.format(dateValue(day))}</span>
                  <strong aria-current={day === today ? 'date' : undefined}>{Number(day.slice(-2))}</strong>
                  <div className="schedule-day-dots" aria-hidden="true">{events.slice(0, 4).map(({ entry }) => <i key={`${entry.bookingId}-${entry.startsAt}`} data-sport={entry.sport.toLowerCase()} />)}</div>
                </div>)}
                <div className="schedule-hour-labels" aria-hidden="true">{hours.map((hour) => <span key={hour}>{String(hour).padStart(2, '0')}:00</span>)}</div>
                {columns.map(({ day, events }) => <div key={day} className={`schedule-day-track${day === today ? ' is-today' : ''}`} style={{ gridTemplateRows: `repeat(${hours.length * 60}, 1.2px)` }}>
                  {!loading && events.map(({ entry, start, end }) => {
                    const status = entryStatus(entry, currentTime)
                    const overlaps = hasOverlap(entry, entries)
                    const continues = dateInCairo(new Date(entry.startsAt)) < day
                    const time = `${continues ? 'Continues until ' : clockTime.format(new Date(entry.startsAt)) + '–'}${clockTime.format(new Date(entry.endsAt))}`
                    return <Link
                      key={`${entry.bookingId}-${entry.startsAt}`}
                      to={`/user/courses/${entry.courseId}`}
                      data-sport={entry.sport.toLowerCase()}
                      className={`schedule-event${status === 'Past' ? ' is-past' : ''}${status === 'In progress' ? ' is-live' : ''}`}
                      style={{ gridRow: `${Math.floor(start - firstHour * 60) + 1} / span ${Math.max(45, Math.ceil(end - start))}` }}
                      aria-label={`${entry.title}, ${shortDate.format(dateValue(day))}, ${time}${overlaps ? ', overlaps another training' : ''}`}
                      title={`${entry.title} · ${time} · ${entry.locationName}${overlaps ? ' · Overlaps another training' : ''}`}
                    >
                      <strong>{entry.title}</strong><time dateTime={entry.startsAt}>{time}</time>
                      {dateInCairo(new Date(entry.endsAt)) > day && <small>Until {shortDate.format(dateValue(dateInCairo(new Date(entry.endsAt))))}</small>}
                      {status === 'In progress' && <small className="schedule-live">Live</small>}
                    </Link>
                  })}
                  {day === today && nowMinute >= firstHour * 60 && nowMinute < lastHour * 60 && <div className="schedule-now" style={{ top: (nowMinute - firstHour * 60) * 1.2 }} aria-label="Current time" />}
                </div>)}
                {loading && <div className="schedule-loading" role="status"><span />Loading your week</div>}
                {!loading && entries.length === 0 && <div className="schedule-empty"><span aria-hidden="true">↗</span><h3>A little room to move.</h3><p>No sessions booked this week.</p><Link to="/user/home">Find your next training <span aria-hidden="true">↗</span></Link></div>}
              </div>
            </div>}
          <footer className="schedule-legend" aria-label="Sports in this week">
            <div>{sports.map((sport) => <span key={sport}><i data-sport={sport.toLowerCase()} />{sport}</span>)}</div>
            <span>{!loading && !error && `${entries.length} session${entries.length === 1 ? '' : 's'} this week`}</span>
          </footer>
        </section>
      </main>
    </>
  )
}
