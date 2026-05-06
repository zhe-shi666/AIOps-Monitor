import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userId = ref(localStorage.getItem('userId') || '')
  const username = ref(localStorage.getItem('username') || '')
  const role = ref(localStorage.getItem('role') || '')
  const passwordChangeRequired = ref(localStorage.getItem('passwordChangeRequired') === 'true')
  const notificationEmail = ref(localStorage.getItem('notificationEmail') || '')
  const notificationEnabled = ref(localStorage.getItem('notificationEnabled') === 'true')

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => role.value === 'ADMIN')
  const isOperator = computed(() => ['ADMIN', 'OPS'].includes(role.value))
  const canOperateOwnResources = computed(() => ['ADMIN', 'OPS', 'USER'].includes(role.value))
  const canViewAudit = computed(() => ['ADMIN', 'OPS', 'AUDITOR'].includes(role.value))

  function setAuth(data) {
    token.value = data.token
    userId.value = data.userId ? String(data.userId) : ''
    username.value = data.username
    role.value = data.role
    passwordChangeRequired.value = Boolean(data.passwordChangeRequired)
    notificationEmail.value = data.notificationEmail || ''
    notificationEnabled.value = Boolean(data.notificationEnabled)
    localStorage.setItem('token', data.token)
    localStorage.setItem('userId', data.userId ? String(data.userId) : '')
    localStorage.setItem('username', data.username)
    localStorage.setItem('role', data.role)
    localStorage.setItem('passwordChangeRequired', String(Boolean(data.passwordChangeRequired)))
    localStorage.setItem('notificationEmail', data.notificationEmail || '')
    localStorage.setItem('notificationEnabled', String(Boolean(data.notificationEnabled)))
  }

  function setNotificationProfile(data) {
    notificationEmail.value = data.recipientEmail || ''
    notificationEnabled.value = Boolean(data.enabled)
    localStorage.setItem('notificationEmail', data.recipientEmail || '')
    localStorage.setItem('notificationEnabled', String(Boolean(data.enabled)))
  }

  function setPasswordChangeRequired(value) {
    passwordChangeRequired.value = Boolean(value)
    localStorage.setItem('passwordChangeRequired', String(Boolean(value)))
  }

  function logout() {
    token.value = ''
    userId.value = ''
    username.value = ''
    role.value = ''
    passwordChangeRequired.value = false
    notificationEmail.value = ''
    notificationEnabled.value = false
    localStorage.removeItem('token')
    localStorage.removeItem('userId')
    localStorage.removeItem('username')
    localStorage.removeItem('role')
    localStorage.removeItem('passwordChangeRequired')
    localStorage.removeItem('notificationEmail')
    localStorage.removeItem('notificationEnabled')
  }

  return {
    token,
    userId,
    username,
    role,
    passwordChangeRequired,
    notificationEmail,
    notificationEnabled,
    isLoggedIn,
    isAdmin,
    isOperator,
    canOperateOwnResources,
    canViewAudit,
    setAuth,
    setNotificationProfile,
    setPasswordChangeRequired,
    logout
  }
})
