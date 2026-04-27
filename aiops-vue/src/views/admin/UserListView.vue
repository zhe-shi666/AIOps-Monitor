<template>
  <div>
    <div class="flex items-center justify-between mb-6">
      <h2 class="text-xl font-bold text-slate-100">用户管理</h2>
      <el-input v-model="search" placeholder="搜索用户名..." clearable style="width: 240px;"
        @input="fetchUsers" />
    </div>

    <el-table :data="users" v-loading="loading" style="width: 100%;"
      :header-cell-style="{ background: '#1e293b', color: '#94a3b8' }"
      :row-style="{ background: '#0f172a', color: '#e2e8f0' }">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="username" label="用户名" />
      <el-table-column prop="email" label="邮箱" />
      <el-table-column prop="role" label="角色" width="100">
        <template #default="{ row }">
          <el-tag :type="row.role === 'ADMIN' ? 'warning' : 'info'" size="small">{{ row.role }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="enabled" label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.enabled ? 'success' : 'danger'" size="small">
            {{ row.enabled ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="注册时间" width="180">
        <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button size="small" @click="$router.push(`/admin/users/${row.id}`)">详情</el-button>
          <el-button size="small" :type="row.enabled ? 'warning' : 'success'"
            @click="toggleStatus(row)">{{ row.enabled ? '禁用' : '启用' }}</el-button>
          <el-popconfirm title="确认删除该用户？" @confirm="handleDelete(row.id)">
            <template #reference>
              <el-button size="small" type="danger">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <div class="flex justify-end mt-4">
      <el-pagination v-model:current-page="page" v-model:page-size="pageSize"
        :total="total" layout="total, prev, pager, next" @current-change="fetchUsers" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getUsers, updateUserStatus, deleteUser } from '../../api/admin'

const users = ref([])
const loading = ref(false)
const search = ref('')
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

async function fetchUsers() {
  loading.value = true
  try {
    const { data } = await getUsers({ page: page.value - 1, size: pageSize.value, username: search.value })
    users.value = data.content
    total.value = data.totalElements
  } catch (e) {
    ElMessage.error('加载用户列表失败')
  } finally {
    loading.value = false
  }
}

async function toggleStatus(row) {
  try {
    await updateUserStatus(row.id, !row.enabled)
    row.enabled = !row.enabled
    ElMessage.success('状态已更新')
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

async function handleDelete(id) {
  try {
    await deleteUser(id)
    ElMessage.success('删除成功')
    fetchUsers()
  } catch (e) {
    ElMessage.error('删除失败')
  }
}

function formatDate(dt) {
  if (!dt) return '-'
  return new Date(dt).toLocaleString('zh-CN')
}

onMounted(fetchUsers)
</script>
