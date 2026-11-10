import { useEffect, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../auth/authContext'
import { getMyUserProfile, type UserProfile } from '../auth/profileApi'
import {
  getRegions,
  getSports,
  searchCourses,
  type CourseSearch,
} from '../discovery/discoveryApi'
import type {
  NearbyCoordinates,
  Region,
  CoursePage,
  Sport,
  TrainingCourse,
} from '../discovery/types'
import { orderedDayLabels } from '../discovery/courseFormatting'
import UserNavigation from '../components/UserNavigation'

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

const courseDateFormatter = new Intl.DateTimeFormat('en-EG', {
  weekday: 'short',
  day: 'numeric',
  month: 'short',
  year: 'numeric',
})

const courseTimeFormatter = new Intl.DateTimeFormat('en-EG', {
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

function CourseCard({ course }: { course: TrainingCourse }) {
  const distance = formatDistance(course.distanceMeters)
  const firstTraining = new Date(course.bookingClosesAt)
  const trainingDays = orderedDayLabels(course.days)
  const availability =
    course.remainingSeats === 0
      ? 'Fully booked'
      : course.bookingOpen
        ? `${course.remainingSeats} places left`
        : 'Booking closed'

  return (
    <Link className="course-card-link" to={`/user/courses/${course.id}`}>
      <article className="course-card">
      <div className="course-card-heading">
        <span className="course-sport">{course.sport}</span>
        <span
          className={
            course.bookingOpen
              ? 'course-availability open'
              : 'course-availability closed'
          }
        >
          {availability}
        </span>
      </div>

      <h2>{course.title}</h2>
      <p className="course-trainer">with {course.trainerName}</p>

      <div className="course-start">
        <span className="course-start-label">First session</span>
        <strong>{courseDateFormatter.format(firstTraining)}</strong>
        <span className="course-time">
          {courseTimeFormatter.format(firstTraining)} for{' '}
          {course.durationMinutes} minutes
        </span>
      </div>

      <div className="course-days" aria-label="Training days">
        {trainingDays.map((day) => (
          <span key={day}>{day}</span>
        ))}
      </div>

      <dl className="course-summary">
        <div>
          <dt>Location</dt>
          <dd>
            {course.locationName}, {course.regionName}
          </dd>
        </div>
      </dl>

      <div className="course-card-footer">
        {distance && <span className="course-distance">{distance}</span>}
        <span className="course-details-link">View details</span>
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
  const [coursePage, setCoursePage] = useState<CoursePage | null>(null)
  const [isLoadingCourses, setIsLoadingCourses] = useState(false)
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
    let search: CourseSearch

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

    async function loadCourses() {
      setIsLoadingCourses(true)
      setError('')

      try {
        const loadedPage = await searchCourses(
          search,
          pageNumber,
          controller.signal,
        )
        setCoursePage(loadedPage)
      } catch (requestError) {
        if (!controller.signal.aborted) {
          setCoursePage(null)
          setError(
            requestError instanceof Error
              ? requestError.message
              : 'Courses could not be loaded.',
          )
        }
      } finally {
        if (!controller.signal.aborted) {
          setIsLoadingCourses(false)
        }
      }
    }

    void loadCourses()

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
  const courses = coursePage?.content ?? []

  return (
    <>
      <UserNavigation />
      <main className="discovery-page">
      <header className="discovery-header">
        <p className="eyebrow">Find your next course</p>
        <h1>{profile ? `Welcome, ${profile.name}` : 'Welcome'}</h1>
        <p>Explore coaches and courses across Cairo and Giza.</p>
      </header>

      <form className="discovery-catalogue" onSubmit={applyFilters}>
        <div className="discovery-search-bar">
          <label htmlFor="course-search">Search courses</label>
          <div>
            <input
              id="course-search"
              type="search"
              value={filters.query}
              onChange={(event) => updateFilter('query', event.target.value)}
              placeholder="Search courses or training locations"
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
              <div className="filter-select-control">
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

              <div className="location-choice-divider" aria-hidden="true">
                <span>or</span>
              </div>

              <button
                className={mode === 'nearby' ? 'nearby-button active' : 'nearby-button'}
                type="button"
                onClick={useCurrentLocation}
                disabled={isLocating}
              >
                {isLocating
                  ? 'Finding location...'
                  : mode === 'nearby'
                    ? 'Using current location'
                    : 'Use current location'}
              </button>
            </div>

            <div className="sidebar-filter-group">
              <label htmlFor="sport-filter">Sport</label>
              <div className="filter-select-control">
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
            </div>

            <button className="apply-filters-button" type="submit">
              Apply filters
            </button>
          </aside>

          <section className="discovery-results-column">
            <div className="results-heading">
              <div>
                <p className="active-search-label">
                  {mode === 'nearby' && coordinates
                    ? 'Nearest courses'
                    : selectedRegion || selectedSport || appliedFilters.query
                      ? `${selectedSport?.name ?? 'Courses'}${selectedRegion ? ` in ${selectedRegion.name}` : ''}`
                      : 'All upcoming courses'}
                </p>
                {appliedFilters.query && (
                  <span>Matching "{appliedFilters.query}"</span>
                )}
              </div>
              {coursePage && <strong>{coursePage.totalElements} results</strong>}
            </div>

            {error && (
              <p className="discovery-error" role="alert">
                {error}
              </p>
            )}

            <div className="course-results" aria-live="polite" aria-busy={isLoadingCourses}>
              {isLoadingCourses ? (
                <>
                  <span className="motion-sr-only" role="status">Finding available courses...</span>
                  {Array.from({ length: 4 }, (_, index) => (
                    <div className="course-skeleton" key={index} aria-hidden="true">
                      <span /><span /><span /><span />
                    </div>
                  ))}
                </>
              ) : courses.length === 0 ? (
                <p className="course-empty-state">
                  No courses match these filters yet. Try clearing one of them.
                </p>
              ) : (
                courses.map((course) => (
                  <CourseCard key={course.id} course={course} />
                ))
              )}
            </div>

            {coursePage && coursePage.totalPages > 1 && (
              <nav className="course-pagination" aria-label="Course result pages">
                <button
                  type="button"
                  onClick={() => setPageNumber((current) => current - 1)}
                  disabled={coursePage.first || isLoadingCourses}
                >
                  Previous
                </button>
                <span>
                  Page {coursePage.page + 1} of {coursePage.totalPages}
                </span>
                <button
                  type="button"
                  onClick={() => setPageNumber((current) => current + 1)}
                  disabled={coursePage.last || isLoadingCourses}
                >
                  Next
                </button>
              </nav>
            )}
          </section>
        </div>
      </form>
      </main>
    </>
  )
}

export default UserHomePage
