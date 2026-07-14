<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import http from '../api/http'
import { useAuthStore } from '../stores/auth'
import { joinUrlList } from '../utils/image'

const router = useRouter()
const auth = useAuthStore()
const formRef = ref()
const form = reactive({
  itemType: 'FOUND',
  title: '',
  description: '',
  category: '',
  location: '',
  lostTime: '',
  contact: '',
  imageUrls: []
})
const state = reactive({ uploading: false, submitting: false })

const rules = {
  itemType: [{ required: true, message: '请选择类型', trigger: 'change' }],
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }]
}

function formatDateTime(localDateTime) {
  if (!localDateTime) return null
  return `${localDateTime.replace('T', ' ')}:00`
}

async function submit() {
  if (auth.user?.role === 'ADMIN') {
    ElMessage.warning('管理员账号不能发布帖子')
    router.push('/admin/reviews')
    return
  }
  await formRef.value?.validate()
  state.submitting = true
  try {
    const created = await http.post('/items', {
      ...form,
      imageUrls: joinUrlList(form.imageUrls),
      lostTime: formatDateTime(form.lostTime)
    })
    ElMessage.success('发布成功，等待管理员审核')
    router.push(`/items/${created.id}`)
  } catch (err) {
    ElMessage.error(err.message)
  } finally {
    state.submitting = false
  }
}

async function uploadImage(option) {
  state.uploading = true
  try {
    const formData = new FormData()
    formData.append('file', option.file)
    const uploaded = await http.post('/files/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    form.imageUrls.push(uploaded.url)
    option.onSuccess(uploaded)
  } catch (err) {
    ElMessage.error(err.message)
    option.onError(err)
  } finally {
    state.uploading = false
  }
}

function removeImage(index) {
  form.imageUrls.splice(index, 1)
}
</script>

<template>
  <section class="panel" style="max-width: 900px; margin: 0 auto">
    <div class="page-header">
      <div>
        <h1 class="page-title">发布失物或招领信息</h1>
        <p class="page-subtitle">请尽量填写清晰的地点、时间、特征和联系方式，方便对方确认。</p>
      </div>
      <el-button @click="router.push('/')">返回广场</el-button>
    </div>

    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="submit">
      <div style="display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px">
        <el-form-item label="类型" prop="itemType">
          <el-segmented
            v-model="form.itemType"
            :options="[
              { label: '招领启事', value: 'FOUND' },
              { label: '寻物启事', value: 'LOST' }
            ]"
          />
        </el-form-item>
        <el-form-item label="分类">
          <el-input v-model="form.category" placeholder="校园卡、数码产品、书籍、钥匙等" />
        </el-form-item>
      </div>

      <el-form-item label="标题" prop="title">
        <el-input v-model="form.title" maxlength="100" show-word-limit placeholder="例如：图书馆门口捡到蓝色钱包" />
      </el-form-item>

      <el-form-item label="物品描述">
        <el-input
          v-model="form.description"
          type="textarea"
          :rows="5"
          maxlength="1000"
          show-word-limit
          placeholder="填写物品外观、品牌、颜色、丢失或拾取经过，以及可用于确认归属的细节。"
        />
      </el-form-item>

      <div style="display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px">
        <el-form-item label="地点">
          <el-input v-model="form.location" placeholder="例如：图书馆一楼服务台" />
        </el-form-item>
        <el-form-item label="时间">
          <el-date-picker
            v-model="form.lostTime"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm"
            placeholder="选择时间"
            style="width: 100%"
          />
        </el-form-item>
      </div>

      <el-form-item label="联系方式">
        <el-input v-model="form.contact" placeholder="手机号、微信号、宿舍号或其他可联系渠道" />
      </el-form-item>

      <el-form-item label="物品图片">
        <el-upload
          multiple
          accept="image/*"
          :show-file-list="false"
          :http-request="uploadImage"
        >
          <el-button :loading="state.uploading">
            <el-icon><Upload /></el-icon>
            上传图片
          </el-button>
        </el-upload>
        <el-text class="muted" style="margin-left: 10px">支持多张图片，单个文件不超过 10MB。</el-text>
      </el-form-item>

      <div v-if="form.imageUrls.length" class="image-grid" style="margin-bottom: 18px">
        <div v-for="(url, index) in form.imageUrls" :key="url">
          <el-image :src="url" fit="cover" :preview-src-list="form.imageUrls" />
          <el-button type="danger" plain size="small" style="margin-top: 8px; width: 100%" @click="removeImage(index)">
            移除
          </el-button>
        </div>
      </div>

      <el-button type="primary" :loading="state.submitting" @click="submit">提交发布</el-button>
    </el-form>
  </section>
</template>
