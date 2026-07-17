import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
    plugins: [vue()],
    server: {
        host: '0.0.0.0',  // 允许外部IP访问
        port: 5173,
        allowedHosts: [
            '.serveousercontent.com',
            'localhost',
            '.trycloudflare.com',
            'www.lostfound.icu',
            'lostfound.icu'
        ],
        proxy: {
            '/api': {
                target: 'http://localhost:8080',
                changeOrigin: true
            },
            '/uploads': {
                target: 'http://localhost:8080',
                changeOrigin: true
            },
            '/ws': {
                target: 'ws://localhost:8080',
                ws: true,
                changeOrigin: true
            }
        }
    }
})