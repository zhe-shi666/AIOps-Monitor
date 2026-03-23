<template>
  <div class="min-h-screen p-8 transition-colors duration-300" 
       :class="isDarkMode ? 'bg-slate-950 text-slate-100' : 'bg-gray-50 text-gray-900'">
    <!-- WebSocket 连接状态 -->
    <div class="fixed top-4 left-6 z-50 flex items-center gap-2 px-2.25 py-0.35 rounded-md border shadow-md backdrop-blur-sm transition-all duration-300"
         :class="wsConnected 
           ? 'bg-emerald-500/10 border-emerald-500/30 hover:bg-emerald-500/20' 
           : 'bg-red-500/10 border-red-500/30 hover:bg-red-500/20'">
      <!-- 状态指示灯 -->
      <span :class="wsConnected ? 'animate-pulse' : ''" 
            class="inline-flex h-1.5 w-1.5 rounded-full opacity-75 flex-shrink-0"
            :style="wsConnected 
              ? 'background-color: rgb(16, 185, 129); box-shadow: 0 0 4px rgb(16, 185, 129);' 
              : 'background-color: rgb(239, 68, 68); box-shadow: 0 0 4px rgb(239, 68, 68);'">
      </span>
      
      <!-- 连接状态文字 -->
      <span class="text-[8px] font-semibold tracking-wide whitespace-nowrap" 
            :class="wsConnected 
              ? 'text-emerald-400' 
              : 'text-red-400'">
        {{ wsConnected 
          ? (fontType === 'chinese' ? 'WS连接中' : 'LIVE') 
          : (fontType === 'chinese' ? 'WS断开' : 'OFF') }}
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
      <!-- 左侧：实时监控图表区域 -->
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

      <!-- 右侧：模式适配区域 -->
      <div class="col-span-4 flex flex-col gap-6">
        <!-- 节点卫士模式：硬件信息 -->
        <div v-if="mode === 'standalone'" class="rounded-2xl p-6 shadow-xl hover:shadow-2xl transition-all duration-300 border bg-slate-900/35 border-slate-700">
          <h3 class="text-sm font-semibold flex items-center gap-3 text-slate-300 mb-4">
            <span class="w-2 h-5 rounded-full bg-cyan-600"></span>
            {{ fontType === 'chinese' ? '硬件信息' : 'Hardware Info' }}
          </h3>
          <div class="space-y-3 text-xs font-mono">
            <div class="flex justify-between">
              <span class="text-slate-400">{{ fontType === 'chinese' ? 'CPU型号' : 'CPU Model' }}:</span>
              <span class="text-cyan-400">Intel Core i7-12700K</span>
            </div>
            <div class="flex justify-between">
              <span class="text-slate-400">{{ fontType === 'chinese' ? '内存总量' : 'Total RAM' }}:</span>
              <span class="text-cyan-400">32GB DDR4</span>
            </div>
            <div class="flex justify-between">
              <span class="text-slate-400">{{ fontType === 'chinese' ? '磁盘剩余' : 'Disk Free' }}:</span>
              <span class="text-cyan-400">256GB / 1TB</span>
            </div>
            <div class="flex justify-between">
              <span class="text-slate-400">{{ fontType === 'chinese' ? '系统负载' : 'System Load' }}:</span>
              <span class="text-cyan-400">2.1 / 8.0</span>
            </div>
          </div>
        </div>
        
        <!-- 集群指挥官模式：统计指标 -->
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

        <!-- AI 诊断报告区域 -->
        <div class="rounded-2xl p-10 flex-1 flex flex-col shadow-xl hover:shadow-2xl transition-all duration-300 border bg-slate-900/35 border-slate-700">
          <div class="flex items-center justify-between mb-6">
            <h2 class="text-sm font-semibold flex items-center gap-4 text-slate-300">
              <span class="w-2 h-5 rounded-full bg-slate-600"></span>
              <span>
                {{ mode === 'standalone' 
                  ? (fontType === 'chinese' ? '[本地诊断] AI 专家分析' : '[Local Diagnosis] AI Expert Analysis')
                  : (fontType === 'chinese' ? '[全局预测] AI 专家分析' : '[Global Forecast] AI Expert Analysis')
                }}
              </span>
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
import { ref, reactive, onMounted, onUnmounted, watch, computed, nextTick } from 'vue'
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

