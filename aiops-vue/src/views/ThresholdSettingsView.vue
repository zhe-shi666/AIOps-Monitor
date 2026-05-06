<template>
  <div class="page-surface threshold-page max-w-6xl">
    <div class="page-hero threshold-hero">
      <div>
        <h1>{{ t('title') }}</h1>
        <p>{{ t('subtitle') }}</p>
      </div>
      <div class="inline-actions">
        <el-button @click="reload" :loading="loading">{{ t('refresh') }}</el-button>
      </div>
    </div>

    <section class="kpi-grid threshold-kpi-grid">
      <div class="kpi-item threshold-kpi-card">
        <p class="kpi-label">{{ t('summaryCpu') }}</p>
        <p class="kpi-value text-cyan-300">{{ form.cpuThreshold }}%</p>
      </div>
      <div class="kpi-item threshold-kpi-card">
        <p class="kpi-label">{{ t('summaryMemory') }}</p>
        <p class="kpi-value text-violet-300">{{ form.memoryThreshold }}%</p>
      </div>
      <div class="kpi-item threshold-kpi-card">
        <p class="kpi-label">{{ t('summaryNoise') }}</p>
        <p class="kpi-value text-amber-300">{{ form.consecutiveBreachCount }} / {{ form.silenceSeconds }}s</p>
      </div>
    </section>

    <section class="card-panel threshold-main-card">
      <div class="section-head threshold-head">
        <div>
          <h2 class="section-title">{{ t('workspaceTitle') }}</h2>
          <p class="section-subtitle">{{ t('workspaceSubtitle') }}</p>
        </div>
        <div v-if="!canOperate" class="readonly-tip">{{ readOnlyReason }}</div>
      </div>

      <div class="threshold-layout">
        <div class="threshold-form-panel">
          <div class="strategy-block">
            <div class="strategy-head">
              <h3>{{ t('resourceThresholds') }}</h3>
              <p>{{ t('resourceThresholdsDesc') }}</p>
            </div>

            <div class="threshold-item-list">
              <div class="threshold-item-card">
                <div class="threshold-item-meta">
                  <strong>{{ t('cpuThreshold') }}</strong>
                  <span>{{ t('cpuHint') }}</span>
                </div>
                <div class="threshold-item-control">
                  <el-slider v-model="form.cpuThreshold" :min="1" :max="100" class="flex-1" />
                  <el-input-number v-model="form.cpuThreshold" :min="1" :max="100" />
                </div>
              </div>

              <div class="threshold-item-card">
                <div class="threshold-item-meta">
                  <strong>{{ t('memoryThreshold') }}</strong>
                  <span>{{ t('memoryHint') }}</span>
                </div>
                <div class="threshold-item-control">
                  <el-slider v-model="form.memoryThreshold" :min="1" :max="100" class="flex-1" />
                  <el-input-number v-model="form.memoryThreshold" :min="1" :max="100" />
                </div>
              </div>

              <div class="threshold-item-card">
                <div class="threshold-item-meta">
                  <strong>{{ t('diskThreshold') }}</strong>
                  <span>{{ t('diskHint') }}</span>
                </div>
                <div class="threshold-item-control">
                  <el-slider v-model="form.diskThreshold" :min="1" :max="100" class="flex-1" />
                  <el-input-number v-model="form.diskThreshold" :min="1" :max="100" />
                </div>
              </div>

              <div class="threshold-item-card">
                <div class="threshold-item-meta">
                  <strong>{{ t('processThreshold') }}</strong>
                  <span>{{ t('processHint') }}</span>
                </div>
                <div class="threshold-item-control">
                  <el-slider v-model="form.processCountThreshold" :min="1" :max="2000" class="flex-1" />
                  <el-input-number v-model="form.processCountThreshold" :min="1" :max="2000" />
                </div>
              </div>
            </div>
          </div>

          <div class="strategy-block">
            <div class="strategy-head">
              <h3>{{ t('noiseControl') }}</h3>
              <p>{{ t('noiseControlDesc') }}</p>
            </div>

            <div class="threshold-item-list">
              <div class="threshold-item-card">
                <div class="threshold-item-meta">
                  <strong>{{ t('consecutiveBreach') }}</strong>
                  <span>{{ t('consecutiveHint') }}</span>
                </div>
                <div class="threshold-item-control">
                  <el-slider v-model="form.consecutiveBreachCount" :min="1" :max="10" class="flex-1" />
                  <el-input-number v-model="form.consecutiveBreachCount" :min="1" :max="10" />
                </div>
              </div>

              <div class="threshold-item-card">
                <div class="threshold-item-meta">
                  <strong>{{ t('silenceSeconds') }}</strong>
                  <span>{{ t('silenceHint') }}</span>
                </div>
                <div class="threshold-item-control">
                  <el-slider v-model="form.silenceSeconds" :min="10" :max="3600" :step="10" class="flex-1" />
                  <el-input-number v-model="form.silenceSeconds" :min="10" :max="3600" :step="10" />
                </div>
              </div>
            </div>
          </div>
        </div>

        <aside class="threshold-side-panel">
          <div class="note-box strategy-note-card">
            <strong>{{ t('noteTitle') }}</strong>
            <p>{{ t('note1') }}</p>
            <p>{{ t('note2') }}</p>
          </div>

          <div class="note-box strategy-note-card">
            <strong>{{ t('updatedAt') }}</strong>
            <p>{{ formatDate(updatedAt) }}</p>
          </div>
        </aside>
      </div>

      <div class="threshold-footer-actions">
        <el-button type="primary" :disabled="!canOperate" :loading="saving" @click="save">{{ t('save') }}</el-button>
      </div>
    </section>
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
  title: { zh: '阈值策略', en: 'Threshold Strategy' },
  subtitle: { zh: '统一配置资源告警阈值与降噪规则，避免事件泛滥。', en: 'Configure resource alert thresholds and noise reduction rules in one place.' },
  refresh: { zh: '刷新', en: 'Refresh' },
  workspaceTitle: { zh: '策略工作区', en: 'Strategy Workspace' },
  workspaceSubtitle: { zh: '左侧调整指标策略，右侧查看说明与当前更新时间。', en: 'Adjust metric policy on the left and review notes and update time on the right.' },
  resourceThresholds: { zh: '资源阈值', en: 'Resource Thresholds' },
  resourceThresholdsDesc: { zh: '定义系统资源触发告警的警戒线。', en: 'Define when system resources should trigger incidents.' },
  noiseControl: { zh: '降噪控制', en: 'Noise Control' },
  noiseControlDesc: { zh: '通过连续触发和静默窗口，减少重复告警。', en: 'Reduce repeated alerts with consecutive triggers and silence windows.' },
  cpuThreshold: { zh: 'CPU 阈值 (%)', en: 'CPU Threshold (%)' },
  memoryThreshold: { zh: '内存阈值 (%)', en: 'Memory Threshold (%)' },
  diskThreshold: { zh: '磁盘阈值 (%)', en: 'Disk Threshold (%)' },
  processThreshold: { zh: '进程数量阈值', en: 'Process Count Threshold' },
  consecutiveBreach: { zh: '连续超阈值次数', en: 'Consecutive Breach Count' },
  silenceSeconds: { zh: '静默窗口 (秒)', en: 'Silence Window (sec)' },
  cpuHint: { zh: '建议用于 CPU 长时间持续高负载场景。', en: 'Useful for persistent CPU saturation scenarios.' },
  memoryHint: { zh: '适合提前暴露内存逼近上限的问题。', en: 'Helps expose memory pressure before exhaustion.' },
  diskHint: { zh: '避免磁盘打满后才发现问题。', en: 'Avoid discovering issues only after disk exhaustion.' },
  processHint: { zh: '适用于进程泄漏、批量拉起或异常膨胀。', en: 'Suitable for process leaks, mass restarts, or abnormal growth.' },
  consecutiveHint: { zh: '连续多次超阈值后再触发，降低瞬时抖动误报。', en: 'Trigger only after repeated breaches to reduce transient noise.' },
  silenceHint: { zh: '首次触发后在此时间内抑制重复通知。', en: 'Suppress repeated notifications for this duration after the first alert.' },
  noteTitle: { zh: '策略说明', en: 'Policy Notes' },
  note1: { zh: '阈值越低，告警越敏感；阈值越高，越容易漏掉早期风险。', en: 'Lower thresholds increase sensitivity; higher thresholds may miss early risk.' },
  note2: { zh: '连续次数和静默窗口建议配合使用，避免同一异常短时间内重复刷屏。', en: 'Use consecutive counts with silence windows to avoid repeated alert storms.' },
  summaryCpu: { zh: 'CPU 警戒线', en: 'CPU Alert Line' },
  summaryMemory: { zh: '内存警戒线', en: 'Memory Alert Line' },
  summaryNoise: { zh: '降噪参数', en: 'Noise Control' },
  updatedAt: { zh: '最近更新时间', en: 'Last Updated' },
  save: { zh: '保存策略', en: 'Save Strategy' },
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

