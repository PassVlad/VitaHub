import { useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { motion } from 'framer-motion'
import { ScanLine, Upload, FileText, Copy, Check, ImagePlus, FolderPlus, CheckCircle2, Loader2 } from 'lucide-react'
import { api } from '../api/client'
import { useAuth } from '../context/useAuth'

export default function OcrPage() {
  const { token } = useAuth()
  const fileRef = useRef(null)
  const [preview, setPreview] = useState(null)
  const [file, setFile] = useState(null)
  const [loading, setLoading] = useState(false)
  const [result, setResult] = useState(null)
  const [error, setError] = useState('')
  const [copied, setCopied] = useState(false)
  const [saving, setSaving] = useState(false)
  const [saved, setSaved] = useState(false)

  const handleFile = (f) => {
    if (!f) return
    if (!f.type.startsWith('image/')) {
      setError('Выберите изображение (JPG, PNG и т.д.)')
      return
    }
    setError('')
    setResult(null)
    setFile(f)
    setSaved(false)
    setPreview(URL.createObjectURL(f))
  }

  const onDrop = (e) => {
    e.preventDefault()
    handleFile(e.dataTransfer.files?.[0])
  }

  const digitize = async () => {
    if (!file) return
    setLoading(true)
    setError('')
    setResult(null)
    try {
      const res = await api.recognizeImage(file)
      if (res && res.error) {
        setError(`Ошибка сервера: ${res.error}`)
      } else {
        setResult(res?.text || 'Текст не распознан')
      }
    } catch (e) {
      setError(
        `Не удалось подключиться к серверу распознавания. Проверьте, что сервер запущен (${api.getBaseUrl()}). ${e.message}`
      )
    } finally {
      setLoading(false)
    }
  }

  const copyResult = async () => {
    if (!result) return
    try {
      await navigator.clipboard.writeText(result)
      setCopied(true)
      setTimeout(() => setCopied(false), 2000)
    } catch {
      /* clipboard unavailable */
    }
  }

  const saveToDocuments = async () => {
    if (!result || !token || saving) return
    setSaving(true)
    setError('')
    try {
      const base = file ? file.name.replace(/\.[^.]+$/, '') : 'document'
      await api.uploadDocumentText(token, result, `${base}.txt`)
      setSaved(true)
    } catch (e) {
      setError(`Не удалось сохранить в документы: ${e.message}`)
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="app-page">
      <div className="container">
        <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.6 }}>
          <div className="page-head">
            <div>
              <span className="eyebrow">OCR</span>
              <h1 className="page-title">Оцифровка документов</h1>
              <p className="page-sub">Загрузите фото показателей или анализа — текст распознается автоматически</p>
            </div>
          </div>
        </motion.div>

        <div className="ocr-grid">
          <motion.div
            className="card panel"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6, delay: 0.1 }}
          >
            <div
              className="dropzone"
              onDragOver={(e) => e.preventDefault()}
              onDrop={onDrop}
              onClick={() => fileRef.current?.click()}
            >
              <input
                ref={fileRef}
                type="file"
                accept="image/*"
                style={{ display: 'none' }}
                onChange={(e) => handleFile(e.target.files?.[0])}
              />
              {preview ? (
                <img src={preview} alt="Предпросмотр" className="dropzone-preview" />
              ) : (
                <div className="dropzone-placeholder">
                  <span className="dropzone-icon"><ImagePlus size={32} /></span>
                  <p>Перетащите изображение сюда</p>
                  <small>или нажмите, чтобы выбрать файл</small>
                </div>
              )}
            </div>

            <div className="ocr-actions">
              {preview && (
                <button className="btn btn-ghost" onClick={() => { fileRef.current?.click() }}>
                  <Upload size={16} /> Другое изображение
                </button>
              )}
              <button
                className="btn btn-primary btn-lg auth-submit"
                onClick={digitize}
                disabled={!file || loading}
              >
                {loading ? <span className="spinner" /> : <ScanLine size={18} />}
                {loading ? 'Распознаём...' : 'Распознать текст'}
              </button>
            </div>
          </motion.div>

          <motion.div
            className="card panel"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6, delay: 0.2 }}
          >
            <div className="panel-title">
              <FileText size={18} /> Результат распознавания
            </div>

            {error && <div className="alert error">{error}</div>}

            {!result && !error && (
              <div className="empty-state ocr-empty">
                <span>🔍</span>
                <p>Результат появится здесь</p>
                <small>Выберите изображение слева и нажмите «Распознать текст»</small>
              </div>
            )}

            {result && (
              <>
                <div className="ocr-result">
                  <pre>{result}</pre>
                </div>
                {saved && (
                  <div className="alert success">
                    <CheckCircle2 size={16} /> Документ сохранён. Теперь ассистент сможет отвечать по нему.
                  </div>
                )}
                <div className="ocr-actions">
                  <button className="btn btn-ghost auth-submit" onClick={copyResult}>
                    {copied ? <Check size={16} /> : <Copy size={16} />}
                    {copied ? 'Скопировано!' : 'Копировать текст'}
                  </button>
                  <button
                    className="btn btn-primary btn-lg auth-submit"
                    onClick={saveToDocuments}
                    disabled={saving || !token}
                  >
                    {saving ? <Loader2 size={16} className="spin" /> : <FolderPlus size={16} />}
                    {saving ? 'Сохраняем...' : 'Сохранить в документы'}
                  </button>
                </div>
                {token && (
                  <p className="doc-hint center-hint">
                    <Link to="/docs">Перейти в раздел документов</Link>
                  </p>
                )}
              </>
            )}
          </motion.div>
        </div>
      </div>
    </div>
  )
}
