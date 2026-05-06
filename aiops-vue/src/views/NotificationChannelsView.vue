<template>
  <div class="page-surface notification-page">
    <div class="page-hero notification-hero">
      <div>
        <h1>{{ t('title') }}</h1>
        <p>{{ t('subtitle') }}</p>
      </div>
      <div class="inline-actions">
        <el-button @click="loadAll" :loading="loadingAny">{{ t('refresh') }}</el-button>
      </div>
    </div>

    <section class="kpi-grid notification-kpi-grid">
      <div class="kpi-item notification-kpi">
        <p class="kpi-label">{{ t('profileStatus') }}</p>
        <p class="kpi-value" :class="profileForm.enabled ? 'text-emerald-300' : 'text-slate-300'">
          {{ profileForm.enabled ? t('enabled') : t('disabled') }}
        </p>
      </div>
      <div v-if="auth.isAdmin" class="kpi-item notification-kpi">
        <p class="kpi-label">{{ t('platformStatus') }}</p>
        <p class="kpi-value" :class="platformConfigured ? 'text-amber-300' : 'text-slate-300'">
          {{ platformConfigured ? t('configured') : t('notConfigured') }}
        </p>
      </div>
      <div v-if="canOperate" class="kpi-item notification-kpi">
        <p class="kpi-label">{{ t('webhookCount') }}</p>
        <p class="kpi-value text-cyan-300">{{ channels.length }}</p>
      </div>
    </section>

    <section class="card-panel notification-main-card">
      <div class="section-head notification-head">
        <div>
          <h2 class="section-title">{{ t('centerTitle') }}</h2>
          <p class="section-subtitle">{{ t('centerSubtitle') }}</p>
        </div>
      </div>

      <el-tabs v-model="activeTab" class="notification-tabs">
        <el-tab-pane :label="t('tabProfile')" name="profile">
          <section class="notification-section-grid">
            <div class="notification-card card-panel-inner">
              <div class="section-head compact-head">
                <div>
                  <h3 class="section-title">{{ t('myProfileTitle') }}</h3>
                  <p class="section-subtitle">{{ t('myProfileSubtitle') }}</p>
                </div>
                <el-tag :type="profileForm.enabled ? 'success' : 'info'">{{ profileForm.enabled ? t('enabled') : t('disabled') }}</el-tag>
              </div>

              <el-form :model="profileForm" label-width="110px" class="notification-form-grid">
                <el-form-item :label="t('recipientEmail')">
                  <el-input v-model="profileForm.recipientEmail" :placeholder="t('recipientPlaceholder')" />
                </el-form-item>
                <div class="form-helper-box">
                  {{ t('recipientHint') }}
                </div>
                <el-form-item :label="t('receiveSwitch')">
                  <el-switch v-model="profileForm.enabled" />
                </el-form-item>
              </el-form>

              <el-alert
                v-if="!profileForm.enabled"
                type="warning"
                :closable="false"
                :title="t('profileDisabledTitle')"
                :description="t('profileDisabledDesc')" />

              <div class="notification-actions">
                <el-button type="primary" :loading="savingProfile" @click="saveProfile">{{ t('saveMyProfile') }}</el-button>
              </div>
            </div>
          </section>
        </el-tab-pane>

        <el-tab-pane v-if="auth.isAdmin" :label="t('tabPlatform')" name="platform">
          <section class="notification-section-grid">
            <div class="notification-card card-panel-inner">
              <div class="section-head compact-head">
                <div>
                  <h3 class="section-title">{{ t('platformMailTitle') }}</h3>
                  <p class="section-subtitle">{{ t('platformMailSubtitle') }}</p>
                </div>
                <el-tag :type="platformForm.enabled ? 'warning' : 'info'">{{ platformConfigured ? t('configured') : t('notConfigured') }}</el-tag>
              </div>

              <el-form :model="platformForm" label-width="120px" class="notification-form-grid">
                <div class="notification-form-columns two-col">
                  <el-form-item :label="t('smtpHost')">
                    <el-input v-model="platformForm.smtpHost" placeholder="smtp.qq.com" />
                  </el-form-item>
                  <el-form-item :label="t('smtpPort')">
                    <el-input-number v-model="platformForm.smtpPort" :min="1" :max="65535" class="w-full" />
                  </el-form-item>
                  <el-form-item :label="t('smtpUsername')">
                    <el-input v-model="platformForm.smtpUsername" :placeholder="t('smtpUserPlaceholder')" />
                  </el-form-item>
                  <el-form-item :label="t('smtpPassword')">
                    <el-input v-model="platformForm.smtpPassword" type="password" show-password :placeholder="platformConfigured ? t('smtpPasswordKeep') : t('smtpPasswordPlaceholder')" />
                  </el-form-item>
                  <el-form-item :label="t('fromAddress')">
                    <el-input v-model="platformForm.fromAddress" :placeholder="t('fromAddressPlaceholder')" />
                  </el-form-item>
                  <el-form-item :label="t('fromName')">
                    <el-input v-model="platformForm.fromName" :placeholder="t('fromNamePlaceholder')" />
                  </el-form-item>
                </div>
                <el-form-item :label="t('smtpSecurity')">
                  <div class="switch-chip-row">
                    <el-switch v-model="platformForm.smtpAuth" :active-text="t('smtpAuth')" />
                    <el-switch v-model="platformForm.smtpStarttls" :active-text="t('smtpStarttls')" />
                    <el-switch v-model="platformForm.enabled" :active-text="t('mailEnabled')" />
                  </div>
                </el-form-item>
                <el-form-item :label="t('testRecipient')">
                  <el-input v-model="platformTestRecipient" :placeholder="t('testRecipientPlaceholder')" />
                </el-form-item>
              </el-form>

              <div class="notification-actions">
                <el-button :loading="testingPlatform" @click="testPlatformMail">{{ t('testMail') }}</el-button>
                <el-button type="primary" :loading="savingPlatform" @click="savePlatformMail">{{ t('savePlatformMail') }}</el-button>
              </div>
            </div>
          </section>
        </el-tab-pane>

        <el-tab-pane v-if="canOperate" :label="t('tabWebhook')" name="webhook">
          <section class="notification-section-grid">
            <div class="notification-card card-panel-inner">
              <div class="section-head compact-head">
                <div>
                  <h3 class="section-title">{{ t('webhookTitle') }}</h3>
                  <p class="section-subtitle">{{ t('webhookSubtitle') }}</p>
                </div>
                <el-button type="primary" @click="openCreateDialog">{{ t('createWebhook') }}</el-button>
              </div>

              <div class="ops-table table-wrap notification-table-wrap">
                <el-table :data="channels" v-loading="loadingChannels">
                  <el-table-column prop="name" :label="t('name')" min-width="170" />
                  <el-table-column :label="t('target')" min-width="320">
                    <template #default="{ row }">
                      <span class="webhook-url-text">{{ row.webhookUrl || '-' }}</span>
                    </template>
                  </el-table-column>
                  <el-table-column :label="t('enabled')" width="120" align="center">
                    <template #default="{ row }">
                      <el-switch
                        :model-value="row.enabled"
                        :disabled="!canOperate"
                        @change="(value) => toggleEnabled(row, value)"
                        inline-prompt
                        active-text="ON"
                        inactive-text="OFF" />
                    </template>
                  </el-table-column>
                  <el-table-column :label="t('lastDelivery')" width="180">
                    <template #default="{ row }">{{ formatDate(row.lastNotifiedAt) }}</template>
                  </el-table-column>
                  <el-table-column :label="t('lastError')" min-width="240">
                    <template #default="{ row }">
                      <span class="webhook-error-text">{{ row.lastError || '-' }}</span>
                    </template>
                  </el-table-column>
                  <el-table-column :label="t('actions')" width="260" fixed="right">
                    <template #default="{ row }">
                      <div class="notification-action-row">
                        <el-button size="small" :loading="testingWebhookId === row.id" @click="testChannel(row)">{{ t('test') }}</el-button>
                        <el-button size="small" @click="openEditDialog(row)">{{ t('edit') }}</el-button>
                        <el-button size="small" type="danger" @click="removeChannel(row)">{{ t('delete') }}</el-button>
                      </div>
                    </template>
                  </el-table-column>
                </el-table>
              </div>
            </div>
          </section>
        </el-tab-pane>
      </el-tabs>
    </section>

    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? t('editWebhook') : t('createWebhook')"
      width="640px"
      destroy-on-close>
      <el-form :model="form" label-width="120px" class="notification-form-grid">
        <el-form-item :label="t('name')">
          <el-input v-model="form.name" maxlength="100" :placeholder="t('webhookNamePlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('webhookUrl')">
          <el-input v-model="form.webhookUrl" placeholder="https://example.com/hooks/aiops" />
        </el-form-item>
        <el-form-item :label="t('secretOptional')">
          <el-input v-model="form.secret" type="password" show-password :placeholder="t('secretPlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('enabled')">
          <el-switch v-model="form.enabled" />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="notification-actions">
          <el-button @click="dialogVisible = false">{{ t('cancel') }}</el-button>
          <el-button type="primary" :loading="savingWebhook" @click="saveChannel">{{ t('save') }}</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createNotificationChannel,
  deleteNotificationChannel,
  getNotificationChannels,
  testNotificationChannel,
  updateNotificationChannel,
  updateNotificationChannelEnabled
} from '../api/notifications'
import { getNotificationProfile, updateNotificationProfile } from '../api/auth'
import { getPlatformMailSettings, testPlatformMailSettings, updatePlatformMailSettings } from '../api/platform'
import { useI18n } from '../composables/useI18n'
import { usePermissions } from '../composables/usePermissions'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const { canOperate, canConfigurePlatform, readOnlyReason } = usePermissions()

