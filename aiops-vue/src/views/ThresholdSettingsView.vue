<template>
  <div class="min-h-screen bg-slate-950 text-slate-100 p-8 pt-24">
    <div class="max-w-4xl mx-auto space-y-6">
      <div class="flex items-center justify-between">
        <div>
          <h1 class="text-2xl font-bold tracking-tight">告警阈值配置</h1>
          <p class="text-sm text-slate-400 mt-1">按当前用户生效，支持连续触发与静默窗口降噪</p>
        </div>
        <el-button @click="reload" :loading="loading">刷新</el-button>
      </div>

      <div class="rounded-2xl border border-slate-700 bg-slate-900/50 p-6">
        <el-form :model="form" label-width="200px" class="space-y-4">
          <el-form-item label="CPU 阈值 (%)">
            <div class="w-full flex items-center gap-4">
              <el-slider v-model="form.cpuThreshold" :min="1" :max="100" class="flex-1" />
              <el-input-number v-model="form.cpuThreshold" :min="1" :max="100" />
            </div>
          </el-form-item>

          <el-form-item label="内存阈值 (%)">
            <div class="w-full flex items-center gap-4">
              <el-slider v-model="form.memoryThreshold" :min="1" :max="100" class="flex-1" />
              <el-input-number v-model="form.memoryThreshold" :min="1" :max="100" />
            </div>
          </el-form-item>

          <el-form-item label="磁盘阈值 (%)">
            <div class="w-full flex items-center gap-4">
              <el-slider v-model="form.diskThreshold" :min="1" :max="100" class="flex-1" />
              <el-input-number v-model="form.diskThreshold" :min="1" :max="100" />
            </div>
          </el-form-item>

          <el-form-item label="进程数量阈值">
            <div class="w-full flex items-center gap-4">
              <el-slider v-model="form.processCountThreshold" :min="1" :max="2000" class="flex-1" />
              <el-input-number v-model="form.processCountThreshold" :min="1" :max="2000" />
            </div>
          </el-form-item>

          <el-form-item label="连续超阈值次数">
            <div class="w-full flex items-center gap-4">
              <el-slider v-model="form.consecutiveBreachCount" :min="1" :max="10" class="flex-1" />
              <el-input-number v-model="form.consecutiveBreachCount" :min="1" :max="10" />
            </div>
          </el-form-item>

          <el-form-item label="静默窗口 (秒)">
            <div class="w-full flex items-center gap-4">
              <el-slider v-model="form.silenceSeconds" :min="10" :max="3600" :step="10" class="flex-1" />
              <el-input-number v-model="form.silenceSeconds" :min="10" :max="3600" :step="10" />
            </div>
          </el-form-item>
        </el-form>
      </div>

      <div class="flex items-center justify-between">
        <p class="text-xs text-slate-500">最近更新时间：{{ formatDate(updatedAt) }}</p>
        <el-button type="primary" :loading="saving" @click="save">保存配置</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getThresholdSettings, updateThresholdSettings } from '../api/settings'

const loading = ref(false)
const saving = ref(false)
const updatedAt = ref(null)

const form = reactive({
  cpuThreshold: 70,
  memoryThreshold: 90,
  diskThreshold: 85,
  processCountThreshold: 400,
  consecutiveBreachCount: 2,
  silenceSeconds: 180
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
  } catch (e) {
    ElMessage.error('加载阈值配置失败')
  } finally {
    loading.value = false
  }
}

async function save() {
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
    ElMessage.success('阈值配置已保存')
  } catch (e) {
    ElMessage.error('保存失败')
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
