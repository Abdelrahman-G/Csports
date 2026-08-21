import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { ApiError, loginAccount } from '../auth/authApi'
import { useAuth } from '../auth/authContext'
import { getHomePath } from '../auth/routePaths'

type LoginErrors = {
  identifier?: string
  password?: string
}

function LoginPage() {
  const navigate = useNavigate()
  const { signIn } = useAuth()
  const [identifier, setIdentifier] = useState('')
  const [password, setPassword] = useState('')
  const [errors, setErrors] = useState<LoginErrors>({})
  const [serverError, setServerError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

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
        <p className="eyebrow">Welcome back</p>
        <h1>Log in</h1>
        <p>Enter your account details to continue.</p>

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

          <div className="form-field">
            <label htmlFor="password">Password</label>
            <input
              id="password"
              name="password"
              type="password"
              autoComplete="current-password"
              value={password}
              onChange={(event) => {
                setPassword(event.target.value)
                setErrors((current) => ({ ...current, password: undefined }))
                setServerError('')
              }}
              disabled={isSubmitting}
              aria-invalid={Boolean(errors.password)}
              aria-describedby={errors.password ? 'password-error' : undefined}
            />
            {errors.password && (
              <span className="field-error" id="password-error">
                {errors.password}
              </span>
            )}
          </div>

          <button
            className="submit-button"
            type="submit"
            disabled={isSubmitting}
          >
            {isSubmitting ? 'Logging in...' : 'Log in'}
          </button>
        </form>

        {serverError && (
          <p className="form-error-message" role="alert">
            {serverError}
          </p>
        )}

        <div className="auth-links">
          <span>
            No account? <Link to="/signup">Sign up</Link>
          </span>
          <Link className="back-link" to="/">
            Back to home
          </Link>
        </div>
      </section>
    </main>
  )
}

export default LoginPage
