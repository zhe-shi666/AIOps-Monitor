<template>
  <div class="min-h-screen p-8 pt-20 transition-colors duration-300"
       :class="isDarkMode ? 'bg-slate-950 text-slate-100' : 'bg-gray-50 text-gray-900'">
    <!-- WebSocket 连接状态 -->
    <div class="fixed top-14 left-6 z-40 flex items-center gap-2 px-2.25 py-0.35 rounded-md border shadow-md backdrop-blur-sm transition-all duration-300"
         :class="wsConnected
           ? 'bg-emerald-500/10 border-emerald-500/30 hover:bg-emerald-500/20'
           : 'bg-red-500/10 border-red-500/30 hover:bg-red-500/20'">
      <span :class="wsConnected ? 'animate-pulse' : ''"
            class="inline-flex h-1.5 w-1.5 rounded-full opacity-75 flex-shrink-0"
            :style="wsConnected
              ? 'background-color: rgb(16, 185, 129); box-shadow: 0 0 4px rgb(16, 185, 129);'
              : 'background-color: rgb(239, 68, 68); box-shadow: 0 0 4px rgb(239, 68, 68);'">
      </span>
      <span class="text-[8px] font-semibold tracking-wide whitespace-nowrap"
            :class="wsConnected ? 'text-emerald-400' : 'text-red-400'">
        {{ wsConnected ? 'WS连接中' : 'WS断开' }}
      </span>
    </div>

    <!-- 控制按钮 -->
    <div class="absolute top-16 left-1/2 transform -translate-x-1/2 z-40 flex items-center gap-3">
      <button @click="toggleTheme"
        class="px-4 py-2.5 rounded-lg border shadow-md hover:shadow-lg flex items-center gap-2 cursor-pointer"
        :class="isDarkMode ? 'bg-slate-800 border-slate-600 text-slate-100 hover:bg-slate-700' : 'bg-white border-gray-300 text-gray-700 hover:bg-gray-50'">
        <span class="text-xl">{{ isDarkMode ? '☀️' : '🌙' }}</span>
        <span class="text-sm font-semibold">{{ isDarkMode ? '白天' : '夜间' }}</span>
      </button>
      <button @click="toggleFontType"
        class="px-4 py-2.5 rounded-lg border shadow-md hover:shadow-lg flex items-center gap-2 cursor-pointer min-w-[60px] justify-center"
        :class="isDarkMode ? 'bg-slate-800 border-slate-600 text-slate-100 hover:bg-slate-700' : 'bg-white border-gray-300 text-gray-700 hover:bg-gray-50'">
        <span class="text-sm font-semibold">{{ fontType === 'chinese' ? 'EN' : '中文' }}</span>
      </button>
    </div>

    <header class="flex justify-between items-center border-b pb-8 mb-8 relative z-10 mt-4"
            :class="isDarkMode ? 'border-slate-800' : 'border-gray-200'">
      <div class="flex-1">
        <h1 class="text-4xl font-bold tracking-tight mb-2"
            :class="isDarkMode ? 'text-slate-100' : 'text-gray-900'">
          {{ mode === 'standalone'
            ? (fontType === 'chinese' ? '🛡️ 节点卫士监控系统' : '🛡️ Node Guardian Monitor')
            : (fontType === 'chinese' ? '🌐 集群指挥官中心' : '🌐 Cluster Commander Center')
          }}
        </h1>
        <p class="text-xs tracking-wider uppercase mt-1"
           :class="isDarkMode ? 'text-slate-400' : 'text-gray-500'">
          {{ mode === 'standalone'
            ? (fontType === 'chinese' ? '单机实时监控与智能诊断' : 'Single Machine Real-time Monitoring & AI Diagnosis')
            : (fontType === 'chinese' ? '分布式集群监控与协同分析' : 'Distributed Cluster Monitoring & Collaborative Analysis')
          }}
        </p>
      </div>
      <div class="flex items-center gap-4">
        <div class="flex items-center gap-4 px-6 py-3 rounded-xl border shadow-sm"
             :class="isDarkMode ? 'bg-slate-900 border-slate-700' : 'bg-white border-gray-200'">
          <span class="relative flex h-3 w-3">
            <span class="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-500 opacity-75"></span>
            <span class="relative inline-flex rounded-full h-3 w-3 bg-emerald-500"></span>
          </span>
          <span class="text-xs tracking-wider font-medium"
                :class="isDarkMode ? 'text-emerald-400' : 'text-emerald-600'">
            {{ fontType === 'chinese' ? '核心系统在线' : 'CORE SYSTEM ONLINE' }}
          </span>
        </div>
      </div>
    </header>

    <main class="grid grid-cols-12 gap-8 relative z-10">
      <div class="col-span-8 space-y-6">
        <div class="bg-slate-900/30 backdrop-blur-xl border border-white/10 rounded-3xl p-10 shadow-2xl">
          <div class="flex justify-between items-center mb-12 px-4">
            <h2 class="text-slate-300 font-medium tracking-widest uppercase text-sm pl-2">
              {{ fontType === 'chinese' ? '系统实时指标' : 'Real-time Metrics' }}
            </h2>
            <div class="flex gap-6 font-mono text-sm pr-2">
              <span class="text-cyan-400 font-semibold">
                {{ mode === 'standalone' ? '本机节点' : `节点数: ${Object.keys(nodesData).length}` }}
              </span>
              <span class="text-purple-400 font-semibold">数据点: {{ totalDataPoints }}</span>
            </div>
          </div>
          <div ref="chartRef" class="h-80 w-full bg-slate-800/20 rounded-2xl border border-slate-600/30 shadow-inner mt-8" style="min-height: 320px;"></div>
        </div>
      </div>

      <div class="col-span-4 flex flex-col gap-6">
        <div v-if="mode === 'standalone'" class="rounded-2xl p-6 shadow-xl hover:shadow-2xl transition-all duration-300 border bg-slate-900/35 border-slate-700">
          <h3 class="text-sm font-semibold flex items-center gap-3 text-slate-300 mb-4">
            <span class="w-2 h-5 rounded-full bg-cyan-600"></span>
            {{ fontType === 'chinese' ? '硬件信息' : 'Hardware Info' }}
          </h3>
          <div class="space-y-3 text-xs font-mono">
            <div class="flex justify-between">
              <span class="text-slate-400">{{ fontType === 'chinese' ? 'CPU型号' : 'CPU Model' }}:</span>
              <span class="text-cyan-400">{{ hardwareCpuModel }}</span>
            </div>
            <div class="flex justify-between">
              <span class="text-slate-400">{{ fontType === 'chinese' ? '内存总量' : 'Total RAM' }}:</span>
              <span class="text-cyan-400">{{ hardwareMemory }}</span>
            </div>
          </div>
        </div>

        <div v-if="mode === 'distributed'" class="rounded-2xl p-6 shadow-xl hover:shadow-2xl transition-all duration-300 border bg-slate-900/35 border-slate-700">
          <h3 class="text-sm font-semibold flex items-center gap-3 text-slate-300 mb-4">
            <span class="w-2 h-5 rounded-full bg-purple-600"></span>
            {{ fontType === 'chinese' ? '集群统计' : 'Cluster Stats' }}
          </h3>
          <div class="space-y-3 text-xs font-mono">
            <div class="flex justify-between">
              <span class="text-slate-400">{{ fontType === 'chinese' ? '在线节点' : 'Online Nodes' }}:</span>
              <span class="text-purple-400 font-semibold">{{ Object.keys(nodesData).length }}</span>
            </div>
            <div class="flex justify-between">
              <span class="text-slate-400">{{ fontType === 'chinese' ? '异常节点' : 'Abnormal Nodes' }}:</span>
              <span class="text-red-400 font-semibold">{{ abnormalNodes }}</span>
            </div>
            <div class="flex justify-between">
              <span class="text-slate-400">{{ fontType === 'chinese' ? '平均负载' : 'Avg Load' }}:</span>
              <span class="text-purple-400">{{ averageLoad }}%</span>
            </div>
            <div class="flex justify-between">
              <span class="text-slate-400">{{ fontType === 'chinese' ? '最高负载' : 'Max Load' }}:</span>
              <span class="text-orange-400">{{ maxLoad }}%</span>
            </div>
          </div>
        </div>

        <div class="rounded-2xl p-10 flex-1 flex flex-col shadow-xl hover:shadow-2xl transition-all duration-300 border bg-slate-900/35 border-slate-700">
          <div class="flex items-center justify-between mb-6">
            <h2 class="text-sm font-semibold flex items-center gap-4 text-slate-300">
              <span class="w-2 h-5 rounded-full bg-slate-600"></span>
              <span>{{ fontType === 'chinese' ? 'AI 专家分析' : 'AI Expert Analysis' }}</span>
            </h2>
            <span class="text-[10px] font-mono tabular-nums text-slate-500">
              {{ reports.length }} {{ fontType === 'chinese' ? '份报告' : 'Reports' }}
            </span>
          </div>
          <div class="flex-1 overflow-y-auto space-y-4 custom-scrollbar pr-3">
            <transition-group name="slide-fade" tag="div">
              <div v-for="r in reports" :key="r.id"
                   class="p-6 rounded-xl border transform transition-all duration-300 hover:shadow-lg hover:-translate-y-1"
                   :class="r.isRisk ? 'bg-red-950/35 border-red-800/40' : 'bg-slate-800/35 border-slate-700/40'">
                <div class="flex items-center justify-between mb-4">
                  <p class="text-[10px] font-mono tabular-nums text-slate-400">{{ r.time }}</p>
                  <span v-if="r.isRisk" class="px-3 py-1 text-[9px] font-mono rounded-lg border bg-red-900/50 text-red-400 border-red-800/50">RISK</span>
                </div>
                <div class="text-sm prose max-w-none" v-html="r.html"></div>
              </div>
            </transition-group>
            <div v-if="reports.length === 0" class="flex items-center justify-center h-full text-slate-500">
              <div class="text-center">
                <div class="text-4xl mb-3 opacity-50">📡</div>
                <p class="text-xs font-mono tracking-wider">Waiting for AI reports...</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, watch, computed, nextTick } from 'vue'
