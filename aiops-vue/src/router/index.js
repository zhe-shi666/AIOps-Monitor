import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const routes = [
  { path: '/login', component: () => import('../views/LoginView.vue'), meta: { guest: true } },
  { path: '/register', component: () => import('../views/RegisterView.vue'), meta: { guest: true } },
  { path: '/', component: () => import('../views/DashboardView.vue'), meta: { requiresAuth: true } },
  { path: '/ai-expert', component: () => import('../views/AiExpertView.vue'), meta: { requiresAuth: true } },
  { path: '/workbench', component: () => import('../views/EventWorkbenchView.vue'), meta: { requiresAuth: true } },
  { path: '/targets', component: () => import('../views/TargetsView.vue'), meta: { requiresAuth: true } },
  { path: '/incidents', component: () => import('../views/IncidentsView.vue'), meta: { requiresAuth: true } },
  { path: '/settings/thresholds', component: () => import('../views/ThresholdSettingsView.vue'), meta: { requiresAuth: true } },
  { path: '/settings/escalation', component: () => import('../views/EscalationPolicyView.vue'), meta: { requiresAuth: true } },
  { path: '/settings/notifications', component: () => import('../views/NotificationChannelsView.vue'), meta: { requiresAuth: true } },
  {
    path: '/admin',
    component: () => import('../views/admin/AdminLayout.vue'),
    meta: { requiresAuth: true, requiresAdmin: true },
    children: [
      { path: '', redirect: '/admin/users' },
      { path: 'users', component: () => import('../views/admin/UserListView.vue') },
      { path: 'users/:id', component: () => import('../views/admin/UserDetailView.vue') },
      { path: 'stats', component: () => import('../views/admin/SystemStatsView.vue') }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, _from, next) => {
  const auth = useAuthStore()
  if (to.meta.requiresAuth && !auth.isLoggedIn) return next('/login')
  if (to.meta.requiresAdmin && !auth.isAdmin) return next('/')
  if (to.meta.guest && auth.isLoggedIn) return next('/')
  next()
})

export default router
