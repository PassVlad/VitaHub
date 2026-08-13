const OVERRIDE = import.meta.env.VITE_API_URL

const TOKEN_KEY = 'glumedic_access_token'
const USER_KEY = 'glumedic_user'

// Сессия недействительна (токен просрочен или пароль изменён) —
// сбрасываем локальные данные и отправляем на страницу входа.
function handleUnauthorized() {
  if (localStorage.getItem(TOKEN_KEY)) {
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
    if (window.location.pathname !== '/login') {
      window.location.href = '/login'
    }
  }
}

// По умолчанию используем относительные пути (dev-прокси в vite.config.js
// перенаправляет /api на бэкенд). При необходимости можно переопределить
// через VITE_API_URL — например, для продакшена за reverse-proxy.
function normalize(url) {
  const clean = url.replace(/^\/+/, '')
  if (OVERRIDE) {
    return OVERRIDE.replace(/\/+$/, '') + '/' + clean
  }
  return '/' + clean
}

function parseBody(text) {
  if (!text) return null
  try {
    return JSON.parse(text)
  } catch {
    return text
  }
}

// FastAPI/DRF отдают ошибки по-разному: detail может быть строкой,
// объектом или массивом {msg, loc, type}. Превращаем всё в читаемую строку.
function extractErrorMessage(data, status) {
  if (typeof data === 'string') return data
  if (data && typeof data === 'object') {
    const detail = data.detail
    if (typeof detail === 'string') return detail
    if (Array.isArray(detail)) {
      return detail
        .map((item) => {
          if (typeof item === 'string') return item
          if (item && item.msg) {
            const field = item.loc && item.loc.length ? `(${item.loc[item.loc.length - 1]})` : ''
            return `${field ? field + ' ' : ''}${item.msg}`
          }
          return null
        })
        .filter(Boolean)
        .join('; ') || `Ошибка сервера (${status})`
    }
    if (detail && typeof detail === 'object') {
      return JSON.stringify(detail)
    }
    if (typeof data.error === 'string') return data.error
    if (typeof data.message === 'string') return data.message
    return JSON.stringify(data)
  }
  return `Ошибка сервера (${status})`
}

async function request(url, options = {}) {
  const { headers, ...rest } = options

  const res = await fetch(normalize(url), {
    ...rest,
    headers: {
      'Content-Type': 'application/json',
      ...(headers || {}),
    },
  })

  const data = parseBody(await res.text())

  if (!res.ok) {
    if (res.status === 401) handleUnauthorized()
    throw new Error(extractErrorMessage(data, res.status))
  }

  return data
}

export const api = {
  async register(payload) {
    return request('api/register/', { method: 'POST', body: JSON.stringify(payload) })
  },

  async login(username, password) {
    const body = new URLSearchParams()
    body.append('username', username)
    body.append('password', password)

    const res = await fetch(normalize('api/login/'), {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body,
    })

    const data = parseBody(await res.text())

    if (!res.ok) {
      throw new Error(extractErrorMessage(data, res.status))
    }
    return data
  },

  async getChatHistory(token) {
    return request('api/chat/', {
      headers: { Authorization: `Bearer ${token}` },
    })
  },

  async sendChatMessage(token, question) {
    return request('api/chat/', {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}` },
      body: JSON.stringify({ question }),
    })
  },

  async askAssistant(token, query, useInternet = false) {
    return request('api/ask/', {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}` },
      body: JSON.stringify({ query, use_internet: useInternet }),
    })
  },

  // ---------- Vital signs ----------
  async getMetrics(token) {
    return request('api/metrics/', {
      headers: { Authorization: `Bearer ${token}` },
    })
  },

  async getDashboard(token) {
    return request('api/dashboard/', {
      headers: { Authorization: `Bearer ${token}` },
    })
  },

  async getMeasurements(token, metric, limit = 500) {
    const q = new URLSearchParams({ limit: String(limit) })
    if (metric) q.set('metric', metric)
    return request(`api/measurements/?${q}`, {
      headers: { Authorization: `Bearer ${token}` },
    })
  },

  async getSeries(token, metric, days) {
    const q = days ? `?days=${days}` : ''
    return request(`api/measurements/${metric}/${q}`, {
      headers: { Authorization: `Bearer ${token}` },
    })
  },

  async getStats(token, metric) {
    return request(`api/measurements/stats/?metric=${metric}`, {
      headers: { Authorization: `Bearer ${token}` },
    })
  },

  async addMeasurement(token, payload) {
    return request('api/measurements/', {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}` },
      body: JSON.stringify(payload),
    })
  },

  async deleteMeasurement(token, id) {
    return request(`api/measurements/${id}/`, {
      method: 'DELETE',
      headers: { Authorization: `Bearer ${token}` },
    })
  },

  // ---------- Documents ----------
  async listDocuments(token) {
    return request('api/documents/list/', {
      headers: { Authorization: `Bearer ${token}` },
    })
  },

  async uploadDocumentText(token, text, filename) {
    return request('api/documents/upload/', {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}` },
      body: JSON.stringify({ text, filename: filename || 'document.txt' }),
    })
  },

  async uploadDocumentFile(token, file) {
    const form = new FormData()
    form.append('file', file)
    const res = await fetch(normalize('api/documents/upload-file/'), {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}` },
      body: form,
    })
    const data = parseBody(await res.text())
    if (!res.ok) {
      throw new Error(extractErrorMessage(data, res.status))
    }
    return data
  },

  async deleteDocument(token, id) {
    return request(`api/documents/${id}/`, {
      method: 'DELETE',
      headers: { Authorization: `Bearer ${token}` },
    })
  },

  async recognizeImage(file) {
    const form = new FormData()
    form.append('file', file)

    const res = await fetch(normalize('api/ocr-fast/'), {
      method: 'POST',
      body: form,
    })

    const data = parseBody(await res.text())

    if (!res.ok) {
      throw new Error(extractErrorMessage(data, res.status))
    }
    return data
  },

  getBaseUrl() {
    return OVERRIDE || (window.location.origin + '/api')
  },
}
