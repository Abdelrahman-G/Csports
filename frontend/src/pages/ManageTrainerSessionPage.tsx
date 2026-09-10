import { useEffect, useState, type FormEvent } from 'react'
import { Link, useLocation, useParams } from 'react-router-dom'
import { ApiError } from '../auth/authApi'
import { useAuth } from '../auth/authContext'
import TrainerNavigation from '../components/TrainerNavigation'
import GoogleMapsLocationField from '../components/GoogleMapsLocationField'
import { getSessionDetails } from '../discovery/discoveryApi'
import type { TrainingSessionDetails } from '../discovery/types'
import {
  cancelSession,
  getParticipants,
  getRegions,
  restoreSession,
  updateSession,
} from '../trainer/trainerSessionApi'
import type { Region, SessionParticipant, UpdateTrainingSessionRequest, VerifiedVenueLocation } from '../trainer/types'

const DAYS = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'] as const

type EditForm = {
  title: string
  description: string
  regionId: string
  startDate: string
  endDate: string
  startTime: string
  durationMinutes: string
  days: string[]
  maxParticipants: string
  reason: string
}

function formFromSession(session: TrainingSessionDetails): EditForm {
  return {
    title: session.title,
    description: session.description ?? '',
    regionId: String(session.regionId),
    startDate: session.startDate,
    endDate: session.endDate,
    startTime: session.startTime.slice(0, 5),
    durationMinutes: String(session.durationMinutes),
    days: session.days,
    maxParticipants: String(session.maxParticipants),
    reason: '',
  }
}

