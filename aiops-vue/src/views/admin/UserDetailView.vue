<template>
  <div class="admin-section-space">
    <div class="section-head admin-detail-head">
      <div>
        <h2 class="section-title">{{ t('title') }}</h2>
        <p class="section-subtitle">{{ t('subtitle') }}</p>
      </div>
      <el-button @click="$router.back()" plain>{{ t('back') }}</el-button>
    </div>

    <template v-if="user">
      <section class="card-panel admin-subcard">
        <div class="section-head admin-subhead">
          <div>
            <h3 class="section-title">{{ t('profileTitle') }}</h3>
            <p class="section-subtitle">{{ t('profileSubtitle') }}</p>
          </div>
          <el-button type="warning" @click="handleTemporaryPasswordReset">{{ t('temporaryPassword') }}</el-button>
        </div>

        <div v-if="temporaryPassword" class="auth-result-box admin-temp-box">
          <div class="auth-result-title">{{ t('temporaryPasswordIssued') }}</div>
          <p>{{ t('temporaryPasswordHint') }}</p>
          <div class="auth-token-row">
            <code>{{ temporaryPassword }}</code>
            <el-button size="small" @click="copyTemporaryPassword">{{ t('copy') }}</el-button>
          </div>
        </div>

        <div class="admin-user-grid">
          <div class="admin-info-card">
            <span>{{ t('username') }}</span>
            <strong>{{ user.username }}</strong>
          </div>
          <div class="admin-info-card">
            <span>{{ t('email') }}</span>
            <strong>{{ user.email }}</strong>
          </div>
          <div class="admin-info-card">
            <span>{{ t('notificationEmail') }}</span>
            <strong>{{ user.notificationEmail || '-' }}</strong>
          </div>
          <div class="admin-info-card inline-card">
            <span>{{ t('notificationStatus') }}</span>
            <el-tag :type="user.notificationEnabled ? 'success' : 'info'" size="small">
              {{ user.notificationEnabled ? t('enabled') : t('disabled') }}
            </el-tag>
          </div>
          <div class="admin-info-card inline-card">
            <span>{{ t('role') }}</span>
            <el-select v-model="user.role" size="small" @change="handleRoleChange" style="width: 140px;">
              <el-option label="USER" value="USER" />
              <el-option label="AUDITOR" value="AUDITOR" />
              <el-option label="OPS" value="OPS" />
              <el-option label="ADMIN" value="ADMIN" />
            </el-select>
          </div>
          <div class="admin-info-card inline-card">
            <span>{{ t('passwordStatus') }}</span>
            <el-tag :type="user.passwordChangeRequired ? 'warning' : 'success'" size="small">
              {{ user.passwordChangeRequired ? t('mustChange') : t('normalPassword') }}
            </el-tag>
          </div>
          <div class="admin-info-card">
            <span>{{ t('createdAt') }}</span>
            <strong>{{ formatDate(user.createdAt) }}</strong>
          </div>
        </div>
      </section>

      <section class="card-panel admin-subcard">
        <div class="section-head admin-subhead">
          <div>
            <h3 class="section-title">{{ t('targetsTitle') }}（{{ targets.length }}）</h3>
            <p class="section-subtitle">{{ t('targetsSubtitle') }}</p>
          </div>
        </div>

        <div class="ops-table table-wrap admin-table-wrap">
          <el-table :data="targets" size="small">
            <el-table-column prop="name" :label="t('name')" min-width="160" />
            <el-table-column prop="hostname" :label="t('hostname')" min-width="160" />
            <el-table-column prop="description" :label="t('description')" min-width="220" />
            <el-table-column prop="createdAt" :label="t('addedAt')" width="180">
              <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
            </el-table-column>
          </el-table>
        </div>
      </section>
    </template>

    <div v-else-if="loading" class="stats-loading-empty">{{ t('loading') }}</div>
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
  subtitle: { zh: '查看账户信息、调整角色并审计其监控资产。', en: 'Inspect account info, update role, and audit monitored assets.' },
  back: { zh: '返回', en: 'Back' },
  profileTitle: { zh: '用户信息', en: 'User Profile' },
  profileSubtitle: { zh: '这里集中展示账号身份、通知配置和密码状态。', en: 'Review identity, notification configuration, and password state in one place.' },
  temporaryPassword: { zh: '重置临时密码', en: 'Reset Temporary Password' },
  temporaryPasswordIssued: { zh: '临时密码已生成', en: 'Temporary password issued' },
  temporaryPasswordHint: { zh: '请把这串临时密码发给用户。用户下次登录后必须先修改密码。', en: 'Share this temporary password with the user. They must change it after the next login.' },
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
  targetsTitle: { zh: '监控目标', en: 'Monitored Targets' },
  targetsSubtitle: { zh: '展示该账号当前关联或拥有的监控目标资产。', en: 'Shows targets currently associated with or owned by this account.' },
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

<style scoped>
.admin-section-space {
  display: grid;
  gap: 18px;
}

.admin-detail-head {
  margin-bottom: 0;
}

.admin-subcard {
  padding: 20px;
}

.admin-subhead {
  margin-bottom: 18px;
}

.admin-temp-box {
  margin-bottom: 18px;
}

.admin-user-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.admin-info-card {
  padding: 14px 16px;
  border-radius: 16px;
  border: 1px solid var(--line);
  background: var(--panel-soft);
}

.admin-info-card span {
  display: block;
  color: var(--text-3);
  font-size: 12px;
  margin-bottom: 6px;
}

.admin-info-card strong {
  color: var(--text-1);
  font-size: 14px;
}

.inline-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.inline-card span {
  margin-bottom: 0;
}

.admin-table-wrap {
  padding: 0;
}

.stats-loading-empty {
  padding: 48px 16px;
  text-align: center;
  color: var(--text-3);
  border: 1px dashed var(--line);
  border-radius: 16px;
  background: var(--panel-soft);
}

@media (max-width: 760px) {
  .admin-user-grid {
    grid-template-columns: 1fr;
  }

  .inline-card {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
