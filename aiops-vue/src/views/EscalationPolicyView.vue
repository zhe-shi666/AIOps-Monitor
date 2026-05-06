<template>
  <div class="page-surface escalation-page max-w-6xl">
    <div class="page-hero escalation-hero">
      <div>
        <h1>{{ t('title') }}</h1>
        <p>{{ t('subtitle') }}</p>
      </div>
      <div class="inline-actions">
        <el-button @click="reload" :loading="loading">{{ t('refresh') }}</el-button>
      </div>
    </div>

    <section class="kpi-grid escalation-kpi-grid">
      <div class="kpi-item escalation-kpi-card">
        <p class="kpi-label">{{ t('summaryP1') }}</p>
        <p class="kpi-value text-rose-300">{{ form.p1Intervals }}</p>
      </div>
      <div class="kpi-item escalation-kpi-card">
        <p class="kpi-label">{{ t('summaryP2') }}</p>
        <p class="kpi-value text-amber-300">{{ form.p2Intervals }}</p>
      </div>
      <div class="kpi-item escalation-kpi-card">
        <p class="kpi-label">{{ t('summaryP3') }}</p>
        <p class="kpi-value text-cyan-300">{{ form.p3Intervals }}</p>
      </div>
    </section>

    <section class="card-panel escalation-main-card">
      <div class="section-head escalation-head">
        <div>
          <h2 class="section-title">{{ t('workspaceTitle') }}</h2>
          <p class="section-subtitle">{{ t('workspaceSubtitle') }}</p>
        </div>
        <div v-if="!canOperate" class="readonly-tip">{{ readOnlyReason }}</div>
      </div>

      <div class="escalation-layout">
        <div class="escalation-form-panel">
          <div class="strategy-block">
            <div class="strategy-head">
              <h3>{{ t('cadenceTitle') }}</h3>
              <p>{{ t('cadenceDesc') }}</p>
            </div>

            <div class="escalation-level-list">
              <div class="escalation-level-card danger-tone-soft">
                <div class="level-meta">
                  <strong>{{ t('p1') }}</strong>
                  <span>{{ t('p1Hint') }}</span>
                </div>
                <el-input v-model="form.p1Intervals" placeholder="1,3,5,10" />
              </div>

              <div class="escalation-level-card warning-tone-soft">
                <div class="level-meta">
                  <strong>{{ t('p2') }}</strong>
                  <span>{{ t('p2Hint') }}</span>
                </div>
                <el-input v-model="form.p2Intervals" placeholder="5,15,30" />
              </div>

              <div class="escalation-level-card info-tone-soft">
                <div class="level-meta">
                  <strong>{{ t('p3') }}</strong>
                  <span>{{ t('p3Hint') }}</span>
                </div>
                <el-input v-model="form.p3Intervals" placeholder="15,30,60" />
              </div>
            </div>
          </div>
        </div>

        <aside class="escalation-side-panel">
          <div class="note-box strategy-note-card">
            <strong>{{ t('noteTitle') }}</strong>
            <p>{{ t('note1') }}</p>
            <p>{{ t('note2') }}</p>
          </div>

          <div class="note-box strategy-note-card">
            <strong>{{ t('formatTitle') }}</strong>
            <p>{{ t('formatDesc') }}</p>
          </div>

          <div class="note-box strategy-note-card">
            <strong>{{ t('updatedAt') }}</strong>
            <p>{{ formatDate(updatedAt) }}</p>
          </div>
        </aside>
      </div>

      <div class="escalation-footer-actions">
        <el-button type="primary" :disabled="!canOperate" :loading="saving" @click="save">{{ t('save') }}</el-button>
      </div>
    </section>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getEscalationPolicy, updateEscalationPolicy } from '../api/settings'
import { useI18n } from '../composables/useI18n'
import { usePermissions } from '../composables/usePermissions'

const loading = ref(false)
const saving = ref(false)
const updatedAt = ref(null)
const { canOperate, readOnlyReason } = usePermissions()

const form = reactive({
  p1Intervals: '1,3,5,10',
  p2Intervals: '5,15,30',
  p3Intervals: '15,30,60'
})

