import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider } from './auth/AuthContext'
import { Layout } from './components/Layout'
import { AnimeDetailPage } from './pages/AnimeDetailPage'
import { AuthPage } from './pages/AuthPage'
import { HomePage } from './pages/HomePage'
import { MyListPage } from './pages/MyListPage'
import { ProfilePage } from './pages/ProfilePage'

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route element={<Layout />}>
            <Route index element={<HomePage />} />
            <Route path="anime/:id" element={<AnimeDetailPage />} />
            <Route path="auth/:mode" element={<AuthPage />} />
            <Route path="my-list" element={<MyListPage />} />
            <Route path="u/:username" element={<ProfilePage />} />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  )
}
