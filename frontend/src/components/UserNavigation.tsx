import { useEffect, useState } from 'react'
import { NavLink } from 'react-router-dom'
import csportsLogo from '../assets/csports-logo-v3.png'
import { useAuth } from '../auth/authContext'
import { getUnreadNotificationCount } from '../notifications/notificationApi'

function UserNavigation() {
  const { authenticatedFetch } = useAuth()
  const [unreadCount, setUnreadCount] = useState(0)

  useEffect(() => {
    const controller = new AbortController()

    getUnreadNotificationCount(authenticatedFetch, controller.signal)
      .then((response) => setUnreadCount(response.unreadCount))
      .catch(() => {
        // A badge failure should not prevent navigation from working.
      })

    return () => controller.abort()
  }, [authenticatedFetch])

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

        <div className="user-navigation-actions">
          <NavLink
            className={({ isActive }) =>
              isActive
                ? 'user-notification-link active'
                : 'user-notification-link'
            }
            to="/user/notifications"
            aria-label={`Notifications, ${unreadCount} unread`}
          >
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9M10 21h4" />
            </svg>
            {unreadCount > 0 && (
              <span className="user-notification-badge">
                {unreadCount > 99 ? '99+' : unreadCount}
              </span>
            )}
          </NavLink>

          <NavLink className="user-profile-link" to="/user/profile">Profile</NavLink>
        </div>
      </div>
    </header>
  )
}

export default UserNavigation
