<template>
  <div class="page-surface max-w-5xl">
    <div class="page-hero">
      <div>
        <h1>{{ t('title') }}</h1>
        <p>{{ t('subtitle') }}</p>
      </div>
      <el-button @click="reload" :loading="loading">{{ t('refresh') }}</el-button>
    </div>

    <div class="card-panel-strong p-6">
      <el-form :model="form" label-width="200px" class="space-y-4">
        <el-form-item :label="t('cpuThreshold')">
          <div class="w-full flex items-center gap-4">
            <el-slider v-model="form.cpuThreshold" :min="1" :max="100" class="flex-1" />
            <el-input-number v-model="form.cpuThreshold" :min="1" :max="100" />
          </div>
        </el-form-item>

        <el-form-item :label="t('memoryThreshold')">
          <div class="w-full flex items-center gap-4">
            <el-slider v-model="form.memoryThreshold" :min="1" :max="100" class="flex-1" />
            <el-input-number v-model="form.memoryThreshold" :min="1" :max="100" />
          </div>
        </el-form-item>

        <el-form-item :label="t('diskThreshold')">
          <div class="w-full flex items-center gap-4">
            <el-slider v-model="form.diskThreshold" :min="1" :max="100" class="flex-1" />
            <el-input-number v-model="form.diskThreshold" :min="1" :max="100" />
          </div>
        </el-form-item>

        <el-form-item :label="t('processThreshold')">
          <div class="w-full flex items-center gap-4">
            <el-slider v-model="form.processCountThreshold" :min="1" :max="2000" class="flex-1" />
            <el-input-number v-model="form.processCountThreshold" :min="1" :max="2000" />
          </div>
        </el-form-item>

        <el-form-item :label="t('consecutiveBreach')">
          <div class="w-full flex items-center gap-4">
            <el-slider v-model="form.consecutiveBreachCount" :min="1" :max="10" class="flex-1" />
            <el-input-number v-model="form.consecutiveBreachCount" :min="1" :max="10" />
          </div>
        </el-form-item>

        <el-form-item :label="t('silenceSeconds')">
          <div class="w-full flex items-center gap-4">
            <el-slider v-model="form.silenceSeconds" :min="10" :max="3600" :step="10" class="flex-1" />
            <el-input-number v-model="form.silenceSeconds" :min="10" :max="3600" :step="10" />
          </div>
        </el-form-item>
      </el-form>
    </div>

    <div class="flex items-center justify-between">
      <p class="text-xs text-slate-500">{{ t('updatedAt') }}：{{ formatDate(updatedAt) }}</p>
      <el-button type="primary" :disabled="!canOperate" :loading="saving" @click="save">{{ t('save') }}</el-button>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getThresholdSettings, updateThresholdSettings } from '../api/settings'
import { useI18n } from '../composables/useI18n'
import { usePermissions } from '../composables/usePermissions'

const loading = ref(false)
const saving = ref(false)
const updatedAt = ref(null)
const { canOperate, readOnlyReason } = usePermissions()

const form = reactive({
  cpuThreshold: 70,
  memoryThreshold: 90,
  diskThreshold: 85,
  processCountThreshold: 400,
  consecutiveBreachCount: 2,
  silenceSeconds: 180
})

const { locale, t } = useI18n({
  title: { zh: '告警阈值配置', en: 'Threshold Configuration' },
  subtitle: { zh: '按当前用户生效，支持连续触发与静默窗口降噪', en: 'User-level thresholds with consecutive trigger and silence-window noise control' },
  refresh: { zh: '刷新', en: 'Refresh' },
  cpuThreshold: { zh: 'CPU 阈值 (%)', en: 'CPU Threshold (%)' },
  memoryThreshold: { zh: '内存阈值 (%)', en: 'Memory Threshold (%)' },
  diskThreshold: { zh: '磁盘阈值 (%)', en: 'Disk Threshold (%)' },
  processThreshold: { zh: '进程数量阈值', en: 'Process Count Threshold' },
  consecutiveBreach: { zh: '连续超阈值次数', en: 'Consecutive Breach Count' },
  silenceSeconds: { zh: '静默窗口 (秒)', en: 'Silence Window (sec)' },
  updatedAt: { zh: '最近更新时间', en: 'Last Updated' },
  save: { zh: '保存配置', en: 'Save Settings' },
  loadFailed: { zh: '加载阈值配置失败', en: 'Failed to load threshold settings' },
  saveSuccess: { zh: '阈值配置已保存', en: 'Threshold settings saved' },
  saveFailed: { zh: '保存失败', en: 'Save failed' }
})

async function reload() {
  loading.value = true
  try {
    const { data } = await getThresholdSettings()
    form.cpuThreshold = Number(data.cpuThreshold || 70)
    form.memoryThreshold = Number(data.memoryThreshold || 90)
    form.diskThreshold = Number(data.diskThreshold || 85)
    form.processCountThreshold = Number(data.processCountThreshold || 400)
    form.consecutiveBreachCount = Number(data.consecutiveBreachCount || 2)
    form.silenceSeconds = Number(data.silenceSeconds || 180)
    updatedAt.value = data.updatedAt || null
  } catch (_e) {
    ElMessage.error(t('loadFailed'))
  } finally {
    loading.value = false
  }
}

async function save() {
  if (!canOperate.value) {
    ElMessage.warning(readOnlyReason.value)
    return
  }
  saving.value = true
  try {
    const payload = {
      cpuThreshold: Number(form.cpuThreshold),
      memoryThreshold: Number(form.memoryThreshold),
      diskThreshold: Number(form.diskThreshold),
      processCountThreshold: Number(form.processCountThreshold),
      consecutiveBreachCount: Number(form.consecutiveBreachCount),
      silenceSeconds: Number(form.silenceSeconds)
    }
    const { data } = await updateThresholdSettings(payload)
    updatedAt.value = data.updatedAt || null
    ElMessage.success(t('saveSuccess'))
  } catch (_e) {
    ElMessage.error(t('saveFailed'))
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
