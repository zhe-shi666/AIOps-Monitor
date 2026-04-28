<template>
  <div class="ai-expert-panel">
    <div class="ai-expert-head">
      <div>
        <h2>{{ locale === 'zh' ? 'AI 专家中心' : 'AI Expert Console' }}</h2>
        <p>{{ locale === 'zh' ? '调查、诊断与运维协作中枢' : 'Investigation, diagnosis, and ops collaboration hub' }}</p>
      </div>
      <span class="ai-connection" :class="{ online: wsConnected }">
        {{ wsConnected ? (locale === 'zh' ? '在线' : 'Online') : (locale === 'zh' ? '重连中' : 'Reconnecting') }}
      </span>
    </div>

    <div class="ai-note">
      {{ locale === 'zh'
        ? '右侧工作台支持调查对象、时间线与 AI 报告双视图。'
        : 'Right-side console supports investigations, timeline, and AI report dual views.'
      }}
    </div>

    <div class="ai-toolbar">
      <div class="toolbar-tabs">
        <button class="tab-btn" :class="{ active: activeTab === 'investigations' }" @click="activeTab = 'investigations'">
          {{ locale === 'zh' ? '调查视图' : 'Investigations' }}
        </button>
        <button class="tab-btn" :class="{ active: activeTab === 'reports' }" @click="activeTab = 'reports'">
          {{ locale === 'zh' ? '报告流' : 'Reports' }}
        </button>
      </div>
      <div class="toolbar-actions">
        <el-button v-if="activeTab === 'investigations'" size="small" plain :loading="loadingInvestigations" @click="refreshInvestigations(false)">
          {{ locale === 'zh' ? '刷新' : 'Refresh' }}
        </el-button>
        <el-button v-else size="small" plain @click="clearReports">
          {{ locale === 'zh' ? '清空' : 'Clear' }}
        </el-button>
      </div>
    </div>

    <div v-if="activeTab === 'investigations'" class="investigation-view">
      <div v-if="qualitySummary" class="quality-grid">
        <div class="quality-item">
          <p class="quality-label">{{ locale === 'zh' ? '误报率' : 'False Positive' }}</p>
          <p class="quality-value">{{ formatPercent(qualitySummary.falsePositiveRate) }}</p>
        </div>
        <div class="quality-item">
          <p class="quality-label">{{ locale === 'zh' ? '执行成功率' : 'Run Success' }}</p>
          <p class="quality-value">{{ formatPercent(qualitySummary.actionRunSuccessRate) }}</p>
        </div>
        <div class="quality-item">
          <p class="quality-label">{{ locale === 'zh' ? '平均定位时长(分钟)' : 'Avg Resolve (min)' }}</p>
          <p class="quality-value">{{ qualitySummary.mttrMinutes ?? 0 }}</p>
        </div>
        <div class="quality-item">
          <p class="quality-label">{{ locale === 'zh' ? '进行中调查' : 'Open Investigations' }}</p>
          <p class="quality-value">{{ qualitySummary.openInvestigations ?? 0 }}</p>
        </div>
      </div>

      <div class="investigation-list">
        <button
          v-for="item in investigations"
          :key="item.id"
          class="investigation-item"
          :class="{ active: selectedInvestigationId === item.id }"
          @click="selectInvestigation(item.id)">
          <p class="inv-title">{{ item.title || `Investigation #${item.id}` }}</p>
          <div class="inv-meta">
            <span class="inv-chip severity">{{ item.severity || 'P2' }}</span>
            <span class="inv-chip">{{ item.status || '-' }}</span>
          </div>
        </button>

        <div v-if="!investigations.length && !loadingInvestigations" class="ai-empty inline">
          <p>{{ locale === 'zh' ? '暂无调查对象' : 'No investigations yet' }}</p>
        </div>
      </div>

      <div class="investigation-detail">
        <div v-if="selectedDetail?.investigation" class="detail-card">
          <div class="detail-head">
            <div>
              <p class="detail-title">{{ selectedDetail.investigation.title || `Investigation #${selectedDetail.investigation.id}` }}</p>
              <p class="detail-sub">
                {{ locale === 'zh' ? '状态' : 'Status' }}:
                <span class="tone-highlight">{{ selectedDetail.investigation.status }}</span>
                ·
                {{ locale === 'zh' ? '等级' : 'Severity' }}:
                <span class="tone-highlight">{{ selectedDetail.investigation.severity }}</span>
              </p>
            </div>
            <div class="detail-head-actions">
              <el-button
                size="small"
                type="primary"
                plain
                :loading="aiGenerating"
                @click="runStructuredAnalysis">
                {{ locale === 'zh' ? 'AI 结构化分析' : 'AI Analyze' }}
              </el-button>
              <el-button
                v-if="selectedDetail.investigation.status !== 'CLOSED'"
                size="small"
                type="danger"
                plain
                :loading="closingInvestigation"
                @click="closeCurrentInvestigation">
                {{ locale === 'zh' ? '关闭调查' : 'Close' }}
              </el-button>
            </div>
          </div>

          <div v-if="selectedSnapshotHtml" class="report-snapshot prose" v-html="selectedSnapshotHtml"></div>
          <div v-else class="ai-empty inline">
            <p>{{ locale === 'zh' ? '暂无报告快照' : 'No report snapshot' }}</p>
          </div>

          <div class="snapshot-editor">
            <el-input
              v-model="snapshotDraft"
              type="textarea"
              :rows="3"
              resize="none"
              :placeholder="locale === 'zh' ? '可编辑当前报告，保存为新快照版本...' : 'Edit report and save as a new snapshot version...'" />
            <div class="snapshot-editor-row">
              <el-button
                size="small"
                plain
                :loading="postmortemGenerating"
                @click="generatePostmortemDraft">
                {{ locale === 'zh' ? '生成复盘草稿' : 'Generate Postmortem' }}
              </el-button>
              <el-button
                size="small"
                type="primary"
                plain
                :disabled="!snapshotDraft.trim()"
                :loading="snapshotSaving"
                @click="saveSnapshot">
                {{ locale === 'zh' ? '保存快照' : 'Save Snapshot' }}
              </el-button>
            </div>
          </div>

          <div class="observation-head">
            <span>{{ locale === 'zh' ? '证据观测' : 'Observations' }}</span>
            <span>{{ observations.length }}</span>
          </div>

          <div v-if="selectedDetail.investigation.status !== 'CLOSED'" class="observation-create">
            <div class="observation-create-row">
              <el-select v-model="observationDraft.type" size="small" class="observation-type-select">
                <el-option label="METRIC" value="METRIC" />
                <el-option label="LOG" value="LOG" />
                <el-option label="TRACE" value="TRACE" />
                <el-option label="CHANGE" value="CHANGE" />
                <el-option label="EVENT" value="EVENT" />
              </el-select>
              <el-input
                v-model="observationDraft.metricName"
                size="small"
                :placeholder="locale === 'zh' ? '指标名，如 cpu.usage' : 'Metric name, e.g. cpu.usage'" />
            </div>
            <div class="observation-create-row">
              <el-input-number
                v-model="observationDraft.metricValue"
                size="small"
                :step="0.1"
                class="observation-number" />
              <el-input
                v-model="observationDraft.hostname"
                size="small"
                :placeholder="locale === 'zh' ? '主机名（可选）' : 'Hostname (optional)'" />
            </div>
            <el-input
              v-model="observationDraft.sourceRef"
              size="small"
              :placeholder="locale === 'zh' ? '来源引用（日志ID、链路ID、任务ID）' : 'Source ref (log ID, trace ID, job ID)'" />
            <div class="observation-create-row">
              <el-input-number
                v-model="observationDraft.confidence"
                size="small"
                :min="0"
                :max="1"
                :step="0.05"
                :precision="2"
                class="observation-confidence" />
              <el-button size="small" type="primary" :loading="observationSubmitting" @click="createObservation">
                {{ locale === 'zh' ? '新增证据' : 'Add Observation' }}
              </el-button>
            </div>
          </div>

          <div class="observation-list">
            <div v-for="item in observations" :key="item.id" class="observation-item">
              <div class="observation-item-head">
                <p class="observation-title">{{ item.type }} · {{ item.metricName || item.sourceRef || 'N/A' }}</p>
                <div class="action-tags">
                  <span class="inv-chip">V={{ item.metricValue ?? '-' }}</span>
                  <span class="inv-chip">C={{ typeof item.confidence === 'number' ? item.confidence.toFixed(2) : '-' }}</span>
                </div>
              </div>
              <p class="observation-meta">
                {{ formatDateTime(item.observedAt || item.createdAt) }} · {{ item.hostname || 'unknown-host' }}
              </p>
            </div>
            <div v-if="!observations.length" class="ai-empty inline">
              <p>{{ locale === 'zh' ? '暂无证据观测' : 'No observations yet' }}</p>
            </div>
          </div>

          <div class="hypothesis-head">
            <span>{{ locale === 'zh' ? '根因假设' : 'Hypotheses' }}</span>
            <span>{{ hypotheses.length }}</span>
          </div>

          <div v-if="selectedDetail.investigation.status !== 'CLOSED'" class="hypothesis-create">
            <el-input
              v-model="hypothesisDraft.title"
              size="small"
              :placeholder="locale === 'zh' ? '假设标题，例如：JVM 堆外内存泄漏' : 'Hypothesis title, e.g. JVM off-heap memory leak'" />
            <el-input
              v-model="hypothesisDraft.reasoning"
              type="textarea"
              :rows="2"
              resize="none"
              :placeholder="locale === 'zh' ? '推理依据（指标、日志、变更线索）' : 'Reasoning evidence (metrics, logs, changes)'" />
            <div class="hypothesis-create-row">
              <el-select v-model="hypothesisDraft.status" size="small" class="hypothesis-select">
                <el-option label="CANDIDATE" value="CANDIDATE" />
                <el-option label="CONFIRMED" value="CONFIRMED" />
                <el-option label="REJECTED" value="REJECTED" />
              </el-select>
              <el-input-number
                v-model="hypothesisDraft.confidence"
                size="small"
                :min="0"
                :max="1"
                :step="0.05"
                :precision="2"
                class="hypothesis-confidence" />
              <el-button size="small" type="primary" :loading="hypothesisSubmitting" @click="createHypothesis">
                {{ locale === 'zh' ? '新增假设' : 'Add Hypothesis' }}
              </el-button>
            </div>
          </div>

          <div class="hypothesis-list">
            <div v-for="item in hypotheses" :key="item.id" class="hypothesis-item">
              <div class="hypothesis-item-head">
                <p class="hypothesis-title">#{{ item.rankOrder }} · {{ item.title }}</p>
                <div class="action-tags">
                  <span class="inv-chip">{{ item.status || 'CANDIDATE' }}</span>
                  <span class="inv-chip">C={{ typeof item.confidence === 'number' ? item.confidence.toFixed(2) : '-' }}</span>
                </div>
              </div>
              <p v-if="item.reasoning" class="hypothesis-reasoning">{{ item.reasoning }}</p>
            </div>
            <div v-if="!hypotheses.length" class="ai-empty inline">
              <p>{{ locale === 'zh' ? '暂无根因假设' : 'No hypotheses yet' }}</p>
            </div>
          </div>

          <div class="action-head">
            <span>{{ locale === 'zh' ? '动作计划' : 'Action Plans' }}</span>
            <span>{{ actionPlans.length }}</span>
          </div>

          <div v-if="selectedDetail.investigation.status !== 'CLOSED'" class="action-create">
            <el-input
              v-model="actionDraft.title"
              size="small"
              :placeholder="locale === 'zh' ? '动作标题，例如：回收高内存进程' : 'Action title, e.g. recycle high-memory process'" />
            <el-input
              v-model="actionDraft.commandText"
              type="textarea"
              :rows="2"
              resize="none"
              :placeholder="locale === 'zh' ? '执行命令或 runbook 描述' : 'Command or runbook notes'" />
            <div class="action-create-row">
              <el-select v-model="actionDraft.riskLevel" size="small" class="action-select">
                <el-option label="LOW" value="LOW" />
                <el-option label="MEDIUM" value="MEDIUM" />
                <el-option label="HIGH" value="HIGH" />
              </el-select>
              <el-switch
                v-model="actionDraft.requiresApproval"
                :active-text="locale === 'zh' ? '需审批' : 'Approval'"
                :inactive-text="locale === 'zh' ? '免审批' : 'No approval'" />
              <el-button size="small" type="primary" :loading="actionSubmitting" @click="createActionPlan">
                {{ locale === 'zh' ? '新增动作' : 'Add Action' }}
              </el-button>
            </div>
          </div>

          <div class="action-list">
            <div v-for="action in actionPlans" :key="action.id" class="action-item">
              <div class="action-item-head">
                <p class="action-title">{{ action.title }}</p>
                <div class="action-tags">
                  <span class="inv-chip">{{ action.actionType }}</span>
                  <span class="inv-chip" :class="`risk-${(action.riskLevel || '').toLowerCase()}`">{{ action.riskLevel || 'MEDIUM' }}</span>
                  <span class="inv-chip">{{ action.status }}</span>
                </div>
              </div>
              <p v-if="action.commandText" class="action-command">{{ action.commandText }}</p>
              <div class="action-buttons">
                <el-button
                  size="small"
                  plain
                  :disabled="action.status === 'APPROVED' || action.status === 'EXECUTED' || action.status === 'FAILED' || !action.requiresApproval"
                  :loading="actionOperatingId === action.id && actionOperatingType === 'approve'"
                  @click="approveAction(action)">
                  {{ locale === 'zh' ? '审批' : 'Approve' }}
                </el-button>
                <el-button
                  size="small"
                  type="success"
                  plain
                  :disabled="action.status === 'EXECUTED' || (action.requiresApproval && action.status !== 'APPROVED' && action.status !== 'FAILED')"
                  :loading="actionOperatingId === action.id && actionOperatingType === 'execute'"
                  @click="executeAction(action)">
                  {{ locale === 'zh' ? '执行' : 'Execute' }}
                </el-button>
                <el-button
                  size="small"
                  type="warning"
                  plain
                  :disabled="action.status !== 'FAILED'"
                  :loading="actionOperatingId === action.id && actionOperatingType === 'retry'"
                  @click="retryAction(action)">
                  {{ locale === 'zh' ? '重试' : 'Retry' }}
                </el-button>
              </div>
            </div>
            <div v-if="!actionPlans.length" class="ai-empty inline">
              <p>{{ locale === 'zh' ? '暂无动作计划' : 'No action plans' }}</p>
            </div>
          </div>

          <div class="execution-head">
            <span>{{ locale === 'zh' ? '执行记录' : 'Execution Runs' }}</span>
            <span>{{ actionRuns.length }}</span>
          </div>

          <div class="execution-list">
            <div v-for="run in actionRuns" :key="run.id" class="execution-item">
              <div class="execution-item-head">
                <p class="execution-title">#{{ run.id }} · {{ run.status || '-' }}</p>
                <div class="action-tags">
                  <span class="inv-chip">{{ run.executionMode || 'MANUAL' }}</span>
                  <span class="inv-chip">{{ run.executor || '-' }}</span>
                </div>
              </div>
              <p class="execution-time">
                {{ formatDateTime(run.startedAt || run.createdAt) }} → {{ formatDateTime(run.endedAt || run.createdAt) }}
              </p>
              <p v-if="run.outputText" class="execution-output">{{ run.outputText }}</p>
              <p v-if="run.errorMessage" class="execution-error">{{ run.errorMessage }}</p>
            </div>
            <div v-if="!actionRuns.length" class="ai-empty inline">
              <p>{{ locale === 'zh' ? '暂无执行记录' : 'No execution runs' }}</p>
            </div>
          </div>

          <div class="timeline-head">
            <span>{{ locale === 'zh' ? '调查时间线' : 'Timeline' }}</span>
            <div class="timeline-filter">
              <el-select v-model="timelineCategory" size="small" class="timeline-select">
                <el-option value="ALL" :label="locale === 'zh' ? '全部' : 'All'" />
                <el-option value="INVESTIGATION" label="INVESTIGATION" />
                <el-option value="OBSERVATION" label="OBSERVATION" />
                <el-option value="HYPOTHESIS" label="HYPOTHESIS" />
                <el-option value="ACTION_PLAN" label="ACTION_PLAN" />
                <el-option value="ACTION_RUN" label="ACTION_RUN" />
                <el-option value="REPORT_SNAPSHOT" label="REPORT_SNAPSHOT" />
              </el-select>
              <span>{{ filteredTimelineEvents.length }}</span>
            </div>
          </div>

          <div class="timeline-list">
            <div v-for="event in filteredTimelineEvents" :key="`${event.category}-${event.refId}-${event.time}`" class="timeline-item">
              <p class="timeline-time">{{ formatDateTime(event.time) }}</p>
              <p class="timeline-title">{{ event.title }}</p>
              <p class="timeline-detail">{{ event.detail }}</p>
              <p v-if="formatTimelineMeta(event)" class="timeline-meta">{{ formatTimelineMeta(event) }}</p>
            </div>
            <div v-if="!filteredTimelineEvents.length && !loadingDetail" class="ai-empty inline">
              <p>{{ locale === 'zh' ? '暂无时间线事件' : 'No timeline events' }}</p>
            </div>
          </div>
        </div>

        <div v-else class="ai-empty">
          <p>{{ loadingDetail ? (locale === 'zh' ? '加载调查详情...' : 'Loading investigation...') : (locale === 'zh' ? '请选择调查对象' : 'Select an investigation') }}</p>
        </div>
      </div>
    </div>

    <div v-else class="ai-report-list">
      <transition-group name="slide-fade" tag="div">
        <div
          v-for="report in reports"
          :key="report.id"
          class="ai-report-card"
          :class="{ risk: report.isRisk }">
          <div class="ai-report-meta">
            <span>{{ report.time }}</span>
            <span v-if="report.isRisk" class="risk-tag">RISK</span>
          </div>
          <div class="ai-report-content prose" v-html="report.html"></div>
        </div>
      </transition-group>

      <div v-if="reports.length === 0" class="ai-empty">
        <p>{{ locale === 'zh' ? '等待 AI 诊断结果...' : 'Waiting for AI diagnostics...' }}</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { marked } from 'marked'
