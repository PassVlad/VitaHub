import { useEffect, useRef, useState } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { Send, Bot, User, Trash2, Sparkles, BookOpen, Globe, MessageSquare } from 'lucide-react'
import { api } from '../api/client'
import { useAuth } from '../context/useAuth'

const MODES = [
  { key: 'documents', icon: BookOpen, label: 'По моим документам' },
  { key: 'internet', icon: Globe, label: 'Интернет' },
  { key: 'general', icon: MessageSquare, label: 'Общий чат' },
]

const SOURCE_LABEL = {
  documents: '📚 по документам',
  internet: '🌐 из интернета',
  general: '💬 общий чат',
}

const SUGGESTIONS_BY_MODE = {
  documents: [
    'Что сказано в моих анализах?',
    'Какие показатели у меня вне нормы?',
    'Расскажи о моих документах',
  ],
  internet: [
    'Что такое HbA1c и о чём говорит?',
    'Нормальное давление для взрослого',
    'Что влияет на уровень холестерина?',
  ],
  general: [
    'Что делать при высоком сахаре?',
    'Какие продукты снижают глюкозу?',
    'Как часто измерять давление?',
  ],
}

function formatTime(iso) {
  if (!iso) return ''
  const d = new Date(iso)
  if (isNaN(d.getTime())) return ''
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

export default function ChatPage() {
  const { token } = useAuth()
  const [messages, setMessages] = useState([])
  const [input, setInput] = useState('')
  const [mode, setMode] = useState('documents')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const bottomRef = useRef(null)

  useEffect(() => {
    let active = true
    api
      .getChatHistory(token)
      .then((data) => {
        if (!active) return
        if (Array.isArray(data)) {
          setMessages(data.map((m) => ({ ...m, isUser: false })))
        }
      })
      .catch((e) => {
        if (active) setError(`Ошибка загрузки истории: ${e.message}`)
      })
      .finally(() => active && setLoading(false))
    return () => {
      active = false
    }
  }, [token])

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages, loading])

  const send = async (text) => {
    const question = (text ?? input).trim()
    if (!question || loading) return

    setInput('')
    setError('')
    setLoading(true)
    setMessages((prev) => [
      ...prev,
      { id: `user-${Date.now()}`, question, answer: '', isUser: true, created_at: new Date().toISOString() },
    ])

    try {
      const res =
        mode === 'general'
          ? await api.sendChatMessage(token, question)
          : await api.askAssistant(token, question, mode === 'internet')
      setMessages((prev) => [
        ...prev,
        {
          id: `bot-${res.chat_id || Date.now()}`,
          question,
          answer: res.answer,
          source: res.source || mode,
          isUser: false,
          created_at: res.created_at || new Date().toISOString(),
        },
      ])
    } catch (e) {
      setError(`Ошибка: ${e.message}`)
      setMessages((prev) => [
        ...prev,
        {
          id: `err-${Date.now()}`,
          question: '',
          answer: `Не удалось получить ответ: ${e.message}. Проверьте подключение к серверу.`,
          isUser: false,
          isError: true,
          created_at: new Date().toISOString(),
        },
      ])
    } finally {
      setLoading(false)
    }
  }

  const handleSubmit = (e) => {
    e.preventDefault()
    send()
  }

  const clearHistory = () => {
    setMessages([])
    setError('')
  }

  const activeMode = MODES.find((m) => m.key === mode) || MODES[0]

  return (
    <div className="app-page chat-page">
      <div className="container chat-container">
        <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.6 }}>
          <div className="page-head chat-head">
            <div>
              <span className="eyebrow">ИИ-ассистент</span>
              <h1 className="page-title">Медицинский ассистент</h1>
              <p className="page-sub">Отвечает по вашим документам, по интернету или просто разговаривает</p>
            </div>
            {messages.length > 0 && (
              <button className="btn btn-ghost btn-sm" onClick={clearHistory}>
                <Trash2 size={16} /> Очистить
              </button>
            )}
          </div>
        </motion.div>

        <motion.div
          className="card chat-card"
          initial={{ opacity: 0, y: 30 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6, delay: 0.1 }}
        >
          <div className="chat-mode-bar">
            <div className="seg">
              {MODES.map((m) => {
                const Icon = m.icon
                return (
                  <button
                    key={m.key}
                    className={`seg-btn${mode === m.key ? ' active' : ''}`}
                    onClick={() => setMode(m.key)}
                  >
                    <Icon size={15} /> {m.label}
                  </button>
                )
              })}
            </div>
            <span className="chat-mode-hint">
              {mode === 'documents' && 'Ответ — только по загруженным документам'}
              {mode === 'internet' && 'Ответ — на основе поиска в интернете'}
              {mode === 'general' && 'Ответ — свободный диалог с ИИ'}
            </span>
          </div>

          <div className="chat-body">
            {messages.length === 0 && !loading && (
              <div className="chat-empty">
                <span className="chat-empty-icon"><Sparkles size={26} /></span>
                <h3>Спросите что угодно</h3>
                <p>Выбран режим: {activeMode.label}. Помогу разобраться с показателями, анализами и рекомендациями.</p>
                <div className="chat-suggestions">
                  {SUGGESTIONS_BY_MODE[mode].map((s) => (
                    <button key={s} className="suggestion-chip" onClick={() => send(s)}>
                      {s}
                    </button>
                  ))}
                </div>
              </div>
            )}

            <AnimatePresence initial={false}>
              {messages.map((m) => {
                if (m.isUser) {
                  return (
                    <motion.div
                      key={m.id}
                      className="chat-msg user"
                      initial={{ opacity: 0, y: 12, scale: 0.98 }}
                      animate={{ opacity: 1, y: 0, scale: 1 }}
                      transition={{ duration: 0.3 }}
                    >
                      <div className="chat-avatar user">
                        <User size={16} />
                      </div>
                      <div className="chat-bubble">
                        <div>{m.question}</div>
                        <div className="chat-time">{formatTime(m.created_at)}</div>
                      </div>
                    </motion.div>
                  )
                }
                return (
                  <motion.div
                    key={m.id}
                    className="chat-msg bot"
                    initial={{ opacity: 0, y: 12, scale: 0.98 }}
                    animate={{ opacity: 1, y: 0, scale: 1 }}
                    transition={{ duration: 0.3 }}
                  >
                    <div className="chat-avatar bot">
                      <Bot size={16} />
                    </div>
                    <div className={`chat-bubble${m.isError ? ' error' : ''}`}>
                      {m.source && !m.isError && (
                        <div className="chat-source">{SOURCE_LABEL[m.source] || m.source}</div>
                      )}
                      <div className="chat-answer">{m.answer || '🤔 Думаю...'}</div>
                      {m.created_at && <div className="chat-time">{formatTime(m.created_at)}</div>}
                    </div>
                  </motion.div>
                )
              })}
            </AnimatePresence>

            {loading && (
              <div className="chat-msg bot">
                <div className="chat-avatar bot">
                  <Bot size={16} />
                </div>
                <div className="chat-bubble typing">
                  <span className="typing-dot" />
                  <span className="typing-dot" />
                  <span className="typing-dot" />
                </div>
              </div>
            )}

            {error && <div className="alert error chat-error">{error}</div>}
            <div ref={bottomRef} />
          </div>

          <form className="chat-input-row" onSubmit={handleSubmit}>
            <input
              className="input chat-input"
              placeholder={
                mode === 'documents'
                  ? 'Вопрос по вашим документам...'
                  : mode === 'internet'
                  ? 'Вопрос для поиска в интернете...'
                  : 'Напишите вопрос о здоровье...'
              }
              value={input}
              onChange={(e) => setInput(e.target.value)}
              disabled={loading}
            />
            <button type="submit" className="btn btn-primary" disabled={loading || !input.trim()}>
              <Send size={18} />
            </button>
          </form>
        </motion.div>
      </div>
    </div>
  )
}
