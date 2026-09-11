import { Link } from 'react-router-dom'

function TrainerNavigation() {
  return (
    <header className="trainer-navigation">
      <Link className="trainer-brand" to="/trainer/home">Csports</Link>
      <nav aria-label="Trainer navigation">
        <Link to="/trainer/home">My sessions</Link>
        <Link className="trainer-create-link" to="/trainer/sessions/new">Create session</Link>
        <Link to="/trainer/profile">Profile</Link>
      </nav>
    </header>
  )
}

export default TrainerNavigation
