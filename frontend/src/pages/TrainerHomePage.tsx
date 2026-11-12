import AppNavigation from '../components/AppNavigation'
import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../auth/authContext'
import { getMyTrainerProfile, type TrainerProfile } from '../auth/profileApi'
import { orderedDayLabels } from '../discovery/courseFormatting'
import { getMyCourses } from '../trainer/trainerCourseApi'
import type { TrainingCourse } from '../trainer/types'

function TrainerHomePage() {
  const { authenticatedFetch } = useAuth()
  const [profile, setProfile] = useState<TrainerProfile | null>(null)
  const [courses, setCourses] = useState<TrainingCourse[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    const controller = new AbortController()

    async function loadDashboard() {
      setIsLoading(true)
      setError('')

      try {
        const [loadedProfile, loadedCourses] = await Promise.all([
          getMyTrainerProfile(authenticatedFetch),
          getMyCourses(authenticatedFetch, controller.signal),
        ])
        setProfile(loadedProfile)
        setCourses(loadedCourses.content)
      } catch (requestError) {
        if (!controller.signal.aborted) {
          setError(
            requestError instanceof Error
              ? requestError.message
              : 'Your coaching dashboard could not be loaded.',
          )
        }
      } finally {
        if (!controller.signal.aborted) setIsLoading(false)
      }
    }

    void loadDashboard()
    return () => controller.abort()
  }, [authenticatedFetch])

  return (
    <>
      <AppNavigation />
      <main className="trainer-page">
        <section className="trainer-dashboard-header">
          <div>
            <p className="eyebrow">Your coaching space</p>
            <h1>{profile ? `Welcome back, ${profile.name}` : 'My courses'}</h1>
            <p>
              {profile
                ? `${profile.sport} coach · ${profile.experienceYears} years of experience`
                : 'Create and manage your training courses.'}
            </p>
          </div>
          <Link className="action-button action-button-primary" to="/trainer/courses/new">Create a course</Link>
        </section>

        {isLoading && <p className="trainer-state">Loading your courses...</p>}
        {error && <p className="trainer-state error" role="alert">{error}</p>}

        {!isLoading && !error && courses.length === 0 && (
          <section className="trainer-empty-state">
            <h2>Your first course starts here</h2>
            <p>Create a schedule and make it available to athletes.</p>
            <Link to="/trainer/courses/new">Create a course</Link>
          </section>
        )}

        {!isLoading && !error && courses.length > 0 && (
          <section className="trainer-course-grid" aria-label="Your courses">
            {courses.map((course) => (
              <article className="trainer-course-card" key={course.id}>
                <div className="trainer-course-card-heading">
                  <span className={`trainer-status ${course.status.toLowerCase()}`}>{course.status}</span>
                  <span>{course.currentParticipants}/{course.maxParticipants} booked</span>
                </div>
                <h2>{course.title}</h2>
                <p>{course.locationName}</p>
                <dl>
                  <div><dt>Starts</dt><dd>{course.startDate}</dd></div>
                  <div><dt>Schedule</dt><dd>{orderedDayLabels(course.days).join(', ')}</dd></div>
                </dl>
                <Link to={`/trainer/courses/${course.id}/manage`}>Manage course<span aria-hidden="true">↗</span></Link>
              </article>
            ))}
          </section>
        )}
      </main>
    </>
  )
}

export default TrainerHomePage
