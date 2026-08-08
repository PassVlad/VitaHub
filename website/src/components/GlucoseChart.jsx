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

const TARGET_MIN = 3.9
const TARGET_MAX = 7.8

function statusOf(v) {
  if (v < TARGET_MIN) return 'low'
  if (v > TARGET_MAX) return 'high'
  return 'normal'
}

const STATUS_COLOR = {
  low: '#38bdf8',
  normal: '#2dd4bf',
  high: '#f87171',
}

function CustomTooltip({ active, payload }) {
  if (!active || !payload || !payload.length) return null
  const m = payload[0].payload
  const status = statusOf(m.glucoseLevel)
  const statusText = {
    low: 'Низкий',
    normal: 'Норма',
    high: 'Повышен',
  }[status]

  return (
    <div
      style={{
        background: '#0e1626',
        border: '1px solid rgba(148,163,184,0.2)',
        borderRadius: 12,
        padding: '12px 14px',
        fontSize: 13,
        boxShadow: '0 10px 30px rgba(0,0,0,0.4)',
      }}
    >
      <div style={{ color: '#94a3b8' }}>{m.dateTime}</div>
      <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginTop: 4 }}>
        <span
          style={{
            width: 8,
            height: 8,
            borderRadius: '50%',
            background: STATUS_COLOR[status],
            display: 'inline-block',
          }}
        />
        <b style={{ fontSize: 16 }}>
          {m.glucoseLevel.toFixed(1)} <span style={{ color: '#94a3b8', fontWeight: 400 }}>ммоль/л</span>
        </b>
      </div>
      {m.mealTime && <div style={{ color: '#64748b', marginTop: 2 }}>🍽 {m.mealTime}</div>}
      <div style={{ color: STATUS_COLOR[status], marginTop: 2 }}>{statusText}</div>
    </div>
  )
}

export default function GlucoseChart({ data }) {
  if (!data || data.length === 0) {
    return (
      <div className="chart-empty">
        <span>📉</span>
        <p>Нет данных для графика</p>
        <small>Добавьте первое измерение, чтобы увидеть динамику</small>
      </div>
    )
  }

  const sorted = [...data].sort((a, b) => new Date(a.iso) - new Date(b.iso))

  return (
    <div className="chart-wrap">
      <ResponsiveContainer width="100%" height={320}>
        <ComposedChart data={sorted} margin={{ top: 10, right: 10, bottom: 0, left: -20 }}>
          <defs>
            <linearGradient id="glucoseFill" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stopColor="#2dd4bf" stopOpacity={0.35} />
              <stop offset="100%" stopColor="#2dd4bf" stopOpacity={0} />
            </linearGradient>
          </defs>
          <CartesianGrid stroke="rgba(148,163,184,0.1)" strokeDasharray="4 6" />
          <XAxis
            dataKey="short"
            stroke="#64748b"
            fontSize={11}
            tickLine={false}
            axisLine={false}
            interval="preserveStartEnd"
          />
          <YAxis
            stroke="#64748b"
            fontSize={11}
            tickLine={false}
            axisLine={false}
            domain={[0, (dataMax) => Math.ceil(Math.max(dataMax, TARGET_MAX) + 2)]}
          />
          <Tooltip content={<CustomTooltip />} />
          <ReferenceArea y1={TARGET_MIN} y2={TARGET_MAX} fill="#2dd4bf" fillOpacity={0.06} />
          <ReferenceLine y={TARGET_MIN} stroke="#38bdf8" strokeDasharray="4 4" strokeOpacity={0.4} />
          <ReferenceLine y={TARGET_MAX} stroke="#f87171" strokeDasharray="4 4" strokeOpacity={0.4} />
          <Area
            type="monotone"
            dataKey="glucoseLevel"
            stroke="none"
            fill="url(#glucoseFill)"
            animationDuration={900}
          />
          <Line
            type="monotone"
            dataKey="glucoseLevel"
            stroke="#2dd4bf"
            strokeWidth={2.5}
            dot={(props) => {
              const { cx, cy, payload } = props
              const color = STATUS_COLOR[statusOf(payload.glucoseLevel)]
              return (
                <circle
                  key={payload.id}
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
        </ComposedChart>
      </ResponsiveContainer>
    </div>
  )
}
