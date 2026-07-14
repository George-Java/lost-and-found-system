import { createRouter, createWebHistory } from 'vue-router'
import ItemListView from '../views/ItemListView.vue'
import LoginView from '../views/LoginView.vue'
import RegisterView from '../views/RegisterView.vue'
import CreateItemView from '../views/CreateItemView.vue'
import MyClaimsView from '../views/MyClaimsView.vue'
import MyItemsView from '../views/MyItemsView.vue'
import ItemDetailView from '../views/ItemDetailView.vue'
import AdminDashboardView from '../views/AdminDashboardView.vue'
import AdminReviewView from '../views/AdminReviewView.vue'
import MessageView from '../views/MessageView.vue'
import { useAuthStore } from '../stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: ItemListView },
    { path: '/items/:id', component: ItemDetailView },
    { path: '/login', component: LoginView },
    { path: '/register', component: RegisterView },
    { path: '/items/create', component: CreateItemView, meta: { auth: true } },
    { path: '/items/mine', component: MyItemsView, meta: { auth: true, userOnly: true } },
    { path: '/claims/mine', component: MyClaimsView, meta: { auth: true, userOnly: true } },
    { path: '/messages', component: MessageView, meta: { auth: true } },
    { path: '/admin', component: AdminDashboardView, meta: { auth: true, admin: true } },
    { path: '/admin/reviews', component: AdminReviewView, meta: { auth: true, admin: true } }
  ]
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.meta.auth && !auth.token) return '/login'
  if (to.meta.admin && auth.user?.role !== 'ADMIN') return '/'
  if (to.meta.userOnly && auth.user?.role === 'ADMIN') return '/admin'
  if (to.path === '/items/create' && auth.user?.role === 'ADMIN') return '/admin/reviews'
  return true
})

export default router
