<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '../api/http'
import { useAuthStore } from '../stores/auth'
import { joinUrlList, parseUrlList } from '../utils/image'
import {
  claimStatusText,
  itemStatusText,
  itemTypeText,
  tagTypeForClaimStatus,
  tagTypeForItemStatus,
  tagTypeForItemType
} from '../utils/status'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const claimFormRef = ref()

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

const rules = {
  claimReason: [{ required: true, message: '请填写认领原因', trigger: 'blur' }]
}

const itemImages = computed(() => parseUrlList(state.item?.imageUrls))
const isAdmin = computed(() => auth.user?.role === 'ADMIN')
const canClaim = computed(() => {
  if (!state.item || !auth.token) return false
  if (isAdmin.value) return false
  if (state.item.status !== 'OPEN') return false
  return auth.user?.userId !== state.item.publisherId
})
const canViewClaims = computed(() => auth.user?.role === 'ADMIN')
const canReviewClaims = computed(() => canViewClaims.value && state.item?.status === 'OPEN')
const canDeleteItem = computed(() => {
  if (!state.item || !auth.token) return false
  return isAdmin.value || auth.user?.userId === state.item.publisherId
})

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
    ElMessage.error(err.message)
    router.push('/')
  } finally {
    state.loading = false
  }
}

async function uploadProofMaterial(option) {
  state.uploading = true
  try {
    const formData = new FormData()
    formData.append('file', option.file)
    const uploaded = await http.post('/files/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    state.claimForm.proofMaterials.push(uploaded.url)
    option.onSuccess(uploaded)
  } catch (err) {
    ElMessage.error(err.message)
    option.onError(err)
  } finally {
    state.uploading = false
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
  await claimFormRef.value?.validate()
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
    ElMessage.success('认领申请已提交')
    if (canViewClaims.value) {
      state.itemClaims = await http.get(`/items/${route.params.id}/claims`)
    }
  } catch (err) {
    ElMessage.error(err.message)
  } finally {
    state.submitting = false
  }
}

async function reviewClaim(row, status) {
  const actionText = status === 'APPROVED' ? '通过' : '驳回'
  let note = ''
  try {
    const result = await ElMessageBox.prompt(`请输入${actionText}备注`, `审核认领 #${row.id}`, {
      confirmButtonText: actionText,
      cancelButtonText: '取消',
      inputValue: status === 'APPROVED' ? '已核验并同意认领' : '材料不足，请补充后重试'
    })
    note = result.value
  } catch {
    return
  }

  state.reviewing = true
  try {
    await http.put(`/claims/${row.id}/review`, { status, reviewNote: note })
    ElMessage.success('审核完成')
    if (status === 'APPROVED') {
      router.push('/')
      return
    }
    await load()
  } catch (err) {
    ElMessage.error(err.message)
  } finally {
    state.reviewing = false
  }
}

async function deleteItem() {
  try {
    await ElMessageBox.confirm(`确认删除“${state.item.title}”？`, '删除帖子', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }

  try {
    await http.delete(`/items/${state.item.id}`)
    ElMessage.success('帖子已删除')
    router.push(isAdmin.value ? '/admin/reviews' : '/items/mine')
  } catch (err) {
    ElMessage.error(err.message)
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
  <section class="panel">
    <el-skeleton v-if="state.loading" :rows="8" animated />

    <template v-else-if="state.item">
      <div class="page-header">
        <div>
          <div class="tag-row">
            <el-tag :type="tagTypeForItemType(state.item.itemType)">{{ itemTypeText(state.item.itemType) }}</el-tag>
            <el-tag :type="tagTypeForItemStatus(state.item.status)" effect="plain">{{ itemStatusText(state.item.status) }}</el-tag>
          </div>
          <h1 class="page-title" style="margin-top: 10px">{{ state.item.title }}</h1>
          <p class="page-subtitle">发布于 {{ state.item.createdAt || '-' }}</p>
        </div>
        <div class="action-row">
          <el-button v-if="canDeleteItem" type="danger" plain @click="deleteItem">删除帖子</el-button>
          <el-button @click="router.push('/')">返回广场</el-button>
        </div>
      </div>

      <div class="detail-grid">
        <div>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="分类">{{ state.item.category || '-' }}</el-descriptions-item>
            <el-descriptions-item label="地点">{{ state.item.location || '-' }}</el-descriptions-item>
            <el-descriptions-item label="时间">{{ state.item.lostTime || '-' }}</el-descriptions-item>
            <el-descriptions-item label="联系方式">{{ state.item.contact || '-' }}</el-descriptions-item>
            <el-descriptions-item label="描述">{{ state.item.description || '暂无描述' }}</el-descriptions-item>
          </el-descriptions>

          <el-alert
            style="margin-top: 16px"
            title="认领提示"
            type="info"
            :closable="false"
            description="请提供能证明归属的信息，例如物品特征、购买记录、校园卡局部、聊天截图或其他材料。"
          />
        </div>

        <div>
          <div v-if="itemImages.length" class="image-grid">
            <el-image
              v-for="url in itemImages"
              :key="url"
              :src="url"
              fit="cover"
              :preview-src-list="itemImages"
            />
          </div>
          <div v-else class="image-placeholder" style="height: 240px">暂无图片</div>
        </div>
      </div>

      <section class="panel">
        <div class="page-header">
          <div>
            <h2 class="page-title" style="font-size: 22px">提交认领</h2>
            <p class="page-subtitle">普通用户可提交证明材料，管理员负责审核认领申请。</p>
          </div>
        </div>

        <el-empty v-if="!auth.token" description="请先登录后再提交认领">
          <el-button type="primary" @click="router.push('/login')">去登录</el-button>
        </el-empty>
        <el-alert v-else-if="isAdmin" title="管理员账号不参与认领申请" type="warning" :closable="false" />
        <el-alert v-else-if="!canClaim" title="你当前无法认领该物品" type="info" :closable="false" />

        <el-form v-else ref="claimFormRef" :model="state.claimForm" :rules="rules" label-position="top">
          <el-form-item label="认领原因" prop="claimReason">
            <el-input
              v-model="state.claimForm.claimReason"
              type="textarea"
              :rows="4"
              maxlength="1000"
              show-word-limit
              placeholder="说明为什么该物品属于你。"
            />
          </el-form-item>
          <el-form-item label="补充说明">
            <el-input
              v-model="state.claimForm.proofDescription"
              type="textarea"
              :rows="3"
              maxlength="1000"
              show-word-limit
              placeholder="例如材料对应含义、可联系时间等。"
            />
          </el-form-item>
          <el-form-item label="证明材料">
            <el-upload multiple :show-file-list="false" :http-request="uploadProofMaterial">
              <el-button :loading="state.uploading">
                <el-icon><Upload /></el-icon>
                上传材料
              </el-button>
            </el-upload>
            <el-text class="muted" style="margin-left: 10px">支持图片、PDF、Word、TXT，单个文件不超过 10MB。</el-text>
          </el-form-item>
          <div v-if="state.claimForm.proofMaterials.length" class="material-list" style="margin-bottom: 16px">
            <div v-for="(url, index) in state.claimForm.proofMaterials" :key="url" class="material-item">
              <a :href="url" target="_blank" rel="noreferrer" class="material-link">
                {{ isImage(url) ? `图片材料 ${index + 1}` : fileName(url) }}
              </a>
              <el-button type="danger" text @click="removeProofMaterial(index)">移除</el-button>
            </div>
          </div>
          <el-button type="primary" :loading="state.submitting" @click="submitClaim">提交认领申请</el-button>
        </el-form>
      </section>

      <section v-if="canViewClaims" class="panel">
        <div class="page-header">
          <div>
            <h2 class="page-title" style="font-size: 22px">认领记录</h2>
            <p class="page-subtitle">管理员可查看材料并审核。</p>
          </div>
        </div>
        <el-table :data="state.itemClaims" empty-text="暂无认领记录" border>
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column label="认领人" min-width="150">
            <template #default="{ row }">
              <div>{{ row.claimantName || `用户 #${row.claimantId}` }}</div>
              <el-text class="muted">{{ row.claimantPhone || '未填写联系方式' }}</el-text>
            </template>
          </el-table-column>
          <el-table-column label="理由与说明" min-width="240">
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
          <el-table-column label="材料" min-width="150">
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
          <el-table-column v-if="canReviewClaims" label="操作" width="170" fixed="right">
            <template #default="{ row }">
              <div v-if="row.status === 'PENDING'" class="action-row">
                <el-button size="small" type="success" :loading="state.reviewing" @click="reviewClaim(row, 'APPROVED')">
                  通过
                </el-button>
                <el-button size="small" type="danger" :disabled="state.reviewing" @click="reviewClaim(row, 'REJECTED')">
                  驳回
                </el-button>
              </div>
              <span v-else>-</span>
            </template>
          </el-table-column>
        </el-table>
      </section>
    </template>
  </section>
</template>
