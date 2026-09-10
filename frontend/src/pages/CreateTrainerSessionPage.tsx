import { useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { ApiError } from '../auth/authApi'
import { useAuth } from '../auth/authContext'
import TrainerNavigation from '../components/TrainerNavigation'
import GoogleMapsLocationField from '../components/GoogleMapsLocationField'
import { createSession, getRegions } from '../trainer/trainerSessionApi'
import type { Region, VerifiedVenueLocation } from '../trainer/types'

const DAYS = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'] as const

type SessionForm = {
  title: string
  description: string
  regionId: string
  startDate: string
  endDate: string
  startTime: string
  durationMinutes: string
  days: string[]
  maxParticipants: string
  price: string
}

const EMPTY_FORM: SessionForm = {
  title: '', description: '', regionId: '', startDate: '', endDate: '',
  startTime: '', durationMinutes: '60', days: [], maxParticipants: '', price: '',
}

function CreateTrainerSessionPage() {
  const navigate = useNavigate()
  const { authenticatedFetch } = useAuth()
  const [form, setForm] = useState<SessionForm>(EMPTY_FORM)
  const [regions, setRegions] = useState<Region[]>([])
  const [venue, setVenue] = useState<VerifiedVenueLocation | null>(null)
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [serverError, setServerError] = useState('')
  const [isLoadingRegions, setIsLoadingRegions] = useState(true)
  const [isSubmitting, setIsSubmitting] = useState(false)

  useEffect(() => {
    const controller = new AbortController()
    async function loadRegions() {
      try {
        setRegions(await getRegions(controller.signal))
      } catch (requestError) {
        if (!controller.signal.aborted) {
          setServerError(requestError instanceof Error ? requestError.message : 'Areas could not be loaded.')
        }
      } finally {
        if (!controller.signal.aborted) setIsLoadingRegions(false)
      }
    }
    void loadRegions()
    return () => controller.abort()
  }, [])

  function updateField(field: keyof SessionForm, value: string | string[]) {
    setForm((current) => ({ ...current, [field]: value }))
    setErrors((current) => ({ ...current, [field]: '' }))
    setServerError('')
  }

  function toggleDay(day: string) {
    updateField('days', form.days.includes(day)
      ? form.days.filter((selectedDay) => selectedDay !== day)
      : [...form.days, day])
  }

  function validateForm() {
    const nextErrors: Record<string, string> = {}
    if (!form.title.trim()) nextErrors.title = 'Enter a session title.'
    if (!form.regionId) nextErrors.regionId = 'Choose the venue area.'
    if (!venue) nextErrors.venue = 'Verify the venue Google Maps link.'
    if (!form.startDate) nextErrors.startDate = 'Choose a start date.'
    if (!form.endDate) nextErrors.endDate = 'Choose an end date.'
    if (form.endDate && form.startDate && form.endDate < form.startDate) nextErrors.endDate = 'End date cannot be before the start date.'
    if (!form.startTime) nextErrors.startTime = 'Choose a start time.'
    if (form.days.length === 0) nextErrors.days = 'Choose at least one training day.'
    if (Number(form.durationMinutes) <= 0) nextErrors.durationMinutes = 'Enter a valid duration.'
    if (Number(form.maxParticipants) <= 0) nextErrors.maxParticipants = 'Enter a valid capacity.'
    if (form.price === '' || Number(form.price) < 0) nextErrors.price = 'Enter a valid price.'
    return nextErrors
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const nextErrors = validateForm()
    setErrors(nextErrors)
    if (Object.keys(nextErrors).length > 0 || !venue) return

    setIsSubmitting(true)
    setServerError('')
    try {
      const createdSession = await createSession(authenticatedFetch, {
        title: form.title.trim(),
        description: form.description.trim(),
        locationName: venue.locationName,
        regionId: Number(form.regionId),
        latitude: venue.latitude,
        longitude: venue.longitude,
        startDate: form.startDate,
        endDate: form.endDate,
        startTime: form.startTime,
        durationMinutes: Number(form.durationMinutes),
        days: form.days,
        maxParticipants: Number(form.maxParticipants),
        price: Number(form.price),
      })
      navigate(`/trainer/sessions/${createdSession.id}/manage`, {
        replace: true,
        state: { message: 'Session created successfully.' },
      })
    } catch (requestError) {
      if (requestError instanceof ApiError) setErrors(requestError.fieldErrors)
      setServerError(requestError instanceof Error ? requestError.message : 'The session could not be created.')
    } finally {
      setIsSubmitting(false)
    }
  }

  const disabled = isSubmitting || isLoadingRegions
  return (
    <>
      <TrainerNavigation />
      <main className="trainer-page">
        <Link className="trainer-back-link" to="/trainer/home">Back to my sessions</Link>
        <section className="trainer-form-card">
          <p className="eyebrow">Create a session</p>
          <h1>Plan your next training</h1>
          <p>Set the schedule, venue, capacity and price athletes will see.</p>

          <form className="trainer-session-form" onSubmit={handleSubmit} noValidate>
            <div className="form-field full-width">
              <label htmlFor="session-title">Session title</label>
              <input id="session-title" value={form.title} onChange={(event) => updateField('title', event.target.value)} disabled={disabled} aria-invalid={Boolean(errors.title)} />
              {errors.title && <span className="field-error">{errors.title}</span>}
            </div>
            <div className="form-field full-width">
              <label htmlFor="session-description">Description</label>
              <textarea id="session-description" value={form.description} onChange={(event) => updateField('description', event.target.value)} disabled={disabled} placeholder="Describe the level, goals and what athletes should expect." />
              {errors.description && <span className="field-error">{errors.description}</span>}
            </div>
            <div className="form-field full-width">
              <label htmlFor="session-region">Area</label>
              <select id="session-region" value={form.regionId} onChange={(event) => updateField('regionId', event.target.value)} disabled={disabled} aria-invalid={Boolean(errors.regionId)}>
                <option value="">Choose the venue area</option>
                {regions.map((region) => <option key={region.id} value={region.id}>{region.name}, {region.city}</option>)}
              </select>
              {errors.regionId && <span className="field-error">{errors.regionId}</span>}
            </div>
            <GoogleMapsLocationField authenticatedFetch={authenticatedFetch} selectedVenue={venue}
              onVerified={(selectedVenue) => { setVenue(selectedVenue); setErrors((current) => ({ ...current, venue: '' })) }}
              onClear={() => setVenue(null)} disabled={disabled} error={errors.venue} />

            <SessionInput id="start-date" label="Start date" type="date" value={form.startDate} error={errors.startDate} disabled={disabled} onChange={(value) => updateField('startDate', value)} />
            <SessionInput id="end-date" label="End date" type="date" value={form.endDate} error={errors.endDate} disabled={disabled} onChange={(value) => updateField('endDate', value)} />
            <SessionInput id="start-time" label="Start time" type="time" value={form.startTime} error={errors.startTime} disabled={disabled} onChange={(value) => updateField('startTime', value)} />
            <SessionInput id="duration" label="Duration in minutes" type="number" value={form.durationMinutes} error={errors.durationMinutes} disabled={disabled} min="1" onChange={(value) => updateField('durationMinutes', value)} />

            <fieldset className="trainer-days full-width">
              <legend>Training days</legend>
              <div>{DAYS.map((day) => <label key={day}><input type="checkbox" checked={form.days.includes(day)} onChange={() => toggleDay(day)} disabled={disabled} /><span>{day.slice(0, 3)}</span></label>)}</div>
              {errors.days && <span className="field-error">{errors.days}</span>}
            </fieldset>

            <SessionInput id="capacity" label="Maximum participants" type="number" value={form.maxParticipants} error={errors.maxParticipants} disabled={disabled} min="1" onChange={(value) => updateField('maxParticipants', value)} />
            <SessionInput id="price" label="Price in EGP" type="number" value={form.price} error={errors.price} disabled={disabled} min="0" step="0.01" onChange={(value) => updateField('price', value)} />

            <button className="trainer-primary-action full-width" type="submit" disabled={disabled}>{isSubmitting ? 'Creating session...' : 'Create session'}</button>
          </form>
          {serverError && <p className="trainer-form-error" role="alert">{serverError}</p>}
        </section>
      </main>
    </>
  )
}

type SessionInputProps = {
  id: string
  label: string
  type: 'date' | 'time' | 'number'
  value: string
  error?: string
  disabled: boolean
  min?: string
  step?: string
  onChange: (value: string) => void
}

function SessionInput({ id, label, type, value, error, disabled, min, step, onChange }: SessionInputProps) {
  return (
    <div className="form-field">
      <label htmlFor={id}>{label}</label>
      <input id={id} type={type} value={value} min={min} step={step} onChange={(event) => onChange(event.target.value)} disabled={disabled} aria-invalid={Boolean(error)} />
      {error && <span className="field-error">{error}</span>}
    </div>
  )
}

export default CreateTrainerSessionPage
