<script setup>
import { reactive } from 'vue'
import { useRouter } from 'vue-router'
import http from '../api/http'

const router = useRouter()
const form = reactive({ username: '', password: '', realName: '', phone: '' })

async function submit() {
  try {
    await http.post('/auth/register', form)
    alert('注册成功，请登录')
    router.push('/login')
  } catch (err) {
    alert(err.message)
  }
}
</script>

<template>
  <section class="card" style="max-width:640px;margin:0 auto;">
    <div class="section-title">
      <h2>创建账号</h2>
      <RouterLink to="/login">已有账号</RouterLink>
    </div>
    <div class="grid">
      <input v-model="form.username" class="input" placeholder="用户名（4-20位）" />
      <input v-model="form.password" class="input" type="password" placeholder="密码（6-30位）" />
      <div class="grid grid-2">
        <input v-model="form.realName" class="input" placeholder="真实姓名" />
        <input v-model="form.phone" class="input" placeholder="手机号或微信号" />
      </div>
      <button class="btn btn-primary" @click="submit">注册</button>
    </div>
  </section>
</template>
