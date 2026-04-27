<template>
  <div class="space-y-5">
    <div>
      <h2 class="section-title">{{ t('title') }}</h2>
      <p class="section-subtitle">{{ t('subtitle') }}</p>
    </div>

    <div v-if="stats" class="kpi-grid">
      <div class="kpi-item text-center">
        <p class="kpi-value text-sky-300">{{ stats.totalUsers }}</p>
        <p class="kpi-label mt-2">{{ t('totalUsers') }}</p>
      </div>
      <div class="kpi-item text-center">
        <p class="kpi-value text-violet-300">{{ stats.totalTargets }}</p>
        <p class="kpi-label mt-2">{{ t('totalTargets') }}</p>
      </div>
      <div class="kpi-item text-center">
        <p class="kpi-value text-rose-300">{{ stats.todayIncidents }}</p>
        <p class="kpi-label mt-2">{{ t('todayIncidents') }}</p>
      </div>
    </div>

    <div v-else class="card-panel p-10 text-slate-400 text-center">{{ t('loading') }}</div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getStats } from '../../api/admin'
import { useI18n } from '../../composables/useI18n'

const stats = ref(null)
const { t } = useI18n({
  title: { zh: '系统统计', en: 'System Statistics' },
  subtitle: { zh: '平台用户、目标规模与告警负载', en: 'Platform users, target scale and incident load' },
  totalUsers: { zh: '总用户数', en: 'Total Users' },
  totalTargets: { zh: '监控目标数', en: 'Total Targets' },
  todayIncidents: { zh: '今日告警数', en: 'Incidents Today' },
  loading: { zh: '加载中...', en: 'Loading...' },
  loadFailed: { zh: '加载统计数据失败', en: 'Failed to load statistics' }
})

onMounted(async () => {
  try {
    const { data } = await getStats()
    stats.value = data
  } catch (_e) {
    ElMessage.error(t('loadFailed'))
  }
})
</script>
