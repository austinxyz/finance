import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  server: {
    port: 3000,
    // Hot Module Replacement (HMR) configuration
    hmr: {
      overlay: true, // Show error overlay in browser
    },
    // Watch options for better file change detection
    watch: {
      usePolling: false, // Set to true if running in Docker or having issues
    },
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },
  // Build optimizations
  build: {
    sourcemap: true, // Enable source maps for debugging
  }
})
