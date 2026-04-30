<template>
  <div class="page-surface">
    <header class="page-hero">
      <div>
        <h1>{{ t('title') }}</h1>
        <p>{{ t('subtitle') }}</p>
      </div>
      <div class="inline-actions">
        <el-button :loading="loadingIncidents || loadingInvestigations" @click="refreshAll">
          {{ t('refresh') }}
        </el-button>
      </div>
    </header>

    <section class="kpi-grid">
      <div class="kpi-item">
        <p class="kpi-label">{{ t('openIncidents') }}</p>
        <p class="kpi-value text-rose-300">{{ openIncidents }}</p>
      </div>
      <div class="kpi-item">
        <p class="kpi-label">{{ t('ackedIncidents') }}</p>
        <p class="kpi-value text-amber-300">{{ ackedIncidents }}</p>
      </div>
      <div class="kpi-item">
        <p class="kpi-label">{{ t('openInvestigations') }}</p>
        <p class="kpi-value text-cyan-300">{{ openInvestigations }}</p>
      </div>
      <div class="kpi-item">
        <p class="kpi-label">{{ t('resolvedIncidents') }}</p>
        <p class="kpi-value text-emerald-300">{{ resolvedIncidents }}</p>
      </div>
    </section>

    <section class="workbench-grid">
      <div class="card-panel table-wrap ops-table workbench-col workbench-left">
        <div class="section-head">
          <div>
            <h2 class="section-title">{{ t('incidentLane') }}</h2>
            <p class="section-subtitle">{{ t('incidentLaneDesc') }}</p>
          </div>
        </div>

        <div class="workbench-filters">
          <el-select v-model="filters.status" clearable :placeholder="t('status')" @change="reloadIncidents">
            <el-option label="OPEN" value="OPEN" />
            <el-option label="ACKNOWLEDGED" value="ACKNOWLEDGED" />
            <el-option label="RESOLVED" value="RESOLVED" />
          </el-select>
          <el-select v-model="filters.metricName" clearable :placeholder="t('metric')" @change="reloadIncidents">
            <el-option label="CPU" value="CPU" />
            <el-option label="MEMORY" value="MEMORY" />
            <el-option label="DISK" value="DISK" />
            <el-option label="PROCESS_COUNT" value="PROCESS_COUNT" />
          </el-select>
          <el-input v-model="filters.keyword" clearable :placeholder="t('keyword')" @change="reloadIncidents" />
          <el-switch
            v-model="filters.dedupOnly"
            :active-text="t('dedupOn')"
            :inactive-text="t('dedupOff')" />
          <el-button type="primary" @click="reloadIncidents">{{ t('query') }}</el-button>
        </div>

        <el-table
          :data="displayedIncidents"
          v-loading="loadingIncidents"
          height="100%"
          :row-class-name="rowClassName"
          @row-click="handleRowClick">
          <el-table-column prop="id" label="ID" width="76" />
          <el-table-column :label="t('severity')" width="86">
            <template #default="{ row }">
              <el-tag size="small" :type="severityType(row.severity)">{{ row.severity || 'P2' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="metricName" :label="t('metric')" width="112" />
          <el-table-column prop="hostname" :label="t('hostname')" min-width="130" />
          <el-table-column :label="t('status')" width="132">
            <template #default="{ row }">
              <el-tag size="small" :type="statusType(row.status)">{{ row.status || 'OPEN' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="t('message')" min-width="190" show-overflow-tooltip>
            <template #default="{ row }">
              <div class="message-cell">
                <span>{{ row.message || '-' }}</span>
                <el-tag v-if="row.__duplicateCount > 1" size="small" type="info">
                  {{ t('collapsed') }} x{{ row.__duplicateCount }}
                </el-tag>
              </div>
            </template>
          </el-table-column>
        </el-table>

        <div class="workbench-pagination">
          <el-pagination
            v-model:current-page="incidentPage"
            v-model:page-size="incidentPageSize"
            :total="incidentTotal"
            layout="total, prev, pager, next"
            @current-change="fetchIncidents" />
        </div>
      </div>

      <div class="card-panel table-wrap workbench-col workbench-right">
        <div class="section-head">
          <div>
            <h2 class="section-title">{{ t('investigationPanel') }}</h2>
            <p class="section-subtitle">{{ t('investigationPanelDesc') }}</p>
          </div>
        </div>

        <div v-if="selectedIncidentPanel" class="panel-body">
          <div class="panel-headline">
            <div>
              <p class="panel-title">#{{ selectedIncidentPanel.id }} · {{ selectedIncidentPanel.metricName || '-' }}</p>
              <p class="panel-subtitle">{{ selectedIncidentPanel.hostname || '-' }} · {{ formatDate(selectedIncidentPanel.createdAt) }}</p>
            </div>
            <div class="inline-actions">
              <el-tag size="small" :type="severityType(selectedIncidentPanel.severity)">{{ selectedIncidentPanel.severity || 'P2' }}</el-tag>
              <el-tag size="small" :type="statusType(selectedIncidentPanel.status)">{{ selectedIncidentPanel.status || 'OPEN' }}</el-tag>
            </div>
          </div>

          <div class="note-box incident-note">
            {{ selectedIncidentPanel.message || '-' }}
          </div>

          <div class="panel-actions">
            <el-button size="small" @click="setIncidentStatus(selectedIncidentPanel, 'ACKNOWLEDGED')" :disabled="selectedIncidentPanel.status === 'ACKNOWLEDGED'">
              {{ t('ack') }}
            </el-button>
            <el-button size="small" type="success" @click="setIncidentStatus(selectedIncidentPanel, 'RESOLVED')" :disabled="selectedIncidentPanel.status === 'RESOLVED'">
              {{ t('resolve') }}
            </el-button>
            <el-button size="small" type="info" @click="setIncidentStatus(selectedIncidentPanel, 'OPEN')" :disabled="selectedIncidentPanel.status === 'OPEN'">
              {{ t('reopen') }}
            </el-button>
            <el-button size="small" type="primary" plain :loading="investigatingIncidentId === selectedIncidentPanel.id" @click="investigateIncident(selectedIncidentPanel)">
              {{ resolveInvestigateLabel(selectedIncidentPanel) }}
            </el-button>
            <el-button size="small" text type="primary" @click="openDeliveries(selectedIncidentPanel)">
              {{ t('deliveries') }}
            </el-button>
          </div>

          <el-divider />

          <div class="panel-investigation">
            <div class="evidence-head">
              <div>
                <p class="panel-section-title">{{ t('evidenceContext') }}</p>
                <p class="panel-subtitle">{{ t('evidenceContextDesc') }}</p>
              </div>
              <el-button size="small" :loading="contextLoading" @click="fetchContext(selectedIncidentPanel)">
                {{ t('loadEvidence') }}
              </el-button>
            </div>

            <div v-if="incidentContext" class="context-grid">
              <div class="context-card">
                <span>{{ t('logs') }}</span>
                <strong>{{ incidentContext.logCount || 0 }}</strong>
              </div>
              <div class="context-card">
                <span>{{ t('traces') }}</span>
                <strong>{{ incidentContext.traceCount || 0 }}</strong>
              </div>
              <div class="context-card">
                <span>{{ t('window') }}</span>
                <strong>{{ incidentContext.windowMinutes || 30 }}m</strong>
              </div>
            </div>

            <div v-if="incidentContext" class="context-preview">
              <p class="panel-subtitle">{{ t('latestLogs') }}</p>
              <div v-if="incidentContext.logs?.length" class="context-list">
                <div v-for="log in incidentContext.logs.slice(0, 3)" :key="`log-${log.id}`">
                  <span>{{ log.level }} · {{ formatDate(log.occurredAt) }}</span>
                  <strong>{{ log.message }}</strong>
                </div>
              </div>
              <div v-else class="investigation-empty compact-empty">
                <p>{{ t('noLogs') }}</p>
              </div>

              <p class="panel-subtitle mt-2">{{ t('latestTraces') }}</p>
              <div v-if="incidentContext.traces?.length" class="context-list">
                <div v-for="trace in incidentContext.traces.slice(0, 3)" :key="`trace-${trace.id}`">
                  <span>{{ trace.status || 'OK' }} · {{ trace.durationMs }}ms</span>
                  <strong>{{ trace.operationName || trace.traceId }}</strong>
                </div>
              </div>
              <div v-else class="investigation-empty compact-empty">
                <p>{{ t('noTraces') }}</p>
              </div>
            </div>
          </div>

          <el-divider />

          <div class="panel-investigation">
            <p class="panel-section-title">{{ t('relatedInvestigation') }}</p>

            <div v-if="relatedInvestigation" class="investigation-card">
              <div class="investigation-card-head">
                <p class="panel-title">#{{ relatedInvestigation.id }} · {{ relatedInvestigation.status }}</p>
                <el-tag size="small" type="warning">{{ relatedInvestigation.severity || 'P2' }}</el-tag>
              </div>
              <p class="panel-subtitle">{{ t('source') }}: {{ relatedInvestigation.triggerSource || '-' }}</p>
              <p class="panel-subtitle">{{ t('createdAt') }}: {{ formatDate(relatedInvestigation.createdAt) }}</p>
              <p class="investigation-summary">{{ relatedInvestigation.summary || relatedInvestigation.title || '-' }}</p>
              <div class="panel-actions">
                <el-button size="small" type="primary" plain @click="openInAiExpert(relatedInvestigation.id)">
                  {{ t('openAiExpert') }}
                </el-button>
              </div>
            </div>

            <div v-else class="investigation-empty">
              <p>{{ t('noRelatedInvestigation') }}</p>
              <el-button size="small" type="primary" plain :loading="investigatingIncidentId === selectedIncidentPanel.id" @click="investigateIncident(selectedIncidentPanel)">
                {{ t('createInvestigation') }}
              </el-button>
            </div>
          </div>

          <el-divider />

          <div class="panel-investigation">
            <p class="panel-section-title">{{ t('recentInvestigations') }}</p>
            <div v-if="recentInvestigations.length" class="recent-list">
              <button
                v-for="inv in recentInvestigations"
                :key="inv.id"
                class="recent-item"
                type="button"
                @click="openInAiExpert(inv.id)">
                <span>#{{ inv.id }} · {{ inv.status }}</span>
                <span>{{ formatDate(inv.createdAt) }}</span>
              </button>
            </div>
            <div v-else class="investigation-empty">
              <p>{{ t('noInvestigationData') }}</p>
            </div>
          </div>
        </div>

        <div v-else class="investigation-empty panel-empty-state">
          <p>{{ t('selectIncidentHint') }}</p>
        </div>
      </div>
    </section>

    <el-dialog
      v-model="deliveryDialogVisible"
      :title="t('deliveryTitle')"
      width="980px"
      destroy-on-close>
      <div class="mb-3 text-xs text-slate-500">
        {{ t('incidentNo') }} #{{ selectedIncident?.id || '-' }} · {{ selectedIncident?.metricName || '-' }} · {{ selectedIncident?.hostname || '-' }}
      </div>
      <div class="ops-table">
        <el-table :data="deliveryRecords" v-loading="deliveryLoading">
          <el-table-column prop="channelName" :label="t('channelName')" width="160" />
          <el-table-column prop="status" :label="t('result')" width="120">
            <template #default="{ row }">
              <el-tag :type="row.status === 'SUCCESS' ? 'success' : 'danger'" size="small">{{ row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="httpStatus" label="HTTP" width="100" />
          <el-table-column prop="target" :label="t('target')" min-width="240" />
          <el-table-column :label="t('errorMessage')" min-width="220">
            <template #default="{ row }">{{ row.errorMessage || '-' }}</template>
          </el-table-column>
          <el-table-column :label="t('time')" width="180">
            <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
          </el-table-column>
        </el-table>
      </div>
      <div class="workbench-pagination">
        <el-pagination
          v-model:current-page="deliveryPage"
          v-model:page-size="deliveryPageSize"
          :total="deliveryTotal"
          layout="total, prev, pager, next"
          @current-change="fetchDeliveries" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getIncidentContext, getIncidentDeliveries, getIncidents, updateIncidentStatus } from '../api/incidents'
import { createInvestigation, getInvestigations } from '../api/investigations'
import { useI18n } from '../composables/useI18n'

const router = useRouter()
const loadingIncidents = ref(false)
const loadingInvestigations = ref(false)
const investigatingIncidentId = ref(null)

const incidentPage = ref(1)
const incidentPageSize = ref(12)
const incidentTotal = ref(0)
const incidents = ref([])
const selectedIncidentId = ref(null)

const investigations = ref([])
const investigationIndexByIncident = ref(new Map())

const deliveryDialogVisible = ref(false)
const deliveryLoading = ref(false)
const deliveryRecords = ref([])
const deliveryPage = ref(1)
const deliveryPageSize = ref(10)
const deliveryTotal = ref(0)
const selectedIncident = ref(null)
const contextLoading = ref(false)
const incidentContext = ref(null)

const filters = reactive({
  status: '',
  metricName: '',
  keyword: '',
  dedupOnly: true
})

const { locale, t } = useI18n({
  title: { zh: '事件工作台', en: 'Event Workbench' },
  subtitle: { zh: '左侧处理告警，右侧聚焦调查详情与执行入口', en: 'Handle incidents on the left and focus investigation details on the right' },
  refresh: { zh: '刷新', en: 'Refresh' },
  openIncidents: { zh: '待处理告警', en: 'Open Incidents' },
  ackedIncidents: { zh: '已确认告警', en: 'Acknowledged Incidents' },
  resolvedIncidents: { zh: '已解决告警', en: 'Resolved Incidents' },
  openInvestigations: { zh: '进行中调查', en: 'Open Investigations' },
  incidentLane: { zh: '告警泳道', en: 'Incident Lane' },
  incidentLaneDesc: { zh: '精简列表，先选中一条告警再在右侧操作', en: 'Compact list. Select one incident to operate in right panel' },
  investigationPanel: { zh: '调查详情面板', en: 'Investigation Detail Panel' },
  investigationPanelDesc: { zh: '围绕当前告警展示调查状态、入口与最近记录', en: 'Show investigation state and actions for selected incident' },
  status: { zh: '状态', en: 'Status' },
  metric: { zh: '指标', en: 'Metric' },
  keyword: { zh: '关键词', en: 'Keyword' },
  dedupOn: { zh: '去重显示', en: 'Dedup View' },
  dedupOff: { zh: '原始明细', en: 'Raw View' },
  collapsed: { zh: '折叠', en: 'Collapsed' },
  query: { zh: '查询', en: 'Query' },
  severity: { zh: '级别', en: 'Severity' },
  hostname: { zh: '主机名', en: 'Hostname' },
  message: { zh: '消息', en: 'Message' },
  summary: { zh: '摘要', en: 'Summary' },
  source: { zh: '来源', en: 'Source' },
  createdAt: { zh: '创建时间', en: 'Created At' },
  actions: { zh: '操作', en: 'Actions' },
  ack: { zh: '确认', en: 'Acknowledge' },
  resolve: { zh: '解决', en: 'Resolve' },
  reopen: { zh: '重开', en: 'Reopen' },
  createInvestigation: { zh: '发起调查', en: 'Create Investigation' },
  openInvestigation: { zh: '打开调查', en: 'Open Investigation' },
  openAiExpert: { zh: '进入 AI 专家', en: 'Open AI Expert' },
  deliveries: { zh: '投递记录', en: 'Deliveries' },
  evidenceContext: { zh: '证据上下文', en: 'Evidence Context' },
  evidenceContextDesc: { zh: '拉取该告警附近的日志与 Trace，供 AI 专家诊断引用', en: 'Load logs and traces around this incident for AI diagnosis' },
  loadEvidence: { zh: '加载证据', en: 'Load Evidence' },
  logs: { zh: '日志', en: 'Logs' },
  traces: { zh: 'Trace', en: 'Trace' },
  window: { zh: '窗口', en: 'Window' },
  latestLogs: { zh: '最近日志', en: 'Latest Logs' },
  latestTraces: { zh: '最近 Trace', en: 'Latest Traces' },
  noLogs: { zh: '暂无关联日志，确认 agent-lite 已开启 OBSERVABILITY_ENABLED。', en: 'No related logs. Check OBSERVABILITY_ENABLED in agent-lite.' },
  noTraces: { zh: '暂无关联 Trace，等待下一次 Agent 心跳即可生成。', en: 'No related traces. Wait for the next agent heartbeat.' },
  relatedInvestigation: { zh: '关联调查', en: 'Related Investigation' },
  noRelatedInvestigation: { zh: '当前告警尚未创建调查', en: 'No investigation created for this incident' },
  recentInvestigations: { zh: '最近调查', en: 'Recent Investigations' },
  noInvestigationData: { zh: '暂无调查数据', en: 'No investigation data' },
  selectIncidentHint: { zh: '请先在左侧选中一条告警', en: 'Select an incident from the left list first' },
  deliveryTitle: { zh: '通知投递记录', en: 'Delivery Records' },
  incidentNo: { zh: '事件', en: 'Incident' },
  channelName: { zh: '通道名称', en: 'Channel' },
  result: { zh: '结果', en: 'Result' },
  target: { zh: '目标', en: 'Target' },
  errorMessage: { zh: '错误信息', en: 'Error Message' },
  time: { zh: '时间', en: 'Time' },
  statusUpdated: { zh: '状态已更新', en: 'Status updated' },
  statusUpdateFailed: { zh: '更新状态失败', en: 'Failed to update status' },
  loadIncidentFailed: { zh: '加载告警失败', en: 'Failed to load incidents' },
  loadInvestigationFailed: { zh: '加载调查失败', en: 'Failed to load investigations' },
  investigationCreated: { zh: '调查已创建，已跳转 AI 专家', en: 'Investigation created and opened in AI Expert' },
  investigationCreateFailed: { zh: '创建调查失败', en: 'Failed to create investigation' },
  loadDeliveryFailed: { zh: '加载投递记录失败', en: 'Failed to load delivery records' },
  loadContextFailed: { zh: '加载证据上下文失败', en: 'Failed to load evidence context' }
})

const displayedIncidents = computed(() => {
  const raw = incidents.value || []
  if (!filters.dedupOnly) {
    return raw.map((row) => ({
      ...row,
      __primaryIncidentId: row.id,
      __incidentIds: [row.id],
      __duplicateCount: 1
    }))
  }

  const grouped = new Map()
  for (const row of raw) {
    const key = dedupKey(row)
    const existing = grouped.get(key)
    if (!existing) {
      grouped.set(key, {
        ...row,
        __primaryIncidentId: row.id,
        __incidentIds: [row.id],
        __duplicateCount: 1
      })
      continue
    }
    existing.__duplicateCount += 1
    existing.__incidentIds.push(row.id)
    if (new Date(row.createdAt || 0).getTime() > new Date(existing.createdAt || 0).getTime()) {
      existing.id = row.id
      existing.status = row.status
      existing.severity = row.severity
      existing.metricValue = row.metricValue
      existing.message = row.message
      existing.createdAt = row.createdAt
      existing.lastSeenAt = row.lastSeenAt
      existing.hostname = row.hostname
      existing.metricName = row.metricName
      existing.__primaryIncidentId = row.id
    }
  }
  return [...grouped.values()]
})

const openIncidents = computed(() => displayedIncidents.value.filter((x) => x.status === 'OPEN').length)
const ackedIncidents = computed(() => displayedIncidents.value.filter((x) => x.status === 'ACKNOWLEDGED').length)
const resolvedIncidents = computed(() => displayedIncidents.value.filter((x) => x.status === 'RESOLVED').length)
const openInvestigations = computed(() => investigations.value.filter((x) => x.status !== 'CLOSED').length)

const selectedIncidentPanel = computed(() => {
  if (!displayedIncidents.value.length) return null
  return displayedIncidents.value.find((x) => x.__primaryIncidentId === selectedIncidentId.value) || displayedIncidents.value[0]
})

const relatedInvestigation = computed(() => {
  const inc = selectedIncidentPanel.value
  if (!inc) return null
  const ids = incidentIdsOf(inc)
  for (const id of ids) {
    const found = investigationIndexByIncident.value.get(id)
    if (found) return found
  }
  return null
})

const recentInvestigations = computed(() => investigations.value.slice(0, 6))

function severityType(severity) {
  if (severity === 'P1') return 'danger'
  if (severity === 'P2') return 'warning'
  return 'info'
}

function statusType(status) {
  if (status === 'RESOLVED') return 'success'
  if (status === 'ACKNOWLEDGED') return 'warning'
  return 'danger'
}

function formatDate(value) {
  if (!value) return '-'
  const localeCode = locale.value === 'zh' ? 'zh-CN' : 'en-US'
  return new Date(value).toLocaleString(localeCode)
}

function resolveInvestigateLabel(incident) {
  return findRelatedInvestigation(incident) ? t('openInvestigation') : t('createInvestigation')
}

function dedupKey(row) {
  const fingerprint = row?.fingerprint ? String(row.fingerprint).trim() : ''
  if (fingerprint) return `${fingerprint}|${row?.status || 'OPEN'}`
  return [
    row?.hostname || 'unknown-host',
    row?.metricName || 'UNKNOWN_METRIC',
    row?.severity || 'P2',
    row?.status || 'OPEN',
    normalizeMessage(row?.message)
  ].join('|')
}

function normalizeMessage(msg) {
  return String(msg || '')
    .toLowerCase()
    .replace(/\s+/g, ' ')
    .replace(/\d+/g, '#')
    .trim()
    .slice(0, 120)
}

function incidentIdsOf(incident) {
  if (Array.isArray(incident?.__incidentIds) && incident.__incidentIds.length) {
    return incident.__incidentIds
  }
  if (incident?.id != null) return [incident.id]
  return []
}

function findRelatedInvestigation(incident) {
  const ids = incidentIdsOf(incident)
  for (const id of ids) {
    const found = investigationIndexByIncident.value.get(id)
    if (found) return found
  }
  return null
}

function openInAiExpert(investigationId) {
  router.push({ path: '/ai-expert', query: { investigationId: String(investigationId) } })
}

function handleRowClick(row) {
  selectedIncidentId.value = row.__primaryIncidentId || row.id
  incidentContext.value = null
}

function rowClassName({ row }) {
  return (row.__primaryIncidentId || row.id) === selectedIncidentId.value ? 'is-selected-incident-row' : ''
}

async function fetchIncidents() {
  loadingIncidents.value = true
  try {
    const { data } = await getIncidents({
      page: incidentPage.value - 1,
      size: incidentPageSize.value,
      status: filters.status || undefined,
      metricName: filters.metricName || undefined,
      keyword: filters.keyword || undefined
    })
    incidents.value = data?.content || []
    incidentTotal.value = data?.totalElements || 0
    const visible = displayedIncidents.value
    if (!visible.length) {
      selectedIncidentId.value = null
    } else if (!visible.some((x) => (x.__primaryIncidentId || x.id) === selectedIncidentId.value)) {
      selectedIncidentId.value = visible[0].__primaryIncidentId || visible[0].id
    }
  } catch (_e) {
    ElMessage.error(t('loadIncidentFailed'))
  } finally {
    loadingIncidents.value = false
  }
}

async function fetchInvestigations() {
  loadingInvestigations.value = true
  try {
    const { data } = await getInvestigations({ page: 0, size: 60 })
    investigations.value = data?.content || []
    const index = new Map()
    for (const inv of investigations.value) {
      if (inv?.incidentId != null && !index.has(inv.incidentId)) {
        index.set(inv.incidentId, inv)
      }
    }
    investigationIndexByIncident.value = index
  } catch (_e) {
    ElMessage.error(t('loadInvestigationFailed'))
  } finally {
    loadingInvestigations.value = false
  }
}

async function refreshAll() {
  await Promise.all([fetchIncidents(), fetchInvestigations()])
}

function reloadIncidents() {
  incidentPage.value = 1
  fetchIncidents()
}

async function setIncidentStatus(row, status) {
  try {
    const ids = incidentIdsOf(row)
    await Promise.all(ids.map((id) => updateIncidentStatus(id, status)))
    await fetchIncidents()
    await fetchInvestigations()
    ElMessage.success(t('statusUpdated'))
  } catch (_e) {
    ElMessage.error(t('statusUpdateFailed'))
  }
}

async function fetchContext(row) {
  const id = row?.__primaryIncidentId || row?.id
  if (!id) return
  contextLoading.value = true
  try {
    const { data } = await getIncidentContext(id, { minutes: 60, limit: 40 })
    incidentContext.value = data || null
  } catch (_e) {
    ElMessage.error(t('loadContextFailed'))
  } finally {
    contextLoading.value = false
  }
}

async function investigateIncident(row) {
  const existing = findRelatedInvestigation(row)
  if (existing?.id) {
    openInAiExpert(existing.id)
    return
  }

  const primaryId = row.__primaryIncidentId || row.id
  investigatingIncidentId.value = primaryId
  try {
    const { data } = await createInvestigation({
      incidentId: primaryId,
      targetId: row.targetId || undefined,
      title: `${row.metricName || 'Metric'} @ ${row.hostname || 'host'}`,
      triggerSource: 'AUTO',
      severity: row.severity || 'P2',
      summary: row.message || ''
    })
    await fetchInvestigations()
    ElMessage.success(t('investigationCreated'))
    openInAiExpert(data.id)
  } catch (_e) {
    ElMessage.error(t('investigationCreateFailed'))
  } finally {
    investigatingIncidentId.value = null
  }
}

function openDeliveries(row) {
  selectedIncident.value = {
    ...row,
    id: row.__primaryIncidentId || row.id
  }
  deliveryPage.value = 1
  deliveryDialogVisible.value = true
  fetchDeliveries()
}

async function fetchDeliveries() {
  if (!selectedIncident.value?.id) return
  deliveryLoading.value = true
  try {
    const { data } = await getIncidentDeliveries(selectedIncident.value.id, {
      page: deliveryPage.value - 1,
      size: deliveryPageSize.value
    })
    deliveryRecords.value = data?.content || []
    deliveryTotal.value = data?.totalElements || 0
  } catch (_e) {
    ElMessage.error(t('loadDeliveryFailed'))
  } finally {
    deliveryLoading.value = false
  }
}

onMounted(refreshAll)
</script>

<style scoped>
.workbench-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 16px;
}

.workbench-col {
  min-height: 520px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.workbench-left {
  min-width: 0;
}

.workbench-right {
  min-width: 0;
}

.workbench-filters {
  display: grid;
  grid-template-columns: repeat(5, minmax(110px, 1fr));
  gap: 10px;
}

.message-cell {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.workbench-pagination {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}

.panel-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 0;
}

.panel-headline {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 10px;
}

.panel-title {
  margin: 0;
  font-size: 14px;
  color: var(--text-1);
  font-weight: 700;
}

.panel-subtitle {
  margin: 3px 0 0;
  font-size: 12px;
  color: var(--text-3);
}

.incident-note {
  font-size: 12px;
  line-height: 1.55;
  white-space: pre-wrap;
}

.panel-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.panel-section-title {
  margin: 0 0 8px;
  font-size: 13px;
  color: var(--text-2);
  font-weight: 650;
}

.evidence-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 10px;
}

.context-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  margin-bottom: 10px;
}

.context-card,
.context-list div {
  border: 1px solid var(--line);
  border-radius: 12px;
  background: var(--panel-soft);
  padding: 10px;
}

.context-card span,
.context-list span {
  display: block;
  color: var(--text-3);
  font-size: 11px;
}

.context-card strong,
.context-list strong {
  display: block;
  color: var(--text-1);
  font-size: 12px;
  margin-top: 4px;
  line-height: 1.4;
}

.context-list {
  display: grid;
  gap: 8px;
  margin: 6px 0 10px;
}

.compact-empty {
  min-height: auto;
  padding: 8px 10px;
}

.investigation-card {
  border: 1px solid var(--line);
  border-radius: 12px;
  padding: 10px 12px;
  background: var(--panel-soft);
}

.investigation-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.investigation-summary {
  margin: 8px 0 0;
  color: var(--text-2);
  font-size: 12px;
  line-height: 1.5;
  white-space: pre-wrap;
}

.investigation-empty {
  border: 1px dashed var(--line);
  border-radius: 12px;
  background: var(--panel-soft);
  padding: 12px;
  color: var(--text-3);
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.panel-empty-state {
  min-height: 280px;
  justify-content: center;
}

.recent-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.recent-item {
  width: 100%;
  border: 1px solid var(--line);
  border-radius: 10px;
  background: var(--panel-soft);
  color: var(--text-2);
  font-size: 12px;
  text-align: left;
  padding: 8px 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  cursor: pointer;
  transition: all 0.2s ease;
}

.recent-item:hover {
  border-color: var(--line-strong);
  background: var(--panel);
}

:deep(.is-selected-incident-row > td) {
  background: rgba(14, 165, 233, 0.12) !important;
}

@media (min-width: 1480px) {
  .workbench-grid {
    grid-template-columns: 1.28fr 0.72fr;
    align-items: stretch;
  }
}

@media (max-width: 900px) {
  .workbench-filters {
    grid-template-columns: 1fr;
  }

  .panel-headline {
    flex-direction: column;
  }

  .investigation-empty {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
