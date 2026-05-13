<script setup>
import { computed, onMounted, reactive } from 'vue'
import http from '../api/http'
import { useAuthStore } from '../stores/auth'
import { parseUrlList } from '../utils/image'
import { itemStatusText, itemTypeText } from '../utils/status'

const auth = useAuthStore()
const state = reactive({
  loading: false,
  keyword: '',
  status: '',
  itemType: '',
  category: '',
  categories: [],
  page: 1,
  size: 9,
  total: 0,
  records: []
})
const isAdmin = computed(() => auth.user?.role === 'ADMIN')

async function load() {
  state.loading = true
  try {
    const data = await http.get('/items', {
      params: {
        keyword: state.keyword || undefined,
        status: state.status || undefined,
        itemType: state.itemType || undefined,
        category: state.category || undefined,
        page: state.page,
        size: state.size
      }
    })
    state.total = data.total
    state.records = data.records
  } catch (err) {
    alert(err.message)
  } finally {
    state.loading = false
  }
}

async function loadCategories() {
  try {
    state.categories = await http.get('/items/categories')
  } catch {
    state.categories = []
  }
}

function search() {
  state.page = 1
  load()
}

function nextPage() {
  if (state.page * state.size >= state.total) return
  state.page += 1
  load()
}

function prevPage() {
  if (state.page <= 1) return
  state.page -= 1
  load()
}

function firstImage(item) {
  return parseUrlList(item.imageUrls)[0] || ''
}

onMounted(() => {
  load()
  loadCategories()
})
</script>

<template>
  <div>
    <section class="hero">
      <div class="hero-panel">
        <span class="badge badge-warm">LostLink 校园</span>
        <h1 style="margin-top:16px;">从发布到认领，一套能完整演示的校园失物流程</h1>
        <p>支持物品发布、材料上传、认领申请、发布者审核、管理员总览，适合课程设计答辩演示。</p>
        <div style="display:flex;gap:10px;flex-wrap:wrap;">
          <RouterLink class="btn btn-secondary" to="/items/create">发布物品</RouterLink>
          <RouterLink v-if="isAdmin" class="btn btn-ghost" to="/admin/reviews">进入审核模块</RouterLink>
          <RouterLink v-else class="btn btn-ghost" to="/claims/mine">查看我的认领</RouterLink>
        </div>
      </div>
      <div class="card">
        <h3 style="margin-top:0;">广场说明</h3>
        <p>总帖数：{{ state.total }}</p>
        <p>可按状态、物品类型、分类和关键词筛选。</p>
        <p>详情页支持上传图片、PDF、Word、TXT 作为认领证明材料。</p>
      </div>
    </section>

    <section class="card">
      <div class="section-title">
        <h2>失物招领广场</h2>
      </div>
      <div class="grid grid-3" style="margin-bottom:14px;">
        <input v-model="state.keyword" class="input" placeholder="搜索标题、描述、地点" @keyup.enter="search" />
        <select v-model="state.itemType" class="select" @change="search">
          <option value="">全部类型</option>
          <option value="FOUND">招领启事</option>
          <option value="LOST">寻物启事</option>
        </select>
        <select v-model="state.status" class="select" @change="search">
          <option value="">全部状态</option>
          <option value="OPEN">开放中</option>
          <option value="MATCHED">已匹配</option>
          <option value="CLOSED">已关闭</option>
        </select>
      </div>
      <div class="grid grid-2" style="margin-bottom:16px;">
        <select v-model="state.category" class="select" @change="search">
          <option value="">全部分类</option>
          <option v-for="category in state.categories" :key="category" :value="category">{{ category }}</option>
        </select>
        <button class="btn btn-primary" @click="search">搜索</button>
      </div>

      <div v-if="state.loading" class="empty">加载中...</div>
      <div v-else-if="!state.records.length" class="empty">暂无数据</div>
      <div v-else class="item-grid">
        <article v-for="item in state.records" :key="item.id" class="card">
          <img v-if="firstImage(item)" :src="firstImage(item)" class="preview-img" alt="item photo" />
          <div v-else class="preview-img preview-placeholder">暂无图片</div>
          <div style="display:flex;gap:8px;flex-wrap:wrap;margin:12px 0;">
            <span class="badge">{{ itemTypeText(item.itemType) }}</span>
            <span class="badge badge-warm">{{ itemStatusText(item.status) }}</span>
          </div>
          <h3 style="margin:0 0 8px;">{{ item.title }}</h3>
          <p style="color:#5e6b7b;min-height:48px;">{{ item.description || '暂无描述' }}</p>
          <p>地点：{{ item.location || '-' }}</p>
          <p>分类：{{ item.category || '-' }}</p>
          <RouterLink class="btn btn-primary" :to="`/items/${item.id}`">查看详情</RouterLink>
        </article>
      </div>

      <div style="display:flex;justify-content:space-between;align-items:center;margin-top:18px;">
        <button class="btn btn-ghost" :disabled="state.page <= 1" @click="prevPage">上一页</button>
        <span>第 {{ state.page }} 页，共 {{ state.total }} 条</span>
        <button class="btn btn-ghost" :disabled="state.page * state.size >= state.total" @click="nextPage">下一页</button>
      </div>
    </section>
  </div>
</template>
