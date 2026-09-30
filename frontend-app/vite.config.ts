/// <reference types="vitest" />
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: './src/setupTests.ts',
  },
  server: {
    proxy: {
      '/api/productos': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/api/inventarios': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      },
      '/api/usuarios': {
        target: 'http://localhost:8082',
        changeOrigin: true,
      },
      '/api/pedidos': {
        target: 'http://localhost:8083',
        changeOrigin: true,
      }
    }
  }
})
