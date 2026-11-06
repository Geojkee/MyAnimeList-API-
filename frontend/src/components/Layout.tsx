import { NavLink, Outlet, Link } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

export function Layout() {
  const { user, logout, loading } = useAuth()

  return (
    <div className="app-shell">
      <header className="site-header">
        <div className="site-header__inner">
          <Link to="/" className="brand">
            <div className="brand__mark">
              MyAnime<span>List</span>
            </div>
            <div className="brand__tag">Catalog UI</div>
          </Link>

          <nav className="nav-links">
            <NavLink to="/" end>
              Browse
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
                <span className="user-chip">
                  Signed in as <strong>{user.username}</strong>
                </span>
                <button type="button" className="btn btn-ghost" onClick={() => void logout()}>
                  Log out
                </button>
              </>
            ) : (
              <>
                <Link to="/auth/sign-in" className="btn btn-ghost">
                  Sign in
                </Link>
                <Link to="/auth/sign-up" className="btn btn-primary">
                  Sign up
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
        MyAnimeList UI · talks to <code>/api/v1</code> on the Spring Boot API
      </footer>
    </div>
  )
}