const loadingProfile = ref(false)
const loadingPlatform = ref(false)
const loadingChannels = ref(false)
const savingProfile = ref(false)
const savingPlatform = ref(false)
const savingWebhook = ref(false)
const testingPlatform = ref(false)
const testingWebhookId = ref(null)
const dialogVisible = ref(false)
const editingId = ref(null)
const channels = ref([])
const platformConfigured = ref(false)
const platformTestRecipient = ref('')
const activeTab = ref('profile')

const profileForm = reactive({
  recipientEmail: '',
  enabled: false
})

const platformForm = reactive({
  smtpHost: '',
  smtpPort: 587,
  smtpUsername: '',
  smtpPassword: '',
  smtpAuth: true,
  smtpStarttls: true,
  fromAddress: '',
  fromName: '',
  enabled: true
})

const form = reactive({
  name: '',
  webhookUrl: '',
  secret: '',
  enabled: true
})

const loadingAny = computed(() => loadingProfile.value || loadingPlatform.value || loadingChannels.value)

const { locale, t } = useI18n({
  title: { zh: '通知中心', en: 'Notification Center' },
  subtitle: { zh: '统一管理用户收件邮箱、平台发信配置与 Webhook 集成通道。', en: 'Manage personal recipient settings, outbound mail, and webhook integrations in one place.' },
  refresh: { zh: '刷新', en: 'Refresh' },
  centerTitle: { zh: '通知配置面板', en: 'Notification Configuration' },
  centerSubtitle: { zh: '按角色进入不同配置分区，避免一页堆叠过多表单。', en: 'Use role-based tabs to keep configuration areas focused and easier to operate.' },
  tabProfile: { zh: '我的接收', en: 'My Recipient' },
  tabPlatform: { zh: '平台发信', en: 'Platform Mail' },
  tabWebhook: { zh: 'Webhook 通道', en: 'Webhook Channels' },
  profileStatus: { zh: '个人接收状态', en: 'Recipient Status' },
  platformStatus: { zh: '平台发信状态', en: 'Platform Mail Status' },
  webhookCount: { zh: 'Webhook 数量', en: 'Webhook Count' },
  myProfileTitle: { zh: '我的接收设置', en: 'My Recipient Settings' },
  myProfileSubtitle: { zh: '这里决定系统把我的告警邮件发到哪里。', en: 'This decides where my alert emails are delivered.' },
  recipientEmail: { zh: '接收邮箱', en: 'Recipient Email' },
  recipientPlaceholder: { zh: '例如：1789239110@qq.com', en: 'e.g. alerts@example.com' },
  recipientHint: { zh: '默认使用注册邮箱，你也可以改成值班邮箱、团队邮箱或其他专用收件地址。', en: 'Your registration email is used by default, but you can switch to an on-call inbox, team mailbox, or other dedicated address.' },
  receiveSwitch: { zh: '接收开关', en: 'Delivery Switch' },
  saveMyProfile: { zh: '保存接收设置', en: 'Save Recipient Settings' },
  profileDisabledTitle: { zh: '当前未接收个人告警邮件', en: 'Personal alert email is currently disabled' },
  profileDisabledDesc: { zh: '即使管理员已配置平台发信邮箱，只要这里关闭，你的账号也不会收到任何告警邮件。', en: 'Even if outbound SMTP is configured, your account will not receive alert emails while this switch is off.' },
  savedProfile: { zh: '接收设置已更新', en: 'Recipient settings updated' },
  saveProfileFailed: { zh: '保存接收设置失败', en: 'Failed to save recipient settings' },
  profileEmailRequired: { zh: '请填写接收邮箱', en: 'Please enter recipient email' },
  platformMailTitle: { zh: '平台发信配置', en: 'Platform Outbound Mail' },
  platformMailSubtitle: { zh: '仅管理员可维护 SMTP 发信邮箱与授权码。', en: 'Only admins can maintain the SMTP sender account and authorization code.' },
  smtpHost: { zh: 'SMTP 主机', en: 'SMTP Host' },
  smtpPort: { zh: 'SMTP 端口', en: 'SMTP Port' },
  smtpUsername: { zh: '发件邮箱', en: 'Sender Account' },
  smtpUserPlaceholder: { zh: '例如：1402863249@qq.com', en: 'e.g. no-reply@example.com' },
  smtpPassword: { zh: '授权码', en: 'Authorization Code' },
  smtpPasswordPlaceholder: { zh: '请输入 SMTP 授权码', en: 'Enter SMTP password or app code' },
  smtpPasswordKeep: { zh: '留空表示保持原授权码', en: 'Leave blank to keep the current secret' },
  fromAddress: { zh: '发件地址', en: 'From Address' },
  fromAddressPlaceholder: { zh: '默认可与发件邮箱一致', en: 'Usually the same as sender account' },
  fromName: { zh: '发件人名称', en: 'From Name' },
  fromNamePlaceholder: { zh: '例如：AIOps Monitor', en: 'e.g. AIOps Monitor' },
  smtpSecurity: { zh: '安全选项', en: 'Security' },
  smtpAuth: { zh: 'SMTP 认证', en: 'SMTP Auth' },
  smtpStarttls: { zh: 'STARTTLS', en: 'STARTTLS' },
  mailEnabled: { zh: '启用发信', en: 'Enable Mail' },
  testRecipient: { zh: '测试收件人', en: 'Test Recipient' },
  testRecipientPlaceholder: { zh: '点击测试发送到这个邮箱', en: 'Send test mail to this address' },
  testMail: { zh: '测试发信', en: 'Send Test Mail' },
  savePlatformMail: { zh: '保存平台配置', en: 'Save Platform Mail' },
  configured: { zh: '已配置', en: 'Configured' },
  notConfigured: { zh: '未配置', en: 'Not Configured' },
  loadPlatformFailed: { zh: '加载平台发信配置失败', en: 'Failed to load platform mail settings' },
  savePlatformFailed: { zh: '保存平台发信配置失败', en: 'Failed to save platform mail settings' },
  testPlatformFailed: { zh: '测试发信失败', en: 'Failed to send test mail' },
  savedPlatform: { zh: '平台发信配置已保存', en: 'Platform mail settings saved' },
  platformTestSent: { zh: '测试邮件已发送', en: 'Test mail sent' },
  webhookTitle: { zh: 'Webhook 通道', en: 'Webhook Channels' },
  webhookSubtitle: { zh: '用于对接企业微信、飞书、工单平台或自定义网关。', en: 'Used to integrate with enterprise chat, ticketing tools, or custom gateways.' },
  createWebhook: { zh: '新增 Webhook', en: 'New Webhook' },
  editWebhook: { zh: '编辑 Webhook', en: 'Edit Webhook' },
  name: { zh: '名称', en: 'Name' },
  target: { zh: '目标地址', en: 'Target' },
  webhookUrl: { zh: 'Webhook 地址', en: 'Webhook URL' },
  secretOptional: { zh: 'Secret(可选)', en: 'Secret (Optional)' },
  secretPlaceholder: { zh: '用于接收端鉴权', en: 'Used for receiver authentication' },
  enabled: { zh: '启用', en: 'Enabled' },
  disabled: { zh: '停用', en: 'Disabled' },
  lastDelivery: { zh: '最近投递', en: 'Last Delivery' },
  lastError: { zh: '最近错误', en: 'Last Error' },
  actions: { zh: '操作', en: 'Actions' },
  test: { zh: '测试发送', en: 'Test Send' },
  edit: { zh: '编辑', en: 'Edit' },
  delete: { zh: '删除', en: 'Delete' },
  webhookNamePlaceholder: { zh: '例如：飞书告警网关', en: 'e.g. Feishu Alert Gateway' },
  cancel: { zh: '取消', en: 'Cancel' },
  save: { zh: '保存', en: 'Save' },
  loadFailed: { zh: '加载 Webhook 通道失败', en: 'Failed to load webhook channels' },
  nameRequired: { zh: '请填写通道名称', en: 'Please enter channel name' },
  webhookRequired: { zh: '请填写 Webhook 地址', en: 'Please enter Webhook URL' },
  saved: { zh: 'Webhook 通道已保存', en: 'Webhook channel saved' },
  saveFailed: { zh: '保存失败', en: 'Save failed' },
  statusUpdated: { zh: '状态已更新', en: 'Status updated' },
  statusUpdateFailed: { zh: '更新启用状态失败', en: 'Failed to update status' },
  testSent: { zh: '测试通知已发出，请检查目标系统', en: 'Test notification sent. Check your target system.' },
  testFailed: { zh: '测试发送失败', en: 'Test send failed' },
  deleteConfirm: { zh: '确认删除 Webhook「{name}」吗？', en: 'Delete webhook channel "{name}"?' },
  deleteTitle: { zh: '删除确认', en: 'Delete Confirmation' },
  deleted: { zh: '已删除', en: 'Deleted' },
  deleteFailed: { zh: '删除失败', en: 'Delete failed' }
})

