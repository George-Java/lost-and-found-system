<script setup>
import { reactive } from 'vue'
import { useRouter } from 'vue-router'
import http from '../api/http'
import { useAuthStore } from '../stores/auth'
import { joinUrlList } from '../utils/image'

const router = useRouter()
const auth = useAuthStore()
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

function formatDateTime(localDateTime) {
  if (!localDateTime) return null
  return `${localDateTime.replace('T', ' ')}:00`
}

async function submit() {
  if (!form.title.trim()) {
    alert('标题是必填的')
    return
  }
  state.submitting = true
  try {
    const created = await http.post('/items', {
      ...form,
      imageUrls: joinUrlList(form.imageUrls),
      lostTime: formatDateTime(form.lostTime)
    })
    router.push(`/items/${created.id}`)
  } catch (err) {
    alert(err.message)
  } finally {
    state.submitting = false
  }
}

async function onUploadImages(event) {
  if (!auth.token) {
    alert('请先登录后再上传图片')
    router.push('/login')
    event.target.value = ''
    return
  }

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
      form.imageUrls.push(uploaded.url)
    }
  } catch (err) {
    alert(err.message)
  } finally {
    state.uploading = false
    event.target.value = ''
  }
}

function removeImage(index) {
  form.imageUrls.splice(index, 1)
}
</script>

<template>
  <section class="card" style="max-width:860px;margin:0 auto;">
    <div class="section-title">
      <div>
        <span class="badge">发布信息</span>
        <h2 style="margin:8px 0 0;">发布失物或招领信息</h2>
      </div>
      <RouterLink to="/" class="btn btn-ghost">返回广场</RouterLink>
    </div>

    <div class="grid">
      <div class="grid grid-2">
        <label>
          类型
          <select v-model="form.itemType" class="select">
            <option value="FOUND">招领启事</option>
            <option value="LOST">寻物启事</option>
          </select>
        </label>
        <label>
          分类
          <input v-model="form.category" class="input" placeholder="如：校园卡、数码、书籍、钥匙" />
        </label>
      </div>
      <input v-model="form.title" class="input" placeholder="标题" />
      <textarea v-model="form.description" class="textarea" rows="5" placeholder="请填写物品特征、丢失/拾取经过、识别细节"></textarea>
      <div class="grid grid-2">
        <input v-model="form.location" class="input" placeholder="地点" />
        <input v-model="form.lostTime" class="input" type="datetime-local" />
      </div>
      <input v-model="form.contact" class="input" placeholder="联系方式，如手机号、微信号、宿舍号" />

      <div class="card">
        <strong>上传物品图片</strong>
        <p style="color:#667085;margin:6px 0 12px;">支持多张图片，建议上传能体现物品外观特征的照片。</p>
        <input type="file" accept="image/*" multiple @change="onUploadImages" :disabled="!auth.token" />
        <span v-if="state.uploading" style="margin-left:8px;">上传中...</span>
        <div v-if="!auth.token" style="color:#bd4b3f;margin-top:8px;">请先登录后再上传图片。</div>
        <div v-if="form.imageUrls.length" class="thumb-grid" style="margin-top:12px;">
          <div v-for="(url, index) in form.imageUrls" :key="url" class="grid">
            <img :src="url" class="preview-img" alt="item image" />
            <button class="btn btn-danger" @click="removeImage(index)">移除</button>
          </div>
        </div>
      </div>

      <button class="btn btn-primary" :disabled="state.submitting" @click="submit">
        {{ state.submitting ? '提交中...' : '提交发布' }}
      </button>
    </div>
  </section>
</template>
