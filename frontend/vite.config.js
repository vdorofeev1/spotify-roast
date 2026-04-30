import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

const backendTarget = 'http://127.0.0.1:8080'

export default defineConfig({
  plugins: [react()],
  server: {
    host: '127.0.0.1',
    port: 3000,
    strictPort: true,
    proxy: {
      '/api': backendTarget,
      '/login': backendTarget,
      '/oauth2': backendTarget,
      '/logout': backendTarget,
    },
  },
})
