<script setup>
import { onMounted, reactive } from 'vue'
import http from '../api/http'
import { parseUrlList } from '../utils/image'
import { itemStatusText, itemTypeText } from '../utils/status'

const state = reactive({ records: [], loading: false })

async function load() {
  state.loading = true
  try {
    state.records = await http.get('/items/mine')
  } catch (err) {
    alert(err.message)
  } finally {
    state.loading = false
  }
}

async function closeItem(id) {
  try {
    await http.put(`/items/${id}/close`)
    await load()
  } catch (err) {
    alert(err.message)
  }
}

async function openItem(id) {
  try {
    await http.put(`/items/${id}/open`)
    await load()
  } catch (err) {
    alert(err.message)
  }
}

function firstImage(item) {
  return parseUrlList(item.imageUrls)[0] || ''
}

onMounted(load)
</script>

<template>
  <section class="card">
    <div class="section-title">
      <h2>我发布的物品</h2>
    </div>
    <div v-if="state.loading" class="empty">加载中...</div>
    <div v-else-if="!state.records.length" class="empty">你还没有发布任何物品</div>
    <div v-else class="item-grid">
      <article v-for="item in state.records" :key="item.id" class="card">
        <img v-if="firstImage(item)" :src="firstImage(item)" class="preview-img" alt="item image" />
        <div v-else class="preview-img preview-placeholder">暂无图片</div>
        <div style="display:flex;gap:8px;flex-wrap:wrap;margin:12px 0;">
          <span class="badge">{{ itemTypeText(item.itemType) }}</span>
          <span class="badge badge-warm">{{ itemStatusText(item.status) }}</span>
        </div>
        <h3 style="margin:0 0 8px;">{{ item.title }}</h3>
        <p>{{ item.description || '暂无描述' }}</p>
        <div style="display:flex;gap:8px;flex-wrap:wrap;">
          <RouterLink class="btn btn-primary" :to="`/items/${item.id}`">查看详情</RouterLink>
          <button v-if="item.status !== 'CLOSED'" class="btn btn-danger" @click="closeItem(item.id)">关闭物品</button>
          <button v-else class="btn btn-secondary" @click="openItem(item.id)">重新开放</button>
        </div>
      </article>
    </div>
  </section>
</template>
