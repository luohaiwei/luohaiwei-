import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  css: {
    preprocessorOptions: {
      scss: {
        // 消除 Dart Sass 2.0 前的 legacy-js-api 弃用警告
        silenceDeprecations: ['legacy-js-api']
      }
    }
  },
  server: {
    port: 5175,
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8081',
        changeOrigin: true,
        configure: (proxy) => {
          proxy.on('error', (err) => {
            // 后端没启动时静默处理，不往终端刷红字
          })
        }
      }
    }
  }
})
