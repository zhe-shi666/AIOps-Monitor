<template>
  <div class="page-surface">
    <header class="page-hero">
      <div>
        <h1>
          {{ mode === 'standalone'
            ? (isZh ? '节点监控总览' : 'Node Monitor Overview')
            : (isZh ? '集群监控总览' : 'Cluster Monitor Overview')
          }}
        </h1>
        <p>
          {{ mode === 'standalone'
            ? (isZh ? '单机实时监控与智能诊断' : 'Single-host monitoring and diagnostics')
            : (isZh ? '分布式监控与协同分析' : 'Distributed monitoring and collaborative analysis')
          }}
        </p>
      </div>
      <div class="flex items-center gap-3 flex-wrap">
        <span class="user-chip" :class="wsConnected ? '!border-emerald-300/35 !bg-emerald-500/15 !text-emerald-100' : '!border-rose-300/35 !bg-rose-500/15 !text-rose-100'">
          <span class="inline-block h-2 w-2 rounded-full" :style="{ background: wsConnected ? '#34d399' : '#fb7185' }"></span>
          <span>{{ wsConnected ? (isZh ? 'WebSocket 在线' : 'WebSocket Online') : (isZh ? 'WebSocket 断开' : 'WebSocket Offline') }}</span>
        </span>
      </div>
    </header>

    <main class="grid grid-cols-1 2xl:grid-cols-12 gap-6 relative z-10">
      <section class="2xl:col-span-8">
        <div class="card-panel-strong p-4 md:p-8">
          <div class="flex justify-between items-center px-4">
            <h2 class="tone-subtle font-medium tracking-widest uppercase text-sm pl-2">
              {{ isZh ? '系统实时指标' : 'Real-time Metrics' }}
            </h2>
            <div class="flex gap-6 font-mono text-sm pr-2">
              <span class="text-cyan-400 font-semibold">
                {{ isZh ? '节点数' : 'Nodes' }}: {{ nodeCount }}
              </span>
              <span class="text-purple-400 font-semibold">{{ isZh ? '数据点' : 'Points' }}: {{ totalDataPoints }}</span>
            </div>
          </div>

          <div class="flex flex-wrap gap-2 mt-6 mb-8 px-4">
            <button
              v-for="metric in metricOptions"
              :key="metric.key"
              @click="selectedMetric = metric.key"
              class="metric-pill px-3 py-1.5 rounded-lg border text-xs font-semibold transition-all duration-200 cursor-pointer"
              :class="selectedMetric === metric.key
                ? 'metric-pill-active'
                : 'metric-pill-idle'">
              {{ isZh ? metric.labelZh : metric.labelEn }}
            </button>
          </div>

          <div class="flex justify-between items-center mb-4 px-4">
            <p class="text-xs tone-muted">
              {{ isZh ? '当前指标' : 'Current Metric' }}:
              <span class="text-cyan-300 font-semibold">{{ selectedMetricLabel }}</span>
            </p>
            <p class="text-xs tone-muted font-mono">
              {{ isZh ? '单位' : 'Unit' }}: {{ selectedMetricUnit }}
            </p>
          </div>

          <div ref="chartRef" class="chart-surface h-80 w-full mt-6" style="min-height: 320px;"></div>
        </div>
      </section>

      <section class="2xl:col-span-4">
        <div class="card-panel p-6 space-y-4">
          <div class="section-head mb-0">
            <div>
              <h2 class="section-title text-base">
                {{ mode === 'standalone'
                  ? (isZh ? '节点硬件概览' : 'Node Snapshot')
                  : (isZh ? '集群运行概览' : 'Cluster Snapshot')
                }}
              </h2>
              <p class="section-subtitle">
                {{ mode === 'standalone'
                  ? (isZh ? '核心硬件与采集状态' : 'Core hardware and collector state')
                  : (isZh ? '节点规模与异常分布' : 'Node scale and anomaly distribution')
                }}
              </p>
            </div>
          </div>

          <div v-if="mode === 'standalone'" class="kpi-grid">
            <div class="kpi-item">
              <p class="kpi-label">{{ isZh ? 'CPU 型号' : 'CPU Model' }}</p>
              <p class="kpi-value text-lg mt-2">{{ hardwareCpuModel }}</p>
            </div>
            <div class="kpi-item">
              <p class="kpi-label">{{ isZh ? '总内存' : 'Total RAM' }}</p>
              <p class="kpi-value text-emerald-300">{{ hardwareMemory }}</p>
            </div>
          </div>

          <div v-else class="kpi-grid">
            <div class="kpi-item">
              <p class="kpi-label">{{ isZh ? '在线节点' : 'Online Nodes' }}</p>
              <p class="kpi-value text-violet-300">{{ nodeCount }}</p>
            </div>
            <div class="kpi-item">
              <p class="kpi-label">{{ isZh ? '异常节点' : 'Abnormal Nodes' }}</p>
              <p class="kpi-value text-rose-300">{{ abnormalNodes }}</p>
            </div>
            <div class="kpi-item">
              <p class="kpi-label">{{ isZh ? '平均值' : 'Average' }}</p>
              <p class="kpi-value text-cyan-300">{{ averageMetricDisplay }}</p>
            </div>
            <div class="kpi-item">
              <p class="kpi-label">{{ isZh ? '峰值' : 'Max' }}</p>
              <p class="kpi-value text-amber-300">{{ maxMetricDisplay }}</p>
            </div>
          </div>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, watch, computed, nextTick } from 'vue'
