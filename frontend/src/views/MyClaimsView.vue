<script setup>
import { onMounted, reactive } from 'vue'
import http from '../api/http'
import { claimStatusText } from '../utils/status'
import { parseUrlList } from '../utils/image'

const state = reactive({ records: [], loading: false })

async function load() {
  state.loading = true
  try {
    state.records = await http.get('/claims/mine')
  } catch (err) {
    alert(err.message)
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
  <section class="card">
    <div class="section-title">
      <h2>我的认领</h2>
    </div>
    <div v-if="state.loading" class="empty">加载中...</div>
    <div v-else-if="!state.records.length" class="empty">你还没有提交任何认领</div>
    <div v-else class="table-wrap">
      <table>
        <thead>
          <tr>
            <th>认领ID</th>
            <th>物品</th>
            <th>认领原因</th>
            <th>状态</th>
            <th>审核备注</th>
            <th>证明材料</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in state.records" :key="row.id">
            <td>{{ row.id }}</td>
            <td>
              <div>{{ row.itemTitle || `物品 #${row.itemId}` }}</div>
              <RouterLink :to="`/items/${row.itemId}`">查看详情</RouterLink>
            </td>
            <td>
              <div>{{ row.claimReason }}</div>
              <div style="color:#667085;margin-top:6px;">{{ row.proofDescription || '未填写补充说明' }}</div>
            </td>
            <td>{{ claimStatusText(row.status) }}</td>
            <td>{{ row.reviewNote || '-' }}</td>
            <td>
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
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>
