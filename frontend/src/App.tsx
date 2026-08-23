import { Route, Routes } from 'react-router-dom'
import './App.css'
import LandingPage from './pages/LandingPage'
import LoginPage from './pages/LoginPage'
import NotFoundPage from './pages/NotFoundPage'
import SignupPage from './pages/SignupPage'
import RegistrationPage from './pages/RegistrationPage'
import TrainerHomePage from './pages/TrainerHomePage'
import UserHomePage from './pages/UserHomePage'
import RequireRole from './auth/RequireRole'

function App() {
  return (
    <Routes>
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
        path="/trainer/home"
        element={
          <RequireRole allowedRole="TRAINER">
            <TrainerHomePage />
          </RequireRole>
        }
      />
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  )
}

export default App
