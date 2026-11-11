import { useEffect, useState } from 'react'
import { NavLink } from 'react-router-dom'
import csportsLogo from '../assets/csports-logo-v3.png'
import { useAuth } from '../auth/authContext'
import { getUnreadNotificationCount } from '../notifications/notificationApi'

export default function AppNavigation() {
  const { session, authenticatedFetch } = useAuth()
  const isTrainer = session?.role === 'TRAINER'
  const isTrainee = session?.role === 'USER'
  const home = isTrainer ? '/trainer/home' : '/user/home'
  const links = isTrainer
    ? [{ to: '/trainer/home', label: 'My courses' }, { to: '/trainer/courses/new', label: 'Create course' }]
    : [{ to: '/user/schedule', label: 'My schedule' }, { to: '/user/home', label: 'Discover' }, { to: '/user/bookings', label: 'My bookings' }]
  const [unreadCount, setUnreadCount] = useState(0)

  useEffect(() => {
    if (!isTrainee) return
    const controller = new AbortController()
    getUnreadNotificationCount(authenticatedFetch, controller.signal)
      .then(response => { if (!controller.signal.aborted) setUnreadCount(response.unreadCount) })
      .catch(() => {
        // A badge failure must not prevent navigation.
      })
    return () => controller.abort()
  }, [authenticatedFetch, isTrainee])

  return <header className="app-navigation">
    <div className="app-navigation-inner">
      <NavLink className="app-navigation-brand" to={home} aria-label="Csports home">
        <img src={csportsLogo} alt="" />
      </NavLink>
      <nav aria-label={isTrainer ? 'Trainer navigation' : 'Trainee navigation'}>
        {links.map(link => <NavLink key={link.to} to={link.to} end
          className={({ isActive }) => `app-navigation-link${isActive ? ' active' : ''}`}>
          {link.label}
        </NavLink>)}
      </nav>
      <div className="app-navigation-actions">
        {isTrainee && <NavLink to="/user/notifications"
          className={({ isActive }) => `app-notification-link${isActive ? ' active' : ''}`}
          aria-label={`Notifications, ${unreadCount} unread`}>
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9M10 21h4" />
          </svg>
          {unreadCount > 0 && <span className="app-notification-badge">{unreadCount > 99 ? '99+' : unreadCount}</span>}
        </NavLink>}
        <NavLink className="app-profile-link" to={isTrainer ? '/trainer/profile' : '/user/profile'}>Profile</NavLink>
      </div>
    </div>
  </header>
}