import SockJS from 'sockjs-client'
import Stomp from 'stompjs'
import { ElMessage } from 'element-plus'
import { WS_BASE_URL } from '../config/env'
import { useAuthStore } from '../stores/auth'
import { useLocaleMode } from '../composables/useLocaleMode'
import {
  approveInvestigationAction,
  closeInvestigation,
  createInvestigationAction,
  createInvestigationHypothesis,
  createInvestigationObservation,
  createInvestigationSnapshot,
  executeInvestigationAction,
  generateInvestigationAi,
  generateInvestigationPostmortem,
  getInvestigationDetail,
  getInvestigations,
  getInvestigationQualitySummary,
  getInvestigationTimeline,
  retryInvestigationAction
} from '../api/investigations'

const auth = useAuthStore()
const { locale } = useLocaleMode()

const activeTab = ref('investigations')
const reports = ref([])
const wsConnected = ref(false)

const investigations = ref([])
const selectedInvestigationId = ref(null)
const selectedDetail = ref(null)
const timelineEvents = ref([])
const loadingInvestigations = ref(false)
const loadingDetail = ref(false)
const closingInvestigation = ref(false)
const aiGenerating = ref(false)
const postmortemGenerating = ref(false)
const observationSubmitting = ref(false)
const hypothesisSubmitting = ref(false)
const actionSubmitting = ref(false)
const actionOperatingId = ref(null)
const actionOperatingType = ref('')
const snapshotSaving = ref(false)
const snapshotDraft = ref('')
const timelineCategory = ref('ALL')
const qualitySummary = ref(null)

