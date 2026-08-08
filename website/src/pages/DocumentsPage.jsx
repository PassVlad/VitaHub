import { useCallback, useEffect, useRef, useState } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { FileText, Upload, Trash2, FolderOpen, Search, Check, Loader2, FileUp } from 'lucide-react'
import { api } from '../api/client'
import { useAuth } from '../context/useAuth'

function fmtDate(iso) {
  if (!iso) return ''
  const d = new Date(iso)
  if (isNaN(d.getTime())) return ''
  const pad = (n) => String(n).padStart(2, '0')
  return `${pad(d.getDate())}.${pad(d.getMonth() + 1)}.${d.getFullYear()}`
}

function fmtSize(bytes) {
  if (bytes == null) return ''
  if (bytes < 1024) return `${bytes} Б`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} КБ`
  return `${(bytes / (1024 * 1024)).toFixed(2)} МБ`
}

export default function DocumentsPage() {
  const { token } = useAuth()
  const fileRef = useRef(null)
  const [docs, setDocs] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [query, setQuery] = useState('')

  const [text, setText] = useState('')
  const [fileName, setFileName] = useState('')
  const [textSending, setTextSending] = useState(false)

  const [fileUploading, setFileUploading] = useState(false)
  const [deleting, setDeleting] = useState(null)

  const load = useCallback(async () => {
    try {
      const data = await api.listDocuments(token)
      setDocs(Array.isArray(data) ? data : [])
    } catch (e) {
      setError(`Не удалось загрузить документы: ${e.message}`)
    } finally {
      setLoading(false)
    }
  }, [token])

  useEffect(() => {
    load()
  }, [load])

  const filtered = docs.filter((d) =>
    !query.trim() ||
    (d.filename || '').toLowerCase().includes(query.trim().toLowerCase()) ||
    (d.text_preview || '').toLowerCase().includes(query.trim().toLowerCase())
  )

  const handleTextSubmit = async (e) => {
    e.preventDefault()
    if (!text.trim()) return
    setError('')
    setSuccess('')
    setTextSending(true)
    try {
      await api.uploadDocumentText(token, text.trim(), fileName.trim() || 'document.txt')
      setText('')
      setFileName('')
      setSuccess('✓ Документ добавлен')
      setTimeout(() => setSuccess(''), 2500)
      await load()
    } catch (err) {
      setError(`Ошибка: ${err.message}`)
    } finally {
      setTextSending(false)
    }
  }

  const handleFileUpload = async (e) => {
    const f = e.target.files?.[0]
    e.target.value = ''
    if (!f) return
    setError('')
    setSuccess('')
    setFileUploading(true)
    try {
      await api.uploadDocumentFile(token, f)
      setSuccess('✓ Файл загружен и оцифрован')
      setTimeout(() => setSuccess(''), 3000)
      await load()
    } catch (err) {
      setError(`Ошибка загрузки файла: ${err.message}`)
    } finally {
      setFileUploading(false)
    }
  }

  const handleDelete = async (id) => {
    setDeleting(id)
    setError('')
    try {
      await api.deleteDocument(token, id)
      setDocs((prev) => prev.filter((d) => d.id !== id))
    } catch (err) {
      setError(`Ошибка удаления: ${err.message}`)
    } finally {
      setDeleting(null)
    }
  }

  return (
    <div className="app-page">
      <div className="container">
        <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.6 }}>
          <div className="page-head">
            <div>
              <span className="eyebrow">Библиотека</span>
              <h1 className="page-title">Мои документы</h1>
              <p className="page-sub">Храните анализы и справки — ассистент сможет отвечать по их содержимому</p>
            </div>
          </div>
        </motion.div>

        {(error || success) && (
          <div className={`alert ${error ? 'error' : 'success'}`}>{error || success}</div>
        )}

        <div className="dash-grid">
          {/* Upload panels */}
          <motion.div
            className="card panel"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6, delay: 0.1 }}
          >
            <div className="panel-title">
              <FileText size={18} /> Добавить текст
            </div>
            <form onSubmit={handleTextSubmit} className="measure-form">
              <div className="field">
                <label htmlFor="doc-name">Название</label>
                <input
                  id="doc-name"
                  className="input"
                  placeholder="Например: Анализ крови, июль"
                  value={fileName}
                  onChange={(e) => setFileName(e.target.value)}
                />
              </div>
              <div className="field">
                <label htmlFor="doc-text">Текст документа</label>
                <textarea
                  id="doc-text"
                  className="input textarea"
                  rows={5}
                  placeholder="Вставьте содержимое анализа или справки..."
                  value={text}
                  onChange={(e) => setText(e.target.value)}
                />
              </div>
              <button type="submit" className="btn btn-primary btn-lg auth-submit" disabled={textSending || !text.trim()}>
                {textSending ? <Loader2 size={18} className="spin" /> : <Upload size={18} />}
                {textSending ? 'Сохраняем...' : 'Сохранить текст'}
              </button>
            </form>

            <div className="panel-sep" />

            <div className="panel-title">
              <FileUp size={18} /> Загрузить файл
            </div>
            <p className="doc-hint">PDF, изображения и текстовые файлы. Изображения автоматически оцифровываются (OCR).</p>
            <button
              className="btn btn-primary btn-lg auth-submit"
              onClick={() => fileRef.current?.click()}
              disabled={fileUploading}
            >
              {fileUploading ? <Loader2 size={18} className="spin" /> : <FileUp size={18} />}
              {fileUploading ? 'Загружаем...' : 'Выбрать файл'}
            </button>
            <input ref={fileRef} type="file" style={{ display: 'none' }} onChange={handleFileUpload} />
          </motion.div>

          {/* Documents list */}
          <motion.div
            className="card panel"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6, delay: 0.2 }}
          >
            <div className="panel-title">
              <FolderOpen size={18} /> Документы
              <span className="panel-count">{docs.length}</span>
            </div>

            {docs.length > 1 && (
              <div className="field" style={{ marginBottom: 14 }}>
                <div className="input-icon">
                  <Search size={16} />
                  <input
                    className="input"
                    placeholder="Поиск по названию и содержимому..."
                    value={query}
                    onChange={(e) => setQuery(e.target.value)}
                  />
                </div>
              </div>
            )}

            {loading ? (
              <div style={{ display: 'grid', placeItems: 'center', minHeight: 240 }}>
                <div className="spinner" />
              </div>
            ) : filtered.length === 0 ? (
              <div className="empty-state">
                <span>📄</span>
                <p>{docs.length === 0 ? 'Документов пока нет' : 'Ничего не найдено'}</p>
                <small>Добавьте первый документ слева</small>
              </div>
            ) : (
              <div className="doc-list">
                <AnimatePresence initial={false}>
                  {filtered.map((d) => (
                    <motion.div
                      key={d.id}
                      layout
                      initial={{ opacity: 0, y: -10 }}
                      animate={{ opacity: 1, y: 0 }}
                      exit={{ opacity: 0, x: -30 }}
                      transition={{ duration: 0.3 }}
                      className="doc-item"
                    >
                      <div className="doc-icon">
                        <FileText size={20} />
                      </div>
                      <div className="doc-main">
                        <div className="doc-name">{d.filename || 'Без названия'}</div>
                        <div className="doc-meta">
                          <span>🕒 {fmtDate(d.created_at)}</span>
                          {d.file_size != null && <span>· {fmtSize(d.file_size)}</span>}
                        </div>
                        {d.text_preview && (
                          <div className="doc-preview">{d.text_preview}</div>
                        )}
                      </div>
                      <span className={`doc-badge ${d.has_text ? 'ok' : 'no'}`}>
                        {d.has_text ? <Check size={12} /> : null}
                        {d.has_text ? 'В индексе' : 'Без текста'}
                      </span>
                      <button
                        className="history-del"
                        onClick={() => handleDelete(d.id)}
                        disabled={deleting === d.id}
                        aria-label="Удалить документ"
                        title="Удалить"
                      >
                        {deleting === d.id ? <Loader2 size={16} className="spin" /> : <Trash2 size={16} />}
                      </button>
                    </motion.div>
                  ))}
                </AnimatePresence>
              </div>
            )}
          </motion.div>
        </div>
      </div>
    </div>
  )
}
