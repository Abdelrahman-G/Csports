import { Link } from 'react-router-dom'

function UserHomePage() {
  return (
    <main className="placeholder-page">
      <section className="placeholder-card dashboard-card">
        <p className="eyebrow">Participant account</p>
        <h1>Login successful</h1>
        <p>
          You are signed in as a participant. Session discovery and bookings
          will be added here later.
        </p>
        <Link className="back-link" to="/">
          Back to landing page
        </Link>
      </section>
    </main>
  )
}

export default UserHomePage
