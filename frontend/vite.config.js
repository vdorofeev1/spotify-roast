import { fileURLToPath, URL } from 'node:url'
import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'

const repoRoot = fileURLToPath(new URL('..', import.meta.url))

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, repoRoot, 'VITE_')
  const backendTarget = env.VITE_BACKEND_URL || 'http://127.0.0.1:8080'

  return {
    envDir: repoRoot,
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
  }
})