let stompClient = null
let reconnectTimer = null
let reportCounter = 0
let investigationRefreshTimer = null

const observationDraft = reactive({
  type: 'METRIC',
  metricName: '',
  metricValue: null,
  hostname: '',
  sourceRef: '',
  confidence: 0.8
})

const hypothesisDraft = reactive({
  title: '',
  reasoning: '',
  confidence: 0.7,
  status: 'CANDIDATE'
})

const actionDraft = reactive({
  title: '',
  commandText: '',
  riskLevel: 'MEDIUM',
  requiresApproval: true
})

const selectedSnapshotHtml = computed(() => {
  const markdown = selectedDetail.value?.latestSnapshot?.reportMarkdown
  return markdown ? marked.parse(markdown) : ''
})

const observations = computed(() => selectedDetail.value?.observations || [])
const hypotheses = computed(() => selectedDetail.value?.hypotheses || [])
const actionPlans = computed(() => selectedDetail.value?.actionPlans || [])
const actionRuns = computed(() => selectedDetail.value?.actionRuns || [])
const filteredTimelineEvents = computed(() => {
  if (timelineCategory.value === 'ALL') return timelineEvents.value
  return timelineEvents.value.filter((x) => x?.category === timelineCategory.value)
})

function clearReports() {
  reports.value = []
}