async function loadAll() {
  await Promise.all([
    loadProfile(),
    canConfigurePlatform.value ? loadPlatformMail() : Promise.resolve(),
    canOperate.value ? loadChannels() : Promise.resolve()
  ])
}

async function loadProfile() {
  loadingProfile.value = true
  try {
    const { data } = await getNotificationProfile()
    profileForm.recipientEmail = data?.recipientEmail || auth.notificationEmail || ''
    profileForm.enabled = Boolean(data?.enabled)
    auth.setNotificationProfile(data || { recipientEmail: '', enabled: false })
  } catch (_e) {
    ElMessage.error(t('saveProfileFailed'))
  } finally {
    loadingProfile.value = false
  }
}

async function loadPlatformMail() {
  loadingPlatform.value = true
  try {
    const { data } = await getPlatformMailSettings()
    platformConfigured.value = Boolean(data?.configured)
    platformForm.smtpHost = data?.smtpHost || ''
    platformForm.smtpPort = data?.smtpPort || 587
    platformForm.smtpUsername = data?.smtpUsername || ''
    platformForm.smtpPassword = ''
    platformForm.smtpAuth = data?.smtpAuth !== false
    platformForm.smtpStarttls = data?.smtpStarttls !== false
    platformForm.fromAddress = data?.fromAddress || ''
    platformForm.fromName = data?.fromName || ''
    platformForm.enabled = data?.enabled !== false
    platformTestRecipient.value = auth.notificationEmail || profileForm.recipientEmail || ''
  } catch (_e) {
    ElMessage.error(t('loadPlatformFailed'))
  } finally {
    loadingPlatform.value = false
  }
}

