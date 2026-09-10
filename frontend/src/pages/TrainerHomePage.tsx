import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../auth/authContext'
import { getMyTrainerProfile, type TrainerProfile } from '../auth/profileApi'
import TrainerNavigation from '../components/TrainerNavigation'
import { orderedDayLabels } from '../discovery/sessionFormatting'
import { getMySessions } from '../trainer/trainerSessionApi'
import type { TrainingSession } from '../trainer/types'

function TrainerHomePage() {
  const { authenticatedFetch } = useAuth()
  const [profile, setProfile] = useState<TrainerProfile | null>(null)
  const [sessions, setSessions] = useState<TrainingSession[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    const controller = new AbortController()

    async function loadDashboard() {
      setIsLoading(true)
      setError('')

      try {
        const [loadedProfile, loadedSessions] = await Promise.all([
          getMyTrainerProfile(authenticatedFetch),
          getMySessions(authenticatedFetch, controller.signal),
        ])
        setProfile(loadedProfile)
        setSessions(loadedSessions.content)
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
      <TrainerNavigation />
      <main className="trainer-page">
        <section className="trainer-dashboard-header">
          <div>
            <p className="eyebrow">Your coaching space</p>
            <h1>{profile ? `Welcome back, ${profile.name}` : 'My sessions'}</h1>
            <p>
              {profile
                ? `${profile.sport} coach · ${profile.experienceYears} years of experience`
                : 'Create and manage your training sessions.'}
            </p>
          </div>
          <Link className="trainer-primary-action" to="/trainer/sessions/new">Create a session</Link>
        </section>

        {isLoading && <p className="trainer-state">Loading your sessions...</p>}
        {error && <p className="trainer-state error" role="alert">{error}</p>}

        {!isLoading && !error && sessions.length === 0 && (
          <section className="trainer-empty-state">
            <h2>Your first session starts here</h2>
            <p>Create a schedule and make it available to athletes.</p>
            <Link to="/trainer/sessions/new">Create a session</Link>
          </section>
        )}

        {!isLoading && !error && sessions.length > 0 && (
          <section className="trainer-session-grid" aria-label="Your sessions">
            {sessions.map((session) => (
              <article className="trainer-session-card" key={session.id}>
                <div className="trainer-session-card-heading">
                  <span className={`trainer-status ${session.status.toLowerCase()}`}>{session.status}</span>
                  <span>{session.currentParticipants}/{session.maxParticipants} booked</span>
                </div>
                <h2>{session.title}</h2>
                <p>{session.locationName}</p>
                <dl>
                  <div><dt>Starts</dt><dd>{session.startDate}</dd></div>
                  <div><dt>Schedule</dt><dd>{orderedDayLabels(session.days).join(', ')}</dd></div>
                </dl>
                <Link to={`/trainer/sessions/${session.id}/manage`}>Manage session</Link>
              </article>
            ))}
          </section>
        )}
      </main>
    </>
  )
}

export default TrainerHomePage
