<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import http from '../api/http'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()
const formRef = ref()
const loading = ref(false)
const form = reactive({ username: 'user01', password: 'user123' })

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function submit() {
  await formRef.value?.validate()
  loading.value = true
  try {
    const data = await http.post('/auth/login', form)
    auth.setLogin(data)
    ElMessage.success('登录成功')
    router.push(data.role === 'ADMIN' ? '/admin' : '/')
  } catch (err) {
    ElMessage.error(err.message)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <section class="auth-layout">
    <div class="auth-intro">
      <el-tag type="warning" effect="dark" style="width: fit-content">课程设计演示账号</el-tag>
      <h1 class="page-title" style="margin-top: 18px; color: #fff">安全登录与权限控制</h1>
      <p>后端通过 Spring Security 过滤链校验 Bearer Token，密码使用 BCrypt 加密存储，旧的明文演示密码会在登录后自动升级。</p>
      <p>普通用户：user01 / user123；管理员：admin / admin123。</p>
    </div>

    <el-card shadow="never">
      <template #header>
        <div class="page-header" style="margin-bottom: 0">
          <div>
            <h2 class="page-title" style="font-size: 22px">登录</h2>
            <p class="page-subtitle">进入系统后可发布物品、认领、审核或发送消息。</p>
          </div>
        </div>
      </template>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="submit">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" autocomplete="username" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" autocomplete="current-password" show-password />
        </el-form-item>
        <el-button type="primary" :loading="loading" style="width: 100%" @click="submit">登录</el-button>
        <el-button text style="width: 100%; margin: 10px 0 0" @click="router.push('/register')">创建新账号</el-button>
      </el-form>
    </el-card>
  </section>
</template>
