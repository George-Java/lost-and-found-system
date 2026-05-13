<script setup>
import { computed, onMounted, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import http from '../api/http'
import { useAuthStore } from '../stores/auth'
import { joinUrlList, parseUrlList } from '../utils/image'
import { claimStatusText, itemStatusText, itemTypeText } from '../utils/status'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const state = reactive({
  loading: false,
  submitting: false,
  uploading: false,
  reviewing: false,
  item: null,
  itemClaims: [],
  claimForm: {
    claimReason: '',
    proofDescription: '',
    proofMaterials: []
  }
})

const itemImages = computed(() => parseUrlList(state.item?.imageUrls))
const isAdmin = computed(() => auth.user?.role === 'ADMIN')
const canClaim = computed(() => {
  if (!state.item || !auth.token) return false
  if (isAdmin.value) return false
  if (state.item.status !== 'OPEN') return false
  return auth.user?.userId !== state.item.publisherId
})
const canViewClaims = computed(() => {
  return auth.user?.role === 'ADMIN' || auth.user?.userId === state.item?.publisherId
})
const canReviewClaims = computed(() => canViewClaims.value && state.item?.status === 'OPEN')

async function load() {
  state.loading = true
  try {
    state.item = await http.get(`/items/${route.params.id}`)
    if (auth.token && canViewClaims.value) {
      state.itemClaims = await http.get(`/items/${route.params.id}/claims`)
    } else {
      state.itemClaims = []
    }
  } catch (err) {
    alert(err.message)
    router.push('/')
  } finally {
    state.loading = false
  }
}

async function onUploadProofMaterials(event) {
  const files = Array.from(event.target.files || [])
  if (!files.length) return
  state.uploading = true
  try {
    for (const file of files) {
      const formData = new FormData()
      formData.append('file', file)
      const uploaded = await http.post('/files/upload', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      })
      state.claimForm.proofMaterials.push(uploaded.url)
    }
  } catch (err) {
    alert(err.message)
  } finally {
    state.uploading = false
    event.target.value = ''
  }
}

function removeProofMaterial(index) {
  state.claimForm.proofMaterials.splice(index, 1)
}

async function submitClaim() {
  if (!auth.token) {
    router.push('/login')
    return
  }
  if (!state.claimForm.claimReason.trim()) {
    alert('认领原因是必填的')
    return
  }
  state.submitting = true
  try {
    await http.post('/claims', {
      itemId: state.item.id,
      claimReason: state.claimForm.claimReason,
      proofDescription: state.claimForm.proofDescription,
      proofMaterials: joinUrlList(state.claimForm.proofMaterials)
    })
    state.claimForm.claimReason = ''
    state.claimForm.proofDescription = ''
    state.claimForm.proofMaterials = []
    alert('认领申请已提交')
    if (canViewClaims.value) {
      state.itemClaims = await http.get(`/items/${route.params.id}/claims`)
    }
  } catch (err) {
    alert(err.message)
  } finally {
    state.submitting = false
  }
}