function formatTime() {
  return new Date().toLocaleTimeString(locale.value === 'zh' ? 'zh-CN' : 'en-US', { hour12: false })
}

function formatDateTime(value) {
  if (!value) return '-'
  const dt = new Date(value)
  if (Number.isNaN(dt.getTime())) return '-'
  return dt.toLocaleString(locale.value === 'zh' ? 'zh-CN' : 'en-US', { hour12: false })
}

function formatTimelineMeta(event) {
  if (!event?.metadata || typeof event.metadata !== 'object') return ''
  const entries = Object.entries(event.metadata)
    .filter(([, v]) => v !== null && v !== undefined && v !== '' && v !== '-')
    .slice(0, 3)
    .map(([k, v]) => `${k}: ${v}`)
  return entries.join(' | ')
}

function formatPercent(value) {
  const n = Number(value)
  if (Number.isNaN(n)) return '0.00%'
  return `${n.toFixed(2)}%`
}

function scheduleReconnect() {
  clearTimeout(reconnectTimer)
  reconnectTimer = setTimeout(connectReportsStream, 5000)
}

function connectReportsStream() {
  if (stompClient?.connected) return

  const socket = new SockJS(`${WS_BASE_URL}/ws-monitor`)
  const client = Stomp.over(socket)
  client.debug = null

  const headers = auth.token ? { Authorization: `Bearer ${auth.token}` } : {}
  client.connect(headers, () => {
    wsConnected.value = true
    stompClient = client

    client.subscribe('/topic/ai-reports', (msg) => {
      const raw = msg.body || ''
      reports.value.unshift({
        id: `expert-${++reportCounter}-${Date.now()}`,
        time: formatTime(),
        html: marked.parse(raw),
        isRisk: /有风险|风险|告警|critical|warning|alert/i.test(raw)
      })
      if (reports.value.length > 80) {
        reports.value = reports.value.slice(0, 80)
      }
    })

    client.subscribe('/topic/investigations', async (msg) => {
      let event = null
      try {
        event = JSON.parse(msg.body || '{}')
      } catch (_e) {
        event = null
      }
      const eventMessage = event?.message || (locale.value === 'zh' ? '调查事件已更新' : 'Investigation event updated')
      reports.value.unshift({
        id: `inv-${++reportCounter}-${Date.now()}`,
        time: formatTime(),
        html: marked.parse(`**${event?.eventType || 'INV_EVENT'}**  \n${eventMessage}`),
        isRisk: /P1|CRITICAL|FAILED|风险|告警|critical|warning|alert/i.test(String(msg.body || ''))
      })
      if (reports.value.length > 80) {
        reports.value = reports.value.slice(0, 80)
      }

      const currentId = selectedInvestigationId.value
      const incomingId = Number(event?.investigationId)
      if (currentId && incomingId && currentId === incomingId) {
        await loadInvestigationDetail(currentId, true)
        await loadInvestigationTimeline(currentId, true)
      }
      await loadQualitySummary(true)
    })
  }, () => {
    wsConnected.value = false
    stompClient = null
    scheduleReconnect()
  })
}

