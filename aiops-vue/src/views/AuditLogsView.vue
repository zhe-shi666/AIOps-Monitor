<template>
  <div class="page-surface audit-page">
    <div class="page-hero audit-hero">
      <div>
        <h1>{{ t('title') }}</h1>
        <p>{{ t('subtitle') }}</p>
      </div>
      <el-button @click="fetchLogs" :loading="loading">{{ t('refresh') }}</el-button>
    </div>

    <section class="audit-grid">
      <div class="kpi-item">
        <p class="kpi-label">{{ t('total') }}</p>
        <p class="kpi-value">{{ total }}</p>
      </div>
      <div class="kpi-item">
        <p class="kpi-label">{{ t('pageCount') }}</p>
        <p class="kpi-value text-sky-300">{{ logs.length }}</p>
      </div>
      <div class="kpi-item">
        <p class="kpi-label">{{ t('scope') }}</p>
        <p class="kpi-value compact">{{ auth.isAdmin ? t('allUsers') : t('currentUser') }}</p>
      </div>
    </section>

    <section class="card-panel audit-toolbar-card">
      <div class="section-head audit-section-head">
        <div>
          <h2 class="section-title">{{ t('filterTitle') }}</h2>
          <p class="section-subtitle">{{ t('filterSubtitle') }}</p>
        </div>
      </div>

      <div class="audit-toolbar">
        <el-input
          v-model="keyword"
          clearable
          :placeholder="t('keywordPlaceholder')"
          @keyup.enter="resetAndFetch" />
        <el-select v-model="action" clearable :placeholder="t('action')" @change="resetAndFetch">
          <el-option v-for="item in actionOptions" :key="item" :label="item" :value="item" />
        </el-select>
        <el-select v-model="resourceType" clearable :placeholder="t('resourceType')" @change="resetAndFetch">
          <el-option label="MONITOR_TARGET" value="MONITOR_TARGET" />
          <el-option label="USER" value="USER" />
        </el-select>
        <el-button type="primary" @click="resetAndFetch">{{ t('search') }}</el-button>
      </div>
    </section>

    <section class="card-panel audit-table-card">
      <div class="section-head audit-section-head">
        <div>
          <h2 class="section-title">{{ t('tableTitle') }}</h2>
          <p class="section-subtitle">{{ t('tableSubtitle') }}</p>
        </div>
      </div>

      <div class="table-wrap ops-table audit-table-wrap">
        <el-table :data="logs" v-loading="loading" style="width: 100%;">
          <el-table-column prop="createdAt" :label="t('time')" min-width="170">
            <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column prop="actor" :label="t('actor')" min-width="130">
            <template #default="{ row }">
              <strong>{{ row.actor || '-' }}</strong>
              <p class="audit-muted">UID {{ row.userId || '-' }}</p>
            </template>
          </el-table-column>
          <el-table-column prop="action" :label="t('action')" min-width="180">
            <template #default="{ row }">
              <el-tag :type="actionTone(row.action)" size="small">{{ row.action }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="resourceType" :label="t('resource')" min-width="160">
            <template #default="{ row }">
              <span>{{ row.resourceType || '-' }}</span>
              <p class="audit-muted">#{{ row.resourceId || '-' }}</p>
            </template>
          </el-table-column>
          <el-table-column prop="ipAddress" :label="t('ip')" min-width="130" />
          <el-table-column prop="detailJson" :label="t('detail')" min-width="260">
            <template #default="{ row }">
              <pre class="audit-detail">{{ formatDetail(row.detailJson) }}</pre>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!loading && !logs.length" :description="t('empty')" />
      </div>
    </section>

    <div class="audit-pagination">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :total="total"
        layout="total, sizes, prev, pager, next"
        :page-sizes="[10, 20, 50, 100]"
        @current-change="fetchLogs"
        @size-change="resetAndFetch" />
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getAuditLogs } from '../api/audit'
import { useAuthStore } from '../stores/auth'
import { useI18n } from '../composables/useI18n'

const auth = useAuthStore()
const logs = ref([])
const loading = ref(false)
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const keyword = ref('')
const action = ref('')
const resourceType = ref('')

