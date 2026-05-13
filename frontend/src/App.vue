<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from './stores/auth'

const router = useRouter()
const auth = useAuthStore()
const isLoggedIn = computed(() => !!auth.token)
const isAdmin = computed(() => auth.user?.role === 'ADMIN')
const showUserModules = computed(() => isLoggedIn.value && !isAdmin.value)

function logout() {
  auth.logout()
  router.push('/')
}
</script>

<template>
  <div class="app-shell">
    <header class="topbar">
      <div class="container topbar-inner">
        <RouterLink class="brand" to="/">
          <span class="brand-mark">LL</span>
          <span>
            <strong>LostLink</strong>
            <small>校园失物招领平台</small>
          </span>
        </RouterLink>
        <nav>
          <RouterLink to="/">广场</RouterLink>
          <RouterLink to="/items/create">发布</RouterLink>
          <RouterLink v-if="showUserModules" to="/items/mine">我的物品</RouterLink>
          <RouterLink v-if="showUserModules" to="/claims/mine">我的认领</RouterLink>
          <RouterLink v-if="isAdmin" to="/admin">用户管理</RouterLink>
          <RouterLink v-if="isAdmin" to="/admin/reviews">审核</RouterLink>
          <RouterLink v-if="!isLoggedIn" to="/login">登录</RouterLink>
          <RouterLink v-if="!isLoggedIn" to="/register">注册</RouterLink>
          <button v-if="isLoggedIn" class="link-btn" @click="logout">退出</button>
        </nav>
      </div>
    </header>

    <main class="container main">
      <RouterView />
    </main>
  </div>
</template>
