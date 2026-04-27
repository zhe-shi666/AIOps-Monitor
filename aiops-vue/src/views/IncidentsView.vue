<template>
  <div class="min-h-screen bg-slate-950 text-slate-100 p-8 pt-24">
    <div class="max-w-7xl mx-auto space-y-6">
      <div class="flex items-center justify-between">
        <div>
          <h1 class="text-2xl font-bold tracking-tight">告警中心</h1>
          <p class="text-sm text-slate-400 mt-1">查看与处理当前用户的告警事件</p>
        </div>
        <el-button @click="fetchIncidents" :loading="loading">刷新</el-button>
      </div>

      <div class="rounded-xl border border-slate-700 bg-slate-900/50 p-4">
        <div class="grid grid-cols-1 md:grid-cols-5 gap-3">
          <el-select v-model="filters.status" clearable placeholder="状态" @change="handleFilterChange">
            <el-option label="OPEN" value="OPEN" />
            <el-option label="ACKNOWLEDGED" value="ACKNOWLEDGED" />
            <el-option label="RESOLVED" value="RESOLVED" />
          </el-select>
          <el-select v-model="filters.metricName" clearable placeholder="指标" @change="handleFilterChange">
            <el-option label="CPU" value="CPU" />
            <el-option label="MEMORY" value="MEMORY" />
            <el-option label="DISK" value="DISK" />
            <el-option label="PROCESS_COUNT" value="PROCESS_COUNT" />
          </el-select>
          <el-input v-model="filters.hostname" placeholder="主机名" clearable @change="handleFilterChange" />
          <el-input v-model="filters.keyword" placeholder="关键词" clearable @change="handleFilterChange" />
          <el-button type="primary" @click="handleFilterChange">查询</el-button>
        </div>
      </div>

      <div class="rounded-xl border border-slate-700 bg-slate-900/50 p-4">
        <el-table
          :data="incidents"
          v-loading="loading"
          :header-cell-style="{ background: '#1e293b', color: '#94a3b8' }"
          :row-style="{ background: '#0f172a', color: '#e2e8f0' }">
          <el-table-column prop="id" label="ID" width="90" />
          <el-table-column label="级别" width="100">
            <template #default="{ row }">
              <el-tag :type="severityType(row.severity)" size="small">{{ row.severity || 'P2' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="metricName" label="指标" width="140" />
          <el-table-column label="值/阈值" width="160">
            <template #default="{ row }">
              <span class="font-mono">{{ formatMetric(row.metricValue) }} / {{ formatMetric(row.threshold) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="hostname" label="主机名" width="180" />
          <el-table-column prop="message" label="消息" min-width="240" />
          <el-table-column label="状态" width="150">
            <template #default="{ row }">
              <el-tag :type="statusType(row.status)" size="small">{{ row.status || 'OPEN' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="升级" width="170">
            <template #default="{ row }">
              <div class="text-xs leading-5">
                <div>L{{ row.escalationLevel ?? 0 }}</div>
                <div class="text-slate-400">下次：{{ formatDate(row.nextNotifyAt) }}</div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="发生时间" width="180">
            <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="300" fixed="right">
            <template #default="{ row }">
              <div class="flex items-center gap-2">
                <el-button size="small" :disabled="row.status === 'ACKNOWLEDGED'" @click="setStatus(row, 'ACKNOWLEDGED')">
                  确认
                </el-button>
                <el-button size="small" type="success" :disabled="row.status === 'RESOLVED'" @click="setStatus(row, 'RESOLVED')">
                  解决
                </el-button>
                <el-button size="small" type="info" :disabled="row.status === 'OPEN'" @click="setStatus(row, 'OPEN')">
                  重开
                </el-button>
                <el-button size="small" type="primary" plain @click="openDeliveries(row)">
                  投递记录
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
    </div>

    <el-dialog
      v-model="deliveryDialogVisible"
      title="通知投递记录"
      width="980px"
      destroy-on-close>
      <div class="mb-3 text-xs text-slate-500">
        事件 #{{ selectedIncident?.id || '-' }} · {{ selectedIncident?.metricName || '-' }} · {{ selectedIncident?.hostname || '-' }}
      </div>
      <el-table
        :data="deliveryRecords"
        v-loading="deliveryLoading"
        :header-cell-style="{ background: '#1e293b', color: '#94a3b8' }"
        :row-style="{ background: '#0f172a', color: '#e2e8f0' }">
        <el-table-column prop="channelName" label="通道名称" width="160" />
        <el-table-column prop="status" label="结果" width="120">
          <template #default="{ row }">
            <el-tag :type="row.status === 'SUCCESS' ? 'success' : 'danger'" size="small">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="httpStatus" label="HTTP" width="100" />
        <el-table-column prop="target" label="目标" min-width="240" />
        <el-table-column label="错误信息" min-width="220">
          <template #default="{ row }">{{ row.errorMessage || '-' }}</template>
        </el-table-column>
        <el-table-column label="时间" width="180">
          <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
        </el-table-column>
      </el-table>

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

const filters = reactive({
  status: '',
  metricName: '',
  hostname: '',
  keyword: ''
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
  } catch (e) {
    ElMessage.error('加载告警失败')
  } finally {
    loading.value = false
  }
}

function handleFilterChange() {
  page.value = 1
  fetchIncidents()
}

async function setStatus(row, status) {
  try {
    const { data } = await updateIncidentStatus(row.id, status)
    row.status = data.status
    row.severity = data.severity || row.severity
    row.escalationLevel = data.escalationLevel
    row.nextNotifyAt = data.nextNotifyAt
    row.acknowledgedAt = data.acknowledgedAt
    row.resolvedAt = data.resolvedAt
    ElMessage.success('状态已更新')
  } catch (e) {
    ElMessage.error('更新状态失败')
  }
}

function openDeliveries(row) {
  selectedIncident.value = row
  deliveryPage.value = 1
  deliveryDialogVisible.value = true
  fetchDeliveries()
}

async function fetchDeliveries() {
  if (!selectedIncident.value?.id) {
    return
  }
  deliveryLoading.value = true
  try {
    const params = {
      page: deliveryPage.value - 1,
      size: deliveryPageSize.value
    }
    const { data } = await getIncidentDeliveries(selectedIncident.value.id, params)
    deliveryRecords.value = data.content || []
    deliveryTotal.value = data.totalElements || 0
  } catch (e) {
    ElMessage.error('加载投递记录失败')
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
  return new Date(value).toLocaleString('zh-CN')
}

function formatMetric(value) {
  if (value == null) return '-'
  return Number(value).toFixed(2)
}

onMounted(fetchIncidents)
</script>