// 数据分流：存储不同节点的数据
const nodesData = reactive({})
const timeLabels = ref([])

// 监控模式
const mode = ref('standalone') // 'standalone' | 'distributed'

// 计算总数据点
const totalDataPoints = computed(() => {
  return Object.values(nodesData).reduce((total, data) => total + data.length, 0)
})

// 计算异常节点数（负载超过80%）
const abnormalNodes = computed(() => {
  return Object.keys(nodesData).filter(hostname => {
    const data = nodesData[hostname]
    if (data.length === 0) return false
    const latestValue = data[data.length - 1]
    return latestValue > 80
  }).length
})

// 计算平均负载
const averageLoad = computed(() => {
  const allLatestValues = Object.keys(nodesData).map(hostname => {
    const data = nodesData[hostname]
    return data.length > 0 ? data[data.length - 1] : 0
  }).filter(value => value > 0)
  
  if (allLatestValues.length === 0) return 0
  const avg = allLatestValues.reduce((sum, val) => sum + val, 0) / allLatestValues.length
  return avg.toFixed(1)
})

// 计算最高负载
const maxLoad = computed(() => {
  const allLatestValues = Object.keys(nodesData).map(hostname => {
    const data = nodesData[hostname]
    return data.length > 0 ? data[data.length - 1] : 0
  }).filter(value => value > 0)
  
  if (allLatestValues.length === 0) return 0
  return Math.max(...allLatestValues).toFixed(1)
})

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

// 获取节点对应的霓虹颜色
const getNodeColor = (hostname) => {
  const colors = [
    '#22d3ee', // 霓虹青
    '#a855f7', // 霓虹紫
    '#f59e0b', // 霓虹橙
    '#10b981', // 霓虹绿
    '#ef4444', // 霓虹红
    '#8b5cf6', // 霓虹蓝紫
    '#ec4899', // 霓虹粉
    '#14b8a6', // 霓虹青绿
  ]
  const index = hostname.split('').reduce((acc, char) => acc + char.charCodeAt(0), 0) % colors.length
  return colors[index]
}

// 获取图表配置
const getChartOption = () => {
  // 根据模式过滤节点数据
  let filteredNodesData = {}
  
  if (mode.value === 'standalone') {
    // 节点卫士模式：显示所有节点（因为你的节点名不包含localhost）
    filteredNodesData = { ...nodesData }
  } else {
    // 集群指挥官模式：显示所有节点，但不包括localhost
    Object.keys(nodesData).forEach(hostname => {
      // 过滤掉localhost相关的节点
      if (!hostname.includes('localhost') && !hostname.includes('127.0.0.1')) {
        filteredNodesData[hostname] = nodesData[hostname]
      }
    })
  }
  
  console.log('模式:', mode.value)
  console.log('所有节点:', Object.keys(nodesData))
  console.log('过滤后节点:', Object.keys(filteredNodesData))
  
  const series = Object.keys(filteredNodesData).map(hostname => ({
    name: hostname,
    type: 'line',
    smooth: true,
    showSymbol: false,
    lineStyle: {
      width: 3,
      color: getNodeColor(hostname),
      shadowColor: getNodeColor(hostname),
      shadowBlur: 10
    },
    areaStyle: {
      color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
        { offset: 0, color: getNodeColor(hostname) + '4d' }, // 30% opacity
        { offset: 1, color: getNodeColor(hostname) + '00' }  // 0% opacity
      ])
    },
    data: filteredNodesData[hostname] || []
  }))

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
      data: Object.keys(filteredNodesData),
      textStyle: { color: '#94a3b8' },
      right: '10%',
      top: '5%'
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
    series,
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
    
    // 强制重新计算布局和设置样式
    chartRef.value.style.display = 'block'
    chartRef.value.style.visibility = 'visible'
    chartRef.value.style.position = 'relative'
    chartRef.value.style.width = '100%'
    chartRef.value.style.height = '320px'
    chartRef.value.style.minWidth = '400px'
    chartRef.value.style.minHeight = '320px'
    
    // 强制重新计算布局
    chartRef.value.offsetHeight // 触发重排
    
    const width = chartRef.value.offsetWidth || chartRef.value.clientWidth
    const height = chartRef.value.offsetHeight || chartRef.value.clientHeight
    console.log(`尝试 ${attempt + 1}: 图表容器尺寸: ${width} x ${height}`)
    
    if (width === 0 || height === 0) {
      if (attempt < 15) {
        console.log(`容器尺寸为0，300ms后重试 (尝试 ${attempt + 1}/15)`)
        setTimeout(() => tryInitChart(attempt + 1), 300)
      } else {
        console.error('多次尝试后容器尺寸仍为0，强制设置尺寸并初始化')
        // 最后的强制措施
        chartRef.value.style.width = '800px'
        chartRef.value.style.height = '320px'
        chartRef.value.style.minWidth = '800px'
        chartRef.value.style.minHeight = '320px'
        
        setTimeout(() => {
          try {
            if (myChart) {
              myChart.dispose()
              myChart = null
            }
            myChart = echarts.init(chartRef.value)
            const option = getChartOption()
            myChart.setOption(option)
            console.log('✅ 强制初始化图表成功!')
          } catch (error) {
            console.error('强制初始化失败:', error)
          }
        }, 100)
      }
      return
    }
    
    console.log('容器尺寸正常，开始初始化图表...')
    try {
      // 如果已有图表实例，先销毁
      if (myChart) {
        myChart.dispose()
        myChart = null
      }
      
      myChart = echarts.init(chartRef.value)
      const option = getChartOption()
      myChart.setOption(option)
      console.log('✅ 图表初始化成功!')
      console.log('当前节点:', Object.keys(nodesData))
      console.log('图表配置:', option.series.length, '条线')
    } catch (error) {
      console.error('图表初始化失败:', error)
      console.error('错误详情:', error.stack)
    }
  }
  
  // 使用 nextTick 确保 DOM 完全渲染
  nextTick(() => {
    setTimeout(() => tryInitChart(), 200)
  })
}

