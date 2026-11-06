import { useState, type FormEvent } from 'react'
import { Link, Navigate, useNavigate, useParams } from 'react-router-dom'
import { ApiRequestError } from '../api/client'
import { useAuth } from '../auth/AuthContext'

export function AuthPage() {
  const { mode } = useParams<{ mode: string }>()
  const isSignUp = mode === 'sign-up'
  const { user, signIn, signUp, loading } = useAuth()
  const navigate = useNavigate()

  const [username, setUsername] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  if (!loading && user) {
    return <Navigate to="/my-list" replace />
  }

  if (mode !== 'sign-in' && mode !== 'sign-up') {
    return <Navigate to="/auth/sign-in" replace />
  }

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      if (isSignUp) {
        await signUp(username.trim(), email.trim(), password)
      } else {
        await signIn(username.trim(), password)
      }
      navigate('/my-list')
    } catch (err) {
      if (err instanceof ApiRequestError) {
        setError(err.message)
      } else {
        setError('Something went wrong')
      }
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="auth-layout">
      <div className="auth-panel">
        <h1>{isSignUp ? 'Join the list' : 'Welcome back'}</h1>
        <p>
          {isSignUp
            ? 'Create an account via POST /api/v1/auth/sign-up.'
            : 'Sign in via POST /api/v1/auth/sign-in — refresh cookie stays HTTP-only.'}
        </p>

        <form className="form-stack" onSubmit={onSubmit}>
          <div className="form-row">
            <label className="label" htmlFor="username">
              Username
            </label>
            <input
              id="username"
              className="field"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              minLength={4}
              maxLength={24}
              required
              autoComplete="username"
            />
          </div>

          {isSignUp && (
            <div className="form-row">
              <label className="label" htmlFor="email">
                Email
              </label>
              <input
                id="email"
                type="email"
                className="field"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                autoComplete="email"
              />
            </div>
          )}

          <div className="form-row">
            <label className="label" htmlFor="password">
              Password
            </label>
            <input
              id="password"
              type="password"
              className="field"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              minLength={8}
              required
              autoComplete={isSignUp ? 'new-password' : 'current-password'}
            />
          </div>

          {error && <p className="form-error">{error}</p>}

          <button type="submit" className="btn btn-primary" disabled={submitting}>
            {submitting ? 'Working…' : isSignUp ? 'Sign up' : 'Sign in'}
          </button>
        </form>

        <p className="form-hint">
          {isSignUp ? (
            <>
              Already have an account? <Link to="/auth/sign-in">Sign in</Link>
            </>
          ) : (
            <>
              New here? <Link to="/auth/sign-up">Create one</Link>
            </>
          )}
        </p>
      </div>
    </div>
  )
}
