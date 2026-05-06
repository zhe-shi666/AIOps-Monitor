<template>
  <div class="page-surface incidents-page">
    <div class="page-hero incidents-hero">
      <div>
        <h1>{{ t('title') }}</h1>
        <p>{{ t('subtitle') }}</p>
      </div>
      <div class="inline-actions">
        <el-button @click="fetchIncidents" :loading="loading">{{ t('refresh') }}</el-button>
      </div>
    </div>

    <section class="kpi-grid incident-kpi-grid">
      <div class="kpi-item incident-kpi danger-tone">
        <p class="kpi-label">{{ t('openCount') }}</p>
        <p class="kpi-value text-rose-300">{{ openCount }}</p>
      </div>
      <div class="kpi-item incident-kpi warning-tone">
        <p class="kpi-label">{{ t('ackedCount') }}</p>
        <p class="kpi-value text-amber-300">{{ acknowledgedCount }}</p>
      </div>
      <div class="kpi-item incident-kpi success-tone">
        <p class="kpi-label">{{ t('resolvedCount') }}</p>
        <p class="kpi-value text-emerald-300">{{ resolvedCount }}</p>
      </div>
      <div class="kpi-item incident-kpi info-tone">
        <p class="kpi-label">{{ t('totalCount') }}</p>
        <p class="kpi-value text-cyan-300">{{ total }}</p>
      </div>
    </section>

    <section class="card-panel incident-filter-card">
      <div class="section-head incident-filter-head">
        <div>
          <h2 class="section-title">{{ t('filterTitle') }}</h2>
          <p class="section-subtitle">{{ t('filterSubtitle') }}</p>
        </div>
        <el-tag effect="plain" type="info">{{ t('currentScope') }} {{ total }}</el-tag>
      </div>

      <div class="incident-filter-grid">
        <el-select v-model="filters.status" clearable :placeholder="t('status')" @change="handleFilterChange">
          <el-option label="OPEN" value="OPEN" />
          <el-option label="ACKNOWLEDGED" value="ACKNOWLEDGED" />
          <el-option label="RESOLVED" value="RESOLVED" />
        </el-select>
        <el-select v-model="filters.metricName" clearable :placeholder="t('metric')" @change="handleFilterChange">
          <el-option label="CPU" value="CPU" />
          <el-option label="MEMORY" value="MEMORY" />
          <el-option label="DISK" value="DISK" />
          <el-option label="PROCESS_COUNT" value="PROCESS_COUNT" />
        </el-select>
        <el-input v-model="filters.hostname" :placeholder="t('hostname')" clearable @change="handleFilterChange" />
        <el-input v-model="filters.keyword" :placeholder="t('keyword')" clearable @change="handleFilterChange" />
        <div class="incident-filter-actions">
          <el-button type="primary" @click="handleFilterChange">{{ t('query') }}</el-button>
        </div>
      </div>
    </section>

    <section class="card-panel incident-table-card">
      <div class="section-head incident-table-head">
        <div>
          <h2 class="section-title">{{ t('listTitle') }}</h2>
          <p class="section-subtitle">{{ t('listSubtitle') }}</p>
        </div>
        <div v-if="!canOperate" class="readonly-tip">
          {{ readOnlyReason }}
        </div>
      </div>

      <div class="incident-batch-bar">
        <div class="incident-batch-summary">
          <strong>{{ t('selectedCount') }}</strong>
          <span>{{ selectedIncidents.length }} {{ t('items') }}</span>
        </div>
        <div class="incident-batch-actions">
          <el-button size="small" @click="clearSelection" :disabled="!selectedIncidents.length">
            {{ t('clearSelection') }}
          </el-button>
          <el-button
            size="small"
            :disabled="!canOperate || !selectedIncidents.length"
            :loading="batchLoading"
            @click="batchSetStatus('ACKNOWLEDGED')">
            {{ t('batchAck') }}
          </el-button>
          <el-button
            size="small"
            type="success"
            :disabled="!canOperate || !selectedIncidents.length"
            :loading="batchLoading"
            @click="batchSetStatus('RESOLVED')">
            {{ t('batchResolve') }}
          </el-button>
          <el-button
            size="small"
            type="info"
            :disabled="!canOperate || !selectedIncidents.length"
            :loading="batchLoading"
            @click="batchSetStatus('OPEN')">
            {{ t('batchReopen') }}
          </el-button>
        </div>
      </div>

      <div class="table-wrap ops-table incident-table-wrap">
        <el-table
          ref="incidentTableRef"
          :data="incidents"
          v-loading="loading"
          row-key="id"
          @selection-change="handleSelectionChange">
          <el-table-column type="selection" width="50" reserve-selection />
          <el-table-column type="expand" width="50">
            <template #default="{ row }">
              <div class="incident-expand-card">
                <div class="incident-expand-grid">
                  <div class="incident-expand-item">
                    <span>{{ t('incidentNo') }}</span>
                    <strong>#{{ row.id }}</strong>
                  </div>
                  <div class="incident-expand-item">
                    <span>{{ t('metric') }}</span>
                    <strong>{{ row.metricName || '-' }}</strong>
                  </div>
                  <div class="incident-expand-item">
                    <span>{{ t('hostname') }}</span>
                    <strong>{{ row.hostname || '-' }}</strong>
                  </div>
                  <div class="incident-expand-item">
                    <span>{{ t('createdAt') }}</span>
                    <strong>{{ formatDate(row.createdAt) }}</strong>
                  </div>
                  <div class="incident-expand-item">
                    <span>{{ t('metricValue') }}</span>
                    <strong>{{ formatMetric(row.metricValue) }} / {{ formatMetric(row.threshold) }}</strong>
                  </div>
                  <div class="incident-expand-item">
                    <span>{{ t('escalation') }}</span>
                    <strong>L{{ row.escalationLevel ?? 0 }}</strong>
                  </div>
                  <div class="incident-expand-item">
                    <span>{{ t('nextNotify') }}</span>
                    <strong>{{ formatDate(row.nextNotifyAt) }}</strong>
                  </div>
                  <div class="incident-expand-item">
                    <span>{{ t('status') }}</span>
                    <strong>{{ row.status || 'OPEN' }}</strong>
                  </div>
                </div>
                <div class="incident-expand-message">
                  <span>{{ t('message') }}</span>
                  <p>{{ row.message || '-' }}</p>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column :label="t('severity')" width="92">
            <template #default="{ row }">
              <el-tag :type="severityType(row.severity)" size="small" effect="dark">{{ row.severity || 'P2' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="t('coreInfo')" min-width="300">
            <template #default="{ row }">
              <div class="incident-core-cell">
                <strong>{{ buildHeadline(row) }}</strong>
                <span>{{ buildSubline(row) }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column :label="t('status')" width="120">
            <template #default="{ row }">
              <el-tag :type="statusType(row.status)" size="small">{{ row.status || 'OPEN' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="t('actions')" width="270">
            <template #default="{ row }">
              <div class="incident-action-row compact-actions">
                <el-button size="small" :disabled="!canOperate || row.status === 'ACKNOWLEDGED'" @click="setStatus(row, 'ACKNOWLEDGED')">
                  {{ t('ack') }}
                </el-button>
                <el-button size="small" type="success" :disabled="!canOperate || row.status === 'RESOLVED'" @click="setStatus(row, 'RESOLVED')">
                  {{ t('resolve') }}
                </el-button>
                <el-button size="small" type="primary" plain @click="openDeliveries(row)">
                  {{ t('deliveries') }}
                </el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="incident-pagination">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="fetchIncidents" />
      </div>
    </section>

    <el-dialog
      v-model="deliveryDialogVisible"
      :title="t('deliveryTitle')"
      width="980px"
      destroy-on-close>
      <div class="delivery-dialog-head">
        <div>
          <p class="delivery-dialog-label">{{ t('incidentNo') }}</p>
          <strong>#{{ selectedIncident?.id || '-' }}</strong>
        </div>
        <div>
          <p class="delivery-dialog-label">{{ t('metric') }}</p>
          <strong>{{ selectedIncident?.metricName || '-' }}</strong>
        </div>
        <div>
          <p class="delivery-dialog-label">{{ t('hostname') }}</p>
          <strong>{{ selectedIncident?.hostname || '-' }}</strong>
        </div>
      </div>
      <div class="ops-table table-wrap incident-table-wrap">
        <el-table :data="deliveryRecords" v-loading="deliveryLoading">
          <el-table-column prop="channelName" :label="t('channelName')" width="170" />
          <el-table-column prop="status" :label="t('result')" width="120">
            <template #default="{ row }">
              <el-tag :type="row.status === 'SUCCESS' ? 'success' : 'danger'" size="small">
                {{ row.status }}
              </el-tag>
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

      <div class="incident-pagination">
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
import { ElMessage } from 'element-plus'
import { getIncidentDeliveries, getIncidents, updateIncidentStatus } from '../api/incidents'
import { useI18n } from '../composables/useI18n'
import { usePermissions } from '../composables/usePermissions'

const loading = ref(false)
const incidents = ref([])
const incidentTableRef = ref(null)
const selectedIncidents = ref([])
const batchLoading = ref(false)
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)
const deliveryDialogVisible = ref(false)
const deliveryLoading = ref(false)
const deliveryRecords = ref([])
const deliveryPage = ref(1)
const deliveryPageSize = ref(10)
const deliveryTotal = ref(0)
const selectedIncident = ref(null)
const { canOperate, readOnlyReason } = usePermissions()

const filters = reactive({
  status: '',
  metricName: '',
  hostname: '',
  keyword: ''
})

const openCount = computed(() => incidents.value.filter((item) => item.status === 'OPEN').length)
const acknowledgedCount = computed(() => incidents.value.filter((item) => item.status === 'ACKNOWLEDGED').length)
const resolvedCount = computed(() => incidents.value.filter((item) => item.status === 'RESOLVED').length)

const { locale, t } = useI18n({
  title: { zh: '告警中心', en: 'Incident Center' },
  subtitle: { zh: '统一查看事件状态、升级节奏与通知投递情况，适合值班与人工处置。', en: 'Review incident status, escalation cadence, and notification delivery in one operational view.' },
  refresh: { zh: '刷新', en: 'Refresh' },
  status: { zh: '状态', en: 'Status' },
  metric: { zh: '指标', en: 'Metric' },
  hostname: { zh: '主机名', en: 'Hostname' },
  keyword: { zh: '关键词', en: 'Keyword' },
  query: { zh: '查询', en: 'Query' },
  selectedCount: { zh: '已选事件', en: 'Selected Incidents' },
  items: { zh: '条', en: 'items' },
  clearSelection: { zh: '清空选择', en: 'Clear Selection' },
  batchAck: { zh: '批量确认', en: 'Batch Ack' },
  batchResolve: { zh: '批量解决', en: 'Batch Resolve' },
  batchReopen: { zh: '批量重开', en: 'Batch Reopen' },
  severity: { zh: '级别', en: 'Severity' },
  coreInfo: { zh: '重点信息', en: 'Key Details' },
  metricValue: { zh: '值/阈值', en: 'Value/Threshold' },
  message: { zh: '消息', en: 'Message' },
  escalation: { zh: '升级', en: 'Escalation' },
  nextNotify: { zh: '下次', en: 'Next' },
  createdAt: { zh: '发生时间', en: 'Created At' },
  actions: { zh: '操作', en: 'Actions' },
  ack: { zh: '确认', en: 'Acknowledge' },
  resolve: { zh: '解决', en: 'Resolve' },
  deliveries: { zh: '投递记录', en: 'Deliveries' },
  deliveryTitle: { zh: '通知投递记录', en: 'Delivery Records' },
  incidentNo: { zh: '事件', en: 'Incident' },
  channelName: { zh: '通道名称', en: 'Channel' },
  result: { zh: '结果', en: 'Result' },
  target: { zh: '目标', en: 'Target' },
  errorMessage: { zh: '错误信息', en: 'Error Message' },
  time: { zh: '时间', en: 'Time' },
  filterTitle: { zh: '事件筛选', en: 'Incident Filters' },
  filterSubtitle: { zh: '按状态、指标、主机和关键词快速缩小排查范围。', en: 'Narrow the incident list quickly by status, metric, host, and keyword.' },
  listTitle: { zh: '事件列表', en: 'Incident List' },
  listSubtitle: { zh: '主表格仅保留处置必需信息，其余详情统一折叠展开。', en: 'Keep only essential operational data in the main grid and fold the rest into expandable details.' },
  currentScope: { zh: '当前结果数：', en: 'Current results:' },
  openCount: { zh: '待处理事件', en: 'Open Incidents' },
  ackedCount: { zh: '已确认事件', en: 'Acknowledged' },
  resolvedCount: { zh: '已解决事件', en: 'Resolved' },
  totalCount: { zh: '总事件数', en: 'Total Incidents' },
  loadFailed: { zh: '加载告警失败', en: 'Failed to load incidents' },
  statusUpdated: { zh: '状态已更新', en: 'Status updated' },
  batchUpdated: { zh: '批量状态已更新', en: 'Batch status updated' },
  statusUpdateFailed: { zh: '更新状态失败', en: 'Failed to update status' },
  batchUpdateFailed: { zh: '批量更新失败', en: 'Batch update failed' },
  loadDeliveryFailed: { zh: '加载投递记录失败', en: 'Failed to load delivery records' }
})

async function fetchIncidents() {
  loading.value = true
  try {
    const params = {
      page: page.value - 1,
      size: pageSize.value,
      status: filters.status || undefined,
      metricName: filters.metricName || undefined,
      hostname: filters.hostname || undefined,
      keyword: filters.keyword || undefined
    }
    const { data } = await getIncidents(params)
    incidents.value = data.content || []
    total.value = data.totalElements || 0
    selectedIncidents.value = []
  } catch (_e) {
    ElMessage.error(t('loadFailed'))
  } finally {
    loading.value = false
  }
}

function handleFilterChange() {
  page.value = 1
  fetchIncidents()
}

function handleSelectionChange(rows) {
  selectedIncidents.value = rows
}

function clearSelection() {
  incidentTableRef.value?.clearSelection()
  selectedIncidents.value = []
}

async function patchIncidentStatus(row, status, silent = false) {
  const { data } = await updateIncidentStatus(row.id, status)
  row.status = data.status
  row.severity = data.severity || row.severity
  row.escalationLevel = data.escalationLevel
  row.nextNotifyAt = data.nextNotifyAt
  row.acknowledgedAt = data.acknowledgedAt
  row.resolvedAt = data.resolvedAt
  if (!silent) {
    ElMessage.success(t('statusUpdated'))
  }
}

async function setStatus(row, status) {
  if (!canOperate.value) {
    ElMessage.warning(readOnlyReason.value)
    return
  }
  try {
    await patchIncidentStatus(row, status)
  } catch (_e) {
    ElMessage.error(t('statusUpdateFailed'))
  }
}

async function batchSetStatus(status) {
  if (!canOperate.value) {
    ElMessage.warning(readOnlyReason.value)
    return
  }
  if (!selectedIncidents.value.length) return

  batchLoading.value = true
  try {
    await Promise.all(selectedIncidents.value.map((row) => patchIncidentStatus(row, status, true)))
    ElMessage.success(t('batchUpdated'))
    clearSelection()
  } catch (_e) {
    ElMessage.error(t('batchUpdateFailed'))
  } finally {
    batchLoading.value = false
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
    const params = {
      page: deliveryPage.value - 1,
      size: deliveryPageSize.value
    }
    const { data } = await getIncidentDeliveries(selectedIncident.value.id, params)
    deliveryRecords.value = data.content || []
    deliveryTotal.value = data.totalElements || 0
  } catch (_e) {
    ElMessage.error(t('loadDeliveryFailed'))
  } finally {
    deliveryLoading.value = false
  }
}

function statusType(status) {
  if (status === 'RESOLVED') return 'success'
  if (status === 'ACKNOWLEDGED') return 'warning'
  return 'danger'
}

function severityType(severity) {
  if (severity === 'P1') return 'danger'
  if (severity === 'P2') return 'warning'
  return 'info'
}

function formatDate(value) {
  if (!value) return '-'
  const localeCode = locale.value === 'zh' ? 'zh-CN' : 'en-US'
  return new Date(value).toLocaleString(localeCode)
}

function formatMetric(value) {
  if (value == null) return '-'
  return Number(value).toFixed(2)
}

function buildHeadline(row) {
  return row.message || `${row.metricName || '-'} incident`
}

function buildSubline(row) {
  const parts = [row.hostname || '-', row.metricName || '-', `${t('metricValue')} ${formatMetric(row.metricValue)}/${formatMetric(row.threshold)}`]
  return parts.join(' · ')
}

onMounted(fetchIncidents)
</script>

<style scoped>
.incidents-page {
  gap: 20px;
}

.incidents-hero {
  background: radial-gradient(760px 220px at 0 0, rgba(251, 113, 133, 0.12), transparent 60%), var(--hero-bg);
}

.incident-kpi-grid {
  gap: 14px;
}

.incident-kpi {
  min-height: 112px;
}

.incident-filter-card,
.incident-table-card {
  padding: 20px;
}

.incident-filter-head,
.incident-table-head {
  margin-bottom: 18px;
}

.incident-filter-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr)) auto;
  gap: 12px;
  align-items: center;
}

.incident-filter-actions {
  display: flex;
  justify-content: flex-end;
}

.incident-batch-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px;
  margin-bottom: 16px;
  border: 1px solid var(--line);
  border-radius: 16px;
  background: var(--panel-soft);
}

