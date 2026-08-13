import { NavLink, Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/useAuth'
import { HeartPulse, LogOut, FolderOpen, Bot, ScanLine, Menu, X } from 'lucide-react'
import { useState } from 'react'

export default function Navbar() {
  const { isAuth, logout } = useAuth()
  const [open, setOpen] = useState(false)
  const navigate = useNavigate()

  const linkClass = ({ isActive }) =>
    'nav-link' + (isActive ? ' active' : '')

  const handleLogout = () => {
    logout()
    navigate('/')
  }

  return (
    <header className="navbar">
      <div className="container nav-inner">
        <Link to="/" className="brand">
          <span className="brand-icon">
            <HeartPulse size={22} />
          </span>
          <span className="brand-text">
            Vita<span>Hub</span>
          </span>
        </Link>

        <nav className="nav-links">
          {!isAuth && (
            <NavLink to="/" className={linkClass} end>
              Главная
            </NavLink>
          )}
          {isAuth && (
            <>
              <NavLink to="/app" className={linkClass}>
                Показатели
              </NavLink>
              <NavLink to="/chat" className={linkClass}>
                Мегамозг
              </NavLink>
              <NavLink to="/docs" className={linkClass}>
                Документы
              </NavLink>
              <NavLink to="/ocr" className={linkClass}>
                Оцифровка
              </NavLink>
            </>
          )}
        </nav>

        <div className="nav-actions">
          {isAuth ? (
            <button className="btn btn-ghost btn-sm" onClick={handleLogout}>
              <LogOut size={16} /> Выйти
            </button>
          ) : (
            <div className="nav-actions">
              <Link to="/login" className="btn btn-ghost btn-sm">
                Войти
              </Link>
              <Link to="/register" className="btn btn-primary btn-sm">
                Регистрация
              </Link>
            </div>
          )}
        </div>

        <button
          className="nav-burger"
          onClick={() => setOpen((v) => !v)}
          aria-label="Меню"
        >
          {open ? <X size={24} /> : <Menu size={24} />}
        </button>
      </div>

      {open && (
        <div className="nav-mobile">
          {!isAuth && (
            <NavLink to="/" className={linkClass} end onClick={() => setOpen(false)}>
              Главная
            </NavLink>
          )}
          {isAuth ? (
            <>
              <NavLink to="/app" className={linkClass} onClick={() => setOpen(false)}>
                Показатели
              </NavLink>
              <NavLink to="/chat" className={linkClass} onClick={() => setOpen(false)}>
                <Bot size={16} /> Мегамозг
              </NavLink>
              <NavLink to="/docs" className={linkClass} onClick={() => setOpen(false)}>
                <FolderOpen size={16} /> Документы
              </NavLink>
              <NavLink to="/ocr" className={linkClass} onClick={() => setOpen(false)}>
                <ScanLine size={16} /> Оцифровка
              </NavLink>
              <button className="btn btn-ghost btn-sm" onClick={handleLogout}>
                <LogOut size={16} /> Выйти
              </button>
            </>
          ) : (
            <>
              <NavLink to="/login" className={linkClass} onClick={() => setOpen(false)}>
                Войти
              </NavLink>
              <NavLink to="/register" className={linkClass} onClick={() => setOpen(false)}>
                Регистрация
              </NavLink>
            </>
          )}
        </div>
      )}
    </header>
  )
}
