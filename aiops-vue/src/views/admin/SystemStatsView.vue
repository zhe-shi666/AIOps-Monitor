<template>
  <div>
    <h2 class="text-xl font-bold text-slate-100 mb-6">系统统计</h2>

    <div v-if="stats" class="grid grid-cols-3 gap-6 mb-8">
      <div class="p-6 rounded-xl border border-slate-700 bg-slate-900/50 text-center">
        <p class="text-3xl font-bold text-cyan-400">{{ stats.totalUsers }}</p>
        <p class="text-slate-400 text-sm mt-2">总用户数</p>
      </div>
      <div class="p-6 rounded-xl border border-slate-700 bg-slate-900/50 text-center">
        <p class="text-3xl font-bold text-purple-400">{{ stats.totalTargets }}</p>
        <p class="text-slate-400 text-sm mt-2">监控目标数</p>
      </div>
      <div class="p-6 rounded-xl border border-slate-700 bg-slate-900/50 text-center">
        <p class="text-3xl font-bold text-red-400">{{ stats.todayIncidents }}</p>
        <p class="text-slate-400 text-sm mt-2">今日告警数</p>
      </div>
    </div>

    <div v-else class="text-slate-400 text-center py-20">加载中...</div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getStats } from '../../api/admin'

const stats = ref(null)

onMounted(async () => {
  try {
    const { data } = await getStats()
    stats.value = data
  } catch (e) {
    ElMessage.error('加载统计数据失败')
  }
})
</script>
