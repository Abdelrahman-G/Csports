import { useEffect, useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { ApiError } from '../auth/authApi'
import { useAuth } from '../auth/authContext'
import {
  getMyTrainerProfile,
  getMyUserProfile,
  updateMyTrainerProfile,
  updateMyUserProfile,
  type TrainerProfile,
  type UserProfile,
} from '../auth/profileApi'
import TrainerNavigation from '../components/TrainerNavigation'
import UserNavigation from '../components/UserNavigation'

type AccountForm = {
  name: string
  phoneNumber: string
  age: string
}

type CoachingForm = {
  bio: string
  experienceYears: string
}

const EGYPTIAN_PHONE = /^01[0125][0-9]{8}$/

function ProfilePage() {
  const navigate = useNavigate()
  const { session, authenticatedFetch, logout } = useAuth()
  const isTrainer = session?.role === 'TRAINER'
  const [profile, setProfile] = useState<UserProfile | null>(null)
  const [trainerProfile, setTrainerProfile] = useState<TrainerProfile | null>(null)
  const [accountForm, setAccountForm] = useState<AccountForm | null>(null)
  const [coachingForm, setCoachingForm] = useState<CoachingForm | null>(null)
  const [accountErrors, setAccountErrors] = useState<Record<string, string>>({})
  const [coachingErrors, setCoachingErrors] = useState<Record<string, string>>({})
  const [isLoading, setIsLoading] = useState(true)
  const [isSavingAccount, setIsSavingAccount] = useState(false)
  const [isSavingCoaching, setIsSavingCoaching] = useState(false)
  const [isLoggingOut, setIsLoggingOut] = useState(false)
  const [accountMessage, setAccountMessage] = useState('')
  const [coachingMessage, setCoachingMessage] = useState('')
  const [pageError, setPageError] = useState('')

  useEffect(() => {
    const controller = new AbortController()

    async function loadProfile() {
      try {
        const [loadedProfile, loadedTrainerProfile] = await Promise.all([
          getMyUserProfile(authenticatedFetch, controller.signal),
          isTrainer
            ? getMyTrainerProfile(authenticatedFetch, controller.signal)
            : Promise.resolve(null),
        ])

        setProfile(loadedProfile)
        setAccountForm({
          name: loadedProfile.name,
          phoneNumber: loadedProfile.phoneNumber,
          age: String(loadedProfile.age),
        })
        setTrainerProfile(loadedTrainerProfile)
        if (loadedTrainerProfile) {
          setCoachingForm({
            bio: loadedTrainerProfile.bio ?? '',
            experienceYears: String(loadedTrainerProfile.experienceYears),
          })
        }
      } catch (requestError) {
        if (!controller.signal.aborted) {
          setPageError(
            requestError instanceof Error
              ? requestError.message
              : 'Your profile could not be loaded.',
          )
        }
      } finally {
        if (!controller.signal.aborted) setIsLoading(false)
      }
    }

    void loadProfile()
    return () => controller.abort()
  }, [authenticatedFetch, isTrainer])

  function updateAccountField(field: keyof AccountForm, value: string) {
    setAccountForm((current) => current ? { ...current, [field]: value } : current)
    setAccountErrors((current) => ({ ...current, [field]: '' }))
    setAccountMessage('')
    setPageError('')
  }

  async function saveAccount(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!accountForm || isSavingAccount) return

    const errors: Record<string, string> = {}
    const minimumAge = isTrainer ? 18 : 13
    if (accountForm.name.trim().length < 2) errors.name = 'Name must contain at least two characters.'
    if (!EGYPTIAN_PHONE.test(accountForm.phoneNumber)) errors.phoneNumber = 'Enter an 11-digit Egyptian mobile number.'
    if (!Number.isInteger(Number(accountForm.age)) || Number(accountForm.age) < minimumAge || Number(accountForm.age) > 100) errors.age = `Age must be between ${minimumAge} and 100.`
    setAccountErrors(errors)
    if (Object.keys(errors).length > 0) return

    setIsSavingAccount(true)
    setAccountMessage('')
    setPageError('')
    try {
      const updatedProfile = await updateMyUserProfile(authenticatedFetch, {
        name: accountForm.name.trim(),
        phoneNumber: accountForm.phoneNumber,
        age: Number(accountForm.age),
      })
      setProfile(updatedProfile)
      setAccountForm({
        name: updatedProfile.name,
        phoneNumber: updatedProfile.phoneNumber,
        age: String(updatedProfile.age),
      })
      setAccountMessage('Account details updated successfully.')
    } catch (requestError) {
      if (requestError instanceof ApiError) setAccountErrors(requestError.fieldErrors)
      setPageError(requestError instanceof Error ? requestError.message : 'Account details could not be updated.')
    } finally {
      setIsSavingAccount(false)
    }
  }

  function updateCoachingField(field: keyof CoachingForm, value: string) {
    setCoachingForm((current) => current ? { ...current, [field]: value } : current)
    setCoachingErrors((current) => ({ ...current, [field]: '' }))
    setCoachingMessage('')
    setPageError('')
  }

  async function saveCoachingProfile(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!coachingForm || isSavingCoaching) return

    const experienceYears = Number(coachingForm.experienceYears)
    if (!Number.isInteger(experienceYears) || experienceYears < 0 || experienceYears > 80) {
      setCoachingErrors({ experienceYears: 'Experience must be between 0 and 80 years.' })
      return
    }

    setIsSavingCoaching(true)
    setCoachingMessage('')
    setPageError('')
    try {
      const updatedProfile = await updateMyTrainerProfile(authenticatedFetch, {
        bio: coachingForm.bio.trim(),
        experienceYears,
      })
      setTrainerProfile(updatedProfile)
      setCoachingForm({
        bio: updatedProfile.bio ?? '',
        experienceYears: String(updatedProfile.experienceYears),
      })
      setCoachingMessage('Coaching profile updated successfully.')
    } catch (requestError) {
      if (requestError instanceof ApiError) setCoachingErrors(requestError.fieldErrors)
      setPageError(requestError instanceof Error ? requestError.message : 'Coaching profile could not be updated.')
    } finally {
      setIsSavingCoaching(false)
    }
  }

  async function handleLogout() {
    if (isLoggingOut) return
    setIsLoggingOut(true)
    try {
      await logout()
    } finally {
      navigate('/login', { replace: true })
    }
  }

  const navigation = isTrainer ? <TrainerNavigation /> : <UserNavigation />
  if (isLoading) return <>{navigation}<main className="profile-page"><p className="profile-state">Loading your profile...</p></main></>
  if (!profile || !accountForm) return <>{navigation}<main className="profile-page"><p className="profile-state profile-state-error">{pageError || 'Your profile could not be loaded.'}</p></main></>

  const initials = profile.name.trim().split(/\s+/).slice(0, 2).map((part) => part[0]).join('').toUpperCase()
  return (
    <>
      {navigation}
      <main className="profile-page">
        <header className="profile-hero">
          <div className="profile-avatar">{profile.photoUrl ? <img src={profile.photoUrl} alt="" /> : initials}</div>
          <div><span>{isTrainer ? 'Trainer account' : 'Athlete account'}</span><h1>{profile.name}</h1><p>{profile.email}</p></div>
        </header>

        {pageError && <p className="profile-error" role="alert">{pageError}</p>}

        <div className="profile-layout">
          <div className="profile-sections">
            <section className="profile-card">
              <div className="profile-card-heading"><div><h2>Account details</h2><p>Your personal and contact information.</p></div></div>
              {accountMessage && <p className="profile-success" role="status">✓ {accountMessage}</p>}
              <form className="profile-form" onSubmit={saveAccount} noValidate>
                <ProfileField id="profile-name" label="Name" value={accountForm.name} error={accountErrors.name} disabled={isSavingAccount} onChange={(value) => updateAccountField('name', value)} />
                <div className="form-field"><label htmlFor="profile-email">Email</label><input id="profile-email" value={profile.email} disabled /><span className="field-hint">Email changes are not available yet.</span></div>
                <ProfileField id="profile-phone" label="Phone number" type="tel" value={accountForm.phoneNumber} error={accountErrors.phoneNumber} disabled={isSavingAccount} onChange={(value) => updateAccountField('phoneNumber', value)} />
                <ProfileField id="profile-age" label="Age" type="number" value={accountForm.age} error={accountErrors.age} disabled={isSavingAccount} onChange={(value) => updateAccountField('age', value)} />
                <button className="profile-save-button" type="submit" disabled={isSavingAccount}>{isSavingAccount ? 'Saving...' : 'Save account details'}</button>
              </form>
            </section>

            {isTrainer && trainerProfile && coachingForm && (
              <section className="profile-card">
                <div className="profile-card-heading"><div><h2>Coaching profile</h2><p>Help athletes understand your experience.</p></div><span>{trainerProfile.sport}</span></div>
                {coachingMessage && <p className="profile-success" role="status">✓ {coachingMessage}</p>}
                <form className="profile-form" onSubmit={saveCoachingProfile} noValidate>
                  <div className="form-field full-width"><label htmlFor="profile-bio">Coaching story</label><textarea id="profile-bio" value={coachingForm.bio} maxLength={1000} disabled={isSavingCoaching} onChange={(event) => updateCoachingField('bio', event.target.value)} placeholder="Describe your coaching approach and the athletes you work with." />{coachingErrors.bio && <span className="field-error">{coachingErrors.bio}</span>}</div>
                  <ProfileField id="profile-experience" label="Years of experience" type="number" value={coachingForm.experienceYears} error={coachingErrors.experienceYears} disabled={isSavingCoaching} onChange={(value) => updateCoachingField('experienceYears', value)} />
                  <button className="profile-save-button" type="submit" disabled={isSavingCoaching}>{isSavingCoaching ? 'Saving...' : 'Save coaching profile'}</button>
                </form>
              </section>
            )}
          </div>

          <aside className="profile-sidebar">
            <section className="profile-card">
              <h2>Profile summary</h2>
              <dl>
                <div><dt>Role</dt><dd>{isTrainer ? 'Trainer' : 'Athlete'}</dd></div>
                {trainerProfile && <div><dt>Sport</dt><dd>{trainerProfile.sport}</dd></div>}
                {profile.regionName && <div><dt>Area</dt><dd>{profile.regionName}</dd></div>}
              </dl>
            </section>
            <section className="profile-card profile-logout-card">
              <h2>Account access</h2><p>Sign out of Csports on this device.</p>
              <button type="button" onClick={handleLogout} disabled={isLoggingOut}>{isLoggingOut ? 'Logging out...' : 'Log out'}</button>
            </section>
          </aside>
        </div>
      </main>
    </>
  )
}

type ProfileFieldProps = {
  id: string
  label: string
  type?: 'text' | 'tel' | 'number'
  value: string
  error?: string
  disabled: boolean
  onChange: (value: string) => void
}

function ProfileField({ id, label, type = 'text', value, error, disabled, onChange }: ProfileFieldProps) {
  return <div className="form-field"><label htmlFor={id}>{label}</label><input id={id} type={type} value={value} disabled={disabled} aria-invalid={Boolean(error)} onChange={(event) => onChange(event.target.value)} />{error && <span className="field-error">{error}</span>}</div>
}

export default ProfilePage
