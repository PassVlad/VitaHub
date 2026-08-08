import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { motion } from 'framer-motion'
import { Activity, Eye, EyeOff, LogIn } from 'lucide-react'
import { useAuth } from '../context/useAuth'

export default function Login() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [showPass, setShowPass] = useState(false)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    if (!username.trim() || !password) {
      setError('Заполните все поля')
      return
    }
    setLoading(true)
    try {
      await login(username.trim(), password)
      navigate('/app')
    } catch (err) {
      setError(err.message || 'Не удалось войти. Проверьте данные.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="auth-page">
      <div className="orb" style={{ top: '5%', left: '-5%', width: 300, height: 300, background: 'rgba(139,92,246,0.15)' }} />
      <div className="orb" style={{ bottom: '5%', right: '-5%', width: 340, height: 340, background: 'rgba(217,70,239,0.15)' }} />

      <motion.div
        className="auth-card card"
        initial={{ opacity: 0, y: 40 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.7, ease: [0.16, 1, 0.3, 1] }}
      >
        <div className="auth-head">
          <span className="brand-icon lg">
            <Activity size={26} />
          </span>
          <h1>С возвращением</h1>
          <p>Войдите в свой аккаунт VitaHub</p>
        </div>

        {error && <div className="alert error">{error}</div>}

        <form onSubmit={handleSubmit} className="auth-form">
          <div className="field">
            <label htmlFor="username">Имя пользователя</label>
            <input
              id="username"
              className="input"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              placeholder="username"
              autoComplete="username"
            />
          </div>

          <div className="field">
            <label htmlFor="password">Пароль</label>
            <div className="pass-wrap">
              <input
                id="password"
                className="input"
                type={showPass ? 'text' : 'password'}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••"
                autoComplete="current-password"
              />
              <button
                type="button"
                className="pass-toggle"
                onClick={() => setShowPass((v) => !v)}
                aria-label="Показать пароль"
              >
                {showPass ? <EyeOff size={18} /> : <Eye size={18} />}
              </button>
            </div>
          </div>

          <button className="btn btn-primary btn-lg auth-submit" disabled={loading}>
            {loading ? <span className="spinner" /> : <LogIn size={18} />}
            {loading ? 'Входим...' : 'Войти'}
          </button>
        </form>

        <p className="auth-switch">
          Нет аккаунта? <Link to="/register">Зарегистрироваться</Link>
        </p>
        <Link to="/" className="auth-back">← На главную</Link>
      </motion.div>
    </div>
  )
}
