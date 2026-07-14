<script setup>
import { computed, onMounted, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import http from '../api/http'
import { useAuthStore } from '../stores/auth'
import { parseUrlList } from '../utils/image'
import { itemStatusText, itemTypeText, tagTypeForItemStatus, tagTypeForItemType } from '../utils/status'

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
    ElMessage.error(err.message)
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

function resetFilters() {
  state.keyword = ''
  state.status = ''
  state.itemType = ''
  state.category = ''
  search()
}

function firstImage(item) {
  return parseUrlList(item.imageUrls)[0] || ''
}

function pageChange(page) {
  state.page = page
  load()
}

onMounted(() => {
  load()
  loadCategories()
})
</script>

<template>
  <section>
    <div class="page-header">
      <div>
        <h1 class="page-title">失物招领广场</h1>
        <p class="page-subtitle">浏览校园失物和招领信息，按类型、状态、分类和关键词快速筛选。</p>
      </div>
      <div class="action-row">
        <el-button v-if="!isAdmin" type="primary" @click="$router.push('/items/create')">
          <el-icon><Plus /></el-icon>
          发布物品
        </el-button>
        <el-button v-if="isAdmin" @click="$router.push('/admin/reviews')">
          <el-icon><Checked /></el-icon>
          审核认领
        </el-button>
      </div>
    </div>

    <section class="panel">
      <el-form class="toolbar" label-position="top" @submit.prevent>
        <el-input
          v-model="state.keyword"
          clearable
          placeholder="搜索标题、描述或地点"
          style="max-width: 300px"
          @keyup.enter="search"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-select v-model="state.itemType" clearable placeholder="全部类型" style="width: 160px" @change="search">
          <el-option label="招领启事" value="FOUND" />
          <el-option label="寻物启事" value="LOST" />
        </el-select>
        <el-select v-model="state.status" clearable placeholder="全部状态" style="width: 160px" @change="search">
          <el-option label="开放中" value="OPEN" />
          <el-option label="已匹配" value="MATCHED" />
          <el-option label="已关闭" value="CLOSED" />
        </el-select>
        <el-select v-model="state.category" clearable filterable placeholder="全部分类" style="width: 180px" @change="search">
          <el-option v-for="category in state.categories" :key="category" :label="category" :value="category" />
        </el-select>
        <el-button type="primary" @click="search">搜索</el-button>
        <el-button @click="resetFilters">重置</el-button>
      </el-form>
    </section>

    <section class="panel">
      <div class="page-header">
        <div>
          <h2 class="page-title" style="font-size: 22px">最新信息</h2>
          <p class="page-subtitle">共 {{ state.total }} 条记录</p>
        </div>
      </div>

      <el-skeleton v-if="state.loading" :rows="6" animated />
      <el-empty v-else-if="!state.records.length" description="暂无物品信息" />
      <div v-else class="item-grid">
        <el-card v-for="item in state.records" :key="item.id" class="item-card" shadow="hover">
          <img v-if="firstImage(item)" :src="firstImage(item)" class="item-image" alt="物品图片" />
          <div v-else class="image-placeholder">暂无图片</div>

          <div class="card-body">
            <div class="tag-row">
              <el-tag :type="tagTypeForItemType(item.itemType)">{{ itemTypeText(item.itemType) }}</el-tag>
              <el-tag :type="tagTypeForItemStatus(item.status)" effect="plain">{{ itemStatusText(item.status) }}</el-tag>
            </div>
            <h3 class="card-title">{{ item.title }}</h3>
            <el-text class="muted" line-clamp="2">{{ item.description || '暂无描述' }}</el-text>
            <div class="meta-list">
              <div class="meta-line"><strong>地点</strong><span>{{ item.location || '-' }}</span></div>
              <div class="meta-line"><strong>分类</strong><span>{{ item.category || '-' }}</span></div>
            </div>
            <div style="flex: 1"></div>
            <el-button type="primary" plain @click="$router.push(`/items/${item.id}`)">查看详情</el-button>
          </div>
        </el-card>
      </div>

      <el-pagination
        v-if="state.total > state.size"
        style="margin-top: 18px; justify-content: flex-end"
        background
        layout="prev, pager, next"
        :current-page="state.page"
        :page-size="state.size"
        :total="state.total"
        @current-change="pageChange"
      />
    </section>
  </section>
</template>