async function reviewClaim(row, status) {
  const note = window.prompt(
    `请输入${status === 'APPROVED' ? '通过' : '驳回'}备注`,
    status === 'APPROVED' ? '已核验并同意认领' : '材料不足，请补充后重试'
  )
  if (note === null) return

  state.reviewing = true
  try {
    await http.put(`/claims/${row.id}/review`, {
      status,
      reviewNote: note
    })
    await load()
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

onMounted(load)
</script>

<template>
  <section class="card">
    <div v-if="state.loading" class="empty">加载中...</div>
    <div v-else-if="state.item" class="grid">
      <div class="section-title">
        <div>
          <div style="display:flex;gap:8px;flex-wrap:wrap;">
            <span class="badge">{{ itemTypeText(state.item.itemType) }}</span>
            <span class="badge badge-warm">{{ itemStatusText(state.item.status) }}</span>
          </div>
          <h2 style="margin:10px 0 0;">{{ state.item.title }}</h2>
        </div>
        <RouterLink class="btn btn-ghost" to="/">返回广场</RouterLink>
      </div>

      <div class="grid grid-2">
        <div class="grid">
          <p><strong>分类：</strong>{{ state.item.category || '-' }}</p>
          <p><strong>地点：</strong>{{ state.item.location || '-' }}</p>
          <p><strong>时间：</strong>{{ state.item.lostTime || '-' }}</p>
          <p><strong>联系方式：</strong>{{ state.item.contact || '-' }}</p>
          <p><strong>描述：</strong>{{ state.item.description || '暂无描述' }}</p>
          <div class="notice-card">
            <strong>认领提示</strong>
            <p>可上传图片、PDF、Word、TXT 作为证明材料。建议提供校园卡局部、购买记录、聊天截图或其他能证明归属的信息。</p>
          </div>
        </div>
        <div>
          <div v-if="itemImages.length" class="thumb-grid">
            <img v-for="url in itemImages" :key="url" :src="url" class="preview-img" alt="item image" />
          </div>
          <div v-else class="preview-img preview-placeholder">暂无图片</div>
        </div>
      </div>

      <section class="card">
        <h3 style="margin-top:0;">提交认领</h3>
        <p v-if="!auth.token">请先登录后再提交认领。</p>
        <p v-else-if="isAdmin">管理员账号仅用于发布、管理用户和审核认领，不参与认领申请。</p>
        <p v-else-if="!canClaim">你当前无法认领该物品。</p>
        <div v-else class="grid">
          <textarea
            v-model="state.claimForm.claimReason"
            class="textarea"
            rows="4"
            placeholder="请说明为什么该物品属于你，例如物品特征、丢失时间、内部物件等"
          ></textarea>
          <textarea
            v-model="state.claimForm.proofDescription"
            class="textarea"
            rows="3"
            placeholder="补充说明（可选），例如材料对应含义、可联系时间等"
          ></textarea>
          <div class="upload-panel">
            <input type="file" multiple @change="onUploadProofMaterials" />
            <span v-if="state.uploading" style="margin-left:8px;">上传中...</span>
            <p style="margin:8px 0 0;color:#667085;">支持图片、PDF、Word、TXT，单个文件不超过 10MB。</p>
          </div>
          <div v-if="state.claimForm.proofMaterials.length" class="material-list">
            <div v-for="(url, index) in state.claimForm.proofMaterials" :key="url" class="material-item">
              <a :href="url" target="_blank" rel="noreferrer">
                {{ isImage(url) ? `图片材料 ${index + 1}` : fileName(url) }}
              </a>
              <button class="btn btn-danger" @click="removeProofMaterial(index)">移除</button>
            </div>
          </div>
          <button class="btn btn-primary" :disabled="state.submitting" @click="submitClaim">
            {{ state.submitting ? '提交中...' : '提交认领申请' }}
          </button>
        </div>
      </section>

      <section v-if="canViewClaims" class="card">
        <div class="section-title">
          <div>
            <h3 style="margin:0;">该物品的认领记录</h3>
            <p style="margin:6px 0 0;color:#667085;">发布者和管理员可查看全部认领材料并直接审核。</p>
          </div>
        </div>
        <div v-if="!state.itemClaims.length" class="empty">暂无认领记录</div>
        <div v-else class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>认领ID</th>
                <th>认领人</th>
                <th>理由与说明</th>
                <th>状态</th>
                <th>审核备注</th>
                <th>证明材料</th>
                <th v-if="canReviewClaims">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in state.itemClaims" :key="row.id">
                <td>{{ row.id }}</td>
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
                <td v-if="canReviewClaims">
                  <div v-if="row.status === 'PENDING'" style="display:flex;gap:8px;flex-wrap:wrap;">
                    <button class="btn btn-primary" :disabled="state.reviewing" @click="reviewClaim(row, 'APPROVED')">通过</button>
                    <button class="btn btn-danger" :disabled="state.reviewing" @click="reviewClaim(row, 'REJECTED')">驳回</button>
                  </div>
                  <span v-else>-</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </div>
  </section>
</template>
