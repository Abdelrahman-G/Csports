import { useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { ApiError } from '../auth/authApi'
import {
  getRegions,
  getSports,
  registerTrainer,
  registerUser,
} from '../auth/registrationApi'
import type { Region, Sport } from '../auth/types'
import PasswordInput from '../components/PasswordInput'

type AccountType = 'user' | 'trainer'

type RegistrationPageProps = {
  accountType: AccountType
}

type Coordinates = {
  latitude: number
  longitude: number
  accuracy: number
}

type RegistrationForm = {
  name: string
  email: string
  phoneNumber: string
  password: string
  age: string
  regionId: string
  bio: string
  experienceYears: string
  sportId: string
}

type RegistrationField = keyof RegistrationForm

type RegistrationErrors = {
  name?: string
  email?: string
  phoneNumber?: string
  password?: string
  age?: string
  regionId?: string
  location?: string
  bio?: string
  experienceYears?: string
  sportId?: string
}

type TextInputProps = {
  id: RegistrationField
  label: string
  value: string
  onChange: (value: string) => void
  error?: string
  type?: 'text' | 'email' | 'tel' | 'password' | 'number'
  autoComplete?: string
  placeholder?: string
  min?: number
  max?: number
  disabled: boolean
  fullWidth?: boolean
}

const EMPTY_FORM: RegistrationForm = {
  name: '',
  email: '',
  phoneNumber: '',
  password: '',
  age: '',
  regionId: '',
  bio: '',
  experienceYears: '',
  sportId: '',
}

const EGYPTIAN_MOBILE_PATTERN = /^01[0125][0-9]{8}$/
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const MIN_LATITUDE = 29.75
const MAX_LATITUDE = 30.35
const MIN_LONGITUDE = 30.75
const MAX_LONGITUDE = 31.75

function TextInput({
  id,
  label,
  value,
  onChange,
  error,
  type = 'text',
  autoComplete,
  placeholder,
  min,
  max,
  disabled,
  fullWidth = false,
}: TextInputProps) {
  const errorId = `${id}-error`

  return (
    <div className={`form-field${fullWidth ? ' full-width' : ''}`}>
      <label htmlFor={id}>{label}</label>
      <input
        id={id}
        name={id}
        type={type}
        value={value}
        onChange={(event) => onChange(event.target.value)}
        autoComplete={autoComplete}
        placeholder={placeholder}
        min={min}
        max={max}
        disabled={disabled}
        aria-invalid={Boolean(error)}
        aria-describedby={error ? errorId : undefined}
      />
      {error && (
        <span className="field-error" id={errorId}>
          {error}
        </span>
      )}
    </div>
  )
}

function RegistrationPage({ accountType }: RegistrationPageProps) {
  const navigate = useNavigate()
  const isTrainer = accountType === 'trainer'
  const [form, setForm] = useState<RegistrationForm>(EMPTY_FORM)
  const [coordinates, setCoordinates] = useState<Coordinates | null>(null)
  const [regions, setRegions] = useState<Region[]>([])
  const [sports, setSports] = useState<Sport[]>([])
  const [errors, setErrors] = useState<RegistrationErrors>({})
  const [serverError, setServerError] = useState('')
  const [isLoadingOptions, setIsLoadingOptions] = useState(true)
  const [isLocating, setIsLocating] = useState(false)
  const [isSubmitting, setIsSubmitting] = useState(false)

  useEffect(() => {
    let shouldIgnoreResult = false

    async function loadSignupOptions() {
      setIsLoadingOptions(true)

      try {
        const loadedRegions = await getRegions()
        const loadedSports = isTrainer ? await getSports() : []

        if (!shouldIgnoreResult) {
          setRegions(loadedRegions)
          setSports(loadedSports)
          setServerError('')
        }
      } catch (error) {
        if (!shouldIgnoreResult) {
          setServerError(
            error instanceof Error
              ? error.message
              : 'Could not load the signup options.',
          )
        }
      } finally {
        if (!shouldIgnoreResult) {
          setIsLoadingOptions(false)
        }
      }
    }

    void loadSignupOptions()

    return () => {
      shouldIgnoreResult = true
    }
  }, [isTrainer])

  function updateField(field: RegistrationField, value: string) {
    setForm((current) => ({ ...current, [field]: value }))
    setErrors((current) => ({ ...current, [field]: undefined }))
    setServerError('')
  }

  function captureCurrentLocation() {
    setErrors((current) => ({ ...current, location: undefined }))
    setServerError('')

    if (!navigator.geolocation) {
      setErrors((current) => ({
        ...current,
        location: 'This browser does not support location access.',
      }))
      return
    }

    setIsLocating(true)

    navigator.geolocation.getCurrentPosition(
      (position) => {
        const { latitude, longitude, accuracy } = position.coords
        const insideServiceArea =
          latitude >= MIN_LATITUDE &&
          latitude <= MAX_LATITUDE &&
          longitude >= MIN_LONGITUDE &&
          longitude <= MAX_LONGITUDE

        if (!insideServiceArea) {
          setCoordinates(null)
          setErrors((current) => ({
            ...current,
            location: 'Your current location is outside Cairo and Giza.',
          }))
        } else {
          setCoordinates({ latitude, longitude, accuracy })
        }

        setIsLocating(false)
      },
      (error) => {
        const message =
          error.code === error.PERMISSION_DENIED
            ? 'Location permission was denied. Please allow it and try again.'
            : 'Your current location could not be detected. Please try again.'

        setCoordinates(null)
        setErrors((current) => ({ ...current, location: message }))
        setIsLocating(false)
      },
      {
        enableHighAccuracy: true,
        timeout: 10000,
        maximumAge: 300000,
      },
    )
  }

  function validateForm(): RegistrationErrors {
    const nextErrors: RegistrationErrors = {}
    const age = Number(form.age)
    const minimumAge = isTrainer ? 18 : 13

    if (form.name.trim().length < 2) {
      nextErrors.name = 'Name must contain at least 2 characters.'
    }

    if (!EMAIL_PATTERN.test(form.email.trim())) {
      nextErrors.email = 'Enter a valid email address.'
    }

    if (!EGYPTIAN_MOBILE_PATTERN.test(form.phoneNumber.trim())) {
      nextErrors.phoneNumber = 'Enter an Egyptian number such as 01123456789.'
    }

    if (form.password.length < 8 || form.password.length > 72) {
      nextErrors.password = 'Password must contain between 8 and 72 characters.'
    }

    if (!form.age || !Number.isInteger(age) || age < minimumAge || age > 100) {
      nextErrors.age = `Age must be between ${minimumAge} and 100.`
    }

    if (!form.regionId) {
      nextErrors.regionId = 'Choose your region.'
    }

    if (!coordinates) {
      nextErrors.location = 'Allow location access to capture your current location.'
    }

    if (isTrainer) {
      const experienceYears = Number(form.experienceYears)

      if (form.bio.length > 1000) {
        nextErrors.bio = 'Bio must not exceed 1000 characters.'
      }

      if (
        form.experienceYears === '' ||
        !Number.isInteger(experienceYears) ||
        experienceYears < 0 ||
        experienceYears > 80
      ) {
        nextErrors.experienceYears = 'Experience must be between 0 and 80 years.'
      }

      if (!form.sportId) {
        nextErrors.sportId = 'Choose your sport.'
      }
    }

    return nextErrors
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    const nextErrors = validateForm()
    setErrors(nextErrors)

    if (Object.keys(nextErrors).length > 0 || !coordinates) {
      setServerError('')
      return
    }

    setIsSubmitting(true)
    setServerError('')

    const commonRequest = {
      name: form.name.trim(),
      email: form.email.trim().toLowerCase(),
      phoneNumber: form.phoneNumber.trim(),
      password: form.password,
      age: Number(form.age),
      regionId: Number(form.regionId),
      latitude: coordinates.latitude,
      longitude: coordinates.longitude,
    }

    try {
      if (isTrainer) {
        await registerTrainer({
          ...commonRequest,
          bio: form.bio.trim(),
          experienceYears: Number(form.experienceYears),
          sportId: Number(form.sportId),
        })
      } else {
        await registerUser(commonRequest)
      }

      navigate('/login', {
        replace: true,
        state: {
          registrationMessage: "You're in. Log in and take your first step.",
        },
      })
    } catch (error) {
      if (error instanceof ApiError) {
        setErrors({
          name: error.fieldErrors.name,
          email: error.fieldErrors.email,
          phoneNumber: error.fieldErrors.phoneNumber,
          password: error.fieldErrors.password,
          age: error.fieldErrors.age,
          regionId: error.fieldErrors.regionId,
          location:
            error.fieldErrors.latitude ?? error.fieldErrors.longitude,
          bio: error.fieldErrors.bio,
          experienceYears: error.fieldErrors.experienceYears,
          sportId: error.fieldErrors.sportId,
        })
      }

      setServerError(
        error instanceof Error
          ? error.message
          : 'Registration failed. Please try again.',
      )
    } finally {
      setIsSubmitting(false)
    }
  }

  const formDisabled = isSubmitting || isLoadingOptions
  const locationMapUrl = coordinates
    ? `https://www.google.com/maps?q=${coordinates.latitude},${coordinates.longitude}`
    : null

  return (
    <main className="placeholder-page registration-page">
      <section className="placeholder-card registration-card">
        <p className="eyebrow">
          {isTrainer ? 'Lead from experience' : 'Your next level starts here'}
        </p>
        <h1>{isTrainer ? 'Build your coach profile' : 'Build your athlete profile'}</h1>
        <p>
          {isTrainer
            ? 'Put your experience where motivated athletes can find it.'
            : 'Tell us where you are and bring the right training closer.'}
        </p>

        <form
          className="auth-form registration-form"
          onSubmit={handleSubmit}
          noValidate
        >
          <TextInput
            id="name"
            label="Full name"
            value={form.name}
            onChange={(value) => updateField('name', value)}
            error={errors.name}
            autoComplete="name"
            disabled={formDisabled}
          />
          <TextInput
            id="email"
            label="Email"
            type="email"
            value={form.email}
            onChange={(value) => updateField('email', value)}
            error={errors.email}
            autoComplete="email"
            disabled={formDisabled}
          />
          <TextInput
            id="phoneNumber"
            label="Egyptian mobile number"
            type="tel"
            value={form.phoneNumber}
            onChange={(value) => updateField('phoneNumber', value)}
            error={errors.phoneNumber}
            autoComplete="tel"
            placeholder="01123456789"
            disabled={formDisabled}
          />
          <TextInput
            id="age"
            label="Age"
            type="number"
            value={form.age}
            onChange={(value) => updateField('age', value)}
            error={errors.age}
            min={isTrainer ? 18 : 13}
            max={100}
            disabled={formDisabled}
          />
          <PasswordInput
            id="password"
            label="Password"
            value={form.password}
            onChange={(value) => updateField('password', value)}
            error={errors.password}
            autoComplete="new-password"
            disabled={formDisabled}
            fullWidth
          />

          {isTrainer && (
            <>
              <div className="form-field">
                <label htmlFor="sportId">What do you coach?</label>
                <select
                  id="sportId"
                  name="sportId"
                  value={form.sportId}
                  onChange={(event) => updateField('sportId', event.target.value)}
                  disabled={formDisabled}
                  aria-invalid={Boolean(errors.sportId)}
                  aria-describedby={errors.sportId ? 'sportId-error' : undefined}
                >
                  <option value="">Select your sport</option>
                  {sports.map((sport) => (
                    <option key={sport.id} value={sport.id}>
                      {sport.name}
                    </option>
                  ))}
                </select>
                {errors.sportId && (
                  <span className="field-error" id="sportId-error">
                    {errors.sportId}
                  </span>
                )}
              </div>
              <TextInput
                id="experienceYears"
                label="Years of experience"
                type="number"
                value={form.experienceYears}
                onChange={(value) => updateField('experienceYears', value)}
                error={errors.experienceYears}
                min={0}
                max={80}
                disabled={formDisabled}
              />
              <div className="form-field full-width">
                <label htmlFor="bio">Your coaching story (optional)</label>
                <span className="field-hint" id="bio-help">
                  A thoughtful introduction helps your profile stand out and
                  gives athletes a reason to train with you.
                </span>
                <textarea
                  id="bio"
                  name="bio"
                  value={form.bio}
                  onChange={(event) => updateField('bio', event.target.value)}
                  placeholder="I help beginner swimmers build confidence and improve their technique through structured, supportive sessions."
                  maxLength={1000}
                  disabled={formDisabled}
                  aria-invalid={Boolean(errors.bio)}
                  aria-describedby={errors.bio ? 'bio-help bio-error' : 'bio-help'}
                />
                {errors.bio && (
                  <span className="field-error" id="bio-error">
                    {errors.bio}
                  </span>
                )}
              </div>
            </>
          )}

          <fieldset className="training-location-section full-width">
            <legend>Where will you train?</legend>
            <p>Choose your region, then confirm your current location.</p>

            <div className="training-location-grid">
              <div className="form-field">
                <label htmlFor="regionId">Region</label>
                <select
                  id="regionId"
                  name="regionId"
                  value={form.regionId}
                  onChange={(event) => updateField('regionId', event.target.value)}
                  disabled={formDisabled}
                  aria-invalid={Boolean(errors.regionId)}
                  aria-describedby={errors.regionId ? 'regionId-error' : undefined}
                >
                  <option value="">
                    {isLoadingOptions ? 'Loading regions...' : 'Choose your region'}
                  </option>
                  {regions.map((region) => (
                    <option key={region.id} value={region.id}>
                      {region.name}, {region.city}
                    </option>
                  ))}
                </select>
                {errors.regionId && (
                  <span className="field-error" id="regionId-error">
                    {errors.regionId}
                  </span>
                )}
              </div>

              <div className="location-field">
                <div>
                  <strong>Current location</strong>
                  <span>
                    We use this to show sessions close to you.
                  </span>
                </div>
                <button
                  className="location-button"
                  type="button"
                  onClick={captureCurrentLocation}
                  disabled={isLocating || isSubmitting}
                >
                  {isLocating
                    ? 'Detecting location...'
                    : coordinates
                      ? 'Update location'
                      : 'Use my location'}
                </button>
                {coordinates && locationMapUrl && (
                  <div className="location-result" role="status">
                    <span>
                      Location detected within about {Math.round(coordinates.accuracy)} metres.
                    </span>
                    <a href={locationMapUrl} target="_blank" rel="noreferrer">
                      Check it on Google Maps
                    </a>
                  </div>
                )}
                {errors.location && (
                  <span className="field-error" role="alert">
                    {errors.location}
                  </span>
                )}
              </div>
            </div>
          </fieldset>

          <button
            className="submit-button full-width"
            type="submit"
            disabled={formDisabled || isLocating}
          >
            {isSubmitting
              ? 'Building your profile...'
              : isTrainer
                ? 'Start coaching'
                : 'Start my journey'}
          </button>
        </form>

        {serverError && (
          <p className="form-error-message" role="alert">
            {serverError}
          </p>
        )}

        <div className="auth-links">
          <Link to="/signup">Choose a different path</Link>
          <Link className="back-link" to="/login">
            Log in instead
          </Link>
        </div>
      </section>
    </main>
  )
}

export default RegistrationPage
