import assert from 'node:assert/strict'
import { test } from 'node:test'
import { getAnnouncement, getNoticeboard, publishAnnouncement } from '../src/announcements/announcementApi.ts'

test('a forbidden noticeboard is a membership state, while server failures remain errors', async () => {
  const signal = new AbortController().signal
  assert.equal(await getNoticeboard(async () => new Response('', { status: 403 }), 7, 0, signal), null)
  await assert.rejects(getNoticeboard(async () => new Response('unavailable', { status: 503 }), 7, 0, signal), /could not be loaded/)
})

test('announcement links stay scoped to their course and preserve request cancellation', async () => {
  const signal = new AbortController().signal
  await assert.rejects(getAnnouncement(async (url, init) => {
    assert.equal(url, '/api/v1/courses/7/announcements/99')
    assert.equal(init?.signal, signal)
    return Response.json({ message: 'Announcement not found.' }, { status: 404 })
  }, 7, 99, signal), /Announcement not found/)
})

test('publishing sends trimmed plain text without client-controlled authors or announcement types', async () => {
  const result = await publishAnnouncement(async (_url, init) => {
    assert.equal(init?.method, 'POST')
    assert.deepEqual(JSON.parse(String(init?.body)), { title: 'Equipment', content: '<script>literal text</script>' })
    return Response.json({ id: 17 }, { status: 201 })
  }, 7, '  Equipment  ', ' <script>literal text</script> ')
  assert.equal(result.id, 17)
})

test('publication errors preserve the server explanation', async () => {
  await assert.rejects(publishAnnouncement(async () => Response.json({ message: 'Announcements are closed for this course.' }, { status: 409 }), 7, 'Title', 'Message'), /Announcements are closed/)
})
