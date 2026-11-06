import { useEffect, useState, type FormEvent } from 'react'
import { Link, useParams } from 'react-router-dom'
import { api } from '../api/endpoints'
import { ApiRequestError, setStoredToken } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import type { UserAnimeListSummary, UserProfile } from '../types/api'

export function ProfilePage() {
  const { username } = useParams<{ username: string }>()
  const { user, applyAuth } = useAuth()
  const isMe = !!user && user.username === username

  const [profile, setProfile] = useState<UserProfile | null>(null)
  const [list, setList] = useState<UserAnimeListSummary[]>([])
  const [error, setError] = useState<string | null>(null)
  const [message, setMessage] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  const [editUsername, setEditUsername] = useState('')
  const [editEmail, setEditEmail] = useState('')
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    if (!username) return
    let cancelled = false

    async function load() {
      setLoading(true)
      setError(null)
      try {
        const [p, l] = await Promise.all([
          isMe ? api.getMe() : api.getUser(username!),
          api.getUserAnimeList(username!),
        ])
        if (cancelled) return
        setProfile(p)
        setList(l)
        setEditUsername(p.username)
        setEditEmail(p.email ?? '')
      } catch (err) {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : 'Failed to load profile')
        }
      } finally {
        if (!cancelled) setLoading(false)
      }
    }

    void load()
    return () => {
      cancelled = true
    }
  }, [username, isMe])

  async function onSave(e: FormEvent) {
    e.preventDefault()
    if (!isMe) return
    setSaving(true)
    setMessage(null)
    try {
      const updated = await api.updateMe({
        username: editUsername.trim() || undefined,
        email: editEmail.trim() || undefined,
      })
      setProfile(updated)
      if (updated.token) {
        setStoredToken(updated.token)
        applyAuth({
          userId: updated.id,
          username: updated.username,
          role: updated.role,
          token: updated.token,
        })
      }
      setMessage('Profile updated.')
    } catch (err) {
      setMessage(err instanceof ApiRequestError ? err.message : 'Update failed')
    } finally {
      setSaving(false)
    }
  }

  if (loading) return <div className="status-banner">Loading profile…</div>
  if (error) return <div className="status-banner status-banner--error">{error}</div>
  if (!profile) return null

  return (
    <>
      <h1 className="page-title">{profile.username}</h1>
      <p className="page-lead">
        {profile.role.replace('ROLE_', '')} · joined{' '}
        {new Date(profile.createdAt).toLocaleDateString()}
        {profile.email ? ` · ${profile.email}` : ''}
      </p>

      <div className="profile-grid">
        <section className="panel" style={{ borderTop: 'none', paddingTop: 0 }}>
          <h2>{isMe ? 'Edit profile' : 'Public profile'}</h2>
          {isMe ? (
            <form className="form-stack" onSubmit={onSave}>
              <div className="form-row">
                <label className="label" htmlFor="edit-username">
                  Username
                </label>
                <input
                  id="edit-username"
                  className="field"
                  value={editUsername}
                  onChange={(e) => setEditUsername(e.target.value)}
                  minLength={4}
                  maxLength={24}
                />
              </div>
              <div className="form-row">
                <label className="label" htmlFor="edit-email">
                  Email
                </label>
                <input
                  id="edit-email"
                  type="email"
                  className="field"
                  value={editEmail}
                  onChange={(e) => setEditEmail(e.target.value)}
                />
              </div>
              {message && <p className="form-hint">{message}</p>}
              <button type="submit" className="btn btn-primary" disabled={saving}>
                {saving ? 'Saving…' : 'Save changes'}
              </button>
            </form>
          ) : (
            <p className="synopsis" style={{ margin: 0 }}>
              Viewing a public profile. Anime list is visible to everyone.
            </p>
          )}
        </section>

        <section className="panel" style={{ borderTop: 'none', paddingTop: 0 }}>
          <h2>Anime list ({list.length})</h2>
          {list.length === 0 ? (
            <div className="empty">
              <strong>Nothing tracked yet</strong>
            </div>
          ) : (
            <table className="list-table">
              <thead>
                <tr>
                  <th>Title</th>
                  <th>Status</th>
                  <th>Score</th>
                  <th>Progress</th>
                </tr>
              </thead>
              <tbody>
                {list.map((entry) => (
                  <tr key={entry.animeId}>
                    <td>
                      <Link to={`/anime/${entry.animeId}`}>
                        {entry.titleEnglish || entry.titleRomaji}
                      </Link>
                    </td>
                    <td>
                      <span className="badge">{entry.status}</span>
                    </td>
                    <td>{entry.score ?? '—'}</td>
                    <td>
                      {entry.watchedEpisodes ?? 0} / {entry.totalEpisodes ?? '?'}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </section>
      </div>
    </>
  )
}
