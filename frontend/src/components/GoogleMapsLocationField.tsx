import { useState } from 'react'
import type { AuthenticatedFetch } from '../auth/authContext'
import { resolveGoogleMapsLocation } from '../trainer/trainerSessionApi'
import type { VerifiedVenueLocation } from '../trainer/types'

type GoogleMapsLocationFieldProps = {
  authenticatedFetch: AuthenticatedFetch
  selectedVenue: VerifiedVenueLocation | null
  onVerified: (venue: VerifiedVenueLocation) => void
  onClear: () => void
  disabled?: boolean
  error?: string
}

function GoogleMapsLocationField({
  authenticatedFetch,
  selectedVenue,
  onVerified,
  onClear,
  disabled = false,
  error,
}: GoogleMapsLocationFieldProps) {
  const [venueName, setVenueName] = useState('')
  const [googleMapsUrl, setGoogleMapsUrl] = useState('')
  const [isVerifying, setIsVerifying] = useState(false)
  const [verificationError, setVerificationError] = useState('')

  function clearVerification() {
    setVerificationError('')
    onClear()
  }

  async function verifyLocation() {
    const trimmedVenueName = venueName.trim()
    const trimmedUrl = googleMapsUrl.trim()
    if (!trimmedVenueName || !trimmedUrl || isVerifying) {
      setVerificationError('Enter the venue name and its Google Maps link.')
      return
    }

    setIsVerifying(true)
    setVerificationError('')
    try {
      const location = await resolveGoogleMapsLocation(authenticatedFetch, trimmedUrl)
      onVerified({ ...location, locationName: trimmedVenueName })
    } catch (requestError) {
      onClear()
      setVerificationError(
        requestError instanceof Error
          ? requestError.message
          : 'The Google Maps link could not be verified.',
      )
    } finally {
      setIsVerifying(false)
    }
  }

  return (
    <div className="form-field full-width maps-location-field">
      <label htmlFor="venue-name">Training venue</label>
      <span className="field-hint" id="venue-location-help">
        Enter the venue name, then paste its shared Google Maps link.
      </span>
      <input
        id="venue-name"
        value={venueName}
        onChange={(event) => {
          setVenueName(event.target.value)
          clearVerification()
        }}
        placeholder="Cairo International Stadium"
        disabled={disabled}
        aria-invalid={Boolean(error)}
      />
      <div className="maps-verification-row">
        <input
          aria-label="Google Maps link"
          type="url"
          value={googleMapsUrl}
          onChange={(event) => {
            setGoogleMapsUrl(event.target.value)
            clearVerification()
          }}
          placeholder="Paste a Google Maps place or pin link"
          disabled={disabled}
          aria-describedby="venue-location-help"
        />
        <button
          type="button"
          onClick={verifyLocation}
          disabled={disabled || isVerifying}
        >
          {isVerifying ? 'Verifying...' : 'Verify location'}
        </button>
      </div>

      {verificationError && <span className="field-error">{verificationError}</span>}
      {error && <span className="field-error">{error}</span>}

      {selectedVenue && (
        <div className="verified-location" aria-live="polite">
          <div>
            <strong>Location verified</strong>
            <span>{selectedVenue.locationName}</span>
          </div>
          <a href={selectedVenue.normalizedGoogleMapsUrl} target="_blank" rel="noreferrer">
            Check on map
          </a>
        </div>
      )}
    </div>
  )
}

export default GoogleMapsLocationField
