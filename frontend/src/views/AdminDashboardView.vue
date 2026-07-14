<script setup>
import { onMounted, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '../api/http'

const state = reactive({
  loading: false,
  stats: null,
  users: [],
  updatingRole: false
})

async function loadStats() {
  state.stats = await http.get('/admin/stats')
}

async function loadUsers() {
  state.users = await http.get('/admin/users')
}

async function loadAll() {
  state.loading = true
  try {
    await Promise.all([loadStats(), loadUsers()])
  } catch (err) {
    ElMessage.error(err.message)
  } finally {
    state.loading = false
  }
}

async function updateRole(user, role) {
  if (user.role === role) return
  try {
    await ElMessageBox.confirm(`确认将 ${user.username} 设置为 ${role}？`, '角色调整', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }

  state.updatingRole = true
  try {
    await http.put(`/admin/users/${user.id}/role`, { role })
    ElMessage.success('角色已更新')
    await loadUsers()
  } catch (err) {
    ElMessage.error(err.message)
  } finally {
    state.updatingRole = false
  }
}

onMounted(loadAll)
</script>

<template>
  <section>
    <div class="page-header">
      <div>
        <h1 class="page-title">管理员控制台</h1>
        <p class="page-subtitle">查看系统概览、用户信息，并调整用户角色。</p>
      </div>
      <div class="action-row">
        <el-button @click="loadAll">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
        <el-button type="primary" @click="$router.push('/admin/reviews')">前往审核</el-button>
      </div>
    </div>

    <section v-loading="state.loading" class="stat-grid">
      <div class="stat-card">
        <p class="stat-label">用户数</p>
        <p class="stat-value">{{ state.stats?.userCount || 0 }}</p>
      </div>
      <div class="stat-card">
        <p class="stat-label">物品数</p>
        <p class="stat-value">{{ state.stats?.itemCount || 0 }}</p>
        <el-text class="muted">待审 {{ state.stats?.pendingItemCount || 0 }} / 开放 {{ state.stats?.openItemCount || 0 }} / 匹配 {{ state.stats?.matchedItemCount || 0 }} / 关闭 {{ state.stats?.closedItemCount || 0 }}</el-text>
      </div>
      <div class="stat-card">
        <p class="stat-label">认领数</p>
        <p class="stat-value">{{ state.stats?.claimCount || 0 }}</p>
        <el-text class="muted">待审 {{ state.stats?.pendingClaimCount || 0 }} / 通过 {{ state.stats?.approvedClaimCount || 0 }} / 驳回 {{ state.stats?.rejectedClaimCount || 0 }}</el-text>
      </div>
    </section>

    <section class="panel">
      <div class="page-header">
        <div>
          <h2 class="page-title" style="font-size: 22px">系统用户</h2>
          <p class="page-subtitle">管理员可查看全部注册用户并调整角色。</p>
        </div>
      </div>

      <el-table :data="state.users" border empty-text="暂无用户数据">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" min-width="140" />
        <el-table-column prop="realName" label="姓名" min-width="140" />
        <el-table-column prop="phone" label="联系方式" min-width="150" />
        <el-table-column label="角色" width="110">
          <template #default="{ row }">
            <el-tag :type="row.role === 'ADMIN' ? 'danger' : 'info'">{{ row.role }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" min-width="180" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button-group>
              <el-button
                size="small"
                :disabled="state.updatingRole || row.role === 'USER'"
                @click="updateRole(row, 'USER')"
              >
                设为用户
              </el-button>
              <el-button
                size="small"
                type="primary"
                :disabled="state.updatingRole || row.role === 'ADMIN'"
                @click="updateRole(row, 'ADMIN')"
              >
                设为管理员
              </el-button>
            </el-button-group>
          </template>
        </el-table-column>
      </el-table>
    </section>
  </section>
</template>
