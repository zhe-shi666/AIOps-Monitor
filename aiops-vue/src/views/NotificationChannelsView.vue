<template>
  <div class="page-surface">
    <div class="page-hero">
      <div>
        <h1>{{ t('title') }}</h1>
        <p>{{ t('subtitle') }}</p>
      </div>
      <div class="inline-actions">
        <el-button @click="loadChannels" :loading="loading">{{ t('refresh') }}</el-button>
        <el-button type="primary" @click="openCreateDialog">{{ t('create') }}</el-button>
      </div>
    </div>

    <div class="card-panel table-wrap ops-table">
      <el-table :data="channels" v-loading="loading">
        <el-table-column prop="name" :label="t('name')" min-width="140" />
        <el-table-column prop="type" :label="t('type')" width="120" />
        <el-table-column label="Webhook" min-width="280">
          <template #default="{ row }">
            <span class="font-mono text-xs text-slate-300 break-all">{{ row.webhookUrl }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('enabled')" width="120" align="center">
          <template #default="{ row }">
            <el-switch
              :model-value="row.enabled"
              @change="(value) => toggleEnabled(row, value)"
              inline-prompt
              active-text="ON"
              inactive-text="OFF" />
          </template>
        </el-table-column>
        <el-table-column :label="t('lastDelivery')" width="180">
          <template #default="{ row }">{{ formatDate(row.lastNotifiedAt) }}</template>
        </el-table-column>
        <el-table-column :label="t('lastError')" min-width="220">
          <template #default="{ row }">
            <span class="text-xs text-rose-300">{{ row.lastError || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('actions')" width="190" fixed="right">
          <template #default="{ row }">
            <div class="flex items-center gap-2">
              <el-button size="small" @click="openEditDialog(row)">{{ t('edit') }}</el-button>
              <el-button size="small" type="danger" @click="removeChannel(row)">{{ t('delete') }}</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? t('editDialog') : t('createDialog')"
      width="640px"
      destroy-on-close>
      <el-form :model="form" label-width="120px" class="space-y-3">
        <el-form-item :label="t('name')">
          <el-input v-model="form.name" maxlength="100" :placeholder="t('namePlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('type')">
          <el-input v-model="form.type" disabled />
        </el-form-item>
        <el-form-item label="Webhook URL">
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
        <div class="flex items-center justify-end gap-2">
          <el-button @click="dialogVisible = false">{{ t('cancel') }}</el-button>
          <el-button type="primary" :loading="saving" @click="saveChannel">{{ t('save') }}</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createNotificationChannel,
  deleteNotificationChannel,
  getNotificationChannels,
  updateNotificationChannel,
  updateNotificationChannelEnabled
} from '../api/notifications'
import { useI18n } from '../composables/useI18n'

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const editingId = ref(null)
const channels = ref([])

const form = reactive({
  name: '',
  type: 'WEBHOOK',
  webhookUrl: '',
  secret: '',
  enabled: true
})

const { locale, t } = useI18n({
  title: { zh: '通知通道', en: 'Notification Channels' },
  subtitle: { zh: '管理告警 Webhook 通道，支持启停与投递审计', en: 'Manage alert webhooks with enable controls and delivery audit' },
  refresh: { zh: '刷新', en: 'Refresh' },
  create: { zh: '新增通道', en: 'New Channel' },
  name: { zh: '名称', en: 'Name' },
  type: { zh: '类型', en: 'Type' },
  enabled: { zh: '启用', en: 'Enabled' },
  lastDelivery: { zh: '最近投递', en: 'Last Delivery' },
  lastError: { zh: '最近错误', en: 'Last Error' },
  actions: { zh: '操作', en: 'Actions' },
  edit: { zh: '编辑', en: 'Edit' },
  delete: { zh: '删除', en: 'Delete' },
  editDialog: { zh: '编辑通知通道', en: 'Edit Channel' },
  createDialog: { zh: '新增通知通道', en: 'Create Channel' },
  namePlaceholder: { zh: '例如：运维值班 Webhook', en: 'e.g. Ops On-call Webhook' },
  secretOptional: { zh: 'Secret(可选)', en: 'Secret (Optional)' },
  secretPlaceholder: { zh: '用于接收端鉴权', en: 'Used for receiver authentication' },
  cancel: { zh: '取消', en: 'Cancel' },
  save: { zh: '保存', en: 'Save' },
  loadFailed: { zh: '加载通知通道失败', en: 'Failed to load channels' },
  nameRequired: { zh: '请填写通道名称', en: 'Please enter channel name' },
  webhookRequired: { zh: '请填写 Webhook URL', en: 'Please enter Webhook URL' },
  saved: { zh: '通知通道已保存', en: 'Channel saved' },
  saveFailed: { zh: '保存失败', en: 'Save failed' },
  statusUpdated: { zh: '状态已更新', en: 'Status updated' },
  statusUpdateFailed: { zh: '更新启用状态失败', en: 'Failed to update status' },
  deleteConfirm: { zh: '确认删除通知通道「{name}」吗？', en: 'Delete notification channel "{name}"?' },
  deleteTitle: { zh: '删除确认', en: 'Delete Confirmation' },
  deleted: { zh: '已删除', en: 'Deleted' },
  deleteFailed: { zh: '删除失败', en: 'Delete failed' }
})

async function loadChannels() {
  loading.value = true
  try {
    const { data } = await getNotificationChannels()
    channels.value = Array.isArray(data) ? data : []
  } catch (_e) {
    ElMessage.error(t('loadFailed'))
  } finally {
    loading.value = false
  }
}

function resetForm() {
  form.name = ''
  form.type = 'WEBHOOK'
  form.webhookUrl = ''
  form.secret = ''
  form.enabled = true
}

function openCreateDialog() {
  editingId.value = null
  resetForm()
  dialogVisible.value = true
}

function openEditDialog(channel) {
  editingId.value = channel.id
  form.name = channel.name || ''
  form.type = channel.type || 'WEBHOOK'
  form.webhookUrl = channel.webhookUrl || ''
  form.secret = ''
  form.enabled = Boolean(channel.enabled)
  dialogVisible.value = true
}

async function saveChannel() {
  if (!form.name.trim()) {
    ElMessage.warning(t('nameRequired'))
    return
  }
  if (!form.webhookUrl.trim()) {
    ElMessage.warning(t('webhookRequired'))
    return
  }

  saving.value = true
  try {
    const payload = {
      name: form.name.trim(),
      type: 'WEBHOOK',
      webhookUrl: form.webhookUrl.trim(),
      enabled: Boolean(form.enabled)
    }
    const normalizedSecret = form.secret?.trim()
    if (editingId.value) {
      if (normalizedSecret) payload.secret = normalizedSecret
    } else {
      payload.secret = normalizedSecret || null
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
    saving.value = false
  }
}

async function toggleEnabled(row, enabled) {
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

async function removeChannel(row) {
  try {
    await ElMessageBox.confirm(
      t('deleteConfirm', { name: row.name }),
      t('deleteTitle'),
      { type: 'warning' }
    )
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

onMounted(loadChannels)
</script>
