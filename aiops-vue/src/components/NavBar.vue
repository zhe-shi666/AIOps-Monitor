<template>
  <div class="fixed top-0 left-0 right-0 z-50 flex items-center justify-between px-6 py-3
              bg-slate-900/90 backdrop-blur-sm border-b border-slate-700">
    <span class="text-slate-100 font-semibold text-sm">🛡️ AIOps Monitor</span>
    <div class="flex items-center gap-4">
      <router-link v-if="auth.isAdmin" to="/admin"
        class="text-xs text-purple-400 hover:text-purple-300 font-medium">
        管理后台
      </router-link>
      <span class="text-xs text-slate-400">{{ auth.username }}</span>
      <span class="text-xs px-2 py-0.5 rounded border"
        :class="auth.isAdmin ? 'text-purple-400 border-purple-700' : 'text-cyan-400 border-cyan-700'">
        {{ auth.isAdmin ? 'ADMIN' : 'USER' }}
      </span>
      <button @click="handleLogout"
        class="text-xs text-slate-400 hover:text-red-400 transition-colors cursor-pointer">
        退出
      </button>
    </div>
  </div>
</template>

<script setup>
import { useAuthStore } from '../stores/auth'
import { useRouter } from 'vue-router'

const auth = useAuthStore()
const router = useRouter()

function handleLogout() {
  auth.logout()
  router.push('/login')
}
</script>
