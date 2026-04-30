<template>
  <div class="page-surface targets-page">
    <div class="page-hero targets-hero">
      <div>
        <h1>{{ t('title') }}</h1>
        <p>{{ t('subtitle') }}</p>
      </div>
      <div class="inline-actions">
        <el-button @click="fetchTargets" :loading="loading">{{ t('refresh') }}</el-button>
        <el-button type="primary" :disabled="!canWriteTargets" @click="openCreateDialog">{{ t('create') }}</el-button>
      </div>
    </div>

    <section class="kpi-grid">
      <div class="kpi-item">
        <p class="kpi-label">{{ t('totalTargets') }}</p>
        <p class="kpi-value">{{ targets.length }}</p>
      </div>
      <div class="kpi-item">
        <p class="kpi-label">{{ t('onlineTargets') }}</p>
        <p class="kpi-value text-emerald-300">{{ onlineCount }}</p>
      </div>
      <div class="kpi-item">
        <p class="kpi-label">{{ t('offlineTargets') }}</p>
        <p class="kpi-value text-amber-300">{{ offlineCount }}</p>
      </div>
      <div class="kpi-item">
        <p class="kpi-label">{{ t('disabledTargets') }}</p>
        <p class="kpi-value text-slate-300">{{ disabledCount }}</p>
      </div>
    </section>

    <section class="target-layout">
      <div class="card-panel table-wrap ops-table target-main-card">
        <div class="section-head">
          <div>
            <h2 class="section-title">{{ t('inventory') }}</h2>
            <p class="section-subtitle">{{ t('inventoryDesc') }}</p>
          </div>
          <div class="target-filters">
            <el-input v-model="keyword" clearable :placeholder="t('searchPlaceholder')" />
            <el-segmented v-model="statusFilter" :options="statusOptions" />
          </div>
        </div>

        <div class="target-list" v-loading="loading">
          <article
            v-for="target in filteredTargets"
            :key="target.id"
            class="target-card"
            :class="{ selected: selectedTarget?.id === target.id, offline: !isOnline(target), disabled: !target.enabled }"
            @click="selectTarget(target)">
            <div class="target-card-top">
              <div>
                <div class="target-name-row">
                  <span class="status-dot" :class="statusTone(target)"></span>
                  <h3>{{ target.name }}</h3>
                </div>
                <p>{{ target.hostname || t('unknownHost') }} · {{ target.ipAddress || t('unknownIp') }}</p>
              </div>
              <el-tag size="small" :type="statusTagType(target)">{{ normalizedStatus(target) }}</el-tag>
            </div>

            <div class="target-meta-grid">
              <div>
                <span>{{ t('agentVersion') }}</span>
                <strong>{{ target.agentVersion || '-' }}</strong>
                <el-tag v-if="target.agentOutdated" size="small" type="warning">{{ t('outdated') }}</el-tag>
              </div>
              <div>
                <span>{{ t('lastHeartbeat') }}</span>
                <strong>{{ relativeHeartbeat(target.lastHeartbeatAt) }}</strong>
              </div>
              <div>
                <span>{{ t('agentKey') }}</span>
                <strong class="mono">{{ target.agentKeyPreview || '****' }}</strong>
              </div>
            </div>

            <div v-if="!isOnline(target) && target.enabled" class="target-offline-note">
              {{ t('offlineHint') }} {{ formatDate(target.lastHeartbeatAt) }}
            </div>
          </article>

          <el-empty v-if="!loading && !filteredTargets.length" :description="t('emptyTargets')" />
        </div>
      </div>

      <aside class="card-panel target-detail-card" v-if="selectedTarget">
        <div class="detail-head">
          <div>
            <p class="detail-eyebrow">{{ t('selectedTarget') }}</p>
            <h2>{{ selectedTarget.name }}</h2>
            <p>{{ selectedTarget.description || t('noDescription') }}</p>
          </div>
          <el-switch v-model="selectedTarget.enabled" :disabled="!canWriteTargets" @change="toggleEnabled(selectedTarget)" />
        </div>

        <div class="detail-status-strip" :class="statusTone(selectedTarget)">
          <span>{{ normalizedStatus(selectedTarget) }}</span>
          <strong>{{ healthLabel(selectedTarget) }}</strong>
        </div>

        <el-tabs v-model="detailTab" class="target-tabs">
          <el-tab-pane :label="t('tabInstall')" name="install">
            <div class="install-steps">
              <div class="step-box">
                <span>1</span>
                <p>{{ t('stepCreate') }}</p>
              </div>
              <div class="step-box">
                <span>2</span>
                <p>{{ t('stepRun') }}</p>
              </div>
              <div class="step-box">
                <span>3</span>
                <p>{{ t('stepVerify') }}</p>
              </div>
            </div>

            <div class="endpoint-grid">
              <el-input v-model="agentPackageBaseUrl" :placeholder="t('agentPackageBaseUrl')">
                <template #prepend>{{ t('agentPackageBaseUrl') }}</template>
              </el-input>
              <el-input v-model="apiBaseUrl" :placeholder="t('apiBaseUrl')">
                <template #prepend>{{ t('apiBaseUrl') }}</template>
              </el-input>
            </div>

            <el-segmented v-model="installPlatform" :options="platformOptions" class="platform-switch" />

            <div class="command-card">
              <div class="command-head">
                <span>{{ t('installCommand') }}</span>
                <el-button size="small" type="primary" plain @click="copyText(installCommand)">{{ t('copyCommand') }}</el-button>
              </div>
              <pre>{{ installCommand }}</pre>
            </div>

            <div class="command-card soft">
              <div class="command-head">
                <span>{{ t('envConfig') }}</span>
                <el-button size="small" plain @click="copyText(envSnippet)">{{ t('copy') }}</el-button>
              </div>
              <pre>{{ envSnippet }}</pre>
            </div>
          </el-tab-pane>

          <el-tab-pane :label="t('tabOps')" name="ops">
            <div v-if="!canWriteTargets" class="note-box target-note readonly-note">
              {{ t('readonlyNote') }}
            </div>
            <div class="ops-grid">
              <button type="button" :disabled="!canWriteTargets" @click="openEditDialog(selectedTarget)">{{ t('edit') }}</button>
              <button type="button" @click="copyText(selectedTarget.agentKey)">{{ t('copyKey') }}</button>
              <button type="button" :disabled="!canWriteTargets" @click="rotateKey(selectedTarget)">{{ t('rotateKey') }}</button>
              <button type="button" @click="copyText(restartCommand)">{{ t('copyRestart') }}</button>
              <button type="button" @click="copyText(serviceCommand)">{{ t('copyServiceInstall') }}</button>
            </div>
            <div class="note-box target-note">
              {{ t('opsNote') }}
            </div>
            <el-popconfirm :title="t('deleteConfirmTarget')" @confirm="removeTarget(selectedTarget.id)">
              <template #reference>
                <el-button type="danger" plain :disabled="!canWriteTargets">{{ t('delete') }}</el-button>
              </template>
            </el-popconfirm>
          </el-tab-pane>

          <el-tab-pane :label="t('tabThreshold')" name="threshold">
            <div class="threshold-head">
              <el-switch v-model="thresholdForm.enabled" :disabled="!canWriteTargets" :active-text="t('enableOverride')" />
              <el-button size="small" :loading="thresholdLoading" @click="loadThresholds(selectedTarget.id)">
                {{ t('refresh') }}
              </el-button>
            </div>
            <el-form label-position="top" class="threshold-form">
              <el-form-item label="CPU %">
                <el-input-number v-model="thresholdForm.cpuThreshold" :min="1" :max="100" />
                <span>{{ t('effective') }} {{ thresholdForm.effectiveCpuThreshold ?? '-' }}</span>
              </el-form-item>
              <el-form-item :label="t('memoryThreshold')">
                <el-input-number v-model="thresholdForm.memoryThreshold" :min="1" :max="100" />
                <span>{{ t('effective') }} {{ thresholdForm.effectiveMemoryThreshold ?? '-' }}</span>
              </el-form-item>
              <el-form-item :label="t('diskThreshold')">
                <el-input-number v-model="thresholdForm.diskThreshold" :min="1" :max="100" />
                <span>{{ t('effective') }} {{ thresholdForm.effectiveDiskThreshold ?? '-' }}</span>
              </el-form-item>
              <el-form-item :label="t('processThreshold')">
                <el-input-number v-model="thresholdForm.processCountThreshold" :min="1" :max="100000" />
                <span>{{ t('effective') }} {{ thresholdForm.effectiveProcessCountThreshold ?? '-' }}</span>
              </el-form-item>
              <el-form-item :label="t('consecutiveBreach')">
                <el-input-number v-model="thresholdForm.consecutiveBreachCount" :min="1" :max="20" />
                <span>{{ t('effective') }} {{ thresholdForm.effectiveConsecutiveBreachCount ?? '-' }}</span>
              </el-form-item>
              <el-form-item :label="t('silenceSeconds')">
                <el-input-number v-model="thresholdForm.silenceSeconds" :min="10" :max="86400" />
                <span>{{ t('effective') }} {{ thresholdForm.effectiveSilenceSeconds ?? '-' }}</span>
              </el-form-item>
            </el-form>
            <el-button type="primary" :disabled="!canWriteTargets" :loading="thresholdSaving" @click="saveThresholds(selectedTarget.id)">
              {{ t('saveThreshold') }}
            </el-button>
          </el-tab-pane>

          <el-tab-pane :label="t('tabEvidence')" name="evidence">
            <div class="evidence-list">
              <div>
                <span>{{ t('metricsEvidence') }}</span>
                <strong>{{ t('metricsEvidenceDesc') }}</strong>
              </div>
              <div>
                <span>{{ t('logsEvidence') }}</span>
                <strong>{{ t('logsEvidenceDesc') }}</strong>
              </div>
              <div>
                <span>{{ t('traceEvidence') }}</span>
                <strong>{{ t('traceEvidenceDesc') }}</strong>
              </div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </aside>
    </section>

    <el-dialog v-model="dialogVisible" :title="isEdit ? t('editDialog') : t('createDialog')" width="560px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="96px">
        <el-form-item :label="t('targetName')" prop="name">
          <el-input v-model="form.name" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item :label="t('hostname')">
          <el-input v-model="form.hostname" maxlength="255" />
        </el-form-item>
        <el-form-item :label="t('description')">
          <el-input v-model="form.description" type="textarea" :rows="3" maxlength="255" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ t('cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="submitForm">{{ isEdit ? t('save') : t('createAction') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createTarget, deleteTarget, getTargetThresholds, getTargets, rotateTargetKey, updateTarget, updateTargetThresholds } from '../api/targets'
import { useAuthStore } from '../stores/auth'
import { useI18n } from '../composables/useI18n'
import { AGENT_BASE_URL, API_BASE_URL } from '../config/env'

const auth = useAuthStore()
const loading = ref(false)
const saving = ref(false)
const targets = ref([])
const selectedTargetId = ref(null)
const keyword = ref('')
const statusFilter = ref('ALL')
const detailTab = ref('install')
const installPlatform = ref('linux')
const apiBaseUrl = ref(API_BASE_URL)
const agentPackageBaseUrl = ref(AGENT_BASE_URL)
const dialogVisible = ref(false)
const isEdit = ref(false)
const editingId = ref(null)
const formRef = ref(null)
const thresholdLoading = ref(false)
const thresholdSaving = ref(false)

const form = reactive({ name: '', hostname: '', description: '' })
const thresholdForm = reactive({
  enabled: false,
  cpuThreshold: null,
  memoryThreshold: null,
  diskThreshold: null,
  processCountThreshold: null,
  consecutiveBreachCount: null,
  silenceSeconds: null,
  effectiveCpuThreshold: null,
  effectiveMemoryThreshold: null,
  effectiveDiskThreshold: null,
  effectiveProcessCountThreshold: null,
  effectiveConsecutiveBreachCount: null,
  effectiveSilenceSeconds: null
})

const { locale, t } = useI18n({
  title: { zh: '监控目标管理', en: 'Target Management' },
  subtitle: { zh: '面向多主机接入的 Agent 生命周期、状态与证据源管理', en: 'Agent lifecycle, status, and evidence management for multi-host monitoring' },
  refresh: { zh: '刷新', en: 'Refresh' },
  create: { zh: '新增目标', en: 'New Target' },
  totalTargets: { zh: '目标总数', en: 'Total Targets' },
  onlineTargets: { zh: '在线目标', en: 'Online Targets' },
  offlineTargets: { zh: '离线目标', en: 'Offline Targets' },
  disabledTargets: { zh: '已禁用目标', en: 'Disabled Targets' },
  inventory: { zh: '目标资产清单', en: 'Target Inventory' },
  inventoryDesc: { zh: '按目标选择右侧操作面板，避免表格横向拥挤', en: 'Select a target to operate in the right panel' },
  searchPlaceholder: { zh: '搜索名称 / 主机 / IP', en: 'Search name / host / IP' },
  all: { zh: '全部', en: 'All' },
  online: { zh: '在线', en: 'Online' },
  offline: { zh: '离线', en: 'Offline' },
  disabled: { zh: '禁用', en: 'Disabled' },
  unknownHost: { zh: '未知主机', en: 'Unknown host' },
  unknownIp: { zh: '未知 IP', en: 'Unknown IP' },
  agentVersion: { zh: 'Agent 版本', en: 'Agent Version' },
  outdated: { zh: '可升级', en: 'Outdated' },
  lastHeartbeat: { zh: '最近心跳', en: 'Last Heartbeat' },
  agentKey: { zh: 'Agent Key', en: 'Agent Key' },
  offlineHint: { zh: '目标已停止上报，最后心跳：', en: 'Target stopped reporting. Last heartbeat:' },
  emptyTargets: { zh: '暂无匹配目标', en: 'No matching targets' },
  selectedTarget: { zh: '当前目标', en: 'Selected Target' },
  noDescription: { zh: '暂无描述', en: 'No description' },
  healthy: { zh: '心跳正常，指标会持续刷新', en: 'Heartbeat healthy; metrics keep refreshing' },
  stale: { zh: '心跳超时，前端应置灰并保留最后数据', en: 'Heartbeat stale; UI should gray out last values' },
  disabledHealth: { zh: '目标已禁用，Agent 请求会被拒绝', en: 'Target disabled; agent requests are rejected' },
  tabInstall: { zh: '安装接入', en: 'Install' },
  tabOps: { zh: '运维操作', en: 'Operations' },
  tabThreshold: { zh: '目标阈值', en: 'Thresholds' },
  tabEvidence: { zh: '证据源', en: 'Evidence' },
  stepCreate: { zh: '在本页创建目标，生成唯一 agentKey。', en: 'Create target and generate an agentKey.' },
  stepRun: { zh: '选择目标系统，在被监控主机执行对应命令。', en: 'Choose OS and run the matching command on the monitored host.' },
  stepVerify: { zh: '回到本页确认状态 ONLINE、版本和 IP。', en: 'Verify ONLINE status, version, and IP here.' },
  agentPackageBaseUrl: { zh: '安装包地址', en: 'Agent Package URL' },
  apiBaseUrl: { zh: '后端地址', en: 'API URL' },
  installCommand: { zh: '安装/升级命令', en: 'Install / Upgrade Command' },
  copyCommand: { zh: '复制命令', en: 'Copy Command' },
  envConfig: { zh: '.env 核心配置', en: '.env Core Config' },
  copy: { zh: '复制', en: 'Copy' },
  edit: { zh: '编辑目标', en: 'Edit Target' },
  copyKey: { zh: '复制 Key', en: 'Copy Key' },
  rotateKey: { zh: '轮换 Key', en: 'Rotate Key' },
  copyRestart: { zh: '复制重启命令', en: 'Copy Restart' },
  copyServiceInstall: { zh: '复制服务化安装', en: 'Copy Service Install' },
  opsNote: { zh: '轮换 Key 会让旧 Agent 立即失效；企业现场建议先复制新 .env，再重启 Agent。', en: 'Rotating key invalidates old agents immediately; update .env before restart.' },
  readonlyNote: { zh: '当前角色为只读角色：可以查看目标、复制安装命令和证据源，但不能创建、删除、轮换 Key 或修改阈值。', en: 'Current role is read-only: you can inspect targets and copy commands, but cannot create, delete, rotate keys, or update thresholds.' },
  enableOverride: { zh: '启用目标级覆盖', en: 'Enable target override' },
  memoryThreshold: { zh: '内存 %', en: 'Memory %' },
  diskThreshold: { zh: '磁盘 %', en: 'Disk %' },
  processThreshold: { zh: '进程数', en: 'Process count' },
  consecutiveBreach: { zh: '连续次数', en: 'Consecutive count' },
  silenceSeconds: { zh: '静默秒数', en: 'Silence seconds' },
  effective: { zh: '生效值:', en: 'Effective:' },
  saveThreshold: { zh: '保存目标阈值', en: 'Save Target Thresholds' },
  thresholdSaved: { zh: '目标阈值已保存', en: 'Target thresholds saved' },
  thresholdLoadFailed: { zh: '加载目标阈值失败', en: 'Failed to load target thresholds' },
  thresholdSaveFailed: { zh: '保存目标阈值失败', en: 'Failed to save target thresholds' },
  delete: { zh: '删除目标', en: 'Delete Target' },
  deleteConfirmTarget: { zh: '确认删除该目标？相关历史数据不会自动清理。', en: 'Delete this target? Historical data will not be auto-cleaned.' },
  metricsEvidence: { zh: '指标', en: 'Metrics' },
  metricsEvidenceDesc: { zh: 'CPU / 内存 / 磁盘 / 网络 / 进程数，每次心跳入库。', en: 'CPU / memory / disk / network / process count on each heartbeat.' },
  logsEvidence: { zh: '日志', en: 'Logs' },
  logsEvidenceDesc: { zh: 'agent-lite 会周期性上报主机心跳日志，供 AI 专家引用。', en: 'agent-lite periodically reports host heartbeat logs for AI evidence.' },
  traceEvidence: { zh: 'Trace', en: 'Trace' },
  traceEvidenceDesc: { zh: '每次采集发送一条轻量 span，记录采集与发送耗时。', en: 'Each collection sends a lightweight span for duration tracking.' },
  targetName: { zh: '目标名称', en: 'Target Name' },
  hostname: { zh: '主机名', en: 'Hostname' },
  description: { zh: '描述', en: 'Description' },
  cancel: { zh: '取消', en: 'Cancel' },
  save: { zh: '保存', en: 'Save' },
  createAction: { zh: '创建', en: 'Create' },
  editDialog: { zh: '编辑监控目标', en: 'Edit Target' },
  createDialog: { zh: '新增监控目标', en: 'Create Target' },
  targetNameRequired: { zh: '请输入目标名称', en: 'Please enter target name' },
  loadFailed: { zh: '加载目标列表失败', en: 'Failed to load target list' },
  updated: { zh: '目标已更新', en: 'Target updated' },
  created: { zh: '目标已创建，请复制安装命令到目标机器执行', en: 'Target created. Copy install command to the host.' },
  updateFailed: { zh: '更新失败', en: 'Update failed' },
  createFailed: { zh: '创建失败', en: 'Create failed' },
  enabledSuccess: { zh: '目标已启用', en: 'Target enabled' },
  disabledSuccess: { zh: '目标已禁用', en: 'Target disabled' },
  statusUpdateFailed: { zh: '状态更新失败', en: 'Failed to update status' },
  rotateConfirm: { zh: '轮换后旧 key 立即失效，目标「{name}」上的 Agent 需要同步更新。是否继续？', en: 'After rotation, old key is invalid. Update agent on target "{name}". Continue?' },
  rotateTitle: { zh: '确认轮换', en: 'Confirm Rotation' },
  rotateContinue: { zh: '继续', en: 'Continue' },
  rotateCancel: { zh: '取消', en: 'Cancel' },
  keyRotated: { zh: 'Key 已轮换并复制到剪贴板', en: 'Key rotated and copied' },
  rotateFailed: { zh: '轮换失败', en: 'Rotation failed' },
  deleted: { zh: '目标已删除', en: 'Target deleted' },
  deleteFailed: { zh: '删除失败', en: 'Delete failed' },
  copied: { zh: '已复制', en: 'Copied' },
  copyFailed: { zh: '复制失败，请手动复制', en: 'Copy failed, please copy manually' },
  justNow: { zh: '刚刚', en: 'Just now' },
  minutesAgo: { zh: '{n} 分钟前', en: '{n} min ago' },
  hoursAgo: { zh: '{n} 小时前', en: '{n} hours ago' },
  daysAgo: { zh: '{n} 天前', en: '{n} days ago' }
})

const statusOptions = computed(() => [
  { label: t('all'), value: 'ALL' },
  { label: t('online'), value: 'ONLINE' },
  { label: t('offline'), value: 'OFFLINE' },
  { label: t('disabled'), value: 'DISABLED' }
])
const platformOptions = computed(() => [
  { label: 'Linux', value: 'linux' },
  { label: 'macOS', value: 'macos' },
  { label: 'Windows', value: 'windows' }
])

const rules = computed(() => ({ name: [{ required: true, message: t('targetNameRequired'), trigger: 'blur' }] }))
const canWriteTargets = computed(() => auth.isOperator)
const selectedTarget = computed(() => targets.value.find((item) => item.id === selectedTargetId.value) || targets.value[0] || null)
const onlineCount = computed(() => targets.value.filter(isOnline).length)
const disabledCount = computed(() => targets.value.filter((item) => !item.enabled).length)
const offlineCount = computed(() => targets.value.filter((item) => item.enabled && !isOnline(item)).length)
const filteredTargets = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  return targets.value.filter((item) => {
    const matchesKw = !kw || [item.name, item.hostname, item.ipAddress, item.agentVersion].some((v) => String(v || '').toLowerCase().includes(kw))
    const status = normalizedStatus(item)
    const matchesStatus = statusFilter.value === 'ALL' || status === statusFilter.value
    return matchesKw && matchesStatus
  })
})
const installCommand = computed(() => {
  if (!selectedTarget.value?.agentKey) return ''
  if (installPlatform.value === 'windows') return windowsInstallCommand.value
  if (installPlatform.value === 'macos') return macosInstallCommand.value
  return linuxInstallCommand.value
})
const envSnippet = computed(() => `API_BASE_URL=${apiBaseUrl.value}\nAGENT_KEY=${selectedTarget.value?.agentKey || ''}\nHOSTNAME_OVERRIDE=${selectedTarget.value?.hostname || 'demo-host-01'}\nAGENT_IP=\nAGENT_VERSION=agent-lite-1.2.0-cross\nINTERVAL_MS=5000\nOBSERVABILITY_ENABLED=true\nLOG_EVERY_N_HEARTBEATS=12`)
const linuxInstallCommand = computed(() => `curl -fsSL ${agentPackageBaseUrl.value}/install-or-upgrade.sh | bash\ncd ~/aiops-agent-lite\ncat > .env <<'EOF'\n${envSnippet.value}\nEOF\n./stop.sh && ./start.sh && ./tail-log.sh`)
const macosInstallCommand = computed(() => `curl -fL ${agentPackageBaseUrl.value}/aiops-agent-lite.tar.gz -o ~/aiops-agent-lite.tar.gz\nrm -rf ~/aiops-agent-lite\ntar -xzf ~/aiops-agent-lite.tar.gz -C ~\ncd ~/aiops-agent-lite\ncat > .env <<'EOF'\n${envSnippet.value}\nEOF\nchmod +x *.sh\n./stop.sh && ./start.sh && ./tail-log.sh`)
const windowsInstallCommand = computed(() => `$base = \"${agentPackageBaseUrl.value}\"\n$pkg = Join-Path $HOME \"aiops-agent-lite.tar.gz\"\nInvoke-WebRequest \"$base/aiops-agent-lite.tar.gz\" -OutFile $pkg\nif (Test-Path \"$HOME\\aiops-agent-lite\") { Remove-Item \"$HOME\\aiops-agent-lite\" -Recurse -Force }\ntar -xzf $pkg -C $HOME\nSet-Location \"$HOME\\aiops-agent-lite\"\n@'\n${envSnippet.value}\n'@ | Set-Content .env -Encoding UTF8\npowershell -ExecutionPolicy Bypass -File .\\stop.ps1\npowershell -ExecutionPolicy Bypass -File .\\start.ps1\npowershell -ExecutionPolicy Bypass -File .\\tail-log.ps1`)
const restartCommand = computed(() => installPlatform.value === 'windows'
  ? `Set-Location \"$HOME\\aiops-agent-lite\"\npowershell -ExecutionPolicy Bypass -File .\\stop.ps1\npowershell -ExecutionPolicy Bypass -File .\\start.ps1\npowershell -ExecutionPolicy Bypass -File .\\tail-log.ps1`
  : `cd ~/aiops-agent-lite\n./stop.sh\n./start.sh\n./tail-log.sh`)
