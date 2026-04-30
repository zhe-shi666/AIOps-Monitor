<template>
  <div class="space-y-5">
    <div class="section-head">
      <div>
        <h2 class="section-title">{{ t('title') }}</h2>
        <p class="section-subtitle">{{ t('subtitle') }}</p>
      </div>
      <el-button @click="$router.back()" plain>{{ t('back') }}</el-button>
    </div>

    <div v-if="user" class="space-y-6">
      <div class="card-panel p-6">
        <h2 class="text-lg font-bold text-slate-100 mb-4">{{ t('profileTitle') }}</h2>
        <div class="grid grid-cols-2 gap-4 text-sm">
          <div><span class="text-slate-400">{{ t('username') }}：</span><span class="text-slate-100">{{ user.username }}</span></div>
          <div><span class="text-slate-400">{{ t('email') }}：</span><span class="text-slate-100">{{ user.email }}</span></div>
          <div>
            <span class="text-slate-400">{{ t('role') }}：</span>
            <el-select v-model="user.role" size="small" @change="handleRoleChange" style="width: 120px;">
              <el-option label="USER" value="USER" />
              <el-option label="AUDITOR" value="AUDITOR" />
              <el-option label="OPS" value="OPS" />
              <el-option label="ADMIN" value="ADMIN" />
            </el-select>
          </div>
          <div><span class="text-slate-400">{{ t('createdAt') }}：</span><span class="text-slate-100">{{ formatDate(user.createdAt) }}</span></div>
        </div>
      </div>

      <div class="card-panel p-6">
        <h3 class="text-base font-semibold text-slate-100 mb-4">{{ t('targetsTitle') }}（{{ targets.length }}）</h3>
        <div class="ops-table">
          <el-table :data="targets" size="small">
            <el-table-column prop="name" :label="t('name')" />
            <el-table-column prop="hostname" :label="t('hostname')" />
            <el-table-column prop="description" :label="t('description')" />
            <el-table-column prop="createdAt" :label="t('addedAt')">
              <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </div>

    <div v-else-if="loading" class="text-slate-400 text-center py-20">{{ t('loading') }}</div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getUserDetail, updateUserRole } from '../../api/admin'
import { useI18n } from '../../composables/useI18n'

const route = useRoute()
const user = ref(null)
const targets = ref([])
const loading = ref(true)

const { locale, t } = useI18n({
  title: { zh: '用户详情', en: 'User Detail' },
  subtitle: { zh: '查看账户信息、调整角色并审计其监控资产', en: 'Inspect account info, update role and audit target assets' },
  back: { zh: '返回', en: 'Back' },
  profileTitle: { zh: '用户信息', en: 'User Profile' },
  username: { zh: '用户名', en: 'Username' },
  email: { zh: '邮箱', en: 'Email' },
  role: { zh: '角色', en: 'Role' },
  createdAt: { zh: '注册时间', en: 'Created At' },
  targetsTitle: { zh: '监控目标', en: 'Targets' },
  name: { zh: '名称', en: 'Name' },
  hostname: { zh: '主机名', en: 'Hostname' },
  description: { zh: '描述', en: 'Description' },
  addedAt: { zh: '添加时间', en: 'Added At' },
  loading: { zh: '加载中...', en: 'Loading...' },
  loadFailed: { zh: '加载失败', en: 'Load failed' },
  roleUpdated: { zh: '角色已更新', en: 'Role updated' },
  roleUpdateFailed: { zh: '更新失败', en: 'Update failed' }
})

async function fetchDetail() {
  try {
    const { data } = await getUserDetail(route.params.id)
    user.value = data.user
    targets.value = data.targets || []
  } catch (_e) {
    ElMessage.error(t('loadFailed'))
  } finally {
    loading.value = false
  }
}

async function handleRoleChange(role) {
  try {
    await updateUserRole(user.value.id, role)
    ElMessage.success(t('roleUpdated'))
  } catch (_e) {
    ElMessage.error(t('roleUpdateFailed'))
  }
}

function formatDate(value) {
  if (!value) return '-'
  const localeCode = locale.value === 'zh' ? 'zh-CN' : 'en-US'
  return new Date(value).toLocaleString(localeCode)
}

onMounted(fetchDetail)
</script>