import * as echarts from 'echarts'
import SockJS from 'sockjs-client'
import Stomp from 'stompjs'
import { useAuthStore } from '../stores/auth'
import { getHardwareInfo } from '../api/system'
import { WS_BASE_URL } from '../config/env'
import { useLocaleMode } from '../composables/useLocaleMode'

const SUPPORTED_METRICS = ['CPU', 'MEMORY', 'DISK', 'NET_RX', 'NET_TX', 'PROCESS_COUNT']

const METRIC_META = {
  CPU: { labelZh: 'CPU 使用率', labelEn: 'CPU Usage', unit: '%', normalHigh: 80 },
  MEMORY: { labelZh: '内存使用率', labelEn: 'Memory Usage', unit: '%', normalHigh: 80 },
  DISK: { labelZh: '磁盘使用率', labelEn: 'Disk Usage', unit: '%', normalHigh: 85 },
  NET_RX: { labelZh: '网络入流量', labelEn: 'Network RX', unit: 'B/s', normalHigh: null },
  NET_TX: { labelZh: '网络出流量', labelEn: 'Network TX', unit: 'B/s', normalHigh: null },
  PROCESS_COUNT: { labelZh: '进程数量', labelEn: 'Process Count', unit: 'count', normalHigh: 350 }
}

const auth = useAuthStore()
const { locale } = useLocaleMode()
const isZh = computed(() => locale.value === 'zh')

const wsConnected = ref(false)
const selectedMetric = ref(localStorage.getItem('selectedMetric') || 'CPU')

watch(selectedMetric, () => {
  localStorage.setItem('selectedMetric', selectedMetric.value)
  if (myChart) myChart.setOption(getChartOption(), true)
})

const chartRef = ref(null)
let myChart = null
const metricSeries = reactive({})
const metricLabels = reactive({})
const mode = ref('standalone')

const hardwareInfo = ref({
  cpuModel: '-',
  totalMemoryBytes: 0
})

for (const metric of SUPPORTED_METRICS) {
  metricSeries[metric] = {}
  metricLabels[metric] = []
}

const metricOptions = computed(() => SUPPORTED_METRICS.map((key) => ({
  key,
  labelZh: METRIC_META[key].labelZh,
  labelEn: METRIC_META[key].labelEn
})))

const selectedMetricMeta = computed(() => METRIC_META[selectedMetric.value] || METRIC_META.CPU)
const selectedMetricLabel = computed(() =>
  isZh.value ? selectedMetricMeta.value.labelZh : selectedMetricMeta.value.labelEn)
const selectedMetricUnit = computed(() => selectedMetricMeta.value.unit)

const activeNodesData = computed(() => metricSeries[selectedMetric.value] || {})
const activeLabels = computed(() => metricLabels[selectedMetric.value] || [])

const nodeCount = computed(() => Object.keys(activeNodesData.value).length)
const totalDataPoints = computed(() =>
  Object.values(activeNodesData.value).reduce((total, data) => total + data.length, 0))

const abnormalNodes = computed(() => {
  const threshold = selectedMetricMeta.value.normalHigh
  if (threshold == null) return 0
  return Object.keys(activeNodesData.value).filter((h) => {
    const d = activeNodesData.value[h]
    return d.length > 0 && d[d.length - 1] > threshold
  }).length
})

const averageMetric = computed(() => {
  const vals = Object.values(activeNodesData.value)
    .map((d) => d.length > 0 ? d[d.length - 1] : 0)
    .filter((v) => v > 0)
  if (!vals.length) return 0
  return vals.reduce((s, v) => s + v, 0) / vals.length
})