async function refreshInvestigations(silent = true) {
  if (!silent) loadingInvestigations.value = true
  try {
    const { data } = await getInvestigations({ page: 0, size: 20 })
    investigations.value = data?.content || []

    if (!investigations.value.length) {
      selectedInvestigationId.value = null
      selectedDetail.value = null
      timelineEvents.value = []
      return
    }

    const exists = investigations.value.some((x) => x.id === selectedInvestigationId.value)
    if (!exists) {
      await selectInvestigation(investigations.value[0].id)
      return
    }

    if (selectedInvestigationId.value) {
      await loadInvestigationDetail(selectedInvestigationId.value, true)
      await loadInvestigationTimeline(selectedInvestigationId.value, true)
    }
  } catch (_e) {
    if (!silent) {
      ElMessage.error(locale.value === 'zh' ? '加载调查列表失败' : 'Failed to load investigations')
    }
  } finally {
    loadingInvestigations.value = false
  }
}

async function selectInvestigation(id) {
  selectedInvestigationId.value = id
  await loadInvestigationDetail(id, false)
  await loadInvestigationTimeline(id, false)
}

async function loadInvestigationDetail(id, silent = false) {
  if (!silent) loadingDetail.value = true
  try {
    const { data } = await getInvestigationDetail(id)
    if (selectedInvestigationId.value === id) {
      selectedDetail.value = data
      if (!snapshotDraft.value || !silent) {
        snapshotDraft.value = data?.latestSnapshot?.reportMarkdown || ''
      }
    }
  } catch (_e) {
    if (!silent) {
      ElMessage.error(locale.value === 'zh' ? '加载调查详情失败' : 'Failed to load investigation detail')
    }
  } finally {
    if (!silent) loadingDetail.value = false
  }
}

async function loadInvestigationTimeline(id, silent = false) {
  if (!silent) loadingDetail.value = true
  try {
    const { data } = await getInvestigationTimeline(id)
    if (selectedInvestigationId.value === id) {
      timelineEvents.value = Array.isArray(data) ? data : []
    }
  } catch (_e) {
    if (!silent) {
      ElMessage.error(locale.value === 'zh' ? '加载时间线失败' : 'Failed to load timeline')
    }
  } finally {
    if (!silent) loadingDetail.value = false
  }
}

async function loadQualitySummary(silent = true) {
  try {
    const { data } = await getInvestigationQualitySummary()
    qualitySummary.value = data || null
  } catch (_e) {
    if (!silent) {
      ElMessage.error(locale.value === 'zh' ? '加载质量指标失败' : 'Failed to load quality metrics')
    }
  }
}

async function closeCurrentInvestigation() {
  if (!selectedDetail.value?.investigation?.id) return
  const id = selectedDetail.value.investigation.id
  closingInvestigation.value = true
  try {
    await closeInvestigation(id)
    ElMessage.success(locale.value === 'zh' ? '调查已关闭' : 'Investigation closed')
    await refreshInvestigations(false)
  } catch (_e) {
    ElMessage.error(locale.value === 'zh' ? '关闭调查失败' : 'Failed to close investigation')
  } finally {
    closingInvestigation.value = false
  }
}

async function runStructuredAnalysis() {
  const investigationId = selectedDetail.value?.investigation?.id
  if (!investigationId) return
  aiGenerating.value = true
  try {
    await generateInvestigationAi(investigationId, { includePostmortem: false })
    ElMessage.success(locale.value === 'zh' ? 'AI 结构化分析已完成' : 'AI structured analysis completed')
    await loadInvestigationDetail(investigationId, true)
    await loadInvestigationTimeline(investigationId, true)
    await loadQualitySummary(true)
  } catch (_e) {
    ElMessage.error(locale.value === 'zh' ? 'AI 分析失败' : 'AI analysis failed')
  } finally {
    aiGenerating.value = false
  }
}

async function generatePostmortemDraft() {
  const investigationId = selectedDetail.value?.investigation?.id
  if (!investigationId) return
  postmortemGenerating.value = true
  try {
    const { data } = await generateInvestigationPostmortem(investigationId, {})
    if (data?.markdown) {
      snapshotDraft.value = data.markdown
    }
    ElMessage.success(locale.value === 'zh' ? '复盘草稿已生成' : 'Postmortem draft generated')
    await loadInvestigationDetail(investigationId, true)
    await loadInvestigationTimeline(investigationId, true)
  } catch (_e) {
    ElMessage.error(locale.value === 'zh' ? '生成复盘草稿失败' : 'Failed to generate postmortem')
  } finally {
    postmortemGenerating.value = false
  }
}