const serviceCommand = computed(() => {
  if (installPlatform.value === 'windows') {
    return `Set-Location \"$HOME\\aiops-agent-lite\"\npowershell -ExecutionPolicy Bypass -File .\\install-service.ps1`
  }
  if (installPlatform.value === 'macos') {
    return `cd ~/aiops-agent-lite\n./install-launchd.sh\n./tail-log.sh`
  }
  return `cd ~/aiops-agent-lite\nsudo ./install-service.sh\njournalctl -u aiops-agent-lite -f`
})

watch(selectedTarget, (target) => {
  if (target?.id) {
    selectedTargetId.value = target.id
    loadThresholds(target.id)
  }
}, { immediate: true })

function normalizedStatus(target) {
  if (!target?.enabled) return 'DISABLED'
  return isOnline(target) ? 'ONLINE' : 'OFFLINE'
}

function isOnline(target) {
  if (!target?.enabled) return false
  if ((target.status || '').toUpperCase() !== 'ONLINE') return false
  if (!target.lastHeartbeatAt) return false
  return Date.now() - new Date(target.lastHeartbeatAt).getTime() <= 120000
}

function statusTone(target) {
  const status = normalizedStatus(target)
  if (status === 'ONLINE') return 'online'
  if (status === 'DISABLED') return 'disabled'
  return 'offline'
}

