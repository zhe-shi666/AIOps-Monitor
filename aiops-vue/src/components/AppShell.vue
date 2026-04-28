<template>
  <div class="app-shell">
    <div v-if="mobileNavOpen" class="shell-sidebar-overlay" @click="mobileNavOpen = false"></div>

    <aside class="shell-sidebar" :class="{ open: mobileNavOpen }">
      <div class="brand-block">
        <div class="brand-logo">AO</div>
        <div>
          <p class="brand-title">AIOps Monitor</p>
          <p class="brand-sub">{{ t('brandSub') }}</p>
        </div>
      </div>

      <p class="nav-group-title">{{ t('group.monitor') }}</p>
      <router-link
        v-for="item in monitorNav"
        :key="item.path"
        :to="item.path"
        class="nav-link"
        :class="{ active: isActive(item.path) }"
        @click="mobileNavOpen = false">
        <span class="dot"></span>
        <span>{{ item.label }}</span>
      </router-link>

      <p class="nav-group-title">{{ t('group.settings') }}</p>
      <router-link
        v-for="item in settingNav"
        :key="item.path"
        :to="item.path"
        class="nav-link"
        :class="{ active: isActive(item.path) }"
        @click="mobileNavOpen = false">
        <span class="dot"></span>
        <span>{{ item.label }}</span>
      </router-link>

      <template v-if="auth.isAdmin">
        <p class="nav-group-title">{{ t('group.admin') }}</p>
        <router-link
          to="/admin/users"
          class="nav-link"
          :class="{ active: isActive('/admin') }"
          @click="mobileNavOpen = false">
          <span class="dot"></span>
          <span>{{ t('nav.admin') }}</span>
        </router-link>
      </template>
    </aside>

    <div class="shell-main">
      <header class="shell-topbar">
        <div class="flex items-center gap-3">
          <el-button
            class="mobile-nav-toggle"
            type="primary"
            plain
            size="small"
            @click="mobileNavOpen = !mobileNavOpen">
            {{ t('action.menu') }}
          </el-button>
          <div>
            <h1 class="top-title">{{ currentHeader.title }}</h1>
            <p class="top-subtitle">{{ currentHeader.subtitle }}</p>
          </div>
        </div>

        <div class="topbar-right">
          <div class="topbar-tools">
            <el-button class="topbar-tool" size="small" plain @click="toggleTheme">
              {{ theme === 'dark' ? t('action.light') : t('action.dark') }}
            </el-button>
            <el-button class="topbar-tool" size="small" plain @click="toggleLocale">
              {{ locale === 'zh' ? t('action.switchEn') : t('action.switchZh') }}
            </el-button>
          </div>
          <div class="topbar-account">
            <span class="user-chip">
              <span>{{ auth.username || t('label.unknown') }}</span>
              <span v-if="auth.isAdmin">ADMIN</span>
            </span>
            <el-button class="topbar-logout" size="small" type="danger" plain @click="handleLogout">{{ t('action.logout') }}</el-button>
          </div>
        </div>
      </header>

      <div class="shell-body">
        <main class="shell-content">
          <router-view />
        </main>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useTheme } from '../composables/useTheme'
import { useLocaleMode } from '../composables/useLocaleMode'
import { useI18n } from '../composables/useI18n'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const mobileNavOpen = ref(false)
const { theme, toggleTheme } = useTheme()
const { locale, toggleLocale } = useLocaleMode()
const { isZh, t } = useI18n({
  'group.monitor': { zh: '监控', en: 'Monitor' },
  'group.settings': { zh: '设置', en: 'Settings' },
  'group.admin': { zh: '管理', en: 'Admin' },
  'nav.overview': { zh: '总览看板', en: 'Overview' },
  'nav.aiExpert': { zh: 'AI 专家', en: 'AI Expert' },
  'nav.workbench': { zh: '事件工作台', en: 'Workbench' },
  'nav.targets': { zh: '监控目标', en: 'Targets' },
  'nav.incidents': { zh: '告警中心', en: 'Incidents' },
  'nav.thresholds': { zh: '阈值策略', en: 'Thresholds' },
  'nav.escalation': { zh: '升级策略', en: 'Escalation' },
  'nav.notifications': { zh: '通知通道', en: 'Channels' },
  'nav.admin': { zh: '管理后台', en: 'Admin Console' },
  'action.menu': { zh: '菜单', en: 'Menu' },
  'action.light': { zh: '白天模式', en: 'Light' },
  'action.dark': { zh: '夜间模式', en: 'Dark' },
  'action.switchEn': { zh: 'English', en: 'English' },
  'action.switchZh': { zh: '中文', en: '中文' },
  'action.logout': { zh: '退出', en: 'Sign Out' },
  'label.unknown': { zh: '未知用户', en: 'Unknown' },
  'brandSub': { zh: '企业运维控制台', en: 'Enterprise Operations Console' }
})

