<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '../api/http'
import { createMessageSocketUrl } from '../api/ws'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const listRef = ref()
const state = reactive({
  loading: false,
  contacts: [],
  incomingRequests: [],
  outgoingRequests: [],
  searchKeyword: '',
  searchResults: [],
  searching: false,
  activeUserId: null,
  socket: null,
  connected: false,
  sending: false,
  messageText: '',
  messages: []
})

const activeContact = computed(() => state.contacts.find((item) => item.id === state.activeUserId))
const activeMessages = computed(() => {
  const currentUserId = auth.user?.userId
  return state.messages
    .filter((message) => {
      return (
        (message.fromUserId === currentUserId && message.toUserId === state.activeUserId)
        || (message.fromUserId === state.activeUserId && message.toUserId === currentUserId)
      )
    })
    .slice()
    .sort((a, b) => String(a.sentAt).localeCompare(String(b.sentAt)) || (a.id || 0) - (b.id || 0))
})

// 同一消息可能既来自历史接口又来自 WebSocket 推送，按 id 去重
function addMessage(message) {
  if (message.id && state.messages.some((item) => item.id === message.id)) {
    return
  }
  state.messages.push(message)
}

async function loadHistory(friendId) {
  if (!friendId) return
  const history = await http.get('/messages', { params: { friendId, limit: 200 } })
  history.forEach(addMessage)
}

async function loadContacts() {
  state.loading = true
  try {
    state.contacts = await http.get('/users/contacts')
    if (!state.activeUserId && state.contacts.length) {
      state.activeUserId = state.contacts[0].id
    }
    if (state.activeUserId && !state.contacts.some((item) => item.id === state.activeUserId)) {
      state.activeUserId = state.contacts[0]?.id || null
    }
  } finally {
    state.loading = false
  }
}

async function loadFriendRequests() {
  const [incoming, outgoing] = await Promise.all([
    http.get('/users/friend-requests/incoming'),
    http.get('/users/friend-requests/outgoing')
  ])
  state.incomingRequests = incoming
  state.outgoingRequests = outgoing
}

async function searchUsers() {
  const keyword = state.searchKeyword.trim()
  if (keyword.length < 2) {
    ElMessage.warning('请输入至少 2 个字符')
    return
  }
  state.searching = true
  try {
    state.searchResults = await http.get('/users/search', { params: { keyword } })
  } finally {
    state.searching = false
  }
}

async function sendFriendRequest(user) {
  await http.post('/users/friend-requests', { targetUserId: user.id })
  ElMessage.success('好友申请已发送')
  await Promise.all([searchUsers(), loadFriendRequests()])
}

async function reviewFriendRequest(request, status) {
  await http.put(`/users/friend-requests/${request.id}/review`, { status })
  ElMessage.success(status === 'APPROVED' ? '已同意好友申请' : '已拒绝好友申请')
  await Promise.all([loadContacts(), loadFriendRequests()])
}