function statusTagType(target) {
  const status = normalizedStatus(target)
  if (status === 'ONLINE') return 'success'
  if (status === 'DISABLED') return 'info'
  return 'warning'
}

function healthLabel(target) {
  if (!target?.enabled) return t('disabledHealth')
  return isOnline(target) ? t('healthy') : t('stale')
}

function selectTarget(target) {
  selectedTargetId.value = target.id
}

async function fetchTargets() {
  loading.value = true
  try {
    const { data } = await getTargets()
    targets.value = Array.isArray(data) ? data : []
    if (!selectedTargetId.value && targets.value.length) selectedTargetId.value = targets.value[0].id
  } catch (_e) {
    ElMessage.error(t('loadFailed'))
  } finally {
    loading.value = false
  }
}

function resetForm() {
  form.name = ''
  form.hostname = ''
  form.description = ''
  editingId.value = null
  isEdit.value = false
}

function openCreateDialog() {
  resetForm()
  dialogVisible.value = true
}

function openEditDialog(row) {
  isEdit.value = true
  editingId.value = row.id
  form.name = row.name || ''
  form.hostname = row.hostname || ''
  form.description = row.description || ''
  dialogVisible.value = true
}

async function submitForm() {
  try { await formRef.value.validate() } catch { return }
  saving.value = true
  try {
    let savedId = editingId.value
    if (isEdit.value && editingId.value) {
      await updateTarget(editingId.value, { name: form.name, hostname: form.hostname, description: form.description })
      ElMessage.success(t('updated'))
    } else {
      const { data } = await createTarget({ name: form.name, hostname: form.hostname, description: form.description })
      savedId = data?.id
      ElMessage.success(t('created'))
    }
    dialogVisible.value = false
    await fetchTargets()
    if (savedId) selectedTargetId.value = savedId
  } catch (_e) {
    ElMessage.error(isEdit.value ? t('updateFailed') : t('createFailed'))
  } finally {
    saving.value = false
  }
}

