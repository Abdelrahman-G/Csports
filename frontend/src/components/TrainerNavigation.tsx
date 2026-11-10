import { Link } from 'react-router-dom'

function TrainerNavigation() {
  return (
    <header className="trainer-navigation">
      <Link className="trainer-brand" to="/trainer/home">Csports</Link>
      <nav aria-label="Trainer navigation">
        <Link to="/trainer/home">My courses</Link>
        <Link className="trainer-create-link" to="/trainer/courses/new">Create course</Link>
        <Link to="/trainer/profile">Profile</Link>
      </nav>
    </header>
  )
}

export default TrainerNavigation
