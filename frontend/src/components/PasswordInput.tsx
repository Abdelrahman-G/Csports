import { useState } from 'react'

type PasswordInputProps = {
  id: string
  label: string
  value: string
  onChange: (value: string) => void
  error?: string
  autoComplete: 'current-password' | 'new-password'
  disabled: boolean
  fullWidth?: boolean
}

function PasswordInput({
  id,
  label,
  value,
  onChange,
  error,
  autoComplete,
  disabled,
  fullWidth = false,
}: PasswordInputProps) {
  const [isPasswordVisible, setIsPasswordVisible] = useState(false)
  const errorId = `${id}-error`

  return (
    <div className={`form-field${fullWidth ? ' full-width' : ''}`}>
      <label htmlFor={id}>{label}</label>
      <div className="password-input-wrapper">
        <input
          id={id}
          name={id}
          type={isPasswordVisible ? 'text' : 'password'}
          autoComplete={autoComplete}
          value={value}
          onChange={(event) => onChange(event.target.value)}
          disabled={disabled}
          aria-invalid={Boolean(error)}
          aria-describedby={error ? errorId : undefined}
        />
        <button
          className="password-visibility-button"
          type="button"
          onClick={() => setIsPasswordVisible((current) => !current)}
          disabled={disabled}
          aria-label={isPasswordVisible ? 'Hide password' : 'Show password'}
          aria-pressed={isPasswordVisible}
        >
          {isPasswordVisible ? (
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path d="m3 3 18 18M10.6 10.7a2 2 0 0 0 2.7 2.7M9.9 4.2A10.9 10.9 0 0 1 12 4c5.5 0 9 5.2 9 5.2a14.8 14.8 0 0 1-2.2 2.7M6.6 6.6C4.3 8.1 3 10.2 3 10.2S6.5 16 12 16c1.1 0 2.1-.2 3-.5" />
            </svg>
          ) : (
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path d="M3 12s3.5-5.8 9-5.8 9 5.8 9 5.8-3.5 5.8-9 5.8S3 12 3 12Z" />
              <circle cx="12" cy="12" r="2.5" />
            </svg>
          )}
        </button>
      </div>
      {error && (
        <span className="field-error" id={errorId}>
          {error}
        </span>
      )}
    </div>
  )
}

export default PasswordInput
