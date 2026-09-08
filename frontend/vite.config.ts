/// <reference types="vitest/config" />
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { visualizer } from 'rollup-plugin-visualizer'

function vendorChunk(id: string): string | undefined {
  const normalized = id.replaceAll('\\', '/')
  if (!normalized.includes('/node_modules/')) {
    return undefined
  }

  if (
    normalized.includes('/node_modules/react/') ||
    normalized.includes('/node_modules/react-dom/') ||
    normalized.includes('/node_modules/scheduler/')
  ) {
    return 'react-vendor'
  }

  if (normalized.includes('/node_modules/axios/')) {
    return 'axios'
  }

  if (normalized.includes('/node_modules/@tanstack/')) {
    return 'query'
  }

  return undefined
}

export default defineConfig({
  plugins: [
    react(),
    visualizer({
      filename: 'dist/stats.html',
      gzipSize: true,
      open: false,
      template: 'treemap',
    }),
  ],
  build: {
    rollupOptions: {
      output: {
        manualChunks: vendorChunk,
      },
    },
  },
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    css: true,
  },
})
