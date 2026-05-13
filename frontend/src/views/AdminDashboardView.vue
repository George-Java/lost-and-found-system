<script setup>
import { onMounted, reactive } from 'vue'
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
    alert(err.message)
  } finally {
    state.loading = false
  }
}

async function updateRole(user, role) {
  if (user.role === role) return
  state.updatingRole = true
  try {
    await http.put(`/admin/users/${user.id}/role`, { role })
    await loadUsers()
  } catch (err) {
    alert(err.message)
  } finally {
    state.updatingRole = false
  }
}

onMounted(loadAll)
</script>

<template>
  <section class="grid">
    <div class="section-title">
      <div>
        <h2>管理员用户管理</h2>
        <p style="margin:6px 0 0;color:#667085;">管理员负责管理系统用户，不参与认领。</p>
      </div>
      <button class="btn btn-ghost" @click="loadAll">刷新</button>
    </div>

    <div v-if="state.loading" class="card empty">加载中...</div>

    <template v-else>
      <section class="grid grid-3">
        <article class="card">
          <h3 style="margin-top:0;">用户数</h3>
          <p style="font-size:30px;font-weight:800;margin:8px 0 0;">{{ state.stats?.userCount || 0 }}</p>
        </article>
        <article class="card">
          <h3 style="margin-top:0;">物品数</h3>
          <p style="font-size:30px;font-weight:800;margin:8px 0 0;">{{ state.stats?.itemCount || 0 }}</p>
          <p>开放中 {{ state.stats?.openItemCount || 0 }} / 已匹配 {{ state.stats?.matchedItemCount || 0 }} / 已关闭 {{ state.stats?.closedItemCount || 0 }}</p>
        </article>
        <article class="card">
          <h3 style="margin-top:0;">认领数</h3>
          <p style="font-size:30px;font-weight:800;margin:8px 0 0;">{{ state.stats?.claimCount || 0 }}</p>
          <p>待审核 {{ state.stats?.pendingClaimCount || 0 }} / 已通过 {{ state.stats?.approvedClaimCount || 0 }} / 已驳回 {{ state.stats?.rejectedClaimCount || 0 }}</p>
        </article>
      </section>

      <section class="card">
        <div class="section-title">
          <div>
            <h3 style="margin:0;">系统用户管理</h3>
            <p style="margin:6px 0 0;color:#667085;">可查看所有注册用户，并调整其角色。当前登录管理员不能把自己降级为普通用户。</p>
          </div>
          <RouterLink class="btn btn-primary" to="/admin/reviews">前往审核模块</RouterLink>
        </div>
        <div v-if="!state.users.length" class="empty">暂无用户数据</div>
        <div v-else class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>用户ID</th>
                <th>用户名</th>
                <th>姓名</th>
                <th>联系方式</th>
                <th>角色</th>
                <th>创建时间</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="user in state.users" :key="user.id">
                <td>{{ user.id }}</td>
                <td>{{ user.username }}</td>
                <td>{{ user.realName }}</td>
                <td>{{ user.phone || '-' }}</td>
                <td>{{ user.role }}</td>
                <td>{{ user.createdAt || '-' }}</td>
                <td>
                  <div style="display:flex;gap:8px;flex-wrap:wrap;">
                    <button class="btn btn-secondary" :disabled="state.updatingRole || user.role === 'USER'" @click="updateRole(user, 'USER')">设为用户</button>
                    <button class="btn btn-primary" :disabled="state.updatingRole || user.role === 'ADMIN'" @click="updateRole(user, 'ADMIN')">设为管理员</button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </template>
  </section>
</template>
