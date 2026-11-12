import { useEffect, useId, useRef, type ReactNode } from 'react'

type Props = {
  title: string
  description: string
  keepLabel: string
  busy: boolean
  confirmDisabled?: boolean
  children?: ReactNode
  onKeep: () => void
  onConfirm: () => void
}

export default function CancellationConfirmation({ title, description, keepLabel, busy, confirmDisabled, children, onKeep, onConfirm }: Props) {
  const headingId = useId()
  const panel = useRef<HTMLElement>(null)

  useEffect(() => {
    const trigger = document.activeElement
    panel.current?.focus()
    return () => { if (trigger instanceof HTMLElement && trigger.isConnected) trigger.focus() }
  }, [])

  return <section className="cancellation-confirmation" ref={panel} tabIndex={-1} aria-labelledby={headingId}>
    <div className="cancellation-heading"><span aria-hidden="true">!</span><div><h2 id={headingId}>{title}</h2><p>{description}</p></div></div>
    {children}
    <div className="cancellation-actions">
      <button className="action-button action-button-secondary" type="button" onClick={onKeep} disabled={busy}>{keepLabel}</button>
      <button className="action-button action-button-danger-solid" type="button" onClick={onConfirm} disabled={busy || confirmDisabled}>
        {busy ? 'Cancelling…' : 'Confirm cancellation'}
      </button>
    </div>
  </section>
}
