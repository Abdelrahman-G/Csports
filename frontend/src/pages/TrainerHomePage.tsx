import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/authContext'
import { getMyTrainerProfile, type TrainerProfile } from '../auth/profileApi'

function TrainerHomePage() {
  const navigate = useNavigate()
  const { authenticatedFetch, logout } = useAuth()
  const [profile, setProfile] = useState<TrainerProfile | null>(null)
  const [error, setError] = useState('')
  const [isLoggingOut, setIsLoggingOut] = useState(false)

  useEffect(() => {
    let shouldIgnoreResult = false

    async function loadProfile() {
      try {
        const loadedProfile = await getMyTrainerProfile(authenticatedFetch)

        if (!shouldIgnoreResult) {
          setProfile(loadedProfile)
        }
      } catch (requestError) {
        if (!shouldIgnoreResult) {
          setError(
            requestError instanceof Error
              ? requestError.message
              : 'Your profile could not be loaded.',
          )
        }
      }
    }

    void loadProfile()

    return () => {
      shouldIgnoreResult = true
    }
  }, [authenticatedFetch])

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

  return (
    <main className="placeholder-page">
      <section className="placeholder-card dashboard-card">
        <p className="eyebrow">Your coaching space</p>
        <h1>
          {profile ? `Ready to lead, ${profile.name}?` : 'Preparing your space...'}
        </h1>

        {error && (
          <p className="form-error-message" role="alert">
            {error}
          </p>
        )}

        {profile && (
          <dl className="profile-details">
            <div>
              <dt>Sport</dt>
              <dd>{profile.sport}</dd>
            </div>
            <div>
              <dt>Experience</dt>
              <dd>{profile.experienceYears} years</dd>
            </div>
            <div>
              <dt>Region</dt>
              <dd>
                {profile.regionName}, {profile.city}
              </dd>
            </div>
            {profile.bio && (
              <div>
                <dt>Bio</dt>
                <dd>{profile.bio}</dd>
              </div>
            )}
          </dl>
        )}

        <button
          className="secondary-action-button"
          type="button"
          onClick={handleLogout}
          disabled={isLoggingOut}
        >
          {isLoggingOut ? 'Signing you out...' : 'Log out'}
        </button>
      </section>
    </main>
  )
}

export default TrainerHomePage
