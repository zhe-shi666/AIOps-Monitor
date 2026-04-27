<template>
  <div class="page-surface">
    <div class="page-hero">
      <div>
        <h1>{{ t('title') }}</h1>
        <p>{{ t('subtitle') }}</p>
      </div>
      <el-button type="primary" @click="openCreateDialog">{{ t('create') }}</el-button>
    </div>

    <div class="kpi-grid">
      <div class="kpi-item">
        <p class="kpi-label">{{ t('totalTargets') }}</p>
        <p class="kpi-value">{{ targets.length }}</p>
      </div>
      <div class="kpi-item">
        <p class="kpi-label">{{ t('onlineTargets') }}</p>
        <p class="kpi-value text-emerald-300">{{ onlineCount }}</p>
      </div>
      <div class="kpi-item">
        <p class="kpi-label">{{ t('disabledTargets') }}</p>
        <p class="kpi-value text-amber-300">{{ disabledCount }}</p>
      </div>
    </div>

    <div class="card-panel table-wrap ops-table">
      <el-table :data="targets" v-loading="loading">
        <el-table-column prop="name" :label="t('targetName')" min-width="180" />
        <el-table-column prop="hostname" :label="t('hostname')" min-width="140" />
        <el-table-column :label="t('status')" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ONLINE' ? 'success' : (row.status === 'DISABLED' ? 'info' : 'warning')" size="small">
              {{ row.status || 'OFFLINE' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('enabled')" width="90">
          <template #default="{ row }">
            <el-switch v-model="row.enabled" @change="toggleEnabled(row)" />
          </template>
        </el-table-column>
        <el-table-column prop="ipAddress" label="IP" min-width="130" />
        <el-table-column prop="agentVersion" :label="t('agentVersion')" min-width="120" />
        <el-table-column :label="t('lastHeartbeat')" min-width="170">
          <template #default="{ row }">{{ formatDate(row.lastHeartbeatAt) }}</template>
        </el-table-column>
        <el-table-column label="Agent Key" min-width="170">
          <template #default="{ row }">
            <div class="flex items-center gap-2">
              <span class="font-mono text-xs text-cyan-300">{{ row.agentKeyPreview || '****' }}</span>
              <el-button link type="primary" @click="copyKey(row.agentKey)">{{ t('copy') }}</el-button>
            </div>
          </template>
        </el-table-column>
        <el-table-column :label="t('actions')" width="260" fixed="right">
          <template #default="{ row }">
            <div class="flex items-center gap-2">
              <el-button size="small" @click="openEditDialog(row)">{{ t('edit') }}</el-button>
              <el-button size="small" type="warning" @click="rotateKey(row)">{{ t('rotateKey') }}</el-button>
              <el-popconfirm :title="t('deleteConfirmTarget')" @confirm="removeTarget(row.id)">
                <template #reference>
                  <el-button size="small" type="danger">{{ t('delete') }}</el-button>
                </template>
              </el-popconfirm>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="isEdit ? t('editDialog') : t('createDialog')" width="520px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="90px">
        <el-form-item :label="t('targetName')" prop="name">
          <el-input v-model="form.name" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item :label="t('hostname')">
          <el-input v-model="form.hostname" maxlength="255" />
        </el-form-item>
        <el-form-item :label="t('description')">
          <el-input v-model="form.description" type="textarea" :rows="3" maxlength="255" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ t('cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="submitForm">{{ isEdit ? t('save') : t('createAction') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createTarget, deleteTarget, getTargets, rotateTargetKey, updateTarget } from '../api/targets'
import { useI18n } from '../composables/useI18n'

const loading = ref(false)
const saving = ref(false)
const targets = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const editingId = ref(null)
const formRef = ref(null)

const form = reactive({
  name: '',
  hostname: '',
  description: ''
})

const { locale, t } = useI18n({
  title: { zh: '监控目标管理', en: 'Target Management' },
  subtitle: { zh: '创建和维护 Agent 采集目标，管理 key 与状态', en: 'Create and maintain agent targets, keys and statuses' },
  create: { zh: '新增目标', en: 'New Target' },
  totalTargets: { zh: '目标总数', en: 'Total Targets' },
  onlineTargets: { zh: '在线目标', en: 'Online Targets' },
  disabledTargets: { zh: '已禁用目标', en: 'Disabled Targets' },
  targetName: { zh: '目标名称', en: 'Target Name' },
  hostname: { zh: '主机名', en: 'Hostname' },
  status: { zh: '状态', en: 'Status' },
  enabled: { zh: '启用', en: 'Enabled' },
  agentVersion: { zh: 'Agent版本', en: 'Agent Version' },
  lastHeartbeat: { zh: '最近心跳', en: 'Last Heartbeat' },
  copy: { zh: '复制', en: 'Copy' },
  actions: { zh: '操作', en: 'Actions' },
  edit: { zh: '编辑', en: 'Edit' },
  rotateKey: { zh: '轮换Key', en: 'Rotate Key' },
  delete: { zh: '删除', en: 'Delete' },
  deleteConfirmTarget: { zh: '确认删除该目标？', en: 'Delete this target?' },
  editDialog: { zh: '编辑监控目标', en: 'Edit Target' },
  createDialog: { zh: '新增监控目标', en: 'Create Target' },
  description: { zh: '描述', en: 'Description' },
  cancel: { zh: '取消', en: 'Cancel' },
  save: { zh: '保存', en: 'Save' },
  createAction: { zh: '创建', en: 'Create' },
  targetNameRequired: { zh: '请输入目标名称', en: 'Please enter target name' },
  loadFailed: { zh: '加载目标列表失败', en: 'Failed to load target list' },
  updated: { zh: '目标已更新', en: 'Target updated' },
  created: { zh: '目标已创建', en: 'Target created' },
  updateFailed: { zh: '更新失败', en: 'Update failed' },
  createFailed: { zh: '创建失败', en: 'Create failed' },
  enabledSuccess: { zh: '目标已启用', en: 'Target enabled' },
  disabledSuccess: { zh: '目标已禁用', en: 'Target disabled' },
  statusUpdateFailed: { zh: '状态更新失败', en: 'Failed to update status' },
  rotateConfirm: { zh: '轮换后旧 key 立即失效，目标「{name}」上的 Agent 需要同步更新。是否继续？', en: 'After rotation, old key is invalid immediately. Update agent on target "{name}". Continue?' },
  rotateTitle: { zh: '确认轮换', en: 'Confirm Rotation' },
  rotateContinue: { zh: '继续', en: 'Continue' },
  rotateCancel: { zh: '取消', en: 'Cancel' },
  keyRotated: { zh: 'Key 已轮换并复制到剪贴板', en: 'Key rotated and copied to clipboard' },
  rotateFailed: { zh: '轮换失败', en: 'Rotation failed' },
  deleted: { zh: '目标已删除', en: 'Target deleted' },
  deleteFailed: { zh: '删除失败', en: 'Delete failed' },
  noKey: { zh: '当前没有可复制的 key', en: 'No key available to copy' },
  copied: { zh: '已复制', en: 'Copied' },
  copyFailed: { zh: '复制失败，请手动复制', en: 'Copy failed, please copy manually' }
})

const rules = computed(() => ({
  name: [{ required: true, message: t('targetNameRequired'), trigger: 'blur' }]
}))

const onlineCount = computed(() => targets.value.filter(item => item.status === 'ONLINE').length)
const disabledCount = computed(() => targets.value.filter(item => !item.enabled).length)

async function fetchTargets() {
  loading.value = true
  try {
    const { data } = await getTargets()
    targets.value = Array.isArray(data) ? data : []
  } catch (_e) {
    ElMessage.error(t('loadFailed'))
  } finally {
    loading.value = false
  }
}

function resetForm() {
  form.name = ''
  form.hostname = ''
  form.description = ''
  editingId.value = null
  isEdit.value = false
}

function openCreateDialog() {
  resetForm()
  dialogVisible.value = true
}

function openEditDialog(row) {
  isEdit.value = true
  editingId.value = row.id
  form.name = row.name || ''
  form.hostname = row.hostname || ''
  form.description = row.description || ''
  dialogVisible.value = true
}

async function submitForm() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  saving.value = true
  try {
    if (isEdit.value && editingId.value) {
      await updateTarget(editingId.value, {
        name: form.name,
        hostname: form.hostname,
        description: form.description
      })
      ElMessage.success(t('updated'))
    } else {
      await createTarget({
        name: form.name,
        hostname: form.hostname,
        description: form.description
      })
      ElMessage.success(t('created'))
    }
    dialogVisible.value = false
    await fetchTargets()
  } catch (_e) {
    ElMessage.error(isEdit.value ? t('updateFailed') : t('createFailed'))
  } finally {
    saving.value = false
  }
}

async function toggleEnabled(row) {
  const value = row.enabled
  try {
    await updateTarget(row.id, { enabled: value })
    row.status = value ? (row.status === 'ONLINE' ? 'ONLINE' : 'OFFLINE') : 'DISABLED'
    ElMessage.success(value ? t('enabledSuccess') : t('disabledSuccess'))
  } catch (_e) {
    row.enabled = !value
    ElMessage.error(t('statusUpdateFailed'))
  }
}

async function rotateKey(row) {
  try {
    await ElMessageBox.confirm(
      t('rotateConfirm', { name: row.name }),
      t('rotateTitle'),
      { type: 'warning', confirmButtonText: t('rotateContinue'), cancelButtonText: t('rotateCancel') }
    )
    const { data } = await rotateTargetKey(row.id)
    await fetchTargets()
    await copyKey(data.agentKey)
    ElMessage.success(t('keyRotated'))
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(t('rotateFailed'))
    }
  }
}

async function removeTarget(id) {
  try {
    await deleteTarget(id)
    ElMessage.success(t('deleted'))
    await fetchTargets()
  } catch (_e) {
    ElMessage.error(t('deleteFailed'))
  }
}

async function copyKey(agentKey) {
  if (!agentKey) {
    ElMessage.warning(t('noKey'))
    return
  }
  try {
    await navigator.clipboard.writeText(agentKey)
    ElMessage.success(t('copied'))
  } catch (_e) {
    ElMessage.error(t('copyFailed'))
  }
}

function formatDate(value) {
  if (!value) return '-'
  const localeCode = locale.value === 'zh' ? 'zh-CN' : 'en-US'
  return new Date(value).toLocaleString(localeCode)
}

onMounted(fetchTargets)
</script>
