import { NavLink, Outlet, Link } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

export function Layout() {
  const { user, logout, loading } = useAuth()

  return (
    <div className="app-shell">
      <header className="site-header">
        <div className="site-header__inner">
          <Link to="/" className="brand">
            <span className="brand__glyph" aria-hidden>
              ▲
            </span>
            <span className="brand__text">
              <span className="brand__mark">
                MyAnime<span>List</span>
              </span>
              <span className="brand__tag">Watch · Track · Discuss</span>
            </span>
          </Link>

          <nav className="nav-links" aria-label="Primary">
            <NavLink to="/" end>
              Catalog
            </NavLink>
            {user && (
              <>
                <NavLink to="/my-list">My list</NavLink>
                <NavLink to={`/u/${user.username}`}>Profile</NavLink>
              </>
            )}
          </nav>

          <div className="header-actions">
            {loading ? (
              <span className="user-chip">…</span>
            ) : user ? (
              <>
                <Link to={`/u/${user.username}`} className="user-chip user-chip--link">
                  <span className="user-chip__avatar" aria-hidden>
                    {user.username.slice(0, 1).toUpperCase()}
                  </span>
                  <strong>{user.username}</strong>
                </Link>
                <button type="button" className="btn btn-ghost btn-sm" onClick={() => void logout()}>
                  Log out
                </button>
              </>
            ) : (
              <>
                <Link to="/auth/sign-in" className="btn btn-ghost btn-sm">
                  Sign in
                </Link>
                <Link to="/auth/sign-up" className="btn btn-primary btn-sm">
                  Join free
                </Link>
              </>
            )}
          </div>
        </div>
      </header>

      <main className="main">
        <Outlet />
      </main>

      <footer className="site-footer">
        <div className="site-footer__inner">
          <span className="brand__mark">
            MyAnime<span>List</span>
          </span>
          <p>A night-screening companion for your anime list.</p>
        </div>
      </footer>
    </div>
  )
}