async function deleteFriend(contact) {
  try {
    await ElMessageBox.confirm(`确认删除好友“${contact.realName || contact.username}”？`, '删除好友', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }

  await http.delete(`/users/friends/${contact.id}`)
  ElMessage.success('好友已删除')
  state.messages = state.messages.filter((message) => message.fromUserId !== contact.id && message.toUserId !== contact.id)
  await loadContacts()
  if (state.searchKeyword.trim().length >= 2) {
    await searchUsers()
  }
}

let reconnectTimer = null
let unmounted = false

function connect() {
  if (!auth.token || state.socket || unmounted) return
  const socket = new WebSocket(createMessageSocketUrl(auth.token))
  state.socket = socket

  socket.onopen = () => {
    state.connected = true
  }
  socket.onclose = () => {
    state.connected = false
    state.socket = null
    // 断线自动重连：重连成功即开始接收离线期间积压在消息队列里的私信
    if (unmounted || reconnectTimer) return
    reconnectTimer = setTimeout(() => {
      reconnectTimer = null
      connect()
    }, 5000)
  }
  socket.onerror = () => {
    state.connected = false
  }
  socket.onmessage = (event) => {
    try {
      const payload = JSON.parse(event.data)
      if (payload.type === 'CHAT') {
        addMessage(payload)
      }
    } catch {
      ElMessage.warning('收到无法解析的消息')
    }
  }
}

function disconnect() {
  if (reconnectTimer) {
    clearTimeout(reconnectTimer)
    reconnectTimer = null
  }
  if (state.socket) {
    state.socket.close()
  }
}

async function sendMessage() {
  const content = state.messageText.trim()
  if (!content) {
    ElMessage.warning('请输入消息内容')
    return
  }
  if (!state.activeUserId) {
    ElMessage.warning('请选择好友')
    return
  }

  state.sending = true
  try {
    // 发送走 HTTP：后端先落库再推送，对方不在线时消息也不会丢
    const message = await http.post('/messages', { toUserId: state.activeUserId, content })
    addMessage(message)
    state.messageText = ''
  } finally {
    state.sending = false
  }
}

function senderName(message) {
  if (message.fromUserId === auth.user?.userId) {
    return '我'
  }
  return message.fromUsername || activeContact.value?.realName || `用户 #${message.fromUserId}`
}

function friendshipLabel(status) {
  const map = {
    FRIEND: '已是好友',
    REQUEST_SENT: '已发送申请',
    REQUEST_RECEIVED: '对方已申请你',
    NONE: '可申请'
  }
  return map[status] || status
}

function formatTime(sentAt) {
  return String(sentAt || '').replace('T', ' ').slice(0, 16)
}

watch(() => state.activeUserId, (id) => {
  if (id) loadHistory(id)
})

watch(activeMessages, async () => {
  await nextTick()
  if (listRef.value) {
    listRef.value.scrollTop = listRef.value.scrollHeight
  }
})

onMounted(() => {
  loadContacts()
  loadFriendRequests()
  connect()
})

onBeforeUnmount(() => {
  unmounted = true
  disconnect()
})
</script>

<template>
  <section>
    <div class="page-header">
      <div>
        <h1 class="page-title">用户消息</h1>
        <p class="page-subtitle">先搜索用户并发送好友申请，对方同意后才能收发消息。私信会保存在服务端：对方在线时实时送达，不在线则登录后即可看到全部历史消息。</p>
      </div>
      <el-tag :type="state.connected ? 'success' : 'warning'">
        {{ state.connected ? '实时接收中' : '实时推送断开' }}
      </el-tag>
    </div>

    <section class="message-layout">
      <el-card shadow="never">
        <template #header>
          <div class="action-row">
            <span>好友与申请</span>
            <el-button text @click="() => { loadContacts(); loadFriendRequests() }">
              <el-icon><Refresh /></el-icon>
            </el-button>
          </div>
        </template>

        <el-tabs>
          <el-tab-pane label="好友">
            <el-skeleton v-if="state.loading" :rows="4" animated />
            <el-empty v-else-if="!state.contacts.length" description="暂无好友" />
            <el-menu v-else :default-active="String(state.activeUserId)" @select="(id) => (state.activeUserId = Number(id))">
              <el-menu-item v-for="contact in state.contacts" :key="contact.id" :index="String(contact.id)">
                <el-icon><User /></el-icon>
                <span style="flex: 1">{{ contact.realName || contact.username }}（{{ contact.role }}）</span>
                <el-button
                  size="small"
                  type="danger"
                  text
                  @click.stop="deleteFriend(contact)"
                >
                  删除
                </el-button>
              </el-menu-item>
            </el-menu>
          </el-tab-pane>

          <el-tab-pane label="搜索">
            <div class="toolbar">
              <el-input
                v-model="state.searchKeyword"
                placeholder="搜索用户名或姓名，例如 admin"
                clearable
                @keyup.enter="searchUsers"
              />
              <el-button type="primary" :loading="state.searching" @click="searchUsers">搜索</el-button>
            </div>
            <div style="margin-top: 12px">
              <el-empty v-if="!state.searchResults.length" description="暂无搜索结果" />
              <div v-else class="material-list">
                <div v-for="user in state.searchResults" :key="user.id" class="material-item">
                  <div>
                    <strong>{{ user.realName || user.username }}</strong>
                    <el-text class="muted"> @{{ user.username }} · {{ user.role }} · {{ friendshipLabel(user.friendshipStatus) }}</el-text>
                  </div>
                  <el-button
                    size="small"
                    type="primary"
                    :disabled="user.friendshipStatus !== 'NONE'"
                    @click="sendFriendRequest(user)"
                  >
                    添加好友
                  </el-button>
                </div>
              </div>
            </div>
          </el-tab-pane>

          <el-tab-pane label="申请">
            <h3 class="card-title">收到的申请</h3>
            <el-empty v-if="!state.incomingRequests.length" description="暂无收到的好友申请" />
            <div v-else class="material-list" style="margin-bottom: 14px">
              <div v-for="request in state.incomingRequests" :key="request.id" class="material-item">
                <div>
                  <strong>{{ request.requester?.realName || request.requester?.username }}</strong>
                  <el-text class="muted"> · {{ request.status }}</el-text>
                </div>
                <div v-if="request.status === 'PENDING'" class="action-row">
                  <el-button size="small" type="success" @click="reviewFriendRequest(request, 'APPROVED')">同意</el-button>
                  <el-button size="small" type="danger" plain @click="reviewFriendRequest(request, 'REJECTED')">拒绝</el-button>
                </div>
              </div>
            </div>

            <h3 class="card-title">发出的申请</h3>
            <el-empty v-if="!state.outgoingRequests.length" description="暂无发出的好友申请" />
            <div v-else class="material-list">
              <div v-for="request in state.outgoingRequests" :key="request.id" class="material-item">
                <div>
                  <strong>{{ request.receiver?.realName || request.receiver?.username }}</strong>
                  <el-text class="muted"> · {{ request.status }}</el-text>
                </div>
              </div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </el-card>

      <el-card shadow="never">
        <template #header>
          <div class="page-header" style="margin-bottom: 0">
            <div>
              <h2 class="page-title" style="font-size: 20px">
                {{ activeContact ? `${activeContact.realName || activeContact.username}` : '请选择好友' }}
              </h2>
              <p class="page-subtitle">当前登录：{{ auth.user?.realName || auth.user?.username }}</p>
            </div>
          </div>
        </template>

        <div ref="listRef" class="message-list">
          <el-empty v-if="!activeMessages.length" description="暂无消息记录" />
          <div
            v-for="(message, index) in activeMessages"
            :key="`${message.sentAt}-${index}`"
            class="message-bubble"
            :class="{ mine: message.fromUserId === auth.user?.userId }"
          >
            <div class="message-meta">{{ senderName(message) }} · {{ formatTime(message.sentAt) }}</div>
            <div>{{ message.content }}</div>
          </div>
        </div>

        <div class="toolbar" style="margin-top: 14px">
          <el-input
            v-model="state.messageText"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            placeholder="输入消息内容"
            @keydown.ctrl.enter.prevent="sendMessage"
          />
          <el-button type="primary" :disabled="!state.activeUserId" :loading="state.sending" @click="sendMessage">
            <el-icon><Promotion /></el-icon>
            发送
          </el-button>
        </div>
      </el-card>
    </section>
  </section>
</template>