async function createObservation() {
  const investigationId = selectedDetail.value?.investigation?.id
  if (!investigationId) return
  if (!observationDraft.metricName.trim() && !observationDraft.sourceRef.trim()) {
    ElMessage.warning(locale.value === 'zh' ? '请至少填写指标名或来源引用' : 'Please provide metric name or source ref')
    return
  }
  observationSubmitting.value = true
  try {
    await createInvestigationObservation(investigationId, {
      type: observationDraft.type,
      metricName: observationDraft.metricName.trim() || undefined,
      metricValue: typeof observationDraft.metricValue === 'number' ? observationDraft.metricValue : undefined,
      hostname: observationDraft.hostname.trim() || undefined,
      sourceRef: observationDraft.sourceRef.trim() || undefined,
      confidence: typeof observationDraft.confidence === 'number' ? observationDraft.confidence : undefined
    })
    observationDraft.type = 'METRIC'
    observationDraft.metricName = ''
    observationDraft.metricValue = null
    observationDraft.hostname = ''
    observationDraft.sourceRef = ''
    observationDraft.confidence = 0.8
    ElMessage.success(locale.value === 'zh' ? '证据观测已写入' : 'Observation added')
    await loadInvestigationDetail(investigationId, true)
    await loadInvestigationTimeline(investigationId, true)
  } catch (_e) {
    ElMessage.error(locale.value === 'zh' ? '新增证据失败' : 'Failed to add observation')
  } finally {
    observationSubmitting.value = false
  }
}

async function createHypothesis() {
  const investigationId = selectedDetail.value?.investigation?.id
  if (!investigationId) return
  if (!hypothesisDraft.title.trim()) {
    ElMessage.warning(locale.value === 'zh' ? '请填写假设标题' : 'Please input hypothesis title')
    return
  }
  hypothesisSubmitting.value = true
  try {
    await createInvestigationHypothesis(investigationId, {
      title: hypothesisDraft.title.trim(),
      reasoning: hypothesisDraft.reasoning.trim() || undefined,
      confidence: typeof hypothesisDraft.confidence === 'number' ? hypothesisDraft.confidence : undefined,
      status: hypothesisDraft.status
    })
    hypothesisDraft.title = ''
    hypothesisDraft.reasoning = ''
    hypothesisDraft.confidence = 0.7
    hypothesisDraft.status = 'CANDIDATE'
    ElMessage.success(locale.value === 'zh' ? '根因假设已创建' : 'Hypothesis created')
    await loadInvestigationDetail(investigationId, true)
    await loadInvestigationTimeline(investigationId, true)
  } catch (_e) {
    ElMessage.error(locale.value === 'zh' ? '创建假设失败' : 'Failed to create hypothesis')
  } finally {
    hypothesisSubmitting.value = false
  }
}

async function createActionPlan() {
  const investigationId = selectedDetail.value?.investigation?.id
  if (!investigationId) return
  if (!actionDraft.title.trim()) {
    ElMessage.warning(locale.value === 'zh' ? '请填写动作标题' : 'Please input action title')
    return
  }
  actionSubmitting.value = true
  try {
    await createInvestigationAction(investigationId, {
      actionType: 'RUNBOOK',
      title: actionDraft.title.trim(),
      commandText: actionDraft.commandText.trim() || undefined,
      riskLevel: actionDraft.riskLevel,
      requiresApproval: actionDraft.requiresApproval
    })
    actionDraft.title = ''
    actionDraft.commandText = ''
    actionDraft.riskLevel = 'MEDIUM'
    actionDraft.requiresApproval = true
    ElMessage.success(locale.value === 'zh' ? '动作计划已创建' : 'Action created')
    await loadInvestigationDetail(investigationId, true)
    await loadInvestigationTimeline(investigationId, true)
  } catch (_e) {
    ElMessage.error(locale.value === 'zh' ? '创建动作失败' : 'Failed to create action')
  } finally {
    actionSubmitting.value = false
  }
}

async function approveAction(action) {
  const investigationId = selectedDetail.value?.investigation?.id
  if (!investigationId || !action?.id) return
  actionOperatingId.value = action.id
  actionOperatingType.value = 'approve'
  try {
    await approveInvestigationAction(investigationId, action.id)
    ElMessage.success(locale.value === 'zh' ? '审批完成' : 'Approved')
    await loadInvestigationDetail(investigationId, true)
    await loadInvestigationTimeline(investigationId, true)
  } catch (_e) {
    ElMessage.error(locale.value === 'zh' ? '审批失败' : 'Approve failed')
  } finally {
    actionOperatingId.value = null
    actionOperatingType.value = ''
  }
}

async function executeAction(action) {
  const investigationId = selectedDetail.value?.investigation?.id
  if (!investigationId || !action?.id) return
  actionOperatingId.value = action.id
  actionOperatingType.value = 'execute'
  try {
    await executeInvestigationAction(investigationId, action.id, {
      status: 'SUCCESS',
      executionMode: 'MANUAL',
      outputText: 'Executed via AI Expert Panel.'
    })
    ElMessage.success(locale.value === 'zh' ? '执行记录已写入' : 'Execution recorded')
    await loadInvestigationDetail(investigationId, true)
    await loadInvestigationTimeline(investigationId, true)
  } catch (_e) {
    ElMessage.error(locale.value === 'zh' ? '执行失败' : 'Execute failed')
  } finally {
    actionOperatingId.value = null
    actionOperatingType.value = ''
  }
}

