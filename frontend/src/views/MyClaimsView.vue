<script setup>
import { onMounted, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import http from '../api/http'
import { parseUrlList } from '../utils/image'
import { claimStatusText, tagTypeForClaimStatus } from '../utils/status'

const state = reactive({ records: [], loading: false })

async function load() {
  state.loading = true
  try {
    state.records = await http.get('/claims/mine')
  } catch (err) {
    ElMessage.error(err.message)
  } finally {
    state.loading = false
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

onMounted(load)
</script>

<template>
  <section>
    <div class="page-header">
      <div>
        <h1 class="page-title">我的认领</h1>
        <p class="page-subtitle">查看自己提交的认领申请、审核结果和证明材料。</p>
      </div>
      <el-button @click="load">
        <el-icon><Refresh /></el-icon>
        刷新
      </el-button>
    </div>

    <section class="panel">
      <el-table v-loading="state.loading" :data="state.records" empty-text="你还没有提交任何认领" border>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="物品" min-width="170">
          <template #default="{ row }">
            <div>{{ row.itemTitle || `物品 #${row.itemId}` }}</div>
            <el-button link type="primary" @click="$router.push(`/items/${row.itemId}`)">查看详情</el-button>
          </template>
        </el-table-column>
        <el-table-column label="认领原因" min-width="260">
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
        <el-table-column prop="reviewNote" label="审核备注" min-width="160" />
        <el-table-column label="证明材料" min-width="170">
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
                {{ isImage(url) ? '查看图片材料' : `下载 ${fileName(url)}` }}
              </a>
            </div>
            <span v-else>-</span>
          </template>
        </el-table-column>
      </el-table>
    </section>
  </section>
</template>
