import { Link } from 'react-router-dom'

function NotFoundPage() {
  return (
    <main className="placeholder-page">
      <section className="placeholder-card">
        <p className="eyebrow">Wrong turn</p>
        <h1>This path leads nowhere.</h1>
        <p>Let’s get you back in motion.</p>
        <Link className="back-link" to="/">
          Return to Csports
        </Link>
      </section>
    </main>
  )
}

export default NotFoundPage
