export const WHEEL_STEP = 68

export function clampWheelPosition(position: number, count: number): number {
  if (!Number.isFinite(position) || count < 1) return 0
  return Math.max(0, Math.min(count - 1, position))
}

export function settleWheel(position: number, velocity: number, count: number, reducedMotion = false): number {
  const momentum = reducedMotion ? 0 : Math.max(-1.5, Math.min(1.5, velocity * 120 / WHEEL_STEP))
  return Math.round(clampWheelPosition(position + momentum, count))
}

export function scrollWheelPosition(position: number, delta: number, deltaMode: number, count: number): number {
  const pixels = delta * (deltaMode === 1 ? 16 : deltaMode === 2 ? 300 : 1)
  return clampWheelPosition(position + pixels / 120, count)
}

export function wheelPose(distance: number) {
  const angle = distance * 0.19
  return { x: 360 * (Math.cos(angle) - 1), y: 360 * Math.sin(angle), rotation: angle * 180 / Math.PI }
}
