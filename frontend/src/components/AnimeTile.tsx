import { Link } from 'react-router-dom'
import type { Anime } from '../types/api'

function statusClass(status: Anime['status']) {
  if (status === 'ONGOING') return 'badge badge--ok'
  if (status === 'ANNOUNCED') return 'badge badge--amber'
  return 'badge'
}

export function AnimeTile({ anime }: { anime: Anime }) {
  const title = anime.titleEnglish || anime.titleRomaji
  const subtitle =
    anime.titleEnglish && anime.titleEnglish !== anime.titleRomaji
      ? anime.titleRomaji
      : null

  return (
    <Link to={`/anime/${anime.id}`} className="anime-tile">
      <div className="anime-tile__meta">
        <span className="badge badge--accent">{anime.type}</span>
        <span className={statusClass(anime.status)}>{anime.status}</span>
        {anime.episodeCount != null && (
          <span className="badge">{anime.episodeCount} ep</span>
        )}
      </div>
      <div>
        <h3>{title}</h3>
        {subtitle && <p className="anime-tile__sub">{subtitle}</p>}
      </div>
      <p className="anime-tile__synopsis">
        {anime.synopsis || 'No synopsis yet — open for details and comments.'}
      </p>
      {anime.genres?.length > 0 && (
        <div className="anime-tile__meta">
          {anime.genres.slice(0, 3).map((g) => (
            <span key={g.id} className="badge">
              {g.name}
            </span>
          ))}
        </div>
      )}
    </Link>
  )
}
