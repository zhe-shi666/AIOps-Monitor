<template>
  <div class="space-y-5">
    <div class="section-head">
      <div>
        <h2 class="section-title">{{ t('title') }}</h2>
        <p class="section-subtitle">{{ t('subtitle') }}</p>
      </div>
      <el-input
        v-model="search"
        :placeholder="t('searchPlaceholder')"
        clearable
        style="width: 260px;"
        @input="fetchUsers" />
    </div>

    <div class="card-panel table-wrap ops-table">
      <el-table :data="users" v-loading="loading" style="width: 100%;">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" :label="t('username')" />
        <el-table-column prop="email" :label="t('email')" />
        <el-table-column prop="role" :label="t('role')" width="100">
          <template #default="{ row }">
            <el-tag :type="row.role === 'ADMIN' ? 'warning' : 'info'" size="small">{{ row.role }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="enabled" :label="t('status')" width="90">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'danger'" size="small">
              {{ row.enabled ? t('enabled') : t('disabled') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" :label="t('createdAt')" width="180">
          <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column :label="t('actions')" width="200">
          <template #default="{ row }">
            <el-button size="small" @click="$router.push(`/admin/users/${row.id}`)">{{ t('detail') }}</el-button>
            <el-button size="small" :type="row.enabled ? 'warning' : 'success'"
              @click="toggleStatus(row)">{{ row.enabled ? t('disable') : t('enable') }}</el-button>
            <el-popconfirm :title="t('deleteConfirm')" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button size="small" type="danger">{{ t('delete') }}</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="flex justify-end mt-4">
      <el-pagination v-model:current-page="page" v-model:page-size="pageSize"
        :total="total" layout="total, prev, pager, next" @current-change="fetchUsers" />
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getUsers, updateUserStatus, deleteUser } from '../../api/admin'
import { useI18n } from '../../composables/useI18n'

const users = ref([])
const loading = ref(false)
const search = ref('')
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

const { locale, t } = useI18n({
  title: { zh: '用户管理', en: 'User Management' },
  subtitle: { zh: '按用户名检索、禁用与角色授权', en: 'Search by username, disable users and manage roles' },
  searchPlaceholder: { zh: '搜索用户名...', en: 'Search username...' },
  username: { zh: '用户名', en: 'Username' },
  email: { zh: '邮箱', en: 'Email' },
  role: { zh: '角色', en: 'Role' },
  status: { zh: '状态', en: 'Status' },
  enabled: { zh: '启用', en: 'Enabled' },
  disabled: { zh: '禁用', en: 'Disabled' },
  createdAt: { zh: '注册时间', en: 'Created At' },
  actions: { zh: '操作', en: 'Actions' },
  detail: { zh: '详情', en: 'Detail' },
  disable: { zh: '禁用', en: 'Disable' },
  enable: { zh: '启用', en: 'Enable' },
  delete: { zh: '删除', en: 'Delete' },
  deleteConfirm: { zh: '确认删除该用户？', en: 'Delete this user?' },
  loadFailed: { zh: '加载用户列表失败', en: 'Failed to load users' },
  statusUpdated: { zh: '状态已更新', en: 'Status updated' },
  actionFailed: { zh: '操作失败', en: 'Action failed' },
  deleted: { zh: '删除成功', en: 'Deleted successfully' },
  deleteFailed: { zh: '删除失败', en: 'Delete failed' }
})

async function fetchUsers() {
  loading.value = true
  try {
    const { data } = await getUsers({ page: page.value - 1, size: pageSize.value, username: search.value })
    users.value = data.content
    total.value = data.totalElements
  } catch (_e) {
    ElMessage.error(t('loadFailed'))
  } finally {
    loading.value = false
  }
}

async function toggleStatus(row) {
  try {
    await updateUserStatus(row.id, !row.enabled)
    row.enabled = !row.enabled
    ElMessage.success(t('statusUpdated'))
  } catch (_e) {
    ElMessage.error(t('actionFailed'))
  }
}

async function handleDelete(id) {
  try {
    await deleteUser(id)
    ElMessage.success(t('deleted'))
    fetchUsers()
  } catch (_e) {
    ElMessage.error(t('deleteFailed'))
  }
}

function formatDate(value) {
  if (!value) return '-'
  const localeCode = locale.value === 'zh' ? 'zh-CN' : 'en-US'
  return new Date(value).toLocaleString(localeCode)
}

onMounted(fetchUsers)
</script>
