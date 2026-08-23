import { useState, type FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { ApiError, loginAccount } from '../auth/authApi'
import { useAuth } from '../auth/authContext'
import { getHomePath } from '../auth/routePaths'
import PasswordInput from '../components/PasswordInput'

type LoginErrors = {
  identifier?: string
  password?: string
}

function LoginPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const { signIn } = useAuth()
  const [identifier, setIdentifier] = useState('')
  const [password, setPassword] = useState('')
  const [errors, setErrors] = useState<LoginErrors>({})
  const [serverError, setServerError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const registrationMessage = (
    location.state as { registrationMessage?: string } | null
  )?.registrationMessage

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    const nextErrors: LoginErrors = {}

    if (!identifier.trim()) {
      nextErrors.identifier = 'Email or phone number is required.'
    }

    if (!password) {
      nextErrors.password = 'Password is required.'
    }

    setErrors(nextErrors)

    if (Object.keys(nextErrors).length > 0) {
      setServerError('')
      return
    }

    setIsSubmitting(true)
    setServerError('')

    try {
      const session = await loginAccount({
        identifier: identifier.trim(),
        password,
      })

      signIn(session)
      navigate(getHomePath(session.role), { replace: true })
    } catch (error) {
      if (error instanceof ApiError) {
        setErrors({
          identifier: error.fieldErrors.identifier,
          password: error.fieldErrors.password,
        })
      }

      setServerError(
        error instanceof Error
          ? error.message
          : 'Login failed. Please try again.',
      )
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <main className="placeholder-page">
      <section className="placeholder-card auth-card">
        <p className="eyebrow">Pick up where you left off</p>
        <h1>Welcome back</h1>
        <p>Your next goal, session, or athlete is waiting.</p>

        {registrationMessage && (
          <p className="form-success-message" role="status">
            {registrationMessage}
          </p>
        )}

        <form className="auth-form" onSubmit={handleSubmit} noValidate>
          <div className="form-field">
            <label htmlFor="identifier">Email or phone number</label>
            <input
              id="identifier"
              name="identifier"
              type="text"
              autoComplete="username"
              value={identifier}
              onChange={(event) => {
                setIdentifier(event.target.value)
                setErrors((current) => ({ ...current, identifier: undefined }))
                setServerError('')
              }}
              disabled={isSubmitting}
              aria-invalid={Boolean(errors.identifier)}
              aria-describedby={errors.identifier ? 'identifier-error' : undefined}
            />
            {errors.identifier && (
              <span className="field-error" id="identifier-error">
                {errors.identifier}
              </span>
            )}
          </div>

          <PasswordInput
            id="password"
            label="Password"
            autoComplete="current-password"
            value={password}
            onChange={(value) => {
              setPassword(value)
              setErrors((current) => ({ ...current, password: undefined }))
              setServerError('')
            }}
            error={errors.password}
            disabled={isSubmitting}
          />

          <button
            className="submit-button"
            type="submit"
            disabled={isSubmitting}
          >
            {isSubmitting ? 'Getting you back in...' : 'Continue'}
          </button>
        </form>

        {serverError && (
          <p className="form-error-message" role="alert">
            {serverError}
          </p>
        )}

        <div className="auth-links">
          <span>
            Ready to begin? <Link to="/signup">Join Csports</Link>
          </span>
          <Link className="back-link" to="/">
            Back to the start
          </Link>
        </div>
      </section>
    </main>
  )
}

export default LoginPage