.incident-batch-summary {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--text-2);
}

.incident-batch-summary strong {
  color: var(--text-1);
}

.incident-batch-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
}

.incident-table-wrap {
  padding: 0;
}

.incident-core-cell {
  display: grid;
  gap: 6px;
  padding-right: 8px;
}

.incident-core-cell strong {
  color: var(--text-1);
  line-height: 1.5;
}

.incident-core-cell span {
  font-size: 12px;
  color: var(--text-3);
  line-height: 1.5;
}

.incident-expand-card {
  margin: 6px 0;
  padding: 16px;
  border-radius: 16px;
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.03);
}

.incident-expand-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 14px;
}

.incident-expand-item {
  display: grid;
  gap: 6px;
}

.incident-expand-item span,
.incident-expand-message span {
  font-size: 12px;
  color: var(--text-3);
}

.incident-expand-item strong,
.incident-expand-message p {
  color: var(--text-1);
}

.incident-expand-message {
  display: grid;
  gap: 8px;
}

.incident-expand-message p {
  margin: 0;
  padding: 12px 14px;
  border-radius: 14px;
  background: var(--panel-soft);
  line-height: 1.6;
}

.incident-action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.compact-actions :deep(.el-button) {
  margin-left: 0;
}

.incident-pagination {
  display: flex;
  justify-content: flex-end;
  padding-top: 18px;
}