<style scoped>
.threshold-page {
  gap: 20px;
}

.threshold-hero {
  background: radial-gradient(760px 220px at 0 0, rgba(34, 197, 94, 0.12), transparent 60%), var(--hero-bg);
}

.threshold-kpi-grid {
  gap: 14px;
}

.threshold-kpi-card {
  min-height: 112px;
}

.threshold-main-card {
  padding: 20px;
}

.threshold-head {
  margin-bottom: 18px;
}

.threshold-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(280px, 0.45fr);
  gap: 18px;
}

.threshold-form-panel {
  display: grid;
  gap: 16px;
}

.strategy-block {
  border: 1px solid var(--line);
  border-radius: 18px;
  background: var(--panel-soft);
  padding: 18px;
}

.strategy-head {
  margin-bottom: 16px;
}

.strategy-head h3 {
  margin: 0;
  color: var(--text-1);
  font-size: 16px;
}

.strategy-head p {
  margin: 6px 0 0;
  color: var(--text-3);
  font-size: 12px;
}

.threshold-item-list {
  display: grid;
  gap: 12px;
}

.threshold-item-card {
  display: grid;
  gap: 12px;
  padding: 14px 16px;
  border-radius: 16px;
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.02);
}

.threshold-item-meta strong {
  display: block;
  color: var(--text-1);
  font-size: 14px;
}

.threshold-item-meta span {
  display: block;
  margin-top: 4px;
  color: var(--text-3);
  font-size: 12px;
  line-height: 1.55;
}

.threshold-item-control {
  display: flex;
  align-items: center;
  gap: 14px;
}

.threshold-side-panel {
  display: grid;
  gap: 14px;
  align-content: start;
}

.strategy-note-card strong {
  display: block;
  margin-bottom: 8px;
  color: var(--text-1);
}

.strategy-note-card p {
  margin: 0 0 8px;
  color: var(--text-3);
  line-height: 1.6;
}

.threshold-footer-actions {
  display: flex;
  justify-content: flex-end;
  padding-top: 18px;
}

.readonly-tip {
  max-width: 320px;
  padding: 10px 12px;
  border-radius: 12px;
  background: rgba(245, 158, 11, 0.08);
  border: 1px solid rgba(245, 158, 11, 0.2);
  color: var(--text-3);
  font-size: 12px;
}

@media (max-width: 980px) {
  .threshold-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .threshold-main-card,
  .strategy-block {
    padding: 16px;
  }

  .threshold-item-control {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
