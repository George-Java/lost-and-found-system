<script setup>
import { computed, onMounted, reactive } from 'vue'
import http from '../api/http'
import { claimStatusText } from '../utils/status'
import { parseUrlList } from '../utils/image'

const state = reactive({
  loading: false,
  claims: [],
  filterStatus: '',
  reviewing: false
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
    alert(err.message)
  } finally {
    state.loading = false
  }
}

async function reviewClaim(row, status) {
  const note = window.prompt(
    `请输入${status === 'APPROVED' ? '通过' : '驳回'}备注`,
    status === 'APPROVED' ? '已核验身份与物品细节，允许认领' : '证明材料不足，请补充后重新提交'
  )
  if (note === null) return
  state.reviewing = true
  try {
    await http.put(`/claims/${row.id}/review`, {
      status,
      reviewNote: note
    })
    await loadClaims()
  } catch (err) {
    alert(err.message)
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

onMounted(loadClaims)
</script>

<template>
  <section class="grid">
    <div class="section-title">
      <div>
        <h2>管理员审核模块</h2>
        <p style="margin:6px 0 0;color:#667085;">管理员审核普通用户提交的认领申请，并可查看审核历史。</p>
      </div>
      <button class="btn btn-ghost" @click="loadClaims">刷新</button>
    </div>

    <section class="grid grid-2">
      <article class="card">
        <h3 style="margin-top:0;">待审核</h3>
        <p style="font-size:30px;font-weight:800;margin:8px 0 0;">{{ pendingClaims.length }}</p>
      </article>
      <article class="card">
        <h3 style="margin-top:0;">审核历史</h3>
        <p style="font-size:30px;font-weight:800;margin:8px 0 0;">{{ reviewedClaims.length }}</p>
      </article>
    </section>

    <section class="card">
      <div class="section-title">
        <h3 style="margin:0;">认领申请与审核历史</h3>
        <select v-model="state.filterStatus" class="select" style="max-width:220px;" @change="loadClaims">
          <option value="">全部</option>
          <option value="PENDING">仅待审核</option>
          <option value="APPROVED">仅已通过</option>
          <option value="REJECTED">仅已驳回</option>
        </select>
      </div>

      <div v-if="state.loading" class="empty">加载中...</div>
      <div v-else-if="!state.claims.length" class="empty">暂无认领记录</div>
      <div v-else class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>认领ID</th>
              <th>物品</th>
              <th>认领人</th>
              <th>认领理由</th>
              <th>状态</th>
              <th>审核备注</th>
              <th>证明材料</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in state.claims" :key="row.id">
              <td>{{ row.id }}</td>
              <td>
                <div>{{ row.itemTitle || `物品 #${row.itemId}` }}</div>
                <RouterLink :to="`/items/${row.itemId}`">查看详情</RouterLink>
              </td>
              <td>
                <div>{{ row.claimantName || `用户 #${row.claimantId}` }}</div>
                <div style="color:#667085;">{{ row.claimantPhone || '未填写手机号' }}</div>
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
                    {{ isImage(url) ? '查看图片' : `下载 ${fileName(url)}` }}
                  </a>
                </div>
                <span v-else>-</span>
              </td>
              <td>
                <div v-if="row.status === 'PENDING'" style="display:flex;gap:8px;flex-wrap:wrap;">
                  <button class="btn btn-primary" :disabled="state.reviewing" @click="reviewClaim(row, 'APPROVED')">通过</button>
                  <button class="btn btn-danger" :disabled="state.reviewing" @click="reviewClaim(row, 'REJECTED')">驳回</button>
                </div>
                <span v-else>已审核</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </section>
</template>