// 更新图表数据
const updateChartData = (metric) => {
  const { hostname, value, name } = metric
  const now = new Date().toLocaleTimeString().replace(/^\D*/, '')
  
  console.log(`收到数据: ${hostname}, ${name} = ${value}%`)
  
  // 1. 如果是新发现的节点，初始化它的数据序列
  if (!nodesData[hostname]) {
    nodesData[hostname] = []
    console.log(`发现新节点: ${hostname}`)
  }
  
  // 2. 将数据推入对应节点的数组
  nodesData[hostname].push(value)
  
  // 3. 维持时间标签（以数据最多的节点为准）
  const maxLength = Math.max(...Object.values(nodesData).map(data => data.length))
  while (timeLabels.value.length < maxLength) {
    timeLabels.value.push(now)
  }
  
  // 4. 限制数据窗口大小为 60 个点
  if (nodesData[hostname].length > 60) {
    nodesData[hostname].shift()
  }
  if (timeLabels.value.length > 60) {
    timeLabels.value.shift()
  }
  
  // 5. 检查图表容器尺寸，如果为0则重新初始化
  if (myChart && chartRef.value) {
    const width = chartRef.value.offsetWidth || chartRef.value.clientWidth
    const height = chartRef.value.offsetHeight || chartRef.value.clientHeight
    
    if (width === 0 || height === 0) {
      console.log('检测到图表容器尺寸为0，重新初始化图表')
      initChart()
      return
    }
  }
  
  // 6. 更新图表
  if (myChart) {
    try {
      const option = getChartOption()
      myChart.setOption(option, true)
      console.log(`图表更新成功: ${hostname} = ${value}%`)
      console.log(`图表中现在有 ${option.series.length} 条线`)
    } catch (error) {
      console.error('图表更新失败:', error)
      console.error('错误详情:', error.stack)
    }
  }
  
  console.log(`当前节点数据:`, Object.keys(nodesData).map(key => `${key}: ${nodesData[key].length}个点`))
}

// 定期检查连接状态
let connectionCheckInterval = null

const startConnectionCheck = () => {
  if (connectionCheckInterval) {
    clearInterval(connectionCheckInterval)
  }
  
  connectionCheckInterval = setInterval(() => {
    if (!wsConnected.value) {
      console.log('检测到连接断开，尝试重新连接...')
      initWebSocket()
    }
  }, 5000) // 每5秒检查一次
}

