import { computed } from 'vue'
import { useAuthStore } from '../stores/auth'

export function usePermissions() {
  const auth = useAuthStore()

  const isAdmin = computed(() => auth.role === 'ADMIN')
  const canOperate = computed(() => ['ADMIN', 'OPS', 'USER'].includes(auth.role))
  const canConfigurePlatform = computed(() => ['ADMIN', 'OPS'].includes(auth.role))
  const canAudit = computed(() => ['ADMIN', 'OPS', 'AUDITOR'].includes(auth.role))
  const canManageUsers = computed(() => auth.role === 'ADMIN')
  const readOnlyReason = computed(() => auth.role === 'AUDITOR'
    ? '当前为审计员角色，仅允许查看与审计'
    : '当前角色为只读角色，不能执行配置或变更操作')

  return {
    isAdmin,
    canOperate,
    canConfigurePlatform,
    canAudit,
    canManageUsers,
    readOnlyReason
  }
}