const maxMetric = computed(() => {
  const vals = Object.values(activeNodesData.value)
    .map((d) => d.length > 0 ? d[d.length - 1] : 0)
    .filter((v) => v > 0)
  return vals.length ? Math.max(...vals) : 0
})

const averageMetricDisplay = computed(() => formatMetricValue(averageMetric.value, selectedMetric.value))
const maxMetricDisplay = computed(() => formatMetricValue(maxMetric.value, selectedMetric.value))

const hardwareCpuModel = computed(() => hardwareInfo.value.cpuModel || '-')
const hardwareMemory = computed(() => formatBytes(hardwareInfo.value.totalMemoryBytes))

const getNodeColor = (hostname) => {
  const colors = ['#22d3ee', '#a855f7', '#f59e0b', '#10b981', '#ef4444', '#8b5cf6', '#ec4899', '#14b8a6']
  const index = hostname.split('').reduce((acc, c) => acc + c.charCodeAt(0), 0) % colors.length
  return colors[index]
}

const formatBytes = (bytes) => {
  if (!bytes || bytes <= 0) return '-'
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  let size = bytes
  let i = 0
  while (size >= 1024 && i < units.length - 1) {
    size /= 1024
    i += 1
  }
  return `${size.toFixed(i >= 3 ? 1 : 0)}${units[i]}`
}

const formatMetricValue = (value, metricName) => {
  if (!value || value <= 0) return '-'
  if (metricName === 'CPU' || metricName === 'MEMORY' || metricName === 'DISK') {
    return `${value.toFixed(1)}%`
  }
  if (metricName === 'NET_RX' || metricName === 'NET_TX') {
    return `${formatBytes(value)}/s`
  }
  if (metricName === 'PROCESS_COUNT') {
    return `${Math.round(value)}`
  }
  return value.toFixed(1)
}

const loadHardwareInfo = async () => {
  try {
    const { data } = await getHardwareInfo()
    hardwareInfo.value = {
      cpuModel: data?.cpuModel || '-',
      totalMemoryBytes: Number(data?.totalMemoryBytes || 0)
    }
    if (data?.mode) mode.value = data.mode
  } catch (_e) {
    hardwareInfo.value = {
      cpuModel: '-',
      totalMemoryBytes: 0
    }
  }
}

const normalizeMetricName = (name) => {
  if (!name) return null
  const n = String(name).toUpperCase()
  if (n === 'MEM') return 'MEMORY'
  if (n === 'PROCESS') return 'PROCESS_COUNT'
  if (SUPPORTED_METRICS.includes(n)) return n
  return null
}

const getYAxisOption = () => {
  const metricName = selectedMetric.value
  if (metricName === 'CPU' || metricName === 'MEMORY' || metricName === 'DISK') {
    return {
      type: 'value',
      max: 100,
      splitLine: { lineStyle: { color: '#1e293b' } },
      axisLabel: { color: '#64748b', formatter: '{value}%' }
    }
  }
  if (metricName === 'NET_RX' || metricName === 'NET_TX') {
    return {
      type: 'value',
      splitLine: { lineStyle: { color: '#1e293b' } },
      axisLabel: {
        color: '#64748b',
        formatter: (value) => formatBytes(value)
      }
    }
  }
  return {
    type: 'value',
    splitLine: { lineStyle: { color: '#1e293b' } },
    axisLabel: { color: '#64748b', formatter: (value) => Math.round(value) }
  }
}

const getChartOption = () => {
  const nodes = activeNodesData.value
  const labels = activeLabels.value
  const series = Object.keys(nodes).map((hostname) => ({
    name: hostname,
    type: 'line',
    smooth: true,
    showSymbol: false,
    lineStyle: { width: 3, color: getNodeColor(hostname), shadowColor: getNodeColor(hostname), shadowBlur: 10 },
    areaStyle: {
      color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
        { offset: 0, color: getNodeColor(hostname) + '4d' },
        { offset: 1, color: getNodeColor(hostname) + '00' }
      ])
    },
    data: nodes[hostname] || []
  }))

  return {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'axis',
      backgroundColor: '#1e293b',
      borderColor: 'rgba(34,211,238,0.3)',
      borderWidth: 1,
      textStyle: { color: '#fff' },
      formatter: (params) => {
        if (!params || !params.length) return ''
        const title = params[0].axisValue
        const lines = params.map((p) => `${p.marker} ${p.seriesName}: ${formatMetricValue(Number(p.value || 0), selectedMetric.value)}`)
        return [title, ...lines].join('<br/>')
      }
    },
    legend: { data: Object.keys(nodes), textStyle: { color: '#94a3b8' }, right: '10%', top: '5%' },
    grid: { top: '15%', left: '5%', right: '5%', bottom: '10%', containLabel: true },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: labels,
      axisLine: { lineStyle: { color: '#334155' } },
      axisLabel: { color: '#64748b' },
      splitLine: { show: false }
    },
    yAxis: getYAxisOption(),
    series,
    animation: true,
    animationDuration: 300,
    animationEasing: 'linear'
  }
}