function ManageTrainerSessionPage() {
  const { authenticatedFetch } = useAuth()
  const { sessionId } = useParams()
  const routeLocation = useLocation()
  const numericSessionId = Number(sessionId)
  const validSessionId = Number.isInteger(numericSessionId) && numericSessionId > 0
  const [session, setSession] = useState<TrainingSessionDetails | null>(null)
  const [form, setForm] = useState<EditForm | null>(null)
  const [regions, setRegions] = useState<Region[]>([])
  const [participants, setParticipants] = useState<SessionParticipant[]>([])
  const [venue, setVenue] = useState<VerifiedVenueLocation | null>(null)
  const [isEditing, setIsEditing] = useState(false)
  const [isLoading, setIsLoading] = useState(true)
  const [isSaving, setIsSaving] = useState(false)
  const [isChangingStatus, setIsChangingStatus] = useState(false)
  const [cancelReason, setCancelReason] = useState('')
  const [showCancellation, setShowCancellation] = useState(false)
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [error, setError] = useState('')
  const [message, setMessage] = useState(
    (routeLocation.state as { message?: string } | null)?.message ?? '',
  )
  const [reloadVersion, setReloadVersion] = useState(0)

  useEffect(() => {
    if (!validSessionId) return
    const controller = new AbortController()

    async function loadSessionManagement() {
      setIsLoading(true)
      setError('')
      try {
        const [loadedSession, loadedRegions, loadedParticipants] = await Promise.all([
          getSessionDetails(numericSessionId, controller.signal),
          getRegions(controller.signal),
          getParticipants(authenticatedFetch, numericSessionId, controller.signal),
        ])
        setSession(loadedSession)
        setForm(formFromSession(loadedSession))
        setRegions(loadedRegions)
        setParticipants(loadedParticipants.content)
        setVenue(null)
      } catch (requestError) {
        if (!controller.signal.aborted) {
          setError(requestError instanceof Error ? requestError.message : 'Session management could not be loaded.')
        }
      } finally {
        if (!controller.signal.aborted) setIsLoading(false)
      }
    }

    void loadSessionManagement()
    return () => controller.abort()
  }, [authenticatedFetch, numericSessionId, reloadVersion, validSessionId])

  function updateField(field: keyof EditForm, value: string | string[]) {
    setForm((current) => current ? { ...current, [field]: value } : current)
    setErrors((current) => ({ ...current, [field]: '' }))
    setError('')
    setMessage('')
  }

  function toggleDay(day: string) {
    if (!form) return
    updateField('days', form.days.includes(day)
      ? form.days.filter((selectedDay) => selectedDay !== day)
      : [...form.days, day])
  }

  function startEditing() {
    if (!session) return
    setForm(formFromSession(session))
    setVenue(null)
    setErrors({})
    setError('')
    setMessage('')
    setIsEditing(true)
  }

  function stopEditing() {
    if (!session || isSaving) return
    setForm(formFromSession(session))
    setVenue(null)
    setErrors({})
    setError('')
    setIsEditing(false)
  }

  async function handleUpdate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!session || !form || isSaving) return

    const nextErrors: Record<string, string> = {}
    if (!form.reason.trim()) nextErrors.reason = 'Explain why this session is being updated.'
    if (!form.title.trim()) nextErrors.title = 'Enter a session title.'
    if (form.days.length === 0) nextErrors.days = 'Choose at least one training day.'
    setErrors(nextErrors)
    if (Object.keys(nextErrors).length > 0) return

    const request: UpdateTrainingSessionRequest = { reason: form.reason.trim() }
    if (form.title.trim() !== session.title) request.title = form.title.trim()
    if (form.description.trim() !== (session.description ?? '')) request.description = form.description.trim()
    if (Number(form.regionId) !== session.regionId) request.regionId = Number(form.regionId)
    if (form.startDate !== session.startDate) request.startDate = form.startDate
    if (form.endDate !== session.endDate) request.endDate = form.endDate
    if (form.startTime !== session.startTime.slice(0, 5)) request.startTime = form.startTime
    if (Number(form.durationMinutes) !== session.durationMinutes) request.durationMinutes = Number(form.durationMinutes)
    if ([...form.days].sort().join() !== [...session.days].sort().join()) request.days = form.days
    if (Number(form.maxParticipants) !== session.maxParticipants) request.maxParticipants = Number(form.maxParticipants)
    if (venue) {
      request.locationName = venue.locationName
      request.latitude = venue.latitude
      request.longitude = venue.longitude
    }

    if (Object.keys(request).length === 1) {
      setError('Change at least one session field before saving.')
      return
    }

    setIsSaving(true)
    setError('')
    try {
      await updateSession(authenticatedFetch, numericSessionId, request)
      setMessage('Session updated successfully.')
      setIsEditing(false)
      setReloadVersion((current) => current + 1)
    } catch (requestError) {
      if (requestError instanceof ApiError) setErrors(requestError.fieldErrors)
      setError(requestError instanceof Error ? requestError.message : 'The session could not be updated.')
    } finally {
      setIsSaving(false)
    }
  }

  async function handleCancellation() {
    if (!cancelReason.trim() || isChangingStatus) return
    setIsChangingStatus(true)
    setError('')
    try {
      await cancelSession(authenticatedFetch, numericSessionId, cancelReason.trim())
      setMessage('Session cancelled. Confirmed participants were notified.')
      setShowCancellation(false)
      setCancelReason('')
      setReloadVersion((current) => current + 1)
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : 'The session could not be cancelled.')
    } finally {
      setIsChangingStatus(false)
    }
  }

  async function handleRestore() {
    if (isChangingStatus) return
    setIsChangingStatus(true)
    setError('')
    try {
      await restoreSession(authenticatedFetch, numericSessionId)
      setMessage('Session restored. Previous cancelled bookings remain cancelled.')
      setReloadVersion((current) => current + 1)
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : 'The session could not be restored.')
    } finally {
      setIsChangingStatus(false)
    }
  }

  if (!validSessionId) return <TrainerState message="This session address is invalid." />
  if (isLoading) return <TrainerState message="Loading session management..." />
  if (!session || !form) return <TrainerState message={error || 'This session could not be loaded.'} />

  const scheduled = session.status === 'SCHEDULED'
  return (
    <>
      <TrainerNavigation />
      <main className="trainer-page">
        <Link className="trainer-back-link" to="/trainer/home">Back to my sessions</Link>
        <section className="trainer-manage-heading">
          <div><span className={`trainer-status ${session.status.toLowerCase()}`}>{session.status}</span><h1>{session.title}</h1><p>{session.locationName}, {session.regionName}</p></div>
          <div className="trainer-status-actions">
            {scheduled && !isEditing && <button className="trainer-primary-action" type="button" onClick={startEditing}>Update session</button>}
            {scheduled ? <button className="danger-action" type="button" onClick={() => setShowCancellation(true)}>Cancel session</button>
              : session.status === 'CANCELLED' ? <button className="trainer-primary-action" type="button" onClick={handleRestore} disabled={isChangingStatus}>Restore session</button> : null}
          </div>
        </section>

        {message && (
          <div className="trainer-feedback-banner" role="status">
            <span aria-hidden="true">✓</span>
            <div><strong>Success</strong><p>{message}</p></div>
          </div>
        )}
        {error && <p className="trainer-form-error" role="alert">{error}</p>}
        {session.status === 'CANCELLED' && <p className="trainer-cancelled-note"><strong>Cancellation reason:</strong> {session.cancellationReason}</p>}

        {showCancellation && (
          <section className="trainer-cancel-panel">
            <h2>Cancel this session?</h2>
            <p>All confirmed bookings will be cancelled and participants will receive your reason.</p>
            <label htmlFor="cancel-reason">Cancellation reason</label>
            <textarea id="cancel-reason" value={cancelReason} onChange={(event) => setCancelReason(event.target.value)} maxLength={500} />
            <div><button type="button" onClick={() => setShowCancellation(false)} disabled={isChangingStatus}>Keep session</button><button className="danger-action" type="button" onClick={handleCancellation} disabled={!cancelReason.trim() || isChangingStatus}>{isChangingStatus ? 'Cancelling...' : 'Confirm cancellation'}</button></div>
          </section>
        )}

        <div className="trainer-management-layout">
          {isEditing ? (
            <section className="trainer-form-card">
              <div className="trainer-editor-heading">
                <div><p className="eyebrow">Update session</p><h2>What would you like to change?</h2><p>Only edit the information that needs updating.</p></div>
              </div>
              <form className="trainer-update-form" onSubmit={handleUpdate} noValidate>
                <section className="trainer-editor-section">
                  <div className="trainer-editor-section-heading"><span>1</span><div><h3>Basics</h3><p>How athletes recognize this session.</p></div></div>
                  <div className="trainer-editor-grid">
                    <EditTextField id="manage-title" label="Title" value={form.title} error={errors.title} disabled={!scheduled || isSaving} onChange={(value) => updateField('title', value)} />
                    <div className="form-field full-width"><label htmlFor="manage-description">Description</label><textarea id="manage-description" value={form.description} onChange={(event) => updateField('description', event.target.value)} disabled={!scheduled || isSaving} /></div>
                  </div>
                </section>

                <section className="trainer-editor-section">
                  <div className="trainer-editor-section-heading"><span>2</span><div><h3>Venue</h3><p>Where athletes should attend.</p></div></div>
                  <div className="trainer-editor-grid">
                    <div className="form-field full-width"><label htmlFor="manage-region">Area</label><select id="manage-region" value={form.regionId} onChange={(event) => updateField('regionId', event.target.value)} disabled={!scheduled || isSaving}>{regions.map((region) => <option key={region.id} value={region.id}>{region.name}, {region.city}</option>)}</select></div>
                    <div className="current-venue full-width"><span>Current venue</span><strong>{session.locationName}</strong><p>Enter another venue only when the location is changing.</p></div>
                    <GoogleMapsLocationField authenticatedFetch={authenticatedFetch} selectedVenue={venue} onVerified={setVenue} onClear={() => setVenue(null)} disabled={!scheduled || isSaving} />
                  </div>
                </section>

                <section className="trainer-editor-section">
                  <div className="trainer-editor-section-heading"><span>3</span><div><h3>Schedule and capacity</h3><p>When the session runs and how many athletes can join.</p></div></div>
                  <div className="trainer-editor-grid">
                    <EditTextField id="manage-start-date" label="Start date" type="date" value={form.startDate} disabled={!scheduled || isSaving} onChange={(value) => updateField('startDate', value)} />
                    <EditTextField id="manage-end-date" label="End date" type="date" value={form.endDate} disabled={!scheduled || isSaving} onChange={(value) => updateField('endDate', value)} />
                    <EditTextField id="manage-start-time" label="Start time" type="time" value={form.startTime} disabled={!scheduled || isSaving} onChange={(value) => updateField('startTime', value)} />
                    <EditTextField id="manage-duration" label="Duration in minutes" type="number" value={form.durationMinutes} disabled={!scheduled || isSaving} onChange={(value) => updateField('durationMinutes', value)} />
                    <fieldset className="trainer-days full-width" disabled={!scheduled || isSaving}><legend>Training days</legend><div>{DAYS.map((day) => <label key={day}><input type="checkbox" checked={form.days.includes(day)} onChange={() => toggleDay(day)} /><span>{day.slice(0, 3)}</span></label>)}</div>{errors.days && <span className="field-error">{errors.days}</span>}</fieldset>
                    <EditTextField id="manage-capacity" label="Maximum participants" type="number" value={form.maxParticipants} disabled={!scheduled || isSaving} onChange={(value) => updateField('maxParticipants', value)} />
                    <div className="form-field"><label>Price</label><input value={`${session.price} EGP`} disabled /><span className="field-hint">Price is fixed after creation.</span></div>
                  </div>
                </section>

                <section className="trainer-editor-section trainer-reason-section">
                  <div className="trainer-editor-section-heading"><span>4</span><div><h3>Reason for update</h3><p>Give athletes clear context for the change.</p></div></div>
                  <div className="form-field"><textarea id="update-reason" aria-label="Reason for update" value={form.reason} onChange={(event) => updateField('reason', event.target.value)} disabled={!scheduled || isSaving} maxLength={500} placeholder="For example: The venue is unavailable, so training has moved nearby." />{errors.reason && <span className="field-error">{errors.reason}</span>}</div>
                </section>

                <div className="trainer-editor-actions">
                  <button type="button" onClick={stopEditing} disabled={isSaving}>Discard changes</button>
                  <button className="trainer-primary-action" type="submit" disabled={isSaving}>{isSaving ? 'Saving changes...' : 'Save changes'}</button>
                </div>
              </form>
            </section>
          ) : (
            <SessionOverview session={session} />
          )}

          <aside className="trainer-participants-card">
            <h2>Participants</h2><p>{session.currentParticipants} of {session.maxParticipants} places booked</p>
            {participants.length === 0 ? <p className="field-hint">No confirmed participants yet.</p> : <ul>{participants.map((participant) => <li key={participant.userId}><strong>{participant.name}</strong><span>{participant.email}</span><span>{participant.phoneNumber}</span></li>)}</ul>}
          </aside>
        </div>
      </main>
    </>
  )
}

