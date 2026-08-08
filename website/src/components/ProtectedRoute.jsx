import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from '../context/useAuth'

export default function ProtectedRoute({ children }) {
  const { isAuth, loading } = useAuth()
  const location = useLocation()

  if (loading) {
    return (
      <div style={{ minHeight: '100vh', display: 'grid', placeItems: 'center' }}>
        <div className="spinner" style={{ width: 40, height: 40 }} />
      </div>
    )
  }

  if (!isAuth) {
    return <Navigate to="/login" state={{ from: location }} replace />
  }

  return children
}
