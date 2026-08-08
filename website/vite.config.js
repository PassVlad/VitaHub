import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  // GitHub Pages с кастомным доменом отдаёт сайт с корня, поэтому '/' по умолчанию.
  // Для предпросмотра на PassVlad.github.io/GluMedic собирайте с VITE_BASE=/GluMedic/
  base: process.env.VITE_BASE || '/',
  plugins: [react()],
  server: {
    proxy: {
      '/api': {
        target: process.env.VITE_API_URL || 'http://10.128.79.95:8000',
        changeOrigin: true,
      },
    },
  },
})
