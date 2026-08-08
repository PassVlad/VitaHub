const KEY = 'glumedic_measurements'

export function getMeasurements() {
  try {
    const raw = localStorage.getItem(KEY)
    const list = raw ? JSON.parse(raw) : []
    return Array.isArray(list) ? list : []
  } catch {
    return []
  }
}

export function saveMeasurements(list) {
  localStorage.setItem(KEY, JSON.stringify(list))
}

export function addMeasurement(measurement) {
  const list = getMeasurements()
  const next = [{ id: String(Date.now()), ...measurement }, ...list]
  saveMeasurements(next)
  return next
}

export function removeMeasurement(id) {
  const list = getMeasurements().filter((m) => m.id !== id)
  saveMeasurements(list)
  return list
}

export function clearMeasurements() {
  localStorage.removeItem(KEY)
}

export function formatDateTime(date = new Date()) {
  const pad = (n) => String(n).padStart(2, '0')
  return `${pad(date.getDate())}.${pad(date.getMonth() + 1)}.${date.getFullYear()} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}