const stopConnectionCheck = () => {
  if (connectionCheckInterval) {
    clearInterval(connectionCheckInterval)
    connectionCheckInterval = null
  }
}
const initWebSocket = () => {
  console.log('正在连接WebSocket...')
  const socket = new SockJS('http://localhost:8080/ws-monitor')
  const stompClient = Stomp.over(socket)
  stompClient.debug = true // 开启调试日志

  stompClient.connect({}, () => {
    wsConnected.value = true
    console.log('✅ WebSocket连接成功!')
    
    // 启动连接检查
    startConnectionCheck()
    
    // 连接成功后，立即请求一次数据（如果后端支持）
    console.log('连接成功，开始订阅数据...')
    
    // 订阅指标数据
    stompClient.subscribe('/topic/metrics', (msg) => {
      try {
        console.log('收到原始消息:', msg.body)
        const metric = JSON.parse(msg.body)
        console.log('解析后的数据:', metric)
        updateChartData(metric)
      } catch (e) {
        console.error('Failed to parse metric data:', e)
        console.error('原始消息内容:', msg.body)
      }
    })
    
    // 订阅模式信息 - 增强调试，支持多个主题
    stompClient.subscribe('/topic/mode', (msg) => {
      try {
        console.log('收到模式消息:', msg.body)
        const modeInfo = JSON.parse(msg.body)
        console.log('解析后的模式信息:', modeInfo)
        mode.value = modeInfo.mode || modeInfo.modeType || 'standalone' // 兼容不同的字段名
        console.log(`监控模式更新: ${mode.value}`)
      } catch (e) {
        console.error('Failed to parse mode data:', e)
        console.error('原始模式消息内容:', msg.body)
      }
    })
    
    // 订阅系统状态消息（后端可能发送到这里）
    stompClient.subscribe('/topic/system-status', (msg) => {
      try {
        console.log('收到系统状态消息:', msg.body)
        const statusInfo = JSON.parse(msg.body)
        console.log('解析后的系统状态信息:', statusInfo)
        
        // 强制更新模式
        const newMode = statusInfo.mode || statusInfo.modeType || 'standalone'
        mode.value = newMode
        console.log(`通过系统状态更新模式: ${mode.value}`)
        console.log(`模式值类型: ${typeof mode.value}`)
        console.log(`模式值长度: ${mode.value.length}`)
        
        // 强制触发Vue响应式更新
        nextTick(() => {
          console.log(`nextTick后当前模式: ${mode.value}`)
        })
      } catch (e) {
        console.error('Failed to parse system status data:', e)
        console.error('原始系统状态消息内容:', msg.body)
      }
    })
    
    // 订阅所有可能的主题进行调试
    stompClient.subscribe('/topic/*', (msg) => {
      console.log('收到通用消息 - 主题:', msg.headers.destination, '内容:', msg.body)
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
    
    // 连接成功后，主动请求模式信息
    console.log('连接成功，等待后端发送模式信息...')
    
    // 10秒后检查是否收到模式信息
    setTimeout(() => {
      console.log('=== WebSocket状态检查 ===')
      console.log('当前模式:', mode.value)
      console.log('已连接节点:', Object.keys(nodesData))
      console.log('是否收到模式信息:', mode.value !== 'standalone' ? '是' : '否')
      
      // 如果没有收到模式信息，可能是后端没有发送
      if (mode.value === 'standalone' && Object.keys(nodesData).length > 0) {
        console.log('⚠️  可能后端没有发送模式信息，建议检查后端配置')
      }
    }, 10000)
    
  }, (error) => {
    wsConnected.value = false
    console.error('❌ WebSocket连接失败:', error)
    console.error('连接错误详情:', error)
    
    // 停止连接检查
    stopConnectionCheck()
    
    // 5秒后重试连接
    setTimeout(() => {
      console.log('尝试重新连接WebSocket...')
      initWebSocket()
    }, 5000)
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
  
  // 添加时间标签
  const now = new Date()
  for (let i = 59; i >= 0; i--) {
    const time = new Date(now - i * 1000)
    timeLabels.value.push(time.toLocaleTimeString().replace(/^\D*/, ''))
  }
  
  console.log('界面初始化完成，等待数据接收...')
  console.log('当前模式:', mode.value)
  
  // 使用 nextTick 确保 DOM 完全渲染后再初始化图表
  nextTick(() => {
    initChart()
    initWebSocket()
    window.addEventListener('resize', handleResize)
  })
})

onUnmounted(() => {
  // 清理图表
  if (myChart) {
    myChart.dispose()
    myChart = null
  }
  
  // 清理WebSocket连接检查
  stopConnectionCheck()
  
  // 清理事件监听器
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
  color: '#60a5fa';
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
