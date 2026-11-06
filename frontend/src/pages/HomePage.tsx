import { useEffect, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api/endpoints'
import { AnimeTile } from '../components/AnimeTile'
import { useAuth } from '../auth/AuthContext'
import type { Anime, AnimeStatus, AnimeType, Genre, Page } from '../types/api'

export function HomePage() {
  const { user } = useAuth()
  const [page, setPage] = useState(0)
  const [search, setSearch] = useState('')
  const [draftSearch, setDraftSearch] = useState('')
  const [type, setType] = useState<AnimeType | ''>('')
  const [status, setStatus] = useState<AnimeStatus | ''>('')
  const [genreId, setGenreId] = useState<number | ''>('')
  const [genres, setGenres] = useState<Genre[]>([])
  const [data, setData] = useState<Page<Anime> | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    void api.getGenres().then(setGenres).catch(() => setGenres([]))
  }, [])

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    setError(null)

    api
      .getAnimePage({
        search: search || undefined,
        type,
        status,
        genreId,
        page,
        size: 12,
      })
      .then((result) => {
        if (!cancelled) setData(result)
      })
      .catch((err: Error) => {
        if (!cancelled) setError(err.message || 'Failed to load anime')
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })

    return () => {
      cancelled = true
    }
  }, [search, type, status, genreId, page])

  function onSearch(e: FormEvent) {
    e.preventDefault()
    setPage(0)
    setSearch(draftSearch.trim())
  }

  return (
    <>
      <section className="hero" aria-label="MyAnimeList hero">
        <div className="hero__wash" />
        <div className="hero__beam" />
        <div className="hero__content">
          <h1 className="hero__brand">
            MyAnime<span>List</span>
          </h1>
          <p className="hero__lead">
            Browse the catalog, track what you&apos;re watching, and leave notes —
            a night-screening front for the MyAnimeList API.
          </p>
          <div className="hero__actions">
            <a href="#catalog" className="btn btn-primary">
              Enter catalog
            </a>
            {user ? (
              <Link to="/my-list" className="btn btn-ghost">
                Open my list
              </Link>
            ) : (
              <Link to="/auth/sign-up" className="btn btn-ghost">
                Create account
              </Link>
            )}
          </div>
        </div>
      </section>

      <section id="catalog">
        <div className="section-head">
          <div>
            <h2>Catalog</h2>
            <p>Search and filter titles from <code>GET /api/v1/anime</code>.</p>
          </div>
        </div>

        <form className="filters" onSubmit={onSearch}>
          <input
            className="field"
            placeholder="Search title…"
            value={draftSearch}
            onChange={(e) => setDraftSearch(e.target.value)}
            aria-label="Search anime"
          />
          <select
            className="select"
            value={type}
            onChange={(e) => {
              setPage(0)
              setType(e.target.value as AnimeType | '')
            }}
            aria-label="Type"
          >
            <option value="">All types</option>
            <option value="TV">TV</option>
            <option value="MOVIE">Movie</option>
            <option value="OVA">OVA</option>
            <option value="SPECIAL">Special</option>
          </select>
          <select
            className="select"
            value={status}
            onChange={(e) => {
              setPage(0)
              setStatus(e.target.value as AnimeStatus | '')
            }}
            aria-label="Status"
          >
            <option value="">All statuses</option>
            <option value="ANNOUNCED">Announced</option>
            <option value="ONGOING">Ongoing</option>
            <option value="FINISHED">Finished</option>
          </select>
          <select
            className="select"
            value={genreId}
            onChange={(e) => {
              setPage(0)
              setGenreId(e.target.value ? Number(e.target.value) : '')
            }}
            aria-label="Genre"
          >
            <option value="">All genres</option>
            {genres.map((g) => (
              <option key={g.id} value={g.id}>
                {g.name}
              </option>
            ))}
          </select>
          <button type="submit" className="btn btn-ghost">
            Search
          </button>
        </form>

        {error && <div className="status-banner status-banner--error">{error}</div>}

        {loading && <div className="status-banner">Loading titles…</div>}

        {!loading && data && data.empty && (
          <div className="empty">
            <strong>No anime in the catalog yet</strong>
            Titles are created by admins via the API / Swagger. Once seeded, they
            appear here automatically.
          </div>
        )}

        {!loading && data && !data.empty && (
          <>
            <div className="anime-grid">
              {data.content.map((anime) => (
                <AnimeTile key={anime.id} anime={anime} />
              ))}
            </div>
            <div className="pager">
              <button
                type="button"
                className="btn btn-ghost"
                disabled={data.first}
                onClick={() => setPage((p) => Math.max(0, p - 1))}
              >
                Previous
              </button>
              <span>
                Page {data.number + 1} / {Math.max(data.totalPages, 1)} ·{' '}
                {data.totalElements} titles
              </span>
              <button
                type="button"
                className="btn btn-ghost"
                disabled={data.last}
                onClick={() => setPage((p) => p + 1)}
              >
                Next
              </button>
            </div>
          </>
        )}
      </section>
    </>
  )
}