async function toggleEnabled(row) {
  if (!canWriteTargets.value) return
  const value = row.enabled
  try {
    await updateTarget(row.id, { enabled: value })
    await fetchTargets()
    ElMessage.success(value ? t('enabledSuccess') : t('disabledSuccess'))
  } catch (_e) {
    row.enabled = !value
    ElMessage.error(t('statusUpdateFailed'))
  }
}

async function rotateKey(row) {
  if (!canWriteTargets.value) return
  try {
    await ElMessageBox.confirm(t('rotateConfirm', { name: row.name }), t('rotateTitle'), {
      type: 'warning', confirmButtonText: t('rotateContinue'), cancelButtonText: t('rotateCancel')
    })
    const { data } = await rotateTargetKey(row.id)
    await fetchTargets()
    selectedTargetId.value = row.id
    await copyText(data.agentKey)
    ElMessage.success(t('keyRotated'))
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(t('rotateFailed'))
  }
}

async function loadThresholds(id) {
  if (!id) return
  thresholdLoading.value = true
  try {
    const { data } = await getTargetThresholds(id)
    Object.assign(thresholdForm, data || {})
  } catch (_e) {
    ElMessage.error(t('thresholdLoadFailed'))
  } finally {
    thresholdLoading.value = false
  }
}

async function saveThresholds(id) {
  if (!canWriteTargets.value) return
  if (!id) return
  thresholdSaving.value = true
  try {
    const payload = {
      enabled: thresholdForm.enabled,
      cpuThreshold: thresholdForm.cpuThreshold,
      memoryThreshold: thresholdForm.memoryThreshold,
      diskThreshold: thresholdForm.diskThreshold,
      processCountThreshold: thresholdForm.processCountThreshold,
      consecutiveBreachCount: thresholdForm.consecutiveBreachCount,
      silenceSeconds: thresholdForm.silenceSeconds
    }
    const { data } = await updateTargetThresholds(id, payload)
    Object.assign(thresholdForm, data || {})
    ElMessage.success(t('thresholdSaved'))
  } catch (_e) {
    ElMessage.error(t('thresholdSaveFailed'))
  } finally {
    thresholdSaving.value = false
  }
}