import { marked } from 'marked'
import * as echarts from 'echarts'
import SockJS from 'sockjs-client'
import Stomp from 'stompjs'
import { useAuthStore } from '../stores/auth'
import { getHardwareInfo } from '../api/system'
import { WS_BASE_URL } from '../config/env'

const auth = useAuthStore()
const reports = ref([])
const wsConnected = ref(false)
let reportIdCounter = 0

const isDarkMode = ref(localStorage.getItem('theme') === 'dark')
const fontType = ref(localStorage.getItem('fontType') || 'chinese')

watch(isDarkMode, () => {
  localStorage.setItem('theme', isDarkMode.value ? 'dark' : 'light')
  updateBodyTheme()
})
watch(fontType, () => {
  localStorage.setItem('fontType', fontType.value)
})

const chartRef = ref(null)
let myChart = null
const nodesData = reactive({})
const timeLabels = ref([])
const mode = ref('standalone')
const hardwareInfo = ref({
  cpuModel: '-',
  totalMemoryBytes: 0
})

const totalDataPoints = computed(() =>
  Object.values(nodesData).reduce((total, data) => total + data.length, 0))

const abnormalNodes = computed(() =>
  Object.keys(nodesData).filter(h => {
    const d = nodesData[h]
    return d.length > 0 && d[d.length - 1] > 80
  }).length)

