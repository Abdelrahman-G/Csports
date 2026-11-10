import { useEffect, useId, useRef, useState, type FormEvent } from 'react'
import { useSearchParams } from 'react-router-dom'
import { useAuth } from '../auth/authContext'
import { getAnnouncement, getNoticeboard, publishAnnouncement, type Announcement, type Noticeboard } from './announcementApi'
import './noticeboard.css'

const dateTime = new Intl.DateTimeFormat('en-GB', { dateStyle: 'medium', timeStyle: 'short', timeZone: 'Africa/Cairo' })
const typeLabels = { TRAINER_MESSAGE: 'From your trainer', COURSE_UPDATED: 'Course update', COURSE_CANCELLED: 'Course cancelled' }

function Notice({ announcement, highlighted = false }: { announcement: Announcement; highlighted?: boolean }) {
  return <article id={`announcement-${announcement.id}`} className={`course-notice${highlighted ? ' is-highlighted' : ''}`}>
    <header><span>{typeLabels[announcement.type]}</span><time dateTime={announcement.createdAt}>{dateTime.format(new Date(announcement.createdAt))}</time></header>
    <h3>{announcement.title}</h3><p>{announcement.content}</p><footer>{announcement.authorName}</footer>
  </article>
}

export default function CourseNoticeboard({ courseId }: { courseId: number }) {
  const { authenticatedFetch } = useAuth()
  const [parameters] = useSearchParams()
  const linkedId = Number(parameters.get('announcement'))
  const [board, setBoard] = useState<Noticeboard | null>(null)
  const [linked, setLinked] = useState<Announcement | null>(null)
  const [page, setPage] = useState(0)
  const [reload, setReload] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [title, setTitle] = useState('')
  const [content, setContent] = useState('')
  const [publishing, setPublishing] = useState(false)
  const [publishError, setPublishError] = useState('')
  const [message, setMessage] = useState('')
  const submitting = useRef(false)
  const formId = useId()

  useEffect(() => {
    const controller = new AbortController()
    async function load() {
      setLoading(true)
      setError('')
      setLinked(null)
      try {
        const result = await getNoticeboard(authenticatedFetch, courseId, page, controller.signal)
        if (controller.signal.aborted) return
        setBoard(result)
        if (result && Number.isSafeInteger(linkedId) && linkedId > 0 && !result.announcements.content.some(a => a.id === linkedId)) {
          const notice = await getAnnouncement(authenticatedFetch, courseId, linkedId, controller.signal)
          if (!controller.signal.aborted) setLinked(notice)
        }
      } catch (requestError) {
        if (!controller.signal.aborted) setError(requestError instanceof Error ? requestError.message : 'The noticeboard could not be loaded.')
      } finally {
        if (!controller.signal.aborted) setLoading(false)
      }
    }
    void load()
    return () => controller.abort()
  }, [authenticatedFetch, courseId, page, reload, linkedId])

  async function publish(event: FormEvent) {
    event.preventDefault()
    if (submitting.current || !title.trim() || !content.trim()) return
    submitting.current = true
    setPublishing(true)
    setPublishError('')
    setMessage('')
    try {
      await publishAnnouncement(authenticatedFetch, courseId, title, content)
      setTitle('')
      setContent('')
      setMessage('Announcement published.')
      setPage(0)
      setReload(n => n + 1)
    } catch (requestError) {
      setPublishError(requestError instanceof Error ? requestError.message : 'The announcement could not be published.')
    } finally {
      submitting.current = false
      setPublishing(false)
    }
  }

  return <section className="course-noticeboard" aria-label="Course noticeboard" aria-busy={loading}>
    <header className="noticeboard-heading"><div><p className="details-label">Keep up with your course</p><h2>Course noticeboard</h2></div><span aria-hidden="true">↗</span></header>
    {board?.canPublish && <form className="notice-composer" onSubmit={publish}>
      <label htmlFor={`${formId}-title`}>Announcement title</label>
      <input id={`${formId}-title`} value={title} onChange={e => setTitle(e.target.value)} required maxLength={150} disabled={publishing} placeholder="What should your trainees know?" />
      <label htmlFor={`${formId}-content`}>Message</label>
      <textarea id={`${formId}-content`} value={content} onChange={e => setContent(e.target.value)} required maxLength={2000} rows={4} disabled={publishing} />
      <div className="notice-composer-actions"><small>Published announcements stay on the noticeboard.</small><button type="submit" disabled={publishing || !title.trim() || !content.trim()}>{publishing ? 'Publishing…' : 'Publish announcement'}</button></div>
    </form>}
    {publishError && <p className="notice-error" role="alert">{publishError}</p>}
    {message && <p className="notice-success" role="status">{message}</p>}
    {error && <div className="notice-error" role="alert"><p>{error}</p><button type="button" onClick={() => setReload(n => n + 1)}>Try again</button></div>}
    {loading ? <p role="status">Loading announcements…</p> : board ? <>
      {linked && <Notice announcement={linked} highlighted />}
      {board.announcements.content.length === 0 && !linked && <p className="notice-empty">No announcements yet.</p>}
      <div className="notice-list">{board.announcements.content.map(a => <Notice key={a.id} announcement={a} highlighted={a.id === linkedId} />)}</div>
      {board.announcements.totalPages > 1 && <nav className="notice-pagination" aria-label="Announcement pages"><button type="button" disabled={board.announcements.first} onClick={() => setPage(p => p - 1)}>Newer</button><span>Page {board.announcements.page + 1} of {board.announcements.totalPages}</span><button type="button" disabled={board.announcements.last} onClick={() => setPage(p => p + 1)}>Older</button></nav>}
    </> : !error && <p className="notice-empty">Announcements are available to registered trainees.</p>}
  </section>
}
