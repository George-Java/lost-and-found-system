<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from './stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const helpVisible = ref(false)
const isLoggedIn = computed(() => !!auth.token)
const isAdmin = computed(() => auth.user?.role === 'ADMIN')
const showUserModules = computed(() => isLoggedIn.value && !isAdmin.value)
const activeMenu = computed(() => {
  if (route.path.startsWith('/admin/reviews')) return '/admin/reviews'
  if (route.path.startsWith('/admin')) return '/admin'
  if (route.path.startsWith('/items/mine')) return '/items/mine'
  if (route.path.startsWith('/claims/mine')) return '/claims/mine'
  if (route.path.startsWith('/messages')) return '/messages'
  if (route.path.startsWith('/items/create')) return '/items/create'
  return '/'
})

function logout() {
  auth.logout()
  ElMessage.success('已退出登录')
  router.push('/')
}
</script>

<template>
  <el-container class="app-shell">
    <el-header class="topbar">
      <div class="topbar-inner">
        <RouterLink class="brand" to="/">
          <span class="brand-mark">LL</span>
          <span class="brand-copy">
            <strong>LostLink</strong>
            <small>校园失物招领平台</small>
          </span>
        </RouterLink>

        <el-menu
          class="main-menu"
          mode="horizontal"
          :default-active="activeMenu"
          :ellipsis="false"
          router
        >
          <el-menu-item index="/">
            <el-icon><Search /></el-icon>
            <span>广场</span>
          </el-menu-item>
          <el-menu-item v-if="!isLoggedIn || !isAdmin" index="/items/create">
            <el-icon><Plus /></el-icon>
            <span>发布</span>
          </el-menu-item>
          <el-menu-item v-if="showUserModules" index="/items/mine">
            <el-icon><Collection /></el-icon>
            <span>我的物品</span>
          </el-menu-item>
          <el-menu-item v-if="showUserModules" index="/claims/mine">
            <el-icon><Tickets /></el-icon>
            <span>我的认领</span>
          </el-menu-item>
          <el-menu-item v-if="isLoggedIn" index="/messages">
            <el-icon><ChatDotRound /></el-icon>
            <span>消息</span>
          </el-menu-item>
          <el-menu-item v-if="isAdmin" index="/admin">
            <el-icon><User /></el-icon>
            <span>用户管理</span>
          </el-menu-item>
          <el-menu-item v-if="isAdmin" index="/admin/reviews">
            <el-icon><Checked /></el-icon>
            <span>审核</span>
          </el-menu-item>
        </el-menu>

        <div class="account-actions">
          <el-tooltip content="使用帮助" placement="bottom">
            <el-button circle @click="helpVisible = true">
              <el-icon><QuestionFilled /></el-icon>
            </el-button>
          </el-tooltip>
          <template v-if="!isLoggedIn">
            <el-button text @click="router.push('/login')">登录</el-button>
            <el-button type="primary" @click="router.push('/register')">注册</el-button>
          </template>
          <el-dropdown v-else trigger="click">
            <el-button>
              <el-icon><UserFilled /></el-icon>
              {{ auth.user?.realName || auth.user?.username }}
              <el-icon class="el-icon--right"><ArrowDown /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="router.push('/messages')">系统消息</el-dropdown-item>
                <el-dropdown-item divided @click="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </el-header>

    <el-main class="main container">
      <RouterView />
    </el-main>

    <el-drawer v-model="helpVisible" title="LostLink 使用帮助" size="420px">
      <el-steps direction="vertical" :active="4">
        <el-step title="普通用户发布帖子" description="登录后点击发布，填写物品信息并提交。新帖子会进入待审核状态，管理员通过后才会出现在广场。" />
        <el-step title="普通用户认领物品" description="在广场进入物品详情，填写认领原因并上传证明材料，等待管理员审核。" />
        <el-step title="管理员审核" description="管理员不发布帖子、不提交认领，只在审核页面处理寻物/招领帖子和认领申请。" />
        <el-step title="添加好友后通信" description="进入消息页，搜索用户名或姓名并发送好友申请；对方同意后才会出现在好友列表，双方才能实时聊天。" />
      </el-steps>
      <el-alert
        style="margin-top: 18px"
        title="演示账号"
        type="info"
        :closable="false"
        description="管理员：admin / admin123；普通用户：user01 / user123。"
      />
    </el-drawer>
  </el-container>
</template>
