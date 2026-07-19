import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    // Frontend code always calls relative /api/... paths. In dev, Vite
    // proxies those to the backend; in production, Nginx does the same
    // (see frontend/nginx.conf) - no CORS config needed on either side.
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
