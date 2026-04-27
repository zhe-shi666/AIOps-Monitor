<template>
  <div class="ai-expert-panel">
    <div class="ai-expert-head">
      <div>
        <h2>{{ locale === 'zh' ? 'AI 专家中心' : 'AI Expert Console' }}</h2>
        <p>{{ locale === 'zh' ? '诊断、建议与运维协作中枢' : 'Diagnosis, suggestions, and ops collaboration hub' }}</p>
      </div>
      <span class="ai-connection" :class="{ online: wsConnected }">
        {{ wsConnected ? (locale === 'zh' ? '在线' : 'Online') : (locale === 'zh' ? '重连中' : 'Reconnecting') }}
      </span>
    </div>

    <div class="ai-note">
      {{ locale === 'zh'
        ? '右侧独立区域已预留扩展位：根因定位、修复建议、工单编排、自动化执行回放。'
        : 'Dedicated right area reserved for RCA, remediation plans, ticket orchestration and automation replay.'
      }}
    </div>

    <div class="ai-toolbar">
      <span>{{ reports.length }} {{ locale === 'zh' ? '份报告' : 'reports' }}</span>
      <el-button size="small" plain @click="clearReports">
        {{ locale === 'zh' ? '清空' : 'Clear' }}
      </el-button>
    </div>

    <div class="ai-report-list">
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
import { onMounted, onUnmounted, ref } from 'vue'
import { marked } from 'marked'
import SockJS from 'sockjs-client'
import Stomp from 'stompjs'
import { WS_BASE_URL } from '../config/env'
import { useAuthStore } from '../stores/auth'
import { useLocaleMode } from '../composables/useLocaleMode'

const auth = useAuthStore()
const { locale } = useLocaleMode()

const reports = ref([])
const wsConnected = ref(false)

let stompClient = null
let reconnectTimer = null
let reportCounter = 0

function clearReports() {
  reports.value = []
}

function formatTime() {
  return new Date().toLocaleTimeString(locale.value === 'zh' ? 'zh-CN' : 'en-US', { hour12: false })
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

onMounted(() => {
  connectReportsStream()
})

onUnmounted(() => {
  wsConnected.value = false
  clearTimeout(reconnectTimer)
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
  color: var(--text-3);
  font-size: 12px;
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
    min-height: 360px;
  }
}
</style>
