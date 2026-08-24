import { useEffect, useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
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

const REGION_STORAGE_KEY = 'csports.discovery.regionId'
const MIN_LATITUDE = 29.75
const MAX_LATITUDE = 30.35
const MIN_LONGITUDE = 30.75
const MAX_LONGITUDE = 31.75
const NEARBY_RADIUS_KM = 10

type DiscoveryMode = 'region' | 'nearby'

type DiscoveryFilters = {
  query: string
  regionId: string
  sportId: string
  minPrice: string
  maxPrice: string
}

const EMPTY_FILTERS: DiscoveryFilters = {
  query: '',
  regionId: '',
  sportId: '',
  minPrice: '',
  maxPrice: '',
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
  const start = new Date(`${session.startDate}T${session.startTime}`)
  const availability =
    session.remainingSeats === 0
      ? 'Fully booked'
      : session.bookingOpen
        ? `${session.remainingSeats} places left`
        : 'Booking closed'

  return (
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

      <div className="session-schedule">
        <span className="session-date">{sessionDateFormatter.format(start)}</span>
        <span className="session-time">
          {sessionTimeFormatter.format(start)} · {session.durationMinutes} minutes
        </span>
      </div>

      <dl className="session-summary">
        <div>
          <dt>Location</dt>
          <dd>
            {session.locationName}, {session.regionName}
          </dd>
        </div>
        <div>
          <dt>Price</dt>
          <dd>{session.price} EGP</dd>
        </div>
      </dl>

      {distance && <span className="session-distance">{distance}</span>}
    </article>
  )
}

function UserHomePage() {
  const navigate = useNavigate()
  const { authenticatedFetch, logout } = useAuth()
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
  const [isLoggingOut, setIsLoggingOut] = useState(false)

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
        minPrice: optionalNumber(appliedFilters.minPrice),
        maxPrice: optionalNumber(appliedFilters.maxPrice),
      }
    } else {
      if (!coordinates) {
        return
      }
      search = {
        mode: 'nearby',
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

    const minPrice = optionalNumber(filters.minPrice)
    const maxPrice = optionalNumber(filters.maxPrice)

    if (
      (minPrice !== undefined && minPrice < 0) ||
      (maxPrice !== undefined && maxPrice < 0)
    ) {
      setError('Prices cannot be negative.')
      return
    }
    if (
      minPrice !== undefined &&
      maxPrice !== undefined &&
      minPrice > maxPrice
    ) {
      setError('The maximum price cannot be lower than the minimum price.')
      return
    }

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

  async function handleLogout() {
    setIsLoggingOut(true)

    try {
      await logout()
    } catch {
      // AuthProvider still removes the local session if the server is unavailable.
    } finally {
      navigate('/login', { replace: true })
    }
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
        <div>
          <p className="eyebrow">Find your next session</p>
          <h1>{profile ? `Welcome, ${profile.name}` : 'Welcome'}</h1>
        </div>
        <button
          className="header-logout-button"
          type="button"
          onClick={handleLogout}
          disabled={isLoggingOut}
        >
          {isLoggingOut ? 'Logging out...' : 'Log out'}
        </button>
      </header>

      <section className="discovery-filter-panel" aria-labelledby="filter-title">
        <div className="filter-panel-heading">
          <div>
            <h2 id="filter-title">Find training that fits you</h2>
            <p>Search every session, or narrow the results when you need to.</p>
          </div>
          <button
            className={mode === 'nearby' ? 'nearby-button active' : 'nearby-button'}
            type="button"
            onClick={useCurrentLocation}
            disabled={isLocating}
          >
            {isLocating ? 'Finding you...' : 'Search near me'}
          </button>
        </div>

        <form className="discovery-filter-form" onSubmit={applyFilters}>
          <label className="search-filter-field">
            <span>Search</span>
            <input
              type="search"
              value={filters.query}
              onChange={(event) => updateFilter('query', event.target.value)}
              placeholder="Try swimming, beginner, or a club name"
              maxLength={100}
            />
          </label>

          <label>
            <span>Area</span>
            <select
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
          </label>

          <label>
            <span>Sport</span>
            <select
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
          </label>

          <fieldset className="price-filter">
            <legend>Price range</legend>
            <div>
              <label>
                <span>Minimum</span>
                <input
                  type="number"
                  min="0"
                  value={filters.minPrice}
                  onChange={(event) => updateFilter('minPrice', event.target.value)}
                  placeholder="0"
                />
              </label>
              <label>
                <span>Maximum</span>
                <input
                  type="number"
                  min="0"
                  value={filters.maxPrice}
                  onChange={(event) => updateFilter('maxPrice', event.target.value)}
                  placeholder="Any"
                />
              </label>
            </div>
          </fieldset>

          <div className="filter-actions">
            <button className="apply-filters-button" type="submit">
              Search sessions
            </button>
            <button className="clear-filters-button" type="button" onClick={clearFilters}>
              Clear filters
            </button>
          </div>
        </form>

        <p className="active-search-label">
          {mode === 'nearby' && coordinates
            ? 'Showing sessions nearest to your current location'
            : selectedRegion || selectedSport || appliedFilters.query
              ? `Showing ${selectedSport?.name ?? 'sessions'}${selectedRegion ? ` in ${selectedRegion.name}` : ''}${appliedFilters.query ? ` matching “${appliedFilters.query}”` : ''}`
              : 'Showing all upcoming sessions'}
          {sessionPage ? ` · ${sessionPage.totalElements} found` : ''}
        </p>
      </section>

      {error && (
        <p className="discovery-error" role="alert">
          {error}
        </p>
      )}

      <section className="session-results" aria-live="polite">
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
      </section>

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
    </main>
  )
}

export default UserHomePage
