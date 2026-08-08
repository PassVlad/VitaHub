import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { motion, useScroll, useTransform } from 'framer-motion'
import {
  Activity,
  ArrowRight,
  ChartLine,
  HeartPulse,
  Smartphone,
  Sparkles,
  Bell,
  ShieldCheck,
  Zap,
  CalendarClock,
  BookOpen,
} from 'lucide-react'
import VitalsChart from '../components/VitalsChart'

/* ---------------- Count up hook ---------------- */
function useCountUp(target, duration = 1800, start) {
  const [value, setValue] = useState(0)
  const started = useRef(false)

  useEffect(() => {
    if (!start || started.current) return
    started.current = true
    const t0 = performance.now()
    let raf
    const tick = (t) => {
      const p = Math.min((t - t0) / duration, 1)
      const eased = 1 - Math.pow(1 - p, 3)
      setValue(Math.round(target * eased))
      if (p < 1) raf = requestAnimationFrame(tick)
    }
    raf = requestAnimationFrame(tick)
    return () => cancelAnimationFrame(raf)
  }, [start, target, duration])

  return value
}

function StatItem({ value, suffix, label, start }) {
  const v = useCountUp(value, 1800, start)
  return (
    <div className="stat-item">
      <div className="stat-value">
        {v}
        <span className="gradient-text">{suffix}</span>
      </div>
      <div className="stat-label">{label}</div>
    </div>
  )
}

/* ---------------- Scroll reveal wrapper ---------------- */
function Reveal({ children, delay = 0, y = 30 }) {
  return (
    <motion.div
      initial={{ opacity: 0, y }}
      whileInView={{ opacity: 1, y: 0 }}
      viewport={{ once: true, margin: '-80px' }}
      transition={{ duration: 0.7, delay, ease: [0.16, 1, 0.3, 1] }}
    >
      {children}
    </motion.div>
  )
}

/* ---------------- Animated hero mockup ---------------- */
const GLUCOSE_METRIC = {
  key: 'glucose',
  short: 'Глюкоза',
  unit: 'ммоль/л',
  min: 3.3,
  max: 5.5,
  decimals: 1,
}

const demoData = [
  { id: '1', value: 5.2, measured_at: '2026-08-01T08:00' },
  { id: '2', value: 6.1, measured_at: '2026-08-02T13:00' },
  { id: '3', value: 4.6, measured_at: '2026-08-03T18:00' },
  { id: '4', value: 7.9, measured_at: '2026-08-04T08:00' },
  { id: '5', value: 5.8, measured_at: '2026-08-05T13:00' },
  { id: '6', value: 6.4, measured_at: '2026-08-06T18:00' },
  { id: '7', value: 5.1, measured_at: '2026-08-07T08:00' },
]

function HeroMockup() {
  return (
    <motion.div
      className="mockup"
      initial={{ opacity: 0, y: 60, rotate: 2 }}
      animate={{ opacity: 1, y: 0, rotate: 0 }}
      transition={{ duration: 1, delay: 0.3, ease: [0.16, 1, 0.3, 1] }}
    >
      <div className="mockup-phone">
        <div className="mockup-notch" />
        <div className="mockup-status">
          <span>9:41</span>
          <span className="mockup-dots">● ● ●</span>
        </div>
        <div className="mockup-app">
          <div className="mockup-head">
            <span className="brand-icon small">
              <HeartPulse size={14} />
            </span>
            <span style={{ fontWeight: 700, fontSize: 13 }}>VitaHub</span>
            <span className="mockup-live">
              <span className="live-dot" /> LIVE
            </span>
          </div>
          <div className="mockup-value">
            <span className="value-big">5.8</span>
            <span className="value-unit">ммоль/л · глюкоза</span>
          </div>
          <div className="mockup-status-chip ok">В пределах нормы</div>
          <div className="mockup-chart">
            <VitalsChart data={demoData} metric={GLUCOSE_METRIC} height={150} />
          </div>
          <div className="mockup-row">
            <div className="mini-chip"><Bell size={12} /> Напоминания</div>
          </div>
        </div>
      </div>
      <div className="mockup-orb orb" style={{ top: -60, right: -40, width: 220, height: 220, background: 'rgba(217,70,239,0.3)' }} />
    </motion.div>
  )
}

/* ---------------- Features ---------------- */
const FEATURES = [
  {
    icon: <Activity size={26} />,
    title: 'Трекер показателей',
    text: 'Давление, пульс, глюкоза, вес, температура, SpO₂ и другие показатели — все в одной медицинской книжке с привязкой ко времени.',
  },
  {
    icon: <BookOpen size={26} />,
    title: 'Библиотека документов',
    text: 'Храните анализы, справки и выписки — все в одном защищённом месте.',
  },
  {
    icon: <ChartLine size={26} />,
    title: 'Умный анализ',
    text: 'Графики динамики и цветовая индикация: норма, повышение или понижение — вы всегда знаете своё состояние.',
  },
  {
    icon: <ShieldCheck size={26} />,
    title: 'Безопасность данных',
    text: 'Личный кабинет с авторизацией и защищённым хранением вашей медицинской информации.',
  },
]

