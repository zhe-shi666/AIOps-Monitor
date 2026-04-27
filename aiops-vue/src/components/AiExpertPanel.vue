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

          <div v-if="selectedSnapshotHtml" class="report-snapshot prose" v-html="selectedSnapshotHtml"></div>
          <div v-else class="ai-empty inline">
            <p>{{ locale === 'zh' ? '暂无报告快照' : 'No report snapshot' }}</p>
          </div>

          <div class="timeline-head">
            <span>{{ locale === 'zh' ? '调查时间线' : 'Timeline' }}</span>
            <span>{{ timelineEvents.length }}</span>
          </div>

          <div class="timeline-list">
            <div v-for="event in timelineEvents" :key="`${event.category}-${event.refId}-${event.time}`" class="timeline-item">
              <p class="timeline-time">{{ formatDateTime(event.time) }}</p>
              <p class="timeline-title">{{ event.title }}</p>
              <p class="timeline-detail">{{ event.detail }}</p>
              <p v-if="formatTimelineMeta(event)" class="timeline-meta">{{ formatTimelineMeta(event) }}</p>
            </div>
            <div v-if="!timelineEvents.length && !loadingDetail" class="ai-empty inline">
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
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { marked } from 'marked'
import SockJS from 'sockjs-client'
import Stomp from 'stompjs'
import { ElMessage } from 'element-plus'
import { WS_BASE_URL } from '../config/env'
import { useAuthStore } from '../stores/auth'
import { useLocaleMode } from '../composables/useLocaleMode'
import {
  closeInvestigation,
  getInvestigationDetail,
  getInvestigations,
  getInvestigationTimeline
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

let stompClient = null
let reconnectTimer = null
let reportCounter = 0
let investigationRefreshTimer = null

const selectedSnapshotHtml = computed(() => {
  const markdown = selectedDetail.value?.latestSnapshot?.reportMarkdown
  return markdown ? marked.parse(markdown) : ''
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

onMounted(async () => {
  connectReportsStream()
  await refreshInvestigations(false)
  investigationRefreshTimer = setInterval(() => {
    refreshInvestigations(true)
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

.timeline-head {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: var(--text-3);
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