const monitorNav = computed(() => [
  { path: '/', label: t('nav.overview') },
  { path: '/workbench', label: t('nav.workbench') },
  { path: '/ai-expert', label: t('nav.aiExpert') },
  { path: '/targets', label: t('nav.targets') },
  { path: '/incidents', label: t('nav.incidents') }
])

const settingNav = computed(() => [
  { path: '/settings/thresholds', label: t('nav.thresholds') },
  { path: '/settings/escalation', label: t('nav.escalation') },
  { path: '/settings/notifications', label: t('nav.notifications') }
])

const headerMap = computed(() => ({
  '/': {
    title: isZh.value ? '实时运营总览' : 'Operations Overview',
    subtitle: isZh.value ? '系统状态、指标走势与 AI 分析' : 'System health, metrics trends, and AI analysis'
  },
  '/targets': {
    title: isZh.value ? '监控目标资产' : 'Target Inventory',
    subtitle: isZh.value ? '主机目标、Agent 密钥与状态管理' : 'Host targets, agent keys, and status management'
  },
  '/ai-expert': {
    title: isZh.value ? 'AI 专家中心' : 'AI Expert Console',
    subtitle: isZh.value ? '调查、诊断、执行与复盘的统一工作流' : 'Unified workflow for investigations, actions, and postmortems'
  },
  '/workbench': {
    title: isZh.value ? '事件工作台' : 'Event Workbench',
    subtitle: isZh.value ? '告警到调查到执行的闭环联动作业台' : 'Closed-loop cockpit from incidents to investigation and execution'
  },
  '/incidents': {
    title: isZh.value ? '告警事件中心' : 'Incident Center',
    subtitle: isZh.value ? '告警处理、升级状态与通知审计' : 'Incident handling, escalation state, and delivery audit'
  },
  '/settings/thresholds': {
    title: isZh.value ? '阈值与降噪' : 'Thresholds and Noise Control',
    subtitle: isZh.value ? '规则阈值、连续触发和静默窗口' : 'Rule thresholds, consecutive triggers, and silence windows'
  },
  '/settings/escalation': {
    title: isZh.value ? '升级节奏策略' : 'Escalation Strategy',
    subtitle: isZh.value ? '按 P1 / P2 / P3 配置通知升级' : 'Configure notification escalation by P1/P2/P3'
  },
  '/settings/notifications': {
    title: isZh.value ? '通知通道编排' : 'Notification Channels',
    subtitle: isZh.value ? 'Webhook 通道配置与投递管理' : 'Webhook channel configuration and delivery management'
  },
  '/admin/users': {
    title: isZh.value ? '管理后台' : 'Admin Console',
    subtitle: isZh.value ? '用户治理与平台统计' : 'User governance and platform statistics'
  },
  '/admin/stats': {
    title: isZh.value ? '管理后台' : 'Admin Console',
    subtitle: isZh.value ? '用户治理与平台统计' : 'User governance and platform statistics'
  }
}))

const currentHeader = computed(() => {
  if (route.path.startsWith('/admin')) {
    return headerMap.value['/admin/users']
  }
  return headerMap.value[route.path] || {
    title: 'AIOps Monitor',
    subtitle: isZh.value ? '智能运维平台' : 'Operations Intelligence Platform'
  }
})

function isActive(path) {
  if (path === '/') return route.path === '/'
  return route.path.startsWith(path)
}

function handleLogout() {
  auth.logout()
  mobileNavOpen.value = false
  router.push('/login')
}
</script>
