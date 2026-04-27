<template>
  <div class="min-h-screen bg-slate-950 text-slate-100 p-8 pt-24">
    <div class="max-w-4xl mx-auto space-y-6">
      <div class="flex items-center justify-between">
        <div>
          <h1 class="text-2xl font-bold tracking-tight">告警升级策略</h1>
          <p class="text-sm text-slate-400 mt-1">按严重级别设置通知节奏（分钟，逗号分隔）</p>
        </div>
        <el-button @click="reload" :loading="loading">刷新</el-button>
      </div>

      <div class="rounded-2xl border border-slate-700 bg-slate-900/50 p-6 space-y-4">
        <el-form :model="form" label-width="180px">
          <el-form-item label="P1 通知节奏">
            <el-input v-model="form.p1Intervals" placeholder="1,3,5,10" />
          </el-form-item>
          <el-form-item label="P2 通知节奏">
            <el-input v-model="form.p2Intervals" placeholder="5,15,30" />
          </el-form-item>
          <el-form-item label="P3 通知节奏">
            <el-input v-model="form.p3Intervals" placeholder="15,30,60" />
          </el-form-item>
        </el-form>

        <div class="rounded-xl border border-slate-700 bg-slate-950/40 p-4 text-xs text-slate-400 space-y-1">
          <p>示例：`1,3,5` 表示首次告警后，1 分钟、3 分钟、5 分钟各追加通知一次。</p>
          <p>仅状态为 OPEN 的事件会继续升级通知，ACKNOWLEDGED/RESOLVED 会停止升级。</p>
        </div>
      </div>

      <div class="flex items-center justify-between">
        <p class="text-xs text-slate-500">最近更新时间：{{ formatDate(updatedAt) }}</p>
        <el-button type="primary" :loading="saving" @click="save">保存策略</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getEscalationPolicy, updateEscalationPolicy } from '../api/settings'

const loading = ref(false)
const saving = ref(false)
const updatedAt = ref(null)

const form = reactive({
  p1Intervals: '1,3,5,10',
  p2Intervals: '5,15,30',
  p3Intervals: '15,30,60'
})

async function reload() {
  loading.value = true
  try {
    const { data } = await getEscalationPolicy()
    form.p1Intervals = data.p1Intervals || '1,3,5,10'
    form.p2Intervals = data.p2Intervals || '5,15,30'
    form.p3Intervals = data.p3Intervals || '15,30,60'
    updatedAt.value = data.updatedAt || null
  } catch (_e) {
    ElMessage.error('加载升级策略失败')
  } finally {
    loading.value = false
  }
}

function normalizeCsv(input) {
  return String(input || '').replace(/\s+/g, '').replace(/，/g, ',')
}

function validateCsv(input) {
  return /^\d+(,\d+)*$/.test(input)
}

async function save() {
  const p1 = normalizeCsv(form.p1Intervals)
  const p2 = normalizeCsv(form.p2Intervals)
  const p3 = normalizeCsv(form.p3Intervals)
  if (!validateCsv(p1) || !validateCsv(p2) || !validateCsv(p3)) {
    ElMessage.warning('请使用逗号分隔整数，例如 1,3,5')
    return
  }

  saving.value = true
  try {
    const payload = {
      p1Intervals: p1,
      p2Intervals: p2,
      p3Intervals: p3
    }
    const { data } = await updateEscalationPolicy(payload)
    form.p1Intervals = data.p1Intervals || p1
    form.p2Intervals = data.p2Intervals || p2
    form.p3Intervals = data.p3Intervals || p3
    updatedAt.value = data.updatedAt || null
    ElMessage.success('升级策略已保存')
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

function formatDate(value) {
  if (!value) return '-'
  return new Date(value).toLocaleString('zh-CN')
}

onMounted(reload)
</script>