const { locale, t } = useI18n({
  title: { zh: '升级策略', en: 'Escalation Policy' },
  subtitle: { zh: '按严重级别定义重复通知节奏，控制值班升级链路。', en: 'Define repeat notification cadence by severity to control escalation workflows.' },
  refresh: { zh: '刷新', en: 'Refresh' },
  workspaceTitle: { zh: '策略工作区', en: 'Strategy Workspace' },
  workspaceSubtitle: { zh: '配置不同级别事件的通知间隔，统一升级节奏。', en: 'Configure notification intervals for each severity level in one place.' },
  cadenceTitle: { zh: '通知节奏', en: 'Notification Cadence' },
  cadenceDesc: { zh: '分钟列表按顺序执行，适用于短信、邮件、Webhook 等升级链路。', en: 'Minute lists are executed in order and apply to email, webhook, and other escalation channels.' },
  p1: { zh: 'P1 通知节奏', en: 'P1 Intervals' },
  p2: { zh: 'P2 通知节奏', en: 'P2 Intervals' },
  p3: { zh: 'P3 通知节奏', en: 'P3 Intervals' },
  p1Hint: { zh: '适合核心业务中断、需快速重复提醒。', en: 'Suitable for critical outages that require fast repeated alerts.' },
  p2Hint: { zh: '适合重要但可控的问题，保持中速升级。', en: 'Fits important but controllable incidents with moderate escalation.' },
  p3Hint: { zh: '适合低优先级问题，避免过度打扰值班人员。', en: 'For lower-priority incidents to avoid excessive operator noise.' },
  summaryP1: { zh: 'P1 节奏', en: 'P1 Cadence' },
  summaryP2: { zh: 'P2 节奏', en: 'P2 Cadence' },
  summaryP3: { zh: 'P3 节奏', en: 'P3 Cadence' },
  noteTitle: { zh: '升级说明', en: 'Escalation Notes' },
  note1: { zh: '示例：`1,3,5` 表示首次告警后，1 分钟、3 分钟、5 分钟各追加通知一次。', en: 'Example: `1,3,5` means additional notifications at 1, 3, and 5 minutes after the first alert.' },
  note2: { zh: '仅状态为 OPEN 的事件会继续升级通知，ACKNOWLEDGED/RESOLVED 会停止升级。', en: 'Only OPEN incidents continue escalating; ACKNOWLEDGED and RESOLVED incidents stop escalation.' },
  formatTitle: { zh: '输入格式', en: 'Input Format' },
  formatDesc: { zh: '请使用半角逗号分隔正整数，例如 `5,15,30`。', en: 'Use comma-separated positive integers such as `5,15,30`.' },
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
  if (!canOperate.value) {
    ElMessage.warning(readOnlyReason.value)
    return
  }
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

<style scoped>
.escalation-page {
  gap: 20px;
}

.escalation-hero {
  background: radial-gradient(760px 220px at 0 0, rgba(251, 113, 133, 0.12), transparent 60%), var(--hero-bg);
}

.escalation-kpi-grid {
  gap: 14px;
}

.escalation-kpi-card {
  min-height: 112px;
}

.escalation-main-card {
  padding: 20px;
}

.escalation-head {
  margin-bottom: 18px;
}

.escalation-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(280px, 0.48fr);
  gap: 18px;
}

.escalation-form-panel {
  display: grid;
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

.escalation-level-list {
  display: grid;
  gap: 12px;
}

.escalation-level-card {
  padding: 14px 16px;
  border-radius: 16px;
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.02);
}

.level-meta {
  margin-bottom: 12px;
}

.level-meta strong {
  display: block;
  color: var(--text-1);
  font-size: 14px;
}

.level-meta span {
  display: block;
  margin-top: 4px;
  color: var(--text-3);
  font-size: 12px;
  line-height: 1.55;
}

.escalation-side-panel {
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

.escalation-footer-actions {
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
  .escalation-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .escalation-main-card,
  .strategy-block {
    padding: 16px;
  }
}
</style>