/* ---------------- Steps ---------------- */
const STEPS = [
  { icon: <CalendarClock size={24} />, title: 'Зафиксируйте', text: 'Внесите показатели или оцифруйте фото анализа.' },
  { icon: <ChartLine size={24} />, title: 'Отслеживайте', text: 'Смотрите динамику на графиках и контролируйте норму.' },
  { icon: <Sparkles size={24} />, title: 'Анализируйте', text: 'Смотрите динамику на графиках и держите здоровье под контролем.' },
]

/* ---------------- Tech ---------------- */
const TECHS = [
  'Android / Kotlin', 'FastAPI', 'PostgreSQL', 'Recharts', 'Vite + React', 'Material 3',
]

export default function Landing() {
  const heroRef = useRef(null)
  const { scrollYProgress } = useScroll({ target: heroRef, offset: ['start start', 'end start'] })
  const heroY = useTransform(scrollYProgress, [0, 1], [0, 120])
  const heroOpacity = useTransform(scrollYProgress, [0, 0.7], [1, 0])

  const [statsStart, setStatsStart] = useState(false)
  const statsRef = useRef(null)

  useEffect(() => {
    const el = statsRef.current
    if (!el) return
    const obs = new IntersectionObserver(
      ([e]) => {
        if (e.isIntersecting) setStatsStart(true)
      },
      { threshold: 0.4 }
    )
    obs.observe(el)
    return () => obs.disconnect()
  }, [])

  return (
    <div className="landing">
      {/* ============ HERO ============ */}
      <section className="hero" ref={heroRef}>
        <div className="orb" style={{ top: '8%', left: '-8%', width: 340, height: 340, background: 'rgba(167,139,250,0.18)', animation: 'float-slow 9s ease-in-out infinite' }} />
        <div className="orb" style={{ top: '30%', right: '-10%', width: 400, height: 400, background: 'rgba(217,70,239,0.16)', animation: 'float 11s ease-in-out infinite' }} />
        <div className="orb" style={{ bottom: '0%', left: '30%', width: 300, height: 300, background: 'rgba(232,121,249,0.12)', animation: 'float-slow 13s ease-in-out infinite reverse' }} />

        <motion.div className="container hero-inner" style={{ y: heroY, opacity: heroOpacity }}>
          <motion.div
            className="hero-badge"
            initial={{ opacity: 0, y: -12 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6 }}
          >
            <span className="badge-dot" />
            Ваше здоровье — под контролем
          </motion.div>

          <motion.h1
            className="hero-title"
            initial={{ opacity: 0, y: 30 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.8, delay: 0.1, ease: [0.16, 1, 0.3, 1] }}
          >
            Цифровая медицинская <span className="gradient-text">книжка</span>
          </motion.h1>

          <motion.p
            className="hero-sub"
            initial={{ opacity: 0, y: 30 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.8, delay: 0.2, ease: [0.16, 1, 0.3, 1] }}
          >
            VitaHub собирает ваши показатели, анализы и справки в одном месте:
            контроль давления и глюкозы, оцифровка документов через камеру
            и персональный ИИ-ассистент.
          </motion.p>

          <motion.div
            className="hero-actions"
            initial={{ opacity: 0, y: 30 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.8, delay: 0.35, ease: [0.16, 1, 0.3, 1] }}
          >
            <Link to="/register" className="btn btn-primary btn-lg">
              Начать бесплатно <ArrowRight size={18} />
            </Link>
            <Link to="/app" className="btn btn-ghost btn-lg">
              <Smartphone size={18} /> Попробовать веб-версию
            </Link>
          </motion.div>

          <motion.div
            className="hero-tags"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ duration: 1, delay: 0.6 }}
          >
            <span>✓ Бесплатно</span>
            <span>✓ Без рекламы</span>
            <span>✓ ИИ-ассистент</span>
            <span>✓ OCR-сканирование</span>
          </motion.div>
        </motion.div>

        <motion.div style={{ y: heroY }} className="container mockup-wrap">
          <HeroMockup />
        </motion.div>
      </section>

      {/* ============ MARQUEE ============ */}
      <section className="marquee-section">
        <div className="marquee">
          <div className="marquee-inner">
            {TECHS.concat(TECHS).map((t, i) => (
              <span key={i} className="marquee-item">{t}</span>
            ))}
          </div>
        </div>
      </section>

      {/* ============ STATS ============ */}
      <section className="container stats-section" ref={statsRef}>
        <Reveal>
          <div className="stats-grid">
            <StatItem value={8} suffix="" label="Показателей здоровья" start={statsStart} />
            <StatItem value={92} suffix="%" label="Точность OCR-распознавания" start={statsStart} />
            <StatItem value={24} suffix="/7" label="ИИ-ассистент онлайн" start={statsStart} />
            <StatItem value={3} suffix="с" label="До результатов оцифровки" start={statsStart} />
          </div>
        </Reveal>
      </section>

      {/* ============ FEATURES ============ */}
      <section className="container section">
        <Reveal>
          <div className="section-head">
            <span className="eyebrow">Возможности</span>
            <h2 className="section-title">
              Всё для здоровья <span className="gradient-text">в одном месте</span>
            </h2>
            <p className="section-sub">
              От быстрого ввода показателей до оцифровки анализов — VitaHub
              объединяет инструменты, которые реально помогают каждый день.
            </p>
          </div>
        </Reveal>

        <div className="features-grid">
          {FEATURES.map((f, i) => (
            <motion.div
              key={f.title}
              className="feature-card card"
              initial={{ opacity: 0, y: 40 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true, margin: '-60px' }}
              transition={{ duration: 0.6, delay: (i % 3) * 0.12, ease: [0.16, 1, 0.3, 1] }}
              whileHover={{ y: -8 }}
            >
              <div className="feature-icon">{f.icon}</div>
              <h3>{f.title}</h3>
              <p>{f.text}</p>
            </motion.div>
          ))}
        </div>
      </section>

      {/* ============ HOW IT WORKS ============ */}
      <section className="container section">
        <Reveal>
          <div className="section-head center">
            <span className="eyebrow">Как это работает</span>
            <h2 className="section-title">Три простых шага</h2>
          </div>
        </Reveal>

        <div className="steps-grid">
          {STEPS.map((s, i) => (
            <Reveal key={s.title} delay={i * 0.15}>
              <div className="step-card card">
                <div className="step-num">{i + 1}</div>
                <div className="feature-icon">{s.icon}</div>
                <h3>{s.title}</h3>
                <p>{s.text}</p>
                {i < STEPS.length - 1 && <div className="step-arrow">→</div>}
              </div>
            </Reveal>
          ))}
        </div>
      </section>

      {/* ============ SHOWCASE ============ */}
      <section className="container section">
        <Reveal>
          <div className="section-head center">
            <span className="eyebrow">Веб-версия</span>
            <h2 className="section-title">
              Тот же функционал — <span className="gradient-text">в браузере</span>
            </h2>
            <p className="section-sub">
              Полноценная веб-версия: трекер показателей с графиками
              и библиотека документов.
            </p>
          </div>
        </Reveal>

        <Reveal delay={0.1}>
          <div className="showcase">
            <div className="showcase-row">
              <Link to="/app" className="showcase-card card">
                <div className="showcase-icon"><Activity size={22} /></div>
                <h3>Трекер показателей</h3>
                <p>Форма ввода, графики динамики и история измерений.</p>
                <span className="showcase-go">Открыть <ArrowRight size={14} /></span>
              </Link>
              <Link to="/docs" className="showcase-card card">
                <div className="showcase-icon"><BookOpen size={22} /></div>
                <h3>Документы</h3>
                <p>Библиотека анализов и справок в одном защищённом месте.</p>
                <span className="showcase-go">Открыть <ArrowRight size={14} /></span>
              </Link>
            </div>
          </div>
        </Reveal>
      </section>

      {/* ============ CTA ============ */}
      <section className="container section">
        <Reveal>
          <div className="cta-banner">
            <div className="orb" style={{ top: -80, right: -60, width: 300, height: 300, background: 'rgba(217,70,239,0.25)' }} />
            <div className="orb" style={{ bottom: -80, left: -40, width: 260, height: 260, background: 'rgba(167,139,250,0.22)' }} />
            <div className="cta-inner">
              <h2 className="cta-title">
                Начните следить за здоровьем <span className="gradient-text">уже сегодня</span>
              </h2>
              <p className="cta-sub">
                Создайте аккаунт и получите доступ к трекеру показателей
                и библиотеке документов.
              </p>
              <div className="hero-actions">
                <Link to="/register" className="btn btn-primary btn-lg">
                  Создать аккаунт <ArrowRight size={18} />
                </Link>
                <Link to="/login" className="btn btn-ghost btn-lg">
                  Войти
                </Link>
              </div>
            </div>
          </div>
        </Reveal>
      </section>

      {/* ============ FUN FACT ============ */}
      <section className="container section">
        <Reveal>
          <div className="quote-card">
            <Zap size={28} className="quote-icon" />
            <blockquote>
              «VitaHub появился как студенческий проект, чтобы сделать контроль
              здоровья проще и понятнее для каждого. Сегодня это полноценная
              экосистема из мобильного приложения и веб-версии».
            </blockquote>
            <div className="quote-author">Команда VitaHub</div>
          </div>
        </Reveal>
      </section>
    </div>
  )
}
