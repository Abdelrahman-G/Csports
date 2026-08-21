import { Link } from 'react-router-dom'

function TrainerHomePage() {
  return (
    <main className="placeholder-page">
      <section className="placeholder-card dashboard-card">
        <p className="eyebrow">Trainer account</p>
        <h1>Login successful</h1>
        <p>
          You are signed in as a trainer. Session management will be added here
          later.
        </p>
        <Link className="back-link" to="/">
          Back to landing page
        </Link>
      </section>
    </main>
  )
}

export default TrainerHomePage
