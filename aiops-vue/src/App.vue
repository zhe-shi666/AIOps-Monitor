<template>
  <div class="min-h-screen p-8 transition-colors duration-300" 
     :class="isDarkMode ? 'bg-slate-950 text-slate-100' : 'bg-gray-50 text-gray-900'">
    <!-- WS 连接状态 -->
    <div class="absolute top-4 left-4 z-50 flex items-center gap-2 px-3 py-1.5 rounded-lg bg-slate-900 border border-slate-700">
      <span class="relative flex h-2.5 w-2.5">
        <span :class="wsConnected ? 'animate-ping' : ''" class="absolute inline-flex h-full w-full rounded-full opacity-75" 
              :style="wsConnected ? 'background-color: rgb(34, 197, 94);' : 'background-color: rgb(239, 68, 68);'"></span>
        <span class="relative inline-flex rounded-full h-2.5 w-2.5" 
              :style="wsConnected ? 'background-color: rgb(34, 197, 94);' : 'background-color: rgb(239, 68, 68);'"></span>
      </span>
      <span class="text-[10px] font-mono tracking-wider" :class="wsConnected ? 'text-emerald-400' : 'text-red-400'">
        WS {{ wsConnected ? '已连接' : '未连接' }}
      </span>
    </div>
    
    <!-- 控制按钮 -->
    <div class="absolute top-4 left-1/2 transform -translate-x-1/2 z-50 flex items-center gap-3">
      <button 
        @click="toggleTheme"
        class="px-4 py-2.5 rounded-lg border shadow-md hover:shadow-lg flex items-center gap-2 cursor-pointer"
        :class="isDarkMode ? 'bg-slate-800 border-slate-600 text-slate-100 hover:bg-slate-700' : 'bg-white border-gray-300 text-gray-700 hover:bg-gray-50'"
      >
        <span class="text-xl">{{ isDarkMode ? '☀️' : '🌙' }}</span>
        <span class="text-sm font-semibold">{{ isDarkMode ? '白天' : '夜间' }}</span>
      </button>
      
      <button 
        @click="toggleFontType"
        class="px-4 py-2.5 rounded-lg border shadow-md hover:shadow-lg flex items-center gap-2 cursor-pointer min-w-[60px] justify-center"
        :class="isDarkMode ? 'bg-slate-800 border-slate-600 text-slate-100 hover:bg-slate-700' : 'bg-white border-gray-300 text-gray-700 hover:bg-gray-50'"
      >
        <span class="text-sm font-semibold">{{ fontType === 'chinese' ? 'EN' : '中文' }}</span>
      </button>
    </div>
    
    <header class="flex justify-between items-center border-b pb-8 mb-8 relative z-10 mt-12" 
            :class="isDarkMode ? 'border-slate-800' : 'border-gray-200'">
      <div class="flex-1">
        <h1 class="text-4xl font-bold tracking-tight mb-2" 
            :class="isDarkMode ? 'text-slate-100' : 'text-gray-900'">
          {{ fontType === 'chinese' ? 'AIOPS 智能运维指挥中心' : 'AIOPS COMMAND CENTER' }}
        </h1>
        <p class="text-xs tracking-wider uppercase mt-1" 
           :class="isDarkMode ? 'text-slate-400' : 'text-gray-500'">
          {{ fontType === 'chinese' ? '实时监控与智能诊断平台' : 'Real-time Monitoring & AI Diagnosis Platform' }}
        </p>
      </div>
      
      <div class="flex items-center gap-3 px-6 py-3 rounded-xl border shadow-sm"
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
    </header>

    <main class="grid grid-cols-12 gap-8 relative z-10">
      <!-- 左侧：实时监控图表区域 -->
      <div class="col-span-8 space-y-6">
        <div class="bg-slate-900/30 backdrop-blur-xl border border-white/10 rounded-3xl p-10 shadow-2xl">
          <div class="flex justify-between items-center mb-12 px-4">
            <h2 class="text-slate-300 font-medium tracking-widest uppercase text-sm pl-2">
              {{ fontType === 'chinese' ? '系统实时指标' : 'Real-time Metrics' }}
            </h2>
            <div class="flex gap-8 font-mono text-sm pr-2">
              <span class="text-cyan-400 font-semibold">CPU: {{ lastCpu }}%</span>
              <span class="text-purple-400 font-semibold">MEM: {{ lastMem }}%</span>
            </div>
          </div>
          <div ref="chartRef" class="h-80 w-full bg-slate-800/20 rounded-2xl border border-slate-600/30 shadow-inner mt-8" style="min-height: 320px;"></div>
        </div>
      </div>

      <!-- 右侧：AI 诊断报告区域 -->
      <div class="col-span-4 flex flex-col gap-6">
        <div class="rounded-2xl p-10 h-[600px] flex flex-col shadow-xl hover:shadow-2xl transition-all duration-300 border bg-slate-900/35 border-slate-700">
          <div class="flex items-center justify-between mb-10">
            <h2 class="text-sm font-semibold flex items-center gap-4 text-slate-300">
              <span class="w-2 h-5 rounded-full bg-slate-600"></span>
              {{ fontType === 'chinese' ? 'AI 专家诊断' : 'AI EXPERT DIAGNOSIS' }}
            </h2>
            <span class="text-[10px] font-mono tabular-nums text-slate-500">
              {{ fontType === 'chinese' ? `${reports.length} 份报告` : `${reports.length} Reports` }}
            </span>
          </div>
          
          <div class="flex-1 overflow-y-auto space-y-4 custom-scrollbar pr-3">
            <transition-group name="slide-fade" tag="div">
              <div v-for="r in reports" :key="r.id"
                   class="p-6 rounded-xl border transform transition-all duration-300 hover:shadow-lg hover:-translate-y-1"
                   :class="r.isRisk ? 'bg-red-950/35 border-red-800/40' : 'bg-slate-800/35 border-slate-700/40'">
                <div class="flex items-center justify-between mb-4">
                  <p class="text-[10px] font-mono tabular-nums text-slate-400">{{ r.time }}</p>
                  <span v-if="r.isRisk" class="px-3 py-1 text-[9px] font-mono rounded-lg border bg-red-900/50 text-red-400 border-red-800/50">
                    RISK
                  </span>
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
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { marked } from 'marked'
import * as echarts from 'echarts'
import SockJS from 'sockjs-client'
import Stomp from 'stompjs'