async function loadChannels() {
  loadingChannels.value = true
  try {
    const { data } = await getNotificationChannels()
    channels.value = Array.isArray(data) ? data : []
  } catch (_e) {
    ElMessage.error(t('loadFailed'))
  } finally {
    loadingChannels.value = false
  }
}

async function saveProfile() {
  if (!profileForm.recipientEmail.trim()) {
    ElMessage.warning(t('profileEmailRequired'))
    return
  }
  savingProfile.value = true
  try {
    const { data } = await updateNotificationProfile({
      recipientEmail: profileForm.recipientEmail.trim(),
      enabled: Boolean(profileForm.enabled)
    })
    auth.setNotificationProfile(data)
    ElMessage.success(t('savedProfile'))
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || t('saveProfileFailed'))
  } finally {
    savingProfile.value = false
  }
}

async function savePlatformMail() {
  savingPlatform.value = true
  try {
    await updatePlatformMailSettings({
      smtpHost: platformForm.smtpHost.trim(),
      smtpPort: platformForm.smtpPort,
      smtpUsername: platformForm.smtpUsername.trim(),
      smtpPassword: platformForm.smtpPassword,
      smtpAuth: Boolean(platformForm.smtpAuth),
      smtpStarttls: Boolean(platformForm.smtpStarttls),
      fromAddress: platformForm.fromAddress.trim(),
      fromName: platformForm.fromName?.trim() || null,
      enabled: Boolean(platformForm.enabled)
    })
    ElMessage.success(t('savedPlatform'))
    await loadPlatformMail()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || t('savePlatformFailed'))
  } finally {
    savingPlatform.value = false
  }
}