function SessionOverview({ session }: { session: TrainingSessionDetails }) {
  return (
    <section className="trainer-details-card">
      <h2>Session details</h2>
      {session.description && <p className="trainer-session-description">{session.description}</p>}

      <div className="trainer-detail-sections">
        <section>
          <h3>Schedule</h3>
          <dl>
            <div><dt>Dates</dt><dd>{session.startDate} to {session.endDate}</dd></div>
            <div><dt>Time</dt><dd>{session.startTime.slice(0, 5)}</dd></div>
            <div><dt>Duration</dt><dd>{session.durationMinutes} minutes</dd></div>
            <div><dt>Training days</dt><dd>{session.days.map((day) => day.slice(0, 3)).join(', ')}</dd></div>
          </dl>
        </section>
        <section>
          <h3>Venue</h3>
          <dl>
            <div><dt>Name</dt><dd>{session.locationName}</dd></div>
            <div><dt>Area</dt><dd>{session.regionName}</dd></div>
          </dl>
        </section>
        <section>
          <h3>Booking</h3>
          <dl>
            <div><dt>Participants</dt><dd>{session.currentParticipants} / {session.maxParticipants}</dd></div>
            <div><dt>Price</dt><dd>{session.price} EGP</dd></div>
          </dl>
        </section>
      </div>
    </section>
  )
}

type EditTextFieldProps = { id: string; label: string; type?: 'text' | 'date' | 'time' | 'number'; value: string; error?: string; disabled: boolean; onChange: (value: string) => void }
function EditTextField({ id, label, type = 'text', value, error, disabled, onChange }: EditTextFieldProps) {
  return <div className="form-field"><label htmlFor={id}>{label}</label><input id={id} type={type} value={value} onChange={(event) => onChange(event.target.value)} disabled={disabled} aria-invalid={Boolean(error)} />{error && <span className="field-error">{error}</span>}</div>
}

function TrainerState({ message }: { message: string }) {
  return <><TrainerNavigation /><main className="trainer-page"><section className="trainer-state"><p>{message}</p><Link to="/trainer/home">Back to my sessions</Link></section></main></>
}

export default ManageTrainerSessionPage