async function retryAction(action) {
  const investigationId = selectedDetail.value?.investigation?.id
  if (!investigationId || !action?.id) return
  actionOperatingId.value = action.id
  actionOperatingType.value = 'retry'
  try {
    await retryInvestigationAction(investigationId, action.id, {
      status: 'SUCCESS',
      executionMode: 'MANUAL',
      outputText: 'Retried via AI Expert Panel.'
    })
    ElMessage.success(locale.value === 'zh' ? '重试记录已写入' : 'Retry recorded')
    await loadInvestigationDetail(investigationId, true)
    await loadInvestigationTimeline(investigationId, true)
    await loadQualitySummary(true)
  } catch (_e) {
    ElMessage.error(locale.value === 'zh' ? '重试失败' : 'Retry failed')
  } finally {
    actionOperatingId.value = null
    actionOperatingType.value = ''
  }
}

async function saveSnapshot() {
  const investigationId = selectedDetail.value?.investigation?.id
  const markdown = snapshotDraft.value.trim()
  if (!investigationId || !markdown) return
  snapshotSaving.value = true
  try {
    await createInvestigationSnapshot(investigationId, {
      format: 'MARKDOWN',
      reportMarkdown: markdown,
      createdBy: 'USER'
    })
    ElMessage.success(locale.value === 'zh' ? '快照已保存' : 'Snapshot saved')
    await loadInvestigationDetail(investigationId, true)
    await loadInvestigationTimeline(investigationId, true)
  } catch (_e) {
    ElMessage.error(locale.value === 'zh' ? '保存快照失败' : 'Failed to save snapshot')
  } finally {
    snapshotSaving.value = false
  }
}

onMounted(async () => {
  connectReportsStream()
  await refreshInvestigations(false)
  await loadQualitySummary(false)
  investigationRefreshTimer = setInterval(() => {
    refreshInvestigations(true)
    loadQualitySummary(true)
  }, 15000)
})

onUnmounted(() => {
  wsConnected.value = false
  clearTimeout(reconnectTimer)
  clearInterval(investigationRefreshTimer)
  if (stompClient?.connected) {
    try {
      stompClient.disconnect()
    } catch (_e) {
      // ignore
    }
  }
  stompClient = null
})
</script>

<style scoped>
.ai-expert-panel {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 18px 16px 20px;
  border-left: 1px solid var(--line);
  background: linear-gradient(180deg, var(--panel-strong), var(--panel));
}

.ai-expert-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}

.ai-expert-head h2 {
  margin: 0;
  font-size: 16px;
  line-height: 1.2;
}

.ai-expert-head p {
  margin: 4px 0 0;
  font-size: 12px;
  color: var(--text-3);
}

.ai-connection {
  font-size: 11px;
  line-height: 1;
  padding: 7px 10px;
  border-radius: 999px;
  border: 1px solid rgba(251, 113, 133, 0.4);
  color: #fb7185;
  background: rgba(251, 113, 133, 0.1);
}

.ai-connection.online {
  border-color: rgba(52, 211, 153, 0.45);
  color: #34d399;
  background: rgba(52, 211, 153, 0.1);
}

.ai-note {
  border-radius: 12px;
  border: 1px solid var(--line);
  background: var(--panel-soft);
  padding: 10px 12px;
  color: var(--text-3);
  font-size: 12px;
  line-height: 1.5;
}

.ai-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.toolbar-tabs {
  display: flex;
  align-items: center;
  gap: 8px;
}