async function testPlatformMail() {
  if (!platformTestRecipient.value.trim()) {
    ElMessage.warning(t('profileEmailRequired'))
    return
  }
  testingPlatform.value = true
  try {
    const { data } = await testPlatformMailSettings(platformTestRecipient.value.trim())
    ElMessage.success(data?.message || t('platformTestSent'))
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || t('testPlatformFailed'))
  } finally {
    testingPlatform.value = false
  }
}

function resetForm() {
  form.name = ''
  form.webhookUrl = ''
  form.secret = ''
  form.enabled = true
}

function openCreateDialog() {
  if (!canOperate.value) {
    ElMessage.warning(readOnlyReason.value)
    return
  }
  editingId.value = null
  resetForm()
  dialogVisible.value = true
}

function openEditDialog(channel) {
  if (!canOperate.value) {
    ElMessage.warning(readOnlyReason.value)
    return
  }
  editingId.value = channel.id
  form.name = channel.name || ''
  form.webhookUrl = channel.webhookUrl || ''
  form.secret = ''
  form.enabled = Boolean(channel.enabled)
  dialogVisible.value = true
}

async function saveChannel() {
  if (!canOperate.value) {
    ElMessage.warning(readOnlyReason.value)
    return
  }
  if (!form.name.trim()) {
    ElMessage.warning(t('nameRequired'))
    return
  }
  if (!form.webhookUrl.trim()) {
    ElMessage.warning(t('webhookRequired'))
    return
  }

  savingWebhook.value = true
  try {
    const payload = {
      name: form.name.trim(),
      type: 'WEBHOOK',
      webhookUrl: form.webhookUrl.trim(),
      secret: form.secret?.trim() || null,
      enabled: Boolean(form.enabled)
    }
    if (editingId.value && !form.secret?.trim()) {
      delete payload.secret
    }
    if (editingId.value) {
      await updateNotificationChannel(editingId.value, payload)
    } else {
      await createNotificationChannel(payload)
    }
    dialogVisible.value = false
    ElMessage.success(t('saved'))
    await loadChannels()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || t('saveFailed'))
  } finally {
    savingWebhook.value = false
  }
}

