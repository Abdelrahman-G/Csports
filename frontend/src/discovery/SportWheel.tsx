import { useLayoutEffect, useMemo, useRef, useState } from 'react'
import gsap from 'gsap'
import type { Sport } from './types'
import { clampWheelPosition, scrollWheelPosition, settleWheel, wheelPose, WHEEL_STEP } from './wheel'
import SportArtwork from './SportArtwork'

type Props = { sports: Sport[]; value: string; onChange: (id: string) => void; loading: boolean }

export default function SportWheel({ sports, value, onChange, loading }: Props) {
  const options = useMemo(() => [{ id: '', name: 'All sports' }, ...sports.map(s => ({ id: String(s.id), name: s.name }))], [sports])
  const selected = Math.max(0, options.findIndex(s => s.id === value))
  const [preview, setPreview] = useState(selected)
  const root = useRef<HTMLDivElement>(null)
  const position = useRef({ index: selected })
  const dragged = useRef(false)
  const pendingFocus = useRef(false)
  const shown = options[Math.min(preview, options.length - 1)]

  useLayoutEffect(() => {
    const element = root.current
    if (!element) return
    const buttons = Array.from(element.querySelectorAll<HTMLButtonElement>('.sport-wheel-item'))
    const motion = position.current
    const preference = window.matchMedia('(prefers-reduced-motion: reduce)')
    let scrollTimer = 0
    let releaseTimer = 0
    let gesture: { id: number; startY: number; startIndex: number; lastY: number; lastTime: number; velocity: number } | null = null

    function paint() {
      buttons.forEach((button, index) => {
        const distance = index - motion.index
        const pose = wheelPose(distance)
        const visible = Math.abs(distance) < 2.7
        button.style.transform = `translate(${pose.x}px, ${pose.y}px) rotate(${pose.rotation}deg)`
        button.style.opacity = visible ? String(Math.max(.25, 1 - Math.abs(distance) * .28)) : '0'
        button.style.visibility = visible ? 'visible' : 'hidden'
      })
      setPreview(Math.round(clampWheelPosition(motion.index, options.length)))
    }
    function animate(index: number) {
      gsap.killTweensOf(motion)
      gsap.to(motion, { index, duration: preference.matches ? 0 : .22, ease: 'power2.out', onUpdate: paint,
        onComplete: () => {
          if (pendingFocus.current) { buttons[index]?.focus(); pendingFocus.current = false }
        },
      })
    }
    function commit(index: number) {
      const settled = Math.round(clampWheelPosition(index, options.length))
      animate(settled)
      if (settled !== selected) onChange(options[settled].id)
    }
    function down(event: PointerEvent) {
      if (event.button !== 0 || !event.isPrimary || loading) return
      window.clearTimeout(scrollTimer)
      gsap.killTweensOf(motion)
      dragged.current = false
      pendingFocus.current = false
      gesture = { id: event.pointerId, startY: event.clientY, startIndex: motion.index, lastY: event.clientY, lastTime: event.timeStamp, velocity: 0 }
    }
    function move(event: PointerEvent) {
      if (!gesture || gesture.id !== event.pointerId) return
      if (event.buttons === 0) { cancel(); return }
      const displacement = gesture.startY - event.clientY
      if (Math.abs(displacement) < 7 && !dragged.current) return
      if (!dragged.current) element!.setPointerCapture(event.pointerId)
      dragged.current = true
      gesture.velocity = (gesture.lastY - event.clientY) / Math.max(1, event.timeStamp - gesture.lastTime)
      gesture.lastY = event.clientY
      gesture.lastTime = event.timeStamp
      motion.index = clampWheelPosition(gesture.startIndex + displacement / WHEEL_STEP, options.length)
      paint()
    }
    function up(event: PointerEvent) {
      if (!gesture || gesture.id !== event.pointerId) return
      const velocity = event.timeStamp - gesture.lastTime > 100 ? 0 : gesture.velocity
      const wasDragged = dragged.current
      gesture = null
      if (element!.hasPointerCapture(event.pointerId)) element!.releasePointerCapture(event.pointerId)
      if (wasDragged) commit(settleWheel(motion.index, velocity, options.length, preference.matches))
      releaseTimer = window.setTimeout(() => { dragged.current = false }, 0)
    }
    function cancel() { gesture = null; dragged.current = false; animate(selected) }
    function scroll(event: WheelEvent) {
      if (loading || Math.abs(event.deltaX) > Math.abs(event.deltaY)) return
      const next = scrollWheelPosition(motion.index, event.deltaY, event.deltaMode, options.length)
      if (next === motion.index) return
      event.preventDefault()
      gsap.killTweensOf(motion)
      motion.index = next
      paint()
      window.clearTimeout(scrollTimer)
      scrollTimer = window.setTimeout(() => commit(Math.round(motion.index)), 70)
    }
    function preferenceChanged() { animate(selected) }
    paint()
    animate(selected)
    element.addEventListener('pointerdown', down)
    element.addEventListener('pointermove', move)
    element.addEventListener('pointerup', up)
    element.addEventListener('pointercancel', cancel)
    element.addEventListener('wheel', scroll, { passive: false })
    preference.addEventListener('change', preferenceChanged)
    return () => {
      if (gesture && element.hasPointerCapture(gesture.id)) element.releasePointerCapture(gesture.id)
      gsap.killTweensOf(motion)
      window.clearTimeout(scrollTimer)
      window.clearTimeout(releaseTimer)
      dragged.current = false
      element.removeEventListener('pointerdown', down)
      element.removeEventListener('pointermove', move)
      element.removeEventListener('pointerup', up)
      element.removeEventListener('pointercancel', cancel)
      element.removeEventListener('wheel', scroll)
      preference.removeEventListener('change', preferenceChanged)
    }
  }, [selected, options, onChange, loading])

  function choose(index: number, focus = false) {
    const next = Math.round(clampWheelPosition(index, options.length))
    dragged.current = false
    pendingFocus.current = focus
    onChange(options[next].id)
    if (focus && next === selected) root.current?.querySelectorAll<HTMLButtonElement>('.sport-wheel-item')[next]?.focus()
  }

  return <section className="sport-explorer" aria-label="Explore sports">
    <div className="sport-wheel-column">
      <div className="sport-wheel" ref={root} role="group" aria-label="Choose a sport" onKeyDown={event => {
        const next = event.key === 'ArrowDown' ? selected + 1 : event.key === 'ArrowUp' ? selected - 1 : event.key === 'Home' ? 0 : event.key === 'End' ? options.length - 1 : null
        if (next !== null) { event.preventDefault(); choose(next, true) }
      }}>
        <div className="sport-wheel-guide" aria-hidden="true" />
        {options.map((sport, index) => <button type="button" className={`sport-wheel-item${index === preview ? ' is-focused' : ''}`} key={sport.id}
          aria-pressed={index === selected} disabled={loading} title={sport.name}
          onClick={() => { if (!dragged.current) choose(index) }}>
          {sport.name}
        </button>)}
      </div>
      <div className="sport-wheel-controls"><span>{loading ? 'Loading sports…' : 'Scroll or drag to explore'}</span><div>
        <button type="button" aria-label="Previous sport" disabled={selected === 0 || loading} onClick={() => choose(selected - 1)}>↑</button>
        <button type="button" aria-label="Next sport" disabled={selected === options.length - 1 || loading} onClick={() => choose(selected + 1)}>↓</button>
      </div></div>
      <span className="motion-sr-only" aria-live="polite">Selected sport: {options[selected].name}</span>
    </div>
    <SportArtwork name={shown.name} />
  </section>
}
