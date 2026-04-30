import { Navigate, Route, Routes } from 'react-router-dom'
import LoginPage from './pages/LoginPage'
import TopTracksPage from './pages/TopTracksPage'
import RoastPage from './pages/RoastPage'

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<LoginPage />} />
      <Route path="/top-tracks" element={<TopTracksPage />} />
      <Route path="/roast" element={<RoastPage />} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
