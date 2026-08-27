import { useEffect, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../auth/authContext'
import { getMyUserProfile, type UserProfile } from '../auth/profileApi'
import {
  getRegions,
  getSports,
  searchSessions,
  type SessionSearch,
} from '../discovery/discoveryApi'
import type {
  NearbyCoordinates,
  Region,
  SessionPage,
  Sport,
  TrainingSession,
} from '../discovery/types'
import { orderedDayLabels } from '../discovery/sessionFormatting'

const REGION_STORAGE_KEY = 'csports.discovery.regionId'
const MIN_LATITUDE = 29.75
const MAX_LATITUDE = 30.35
const MIN_LONGITUDE = 30.75
const MAX_LONGITUDE = 31.75
const NEARBY_RADIUS_KM = 50

type DiscoveryMode = 'region' | 'nearby'

type DiscoveryFilters = {
  query: string
  regionId: string
  sportId: string
}

const EMPTY_FILTERS: DiscoveryFilters = {
  query: '',
  regionId: '',
  sportId: '',
}

const sessionDateFormatter = new Intl.DateTimeFormat('en-EG', {
  weekday: 'short',
  day: 'numeric',
  month: 'short',
  year: 'numeric',
})

const sessionTimeFormatter = new Intl.DateTimeFormat('en-EG', {
  hour: 'numeric',
  minute: '2-digit',
})

function initialFilters(): DiscoveryFilters {
  return {
    ...EMPTY_FILTERS,
    regionId: localStorage.getItem(REGION_STORAGE_KEY) ?? '',
  }
}

function optionalNumber(value: string) {
  return value === '' ? undefined : Number(value)
}

function formatDistance(distanceMeters: number | null) {
  if (distanceMeters === null) {
    return null
  }

  if (distanceMeters < 1000) {
    return `${Math.round(distanceMeters)} m away`
  }

  return `${(distanceMeters / 1000).toFixed(1)} km away`
}

function SessionCard({ session }: { session: TrainingSession }) {
  const distance = formatDistance(session.distanceMeters)
  const firstTraining = new Date(session.bookingClosesAt)
  const trainingDays = orderedDayLabels(session.days)
  const availability =
    session.remainingSeats === 0
      ? 'Fully booked'
      : session.bookingOpen
        ? `${session.remainingSeats} places left`
        : 'Booking closed'

  return (
    <Link className="session-card-link" to={`/user/sessions/${session.id}`}>
      <article className="session-card">
      <div className="session-card-heading">
        <span className="session-sport">{session.sport}</span>
        <span
          className={
            session.bookingOpen
              ? 'session-availability open'
              : 'session-availability closed'
          }
        >
          {availability}
        </span>
      </div>

      <h2>{session.title}</h2>
      <p className="session-trainer">with {session.trainerName}</p>

      <div className="session-start">
        <span className="session-start-label">First session</span>
        <strong>{sessionDateFormatter.format(firstTraining)}</strong>
        <span className="session-time">
          {sessionTimeFormatter.format(firstTraining)} for{' '}
          {session.durationMinutes} minutes
        </span>
      </div>

      <div className="session-days" aria-label="Training days">
        {trainingDays.map((day) => (
          <span key={day}>{day}</span>
        ))}
      </div>

      <dl className="session-summary">
        <div>
          <dt>Location</dt>
          <dd>
            {session.locationName}, {session.regionName}
          </dd>
        </div>
      </dl>

      <div className="session-card-footer">
        {distance && <span className="session-distance">{distance}</span>}
        <span className="session-details-link">View details</span>
      </div>
      </article>
    </Link>
  )
}

function UserHomePage() {
  const { authenticatedFetch } = useAuth()
  const [profile, setProfile] = useState<UserProfile | null>(null)
  const [regions, setRegions] = useState<Region[]>([])
  const [sports, setSports] = useState<Sport[]>([])
  const [filters, setFilters] = useState<DiscoveryFilters>(initialFilters)
  const [appliedFilters, setAppliedFilters] =
    useState<DiscoveryFilters>(initialFilters)
  const [mode, setMode] = useState<DiscoveryMode>('region')
  const [coordinates, setCoordinates] = useState<NearbyCoordinates | null>(null)
  const [pageNumber, setPageNumber] = useState(0)
  const [sessionPage, setSessionPage] = useState<SessionPage | null>(null)
  const [isLoadingSessions, setIsLoadingSessions] = useState(false)
  const [isLocating, setIsLocating] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    const controller = new AbortController()

    async function loadPageData() {
      try {
        const [loadedProfile, loadedRegions, loadedSports] = await Promise.all([
          getMyUserProfile(authenticatedFetch),
          getRegions(controller.signal),
          getSports(controller.signal),
        ])

        setProfile(loadedProfile)
        setRegions(loadedRegions)
        setSports(loadedSports)

        const storedRegionId = localStorage.getItem(REGION_STORAGE_KEY)
        const storedRegionStillExists = loadedRegions.some(
          (region) => String(region.id) === storedRegionId,
        )

        if (storedRegionId && !storedRegionStillExists) {
          localStorage.removeItem(REGION_STORAGE_KEY)
          setFilters((current) => ({ ...current, regionId: '' }))
          setAppliedFilters((current) => ({ ...current, regionId: '' }))
        }
      } catch (requestError) {
        if (!controller.signal.aborted) {
          setError(
            requestError instanceof Error
              ? requestError.message
              : 'Your training space could not be loaded.',
          )
        }
      }
    }

    void loadPageData()
    return () => controller.abort()
  }, [authenticatedFetch])

  useEffect(() => {
    let search: SessionSearch

    if (mode === 'region') {
      search = {
        mode: 'region',
        query: appliedFilters.query || undefined,
        regionId: optionalNumber(appliedFilters.regionId),
        sportId: optionalNumber(appliedFilters.sportId),
      }
    } else {
      if (!coordinates) {
        return
      }
      search = {
        mode: 'nearby',
        query: appliedFilters.query || undefined,
        sportId: optionalNumber(appliedFilters.sportId),
        coordinates,
        radiusKm: NEARBY_RADIUS_KM,
      }
    }

    const controller = new AbortController()

    async function loadSessions() {
      setIsLoadingSessions(true)
      setError('')

      try {
        const loadedPage = await searchSessions(
          search,
          pageNumber,
          controller.signal,
        )
        setSessionPage(loadedPage)
      } catch (requestError) {
        if (!controller.signal.aborted) {
          setSessionPage(null)
          setError(
            requestError instanceof Error
              ? requestError.message
              : 'Sessions could not be loaded.',
          )
        }
      } finally {
        if (!controller.signal.aborted) {
          setIsLoadingSessions(false)
        }
      }
    }

    void loadSessions()

    return () => controller.abort()
  }, [appliedFilters, coordinates, mode, pageNumber])

  function updateFilter(field: keyof DiscoveryFilters, value: string) {
    setFilters((current) => ({ ...current, [field]: value }))
    setError('')
  }

  function applyFilters(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    const nextFilters = { ...filters, query: filters.query.trim() }
    setFilters(nextFilters)
    setAppliedFilters(nextFilters)
    setMode('region')
    setPageNumber(0)

    if (nextFilters.regionId) {
      localStorage.setItem(REGION_STORAGE_KEY, nextFilters.regionId)
    } else {
      localStorage.removeItem(REGION_STORAGE_KEY)
    }
  }

  function clearFilters() {
    localStorage.removeItem(REGION_STORAGE_KEY)
    setFilters(EMPTY_FILTERS)
    setAppliedFilters(EMPTY_FILTERS)
    setMode('region')
    setPageNumber(0)
    setError('')
  }

  function useCurrentLocation() {
    setError('')

    if (!navigator.geolocation) {
      setError('This browser does not support location access.')
      return
    }

    const nearbyFilters = {
      ...filters,
      query: filters.query.trim(),
      regionId: '',
    }

    setIsLocating(true)
    navigator.geolocation.getCurrentPosition(
      (position) => {
        const { latitude, longitude, accuracy } = position.coords
        const insideServiceArea =
          latitude >= MIN_LATITUDE &&
          latitude <= MAX_LATITUDE &&
          longitude >= MIN_LONGITUDE &&
          longitude <= MAX_LONGITUDE

        if (!insideServiceArea) {
          setError('Your current location is outside Cairo and Giza.')
          setIsLocating(false)
          return
        }

        localStorage.removeItem(REGION_STORAGE_KEY)
        setFilters(nearbyFilters)
        setAppliedFilters(nearbyFilters)
        setCoordinates({ latitude, longitude, accuracy })
        setMode('nearby')
        setPageNumber(0)
        setIsLocating(false)
      },
      (locationError) => {
        setError(
          locationError.code === locationError.PERMISSION_DENIED
            ? 'Location permission was denied. Allow it in your browser and try again.'
            : 'Your current location could not be detected. Please try again.',
        )
        setIsLocating(false)
      },
      {
        enableHighAccuracy: true,
        timeout: 10000,
        maximumAge: 300000,
      },
    )
  }

  const selectedRegion = regions.find(
    (region) => String(region.id) === appliedFilters.regionId,
  )
  const selectedSport = sports.find(
    (sport) => String(sport.id) === appliedFilters.sportId,
  )
  const sessions = sessionPage?.content ?? []

  return (
    <main className="discovery-page">
      <header className="discovery-header">
        <p className="eyebrow">Find your next session</p>
        <h1>{profile ? `Welcome, ${profile.name}` : 'Welcome'}</h1>
        <p>Explore coaches and sessions across Cairo and Giza.</p>
      </header>

      <form className="discovery-catalogue" onSubmit={applyFilters}>
        <div className="discovery-search-bar">
          <label htmlFor="session-search">Search sessions</label>
          <div>
            <input
              id="session-search"
              type="search"
              value={filters.query}
              onChange={(event) => updateFilter('query', event.target.value)}
              placeholder="Search by sport, session, or club"
              maxLength={100}
            />
            <button type="submit">Search</button>
          </div>
        </div>

        <div className="discovery-catalogue-layout">
          <aside className="discovery-sidebar" aria-labelledby="filter-title">
            <div className="sidebar-heading">
              <div>
                <p className="sidebar-kicker">Refine results</p>
                <h2 id="filter-title">Filters</h2>
              </div>
              <button className="clear-filters-link" type="button" onClick={clearFilters}>
                Clear
              </button>
            </div>

            <div className="sidebar-filter-group">
              <label htmlFor="region-filter">Area</label>
              <select
                id="region-filter"
                value={filters.regionId}
                onChange={(event) => updateFilter('regionId', event.target.value)}
                disabled={regions.length === 0}
              >
                <option value="">All areas</option>
                {regions.map((region) => (
                  <option key={region.id} value={region.id}>
                    {region.name}, {region.city}
                  </option>
                ))}
              </select>
            </div>

            <div className="sidebar-filter-group">
              <label htmlFor="sport-filter">Sport</label>
              <select
                id="sport-filter"
                value={filters.sportId}
                onChange={(event) => updateFilter('sportId', event.target.value)}
                disabled={sports.length === 0}
              >
                <option value="">All sports</option>
                {sports.map((sport) => (
                  <option key={sport.id} value={sport.id}>
                    {sport.name}
                  </option>
                ))}
              </select>
            </div>

            <button className="apply-filters-button" type="submit">
              Apply filters
            </button>

            <div className="nearby-filter">
              <span>Use a different location method</span>
              <button
                className={mode === 'nearby' ? 'nearby-button active' : 'nearby-button'}
                type="button"
                onClick={useCurrentLocation}
                disabled={isLocating}
              >
                {isLocating ? 'Finding you...' : 'Search near me'}
              </button>
            </div>
          </aside>

          <section className="discovery-results-column">
            <div className="results-heading">
              <div>
                <p className="active-search-label">
                  {mode === 'nearby' && coordinates
                    ? 'Nearest sessions'
                    : selectedRegion || selectedSport || appliedFilters.query
                      ? `${selectedSport?.name ?? 'Sessions'}${selectedRegion ? ` in ${selectedRegion.name}` : ''}`
                      : 'All upcoming sessions'}
                </p>
                {appliedFilters.query && (
                  <span>Matching "{appliedFilters.query}"</span>
                )}
              </div>
              {sessionPage && <strong>{sessionPage.totalElements} results</strong>}
            </div>

            {error && (
              <p className="discovery-error" role="alert">
                {error}
              </p>
            )}

            <div className="session-results" aria-live="polite">
              {isLoadingSessions ? (
                <p className="session-empty-state">Finding available sessions...</p>
              ) : sessions.length === 0 ? (
                <p className="session-empty-state">
                  No sessions match these filters yet. Try clearing one of them.
                </p>
              ) : (
                sessions.map((session) => (
                  <SessionCard key={session.id} session={session} />
                ))
              )}
            </div>

            {sessionPage && sessionPage.totalPages > 1 && (
              <nav className="session-pagination" aria-label="Session result pages">
                <button
                  type="button"
                  onClick={() => setPageNumber((current) => current - 1)}
                  disabled={sessionPage.first || isLoadingSessions}
                >
                  Previous
                </button>
                <span>
                  Page {sessionPage.page + 1} of {sessionPage.totalPages}
                </span>
                <button
                  type="button"
                  onClick={() => setPageNumber((current) => current + 1)}
                  disabled={sessionPage.last || isLoadingSessions}
                >
                  Next
                </button>
              </nav>
            )}
          </section>
        </div>
      </form>
    </main>
  )
}

export default UserHomePage
