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
            Join Csports
          </Link>
        </nav>
      </header>

      <main className="hero">
        <section className="hero-content">
          <p className="eyebrow">Your city. Your sport. Your people.</p>
          <h1>Train for the person you want to become.</h1>
          <p className="hero-description">
            Discover serious coaches and nearby sessions that match your goals.
            Show up, stay consistent, and make every week count.
          </p>

          <div className="hero-actions">
            <Link className="primary-button" to="/signup">
              Sign up
            </Link>
            <Link className="secondary-button" to="/login">
              Log in
            </Link>
          </div>
        </section>

        <section className="hero-visual" aria-label="Csports highlights">
          <h2>Momentum starts with one session.</h2>
          <p>
            Find the right coach, claim your place, and turn intention into
            progress.
          </p>

          <div className="feature-list">
            <span>Train nearby</span>
            <span>Meet your coach</span>
            <span>Keep your momentum</span>
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
