import { useEffect, useState } from 'react'
import { Link, Navigate } from 'react-router-dom'
import { api } from '../api/endpoints'
import { ApiRequestError } from '../api/client'
import { EmptyState } from '../components/EmptyState'
import { PosterArt } from '../components/PosterArt'
import { useAuth } from '../auth/AuthContext'
import type { UserAnimeListSummary, UserAnimeStatus } from '../types/api'

const STATUSES: UserAnimeStatus[] = [
  'PLAN_TO_WATCH',
  'WATCHING',
  'COMPLETED',
  'ON_HOLD',
  'DROPPED',
]

const STATUS_LABEL: Record<UserAnimeStatus, string> = {
  PLAN_TO_WATCH: 'Plan to watch',
  WATCHING: 'Watching',
  COMPLETED: 'Completed',
  ON_HOLD: 'On hold',
  DROPPED: 'Dropped',
}

export function MyListPage() {
  const { user, loading: authLoading } = useAuth()
  const [status, setStatus] = useState<UserAnimeStatus | ''>('')
  const [entries, setEntries] = useState<UserAnimeListSummary[]>([])
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [busyId, setBusyId] = useState<number | null>(null)

  async function load(nextStatus: UserAnimeStatus | '' = status) {
    if (!user) return
    setLoading(true)
    setError(null)
    try {
      setEntries(await api.getUserAnimeList(user.username, nextStatus))
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load list')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    if (user) void load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user?.username])

  if (authLoading) return <div className="status-banner">Loading session…</div>
  if (!user) return <Navigate to="/auth/sign-in" replace />

  async function updateEntry(
    animeId: number,
    patch: { status?: UserAnimeStatus; score?: number | null; watchedEpisodes?: number | null },
  ) {
    setBusyId(animeId)
    try {
      await api.updateMyListEntry(animeId, patch)
      await load()
    } catch (err) {
      setError(err instanceof ApiRequestError ? err.message : 'Update failed')
    } finally {
      setBusyId(null)
    }
  }

  async function removeEntry(animeId: number) {
    setBusyId(animeId)
    try {
      await api.removeFromMyList(animeId)
      await load()
    } catch (err) {
      setError(err instanceof ApiRequestError ? err.message : 'Delete failed')
    } finally {
      setBusyId(null)
    }
  }

  return (
    <>
      <h1 className="page-title">My list</h1>
      <p className="page-lead">
        Track status, score, and episode progress for everything you&apos;re watching.
      </p>

      <div className="list-toolbar">
        <select
          className="select"
          style={{ maxWidth: 240 }}
          value={status}
          onChange={(e) => {
            const next = e.target.value as UserAnimeStatus | ''
            setStatus(next)
            void load(next)
          }}
          aria-label="Filter by status"
        >
          <option value="">All statuses</option>
          {STATUSES.map((s) => (
            <option key={s} value={s}>
              {STATUS_LABEL[s]}
            </option>
          ))}
        </select>
        <Link to="/" className="btn btn-ghost btn-sm">
          Browse catalog
        </Link>
      </div>

      {error && <div className="status-banner status-banner--error">{error}</div>}
      {loading && <div className="status-banner">Loading your list…</div>}

      {!loading && entries.length === 0 && (
        <EmptyState
          title="Your list is empty"
          action={
            <Link to="/" className="btn btn-primary">
              Find something to watch
            </Link>
          }
        >
          Open a title and hit “Add to my list”.
        </EmptyState>
      )}

      {!loading && entries.length > 0 && (
        <table className="list-table">
          <thead>
            <tr>
              <th>Title</th>
              <th>Status</th>
              <th>Score</th>
              <th>Progress</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {entries.map((entry) => {
              const title = entry.titleEnglish || entry.titleRomaji
              return (
                <tr key={entry.animeId}>
                  <td>
                    <div className="list-title-cell">
                      <PosterArt seed={entry.animeId} title={title} size="row" />
                      <Link to={`/anime/${entry.animeId}`}>{title}</Link>
                    </div>
                  </td>
                  <td>
                    <select
                      className="select"
                      value={entry.status}
                      disabled={busyId === entry.animeId}
                      onChange={(e) =>
                        void updateEntry(entry.animeId, {
                          status: e.target.value as UserAnimeStatus,
                        })
                      }
                    >
                      {STATUSES.map((s) => (
                        <option key={s} value={s}>
                          {STATUS_LABEL[s]}
                        </option>
                      ))}
                    </select>
                  </td>
                  <td>
                    <input
                      className="field"
                      type="number"
                      min={1}
                      max={10}
                      style={{ width: 72, padding: '0.45rem 0.6rem' }}
                      defaultValue={entry.score ?? ''}
                      disabled={busyId === entry.animeId}
                      onBlur={(e) => {
                        const raw = e.target.value
                        if (!raw) return
                        const score = Number(raw)
                        if (score === entry.score) return
                        void updateEntry(entry.animeId, { score })
                      }}
                    />
                  </td>
                  <td>
                    <div className="inline-controls">
                      <input
                        className="field"
                        type="number"
                        min={0}
                        style={{ width: 72 }}
                        defaultValue={entry.watchedEpisodes ?? 0}
                        disabled={busyId === entry.animeId}
                        onBlur={(e) => {
                          const watchedEpisodes = Number(e.target.value)
                          if (watchedEpisodes === (entry.watchedEpisodes ?? 0)) return
                          void updateEntry(entry.animeId, { watchedEpisodes })
                        }}
                      />
                      <span className="user-chip">/ {entry.totalEpisodes ?? '?'}</span>
                    </div>
                  </td>
                  <td>
                    <button
                      type="button"
                      className="btn btn-danger btn-sm"
                      disabled={busyId === entry.animeId}
                      onClick={() => void removeEntry(entry.animeId)}
                    >
                      Remove
                    </button>
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      )}
    </>
  )
}