const initChart = () => {
  nextTick(() => {
    setTimeout(() => {
      if (!chartRef.value) return
      if (myChart) { myChart.dispose(); myChart = null }
      myChart = echarts.init(chartRef.value)
      myChart.setOption(getChartOption())
    }, 200)
  })
}

const updateChartData = (metric) => {
  const metricName = normalizeMetricName(metric?.name)
  if (!metricName) return

  const hostname = metric?.hostname || 'unknown-node'
  const value = Number(metric?.value)
  if (Number.isNaN(value)) return

  const labels = metricLabels[metricName]
  const nodes = metricSeries[metricName]

  if (!nodes[hostname]) nodes[hostname] = []
  nodes[hostname].push(value)

  const now = new Date().toLocaleTimeString().replace(/^\D*/, '')
  const maxLength = Math.max(...Object.values(nodes).map((d) => d.length), 0)
  while (labels.length < maxLength) labels.push(now)

  Object.keys(nodes).forEach((h) => {
    if (nodes[h].length > 60) nodes[h].shift()
  })
  while (labels.length > 60) labels.shift()

  if (myChart && metricName === selectedMetric.value) {
    myChart.setOption(getChartOption(), true)
  }
}

let connectionCheckInterval = null

const initWebSocket = () => {
  const socket = new SockJS(`${WS_BASE_URL}/ws-monitor`)
  const stomp = Stomp.over(socket)
  stomp.debug = null

  const headers = auth.token ? { Authorization: `Bearer ${auth.token}` } : {}

  stomp.connect(headers, () => {
    wsConnected.value = true
    connectionCheckInterval = setInterval(() => {
      if (!wsConnected.value) initWebSocket()
    }, 5000)

    stomp.subscribe('/topic/metrics', (msg) => {
      try { updateChartData(JSON.parse(msg.body)) } catch (_e) { /* ignore */ }
    })
    stomp.subscribe('/topic/mode', (msg) => {
      try { mode.value = JSON.parse(msg.body).mode || 'standalone' } catch (_e) { /* ignore */ }
    })
    stomp.subscribe('/topic/system-status', (msg) => {
      try { mode.value = JSON.parse(msg.body).mode || 'standalone' } catch (_e) { /* ignore */ }
    })
  }, () => {
    wsConnected.value = false
    clearInterval(connectionCheckInterval)
    setTimeout(initWebSocket, 5000)
  })
}

const handleResize = () => { if (myChart) myChart.resize() }

onMounted(async () => {
  const now = new Date()
  const initialLabels = []
  for (let i = 59; i >= 0; i--) {
    initialLabels.push(new Date(now - i * 1000).toLocaleTimeString().replace(/^\D*/, ''))
  }
  for (const metric of SUPPORTED_METRICS) {
    metricLabels[metric] = [...initialLabels]
  }

  await loadHardwareInfo()
  nextTick(() => {
    initChart()
    initWebSocket()
    window.addEventListener('resize', handleResize)
  })
})

onUnmounted(() => {
  if (myChart) { myChart.dispose(); myChart = null }
  clearInterval(connectionCheckInterval)
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
.tone-subtle {
  color: var(--text-2);
}

.tone-muted {
  color: var(--text-3);
}

.chart-surface {
  border-radius: 18px;
  border: 1px solid var(--line-strong);
  background: linear-gradient(180deg, var(--panel), transparent);
  box-shadow: inset 0 0 24px rgba(2, 6, 23, 0.2);
}

.metric-pill {
  letter-spacing: 0.02em;
}

.metric-pill-active {
  color: var(--text-1);
  border-color: rgba(34, 211, 238, 0.6);
  background: rgba(34, 211, 238, 0.16);
}

.metric-pill-idle {
  color: var(--text-2);
  border-color: rgba(100, 116, 139, 0.45);
  background: var(--panel);
}

.metric-pill-idle:hover {
  border-color: rgba(34, 211, 238, 0.3);
  background: var(--panel-strong);
}
</style>
