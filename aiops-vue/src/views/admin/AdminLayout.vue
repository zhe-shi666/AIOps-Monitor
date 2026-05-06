<template>
  <div class="page-surface admin-page">
    <div class="page-hero admin-hero">
      <div>
        <h2>{{ t('title') }}</h2>
        <p>{{ t('subtitle') }}</p>
      </div>
      <div class="inline-actions">
        <el-button plain @click="$router.push('/')">{{ t('back') }}</el-button>
      </div>
    </div>

    <section class="card-panel admin-shell-card">
      <div class="section-head admin-shell-head">
        <div>
          <h3 class="section-title">{{ t('workspaceTitle') }}</h3>
          <p class="section-subtitle">{{ t('workspaceSubtitle') }}</p>
        </div>
      </div>

      <div class="admin-tab-shell">
        <el-menu
          :default-active="$route.path"
          router
          mode="horizontal"
          class="admin-top-menu"
          background-color="transparent"
          text-color="var(--text-2)"
          active-text-color="var(--text-1)">
          <el-menu-item index="/admin/users">{{ t('users') }}</el-menu-item>
          <el-menu-item index="/admin/stats">{{ t('stats') }}</el-menu-item>
        </el-menu>
      </div>

      <div class="admin-content-wrap">
        <router-view />
      </div>
    </section>
  </div>
</template>

<script setup>
import { useI18n } from '../../composables/useI18n'

const { t } = useI18n({
  title: { zh: '管理后台', en: 'Admin Console' },
  subtitle: { zh: '集中处理用户治理、权限运营和平台运行统计。', en: 'Centralized governance for users, permissions, and platform operations.' },
  back: { zh: '返回监控控制台', en: 'Back to Console' },
  users: { zh: '用户管理', en: 'User Management' },
  stats: { zh: '系统统计', en: 'System Stats' },
  workspaceTitle: { zh: '后台工作区', en: 'Admin Workspace' },
  workspaceSubtitle: { zh: '通过顶部标签在用户治理和平台统计之间切换。', en: 'Switch between user governance and platform statistics using the tabs above.' }
})
</script>

<style scoped>
.admin-page {
  gap: 20px;
}

.admin-hero {
  background: radial-gradient(760px 220px at 0 0, rgba(56, 189, 248, 0.12), transparent 60%), var(--hero-bg);
}

.admin-shell-card {
  padding: 20px;
}

.admin-shell-head {
  margin-bottom: 18px;
}

.admin-tab-shell {
  margin-bottom: 20px;
}

.admin-top-menu {
  border-bottom: 1px solid rgba(148, 163, 184, 0.16) !important;
}

.admin-top-menu :deep(.el-menu-item) {
  height: 46px;
  margin-right: 8px;
  border-radius: 12px 12px 0 0;
  font-weight: 600;
}

.admin-top-menu :deep(.el-menu-item.is-active) {
  background: rgba(56, 189, 248, 0.1) !important;
  border-bottom-color: var(--brand) !important;
}

.admin-content-wrap {
  display: grid;
  gap: 16px;
}
</style>