.readonly-tip {
  max-width: 360px;
  padding: 10px 12px;
  border-radius: 12px;
  background: rgba(245, 158, 11, 0.08);
  border: 1px solid rgba(245, 158, 11, 0.2);
  color: var(--text-3);
  font-size: 12px;
}

.delivery-dialog-head {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 18px;
}

.delivery-dialog-head > div {
  padding: 12px 14px;
  border-radius: 14px;
  border: 1px solid var(--line);
  background: var(--panel-soft);
}

.delivery-dialog-label {
  margin: 0 0 6px;
  font-size: 11px;
  color: var(--text-3);
}

.delivery-dialog-head strong {
  color: var(--text-1);
}

@media (max-width: 980px) {
  .incident-filter-grid {
    grid-template-columns: 1fr 1fr;
  }

  .incident-batch-bar {
    flex-direction: column;
    align-items: stretch;
  }

  .incident-batch-actions {
    justify-content: stretch;
  }

  .incident-filter-actions {
    grid-column: span 2;
    justify-content: stretch;
  }

  .incident-filter-actions :deep(.el-button) {
    width: 100%;
  }

  .delivery-dialog-head {
    grid-template-columns: 1fr;
  }

  .incident-expand-grid {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 640px) {
  .incident-filter-card,
  .incident-table-card {
    padding: 16px;
  }

  .incident-filter-grid {
    grid-template-columns: 1fr;
  }

  .incident-filter-actions {
    grid-column: auto;
  }

  .incident-expand-grid {
    grid-template-columns: 1fr;
  }
}
</style>
