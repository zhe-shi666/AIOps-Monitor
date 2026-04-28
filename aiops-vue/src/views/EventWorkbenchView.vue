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
      <div class="card-panel table-wrap ops-table workbench-col">
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
          <el-button type="primary" @click="reloadIncidents">{{ t('query') }}</el-button>
        </div>

        <el-table :data="incidents" v-loading="loadingIncidents" height="100%">
          <el-table-column prop="id" label="ID" width="78" />
          <el-table-column :label="t('severity')" width="94">
            <template #default="{ row }">
              <el-tag size="small" :type="severityType(row.severity)">{{ row.severity || 'P2' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="metricName" :label="t('metric')" width="126" />
          <el-table-column prop="hostname" :label="t('hostname')" min-width="140" />
          <el-table-column :label="t('status')" width="140">
            <template #default="{ row }">
              <el-tag size="small" :type="statusType(row.status)">{{ row.status || 'OPEN' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="message" :label="t('message')" min-width="220" />
          <el-table-column :label="t('actions')" width="310" fixed="right">
            <template #default="{ row }">
              <div class="inline-actions">
                <el-button size="small" :disabled="row.status === 'ACKNOWLEDGED'" @click="setIncidentStatus(row, 'ACKNOWLEDGED')">{{ t('ack') }}</el-button>
                <el-button size="small" type="success" :disabled="row.status === 'RESOLVED'" @click="setIncidentStatus(row, 'RESOLVED')">{{ t('resolve') }}</el-button>
                <el-button size="small" type="info" :disabled="row.status === 'OPEN'" @click="setIncidentStatus(row, 'OPEN')">{{ t('reopen') }}</el-button>
                <el-button size="small" type="primary" plain :loading="investigatingIncidentId === row.id" @click="investigateIncident(row)">
                  {{ resolveInvestigateLabel(row) }}
                </el-button>
              </div>
              <div class="mt-1">
                <el-button size="small" text type="primary" @click="openDeliveries(row)">{{ t('deliveries') }}</el-button>
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

      <div class="card-panel table-wrap ops-table workbench-col">
        <div class="section-head">
          <div>
            <h2 class="section-title">{{ t('investigationLane') }}</h2>
            <p class="section-subtitle">{{ t('investigationLaneDesc') }}</p>
          </div>
        </div>

        <el-table :data="investigations" v-loading="loadingInvestigations" height="100%">
          <el-table-column prop="id" label="ID" width="78" />
          <el-table-column prop="severity" :label="t('severity')" width="94" />
          <el-table-column prop="status" :label="t('status')" width="130" />
          <el-table-column prop="triggerSource" :label="t('source')" width="110" />
          <el-table-column :label="t('createdAt')" width="180">
            <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column :label="t('summary')" min-width="220">
            <template #default="{ row }">{{ row.summary || row.title || '-' }}</template>
          </el-table-column>
          <el-table-column :label="t('actions')" width="160" fixed="right">
            <template #default="{ row }">
              <el-button size="small" type="primary" plain @click="openInAiExpert(row.id)">
                {{ t('openAiExpert') }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
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
import { getIncidentDeliveries, getIncidents, updateIncidentStatus } from '../api/incidents'
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

const investigations = ref([])
const investigationIndexByIncident = ref(new Map())

const deliveryDialogVisible = ref(false)
const deliveryLoading = ref(false)
const deliveryRecords = ref([])
const deliveryPage = ref(1)
const deliveryPageSize = ref(10)
const deliveryTotal = ref(0)
const selectedIncident = ref(null)

const filters = reactive({
  status: '',
  metricName: '',
  keyword: ''
})

const { locale, t } = useI18n({
  title: { zh: '事件工作台', en: 'Event Workbench' },
  subtitle: { zh: '在同一界面完成告警处理、调查创建与 AI 处置闭环', en: 'Close the loop from incident handling to AI investigation in one workspace' },
  refresh: { zh: '刷新', en: 'Refresh' },
  openIncidents: { zh: '待处理告警', en: 'Open Incidents' },
  ackedIncidents: { zh: '已确认告警', en: 'Acknowledged Incidents' },
  resolvedIncidents: { zh: '已解决告警', en: 'Resolved Incidents' },
  openInvestigations: { zh: '进行中调查', en: 'Open Investigations' },
  incidentLane: { zh: '告警泳道', en: 'Incident Lane' },
  incidentLaneDesc: { zh: '先处理状态，再选择发起/进入调查', en: 'Process status first, then start or open investigation' },
  investigationLane: { zh: '调查泳道', en: 'Investigation Lane' },
  investigationLaneDesc: { zh: '调查队列集中查看，快速进入 AI 专家执行', en: 'Focus queue and jump into AI Expert execution' },
  status: { zh: '状态', en: 'Status' },
  metric: { zh: '指标', en: 'Metric' },
  keyword: { zh: '关键词', en: 'Keyword' },
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
  loadDeliveryFailed: { zh: '加载投递记录失败', en: 'Failed to load delivery records' }
})

const openIncidents = computed(() => incidents.value.filter(x => x.status === 'OPEN').length)
const ackedIncidents = computed(() => incidents.value.filter(x => x.status === 'ACKNOWLEDGED').length)
const resolvedIncidents = computed(() => incidents.value.filter(x => x.status === 'RESOLVED').length)
const openInvestigations = computed(() => investigations.value.filter(x => x.status !== 'CLOSED').length)

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
  return investigationIndexByIncident.value.has(incident.id) ? t('openInvestigation') : t('createInvestigation')
}

function openInAiExpert(investigationId) {
  router.push({ path: '/ai-expert', query: { investigationId: String(investigationId) } })
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
    const { data } = await updateIncidentStatus(row.id, status)
    row.status = data.status
    row.severity = data.severity || row.severity
    row.escalationLevel = data.escalationLevel
    row.nextNotifyAt = data.nextNotifyAt
    row.acknowledgedAt = data.acknowledgedAt
    row.resolvedAt = data.resolvedAt
    ElMessage.success(t('statusUpdated'))
  } catch (_e) {
    ElMessage.error(t('statusUpdateFailed'))
  }
}

async function investigateIncident(row) {
  const existing = investigationIndexByIncident.value.get(row.id)
  if (existing?.id) {
    openInAiExpert(existing.id)
    return
  }

  investigatingIncidentId.value = row.id
  try {
    const { data } = await createInvestigation({
      incidentId: row.id,
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
  selectedIncident.value = row
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
  min-height: 460px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.workbench-filters {
  display: grid;
  grid-template-columns: repeat(4, minmax(120px, 1fr));
  gap: 10px;
}

.workbench-pagination {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}

@media (min-width: 1560px) {
  .workbench-grid {
    grid-template-columns: 1.1fr 0.9fr;
    align-items: stretch;
  }
}

@media (max-width: 900px) {
  .workbench-filters {
    grid-template-columns: 1fr;
  }
}
</style>
