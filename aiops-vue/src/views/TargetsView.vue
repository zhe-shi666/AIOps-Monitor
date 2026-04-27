<template>
  <div class="min-h-screen bg-slate-950 text-slate-100 p-8 pt-24">
    <div class="max-w-7xl mx-auto space-y-6">
      <div class="flex items-center justify-between">
        <div>
          <h1 class="text-2xl font-bold tracking-tight">监控目标管理</h1>
          <p class="text-sm text-slate-400 mt-1">创建和维护 Agent 采集目标，管理 key 与状态</p>
        </div>
        <el-button type="primary" @click="openCreateDialog">新增目标</el-button>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div class="rounded-xl border border-slate-700 bg-slate-900/60 p-4">
          <p class="text-xs text-slate-400">目标总数</p>
          <p class="text-2xl font-semibold mt-1">{{ targets.length }}</p>
        </div>
        <div class="rounded-xl border border-slate-700 bg-slate-900/60 p-4">
          <p class="text-xs text-slate-400">在线目标</p>
          <p class="text-2xl font-semibold mt-1 text-emerald-400">{{ onlineCount }}</p>
        </div>
        <div class="rounded-xl border border-slate-700 bg-slate-900/60 p-4">
          <p class="text-xs text-slate-400">已禁用目标</p>
          <p class="text-2xl font-semibold mt-1 text-orange-400">{{ disabledCount }}</p>
        </div>
      </div>

      <div class="rounded-xl border border-slate-700 bg-slate-900/50 p-4">
        <el-table
          :data="targets"
          v-loading="loading"
          :header-cell-style="{ background: '#1e293b', color: '#94a3b8' }"
          :row-style="{ background: '#0f172a', color: '#e2e8f0' }">
          <el-table-column prop="name" label="目标名称" min-width="180" />
          <el-table-column prop="hostname" label="主机名" min-width="140" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 'ONLINE' ? 'success' : (row.status === 'DISABLED' ? 'info' : 'warning')" size="small">
                {{ row.status || 'OFFLINE' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="启用" width="90">
            <template #default="{ row }">
              <el-switch v-model="row.enabled" @change="toggleEnabled(row)" />
            </template>
          </el-table-column>
          <el-table-column prop="ipAddress" label="IP" min-width="130" />
          <el-table-column prop="agentVersion" label="Agent版本" min-width="120" />
          <el-table-column label="最近心跳" min-width="170">
            <template #default="{ row }">{{ formatDate(row.lastHeartbeatAt) }}</template>
          </el-table-column>
          <el-table-column label="Agent Key" min-width="170">
            <template #default="{ row }">
              <div class="flex items-center gap-2">
                <span class="font-mono text-xs text-cyan-300">{{ row.agentKeyPreview || '****' }}</span>
                <el-button link type="primary" @click="copyKey(row.agentKey)">复制</el-button>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="260" fixed="right">
            <template #default="{ row }">
              <div class="flex items-center gap-2">
                <el-button size="small" @click="openEditDialog(row)">编辑</el-button>
                <el-button size="small" type="warning" @click="rotateKey(row)">轮换Key</el-button>
                <el-popconfirm title="确认删除该目标？" @confirm="removeTarget(row.id)">
                  <template #reference>
                    <el-button size="small" type="danger">删除</el-button>
                  </template>
                </el-popconfirm>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑监控目标' : '新增监控目标'" width="520px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="90px">
        <el-form-item label="目标名称" prop="name">
          <el-input v-model="form.name" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="主机名">
          <el-input v-model="form.hostname" maxlength="255" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" maxlength="255" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitForm">{{ isEdit ? '保存' : '创建' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createTarget, deleteTarget, getTargets, rotateTargetKey, updateTarget } from '../api/targets'

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

const rules = {
  name: [{ required: true, message: '请输入目标名称', trigger: 'blur' }]
}

const onlineCount = computed(() => targets.value.filter(t => t.status === 'ONLINE').length)
const disabledCount = computed(() => targets.value.filter(t => !t.enabled).length)

async function fetchTargets() {
  loading.value = true
  try {
    const { data } = await getTargets()
    targets.value = Array.isArray(data) ? data : []
  } catch (e) {
    ElMessage.error('加载目标列表失败')
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
      ElMessage.success('目标已更新')
    } else {
      await createTarget({
        name: form.name,
        hostname: form.hostname,
        description: form.description
      })
      ElMessage.success('目标已创建')
    }
    dialogVisible.value = false
    await fetchTargets()
  } catch (e) {
    ElMessage.error(isEdit.value ? '更新失败' : '创建失败')
  } finally {
    saving.value = false
  }
}

async function toggleEnabled(row) {
  const value = row.enabled
  try {
    await updateTarget(row.id, { enabled: value })
    row.status = value ? (row.status === 'ONLINE' ? 'ONLINE' : 'OFFLINE') : 'DISABLED'
    ElMessage.success(value ? '目标已启用' : '目标已禁用')
  } catch (e) {
    row.enabled = !value
    ElMessage.error('状态更新失败')
  }
}

async function rotateKey(row) {
  try {
    await ElMessageBox.confirm(
      `轮换后旧 key 立即失效，目标「${row.name}」上的 Agent 需要同步更新。是否继续？`,
      '确认轮换',
      { type: 'warning', confirmButtonText: '继续', cancelButtonText: '取消' }
    )
    const { data } = await rotateTargetKey(row.id)
    await fetchTargets()
    await copyKey(data.agentKey)
    ElMessage.success('Key 已轮换并复制到剪贴板')
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('轮换失败')
    }
  }
}

async function removeTarget(id) {
  try {
    await deleteTarget(id)
    ElMessage.success('目标已删除')
    await fetchTargets()
  } catch (e) {
    ElMessage.error('删除失败')
  }
}

async function copyKey(agentKey) {
  if (!agentKey) {
    ElMessage.warning('当前没有可复制的 key')
    return
  }
  try {
    await navigator.clipboard.writeText(agentKey)
    ElMessage.success('已复制')
  } catch (e) {
    ElMessage.error('复制失败，请手动复制')
  }
}

function formatDate(value) {
  if (!value) return '-'
  return new Date(value).toLocaleString('zh-CN')
}

onMounted(fetchTargets)
</script>
