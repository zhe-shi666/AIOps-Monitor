<template>
  <div class="page-surface">
    <div class="page-hero">
      <div>
        <h2>{{ t('title') }}</h2>
        <p>{{ t('subtitle') }}</p>
      </div>
      <el-button plain @click="$router.push('/')">{{ t('back') }}</el-button>
    </div>

    <div class="card-panel p-2">
      <el-menu
        :default-active="$route.path"
        router
        mode="horizontal"
        background-color="transparent"
        text-color="#cbd5e1"
        active-text-color="#67e8f9">
        <el-menu-item index="/admin/users">{{ t('users') }}</el-menu-item>
        <el-menu-item index="/admin/stats">{{ t('stats') }}</el-menu-item>
      </el-menu>
    </div>

    <div class="card-panel p-4 md:p-6">
      <router-view />
    </div>
  </div>
</template>

<script setup>
import { useI18n } from '../../composables/useI18n'

const { t } = useI18n({
  title: { zh: '管理后台', en: 'Admin Console' },
  subtitle: { zh: '用户治理、权限管理与系统统计', en: 'User governance, access management, and system statistics' },
  back: { zh: '返回监控控制台', en: 'Back to Console' },
  users: { zh: '用户管理', en: 'User Management' },
  stats: { zh: '系统统计', en: 'System Stats' }
})
</script>
