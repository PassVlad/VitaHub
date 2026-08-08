import { createContext, useState, useCallback, useEffect } from 'react'
import { api } from '../api/client'

const TOKEN_KEY = 'glumedic_access_token'
const USER_KEY = 'glumedic_user'

export const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => localStorage.getItem(TOKEN_KEY))
  const [user, setUser] = useState(() => {
    try {
      return JSON.parse(localStorage.getItem(USER_KEY))
    } catch {
      return null
    }
  })
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const t = setTimeout(() => setLoading(false), 0)
    return () => clearTimeout(t)
  }, [])

  const persist = useCallback((newToken, newUser) => {
    if (newToken) localStorage.setItem(TOKEN_KEY, newToken)
    else localStorage.removeItem(TOKEN_KEY)
    if (newUser) localStorage.setItem(USER_KEY, JSON.stringify(newUser))
    else localStorage.removeItem(USER_KEY)
  }, [])

  const login = useCallback(
    async (username, password) => {
      const data = await api.login(username, password)
      const newToken = data.access_token
      setToken(newToken)
      persist(newToken, { username })
      return data
    },
    [persist]
  )

  const register = useCallback(
    async (payload) => {
      const data = await api.register(payload)
      return data
    },
    []
  )

  const logout = useCallback(() => {
    setToken(null)
    setUser(null)
    persist(null, null)
  }, [persist])

  return (
    <AuthContext.Provider
      value={{ token, user, loading, login, register, logout, isAuth: !!token }}
    >
      {children}
    </AuthContext.Provider>
  )
}
