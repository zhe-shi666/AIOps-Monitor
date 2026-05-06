<template>
  <div class="page-surface">
    <div class="page-hero">
      <div>
        <h1>{{ t('title') }}</h1>
        <p>{{ t('subtitle') }}</p>
      </div>
      <el-button @click="fetchIncidents" :loading="loading">{{ t('refresh') }}</el-button>
    </div>

    <div class="card-panel p-4">
      <div class="grid grid-cols-1 md:grid-cols-5 gap-3">
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
        <el-button type="primary" @click="handleFilterChange">{{ t('query') }}</el-button>
      </div>
    </div>

    <div class="card-panel table-wrap ops-table">
      <el-table :data="incidents" v-loading="loading">
        <el-table-column prop="id" label="ID" width="90" />
        <el-table-column :label="t('severity')" width="100">
          <template #default="{ row }">
            <el-tag :type="severityType(row.severity)" size="small">{{ row.severity || 'P2' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="metricName" :label="t('metric')" width="140" />
        <el-table-column :label="t('metricValue')" width="160">
          <template #default="{ row }">
            <span class="font-mono">{{ formatMetric(row.metricValue) }} / {{ formatMetric(row.threshold) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="hostname" :label="t('hostname')" width="180" />
        <el-table-column prop="message" :label="t('message')" min-width="240" />
        <el-table-column :label="t('status')" width="150">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ row.status || 'OPEN' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('escalation')" width="170">
          <template #default="{ row }">
            <div class="text-xs leading-5">
              <div>L{{ row.escalationLevel ?? 0 }}</div>
              <div class="text-slate-400">{{ t('nextNotify') }}：{{ formatDate(row.nextNotifyAt) }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column :label="t('createdAt')" width="180">
          <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column :label="t('actions')" width="300" fixed="right">
          <template #default="{ row }">
            <div class="flex items-center gap-2">
              <el-button size="small" :disabled="!canOperate || row.status === 'ACKNOWLEDGED'" @click="setStatus(row, 'ACKNOWLEDGED')">
                {{ t('ack') }}
              </el-button>
              <el-button size="small" type="success" :disabled="!canOperate || row.status === 'RESOLVED'" @click="setStatus(row, 'RESOLVED')">
                {{ t('resolve') }}
              </el-button>
              <el-button size="small" type="info" :disabled="!canOperate || row.status === 'OPEN'" @click="setStatus(row, 'OPEN')">
                {{ t('reopen') }}
              </el-button>
              <el-button size="small" type="primary" plain @click="openDeliveries(row)">
                {{ t('deliveries') }}
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="flex justify-end mt-4">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="fetchIncidents" />
      </div>
    </div>

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

      <div class="flex justify-end mt-4">
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
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getIncidentDeliveries, getIncidents, updateIncidentStatus } from '../api/incidents'
import { useI18n } from '../composables/useI18n'
import { usePermissions } from '../composables/usePermissions'

const loading = ref(false)
const incidents = ref([])
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

const { locale, t } = useI18n({
  title: { zh: '告警中心', en: 'Incident Center' },
  subtitle: { zh: '查看与处理当前用户的告警事件', en: 'View and handle incidents for current user' },
  refresh: { zh: '刷新', en: 'Refresh' },
  status: { zh: '状态', en: 'Status' },
  metric: { zh: '指标', en: 'Metric' },
  hostname: { zh: '主机名', en: 'Hostname' },
  keyword: { zh: '关键词', en: 'Keyword' },
  query: { zh: '查询', en: 'Query' },
  severity: { zh: '级别', en: 'Severity' },
  metricValue: { zh: '值/阈值', en: 'Value/Threshold' },
  message: { zh: '消息', en: 'Message' },
  escalation: { zh: '升级', en: 'Escalation' },
  nextNotify: { zh: '下次', en: 'Next' },
  createdAt: { zh: '发生时间', en: 'Created At' },
  actions: { zh: '操作', en: 'Actions' },
  ack: { zh: '确认', en: 'Acknowledge' },
  resolve: { zh: '解决', en: 'Resolve' },
  reopen: { zh: '重开', en: 'Reopen' },
  deliveries: { zh: '投递记录', en: 'Deliveries' },
  deliveryTitle: { zh: '通知投递记录', en: 'Delivery Records' },
  incidentNo: { zh: '事件', en: 'Incident' },
  channelName: { zh: '通道名称', en: 'Channel' },
  result: { zh: '结果', en: 'Result' },
  target: { zh: '目标', en: 'Target' },
  errorMessage: { zh: '错误信息', en: 'Error Message' },
  time: { zh: '时间', en: 'Time' },
  loadFailed: { zh: '加载告警失败', en: 'Failed to load incidents' },
  statusUpdated: { zh: '状态已更新', en: 'Status updated' },
  statusUpdateFailed: { zh: '更新状态失败', en: 'Failed to update status' },
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

async function setStatus(row, status) {
  if (!canOperate.value) {
    ElMessage.warning(readOnlyReason.value)
    return
  }
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

onMounted(fetchIncidents)
</script>
