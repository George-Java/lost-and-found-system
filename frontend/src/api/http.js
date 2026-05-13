import axios from 'axios'
import { useAuthStore } from '../stores/auth'

const http = axios.create({
  baseURL: 'http://localhost:8080/api',
  timeout: 10000
})

http.interceptors.request.use((config) => {
  const auth = useAuthStore()
  if (auth.token) {
    config.headers.Authorization = `Bearer ${auth.token}`
  }
  return config
})

http.interceptors.response.use(
  (resp) => {
    const payload = resp.data
    if (payload.code !== 0) {
      return Promise.reject(new Error(payload.message || 'Request failed'))
    }
    return payload.data
  },
  (error) => Promise.reject(error)
)

export default http
