import { Link } from 'react-router-dom'
import { HeartPulse, Heart } from 'lucide-react'

export default function Footer() {
  return (
    <footer className="footer">
      <div className="container">
        <div className="footer-grid">
          <div>
            <div className="brand">
              <span className="brand-icon">
                <HeartPulse size={20} />
              </span>
              <span className="brand-text">
                Vita<span>Hub</span>
              </span>
            </div>
            <p className="footer-desc">
              Цифровая медицинская книжка: контроль жизненных показателей,
              хранение документов и графики динамики — в одном сервисе.
            </p>
          </div>

          <div>
            <h4>Навигация</h4>
            <ul>
              <li><Link to="/">Главная</Link></li>
              <li><Link to="/login">Вход</Link></li>
              <li><Link to="/register">Регистрация</Link></li>
            </ul>
          </div>

          <div>
            <h4>Возможности</h4>
            <ul>
              <li><Link to="/app">Жизненные показатели</Link></li>
              <li><Link to="/docs">Документы</Link></li>
            </ul>
          </div>

          <div>
            <h4>Проект</h4>
            <ul>
              <li><a href="#">О проекте</a></li>
              <li><a href="#">Документация</a></li>
              <li><a href="#">Версия 3.0</a></li>
            </ul>
          </div>
        </div>

        <div className="footer-bottom">
          <span>© {new Date().getFullYear()} VitaHub. Все права защищены.</span>
          <span className="footer-made">
            Сделано с <Heart size={14} fill="currentColor" color="#f87171" /> для здоровья
          </span>
        </div>
      </div>
    </footer>
  )
}