const reports = ref([])
const wsConnected = ref(false)
let reportIdCounter = 0

// 主题和字体设置
const isDarkMode = ref(localStorage.getItem('theme') === 'dark')
const fontType = ref(localStorage.getItem('fontType') || 'chinese')

// 监听主题变化
watch(isDarkMode, () => {
  localStorage.setItem('theme', isDarkMode.value ? 'dark' : 'light')
  updateBodyTheme()
})

// 监听字体变化
watch(fontType, () => {
  localStorage.setItem('fontType', fontType.value)
  updateBodyFont()
})

// ECharts 相关
const chartRef = ref(null)
let myChart = null
const lastCpu = ref(0)
const lastMem = ref(0)

// 数据缓存区
const cpuData = ref([])
const memData = ref([])
const timeLabels = ref([])

// 切换主题
const toggleTheme = () => {
  isDarkMode.value = !isDarkMode.value
}

// 切换字体类型
const toggleFontType = () => {
  fontType.value = fontType.value === 'chinese' ? 'english' : 'chinese'
}

// 更新 body 主题
const updateBodyTheme = () => {
  if (isDarkMode.value) {
    document.body.style.backgroundColor = '#020617'
    document.body.style.color = '#f1f5f9'
  } else {
    document.body.style.backgroundColor = '#f9fafb'
    document.body.style.color = '#111827'
  }
}

// 更新 body 字体
const updateBodyFont = () => {
  const fontClass = fontType.value === 'chinese' ? 'font-chinese' : 'font-english'
  document.body.classList.remove('font-chinese', 'font-english')
  document.body.classList.add(fontClass)
}

// 获取图表配置
const getChartOption = () => {
  return {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'axis',
      backgroundColor: '#1e293b',
      borderColor: 'rgba(34, 211, 238, 0.3)',
      borderWidth: 1,
      textStyle: { color: '#fff' },
      axisPointer: {
        lineStyle: { color: '#22d3ee' }
      }
    },
    legend: {
      data: ['CPU使用率', '内存使用率'],
      textStyle: { color: '#94a3b8' },
      right: '10%'
    },
    grid: {
      top: '15%',
      left: '5%',
      right: '5%',
      bottom: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: timeLabels.value,
      axisLine: { lineStyle: { color: '#334155' } },
      axisLabel: { color: '#64748b' },
      splitLine: { show: false }
    },
    yAxis: {
      type: 'value',
      max: 100,
      splitLine: { lineStyle: { color: '#1e293b' } },
      axisLabel: { color: '#64748b' },
      axisLine: { lineStyle: { color: '#334155' } }
    },
    series: [
      {
        name: 'CPU使用率',
        type: 'line',
        smooth: true,
        showSymbol: false,
        lineStyle: {
          width: 3,
          color: '#22d3ee',
          shadowColor: '#22d3ee',
          shadowBlur: 10
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(34, 211, 238, 0.3)' },
            { offset: 1, color: 'rgba(34, 211, 238, 0)' }
          ])
        },
        data: cpuData.value
      },
      {
        name: '内存使用率',
        type: 'line',
        smooth: true,
        showSymbol: false,
        lineStyle: {
          width: 3,
          color: '#a855f7',
          shadowColor: '#a855f7',
          shadowBlur: 10
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(168, 85, 247, 0.3)' },
            { offset: 1, color: 'rgba(168, 85, 247, 0)' }
          ])
        },
        data: memData.value
      }
    ],
    animation: true,
    animationDuration: 300,
    animationEasing: 'linear'
  }
}

