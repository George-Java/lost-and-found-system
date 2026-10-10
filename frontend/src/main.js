import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import App from './App.vue'
import router from './router'
import 'element-plus/dist/index.css'
import './style.css'

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: zhCn })

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// 请求失败的提示已由 api/http.js 的拦截器统一弹出，这里避免浏览器再打印一遍未处理的 Promise 异常
window.addEventListener('unhandledrejection', (event) => {
  if (event.reason?.notified) {
    event.preventDefault()
  }
})

app.mount('#app')
