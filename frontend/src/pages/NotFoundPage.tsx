import { Link } from 'react-router-dom'

function NotFoundPage() {
  return (
    <main className="placeholder-page">
      <section className="placeholder-card">
        <p className="eyebrow">404</p>
        <h1>Page not found</h1>
        <p>The page you requested does not exist.</p>
        <Link className="back-link" to="/">
          Back to home
        </Link>
      </section>
    </main>
  )
}

export default NotFoundPage