const averageLoad = computed(() => {
  const vals = Object.values(nodesData).map(d => d.length > 0 ? d[d.length - 1] : 0).filter(v => v > 0)
  if (!vals.length) return 0
  return (vals.reduce((s, v) => s + v, 0) / vals.length).toFixed(1)
})

const maxLoad = computed(() => {
  const vals = Object.values(nodesData).map(d => d.length > 0 ? d[d.length - 1] : 0).filter(v => v > 0)
  return vals.length ? Math.max(...vals).toFixed(1) : 0
})

const hardwareCpuModel = computed(() => hardwareInfo.value.cpuModel || '-')
const hardwareMemory = computed(() => formatBytes(hardwareInfo.value.totalMemoryBytes))

const toggleTheme = () => { isDarkMode.value = !isDarkMode.value }
const toggleFontType = () => { fontType.value = fontType.value === 'chinese' ? 'english' : 'chinese' }

const updateBodyTheme = () => {
  document.body.style.backgroundColor = isDarkMode.value ? '#020617' : '#f9fafb'
  document.body.style.color = isDarkMode.value ? '#f1f5f9' : '#111827'
}

const getNodeColor = (hostname) => {
  const colors = ['#22d3ee','#a855f7','#f59e0b','#10b981','#ef4444','#8b5cf6','#ec4899','#14b8a6']
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

const loadHardwareInfo = async () => {
  try {
    const { data } = await getHardwareInfo()
    hardwareInfo.value = {
      cpuModel: data?.cpuModel || '-',
      totalMemoryBytes: Number(data?.totalMemoryBytes || 0)
    }
    if (data?.mode) mode.value = data.mode
  } catch (e) {
    hardwareInfo.value = {
      cpuModel: '-',
      totalMemoryBytes: 0
    }
  }
}

const getChartOption = () => {
  const series = Object.keys(nodesData).map(hostname => ({
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
    data: nodesData[hostname] || []
  }))
  return {
    backgroundColor: 'transparent',
    tooltip: { trigger: 'axis', backgroundColor: '#1e293b', borderColor: 'rgba(34,211,238,0.3)', borderWidth: 1, textStyle: { color: '#fff' } },
    legend: { data: Object.keys(nodesData), textStyle: { color: '#94a3b8' }, right: '10%', top: '5%' },
    grid: { top: '15%', left: '5%', right: '5%', bottom: '10%', containLabel: true },
    xAxis: { type: 'category', boundaryGap: false, data: timeLabels.value, axisLine: { lineStyle: { color: '#334155' } }, axisLabel: { color: '#64748b' }, splitLine: { show: false } },
    yAxis: { type: 'value', max: 100, splitLine: { lineStyle: { color: '#1e293b' } }, axisLabel: { color: '#64748b' } },
    series,
    animation: true, animationDuration: 300, animationEasing: 'linear'
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
  const { hostname, value } = metric
  const now = new Date().toLocaleTimeString().replace(/^\D*/, '')
  if (!nodesData[hostname]) nodesData[hostname] = []
  nodesData[hostname].push(value)
  const maxLength = Math.max(...Object.values(nodesData).map(d => d.length))
  while (timeLabels.value.length < maxLength) timeLabels.value.push(now)
  if (nodesData[hostname].length > 60) nodesData[hostname].shift()
  if (timeLabels.value.length > 60) timeLabels.value.shift()
  if (myChart) myChart.setOption(getChartOption(), true)
}

let connectionCheckInterval = null

const initWebSocket = () => {
  const socket = new SockJS(`${WS_BASE_URL}/ws-monitor`)
  const stompClient = Stomp.over(socket)
  stompClient.debug = null

  const headers = auth.token ? { Authorization: `Bearer ${auth.token}` } : {}

  stompClient.connect(headers, () => {
    wsConnected.value = true
    connectionCheckInterval = setInterval(() => {
      if (!wsConnected.value) initWebSocket()
    }, 5000)

    stompClient.subscribe('/topic/metrics', (msg) => {
      try { updateChartData(JSON.parse(msg.body)) } catch (e) { /* ignore */ }
    })
    stompClient.subscribe('/topic/mode', (msg) => {
      try { mode.value = JSON.parse(msg.body).mode || 'standalone' } catch (e) { /* ignore */ }
    })
    stompClient.subscribe('/topic/system-status', (msg) => {
      try { mode.value = JSON.parse(msg.body).mode || 'standalone' } catch (e) { /* ignore */ }
    })
    stompClient.subscribe('/topic/ai-reports', (msg) => {
      const raw = msg.body || ''
      const html = marked.parse(raw)
      reports.value.unshift({
        id: `report-${++reportIdCounter}-${Date.now()}`,
        time: new Date().toLocaleTimeString('zh-CN', { hour12: false }),
        html,
        isRisk: /有风险|风险|告警|critical|warning|alert/i.test(raw)
      })
      if (reports.value.length > 50) reports.value = reports.value.slice(0, 50)
    })
  }, () => {
    wsConnected.value = false
    clearInterval(connectionCheckInterval)
    setTimeout(initWebSocket, 5000)
  })
}

const handleResize = () => { if (myChart) myChart.resize() }

onMounted(async () => {
  updateBodyTheme()
  const now = new Date()
  for (let i = 59; i >= 0; i--) {
    timeLabels.value.push(new Date(now - i * 1000).toLocaleTimeString().replace(/^\D*/, ''))
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
.custom-scrollbar::-webkit-scrollbar { width: 6px; }
.custom-scrollbar::-webkit-scrollbar-track { border-radius: 10px; background: #1e293b; }
.custom-scrollbar::-webkit-scrollbar-thumb { border-radius: 10px; background: #475569; }
.custom-scrollbar::-webkit-scrollbar-thumb:hover { background: #64748b; }
.prose { color: inherit; }
.prose strong { font-weight: 600; color: #f1f5f9; }
.prose code { padding: 0.125rem 0.375rem; border-radius: 0.25rem; font-family: 'Courier New', monospace; font-size: 0.875em; background-color: #1e293b; color: #60a5fa; border: 1px solid #334155; }
.prose pre { border-radius: 0.5rem; padding: 1rem; overflow-x: auto; background-color: #0f172a; border: 1px solid #334155; }
.prose pre code { background: transparent; border: none; padding: 0; }
.prose ul { list-style-type: disc; padding-left: 1.25rem; margin: 0.5rem 0; color: #cbd5e1; }
.prose li { margin: 0.25rem 0; }
.prose h1, .prose h2, .prose h3 { margin-top: 1rem; margin-bottom: 0.5rem; font-weight: 600; color: #f1f5f9; }
.slide-fade-enter-active { transition: all 0.3s ease-out; }
.slide-fade-leave-active { transition: all 0.2s ease-in; }
.slide-fade-enter-from, .slide-fade-leave-to { transform: translateY(-10px); opacity: 0; }
</style>
