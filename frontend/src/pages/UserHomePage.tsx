import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/authContext'
import { getMyUserProfile, type UserProfile } from '../auth/profileApi'

function UserHomePage() {
  const navigate = useNavigate()
  const { authenticatedFetch, logout } = useAuth()
  const [profile, setProfile] = useState<UserProfile | null>(null)
  const [error, setError] = useState('')
  const [isLoggingOut, setIsLoggingOut] = useState(false)

  useEffect(() => {
    let shouldIgnoreResult = false

    async function loadProfile() {
      try {
        const loadedProfile = await getMyUserProfile(authenticatedFetch)

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
        <p className="eyebrow">Your training space</p>
        <h1>
          {profile ? `Good to see you, ${profile.name}` : 'Preparing your space...'}
        </h1>

        {error && (
          <p className="form-error-message" role="alert">
            {error}
          </p>
        )}

        {profile && (
          <dl className="profile-details">
            <div>
              <dt>Email</dt>
              <dd>{profile.email}</dd>
            </div>
            <div>
              <dt>Phone</dt>
              <dd>{profile.phoneNumber}</dd>
            </div>
            <div>
              <dt>Age</dt>
              <dd>{profile.age}</dd>
            </div>
            <div>
              <dt>Region</dt>
              <dd>
                {profile.regionName}, {profile.city}
              </dd>
            </div>
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

export default UserHomePage
