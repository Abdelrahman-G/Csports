import { useCallback, useEffect, useRef, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
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
import SportWheel from '../discovery/SportWheel'
import AreaSelector from '../discovery/AreaSelector'
import '../discovery/discover.css'

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
  const [referenceError, setReferenceError] = useState('')
  const [referenceReload, setReferenceReload] = useState(0)
  const [loadingReference, setLoadingReference] = useState(true)
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
  const locationAttempt = useRef(0)
  const [error, setError] = useState('')

  useEffect(() => () => { locationAttempt.current += 1 }, [])

  useEffect(() => {
    const controller = new AbortController()

    async function loadPageData() {
      setLoadingReference(true)
      setReferenceError('')
      try {
        const [loadedRegions, loadedSports] = await Promise.all([
          getRegions(controller.signal),
          getSports(controller.signal),
        ])

        if (controller.signal.aborted) return
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
          setReferenceError(
            requestError instanceof Error
              ? requestError.message
              : 'Your training space could not be loaded.',
          )
        }
      } finally {
        if (!controller.signal.aborted) setLoadingReference(false)
      }
    }

    void loadPageData()
    return () => controller.abort()
  }, [referenceReload])

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
        if (!controller.signal.aborted) setCoursePage(loadedPage)
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

  const selectSport = useCallback((sportId: string) => {
    setFilters(current => ({ ...current, sportId }))
    setAppliedFilters(current => ({ ...current, sportId }))
    setPageNumber(0)
  }, [])

  function selectArea(regionId: string) {
    locationAttempt.current += 1
    setIsLocating(false)
    setFilters(current => ({ ...current, regionId }))
    setAppliedFilters(current => ({ ...current, regionId }))
    setMode('region')
    setPageNumber(0)
    if (regionId) localStorage.setItem(REGION_STORAGE_KEY, regionId)
    else localStorage.removeItem(REGION_STORAGE_KEY)
  }

  function applyFilters(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    const nextFilters = { ...filters, query: filters.query.trim() }
    setFilters(nextFilters)
    setAppliedFilters(nextFilters)
    setPageNumber(0)

    if (nextFilters.regionId) {
      localStorage.setItem(REGION_STORAGE_KEY, nextFilters.regionId)
    } else {
      localStorage.removeItem(REGION_STORAGE_KEY)
    }
  }

  function clearFilters() {
    locationAttempt.current += 1
    setIsLocating(false)
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

    const attempt = ++locationAttempt.current

    setIsLocating(true)
    navigator.geolocation.getCurrentPosition(
      (position) => {
        if (attempt !== locationAttempt.current) return
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
        setFilters(current => ({ ...current, regionId: '' }))
        setAppliedFilters(current => ({ ...current, regionId: '' }))
        setCoordinates({ latitude, longitude, accuracy })
        setMode('nearby')
        setPageNumber(0)
        setIsLocating(false)
      },
      (locationError) => {
        if (attempt !== locationAttempt.current) return
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
      <main className="discovery-page wheel-discovery">
        <header className="discovery-header">
          <div><p className="eyebrow">Cairo &amp; Giza / Discover</p><h1>Choose your sport.</h1></div>
          <p className="discovery-intro-note">Explore courses from coaches across your city.</p>
        </header>
        {referenceError && <p className="discovery-reference-error" role="alert">{referenceError}<button type="button" onClick={() => setReferenceReload(n => n + 1)}>Try again</button></p>}
        <SportWheel sports={sports} value={filters.sportId} onChange={selectSport} loading={loadingReference} />
        <AreaSelector regions={regions} value={filters.regionId} onChange={selectArea} onNearby={useCurrentLocation} locating={isLocating} nearby={mode === 'nearby'} />
        <form className="discovery-query" onSubmit={applyFilters}>
          <label htmlFor="course-search">Looking for something specific?<input id="course-search" type="search" value={filters.query} onChange={event => updateFilter('query', event.target.value)} placeholder="Search course titles or venues" maxLength={100} /></label>
          <button type="submit">Find courses ↗</button>
          <button className="discovery-reset" type="button" onClick={clearFilters}>Reset</button>
        </form>
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
      </main>
    </>
  )
}

export default UserHomePage
