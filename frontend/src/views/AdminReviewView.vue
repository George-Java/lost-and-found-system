<script setup>
import { computed, onMounted, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '../api/http'
import { parseUrlList } from '../utils/image'
import { claimStatusText, tagTypeForClaimStatus } from '../utils/status'

const state = reactive({
  loading: false,
  claims: [],
  pendingItems: [],
  filterStatus: '',
  reviewing: false,
  reviewingItem: false
})

const pendingClaims = computed(() => state.claims.filter((row) => row.status === 'PENDING'))
const reviewedClaims = computed(() => state.claims.filter((row) => row.status !== 'PENDING'))

async function loadClaims() {
  state.loading = true
  try {
    state.claims = await http.get('/claims', {
      params: { status: state.filterStatus || undefined }
    })
  } catch (err) {
    ElMessage.error(err.message)
  } finally {
    state.loading = false
  }
}

async function loadPendingItems() {
  try {
    state.pendingItems = await http.get('/admin/items/pending')
  } catch (err) {
    ElMessage.error(err.message)
  }
}

async function loadAll() {
  state.loading = true
  try {
    await Promise.all([loadClaims(), loadPendingItems()])
  } finally {
    state.loading = false
  }
}

async function reviewItem(row, status) {
  const actionText = status === 'OPEN' ? '通过' : '驳回'
  try {
    await ElMessageBox.confirm(`确认${actionText}帖子“${row.title}”？`, '审核帖子', {
      confirmButtonText: actionText,
      cancelButtonText: '取消',
      type: status === 'OPEN' ? 'success' : 'warning'
    })
  } catch {
    return
  }

  state.reviewingItem = true
  try {
    await http.put(`/admin/items/${row.id}/review`, { status })
    ElMessage.success('帖子审核完成')
    await loadPendingItems()
  } catch (err) {
    ElMessage.error(err.message)
  } finally {
    state.reviewingItem = false
  }
}

async function deleteItem(row) {
  try {
    await ElMessageBox.confirm(`确认删除帖子“${row.title}”？`, '删除帖子', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }

  state.reviewingItem = true
  try {
    await http.delete(`/items/${row.id}`)
    ElMessage.success('帖子已删除')
    await loadPendingItems()
  } catch (err) {
    ElMessage.error(err.message)
  } finally {
    state.reviewingItem = false
  }
}

async function reviewClaim(row, status) {
  const actionText = status === 'APPROVED' ? '通过' : '驳回'
  let note = ''
  try {
    const result = await ElMessageBox.prompt(`请输入${actionText}备注`, `审核认领 #${row.id}`, {
      confirmButtonText: actionText,
      cancelButtonText: '取消',
      inputValue: status === 'APPROVED' ? '已核验身份与物品细节，允许认领' : '证明材料不足，请补充后重新提交'
    })
    note = result.value
  } catch {
    return
  }

  state.reviewing = true
  try {
    await http.put(`/claims/${row.id}/review`, { status, reviewNote: note })
    ElMessage.success('审核完成')
    await loadClaims()
  } catch (err) {
    ElMessage.error(err.message)
  } finally {
    state.reviewing = false
  }
}

function proofMaterials(row) {
  return parseUrlList(row.proofMaterials)
}

function isImage(url) {
  return /\.(png|jpe?g|gif|webp)$/i.test(url)
}

function fileName(url) {
  try {
    return decodeURIComponent(url.split('/').pop() || url)
  } catch {
    return url
  }
}

onMounted(loadAll)
</script>

<template>
  <section>
    <div class="page-header">
      <div>
        <h1 class="page-title">认领审核</h1>
        <p class="page-subtitle">管理员可审核普通用户提交的认领申请，并查看审核历史。</p>
      </div>
      <el-button @click="loadAll">
        <el-icon><Refresh /></el-icon>
        刷新
      </el-button>
    </div>

    <section class="stat-grid">
      <div class="stat-card">
        <p class="stat-label">待审核帖子</p>
        <p class="stat-value">{{ state.pendingItems.length }}</p>
      </div>
      <div class="stat-card">
        <p class="stat-label">待审核</p>
        <p class="stat-value">{{ pendingClaims.length }}</p>
      </div>
      <div class="stat-card">
        <p class="stat-label">审核历史</p>
        <p class="stat-value">{{ reviewedClaims.length }}</p>
      </div>
    </section>

    <section class="panel">
      <el-tabs>
        <el-tab-pane label="帖子审核">
          <el-table :data="state.pendingItems" empty-text="暂无待审核帖子" border>
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="title" label="标题" min-width="180" />
            <el-table-column prop="itemType" label="类型" width="110" />
            <el-table-column prop="category" label="分类" width="120" />
            <el-table-column prop="location" label="地点" min-width="150" />
            <el-table-column prop="description" label="描述" min-width="260" show-overflow-tooltip />
            <el-table-column label="操作" width="230" fixed="right">
              <template #default="{ row }">
                <div class="action-row">
                  <el-button size="small" type="success" :loading="state.reviewingItem" @click="reviewItem(row, 'OPEN')">
                    通过
                  </el-button>
                  <el-button size="small" type="danger" :disabled="state.reviewingItem" @click="reviewItem(row, 'REJECTED')">
                    驳回
                  </el-button>
                  <el-button size="small" type="danger" plain :disabled="state.reviewingItem" @click="deleteItem(row)">
                    删除
                  </el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="认领申请">
          <div class="page-header">
            <div>
              <h2 class="page-title" style="font-size: 22px">认领申请</h2>
              <p class="page-subtitle">按状态筛选后可直接处理待审核记录。</p>
            </div>
            <el-select v-model="state.filterStatus" clearable placeholder="全部状态" style="width: 180px" @change="loadClaims">
              <el-option label="待审核" value="PENDING" />
              <el-option label="已通过" value="APPROVED" />
              <el-option label="已驳回" value="REJECTED" />
            </el-select>
          </div>

          <el-table v-loading="state.loading" :data="state.claims" empty-text="暂无认领记录" border>
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column label="物品" min-width="170">
              <template #default="{ row }">
                <div>{{ row.itemTitle || `物品 #${row.itemId}` }}</div>
                <el-button link type="primary" @click="$router.push(`/items/${row.itemId}`)">查看详情</el-button>
              </template>
            </el-table-column>
            <el-table-column label="认领人" min-width="150">
              <template #default="{ row }">
                <div>{{ row.claimantName || `用户 #${row.claimantId}` }}</div>
                <el-text class="muted">{{ row.claimantPhone || '未填写联系方式' }}</el-text>
              </template>
            </el-table-column>
            <el-table-column label="认领理由" min-width="260">
              <template #default="{ row }">
                <div>{{ row.claimReason }}</div>
                <el-text class="muted">{{ row.proofDescription || '未填写补充说明' }}</el-text>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="110">
              <template #default="{ row }">
                <el-tag :type="tagTypeForClaimStatus(row.status)">{{ claimStatusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="reviewNote" label="审核备注" min-width="170" />
            <el-table-column label="证明材料" min-width="160">
              <template #default="{ row }">
                <div v-if="proofMaterials(row).length" class="material-list">
                  <a
                    v-for="url in proofMaterials(row)"
                    :key="url"
                    :href="url"
                    target="_blank"
                    rel="noreferrer"
                    class="material-link"
                  >
                    {{ isImage(url) ? '查看图片' : `下载 ${fileName(url)}` }}
                  </a>
                </div>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="170" fixed="right">
              <template #default="{ row }">
                <div v-if="row.status === 'PENDING'" class="action-row">
                  <el-button size="small" type="success" :loading="state.reviewing" @click="reviewClaim(row, 'APPROVED')">
                    通过
                  </el-button>
                  <el-button size="small" type="danger" :disabled="state.reviewing" @click="reviewClaim(row, 'REJECTED')">
                    驳回
                  </el-button>
                </div>
                <el-text v-else class="muted">已审核</el-text>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </section>
  </section>
</template>
