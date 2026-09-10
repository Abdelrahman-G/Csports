import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/authContext'

function TrainerNavigation() {
  const navigate = useNavigate()
  const { logout } = useAuth()
  const [isLoggingOut, setIsLoggingOut] = useState(false)

  async function handleLogout() {
    setIsLoggingOut(true)
    try {
      await logout()
    } catch {
      // AuthProvider clears local authentication even if the server is unavailable.
    } finally {
      navigate('/login', { replace: true })
    }
  }

  return (
    <header className="trainer-navigation">
      <Link className="trainer-brand" to="/trainer/home">Csports</Link>
      <nav aria-label="Trainer navigation">
        <Link to="/trainer/home">My sessions</Link>
        <Link className="trainer-create-link" to="/trainer/sessions/new">Create session</Link>
        <button type="button" onClick={handleLogout} disabled={isLoggingOut}>
          {isLoggingOut ? 'Logging out...' : 'Log out'}
        </button>
      </nav>
    </header>
  )
}

export default TrainerNavigation
