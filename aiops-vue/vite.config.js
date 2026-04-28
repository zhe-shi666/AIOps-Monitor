import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  define: {
    global: 'globalThis'
  },
  build: {
    chunkSizeWarningLimit: 800,
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (!id.includes('node_modules')) return null
          if (id.includes('echarts')) return 'vendor-echarts'
          if (id.includes('element-plus') || id.includes('@element-plus')) return 'vendor-element'
          if (id.includes('sockjs-client') || id.includes('stompjs')) return 'vendor-realtime'
          if (id.includes('vue') || id.includes('vue-router') || id.includes('pinia')) return 'vendor-vue'
          return 'vendor-misc'
        }
      }
    }
  }
})
