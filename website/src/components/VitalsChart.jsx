import {
  ResponsiveContainer,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ReferenceArea,
  ReferenceLine,
  Area,
  ComposedChart,
} from 'recharts'

const PURPLE = '#a78bfa'
const FUCHSIA = '#e879f9'
const DANGER = '#f87171'
const OK = '#34d399'

function pad(n) {
  return String(n).padStart(2, '0')
}

function fmtShort(iso) {
  const d = new Date(iso)
  if (isNaN(d.getTime())) return ''
  return `${d.getDate()}.${pad(d.getMonth() + 1)}`
}

function fmtFull(iso) {
  const d = new Date(iso)
  if (isNaN(d.getTime())) return ''
  return `${pad(d.getDate())}.${pad(d.getMonth() + 1)} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function isInRange(metric, p) {
  if (!metric) return true
  let ok = true
  if (metric.min != null && p.value < metric.min) ok = false
  if (metric.max != null && p.value > metric.max) ok = false
  if (metric.two_values && p.value2 != null) {
    if (metric.min2 != null && p.value2 < metric.min2) ok = false
    if (metric.max2 != null && p.value2 > metric.max2) ok = false
  }
  return ok
}

function CustomTooltip({ active, payload, metric }) {
  if (!active || !payload || !payload.length) return null
  const m = payload[0].payload
  const unit = metric?.unit || ''
  const dec = metric?.decimals ?? 1
  return (
    <div
      style={{
        background: '#131d33',
        border: '1px solid rgba(148,163,184,0.2)',
        borderRadius: 12,
        padding: '12px 14px',
        fontSize: 13,
        boxShadow: '0 10px 30px rgba(0,0,0,0.4)',
      }}
    >
      <div style={{ color: '#94a3b8' }}>{m.full}</div>
      <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginTop: 4 }}>
        <span style={{ width: 8, height: 8, borderRadius: '50%', background: PURPLE, display: 'inline-block' }} />
        <b style={{ fontSize: 16 }}>
          {m.value.toFixed(dec)} <span style={{ color: '#94a3b8', fontWeight: 400 }}>{unit}</span>
        </b>
      </div>
      {metric?.two_values && m.value2 != null && (
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginTop: 2 }}>
          <span style={{ width: 8, height: 8, borderRadius: '50%', background: FUCHSIA, display: 'inline-block' }} />
          <b style={{ fontSize: 16 }}>
            {m.value2.toFixed(dec)} <span style={{ color: '#94a3b8', fontWeight: 400 }}>{unit}</span>
          </b>
        </div>
      )}
      {m.notes && <div style={{ color: '#64748b', marginTop: 2 }}>📝 {m.notes}</div>}
      <div style={{ color: isInRange(metric, m) ? OK : DANGER, marginTop: 2 }}>
        {isInRange(metric, m) ? 'В пределах нормы' : 'Вне нормы'}
      </div>
    </div>
  )
}

export default function VitalsChart({ data, metric, height = 320 }) {
  if (!data || !data.length) {
    return (
      <div className="chart-empty">
        <span>📈</span>
        <p>Нет данных для графика</p>
        <small>Добавьте первое измерение, чтобы увидеть динамику</small>
      </div>
    )
  }

  const m = metric || { key: 'value', unit: '', decimals: 1 }
  const rows = [...data]
    .map((p) => ({ ...p, short: fmtShort(p.measured_at), full: fmtFull(p.measured_at) }))
    .sort((a, b) => new Date(a.measured_at) - new Date(b.measured_at))

  const values = rows.flatMap((r) => [r.value, r.value2].filter((v) => typeof v === 'number'))
  const minV = m.min != null ? Math.min(m.min, ...values) : Math.min(0, ...values)
  const maxV = m.max != null ? Math.max(m.max, ...values) : Math.max(...values)
  const padRange = Math.max((maxV - minV) * 0.15, maxV * 0.05, 1)
  const yMin = Math.floor((minV - padRange) * 10) / 10
  const yMax = Math.ceil((maxV + padRange) * 10) / 10
  const gid = `vitalFill-${m.key || 'v'}`

  return (
    <div className="chart-wrap">
      <ResponsiveContainer width="100%" height={height}>
        <ComposedChart data={rows} margin={{ top: 10, right: 10, bottom: 0, left: -20 }}>
          <defs>
            <linearGradient id={gid} x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stopColor={PURPLE} stopOpacity={0.35} />
              <stop offset="100%" stopColor={PURPLE} stopOpacity={0} />
            </linearGradient>
          </defs>
          <CartesianGrid stroke="rgba(148,163,184,0.1)" strokeDasharray="4 6" />
          <XAxis
            dataKey="short"
            stroke="#77718f"
            fontSize={11}
            tickLine={false}
            axisLine={false}
            interval="preserveStartEnd"
          />
          <YAxis
            stroke="#77718f"
            fontSize={11}
            tickLine={false}
            axisLine={false}
            domain={[yMin, yMax]}
          />
          <Tooltip content={<CustomTooltip metric={m} />} />
          {m.min != null && m.max != null && (
            <ReferenceArea y1={m.min} y2={m.max} fill={PURPLE} fillOpacity={0.07} />
          )}
          {m.min != null && (
            <ReferenceLine y={m.min} stroke="#60a5fa" strokeDasharray="4 4" strokeOpacity={0.4} />
          )}
          {m.max != null && (
            <ReferenceLine y={m.max} stroke="#f87171" strokeDasharray="4 4" strokeOpacity={0.4} />
          )}
          {m.two_values && m.min2 != null && m.max2 != null && (
            <ReferenceArea y1={m.min2} y2={m.max2} fill={FUCHSIA} fillOpacity={0.05} />
          )}
          <Area
            type="monotone"
            dataKey="value"
            stroke="none"
            fill={`url(#${gid})`}
            animationDuration={900}
          />
          <Line
            type="monotone"
            dataKey="value"
            stroke={PURPLE}
            strokeWidth={2.5}
            dot={(props) => {
              const { cx, cy, payload } = props
              const color = isInRange(m, payload) ? PURPLE : DANGER
              return (
                <circle
                  key={`${m.key}-${payload.id}`}
                  cx={cx}
                  cy={cy}
                  r={5}
                  fill={color}
                  stroke="#060a13"
                  strokeWidth={2}
                />
              )
            }}
            activeDot={{ r: 7 }}
            animationDuration={900}
          />
          {m.two_values && (
            <Line
              type="monotone"
              dataKey="value2"
              stroke={FUCHSIA}
              strokeWidth={2}
              strokeDasharray="5 4"
              dot={{ r: 4, fill: FUCHSIA, stroke: '#060a13', strokeWidth: 2 }}
              activeDot={{ r: 6 }}
              animationDuration={900}
            />
          )}
        </ComposedChart>
      </ResponsiveContainer>
      {m.two_values && (
        <div className="legend">
          <span className="legend-item">
            <span className="legend-dot" style={{ background: PURPLE }} /> Систолическое
          </span>
          <span className="legend-item">
            <span className="legend-dot" style={{ background: FUCHSIA }} /> Диастолическое
          </span>
        </div>
      )}
    </div>
  )
}
