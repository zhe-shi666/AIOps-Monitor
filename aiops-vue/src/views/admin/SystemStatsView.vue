<template>
  <div class="admin-section-space">
    <section class="card-panel admin-subcard">
      <div class="section-head admin-subhead">
        <div>
          <h2 class="section-title">{{ t('title') }}</h2>
          <p class="section-subtitle">{{ t('subtitle') }}</p>
        </div>
      </div>

      <template v-if="stats">
        <div class="kpi-grid admin-stats-grid">
          <div class="kpi-item admin-stat-card">
            <p class="kpi-value text-sky-300">{{ stats.totalUsers }}</p>
            <p class="kpi-label mt-2">{{ t('totalUsers') }}</p>
          </div>
          <div class="kpi-item admin-stat-card">
            <p class="kpi-value text-violet-300">{{ stats.totalTargets }}</p>
            <p class="kpi-label mt-2">{{ t('totalTargets') }}</p>
          </div>
          <div class="kpi-item admin-stat-card">
            <p class="kpi-value text-rose-300">{{ stats.todayIncidents }}</p>
            <p class="kpi-label mt-2">{{ t('todayIncidents') }}</p>
          </div>
        </div>

        <div class="stats-note-grid">
          <div class="note-box">
            <strong>{{ t('opsInsightTitle') }}</strong>
            <p>{{ t('opsInsightDesc') }}</p>
          </div>
          <div class="note-box">
            <strong>{{ t('growthTitle') }}</strong>
            <p>{{ t('growthDesc') }}</p>
          </div>
        </div>
      </template>

      <div v-else class="stats-loading-empty">{{ t('loading') }}</div>
    </section>
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
  subtitle: { zh: '从用户、监控目标和当日告警量三个维度快速把握平台规模。', en: 'Understand platform scale quickly through users, monitored targets, and today\'s incidents.' },
  totalUsers: { zh: '总用户数', en: 'Total Users' },
  totalTargets: { zh: '监控目标数', en: 'Total Targets' },
  todayIncidents: { zh: '今日告警数', en: 'Incidents Today' },
  opsInsightTitle: { zh: '运维观察', en: 'Ops Insight' },
  opsInsightDesc: { zh: '这里适合继续扩展租户活跃度、通知成功率、在线目标率等企业级统计指标。', en: 'This area is ready for richer enterprise stats like tenant activity, delivery success, and online target rate.' },
  growthTitle: { zh: '增长视角', en: 'Growth View' },
  growthDesc: { zh: '目前展示基础统计，后续可接入趋势图、同比环比和资源占用预测。', en: 'Base statistics are shown now; later we can add trends, comparisons, and capacity forecasting.' },
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

<style scoped>
.admin-section-space {
  display: grid;
  gap: 18px;
}

.admin-subcard {
  padding: 20px;
}

.admin-subhead {
  margin-bottom: 18px;
}

.admin-stats-grid {
  gap: 14px;
}

.admin-stat-card {
  min-height: 132px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  text-align: center;
}

.stats-note-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin-top: 18px;
}

.stats-note-grid strong {
  display: block;
  color: var(--text-1);
  margin-bottom: 6px;
}

.stats-note-grid p {
  margin: 0;
  color: var(--text-3);
  line-height: 1.6;
}

.stats-loading-empty {
  padding: 48px 16px;
  text-align: center;
  color: var(--text-3);
  border: 1px dashed var(--line);
  border-radius: 16px;
  background: var(--panel-soft);
}

@media (max-width: 760px) {
  .stats-note-grid {
    grid-template-columns: 1fr;
  }
}
</style>