// 初始化图表
const initChart = () => {
  console.log('开始初始化图表...')
  
  const tryInitChart = (attempt = 0) => {
    if (!chartRef.value) {
      console.error('图表容器未找到')
      return
    }
    
    const width = chartRef.value.offsetWidth
    const height = chartRef.value.offsetHeight
    console.log(`尝试 ${attempt + 1}: 图表容器尺寸: ${width} x ${height}`)
    
    if (width === 0 || height === 0) {
      if (attempt < 10) {
        console.log(`容器尺寸为0，200ms后重试 (尝试 ${attempt + 1}/10)`)
        setTimeout(() => tryInitChart(attempt + 1), 200)
      } else {
        console.error('多次尝试后容器尺寸仍为0，强制设置最小尺寸')
        chartRef.value.style.width = '800px'
        chartRef.value.style.height = '320px'
        setTimeout(() => tryInitChart(attempt + 1), 100)
      }
      return
    }
    
    console.log('容器尺寸正常，开始初始化图表...')
    try {
      myChart = echarts.init(chartRef.value)
      const option = getChartOption()
      myChart.setOption(option)
      console.log('✅ 图表初始化成功!')
      
      // 立即更新数据
      if (cpuData.value.length > 0 && memData.value.length > 0) {
        myChart.setOption({
          xAxis: { data: timeLabels.value },
          series: [
            { data: cpuData.value },
            { data: memData.value }
          ]
        })
      }
    } catch (error) {
      console.error('图表初始化失败:', error)
    }
  }
  
  setTimeout(() => tryInitChart(), 100)
}

// 更新图表数据
const updateChartData = (metric) => {
  const now = new Date().toLocaleTimeString().replace(/^\D*/, '')
  
  if (metric.name === 'CPU') {
    lastCpu.value = metric.value.toFixed(1)
    cpuData.value.push(metric.value)
    timeLabels.value.push(now)
  } else if (metric.name === 'MEMORY') {
    lastMem.value = metric.value.toFixed(1)
    memData.value.push(metric.value)
  }
  
  // 确保两个数组长度一致
  while (memData.value.length < cpuData.value.length) {
    memData.value.push(memData.value[memData.value.length - 1] || 0)
  }
  while (memData.value.length > cpuData.value.length) {
    memData.value.pop()
  }
  
  // 维持窗口大小为 60 个点
  if (cpuData.value.length > 60) {
    cpuData.value.shift()
    memData.value.shift()
    timeLabels.value.shift()
  }
  
  // 更新图表
  if (myChart) {
    myChart.setOption({
      xAxis: { data: timeLabels.value },
      series: [
        { data: cpuData.value },
        { data: memData.value }
      ]
    })
  }
}

// 初始化 WebSocket
const initWebSocket = () => {
  const socket = new SockJS('http://localhost:8080/ws-monitor')
  const stompClient = Stomp.over(socket)
  stompClient.debug = null

  stompClient.connect({}, () => {
    wsConnected.value = true
    
    // 订阅指标数据
    stompClient.subscribe('/topic/metrics', (msg) => {
      try {
        const metric = JSON.parse(msg.body)
        updateChartData(metric)
      } catch (e) {
        console.error('Failed to parse metric data:', e)
      }
    })
    
    // 订阅 AI 报告
    stompClient.subscribe('/topic/ai-reports', (msg) => {
      const raw = msg.body || ''
      const html = marked.parse(raw)
      const safeHtml = sanitizeHtml(html)
      reports.value.unshift({
        id: `report-${++reportIdCounter}-${Date.now()}`,
        time: new Date().toLocaleTimeString('zh-CN', { hour12: false }),
        html: safeHtml,
        isRisk: /有风险|风险|告警|critical|warning|alert/i.test(raw)
      })
      
      // 限制报告数量
      if (reports.value.length > 50) {
        reports.value = reports.value.slice(0, 50)
      }
    })
  }, () => {
    wsConnected.value = false
  })
}

