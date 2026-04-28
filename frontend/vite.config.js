import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

const backendTarget = 'http://localhost:8080'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
    proxy: {
      '/api': backendTarget,
      '/login': backendTarget,
      '/oauth2': backendTarget,
      '/logout': backendTarget,
    },
  },
})
