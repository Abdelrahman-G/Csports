import { Link } from 'react-router-dom'
import csportsLogo from '../assets/csports-logo-v3.png'

function LandingPage() {
  return (
    <div className="landing-page">
      <header className="header">
        <Link className="brand" to="/" aria-label="Csports home">
          <img className="brand-logo" src={csportsLogo} alt="" />
        </Link>

        <nav className="navigation" aria-label="Main navigation">
          <Link className="login-link" to="/login">
            Log in
          </Link>
          <Link className="signup-link" to="/signup">
            Sign up
          </Link>
        </nav>
      </header>

      <main className="hero">
        <section className="hero-content">
          <p className="eyebrow">Train together. Improve together.</p>
          <h1>Find the right sport for you.</h1>
          <p className="hero-description">
            Discover local training sessions, connect with experienced trainers,
            and book your place in a few simple steps.
          </p>

          <div className="hero-actions">
            <Link className="primary-button" to="/signup">
              Create an account
            </Link>
            <Link className="secondary-button" to="/login">
              Log in
            </Link>
          </div>
        </section>

        <section className="hero-visual" aria-label="Csports highlights">
          <h2>Your next session starts here</h2>
          <p>Search, book, and manage your training sessions from one place.</p>

          <div className="feature-list">
            <span>Find trainers</span>
            <span>Book sessions</span>
            <span>Stay notified</span>
          </div>
        </section>
      </main>

      <footer className="footer">
        <p>© 2026 Csports</p>
      </footer>
    </div>
  )
}

export default LandingPage
