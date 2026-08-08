import { lazy, Suspense } from 'react'
import { Routes, Route, Navigate } from 'react-router-dom'
import Navbar from './components/Navbar'
import Footer from './components/Footer'
import ProtectedRoute from './components/ProtectedRoute'
import { useAuth } from './context/useAuth'

const Landing = lazy(() => import('./pages/Landing'))
const Login = lazy(() => import('./pages/Login'))
const Register = lazy(() => import('./pages/Register'))
const Dashboard = lazy(() => import('./pages/Dashboard'))
const DocumentsPage = lazy(() => import('./pages/DocumentsPage'))

function PageLoader() {
  return (
    <div style={{ minHeight: '100vh', display: 'grid', placeItems: 'center' }}>
      <div className="spinner" style={{ width: 40, height: 40 }} />
    </div>
  )
}

// Главная/авторизация недоступны, если пользователь уже вошёл
function HomeRoute() {
  const { isAuth } = useAuth()
  return isAuth ? <Navigate to="/app" replace /> : <Landing />
}

function GuestRoute({ children }) {
  const { isAuth } = useAuth()
  return isAuth ? <Navigate to="/app" replace /> : children
}

export default function App() {
  return (
    <>
      <Navbar />
      <Suspense fallback={<PageLoader />}>
        <Routes>
          <Route path="/" element={<HomeRoute />} />
          <Route
            path="/login"
            element={
              <GuestRoute>
                <Login />
              </GuestRoute>
            }
          />
          <Route
            path="/register"
            element={
              <GuestRoute>
                <Register />
              </GuestRoute>
            }
          />
          <Route
            path="/app"
            element={
              <ProtectedRoute>
                <Dashboard />
              </ProtectedRoute>
            }
          />
          <Route
            path="/docs"
            element={
              <ProtectedRoute>
                <DocumentsPage />
              </ProtectedRoute>
            }
          />
        </Routes>
      </Suspense>
      <Footer />
    </>
  )
}
