import { useId, useRef, useState } from 'react'
import gsap from 'gsap'
import { useGSAP } from '@gsap/react'

gsap.registerPlugin(useGSAP)

const questions = [
  { question: 'How do I find my next session?', answer: 'Create a trainee account, choose your sport and area, then explore the available sessions. You can also use your current location to find training nearby.' },
  { question: 'What happens when I reserve a place?', answer: 'Your booking appears in My bookings. Once confirmed, you can open the exact training location in Google Maps from the session page.' },
  { question: 'Can I offer my own training sessions?', answer: 'Choose a trainer account when signing up. Add your sport and coaching experience, then create and manage sessions from your coaching dashboard.' },
]

// Adapted from the Annnimate Starter Pack accordion: reversible height and icon timeline.
function Question({ question, answer }: typeof questions[number]) {
  const [open, setOpen] = useState(false)
  const panelId = useId()
  const root = useRef<HTMLDivElement>(null)
  const panel = useRef<HTMLDivElement>(null)
  const icon = useRef<HTMLSpanElement>(null)
  const timeline = useRef<gsap.core.Timeline | null>(null)

  useGSAP(() => {
    const media = gsap.matchMedia()
    media.add('(prefers-reduced-motion: no-preference)', () => {
      timeline.current = gsap.timeline({ paused: true })
        .fromTo(panel.current, { height: 0 }, { height: 'auto', duration: 0.4, ease: 'expo.inOut' }, 0)
        .to(icon.current, { rotation: 45, duration: 0.3 }, 0)
      return () => { timeline.current = null }
    })
    return () => media.revert()
  }, { scope: root })

  function toggle() {
    const next = !open
    setOpen(next)
    if (next) timeline.current?.play()
    else timeline.current?.reverse()
  }

  return (
    <div className="training-question" ref={root}>
      <h3>
        <button type="button" onClick={toggle} aria-expanded={open} aria-controls={panelId}>
          {question}<span ref={icon} aria-hidden="true">+</span>
        </button>
      </h3>
      <div ref={panel} id={panelId} className="training-answer" inert={!open} aria-hidden={!open} data-open={open}>
        <p>{answer}</p>
      </div>
    </div>
  )
}

export default function TrainingQuestions() {
  return (
    <section className="training-questions" aria-labelledby="questions-title">
      <h2 id="questions-title">Before your first session</h2>
      <div>{questions.map((item) => <Question key={item.question} {...item} />)}</div>
    </section>
  )
}
