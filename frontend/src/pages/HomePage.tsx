import { useEffect, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api/endpoints'
import { ApiRequestError } from '../api/client'
import { AnimeTile } from '../components/AnimeTile'
import { EmptyState } from '../components/EmptyState'
import { PosterArt } from '../components/PosterArt'
import { useAuth } from '../auth/AuthContext'
import type { Anime, AnimeStatus, AnimeType, Genre, Page } from '../types/api'

function friendlyError(err: unknown) {
  if (err instanceof ApiRequestError) {
    if (err.status === 502 || err.status === 503 || err.status === 504) {
      return 'API is unreachable. Start it with make up && make run, then refresh.'
    }
    return err.message
  }
  if (err instanceof TypeError) {
    return 'Cannot reach the API. Is it running on :8080?'
  }
  if (err instanceof Error) return err.message
  return 'Failed to load anime'
}

function SkeletonGrid() {
  return (
    <div className="skeleton-grid" aria-hidden>
      {Array.from({ length: 8 }).map((_, i) => (
        <div key={i} className="skeleton-card">
          <div className="skeleton-poster" />
          <div className="skeleton-line" />
          <div className="skeleton-line skeleton-line--short" />
        </div>
      ))}
    </div>
  )
}

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
      .catch((err: unknown) => {
        if (!cancelled) setError(friendlyError(err))
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

  function clearFilters() {
    setDraftSearch('')
    setSearch('')
    setType('')
    setStatus('')
    setGenreId('')
    setPage(0)
  }

  const hasFilters = !!(search || type || status || genreId)

  return (
    <>
      <section className="hero" aria-label="MyAnimeList hero">
        <div className="hero__wash" />
        <div className="hero__beam" />
        <div className="hero__posters" aria-hidden>
          <div className="hero__poster">
            <PosterArt seed="hero-a" title="Neon Pulse" type="TV" size="hero" />
          </div>
          <div className="hero__poster">
            <PosterArt seed="hero-b" title="Crimson Arc" type="MOVIE" size="hero" />
          </div>
          <div className="hero__poster">
            <PosterArt seed="hero-c" title="Night Rail" type="OVA" size="hero" />
          </div>
        </div>
        <div className="hero__content">
          <h1 className="hero__brand">
            MyAnime<span>List</span>
          </h1>
          <p className="hero__lead">
            Your night-screening catalog — discover titles, track progress, and leave thoughts
            with friends who watch.
          </p>
          <div className="hero__actions">
            <a href="#catalog" className="btn btn-primary">
              Browse catalog
            </a>
            {user ? (
              <Link to="/my-list" className="btn btn-ghost">
                Open my list
              </Link>
            ) : (
              <Link to="/auth/sign-up" className="btn btn-ghost">
                Join free
              </Link>
            )}
          </div>
        </div>
      </section>

      <section id="catalog">
        <div className="section-head">
          <div>
            <h2>Now screening</h2>
            <p>Search by title, type, airing status, or genre.</p>
          </div>
          {data && !data.empty && (
            <span className="section-count">{data.totalElements} titles</span>
          )}
        </div>

        <form className="filters" onSubmit={onSearch}>
          <input
            className="field"
            placeholder="Search titles…"
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
            <option value="ANNOUNCED">Upcoming</option>
            <option value="ONGOING">Airing</option>
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

        {loading && <SkeletonGrid />}

        {!loading && data && data.empty && (
          <EmptyState
            title={hasFilters ? 'No matches' : 'Catalog is quiet tonight'}
            action={
              hasFilters ? (
                <button type="button" className="btn btn-ghost" onClick={clearFilters}>
                  Clear filters
                </button>
              ) : (
                <a
                  className="btn btn-ghost"
                  href="http://localhost:8080/swagger-ui/index.html"
                  target="_blank"
                  rel="noreferrer"
                >
                  Open Swagger to seed titles
                </a>
              )
            }
          >
            {hasFilters
              ? 'Try a different search or clear filters.'
              : 'Once an admin adds anime via the API, they appear here with poster art and filters.'}
          </EmptyState>
        )}

        {!loading && data && !data.empty && (
          <>
            <div className="anime-grid">
              {data.content.map((anime, i) => (
                <AnimeTile key={anime.id} anime={anime} index={i} />
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
                Page {data.number + 1} / {Math.max(data.totalPages, 1)}
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
