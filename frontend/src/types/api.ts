export type Role = 'ROLE_USER' | 'ROLE_ADMIN'

export type AnimeType = 'TV' | 'MOVIE' | 'OVA' | 'SPECIAL'
export type AnimeStatus = 'ANNOUNCED' | 'ONGOING' | 'FINISHED'
export type UserAnimeStatus =
  | 'PLAN_TO_WATCH'
  | 'WATCHING'
  | 'COMPLETED'
  | 'ON_HOLD'
  | 'DROPPED'

export interface Genre {
  id: number
  name: string
}

export interface Anime {
  id: number
  titleRomaji: string
  titleEnglish: string | null
  genres: Genre[]
  type: AnimeType
  episodeCount: number | null
  status: AnimeStatus
  synopsis: string | null
  createdAt: string
  updatedAt: string
}

export interface Page<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
  first: boolean
  last: boolean
  empty: boolean
}

export interface AuthResponse {
  userId: number
  username: string
  role: Role
  token: string
}

export interface RefreshTokenResponse {
  accessToken: string
  userId: number
  username: string
  role: Role
}

export interface UserProfile {
  id: number
  username: string
  email?: string
  role: Role
  createdAt: string
  token?: string
}

export interface UserAnimeListSummary {
  animeId: number
  titleRomaji: string
  titleEnglish: string | null
  status: UserAnimeStatus
  score: number | null
  watchedEpisodes: number | null
  totalEpisodes: number | null
}

export interface UserAnimeListEntry {
  id: number
  userId: number
  animeId: number
  titleRomaji: string
  titleEnglish: string | null
  status: UserAnimeStatus
  score: number | null
  watchedEpisodes: number | null
  totalEpisodes: number | null
  synopsis: string | null
}

export interface Comment {
  id: number
  userId: number
  username: string
  animeId: number
  text: string
  createdAt: string
  updatedAt: string
}

export interface ApiError {
  timestamp?: string
  status?: number
  error?: string
  message?: string
  path?: string
  errorCode?: string
}

export interface AnimeQuery {
  search?: string
  type?: AnimeType | ''
  status?: AnimeStatus | ''
  genreId?: number | ''
  page?: number
  size?: number
}
