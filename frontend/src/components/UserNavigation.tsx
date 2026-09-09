import { useState } from 'react'
import { NavLink, useNavigate } from 'react-router-dom'
import csportsLogo from '../assets/csports-logo-v3.png'
import { useAuth } from '../auth/authContext'

function UserNavigation() {
  const navigate = useNavigate()
  const { logout } = useAuth()
  const [isLoggingOut, setIsLoggingOut] = useState(false)

  async function handleLogout() {
    if (isLoggingOut) {
      return
    }

    setIsLoggingOut(true)

    try {
      await logout()
    } catch {
      // AuthProvider removes the browser session even if the API is unavailable.
    } finally {
      navigate('/login', { replace: true })
    }
  }

  return (
    <header className="user-navigation">
      <div className="user-navigation-inner">
        <NavLink
          className="user-navigation-brand"
          to="/user/home"
          aria-label="Csports session discovery"
        >
          <img src={csportsLogo} alt="" />
        </NavLink>

        <nav aria-label="Participant navigation">
          <NavLink
            className={({ isActive }) =>
              isActive ? 'user-navigation-link active' : 'user-navigation-link'
            }
            to="/user/home"
            end
          >
            Discover
          </NavLink>
          <NavLink
            className={({ isActive }) =>
              isActive ? 'user-navigation-link active' : 'user-navigation-link'
            }
            to="/user/bookings"
          >
            My bookings
          </NavLink>
        </nav>

        <button
          className="user-navigation-logout"
          type="button"
          onClick={handleLogout}
          disabled={isLoggingOut}
        >
          {isLoggingOut ? 'Logging out...' : 'Log out'}
        </button>
      </div>
    </header>
  )
}

export default UserNavigation
