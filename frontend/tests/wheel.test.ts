import assert from 'node:assert/strict'
import { test } from 'node:test'
import { clampWheelPosition, scrollWheelPosition, settleWheel, wheelPose } from '../src/discovery/wheel.ts'
import { searchCourses } from '../src/discovery/discoveryApi.ts'

test('wheel positions stay inside finite lists, including empty and single-option lists', () => {
  assert.equal(clampWheelPosition(-10, 9), 0)
  assert.equal(clampWheelPosition(100, 9), 8)
  assert.equal(clampWheelPosition(2.4, 9), 2.4)
  assert.equal(clampWheelPosition(4, 0), 0)
  assert.equal(clampWheelPosition(4, 1), 0)
  assert.equal(clampWheelPosition(Number.NaN, 9), 0)
})

test('settling preserves direction, limits momentum and removes inertia for reduced motion', () => {
  assert.equal(settleWheel(3.2, 0, 9), 3)
  assert.equal(settleWheel(3.2, 1, 9), 5)
  assert.equal(settleWheel(3.2, -1, 9), 2)
  assert.equal(settleWheel(3.2, 100, 9), 5)
  assert.equal(settleWheel(3.2, 100, 9, true), 3)
  assert.equal(settleWheel(8, 2, 9), 8)
  assert.equal(settleWheel(0, -2, 9), 0)
})

test('mouse and trackpad scroll units move consistently and stop at list boundaries', () => {
  assert.equal(scrollWheelPosition(2, 120, 0, 9), 3)
  assert.equal(scrollWheelPosition(2, 7.5, 1, 9), 3)
  assert.equal(scrollWheelPosition(2, .4, 2, 9), 3)
  assert.equal(scrollWheelPosition(3, -120, 0, 9), 2)
  assert.equal(scrollWheelPosition(2, 12, 0, 9), 2.1)
  assert.equal(scrollWheelPosition(8, 120, 0, 9), 8)
  assert.equal(scrollWheelPosition(0, -120, 0, 9), 0)
})

test('the active title stays centered and neighboring titles follow a symmetric arc', () => {
  assert.deepEqual(wheelPose(0), { x: 0, y: 0, rotation: 0 })
  const above = wheelPose(-1), below = wheelPose(1)
  assert.equal(above.x, below.x)
  assert.equal(above.y, -below.y)
  assert.equal(above.rotation, -below.rotation)
  assert.ok(below.y > 44)
})

test('sport filtering preserves nearby coordinates, keywords, paging and cancellation', async t => {
  const signal = new AbortController().signal
  let requested = ''
  t.mock.method(globalThis, 'fetch', async (input: string, init?: RequestInit) => {
    requested = input
    assert.equal(init?.signal, signal)
    return Response.json({ content: [], totalElements: 0 })
  })
  await searchCourses({ mode: 'nearby', sportId: 2, query: 'Club & pool', coordinates: { latitude: 30.05, longitude: 31.25, accuracy: 20 }, radiusKm: 50 }, 0, signal)
  const params = new URL(requested, 'http://localhost').searchParams
  assert.equal(params.get('sportId'), '2')
  assert.equal(params.get('q'), 'Club & pool')
  assert.equal(params.get('sortBy'), 'distance')
  assert.equal(params.get('latitude'), '30.05')
  assert.equal(params.get('page'), '0')
  assert.equal(params.has('regionId'), false)
})
