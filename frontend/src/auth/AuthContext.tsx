import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from 'react'
import type { ReactNode } from 'react'
import { api } from '../api/endpoints'
import { ApiRequestError, getStoredToken, setStoredToken } from '../api/client'
import type { AuthResponse, Role } from '../types/api'

type AuthUser = {
  userId: number
  username: string
  role: Role
}

type AuthContextValue = {
  user: AuthUser | null
  loading: boolean
  signIn: (username: string, password: string) => Promise<void>
  signUp: (username: string, email: string, password: string) => Promise<void>
  logout: () => Promise<void>
  applyAuth: (auth: AuthResponse) => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

function toUser(auth: AuthResponse): AuthUser {
  return {
    userId: auth.userId,
    username: auth.username,
    role: auth.role,
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null)
  const [loading, setLoading] = useState(true)

  const applyAuth = useCallback((auth: AuthResponse) => {
    setStoredToken(auth.token)
    setUser(toUser(auth))
  }, [])

  useEffect(() => {
    let cancelled = false

    async function bootstrap() {
      const token = getStoredToken()
      if (!token) {
        if (!cancelled) setLoading(false)
        return
      }

      try {
        const me = await api.getMe()
        if (!cancelled) {
          setUser({
            userId: me.id,
            username: me.username,
            role: me.role,
          })
        }
      } catch (err) {
        if (err instanceof ApiRequestError && err.status === 401) {
          try {
            const refreshed = await api.refresh()
            if (!cancelled) {
              setStoredToken(refreshed.accessToken)
              setUser({
                userId: refreshed.userId,
                username: refreshed.username,
                role: refreshed.role,
              })
            }
          } catch {
            setStoredToken(null)
            if (!cancelled) setUser(null)
          }
        } else {
          setStoredToken(null)
          if (!cancelled) setUser(null)
        }
      } finally {
        if (!cancelled) setLoading(false)
      }
    }

    void bootstrap()
    return () => {
      cancelled = true
    }
  }, [])

  const signIn = useCallback(
    async (username: string, password: string) => {
      const auth = await api.signIn(username, password)
      applyAuth(auth)
    },
    [applyAuth],
  )

  const signUp = useCallback(
    async (username: string, email: string, password: string) => {
      const auth = await api.signUp(username, email, password)
      // Sign-up returns access token but no refresh cookie — follow with sign-in.
      try {
        const session = await api.signIn(username, password)
        applyAuth(session)
      } catch {
        applyAuth(auth)
      }
    },
    [applyAuth],
  )

  const logout = useCallback(async () => {
    try {
      await api.logout()
    } catch {
      // Clear local session even if logout endpoint fails.
    }
    setStoredToken(null)
    setUser(null)
  }, [])

  const value = useMemo(
    () => ({ user, loading, signIn, signUp, logout, applyAuth }),
    [user, loading, signIn, signUp, logout, applyAuth],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
