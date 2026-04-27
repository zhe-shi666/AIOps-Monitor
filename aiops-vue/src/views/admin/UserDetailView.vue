<template>
  <div>
    <el-button @click="$router.back()" class="mb-6">← 返回</el-button>

    <div v-if="user" class="space-y-6">
      <div class="p-6 rounded-xl border border-slate-700 bg-slate-900/50">
        <h2 class="text-lg font-bold text-slate-100 mb-4">用户信息</h2>
        <div class="grid grid-cols-2 gap-4 text-sm">
          <div><span class="text-slate-400">用户名：</span><span class="text-slate-100">{{ user.username }}</span></div>
          <div><span class="text-slate-400">邮箱：</span><span class="text-slate-100">{{ user.email }}</span></div>
          <div>
            <span class="text-slate-400">角色：</span>
            <el-select v-model="user.role" size="small" @change="handleRoleChange" style="width: 120px;">
              <el-option label="USER" value="USER" />
              <el-option label="ADMIN" value="ADMIN" />
            </el-select>
          </div>
          <div><span class="text-slate-400">注册时间：</span><span class="text-slate-100">{{ formatDate(user.createdAt) }}</span></div>
        </div>
      </div>

      <div class="p-6 rounded-xl border border-slate-700 bg-slate-900/50">
        <h3 class="text-base font-semibold text-slate-100 mb-4">监控目标（{{ targets.length }}）</h3>
        <el-table :data="targets" size="small"
          :header-cell-style="{ background: '#1e293b', color: '#94a3b8' }"
          :row-style="{ background: '#0f172a', color: '#e2e8f0' }">
          <el-table-column prop="name" label="名称" />
          <el-table-column prop="hostname" label="主机名" />
          <el-table-column prop="description" label="描述" />
          <el-table-column prop="createdAt" label="添加时间">
            <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <div v-else-if="loading" class="text-slate-400 text-center py-20">加载中...</div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getUserDetail, updateUserRole } from '../../api/admin'

const route = useRoute()
const user = ref(null)
const targets = ref([])
const loading = ref(true)

async function fetchDetail() {
  try {
    const { data } = await getUserDetail(route.params.id)
    user.value = data.user
    targets.value = data.targets || []
  } catch (e) {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

async function handleRoleChange(role) {
  try {
    await updateUserRole(user.value.id, role)
    ElMessage.success('角色已更新')
  } catch (e) {
    ElMessage.error('更新失败')
  }
}

function formatDate(dt) {
  if (!dt) return '-'
  return new Date(dt).toLocaleString('zh-CN')
}

onMounted(fetchDetail)
</script>
