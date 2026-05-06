<template>
  <div class="space-y-5">
    <div class="section-head">
      <div>
        <h2 class="section-title">{{ t('title') }}</h2>
        <p class="section-subtitle">{{ t('subtitle') }}</p>
      </div>
      <el-button @click="$router.back()" plain>{{ t('back') }}</el-button>
    </div>

    <div v-if="user" class="space-y-6">
      <div class="card-panel p-6">
        <h2 class="text-lg font-bold text-slate-100 mb-4">{{ t('profileTitle') }}</h2>
        <div class="mb-4 flex flex-wrap gap-3">
          <el-button type="warning" @click="handleTemporaryPasswordReset">{{ t('temporaryPassword') }}</el-button>
        </div>
        <div v-if="temporaryPassword" class="auth-result-box">
          <div class="auth-result-title">{{ t('temporaryPasswordIssued') }}</div>
          <p>{{ t('temporaryPasswordHint') }}</p>
          <div class="auth-token-row">
            <code>{{ temporaryPassword }}</code>
            <el-button size="small" @click="copyTemporaryPassword">{{ t('copy') }}</el-button>
          </div>
        </div>
        <div class="grid grid-cols-2 gap-4 text-sm">
          <div><span class="text-slate-400">{{ t('username') }}：</span><span class="text-slate-100">{{ user.username }}</span></div>
          <div><span class="text-slate-400">{{ t('email') }}：</span><span class="text-slate-100">{{ user.email }}</span></div>
          <div><span class="text-slate-400">{{ t('notificationEmail') }}：</span><span class="text-slate-100">{{ user.notificationEmail || '-' }}</span></div>
          <div>
            <span class="text-slate-400">{{ t('notificationStatus') }}：</span>
            <el-tag :type="user.notificationEnabled ? 'success' : 'info'" size="small">
              {{ user.notificationEnabled ? t('enabled') : t('disabled') }}
            </el-tag>
          </div>
          <div>
            <span class="text-slate-400">{{ t('role') }}：</span>
            <el-select v-model="user.role" size="small" @change="handleRoleChange" style="width: 120px;">
              <el-option label="USER" value="USER" />
              <el-option label="AUDITOR" value="AUDITOR" />
              <el-option label="OPS" value="OPS" />
              <el-option label="ADMIN" value="ADMIN" />
            </el-select>
          </div>
          <div>
            <span class="text-slate-400">{{ t('passwordStatus') }}：</span>
            <el-tag :type="user.passwordChangeRequired ? 'warning' : 'success'" size="small">
              {{ user.passwordChangeRequired ? t('mustChange') : t('normalPassword') }}
            </el-tag>
          </div>
          <div><span class="text-slate-400">{{ t('createdAt') }}：</span><span class="text-slate-100">{{ formatDate(user.createdAt) }}</span></div>
        </div>
      </div>

      <div class="card-panel p-6">
        <h3 class="text-base font-semibold text-slate-100 mb-4">{{ t('targetsTitle') }}（{{ targets.length }}）</h3>
        <div class="ops-table">
          <el-table :data="targets" size="small">
            <el-table-column prop="name" :label="t('name')" />
            <el-table-column prop="hostname" :label="t('hostname')" />
            <el-table-column prop="description" :label="t('description')" />
            <el-table-column prop="createdAt" :label="t('addedAt')">
              <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </div>

    <div v-else-if="loading" class="text-slate-400 text-center py-20">{{ t('loading') }}</div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getUserDetail, issueTemporaryPassword, updateUserRole } from '../../api/admin'
import { useI18n } from '../../composables/useI18n'

const route = useRoute()
const user = ref(null)
const targets = ref([])
const loading = ref(true)
const temporaryPassword = ref('')

const { locale, t } = useI18n({
  title: { zh: '用户详情', en: 'User Detail' },
  subtitle: { zh: '查看账户信息、调整角色并审计其监控资产', en: 'Inspect account info, update role and audit target assets' },
  back: { zh: '返回', en: 'Back' },
  profileTitle: { zh: '用户信息', en: 'User Profile' },
  temporaryPassword: { zh: '重置临时密码', en: 'Reset Temporary Password' },
  temporaryPasswordIssued: { zh: '临时密码已生成', en: 'Temporary password issued' },
  temporaryPasswordHint: { zh: '请把这串临时密码发给用户。用户下次登录后必须先修改密码。', en: 'Share this temporary password with the user. They will be forced to change it after the next login.' },
  copy: { zh: '复制', en: 'Copy' },
  copySuccess: { zh: '临时密码已复制', en: 'Temporary password copied' },
  username: { zh: '用户名', en: 'Username' },
  email: { zh: '邮箱', en: 'Email' },
  notificationEmail: { zh: '接收邮箱', en: 'Recipient Email' },
  notificationStatus: { zh: '通知状态', en: 'Notification Status' },
  role: { zh: '角色', en: 'Role' },
  passwordStatus: { zh: '密码状态', en: 'Password Status' },
  enabled: { zh: '启用', en: 'Enabled' },
  disabled: { zh: '停用', en: 'Disabled' },
  mustChange: { zh: '需首次改密', en: 'Password change required' },
  normalPassword: { zh: '正常', en: 'Normal' },
  createdAt: { zh: '注册时间', en: 'Created At' },
  targetsTitle: { zh: '监控目标', en: 'Targets' },
  name: { zh: '名称', en: 'Name' },
  hostname: { zh: '主机名', en: 'Hostname' },
  description: { zh: '描述', en: 'Description' },
  addedAt: { zh: '添加时间', en: 'Added At' },
  loading: { zh: '加载中...', en: 'Loading...' },
  loadFailed: { zh: '加载失败', en: 'Load failed' },
  roleUpdated: { zh: '角色已更新', en: 'Role updated' },
  roleUpdateFailed: { zh: '更新失败', en: 'Update failed' },
  temporaryPasswordFailed: { zh: '生成临时密码失败', en: 'Failed to issue temporary password' }
})

async function fetchDetail() {
  try {
    const { data } = await getUserDetail(route.params.id)
    user.value = data.user
    targets.value = data.targets || []
  } catch (_e) {
    ElMessage.error(t('loadFailed'))
  } finally {
    loading.value = false
  }
}

async function handleRoleChange(role) {
  try {
    await updateUserRole(user.value.id, role)
    ElMessage.success(t('roleUpdated'))
  } catch (_e) {
    ElMessage.error(t('roleUpdateFailed'))
  }
}

async function handleTemporaryPasswordReset() {
  try {
    const { data } = await issueTemporaryPassword(user.value.id)
    temporaryPassword.value = data.temporaryPassword
    user.value.passwordChangeRequired = true
    ElMessage.success(t('temporaryPasswordIssued'))
  } catch (_e) {
    ElMessage.error(t('temporaryPasswordFailed'))
  }
}

async function copyTemporaryPassword() {
  await navigator.clipboard.writeText(temporaryPassword.value)
  ElMessage.success(t('copySuccess'))
}

function formatDate(value) {
  if (!value) return '-'
  const localeCode = locale.value === 'zh' ? 'zh-CN' : 'en-US'
  return new Date(value).toLocaleString(localeCode)
}

onMounted(fetchDetail)
</script>