async function removeTarget(id) {
  if (!canWriteTargets.value) return
  try {
    await deleteTarget(id)
    ElMessage.success(t('deleted'))
    selectedTargetId.value = null
    await fetchTargets()
  } catch (_e) {
    ElMessage.error(t('deleteFailed'))
  }
}

async function copyText(text) {
  if (!text) return
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success(t('copied'))
  } catch (_e) {
    ElMessage.error(t('copyFailed'))
  }
}

function relativeHeartbeat(value) {
  if (!value) return '-'
  const diff = Math.max(0, Date.now() - new Date(value).getTime())
  const mins = Math.floor(diff / 60000)
  if (mins < 1) return t('justNow')
  if (mins < 60) return t('minutesAgo', { n: mins })
  const hours = Math.floor(mins / 60)
  if (hours < 24) return t('hoursAgo', { n: hours })
  return t('daysAgo', { n: Math.floor(hours / 24) })
}

function formatDate(value) {
  if (!value) return '-'
  const localeCode = locale.value === 'zh' ? 'zh-CN' : 'en-US'
  return new Date(value).toLocaleString(localeCode)
}

onMounted(fetchTargets)
</script>

<style scoped>
.targets-page { gap: 16px; }
.targets-hero { background: radial-gradient(900px 220px at 0 0, rgba(34, 197, 94, 0.16), transparent 60%), var(--hero-bg); }
.target-layout { display: grid; grid-template-columns: minmax(0, 1.2fr) minmax(360px, 0.8fr); gap: 16px; align-items: start; }
.target-main-card, .target-detail-card { min-width: 0; }
.target-detail-card { padding: 16px; position: sticky; top: 0; }
.target-filters { display: grid; grid-template-columns: minmax(180px, 1fr) auto; gap: 10px; align-items: center; }
.target-list { display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 12px; }
.target-card { border: 1px solid var(--line); border-radius: 16px; padding: 14px; background: var(--panel-soft); cursor: pointer; transition: all 0.2s ease; }
.target-card:hover, .target-card.selected { border-color: var(--line-strong); transform: translateY(-1px); background: var(--panel-strong); }
.target-card.offline { opacity: 0.78; }
.target-card.disabled { opacity: 0.62; }
.target-card-top, .target-name-row, .detail-head, .command-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 10px; }
.target-name-row { justify-content: flex-start; align-items: center; }
.target-name-row h3, .detail-head h2 { margin: 0; font-size: 16px; color: var(--text-1); }
.target-card-top p, .detail-head p, .detail-eyebrow { margin: 4px 0 0; color: var(--text-3); font-size: 12px; }
.status-dot { width: 9px; height: 9px; border-radius: 999px; background: var(--warning); box-shadow: 0 0 14px rgba(245, 158, 11, 0.7); }
.status-dot.online, .detail-status-strip.online { background: rgba(16, 185, 129, 0.14); }
.status-dot.online { background: var(--success); box-shadow: 0 0 14px rgba(52, 211, 153, 0.85); }
.status-dot.disabled { background: #64748b; box-shadow: none; }
.target-meta-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 8px; margin-top: 14px; }
.target-meta-grid div { border: 1px solid var(--line); border-radius: 12px; padding: 9px; background: rgba(2, 6, 23, 0.08); }
.target-meta-grid span, .evidence-list span { display: block; color: var(--text-3); font-size: 11px; }
.target-meta-grid strong, .evidence-list strong { display: block; color: var(--text-1); font-size: 12px; margin-top: 4px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.mono { font-family: ui-monospace, SFMono-Regular, Menlo, monospace; color: var(--brand) !important; }
.target-offline-note { margin-top: 10px; padding: 9px 10px; border-radius: 12px; border: 1px dashed rgba(245, 158, 11, 0.45); color: var(--warning); font-size: 12px; }
.detail-head { align-items: center; }
.detail-eyebrow { text-transform: uppercase; letter-spacing: 0.12em; }
.detail-status-strip { margin: 14px 0; border: 1px solid var(--line); border-radius: 14px; padding: 12px; display: flex; align-items: center; justify-content: space-between; color: var(--text-2); background: var(--panel-soft); }
.detail-status-strip.online { border-color: rgba(52, 211, 153, 0.35); }
.detail-status-strip.offline { border-color: rgba(245, 158, 11, 0.35); background: rgba(245, 158, 11, 0.08); }
.detail-status-strip.disabled { border-color: rgba(100, 116, 139, 0.35); opacity: 0.8; }
.install-steps { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; margin-bottom: 12px; }
.endpoint-grid { display: grid; grid-template-columns: 1fr; gap: 8px; margin-bottom: 12px; }
.platform-switch { margin-bottom: 12px; }
.step-box { border: 1px solid var(--line); border-radius: 12px; padding: 10px; background: var(--panel-soft); }
.step-box span { display: inline-grid; place-items: center; width: 22px; height: 22px; border-radius: 999px; background: rgba(14, 165, 233, 0.18); color: var(--brand); font-weight: 800; }
.step-box p { margin: 8px 0 0; font-size: 12px; color: var(--text-2); line-height: 1.45; }
.command-card { border: 1px solid var(--line); border-radius: 14px; padding: 12px; background: var(--code-pre-bg); margin-bottom: 12px; }
.command-card.soft { background: var(--panel-soft); }
.command-head { align-items: center; color: var(--text-2); font-size: 12px; font-weight: 700; margin-bottom: 8px; }
.command-card pre { margin: 0; white-space: pre-wrap; word-break: break-all; color: var(--code-text); font-size: 12px; line-height: 1.55; }
.ops-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px; margin-bottom: 12px; }
.ops-grid button { border: 1px solid var(--line); border-radius: 12px; padding: 12px; background: var(--panel-soft); color: var(--text-1); cursor: pointer; text-align: left; font-weight: 700; }
.ops-grid button:hover { border-color: var(--line-strong); color: var(--brand); }
.ops-grid button:disabled { cursor: not-allowed; opacity: 0.48; color: var(--text-3); }
.target-note { margin-bottom: 12px; }
.readonly-note { border-color: rgba(245, 158, 11, 0.35); color: var(--warning); }
.threshold-head { display: flex; align-items: center; justify-content: space-between; gap: 10px; margin-bottom: 12px; }
.threshold-form :deep(.el-form-item) { margin-bottom: 12px; }
.threshold-form :deep(.el-form-item__content) { display: flex; align-items: center; justify-content: space-between; gap: 10px; }
.threshold-form span { color: var(--text-3); font-size: 12px; }
.evidence-list { display: grid; gap: 10px; }
.evidence-list div { border: 1px solid var(--line); border-radius: 12px; padding: 12px; background: var(--panel-soft); }
@media (max-width: 1180px) { .target-layout { grid-template-columns: 1fr; } .target-detail-card { position: static; } }
@media (max-width: 760px) { .target-filters, .target-meta-grid, .install-steps, .ops-grid { grid-template-columns: 1fr; } .target-list { grid-template-columns: 1fr; } }
</style>