async function toggleEnabled(row, enabled) {
  if (!canOperate.value) {
    ElMessage.warning(readOnlyReason.value)
    return
  }
  const previous = row.enabled
  row.enabled = enabled
  try {
    await updateNotificationChannelEnabled(row.id, Boolean(enabled))
    ElMessage.success(t('statusUpdated'))
  } catch (_e) {
    row.enabled = previous
    ElMessage.error(t('statusUpdateFailed'))
  }
}

async function testChannel(row) {
  testingWebhookId.value = row.id
  try {
    const { data } = await testNotificationChannel(row.id)
    ElMessage.success(data?.message || t('testSent'))
    await loadChannels()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || t('testFailed'))
  } finally {
    testingWebhookId.value = null
  }
}

async function removeChannel(row) {
  try {
    await ElMessageBox.confirm(t('deleteConfirm', { name: row.name }), t('deleteTitle'), { type: 'warning' })
    await deleteNotificationChannel(row.id)
    ElMessage.success(t('deleted'))
    await loadChannels()
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') {
      ElMessage.error(t('deleteFailed'))
    }
  }
}

function formatDate(value) {
  if (!value) return '-'
  const localeCode = locale.value === 'zh' ? 'zh-CN' : 'en-US'
  return new Date(value).toLocaleString(localeCode)
}