// HTML 清理
const sanitizeHtml = (html) => {
  const tpl = document.createElement('template')
  tpl.innerHTML = html
  tpl.content.querySelectorAll('script, iframe, object, embed, link[rel="import"]').forEach(el => el.remove())
  tpl.content.querySelectorAll('*').forEach(el => {
    Array.from(el.attributes).forEach(attr => {
      if (attr.name.startsWith('on')) el.removeAttribute(attr.name)
      if ((attr.name === 'href' || attr.name === 'src') && String(attr.value).trim().toLowerCase().startsWith('javascript:')) {
        el.removeAttribute(attr.name)
      }
    })
  })
  return tpl.innerHTML
}

// 窗口调整
const handleResize = () => {
  if (myChart) {
    myChart.resize()
  }
}

onMounted(() => {
  // 初始化主题和字体
  updateBodyTheme()
  updateBodyFont()
  
  // 添加初始模拟数据
  const now = new Date()
  for (let i = 59; i >= 0; i--) {
    const time = new Date(now - i * 1000)
    timeLabels.value.push(time.toLocaleTimeString().replace(/^\D*/, ''))
    cpuData.value.push(Math.random() * 30 + 20) // 20-50% CPU
    memData.value.push(Math.random() * 20 + 40) // 40-60% Memory
  }
  lastCpu.value = cpuData.value[cpuData.value.length - 1].toFixed(1)
  lastMem.value = memData.value[memData.value.length - 1].toFixed(1)
  
  console.log('初始数据已生成:')
  console.log('CPU数据长度:', cpuData.value.length)
  console.log('内存数据长度:', memData.value.length)
  console.log('时间标签长度:', timeLabels.value.length)
  
  initChart()
  initWebSocket()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  if (myChart) {
    myChart.dispose()
    myChart = null
  }
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
/* 字体类型样式 */
.font-chinese {
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", "Helvetica Neue", Helvetica, Arial, sans-serif;
}

.font-english {
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
}

/* 自定义滚动条 */
.custom-scrollbar::-webkit-scrollbar {
  width: 6px;
}
.custom-scrollbar::-webkit-scrollbar-track {
  border-radius: 10px;
  background: #1e293b;
}
.custom-scrollbar::-webkit-scrollbar-thumb {
  border-radius: 10px;
  background: #475569;
}
.custom-scrollbar::-webkit-scrollbar-thumb:hover {
  background: #64748b;
}

/* Prose 样式 */
.prose {
  color: inherit;
}
.prose strong {
  font-weight: 600;
  color: #f1f5f9;
}
.prose code {
  padding: 0.125rem 0.375rem;
  border-radius: 0.25rem;
  font-family: 'Courier New', Courier, monospace !important;
  font-size: 0.875em;
  background-color: #1e293b;
  color: #60a5fa;
  border: 1px solid #334155;
}
.prose pre {
  border-radius: 0.5rem;
  padding: 1rem;
  overflow-x: auto;
  background-color: #0f172a;
  border: 1px solid #334155;
}
.prose pre code {
  background: transparent;
  border: none;
  padding: 0;
  color: #60a5fa;
}
.prose ul {
  list-style-type: disc;
  padding-left: 1.25rem;
  margin: 0.5rem 0;
  color: #cbd5e1;
}
.prose li {
  margin: 0.25rem 0;
}
.prose h1, .prose h2, .prose h3 {
  margin-top: 1rem;
  margin-bottom: 0.5rem;
  font-weight: 600;
  color: #f1f5f9;
}
.prose a {
  text-decoration: underline;
  text-underline-offset: 2px;
  color: #60a5fa;
}
.prose a:hover {
  color: #3b82f6;
}

/* 滑入动画 */
.slide-fade-enter-active {
  transition: all 0.3s ease-out;
}
.slide-fade-leave-active {
  transition: all 0.2s ease-in;
}
.slide-fade-enter-from {
  transform: translateY(-10px);
  opacity: 0;
}
.slide-fade-leave-to {
  transform: translateY(-10px);
  opacity: 0;
}

/* 数字等宽字体 */
.tabular-nums {
  font-variant-numeric: tabular-nums;
  font-feature-settings: "tnum";
}
</style>
