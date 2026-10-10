import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/auth'

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 10000
})

http.interceptors.request.use((config) => {
  const auth = useAuthStore()
  if (auth.token) {
    config.headers.Authorization = `Bearer ${auth.token}`
  }
  return config
})

// 统一错误提示：业务码非 0、网络/HTTP 错误都在这里弹一次，
// 业务代码不必再写 try/catch 只为了提示；需要自己处理的请求传 { silent: true }。
function fail(message, config) {
  if (!config?.silent) {
    ElMessage.error(message)
  }
  const error = new Error(message)
  // 标记为已提示，main.js 里据此不再打印未处理异常
  error.notified = true
  return error
}

http.interceptors.response.use(
  (resp) => {
    const payload = resp.data
    if (payload.code !== 0) {
      return Promise.reject(fail(payload.message || 'Request failed', resp.config))
    }
    return payload.data
  },
  (error) => {
    const payload = error.response?.data
    return Promise.reject(fail(payload?.message || error.message || 'Request failed', error.config))
  }
)

export default http