onMounted(loadAll)
</script>

<style scoped>
.notification-page {
  gap: 20px;
}

.notification-hero {
  background: radial-gradient(760px 220px at 0 0, rgba(34, 197, 94, 0.12), transparent 60%), var(--hero-bg);
}

.notification-kpi-grid {
  gap: 14px;
}

.notification-kpi {
  min-height: 112px;
}

.notification-main-card {
  padding: 20px;
}

.notification-head {
  margin-bottom: 18px;
}

.notification-tabs :deep(.el-tabs__header) {
  margin: 0 0 20px;
}

.notification-tabs :deep(.el-tabs__nav-wrap::after) {
  background: rgba(148, 163, 184, 0.16);
}

.notification-tabs :deep(.el-tabs__item) {
  height: 42px;
  padding: 0 18px;
  border-radius: 12px 12px 0 0;
  color: var(--text-3);
  font-weight: 600;
}

.notification-tabs :deep(.el-tabs__item.is-active) {
  color: var(--text-1);
}

.notification-tabs :deep(.el-tabs__active-bar) {
  height: 3px;
  border-radius: 999px;
  background: linear-gradient(90deg, var(--brand), rgba(34, 197, 94, 0.9));
}

.notification-section-grid {
  display: grid;
  gap: 16px;
}

.card-panel-inner {
  border: 1px solid var(--line);
  border-radius: 18px;
  background: var(--panel-soft);
  padding: 20px;
}

.notification-form-grid {
  display: grid;
  gap: 14px;
}

.notification-form-columns.two-col {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.form-helper-box {
  margin-top: -8px;
  padding: 12px 14px;
  border-radius: 14px;
  border: 1px dashed var(--line);
  background: rgba(56, 189, 248, 0.05);
  color: var(--text-3);
  font-size: 12px;
}

.switch-chip-row {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}

.notification-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 18px;
}

.notification-table-wrap {
  padding: 0;
}

.notification-action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
}

.webhook-url-text {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 12px;
  color: var(--text-1);
  word-break: break-all;
}

.webhook-error-text {
  font-size: 12px;
  color: #fda4af;
}

@media (max-width: 900px) {
  .notification-form-columns.two-col {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .notification-main-card,
  .card-panel-inner {
    padding: 16px;
  }

  .notification-actions {
    justify-content: stretch;
  }

  .notification-actions :deep(.el-button) {
    width: 100%;
  }
}
</style>
