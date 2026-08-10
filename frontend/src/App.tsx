import './App.css'
import csportsLogo from './assets/csports-logo-v3.png'

function App() {
  return (
    <div className="landing-page">
      <header className="header">
        <a className="brand" href="#top" aria-label="Csports home">
          <img className="brand-logo" src={csportsLogo} alt="" />
        </a>

        <nav className="navigation" aria-label="Main navigation">
          <a className="login-link" href="#login">
            Log in
          </a>
          <a className="signup-link" href="#signup">
            Sign up
          </a>
        </nav>
      </header>

      <main className="hero" id="top">
        <section className="hero-content">
          <p className="eyebrow">Train together. Improve together.</p>
          <h1>Find the right sport for you.</h1>
          <p className="hero-description">
            Discover local training sessions, connect with experienced trainers,
            and book your place in a few simple steps.
          </p>

          <div className="hero-actions">
            <a className="primary-button" href="#signup">
              Create an account
            </a>
            <a className="secondary-button" href="#login">
              Log in
            </a>
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

export default App
