<script setup>
import { onMounted, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '../api/http'
import { parseUrlList } from '../utils/image'
import { itemStatusText, itemTypeText, tagTypeForItemStatus, tagTypeForItemType } from '../utils/status'

const state = reactive({ records: [], loading: false, operating: false })

async function load() {
  state.loading = true
  try {
    state.records = await http.get('/items/mine')
  } catch (err) {
    ElMessage.error(err.message)
  } finally {
    state.loading = false
  }
}

async function updateStatus(id, action) {
  const text = action === 'close' ? '关闭' : '重新开放'
  try {
    await ElMessageBox.confirm(`确认${text}该物品？`, '操作确认', {
      confirmButtonText: text,
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }

  state.operating = true
  try {
    await http.put(`/items/${id}/${action}`)
    ElMessage.success('操作成功')
    await load()
  } catch (err) {
    ElMessage.error(err.message)
  } finally {
    state.operating = false
  }
}

async function deleteItem(item) {
  try {
    await ElMessageBox.confirm(`确认删除“${item.title}”？删除后将不再显示在广场和我的发布中。`, '删除帖子', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }

  state.operating = true
  try {
    await http.delete(`/items/${item.id}`)
    ElMessage.success('帖子已删除')
    await load()
  } catch (err) {
    ElMessage.error(err.message)
  } finally {
    state.operating = false
  }
}

function firstImage(item) {
  return parseUrlList(item.imageUrls)[0] || ''
}

onMounted(load)
</script>

<template>
  <section>
    <div class="page-header">
      <div>
        <h1 class="page-title">我发布的物品</h1>
        <p class="page-subtitle">管理自己发布的寻物或招领信息，可关闭、重新开放或删除。</p>
      </div>
      <el-button type="primary" @click="$router.push('/items/create')">
        <el-icon><Plus /></el-icon>
        发布物品
      </el-button>
    </div>

    <section class="panel">
      <el-skeleton v-if="state.loading" :rows="6" animated />
      <el-empty v-else-if="!state.records.length" description="你还没有发布任何物品" />
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
            <div class="action-row">
              <el-button type="primary" plain @click="$router.push(`/items/${item.id}`)">详情</el-button>
              <el-button
                v-if="item.status === 'OPEN'"
                type="danger"
                plain
                :loading="state.operating"
                @click="updateStatus(item.id, 'close')"
              >
                关闭
              </el-button>
              <el-button
                v-if="item.status === 'CLOSED'"
                type="success"
                plain
                :loading="state.operating"
                @click="updateStatus(item.id, 'open')"
              >
                重新开放
              </el-button>
              <el-button
                type="danger"
                :loading="state.operating"
                @click="deleteItem(item)"
              >
                删除
              </el-button>
            </div>
          </div>
        </el-card>
      </div>
    </section>
  </section>
</template>
