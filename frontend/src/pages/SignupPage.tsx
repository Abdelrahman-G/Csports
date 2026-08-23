import { Link } from 'react-router-dom'

function SignupPage() {
  return (
    <main className="placeholder-page">
      <section className="placeholder-card account-choice-card">
        <p className="eyebrow">Choose your path</p>
        <h1>How will you move?</h1>
        <p>Come to grow or come to lead. Your next chapter starts here.</p>

        <div className="account-options">
          <Link className="account-option" to="/signup/user">
            <strong>I want to train</strong>
            <span>
              Find the coach, rhythm, and challenge that move you forward.
            </span>
          </Link>
          <Link className="account-option" to="/signup/trainer">
            <strong>I want to coach</strong>
            <span>
              Turn your experience into impact and build a community around
              your sport.
            </span>
          </Link>
        </div>

        <div className="auth-links">
          <span>
            Already part of Csports?{' '}
            <Link to="/login">Continue your journey</Link>
          </span>
          <Link className="back-link" to="/">
            Back to the start
          </Link>
        </div>
      </section>
    </main>
  )
}

export default SignupPage
