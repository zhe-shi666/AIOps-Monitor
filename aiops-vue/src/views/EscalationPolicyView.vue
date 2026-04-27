<template>
  <div class="page-surface max-w-5xl">
    <div class="page-hero">
      <div>
        <h1>{{ t('title') }}</h1>
        <p>{{ t('subtitle') }}</p>
      </div>
      <el-button @click="reload" :loading="loading">{{ t('refresh') }}</el-button>
    </div>

    <div class="card-panel-strong p-6 space-y-4">
      <el-form :model="form" label-width="180px">
        <el-form-item :label="t('p1')">
          <el-input v-model="form.p1Intervals" placeholder="1,3,5,10" />
        </el-form-item>
        <el-form-item :label="t('p2')">
          <el-input v-model="form.p2Intervals" placeholder="5,15,30" />
        </el-form-item>
        <el-form-item :label="t('p3')">
          <el-input v-model="form.p3Intervals" placeholder="15,30,60" />
        </el-form-item>
      </el-form>

      <div class="note-box space-y-1">
        <p>{{ t('note1') }}</p>
        <p>{{ t('note2') }}</p>
      </div>
    </div>

    <div class="flex items-center justify-between">
      <p class="text-xs text-slate-500">{{ t('updatedAt') }}：{{ formatDate(updatedAt) }}</p>
      <el-button type="primary" :loading="saving" @click="save">{{ t('save') }}</el-button>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getEscalationPolicy, updateEscalationPolicy } from '../api/settings'
import { useI18n } from '../composables/useI18n'

const loading = ref(false)
const saving = ref(false)
const updatedAt = ref(null)

const form = reactive({
  p1Intervals: '1,3,5,10',
  p2Intervals: '5,15,30',
  p3Intervals: '15,30,60'
})

const { locale, t } = useI18n({
  title: { zh: '告警升级策略', en: 'Escalation Policy' },
  subtitle: { zh: '按严重级别设置通知节奏（分钟，逗号分隔）', en: 'Configure notification cadence by severity (minutes, comma-separated)' },
  refresh: { zh: '刷新', en: 'Refresh' },
  p1: { zh: 'P1 通知节奏', en: 'P1 Intervals' },
  p2: { zh: 'P2 通知节奏', en: 'P2 Intervals' },
  p3: { zh: 'P3 通知节奏', en: 'P3 Intervals' },
  note1: { zh: '示例：`1,3,5` 表示首次告警后，1 分钟、3 分钟、5 分钟各追加通知一次。', en: 'Example: `1,3,5` means additional notifications at 1, 3 and 5 minutes after first alert.' },
  note2: { zh: '仅状态为 OPEN 的事件会继续升级通知，ACKNOWLEDGED/RESOLVED 会停止升级。', en: 'Only OPEN incidents continue escalating; ACKNOWLEDGED/RESOLVED stop escalation.' },
  updatedAt: { zh: '最近更新时间', en: 'Last Updated' },
  save: { zh: '保存策略', en: 'Save Policy' },
  loadFailed: { zh: '加载升级策略失败', en: 'Failed to load escalation policy' },
  invalidCsv: { zh: '请使用逗号分隔整数，例如 1,3,5', en: 'Use comma-separated integers, e.g. 1,3,5' },
  saveSuccess: { zh: '升级策略已保存', en: 'Escalation policy saved' },
  saveFailed: { zh: '保存失败', en: 'Save failed' }
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
    ElMessage.error(t('loadFailed'))
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
    ElMessage.warning(t('invalidCsv'))
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
    ElMessage.success(t('saveSuccess'))
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || t('saveFailed'))
  } finally {
    saving.value = false
  }
}

function formatDate(value) {
  if (!value) return '-'
  const localeCode = locale.value === 'zh' ? 'zh-CN' : 'en-US'
  return new Date(value).toLocaleString(localeCode)
}

onMounted(reload)
</script>
