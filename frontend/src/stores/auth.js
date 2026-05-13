import { defineStore } from 'pinia'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('lf_token') || '',
    user: JSON.parse(localStorage.getItem('lf_user') || 'null')
  }),
  actions: {
    setLogin(payload) {
      this.token = payload.token
      this.user = payload
      localStorage.setItem('lf_token', payload.token)
      localStorage.setItem('lf_user', JSON.stringify(payload))
    },
    logout() {
      this.token = ''
      this.user = null
      localStorage.removeItem('lf_token')
      localStorage.removeItem('lf_user')
    }
  }
})
