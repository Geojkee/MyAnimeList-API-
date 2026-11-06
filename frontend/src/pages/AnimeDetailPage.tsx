import { useEffect, useState, type FormEvent } from 'react'
import { Link, useParams } from 'react-router-dom'
import { api } from '../api/endpoints'
import { ApiRequestError } from '../api/client'
import { EmptyState } from '../components/EmptyState'
import { PosterArt } from '../components/PosterArt'
import { useAuth } from '../auth/AuthContext'
import type { Anime, Comment, Page, UserAnimeListSummary } from '../types/api'

function statusLabel(status: Anime['status']) {
  if (status === 'ONGOING') return 'Airing'
  if (status === 'ANNOUNCED') return 'Upcoming'
  return 'Finished'
}

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
      const [a, c] = await Promise.all([api.getAnime(animeId), api.getComments(animeId)])
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
      setMessage('Added to your list as Plan to watch.')
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
      setComments(await api.getComments(animeId))
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
      setComments(await api.getComments(animeId))
    } catch (err) {
      setMessage(err instanceof ApiRequestError ? err.message : 'Could not delete comment')
    } finally {
      setBusy(false)
    }
  }

  if (loading) {
    return (
      <div className="detail" style={{ marginTop: '2rem' }}>
        <div className="detail__stage">
          <div className="skeleton-poster" style={{ maxWidth: 220 }} />
          <div>
            <div className="skeleton-line" style={{ width: '40%', marginBottom: 12 }} />
            <div className="skeleton-line" style={{ width: '70%', height: 28, marginBottom: 12 }} />
            <div className="skeleton-line skeleton-line--short" />
          </div>
        </div>
      </div>
    )
  }

  if (error) return <div className="status-banner status-banner--error">{error}</div>
  if (!anime) return null

  const title = anime.titleEnglish || anime.titleRomaji

  return (
    <article className="detail">
      <div className="detail__stage">
        <div className="detail__poster-wrap">
          <PosterArt seed={anime.id} title={title} type={anime.type} />
        </div>

        <div className="detail__titles">
          <div className="anime-card__meta">
            <span className="badge badge--accent">{anime.type}</span>
            <span
              className={
                anime.status === 'ONGOING'
                  ? 'badge badge--ok'
                  : anime.status === 'ANNOUNCED'
                    ? 'badge badge--amber'
                    : 'badge'
              }
            >
              {statusLabel(anime.status)}
            </span>
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
            <Link to="/" className="btn btn-ghost">
              Back to catalog
            </Link>
          </div>
        </div>
      </div>

      {message && <div className="status-banner">{message}</div>}

      <section>
        <h2
          style={{
            fontFamily: 'var(--font-display)',
            fontSize: '1.25rem',
            letterSpacing: '-0.02em',
            margin: '0 0 0.75rem',
          }}
        >
          Synopsis
        </h2>
        <p className="synopsis">{anime.synopsis || 'No synopsis yet — check back after the catalog grows.'}</p>
      </section>

      <section className="panel">
        <h2>Discussion</h2>
        {user ? (
          <form className="form-stack" onSubmit={submitComment} style={{ marginBottom: '1.1rem' }}>
            <textarea
              className="textarea"
              maxLength={500}
              placeholder="Share a thought (max 500 characters)"
              value={commentText}
              onChange={(e) => setCommentText(e.target.value)}
              required
            />
            <div>
              <button type="submit" className="btn btn-ghost" disabled={busy}>
                Post comment
              </button>
            </div>
          </form>
        ) : (
          <p className="form-hint" style={{ marginTop: 0, marginBottom: '1rem' }}>
            <Link to="/auth/sign-in">Sign in</Link> to join the discussion.
          </p>
        )}

        <div className="stack">
          {comments && comments.empty && (
            <EmptyState title="No comments yet">Be the first to write one.</EmptyState>
          )}
          {comments?.content.map((c) => (
            <div key={c.id} className="comment">
              <div className="comment__head">
                <span>
                  <strong>
                    <Link to={`/u/${c.username}`}>{c.username}</Link>
                  </strong>{' '}
                  · {new Date(c.createdAt).toLocaleString()}
                </span>
                {user && (user.userId === c.userId || user.role === 'ROLE_ADMIN') && (
                  <button
                    type="button"
                    className="btn btn-danger btn-sm"
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
