import { useCallback, useEffect, useMemo, useState } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import {
  Plus,
  Trash2,
  Gauge,
  ClipboardList,
  CalendarClock,
  StickyNote,
  Droplets,
  Activity,
  Heart,
  Thermometer,
  Scale,
  Wind,
  FlaskConical,
  Pill,
} from 'lucide-react'
import VitalsChart from '../components/VitalsChart'
import { api } from '../api/client'
import { useAuth } from '../context/useAuth'

const ICONS = {
  droplets: Droplets,
  activity: Activity,
  heart: Heart,
  thermometer: Thermometer,
  scale: Scale,
  wind: Wind,
  'flask-conical': FlaskConical,
  pill: Pill,
}

function inRange(metric, p) {
  let ok = true
  if (metric.min != null && p.value < metric.min) ok = false
  if (metric.max != null && p.value > metric.max) ok = false
  if (metric.two_values && p.value2 != null) {
    if (metric.min2 != null && p.value2 < metric.min2) ok = false
    if (metric.max2 != null && p.value2 > metric.max2) ok = false
  }
  return ok
}

function statusOf(metric, p) {
  if (!p) return { label: 'Нет данных', cls: 'empty', color: '#77718f' }
  return inRange(metric, p)
    ? { label: 'В норме', cls: 'normal', color: '#34d399' }
    : { label: 'Вне нормы', cls: 'elevated', color: '#f87171' }
}

