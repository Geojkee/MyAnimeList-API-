import { useEffect, useState, type FormEvent } from 'react'
import { Link, useParams } from 'react-router-dom'
import { api } from '../api/endpoints'
import { ApiRequestError } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import type { Anime, Comment, Page, UserAnimeListSummary } from '../types/api'

export function AnimeDetailPage() {
  const { id } = useParams<{ id: string }>()
  const animeId = Number(id)
  const { user } = useAuth()

  const [anime, setAnime] = useState<Anime | null>(null)
  const [comments, setComments] = useState<Page<Comment> | null>(null)
  const [inList, setInList] = useState(false)
  const [commentText, setCommentText] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [message, setMessage] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)

  async function load() {
    setLoading(true)
    setError(null)
    try {
      const [a, c] = await Promise.all([
        api.getAnime(animeId),
        api.getComments(animeId),
      ])
      setAnime(a)
      setComments(c)

      if (user) {
        const list = await api.getUserAnimeList(user.username)
        setInList(list.some((entry: UserAnimeListSummary) => entry.animeId === animeId))
      } else {
        setInList(false)
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load anime')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    if (!Number.isFinite(animeId)) {
      setError('Invalid anime id')
      setLoading(false)
      return
    }
    void load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [animeId, user?.username])

  async function addToList() {
    if (!user) return
    setBusy(true)
    setMessage(null)
    try {
      await api.addToMyList(animeId)
      setInList(true)
      setMessage('Added to your list (PLAN_TO_WATCH).')
    } catch (err) {
      setMessage(err instanceof ApiRequestError ? err.message : 'Could not add to list')
    } finally {
      setBusy(false)
    }
  }

  async function submitComment(e: FormEvent) {
    e.preventDefault()
    if (!user || !commentText.trim()) return
    setBusy(true)
    try {
      await api.postComment(animeId, commentText.trim())
      setCommentText('')
      const c = await api.getComments(animeId)
      setComments(c)
    } catch (err) {
      setMessage(err instanceof ApiRequestError ? err.message : 'Could not post comment')
    } finally {
      setBusy(false)
    }
  }

  async function removeComment(commentId: number) {
    setBusy(true)
    try {
      await api.deleteComment(animeId, commentId)
      const c = await api.getComments(animeId)
      setComments(c)
    } catch (err) {
      setMessage(err instanceof ApiRequestError ? err.message : 'Could not delete comment')
    } finally {
      setBusy(false)
    }
  }

  if (loading) return <div className="status-banner">Loading…</div>
  if (error) return <div className="status-banner status-banner--error">{error}</div>
  if (!anime) return null

  const title = anime.titleEnglish || anime.titleRomaji

  return (
    <article className="detail">
      <div className="detail__header">
        <div className="detail__titles">
          <div className="anime-tile__meta">
            <span className="badge badge--accent">{anime.type}</span>
            <span className="badge badge--ok">{anime.status}</span>
            {anime.episodeCount != null && (
              <span className="badge">{anime.episodeCount} episodes</span>
            )}
            {anime.genres?.map((g) => (
              <span key={g.id} className="badge">
                {g.name}
              </span>
            ))}
          </div>
          <h1>{title}</h1>
          {anime.titleEnglish && anime.titleEnglish !== anime.titleRomaji && (
            <p>{anime.titleRomaji}</p>
          )}
        </div>

        <div className="detail__actions">
          {user ? (
            inList ? (
              <Link to="/my-list" className="btn btn-ghost">
                In your list →
              </Link>
            ) : (
              <button
                type="button"
                className="btn btn-primary"
                disabled={busy}
                onClick={() => void addToList()}
              >
                Add to my list
              </button>
            )
          ) : (
            <Link to="/auth/sign-in" className="btn btn-primary">
              Sign in to track
            </Link>
          )}
        </div>
      </div>

      {message && <div className="status-banner">{message}</div>}

      <section>
        <h2 className="page-title" style={{ fontSize: '1.4rem', marginTop: 0 }}>
          Synopsis
        </h2>
        <p className="synopsis">{anime.synopsis || 'No synopsis provided.'}</p>
      </section>

      <section className="panel">
        <h2>Comments</h2>
        {user ? (
          <form className="form-stack" onSubmit={submitComment} style={{ marginBottom: '1rem' }}>
            <textarea
              className="textarea"
              maxLength={500}
              placeholder="Share a thought (max 500 chars)"
              value={commentText}
              onChange={(e) => setCommentText(e.target.value)}
              required
            />
            <button type="submit" className="btn btn-ghost" disabled={busy}>
              Post comment
            </button>
          </form>
        ) : (
          <p className="form-hint" style={{ marginTop: 0 }}>
            <Link to="/auth/sign-in">Sign in</Link> to leave a comment.
          </p>
        )}

        <div className="stack">
          {comments && comments.empty && (
            <div className="empty">
              <strong>No comments yet</strong>
              Be the first to write one.
            </div>
          )}
          {comments?.content.map((c) => (
            <div key={c.id} className="comment">
              <div className="comment__head">
                <span>
                  <strong>{c.username}</strong> ·{' '}
                  {new Date(c.createdAt).toLocaleString()}
                </span>
                {user && (user.userId === c.userId || user.role === 'ROLE_ADMIN') && (
                  <button
                    type="button"
                    className="btn btn-danger"
                    style={{ padding: '0.3rem 0.7rem', fontSize: '0.8rem' }}
                    disabled={busy}
                    onClick={() => void removeComment(c.id)}
                  >
                    Delete
                  </button>
                )}
              </div>
              <p>{c.text}</p>
            </div>
          ))}
        </div>
      </section>
    </article>
  )
}
