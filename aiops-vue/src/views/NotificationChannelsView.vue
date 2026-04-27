<template>
  <div class="min-h-screen bg-slate-950 text-slate-100 p-8 pt-24">
    <div class="max-w-7xl mx-auto space-y-6">
      <div class="flex items-center justify-between">
        <div>
          <h1 class="text-2xl font-bold tracking-tight">通知通道</h1>
          <p class="text-sm text-slate-400 mt-1">管理告警 Webhook 通道，支持启停与投递审计</p>
        </div>
        <div class="flex items-center gap-3">
          <el-button @click="loadChannels" :loading="loading">刷新</el-button>
          <el-button type="primary" @click="openCreateDialog">新增通道</el-button>
        </div>
      </div>

      <div class="rounded-xl border border-slate-700 bg-slate-900/50 p-4">
        <el-table
          :data="channels"
          v-loading="loading"
          :header-cell-style="{ background: '#1e293b', color: '#94a3b8' }"
          :row-style="{ background: '#0f172a', color: '#e2e8f0' }">
          <el-table-column prop="name" label="名称" min-width="140" />
          <el-table-column prop="type" label="类型" width="120" />
          <el-table-column label="Webhook" min-width="280">
            <template #default="{ row }">
              <span class="font-mono text-xs text-slate-300 break-all">{{ row.webhookUrl }}</span>
            </template>
          </el-table-column>
          <el-table-column label="启用" width="120" align="center">
            <template #default="{ row }">
              <el-switch
                :model-value="row.enabled"
                @change="(value) => toggleEnabled(row, value)"
                inline-prompt
                active-text="ON"
                inactive-text="OFF" />
            </template>
          </el-table-column>
          <el-table-column label="最近投递" width="180">
            <template #default="{ row }">{{ formatDate(row.lastNotifiedAt) }}</template>
          </el-table-column>
          <el-table-column label="最近错误" min-width="220">
            <template #default="{ row }">
              <span class="text-xs text-rose-300">{{ row.lastError || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="190" fixed="right">
            <template #default="{ row }">
              <div class="flex items-center gap-2">
                <el-button size="small" @click="openEditDialog(row)">编辑</el-button>
                <el-button size="small" type="danger" @click="removeChannel(row)">删除</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑通知通道' : '新增通知通道'"
      width="640px"
      destroy-on-close>
      <el-form :model="form" label-width="120px" class="space-y-3">
        <el-form-item label="名称">
          <el-input v-model="form.name" maxlength="100" placeholder="例如：运维值班 Webhook" />
        </el-form-item>
        <el-form-item label="类型">
          <el-input v-model="form.type" disabled />
        </el-form-item>
        <el-form-item label="Webhook URL">
          <el-input v-model="form.webhookUrl" placeholder="https://example.com/hooks/aiops" />
        </el-form-item>
        <el-form-item label="Secret(可选)">
          <el-input v-model="form.secret" type="password" show-password placeholder="用于接收端鉴权" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="form.enabled" />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="flex items-center justify-end gap-2">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="saving" @click="saveChannel">保存</el-button>
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

async function loadChannels() {
  loading.value = true
  try {
    const { data } = await getNotificationChannels()
    channels.value = Array.isArray(data) ? data : []
  } catch (e) {
    ElMessage.error('加载通知通道失败')
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
    ElMessage.warning('请填写通道名称')
    return
  }
  if (!form.webhookUrl.trim()) {
    ElMessage.warning('请填写 Webhook URL')
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
    ElMessage.success('通知通道已保存')
    await loadChannels()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function toggleEnabled(row, enabled) {
  const previous = row.enabled
  row.enabled = enabled
  try {
    await updateNotificationChannelEnabled(row.id, Boolean(enabled))
    ElMessage.success('状态已更新')
  } catch (e) {
    row.enabled = previous
    ElMessage.error('更新启用状态失败')
  }
}

async function removeChannel(row) {
  try {
    await ElMessageBox.confirm(
      `确认删除通知通道「${row.name}」吗？`,
      '删除确认',
      { type: 'warning' }
    )
    await deleteNotificationChannel(row.id)
    ElMessage.success('已删除')
    await loadChannels()
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') {
      ElMessage.error('删除失败')
    }
  }
}

function formatDate(value) {
  if (!value) return '-'
  return new Date(value).toLocaleString('zh-CN')
}

onMounted(loadChannels)
</script>
