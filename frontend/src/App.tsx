import { Navigate, Route, Routes, useLocation } from 'react-router-dom'
import './App.css'
import LandingPage from './pages/LandingPage'
import LoginPage from './pages/LoginPage'
import NotFoundPage from './pages/NotFoundPage'
import SignupPage from './pages/SignupPage'
import RegistrationPage from './pages/RegistrationPage'
import TrainerHomePage from './pages/TrainerHomePage'
import UserHomePage from './pages/UserHomePage'
import CourseDetailsPage from './pages/CourseDetailsPage'
import MyBookingsPage from './pages/MyBookingsPage'
import NotificationsPage from './pages/NotificationsPage'
import RequireRole from './auth/RequireRole'
import CreateTrainerCoursePage from './pages/CreateTrainerCoursePage'
import ManageTrainerCoursePage from './pages/ManageTrainerCoursePage'
import ProfilePage from './pages/ProfilePage'
import MySchedulePage from './pages/MySchedulePage'

function LegacyCourseLink() {
  const { pathname, search, hash } = useLocation()
  return <Navigate to={`${pathname.replace('/sessions/', '/courses/')}${search}${hash}`} replace />
}

function App() {
  return (
    <Routes>
      <Route path="/user/sessions/:courseId" element={<LegacyCourseLink />} />
      <Route path="/trainer/sessions/new" element={<LegacyCourseLink />} />
      <Route path="/trainer/sessions/:courseId/manage" element={<LegacyCourseLink />} />
      <Route path="/" element={<LandingPage />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/signup" element={<SignupPage />} />
      <Route
        path="/signup/user"
        element={<RegistrationPage accountType="user" />}
      />
      <Route
        path="/signup/trainer"
        element={<RegistrationPage accountType="trainer" />}
      />
      <Route
        path="/user/home"
        element={
          <RequireRole allowedRole="USER">
            <UserHomePage />
          </RequireRole>
        }
      />
      <Route
        path="/user/courses/:courseId"
        element={
          <RequireRole allowedRole="USER">
            <CourseDetailsPage />
          </RequireRole>
        }
      />
      <Route
        path="/user/schedule"
        element={<RequireRole allowedRole="USER"><MySchedulePage /></RequireRole>}
      />
      <Route
        path="/user/bookings"
        element={
          <RequireRole allowedRole="USER">
            <MyBookingsPage />
          </RequireRole>
        }
      />
      <Route
        path="/user/notifications"
        element={
          <RequireRole allowedRole="USER">
            <NotificationsPage />
          </RequireRole>
        }
      />
      <Route
        path="/user/profile"
        element={
          <RequireRole allowedRole="USER">
            <ProfilePage />
          </RequireRole>
        }
      />
      <Route
        path="/trainer/home"
        element={
          <RequireRole allowedRole="TRAINER">
            <TrainerHomePage />
          </RequireRole>
        }
      />
      <Route
        path="/trainer/courses/new"
        element={
          <RequireRole allowedRole="TRAINER">
            <CreateTrainerCoursePage />
          </RequireRole>
        }
      />
      <Route
        path="/trainer/courses/:courseId/manage"
        element={
          <RequireRole allowedRole="TRAINER">
            <ManageTrainerCoursePage />
          </RequireRole>
        }
      />
      <Route
        path="/trainer/profile"
        element={
          <RequireRole allowedRole="TRAINER">
            <ProfilePage />
          </RequireRole>
        }
      />
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  )
}

export default App
