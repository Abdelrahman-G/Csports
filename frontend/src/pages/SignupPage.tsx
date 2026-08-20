import { Link } from 'react-router-dom'

function SignupPage() {
  return (
    <main className="placeholder-page">
      <section className="placeholder-card">
        <p className="eyebrow">Join Csports</p>
        <h1>Create an account</h1>
        <p>The sign-up form will be built in our next lesson.</p>
        <Link className="back-link" to="/">
          Back to home
        </Link>
      </section>
    </main>
  )
}

export default SignupPage