const { locale, t } = useI18n({
  title: { zh: '运维审计', en: 'Operations Audit' },
  subtitle: { zh: '追踪目标、阈值、密钥和用户治理等关键变更，支撑企业合规交付', en: 'Track critical changes for targets, thresholds, keys, and user governance' },
  refresh: { zh: '刷新', en: 'Refresh' },
  total: { zh: '匹配记录', en: 'Matched Records' },
  pageCount: { zh: '当前页', en: 'Current Page' },
  scope: { zh: '查看范围', en: 'Scope' },
  allUsers: { zh: '全平台', en: 'All Users' },
  currentUser: { zh: '当前用户', en: 'Current User' },
  keywordPlaceholder: { zh: '搜索操作者 / 详情 JSON', en: 'Search actor / detail JSON' },
  action: { zh: '动作', en: 'Action' },
  resourceType: { zh: '资源类型', en: 'Resource Type' },
  search: { zh: '筛选', en: 'Filter' },
  filterTitle: { zh: '筛选条件', en: 'Filter Conditions' },
  filterSubtitle: { zh: '按动作、资源类型和关键字快速定位审计记录。', en: 'Filter audit records by action, resource type, and keyword.' },
  tableTitle: { zh: '审计记录', en: 'Audit Records' },
  tableSubtitle: { zh: '统一查看关键操作留痕，便于问题追踪与合规审查。', en: 'Review operational traces for incident tracking and compliance review.' },
  time: { zh: '时间', en: 'Time' },
  actor: { zh: '操作者', en: 'Actor' },
  resource: { zh: '资源', en: 'Resource' },
  ip: { zh: '来源 IP', en: 'Source IP' },
  detail: { zh: '详情', en: 'Detail' },
  empty: { zh: '暂无审计记录', en: 'No audit records' },
  loadFailed: { zh: '加载审计日志失败', en: 'Failed to load audit logs' }
})

const actionOptions = computed(() => [
  'TARGET_CREATE',
  'TARGET_UPDATE',
  'TARGET_DELETE',
  'TARGET_ROTATE_KEY',
  'TARGET_THRESHOLD_UPDATE',
  'USER_ROLE_UPDATE',
  'USER_STATUS_UPDATE',
  'USER_DELETE'
])

async function fetchLogs() {
  loading.value = true
  try {
    const { data } = await getAuditLogs({
      page: page.value - 1,
      size: pageSize.value,
      keyword: keyword.value,
      action: action.value,
      resourceType: resourceType.value
    })
    logs.value = Array.isArray(data?.content) ? data.content : []
    total.value = Number(data?.totalElements || 0)
  } catch (_e) {
    ElMessage.error(t('loadFailed'))
  } finally {
    loading.value = false
  }
}

function resetAndFetch() {
  page.value = 1
  fetchLogs()
}

function formatDate(value) {
  if (!value) return '-'
  return new Date(value).toLocaleString(locale.value === 'zh' ? 'zh-CN' : 'en-US')
}

function formatDetail(value) {
  if (!value) return '-'
  try {
    return JSON.stringify(JSON.parse(value), null, 2)
  } catch (_e) {
    return value
  }
}

function actionTone(value) {
  if (String(value || '').includes('DELETE')) return 'danger'
  if (String(value || '').includes('ROTATE')) return 'warning'
  if (String(value || '').includes('UPDATE')) return 'primary'
  return 'success'
}

onMounted(fetchLogs)
</script>

<style scoped>
.audit-page { gap: 16px; }
.audit-hero { background: radial-gradient(900px 240px at 0 0, rgba(14, 165, 233, 0.18), transparent 60%), var(--hero-bg); }
.audit-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 14px; }
.kpi-value.compact { font-size: 22px; }
.audit-toolbar-card,
.audit-table-card { padding: 22px; }
.audit-section-head { margin-bottom: 18px; }
.audit-toolbar { display: grid; grid-template-columns: minmax(280px, 1.2fr) 200px 200px auto; gap: 12px; align-items: center; }
.audit-table-wrap { border-radius: 18px; overflow: hidden; }
.audit-muted { margin: 4px 0 0; color: var(--text-3); font-size: 12px; }
.audit-detail { margin: 0; max-height: 92px; overflow: auto; white-space: pre-wrap; word-break: break-word; color: var(--code-text); font-size: 12px; line-height: 1.45; }
.audit-pagination { display: flex; justify-content: flex-end; padding-top: 18px; }
@media (max-width: 900px) {
  .audit-grid, .audit-toolbar { grid-template-columns: 1fr; }
}
</style>
