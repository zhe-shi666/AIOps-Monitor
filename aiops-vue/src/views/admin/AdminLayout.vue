<template>
  <div class="min-h-screen bg-slate-950 flex">
    <!-- 侧边栏 -->
    <el-menu
      :default-active="$route.path"
      router
      class="admin-sidebar"
      background-color="#0f172a"
      text-color="#94a3b8"
      active-text-color="#22d3ee"
      style="width: 220px; min-height: 100vh; border-right: 1px solid #1e293b;">
      <div class="px-6 py-5 border-b border-slate-800">
        <p class="text-slate-100 font-bold text-sm">🛡️ 管理后台</p>
        <p class="text-slate-500 text-xs mt-1">{{ auth.username }}</p>
      </div>
      <el-menu-item index="/admin/users">
        <el-icon><User /></el-icon>
        <span>用户管理</span>
      </el-menu-item>
      <el-menu-item index="/admin/stats">
        <el-icon><DataAnalysis /></el-icon>
        <span>系统统计</span>
      </el-menu-item>
      <el-menu-item index="/" style="margin-top: auto;">
        <el-icon><Monitor /></el-icon>
        <span>返回监控</span>
      </el-menu-item>
    </el-menu>

    <!-- 内容区 -->
    <div class="flex-1 p-8 overflow-auto">
      <router-view />
    </div>
  </div>
</template>

<script setup>
import { User, DataAnalysis, Monitor } from '@element-plus/icons-vue'
import { useAuthStore } from '../../stores/auth'
const auth = useAuthStore()
</script>
