import { Link } from 'react-router-dom'
import type { Anime } from '../types/api'
import { PosterArt } from './PosterArt'

function statusClass(status: Anime['status']) {
  if (status === 'ONGOING') return 'badge badge--ok'
  if (status === 'ANNOUNCED') return 'badge badge--amber'
  return 'badge'
}

function statusLabel(status: Anime['status']) {
  if (status === 'ONGOING') return 'Airing'
  if (status === 'ANNOUNCED') return 'Upcoming'
  return 'Finished'
}

export function AnimeTile({ anime, index = 0 }: { anime: Anime; index?: number }) {
  const title = anime.titleEnglish || anime.titleRomaji
  const subtitle =
    anime.titleEnglish && anime.titleEnglish !== anime.titleRomaji
      ? anime.titleRomaji
      : null

  return (
    <Link
      to={`/anime/${anime.id}`}
      className="anime-card"
      style={{ animationDelay: `${Math.min(index, 11) * 45}ms` }}
    >
      <PosterArt seed={anime.id} title={title} type={anime.type} />
      <div className="anime-card__body">
        <div className="anime-card__meta">
          <span className={statusClass(anime.status)}>{statusLabel(anime.status)}</span>
          {anime.episodeCount != null && (
            <span className="badge">{anime.episodeCount} ep</span>
          )}
        </div>
        <h3 className="anime-card__title">{title}</h3>
        {subtitle && <p className="anime-card__sub">{subtitle}</p>}
        {anime.genres?.length > 0 && (
          <div className="anime-card__genres">
            {anime.genres.slice(0, 3).map((g) => (
              <span key={g.id}>{g.name}</span>
            ))}
          </div>
        )}
      </div>
    </Link>
  )
}
