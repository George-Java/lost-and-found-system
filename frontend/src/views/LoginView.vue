<script setup>
import { reactive } from 'vue'
import { useRouter } from 'vue-router'
import http from '../api/http'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()
const form = reactive({ username: 'user01', password: 'user123' })

async function submit() {
  try {
    const data = await http.post('/auth/login', form)
    auth.setLogin(data)
    router.push(data.role === 'ADMIN' ? '/admin' : '/')
  } catch (err) {
    alert(err.message)
  }
}
</script>

<template>
  <div class="hero">
    <section class="hero-panel">
      <span class="badge badge-warm">课程演示账号</span>
      <h1 style="margin-top:16px;">让校园失物认领流程可视化、可追踪</h1>
      <p>普通用户账号：`user01 / user123`。管理员账号：`admin / admin123`。</p>
      <p>普通用户可发布信息、提交认领材料；管理员和发布者都可以审核认领。</p>
    </section>
    <section class="card">
      <div class="section-title">
        <h2>登录</h2>
      </div>
      <div class="grid">
        <input v-model="form.username" class="input" placeholder="用户名" />
        <input v-model="form.password" class="input" type="password" placeholder="密码" />
        <button class="btn btn-primary" @click="submit">登录</button>
        <RouterLink class="btn btn-ghost" to="/register">创建账号</RouterLink>
      </div>
    </section>
  </div>
</template>