function fmtDateTime(iso) {
  if (!iso) return ''
  const d = new Date(iso)
  if (isNaN(d.getTime())) return ''
  const pad = (n) => String(n).padStart(2, '0')
  return `${pad(d.getDate())}.${pad(d.getMonth() + 1)}.${d.getFullYear()} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function nowLocalInput() {
  const d = new Date()
  d.setMinutes(d.getMinutes() - d.getTimezoneOffset())
  return d.toISOString().slice(0, 16)
}

export default function Dashboard() {
  const { token } = useAuth()
  const [metrics, setMetrics] = useState([])
  const [latest, setLatest] = useState({})
  const [activeKey, setActiveKey] = useState(null)
  const [series, setSeries] = useState([])
  const [loading, setLoading] = useState(true)
  const [seriesLoading, setSeriesLoading] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  const [value, setValue] = useState('')
  const [value2, setValue2] = useState('')
  const [datetime, setDatetime] = useState(nowLocalInput)
  const [notes, setNotes] = useState('')

  const active = useMemo(
    () => metrics.find((m) => m.key === activeKey) || null,
    [metrics, activeKey]
  )

  const loadOverview = useCallback(async () => {
    try {
      const dash = await api.getDashboard(token)
      setMetrics(dash.metrics)
      setLatest(dash.latest)
      setActiveKey((prev) => prev || (dash.metrics[0]?.key ?? 'glucose'))
    } catch (e) {
      setError(`Не удалось загрузить показатели: ${e.message}`)
    } finally {
      setLoading(false)
    }
  }, [token])

  const loadSeries = useCallback(
    async (key) => {
      if (!key) return
      setSeriesLoading(true)
      try {
        const data = await api.getSeries(token, key)
        setSeries(Array.isArray(data) ? data : [])
      } catch (e) {
        setError(`Не удалось загрузить историю: ${e.message}`)
      } finally {
        setSeriesLoading(false)
      }
    },
    [token]
  )

  useEffect(() => {
    loadOverview()
  }, [loadOverview])

  useEffect(() => {
    loadSeries(activeKey)
  }, [activeKey, loadSeries])

  const refreshAll = async () => {
    await Promise.all([loadOverview(), loadSeries(activeKey)])
  }

  const handleSave = async (e) => {
    e.preventDefault()
    setError('')
    setSuccess('')

    const num = parseFloat(String(value).replace(',', '.'))
    if (isNaN(num)) {
      setError('Введите значение показателя')
      return
    }
    let num2 = null
    if (active?.two_values) {
      num2 = parseFloat(String(value2).replace(',', '.'))
      if (isNaN(num2)) {
        setError('Укажите второе значение (диастолическое)')
        return
      }
    }

    const measured_at = datetime ? new Date(datetime).toISOString() : undefined

    try {
      await api.addMeasurement(token, {
        metric: activeKey,
        value: num,
        ...(num2 != null ? { value2: num2 } : {}),
        ...(measured_at ? { measured_at } : {}),
        ...(notes.trim() ? { notes: notes.trim() } : {}),
      })
      setValue('')
      setValue2('')
      setNotes('')
      setSuccess('✓ Измерение сохранено')
      setTimeout(() => setSuccess(''), 2500)
      await refreshAll()
    } catch (err) {
      setError(`Ошибка сохранения: ${err.message}`)
    }
  }

  const handleDelete = async (id) => {
    try {
      await api.deleteMeasurement(token, id)
      await refreshAll()
    } catch (err) {
      setError(`Ошибка удаления: ${err.message}`)
    }
  }

  const sortedAsc = [...series].sort(
    (a, b) => new Date(a.measured_at) - new Date(b.measured_at)
  )
  const sortedDesc = [...series].sort(
    (a, b) => new Date(b.measured_at) - new Date(a.measured_at)
  )

  const stats = useMemo(() => {
    if (!sortedAsc.length) return null
    const values = sortedAsc.map((r) => r.value)
    const avg = values.reduce((a, b) => a + b, 0) / values.length
    const min = Math.min(...values)
    const max = Math.max(...values)
    const inR = sortedAsc.filter((r) => inRange(active, r)).length
    return {
      avg,
      min,
      max,
      inRangePct: (inR / sortedAsc.length) * 100,
      total: sortedAsc.length,
    }
  }, [sortedAsc, active])

  if (loading) {
    return (
      <div className="app-page" style={{ display: 'grid', placeItems: 'center' }}>
        <div className="spinner" style={{ width: 40, height: 40 }} />
      </div>
    )
  }

  const dec = active?.decimals ?? 1

  return (
    <div className="app-page">
      <div className="container">
        <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.6 }}>
          <div className="page-head">
            <div>
              <span className="eyebrow">Жизненные показатели</span>
              <h1 className="page-title">Цифровая медицинская книжка</h1>
              <p className="page-sub">Вносите показатели, следите за динамикой и нормой</p>
            </div>
          </div>
        </motion.div>

        {/* Metric cards (latest) */}
        {metrics.length > 0 && (
          <motion.div className="metric-grid" initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.6, delay: 0.05 }}>
            {metrics.map((m) => {
              const Icon = ICONS[m.icon] || Gauge
              const last = latest[m.key]
              const st = statusOf(m, last)
              const disp = last
                ? m.two_values
                  ? `${last.value.toFixed(m.decimals)}/${last.value2?.toFixed(m.decimals) ?? '–'}`
                  : last.value.toFixed(m.decimals)
                : '—'
              return (
                <button key={m.key} className={`card metric-card${activeKey === m.key ? ' active-metric' : ''}`} onClick={() => setActiveKey(m.key)}>
                  <div className="metric-card-top">
                    <span className="metric-card-name">{m.short}</span>
                    <span className="metric-card-icon"><Icon size={16} /></span>
                  </div>
                  <div className="metric-card-value">
                    {disp} <small>{m.unit}</small>
                  </div>
                  <span className={`metric-status ${st.cls}`}>{st.label}</span>
                </button>
              )
            })}
          </motion.div>
        )}

        <div className="dash-grid">
          {/* Form */}
          <motion.div className="card panel" initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.6, delay: 0.15 }}>
            <div className="panel-title">
              <Plus size={18} /> Новое измерение
            </div>

            {(error || success) && (
              <div className={`alert ${error ? 'error' : 'success'}`}>{error || success}</div>
            )}

            {active ? (
              <form onSubmit={handleSave} className="measure-form">
                <p className="metric-desc">{active.description}</p>

                <div className="field">
                  <label htmlFor="value">
                    {active.two_values ? 'Систолическое' : active.name} ({active.unit})
                  </label>
                  <div className="input-icon">
                    <Activity size={16} />
                    <input
                      id="value"
                      className="input"
                      type="number"
                      step={active.decimals ? 10 ** -active.decimals : 1}
                      placeholder="Например: 5.8"
                      value={value}
                      onChange={(e) => setValue(e.target.value)}
                    />
                  </div>
                </div>

                {active.two_values && (
                  <div className="field">
                    <label htmlFor="value2">Диастолическое ({active.unit})</label>
                    <div className="input-icon">
                      <Activity size={16} />
                      <input
                        id="value2"
                        className="input"
                        type="number"
                        step={active.decimals ? 10 ** -active.decimals : 1}
                        placeholder="Например: 80"
                        value={value2}
                        onChange={(e) => setValue2(e.target.value)}
                      />
                    </div>
                  </div>
                )}

                <div className="field">
                  <label htmlFor="datetime">Дата и время</label>
                  <div className="input-icon">
                    <CalendarClock size={16} />
                    <input
                      id="datetime"
                      className="input"
                      type="datetime-local"
                      value={datetime}
                      onChange={(e) => setDatetime(e.target.value)}
                    />
                  </div>
                </div>

                <div className="field">
                  <label htmlFor="notes">Заметки</label>
                  <div className="input-icon">
                    <StickyNote size={16} />
                    <input
                      id="notes"
                      className="input"
                      placeholder="Самочувствие, активность..."
                      value={notes}
                      onChange={(e) => setNotes(e.target.value)}
                    />
                  </div>
                </div>

                <button type="submit" className="btn btn-primary btn-lg auth-submit">
                  <Plus size={18} /> Сохранить измерение
                </button>
              </form>
            ) : (
              <div className="empty-state">
                <span>💜</span>
                <p>Нет доступных метрик</p>
              </div>
            )}
          </motion.div>

          {/* Chart */}
          <motion.div className="card panel" initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.6, delay: 0.2 }}>
            <div className="panel-title">
              <Activity size={18} /> Динамика: {active?.short ?? ''}
              {sortedAsc.length > 0 && <span className="panel-count">{sortedAsc.length}</span>}
            </div>

            {stats && (
              <div className="stat-mini-grid">
                <div className="stat-mini">
                  <div className="stat-mini-label">Среднее</div>
                  <div className="stat-mini-value">{stats.avg.toFixed(dec)}</div>
                </div>
                <div className="stat-mini">
                  <div className="stat-mini-label">Минимум</div>
                  <div className="stat-mini-value">{stats.min.toFixed(dec)}</div>
                </div>
                <div className="stat-mini">
                  <div className="stat-mini-label">Максимум</div>
                  <div className="stat-mini-value">{stats.max.toFixed(dec)}</div>
                </div>
                <div className="stat-mini">
                  <div className="stat-mini-label">В норме</div>
                  <div className="stat-mini-value">{Math.round(stats.inRangePct)}%</div>
                </div>
              </div>
            )}

            {seriesLoading && series.length === 0 ? (
              <div style={{ display: 'grid', placeItems: 'center', minHeight: 320 }}>
                <div className="spinner" />
              </div>
            ) : (
              <VitalsChart data={sortedAsc} metric={active} />
            )}
          </motion.div>
        </div>

        {/* History */}
        <motion.div className="card panel" initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.6, delay: 0.25 }}>
          <div className="panel-title">
            <ClipboardList size={18} /> История: {active?.short ?? ''}
            <span className="panel-count">{sortedDesc.length}</span>
          </div>

          {sortedDesc.length === 0 ? (
            <div className="empty-state">
              <span>📋</span>
              <p>История пуста</p>
              <small>Добавьте первое измерение через форму</small>
            </div>
          ) : (
            <div className="history-list">
              <AnimatePresence initial={false}>
                {sortedDesc.map((m) => {
                  const st = statusOf(active, m)
                  const disp = active?.two_values
                    ? `${m.value.toFixed(dec)}/${m.value2?.toFixed(dec) ?? '–'}`
                    : m.value.toFixed(dec)
                  return (
                    <motion.div
                      key={m.id}
                      layout
                      initial={{ opacity: 0, y: -10 }}
                      animate={{ opacity: 1, y: 0 }}
                      exit={{ opacity: 0, x: -30 }}
                      transition={{ duration: 0.3 }}
                      className="history-item"
                    >
                      <span className="history-status" style={{ background: st.color }} />
                      <div className="history-main">
                        <div className="history-value" style={{ color: st.color }}>
                          {disp} <small>{active?.unit}</small>
                        </div>
                        <div className="history-meta">
                          <span>🕒 {fmtDateTime(m.measured_at)}</span>
                          {m.notes && <span>📝 {m.notes}</span>}
                          {stats && <span style={{ color: 'var(--text-dim)' }}>· показано на графике</span>}
                        </div>
                      </div>
                      <span className={`history-badge ${st.cls}`}>{st.label}</span>
                      <button
                        className="history-del"
                        onClick={() => handleDelete(m.id)}
                        aria-label="Удалить измерение"
                        title="Удалить"
                      >
                        <Trash2 size={16} />
                      </button>
                    </motion.div>
                  )
                })}
              </AnimatePresence>
            </div>
          )}
        </motion.div>
      </div>
    </div>
  )
}
