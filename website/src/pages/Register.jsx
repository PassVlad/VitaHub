import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { motion } from 'framer-motion'
import { Activity, Eye, EyeOff, UserPlus } from 'lucide-react'
import { useAuth } from '../context/useAuth'

export default function Register() {
  const { register, login } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState({
    username: '',
    email: '',
    first_name: '',
    last_name: '',
    password: '',
    confirm: '',
  })
  const [showPass, setShowPass] = useState(false)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }))

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')

    if (!form.username.trim() || !form.email.trim() || !form.password) {
      setError('Заполните имя пользователя, email и пароль')
      return
    }
    if (form.password.length < 6) {
      setError('Пароль должен содержать минимум 6 символов')
      return
    }
    if (form.password !== form.confirm) {
      setError('Пароли не совпадают')
      return
    }

    setLoading(true)
    try {
      await register({
        username: form.username.trim(),
        email: form.email.trim(),
        password: form.password,
        first_name: form.first_name.trim(),
        last_name: form.last_name.trim(),
      })
      await login(form.username.trim(), form.password)
      navigate('/app')
    } catch (err) {
      const raw = err.message || 'Не удалось зарегистрироваться'
      let message = raw
      try {
        const parsed = JSON.parse(raw)
        message = Object.entries(parsed)
          .map(([k, v]) => `${k}: ${Array.isArray(v) ? v.join(', ') : v}`)
          .join('\n')
      } catch {
        /* plain message */
      }
      setError(message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="auth-page">
      <div className="orb" style={{ top: '5%', left: '-5%', width: 300, height: 300, background: 'rgba(139,92,246,0.15)' }} />
      <div className="orb" style={{ bottom: '5%', right: '-5%', width: 340, height: 340, background: 'rgba(217,70,239,0.15)' }} />

      <motion.div
        className="auth-card card auth-card-lg"
        initial={{ opacity: 0, y: 40 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.7, ease: [0.16, 1, 0.3, 1] }}
      >
        <div className="auth-head">
          <span className="brand-icon lg">
            <Activity size={26} />
          </span>
          <h1>Создать аккаунт</h1>
          <p>Присоединяйтесь к VitaHub — заведите цифровую медицинскую книжку</p>
        </div>

        {error && (
          <div className="alert error">
            {error.split('\n').map((line, i) => (
              <div key={i}>{line}</div>
            ))}
          </div>
        )}

        <form onSubmit={handleSubmit} className="auth-form">
          <div className="form-row">
            <div className="field">
              <label htmlFor="username">Имя пользователя *</label>
              <input
                id="username"
                className="input"
                value={form.username}
                onChange={set('username')}
                placeholder="username"
                autoComplete="username"
              />
            </div>
            <div className="field">
              <label htmlFor="email">Email *</label>
              <input
                id="email"
                className="input"
                type="email"
                value={form.email}
                onChange={set('email')}
                placeholder="you@example.com"
                autoComplete="email"
              />
            </div>
          </div>

          <div className="form-row">
            <div className="field">
              <label htmlFor="first_name">Имя</label>
              <input
                id="first_name"
                className="input"
                value={form.first_name}
                onChange={set('first_name')}
                placeholder="Иван"
              />
            </div>
            <div className="field">
              <label htmlFor="last_name">Фамилия</label>
              <input
                id="last_name"
                className="input"
                value={form.last_name}
                onChange={set('last_name')}
                placeholder="Петров"
              />
            </div>
          </div>

          <div className="form-row">
            <div className="field">
              <label htmlFor="password">Пароль *</label>
              <div className="pass-wrap">
                <input
                  id="password"
                  className="input"
                  type={showPass ? 'text' : 'password'}
                  value={form.password}
                  onChange={set('password')}
                  placeholder="Минимум 6 символов"
                  autoComplete="new-password"
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
            <div className="field">
              <label htmlFor="confirm">Повторите пароль *</label>
              <input
                id="confirm"
                className="input"
                type={showPass ? 'text' : 'password'}
                value={form.confirm}
                onChange={set('confirm')}
                placeholder="••••••••"
                autoComplete="new-password"
              />
            </div>
          </div>

          <button className="btn btn-primary btn-lg auth-submit" disabled={loading}>
            {loading ? <span className="spinner" /> : <UserPlus size={18} />}
            {loading ? 'Создаём аккаунт...' : 'Зарегистрироваться'}
          </button>
        </form>

        <p className="auth-switch">
          Уже есть аккаунт? <Link to="/login">Войти</Link>
        </p>
        <Link to="/" className="auth-back">← На главную</Link>
      </motion.div>
    </div>
  )
}