.tab-btn {
  border: 1px solid var(--line);
  background: transparent;
  color: var(--text-3);
  border-radius: 999px;
  padding: 6px 12px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.tab-btn.active {
  border-color: rgba(34, 211, 238, 0.4);
  background: rgba(34, 211, 238, 0.12);
  color: #67e8f9;
}

.investigation-view {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.quality-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.quality-item {
  border: 1px solid var(--line);
  border-radius: 10px;
  background: var(--panel-soft);
  padding: 8px 10px;
}

.quality-label {
  margin: 0;
  font-size: 10px;
  color: var(--text-3);
}

.quality-value {
  margin: 4px 0 0;
  font-size: 14px;
  color: var(--text-1);
  font-weight: 700;
}

.investigation-list {
  display: grid;
  grid-template-columns: 1fr;
  gap: 8px;
  max-height: 160px;
  overflow: auto;
  padding-right: 4px;
}

.investigation-item {
  text-align: left;
  border: 1px solid var(--line);
  border-radius: 12px;
  background: var(--panel);
  padding: 8px 10px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.investigation-item.active {
  border-color: rgba(59, 130, 246, 0.5);
  background: rgba(30, 58, 138, 0.28);
}

.inv-title {
  margin: 0;
  font-size: 12px;
  color: var(--text-1);
  line-height: 1.4;
}

.inv-meta {
  margin-top: 6px;
  display: flex;
  gap: 6px;
}

.inv-chip {
  border: 1px solid var(--line);
  border-radius: 999px;
  padding: 1px 8px;
  font-size: 11px;
  color: var(--text-3);
}

.inv-chip.severity {
  border-color: rgba(251, 191, 36, 0.35);
  color: #fbbf24;
}

.investigation-detail {
  flex: 1;
  min-height: 0;
  display: flex;
}

.detail-card {
  width: 100%;
  border: 1px solid var(--line);
  border-radius: 14px;
  background: var(--panel);
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-height: 0;
}

.detail-head {
  display: flex;
  justify-content: space-between;
  gap: 10px;
}

.detail-head-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.detail-title {
  margin: 0;
  font-size: 13px;
  color: var(--text-1);
}

.detail-sub {
  margin: 4px 0 0;
  font-size: 11px;
  color: var(--text-3);
}

.tone-highlight {
  color: #67e8f9;
}

.report-snapshot {
  border: 1px solid var(--line);
  border-radius: 10px;
  background: var(--panel-soft);
  padding: 10px;
  max-height: 170px;
  overflow: auto;
}

.report-snapshot :deep(*) {
  color: inherit;
}

.snapshot-editor {
  border: 1px solid var(--line);
  border-radius: 10px;
  background: var(--panel-soft);
  padding: 8px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.snapshot-editor-row {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}

.observation-head {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: var(--text-3);
}

.observation-create {
  border: 1px solid var(--line);
  border-radius: 10px;
  background: var(--panel-soft);
  padding: 8px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.observation-create-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.observation-type-select {
  width: 140px;
}

.observation-number,
.observation-confidence {
  width: 120px;
}

.observation-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 170px;
  overflow: auto;
  padding-right: 2px;
}

.observation-item {
  border: 1px solid var(--line);
  border-radius: 10px;
  background: var(--panel-soft);
  padding: 8px 10px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.observation-item-head {
  display: flex;
  justify-content: space-between;
  gap: 8px;
}

.observation-title {
  margin: 0;
  font-size: 12px;
  color: var(--text-1);
}

.observation-meta {
  margin: 0;
  font-size: 10px;
  color: var(--text-3);
}

.hypothesis-head {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: var(--text-3);
}

.hypothesis-create {
  border: 1px solid var(--line);
  border-radius: 10px;
  background: var(--panel-soft);
  padding: 8px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.hypothesis-create-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.hypothesis-select {
  width: 140px;
}

.hypothesis-confidence {
  flex: 1;
  min-width: 110px;
}

.hypothesis-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 170px;
  overflow: auto;
  padding-right: 2px;
}

.hypothesis-item {
  border: 1px solid var(--line);
  border-radius: 10px;
  background: var(--panel-soft);
  padding: 8px 10px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.hypothesis-item-head {
  display: flex;
  justify-content: space-between;
  gap: 8px;
}

.hypothesis-title {
  margin: 0;
  font-size: 12px;
  color: var(--text-1);
}

.hypothesis-reasoning {
  margin: 0;
  font-size: 11px;
  color: var(--text-2);
  white-space: pre-wrap;
}

.action-head {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: var(--text-3);
}

.action-create {
  border: 1px solid var(--line);
  border-radius: 10px;
  background: var(--panel-soft);
  padding: 8px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.action-create-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.action-select {
  width: 120px;
}

.action-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 190px;
  overflow: auto;
  padding-right: 2px;
}

.action-item {
  border: 1px solid var(--line);
  border-radius: 10px;
  background: var(--panel-soft);
  padding: 8px 10px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.action-item-head {
  display: flex;
  justify-content: space-between;
  gap: 8px;
}

.action-title {
  margin: 0;
  font-size: 12px;
  color: var(--text-1);
}

.action-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.inv-chip.risk-low {
  border-color: rgba(74, 222, 128, 0.4);
  color: #4ade80;
}

.inv-chip.risk-medium {
  border-color: rgba(251, 191, 36, 0.4);
  color: #fbbf24;
}

.inv-chip.risk-high {
  border-color: rgba(248, 113, 113, 0.45);
  color: #f87171;
}

.action-command {
  margin: 0;
  font-size: 11px;
  color: var(--text-2);
  white-space: pre-wrap;
}

.action-buttons {
  display: flex;
  gap: 8px;
}

.execution-head {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: var(--text-3);
}

.execution-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 170px;
  overflow: auto;
  padding-right: 2px;
}

.execution-item {
  border: 1px solid var(--line);
  border-radius: 10px;
  background: var(--panel-soft);
  padding: 8px 10px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.execution-item-head {
  display: flex;
  justify-content: space-between;
  gap: 8px;
}

.execution-title {
  margin: 0;
  font-size: 12px;
  color: var(--text-1);
}

.execution-time {
  margin: 0;
  font-size: 10px;
  color: var(--text-3);
}

.execution-output {
  margin: 0;
  font-size: 11px;
  color: var(--text-2);
  white-space: pre-wrap;
}

.execution-error {
  margin: 0;
  font-size: 11px;
  color: #fb7185;
  white-space: pre-wrap;
}

.timeline-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: var(--text-3);
}

.timeline-filter {
  display: flex;
  align-items: center;
  gap: 8px;
}

.timeline-select {
  width: 150px;
}

.timeline-list {
  flex: 1;
  min-height: 0;
  overflow: auto;
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding-right: 2px;
}

.timeline-item {
  border: 1px solid var(--line);
  border-radius: 10px;
  background: var(--panel-soft);
  padding: 8px 10px;
}

.timeline-time {
  margin: 0;
  font-size: 10px;
  color: var(--text-3);
}

.timeline-title {
  margin: 3px 0 0;
  font-size: 12px;
  color: var(--text-1);
}

.timeline-detail {
  margin: 3px 0 0;
  font-size: 11px;
  color: var(--text-2);
}

.timeline-meta {
  margin: 4px 0 0;
  font-size: 10px;
  color: var(--text-3);
}

.ai-report-list {
  flex: 1;
  min-height: 0;
  overflow: auto;
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding-right: 4px;
}

.ai-report-card {
  border-radius: 14px;
  border: 1px solid var(--line);
  background: var(--panel);
  padding: 12px;
}

.ai-report-card.risk {
  border-color: rgba(251, 113, 133, 0.36);
  background: rgba(127, 29, 29, 0.2);
}

.ai-report-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 11px;
  color: var(--text-3);
}

.risk-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 2px 8px;
  border-radius: 999px;
  border: 1px solid rgba(251, 113, 133, 0.4);
  color: #fb7185;
}

.ai-report-content :deep(*) {
  color: inherit;
}

.ai-empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-3);
  font-size: 12px;
}

.ai-empty.inline {
  min-height: 72px;
}

.slide-fade-enter-active {
  transition: all 0.25s ease-out;
}

.slide-fade-leave-active {
  transition: all 0.2s ease-in;
}

.slide-fade-enter-from,
.slide-fade-leave-to {
  transform: translateY(-6px);
  opacity: 0;
}

@media (max-width: 1280px) {
  .ai-expert-panel {
    border-left: none;
    border-top: 1px solid var(--line);
    min-height: 420px;
  }
}
</style>
