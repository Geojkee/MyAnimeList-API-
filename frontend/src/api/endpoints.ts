import { apiRequest } from './client'
import type {
  Anime,
  AnimeQuery,
  AuthResponse,
  Comment,
  Genre,
  Page,
  RefreshTokenResponse,
  UserAnimeListEntry,
  UserAnimeListSummary,
  UserAnimeStatus,
  UserProfile,
} from '../types/api'

function animeQueryString(query: AnimeQuery = {}) {
  const params = new URLSearchParams()
  if (query.search) params.set('search', query.search)
  if (query.type) params.set('type', query.type)
  if (query.status) params.set('status', query.status)
  if (query.genreId) params.set('genreId', String(query.genreId))
  params.set('page', String(query.page ?? 0))
  params.set('size', String(query.size ?? 12))
  return params.toString()
}

export const api = {
  signUp(username: string, email: string, password: string) {
    return apiRequest<AuthResponse>('/api/v1/auth/sign-up', {
      method: 'POST',
      body: { username, email, password },
    })
  },

  signIn(username: string, password: string) {
    return apiRequest<AuthResponse>('/api/v1/auth/sign-in', {
      method: 'POST',
      body: { username, password },
    })
  },

  refresh() {
    return apiRequest<RefreshTokenResponse>('/api/v1/auth/refresh', {
      method: 'POST',
    })
  },

  logout() {
    return apiRequest<void>('/api/v1/auth/logout', {
      method: 'POST',
      auth: true,
    })
  },

  getAnimePage(query?: AnimeQuery) {
    return apiRequest<Page<Anime>>(`/api/v1/anime?${animeQueryString(query)}`)
  },

  getAnime(id: number) {
    return apiRequest<Anime>(`/api/v1/anime/${id}`)
  },

  getGenres() {
    return apiRequest<Genre[]>('/api/v1/genre')
  },

  getMe() {
    return apiRequest<UserProfile>('/api/v1/users/me', { auth: true })
  },

  updateMe(payload: { username?: string; email?: string }) {
    return apiRequest<UserProfile>('/api/v1/users/me', {
      method: 'PATCH',
      body: payload,
      auth: true,
    })
  },

  getUser(username: string) {
    return apiRequest<UserProfile>(`/api/v1/users/${encodeURIComponent(username)}`)
  },

  getUserAnimeList(username: string, status?: UserAnimeStatus | '') {
    const qs = status ? `?status=${status}` : ''
    return apiRequest<UserAnimeListSummary[]>(
      `/api/v1/users/${encodeURIComponent(username)}/animelist${qs}`,
    )
  },

  addToMyList(animeId: number) {
    return apiRequest<UserAnimeListEntry>('/api/v1/users/me/animelist', {
      method: 'POST',
      body: { animeId },
      auth: true,
    })
  },

  updateMyListEntry(
    animeId: number,
    payload: { status?: UserAnimeStatus; score?: number | null; watchedEpisodes?: number | null },
  ) {
    return apiRequest<UserAnimeListEntry>(`/api/v1/users/me/animelist/${animeId}`, {
      method: 'PATCH',
      body: payload,
      auth: true,
    })
  },

  removeFromMyList(animeId: number) {
    return apiRequest<void>(`/api/v1/users/me/animelist/${animeId}`, {
      method: 'DELETE',
      auth: true,
    })
  },

  getComments(animeId: number, page = 0, size = 10) {
    return apiRequest<Page<Comment>>(
      `/api/v1/anime/${animeId}/comments?page=${page}&size=${size}`,
    )
  },

  postComment(animeId: number, text: string) {
    return apiRequest<Comment>(`/api/v1/anime/${animeId}/comments`, {
      method: 'POST',
      body: { text },
      auth: true,
    })
  },

  deleteComment(animeId: number, commentId: number) {
    return apiRequest<void>(`/api/v1/anime/${animeId}/comments/${commentId}`, {
      method: 'DELETE',
      auth: true,
    })
  },
}
