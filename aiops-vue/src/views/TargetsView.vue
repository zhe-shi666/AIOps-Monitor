<template>
  <div class="page-surface targets-page">
    <div class="page-hero targets-hero">
      <div>
        <h1>{{ t('title') }}</h1>
        <p>{{ pageSubtitle }}</p>
      </div>
      <div class="inline-actions">
        <el-button @click="fetchTargets" :loading="loading">{{ t('refresh') }}</el-button>
        <el-button v-if="canManageTargets" type="primary" @click="openCreateDialog">{{ t('create') }}</el-button>
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

    <section class="card-panel target-inventory-card">
      <div class="section-head inventory-head">
        <div>
          <h2 class="section-title">{{ t('inventory') }}</h2>
          <p class="section-subtitle">{{ inventoryDescription }}</p>
        </div>
        <div class="target-filters">
          <el-input v-model="keyword" clearable :placeholder="t('searchPlaceholder')" />
          <el-segmented v-model="statusFilter" :options="statusOptions" />
        </div>
      </div>

      <div class="target-grid" v-loading="loading">
        <article
          v-for="target in filteredTargets"
          :key="target.id"
          class="target-card"
          :class="{ offline: !isOnline(target), disabled: !target.enabled }"
          @click="openDetailDialog(target)">
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

          <div class="target-stat-row">
            <div>
              <span>{{ t('agentVersion') }}</span>
              <strong>{{ target.agentVersion || '-' }}</strong>
            </div>
            <div>
              <span>{{ t('lastHeartbeat') }}</span>
              <strong>{{ relativeHeartbeat(target.lastHeartbeatAt) }}</strong>
            </div>
            <div>
              <span>{{ t('subscriberCount') }}</span>
              <strong>{{ target.subscriberCount ?? 0 }}</strong>
            </div>
          </div>

          <div class="target-card-footer">
            <span class="mono">{{ target.agentKeyPreview || '****' }}</span>
            <div class="target-card-actions">
              <el-tag v-if="target.agentOutdated" size="small" type="warning">{{ t('outdated') }}</el-tag>
              <el-button text type="primary">{{ t('viewDetails') }}</el-button>
            </div>
          </div>
        </article>

        <el-empty v-if="!loading && !filteredTargets.length" :description="t('emptyTargets')" />
      </div>
    </section>

    <el-dialog v-model="detailDialogVisible" :title="selectedTarget?.name || t('detailDialogTitle')" width="1120px" top="4vh" destroy-on-close>
      <template v-if="selectedTarget">
        <section class="detail-hero">
          <div class="detail-summary-card">
            <div class="detail-summary-top">
              <div>
                <p class="detail-eyebrow">{{ t('selectedTarget') }}</p>
                <h2>{{ selectedTarget.name }}</h2>
                <p>{{ selectedTarget.description || t('noDescription') }}</p>
              </div>
              <el-tag :type="statusTagType(selectedTarget)" size="large">{{ normalizedStatus(selectedTarget) }}</el-tag>
            </div>
            <div class="detail-pill-grid">
              <div class="pill-item">
                <span>{{ t('hostname') }}</span>
                <strong>{{ selectedTarget.hostname || t('unknownHost') }}</strong>
              </div>
              <div class="pill-item">
                <span>IP</span>
                <strong>{{ selectedTarget.ipAddress || t('unknownIp') }}</strong>
              </div>
              <div class="pill-item">
                <span>{{ t('agentVersion') }}</span>
                <strong>{{ selectedTarget.agentVersion || '-' }}</strong>
              </div>
              <div class="pill-item">
                <span>{{ t('lastHeartbeat') }}</span>
                <strong>{{ relativeHeartbeat(selectedTarget.lastHeartbeatAt) }}</strong>
              </div>
            </div>
          </div>

          <div class="detail-action-card">
            <div class="action-card-head">
              <div>
                <h3>{{ t('quickActions') }}</h3>
                <p>{{ quickActionSubtitle }}</p>
              </div>
              <el-switch
                v-if="canManageTargets"
                v-model="selectedTargetEnabled"
                @change="toggleEnabled(selectedTarget)" />
            </div>
            <div class="quick-action-grid">
              <el-button @click="copyText(selectedTarget.agentKey)">{{ t('copyKey') }}</el-button>
              <el-button @click="copyText(installCommand)">{{ t('copyCommand') }}</el-button>
              <el-button @click="copyText(envSnippet)">{{ t('copyEnv') }}</el-button>
              <el-button @click="copyText(restartCommand)">{{ t('copyRestart') }}</el-button>
              <el-button v-if="canManageTargets" @click="openEditDialog(selectedTarget)">{{ t('edit') }}</el-button>
              <el-button v-if="canManageTargets" @click="rotateKey(selectedTarget)">{{ t('rotateKey') }}</el-button>
            </div>
          </div>
        </section>

        <el-tabs v-model="detailTab" class="target-tabs">
          <el-tab-pane :label="t('tabOverview')" name="overview">
            <div class="detail-section-grid two-col">
              <div class="detail-card">
                <div class="detail-card-head">
                  <h3>{{ t('targetOverview') }}</h3>
                  <p>{{ t('targetOverviewDesc') }}</p>
                </div>
                <div class="overview-list">
                  <div><span>{{ t('targetName') }}</span><strong>{{ selectedTarget.name }}</strong></div>
                  <div><span>{{ t('hostname') }}</span><strong>{{ selectedTarget.hostname || '-' }}</strong></div>
                  <div><span>{{ t('description') }}</span><strong>{{ selectedTarget.description || '-' }}</strong></div>
                  <div><span>{{ t('agentKey') }}</span><strong class="mono">{{ selectedTarget.agentKeyPreview || '****' }}</strong></div>
                </div>
              </div>

              <div class="detail-card">
                <div class="detail-card-head">
                  <h3>{{ t('subscriptionTitle') }}</h3>
                  <p>{{ t('subscriptionDescManaged') }}</p>
                </div>
                <div class="subscription-state-box">
                  <div>
                    <strong>{{ t('subscriberCount') }}: {{ subscriptionForm.subscriberCount }}</strong>
                    <p>{{ subscriptionSummary }}</p>
                  </div>
                  <el-tag :type="subscriptionForm.enabled ? 'success' : 'info'">
                    {{ subscriptionForm.enabled ? t('subscriptionOn') : t('subscriptionOff') }}
                  </el-tag>
                </div>
                <div class="note-box target-note">
                  {{ canManageTargets ? t('subscriptionOperatorNote') : t('subscriptionReadonlyNote') }}
                </div>
              </div>
            </div>
          </el-tab-pane>

          <el-tab-pane :label="t('tabInstall')" name="install">
            <div class="detail-section-grid">
              <div class="detail-card">
                <div class="detail-card-head">
                  <h3>{{ t('installGuide') }}</h3>
                  <p>{{ t('installGuideDesc') }}</p>
                </div>
                <div class="install-steps">
                  <div class="step-box">
                    <span>1</span>
                    <p>{{ t('stepCreateManaged') }}</p>
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
                    <el-button size="small" plain @click="copyText(envSnippet)">{{ t('copyEnv') }}</el-button>
                  </div>
                  <pre>{{ envSnippet }}</pre>
                </div>
              </div>
            </div>
          </el-tab-pane>

          <el-tab-pane :label="t('tabThreshold')" name="threshold">
            <div class="detail-section-grid">
              <div class="detail-card">
                <div class="detail-card-head">
                  <h3>{{ t('thresholdTitle') }}</h3>
                  <p>{{ canManageTargets ? t('thresholdDescManage') : t('thresholdDescReadonly') }}</p>
                </div>
                <div class="threshold-head">
                  <el-switch v-model="thresholdForm.enabled" :disabled="!canManageTargets" :active-text="t('enableOverride')" />
                  <el-button size="small" :loading="thresholdLoading" @click="loadThresholds(selectedTarget.id)">
                    {{ t('refresh') }}
                  </el-button>
                </div>
                <el-form label-position="top" class="threshold-form">
                  <el-form-item label="CPU %">
                    <el-input-number v-model="thresholdForm.cpuThreshold" :min="1" :max="100" :disabled="!canManageTargets" />
                    <span>{{ t('effective') }} {{ thresholdForm.effectiveCpuThreshold ?? '-' }}</span>
                  </el-form-item>
                  <el-form-item :label="t('memoryThreshold')">
                    <el-input-number v-model="thresholdForm.memoryThreshold" :min="1" :max="100" :disabled="!canManageTargets" />
                    <span>{{ t('effective') }} {{ thresholdForm.effectiveMemoryThreshold ?? '-' }}</span>
                  </el-form-item>
                  <el-form-item :label="t('diskThreshold')">
                    <el-input-number v-model="thresholdForm.diskThreshold" :min="1" :max="100" :disabled="!canManageTargets" />
                    <span>{{ t('effective') }} {{ thresholdForm.effectiveDiskThreshold ?? '-' }}</span>
                  </el-form-item>
                  <el-form-item :label="t('processThreshold')">
                    <el-input-number v-model="thresholdForm.processCountThreshold" :min="1" :max="100000" :disabled="!canManageTargets" />
                    <span>{{ t('effective') }} {{ thresholdForm.effectiveProcessCountThreshold ?? '-' }}</span>
                  </el-form-item>
                  <el-form-item :label="t('consecutiveBreach')">
                    <el-input-number v-model="thresholdForm.consecutiveBreachCount" :min="1" :max="20" :disabled="!canManageTargets" />
                    <span>{{ t('effective') }} {{ thresholdForm.effectiveConsecutiveBreachCount ?? '-' }}</span>
                  </el-form-item>
                  <el-form-item :label="t('silenceSeconds')">
                    <el-input-number v-model="thresholdForm.silenceSeconds" :min="10" :max="86400" :disabled="!canManageTargets" />
                    <span>{{ t('effective') }} {{ thresholdForm.effectiveSilenceSeconds ?? '-' }}</span>
                  </el-form-item>
                </el-form>
                <el-button v-if="canManageTargets" type="primary" :loading="thresholdSaving" @click="saveThresholds(selectedTarget.id)">
                  {{ t('saveThreshold') }}
                </el-button>
              </div>
            </div>
          </el-tab-pane>

          <el-tab-pane :label="t('tabEvidence')" name="evidence">
            <div class="detail-section-grid two-col">
              <div class="detail-card">
                <div class="detail-card-head">
                  <h3>{{ t('evidenceTitle') }}</h3>
                  <p>{{ t('evidenceDesc') }}</p>
                </div>
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
              </div>
              <div class="detail-card">
                <div class="detail-card-head">
                  <h3>{{ t('opsSafeguardTitle') }}</h3>
                  <p>{{ t('opsSafeguardDesc') }}</p>
                </div>
                <div class="note-box target-note">
                  {{ t('opsNote') }}
                </div>
              </div>
            </div>
          </el-tab-pane>

          <el-tab-pane v-if="canManageTargets" :label="t('tabMembers')" name="members">
            <div class="detail-section-grid">
              <div class="detail-card">
                <div class="detail-card-head">
                  <h3>{{ t('memberTitle') }}</h3>
                  <p>{{ t('memberSubtitle') }}</p>
                </div>
                <div class="member-selector-row">
                  <el-select v-model="memberAssignUserId" filterable clearable :placeholder="t('memberSelectPlaceholder')" style="width: 100%;">
                    <el-option
                      v-for="user in assignableUsers"
                      :key="user.id"
                      :label="`${user.username} (${user.role})`"
                      :value="user.id" />
                  </el-select>
                  <el-button type="primary" :disabled="!memberAssignUserId" :loading="memberSaving" @click="assignMember(true)">
                    {{ t('addMember') }}
                  </el-button>
                </div>
                <div class="ops-table table-wrap">
                  <el-table :data="targetMembers" v-loading="memberLoading">
                    <el-table-column prop="username" :label="t('memberUsername')" min-width="160" />
                    <el-table-column prop="role" :label="t('memberRole')" width="120" />
                    <el-table-column :label="t('memberEmail')" min-width="220">
                      <template #default="{ row }">{{ row.notificationEmail || row.email || '-' }}</template>
                    </el-table-column>
                    <el-table-column :label="t('memberNotify')" width="140">
                      <template #default="{ row }">
                        <el-tag :type="row.notificationEnabled ? 'success' : 'info'" size="small">
                          {{ row.notificationEnabled ? t('enabled') : t('disabled') }}
                        </el-tag>
                      </template>
                    </el-table-column>
                    <el-table-column :label="t('actions')" width="120" fixed="right">
                      <template #default="{ row }">
                        <el-button size="small" type="danger" :loading="memberSaving" @click="removeMember(row)">
                          {{ t('removeMember') }}
                        </el-button>
                      </template>
                    </el-table-column>
                  </el-table>
                </div>
              </div>
            </div>
          </el-tab-pane>

          <el-tab-pane v-if="canManageTargets" :label="t('tabOps')" name="ops">
            <div class="detail-section-grid two-col">
              <div class="detail-card">
                <div class="detail-card-head">
                  <h3>{{ t('opsActionTitle') }}</h3>
                  <p>{{ t('opsActionDesc') }}</p>
                </div>
                <div class="ops-grid">
                  <button type="button" @click="openEditDialog(selectedTarget)">{{ t('edit') }}</button>
                  <button type="button" @click="rotateKey(selectedTarget)">{{ t('rotateKey') }}</button>
                  <button type="button" @click="copyText(serviceCommand)">{{ t('copyServiceInstall') }}</button>
                  <button type="button" @click="copyText(restartCommand)">{{ t('copyRestart') }}</button>
                </div>
              </div>
              <div class="detail-card danger-card">
                <div class="detail-card-head">
                  <h3>{{ t('dangerZone') }}</h3>
                  <p>{{ t('dangerZoneDesc') }}</p>
                </div>
                <el-popconfirm :title="t('deleteConfirmTarget')" @confirm="removeTarget(selectedTarget.id)">
                  <template #reference>
                    <el-button type="danger" plain>{{ t('delete') }}</el-button>
                  </template>
                </el-popconfirm>
              </div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </template>
    </el-dialog>

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
import { createTarget, deleteTarget, getTargetMembers, getTargetSubscription, getTargetThresholds, getTargets, rotateTargetKey, updateTarget, updateTargetMember, updateTargetSubscription, updateTargetThresholds } from '../api/targets'
import { getUsers } from '../api/admin'
import { useAuthStore } from '../stores/auth'
import { useI18n } from '../composables/useI18n'
import { AGENT_BASE_URL, API_BASE_URL } from '../config/env'

const auth = useAuthStore()
const loading = ref(false)
const saving = ref(false)
const targets = ref([])
const selectedTargetId = ref(null)
const detailDialogVisible = ref(false)
const keyword = ref('')
const statusFilter = ref('ALL')
const detailTab = ref('overview')
const installPlatform = ref('linux')
const apiBaseUrl = ref(API_BASE_URL)
const agentPackageBaseUrl = ref(AGENT_BASE_URL)
const dialogVisible = ref(false)
const isEdit = ref(false)
const editingId = ref(null)
const formRef = ref(null)
const thresholdLoading = ref(false)
const thresholdSaving = ref(false)
const subscriptionLoading = ref(false)
const subscriptionSaving = ref(false)
const memberLoading = ref(false)
const memberSaving = ref(false)
const memberAssignUserId = ref(null)
const targetMembers = ref([])
const availableUsers = ref([])

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
const subscriptionForm = reactive({
  enabled: false,
  subscriberCount: 0
})

const { locale, t } = useI18n({
  title: { zh: '监控目标', en: 'Monitoring Targets' },
  subtitleAdmin: { zh: '由管理员统一纳管与分配监控目标，面向企业场景管理接入、成员和阈值。', en: 'Operators centrally manage, assign, and govern monitored targets for enterprise operations.' },
  subtitleUser: { zh: '查看管理员分配给你的监控目标，获取安装信息、阈值和证据说明。', en: 'Review the targets assigned to you, along with install instructions, thresholds, and evidence.' },
  refresh: { zh: '刷新', en: 'Refresh' },
  create: { zh: '新增目标', en: 'New Target' },
  totalTargets: { zh: '目标总数', en: 'Total Targets' },
  onlineTargets: { zh: '在线目标', en: 'Online Targets' },
  offlineTargets: { zh: '离线目标', en: 'Offline Targets' },
  disabledTargets: { zh: '已禁用目标', en: 'Disabled Targets' },
  inventory: { zh: '目标清单', en: 'Target Inventory' },
  inventoryDescAdmin: { zh: '管理员在这里集中维护监控目标，并为不同账号分配可见范围。', en: 'Operators maintain all targets here and assign visibility to different users.' },
  inventoryDescUser: { zh: '这里只展示管理员分配给你的监控目标，你不能自行新增或变更。', en: 'Only targets assigned by administrators are shown here; self-service changes are disabled.' },
  searchPlaceholder: { zh: '搜索名称 / 主机 / IP', en: 'Search by name / host / IP' },
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
  emptyTargets: { zh: '暂无匹配目标', en: 'No matching targets' },
  viewDetails: { zh: '查看详情', en: 'View Details' },
  detailDialogTitle: { zh: '目标详情', en: 'Target Details' },
  selectedTarget: { zh: '当前目标', en: 'Selected Target' },
  noDescription: { zh: '暂无描述', en: 'No description' },
  quickActions: { zh: '快捷操作', en: 'Quick Actions' },
  quickActionsAdmin: { zh: '常用复制与运维操作集中在这里。', en: 'Common copy and operations are gathered here.' },
  quickActionsUser: { zh: '你可以查看安装信息和复制接入指令。', en: 'You can inspect installation details and copy onboarding commands.' },
  copyCommand: { zh: '复制安装命令', en: 'Copy Install Command' },
  copyEnv: { zh: '复制环境变量', en: 'Copy Env Config' },
  edit: { zh: '编辑目标', en: 'Edit Target' },
  copyKey: { zh: '复制 Key', en: 'Copy Key' },
  rotateKey: { zh: '轮换 Key', en: 'Rotate Key' },
  copyRestart: { zh: '复制重启命令', en: 'Copy Restart' },
  copyServiceInstall: { zh: '复制服务化安装', en: 'Copy Service Install' },
  tabOverview: { zh: '概览', en: 'Overview' },
  tabInstall: { zh: '安装接入', en: 'Install' },
  tabThreshold: { zh: '目标阈值', en: 'Thresholds' },
  tabEvidence: { zh: '证据源', en: 'Evidence' },
  tabMembers: { zh: '目标成员', en: 'Members' },
  tabOps: { zh: '运维操作', en: 'Operations' },
  targetOverview: { zh: '目标信息', en: 'Target Overview' },
  targetOverviewDesc: { zh: '展示该监控目标的基础识别信息与接入凭据摘要。', en: 'Shows core identification data and access credential summary.' },
  subscriptionTitle: { zh: '可见性与通知', en: 'Visibility and Notification' },
  subscriptionDescManaged: { zh: '目标是否能被哪个账号看到，由管理员在“目标成员”中统一维护。', en: 'Target visibility is centrally maintained by operators in the member tab.' },
  subscriptionReadonlyNote: { zh: '你当前只能查看分配结果。如果需要新增或调整目标归属，请联系管理员。', en: 'You can only review the assignment result. Contact an administrator to change ownership.' },
  subscriptionOperatorNote: { zh: '若要让某个账号看到这个目标并接收告警，请到“目标成员”里加入该账号。', en: 'To grant visibility and alert delivery, add the user in the member tab.' },
  subscriberCount: { zh: '成员数', en: 'Members' },
  subscriptionOn: { zh: '你已被分配', en: 'Assigned to you' },
  subscriptionOff: { zh: '你未被分配', en: 'Not assigned to you' },
  installGuide: { zh: '接入指引', en: 'Onboarding Guide' },
  installGuideDesc: { zh: '按目标系统复制命令到被监控主机执行，完成 Agent 安装与首次注册。', en: 'Copy the matching command to the monitored host to install the agent and register it.' },
  stepCreateManaged: { zh: '管理员创建并保存目标，系统生成唯一 agentKey。', en: 'An operator creates the target and the system generates a unique agent key.' },
  stepRun: { zh: '在被监控主机执行对应系统的安装命令。', en: 'Run the correct install command on the monitored host.' },
  stepVerify: { zh: '回到本页确认目标上线、版本和心跳时间。', en: 'Return here to verify online status, version, and heartbeat.' },
  agentPackageBaseUrl: { zh: '安装包地址', en: 'Agent Package URL' },
  apiBaseUrl: { zh: '后端地址', en: 'API URL' },
  installCommand: { zh: '安装/升级命令', en: 'Install / Upgrade Command' },
  envConfig: { zh: '.env 核心配置', en: '.env Core Config' },
  thresholdTitle: { zh: '阈值策略', en: 'Threshold Policy' },
  thresholdDescManage: { zh: '管理员可按目标覆盖默认阈值，用于更细粒度的告警控制。', en: 'Operators can override platform defaults for fine-grained alerting.' },
  thresholdDescReadonly: { zh: '这里只读展示该目标当前生效的告警阈值。', en: 'Read-only view of the effective alert thresholds for this target.' },
  enableOverride: { zh: '启用目标级覆盖', en: 'Enable target override' },
  memoryThreshold: { zh: '内存 %', en: 'Memory %' },
  diskThreshold: { zh: '磁盘 %', en: 'Disk %' },
  processThreshold: { zh: '进程数', en: 'Process Count' },
  consecutiveBreach: { zh: '连续次数', en: 'Consecutive Count' },
  silenceSeconds: { zh: '静默秒数', en: 'Silence Seconds' },
  effective: { zh: '生效值:', en: 'Effective:' },
  saveThreshold: { zh: '保存目标阈值', en: 'Save Target Thresholds' },
  evidenceTitle: { zh: '证据源说明', en: 'Evidence Sources' },
  evidenceDesc: { zh: 'AI 专家和事件工作台会从这些证据源取数，辅助诊断与审计。', en: 'AI expert workflows and incident operations rely on these evidence sources.' },
  metricsEvidence: { zh: '指标', en: 'Metrics' },
  metricsEvidenceDesc: { zh: 'CPU / 内存 / 磁盘 / 网络 / 进程数，每次心跳持续上报。', en: 'CPU, memory, disk, network, and process counts are reported on each heartbeat.' },
  logsEvidence: { zh: '日志', en: 'Logs' },
  logsEvidenceDesc: { zh: 'Agent 会周期性写入心跳与异常事件，供 AI 和人工调查引用。', en: 'The agent periodically records heartbeat and anomaly events for AI and human investigation.' },
  traceEvidence: { zh: 'Trace', en: 'Trace' },
  traceEvidenceDesc: { zh: '记录采集与发送链路耗时，用于定位采集侧性能问题。', en: 'Tracks collection and delivery latency to locate agent-side performance issues.' },
  opsSafeguardTitle: { zh: '运维提醒', en: 'Operations Reminder' },
  opsSafeguardDesc: { zh: '重要变更前请确认现场 Agent 能及时同步新配置。', en: 'Ensure field agents can promptly sync configuration before critical changes.' },
  opsNote: { zh: '轮换 Key 会让旧 Agent 立即失效；企业现场建议先更新 .env，再执行重启。', en: 'Rotating the key immediately invalidates the old agent; update .env before restart.' },
  memberTitle: { zh: '目标成员分配', en: 'Target Member Assignment' },
  memberSubtitle: { zh: '这里决定谁能看到该目标、谁能收到该目标告警。普通用户不能自行加入。', en: 'This defines who can see the target and receive its alerts. Users cannot self-assign.' },
  memberSelectPlaceholder: { zh: '选择要分配到该目标的账号', en: 'Select a user to assign to this target' },
  addMember: { zh: '加入成员', en: 'Add Member' },
  removeMember: { zh: '移除', en: 'Remove' },
  memberUsername: { zh: '用户名', en: 'Username' },
  memberRole: { zh: '角色', en: 'Role' },
  memberEmail: { zh: '接收邮箱', en: 'Recipient Email' },
  memberNotify: { zh: '通知开关', en: 'Notification' },
  opsActionTitle: { zh: '目标运维', en: 'Target Operations' },
  opsActionDesc: { zh: '对目标做变更前，请确认该主机当前是否在线以及是否存在未处理告警。', en: 'Before changing a target, verify host availability and outstanding alerts.' },
  dangerZone: { zh: '危险操作', en: 'Danger Zone' },
  dangerZoneDesc: { zh: '删除目标不会自动清理历史数据，请确认后执行。', en: 'Deleting a target does not automatically remove historical data.' },
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
  thresholdSaved: { zh: '目标阈值已保存', en: 'Target thresholds saved' },
  thresholdLoadFailed: { zh: '加载目标阈值失败', en: 'Failed to load target thresholds' },
  thresholdSaveFailed: { zh: '保存目标阈值失败', en: 'Failed to save target thresholds' },
  memberLoadFailed: { zh: '加载目标成员失败', en: 'Failed to load target members' },
  memberSaveFailed: { zh: '更新目标成员失败', en: 'Failed to update target members' },
  memberSaved: { zh: '目标成员已更新', en: 'Target members updated' },
  subscriptionSaved: { zh: '成员状态已更新', en: 'Assignment updated' },
  subscriptionLoadFailed: { zh: '加载目标成员状态失败', en: 'Failed to load assignment status' },
  subscriptionSaveFailed: { zh: '更新目标成员状态失败', en: 'Failed to update assignment status' },
  deleteConfirmTarget: { zh: '确认删除该目标？相关历史数据不会自动清理。', en: 'Delete this target? Historical data will not be auto-cleaned.' },
  actions: { zh: '操作', en: 'Actions' },
  enabled: { zh: '已启用', en: 'Enabled' },
  justNow: { zh: '刚刚', en: 'Just now' },
  minutesAgo: { zh: '{n} 分钟前', en: '{n} min ago' },
  hoursAgo: { zh: '{n} 小时前', en: '{n} hours ago' },
  daysAgo: { zh: '{n} 天前', en: '{n} days ago' }
})

const canManageTargets = computed(() => auth.isOperator || auth.isAdmin)
const pageSubtitle = computed(() => canManageTargets.value ? t('subtitleAdmin') : t('subtitleUser'))
const inventoryDescription = computed(() => canManageTargets.value ? t('inventoryDescAdmin') : t('inventoryDescUser'))
const quickActionSubtitle = computed(() => canManageTargets.value ? t('quickActionsAdmin') : t('quickActionsUser'))
const selectedTarget = computed(() => targets.value.find((item) => item.id === selectedTargetId.value) || null)
const selectedTargetEnabled = computed({
  get: () => Boolean(selectedTarget.value?.enabled),
  set: (value) => {
    if (selectedTarget.value) selectedTarget.value.enabled = value
  }
})
const subscriptionSummary = computed(() => subscriptionForm.enabled ? t('subscriptionOn') : t('subscriptionOff'))
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
const assignableUsers = computed(() => {
  const existingIds = new Set(targetMembers.value.map((item) => item.id))
  return availableUsers.value.filter((item) => !existingIds.has(item.id))
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
const windowsInstallCommand = computed(() => `$base = "${agentPackageBaseUrl.value}"\n$pkg = Join-Path $HOME "aiops-agent-lite.tar.gz"\nInvoke-WebRequest "$base/aiops-agent-lite.tar.gz" -OutFile $pkg\nif (Test-Path "$HOME\\aiops-agent-lite") { Remove-Item "$HOME\\aiops-agent-lite" -Recurse -Force }\ntar -xzf $pkg -C $HOME\nSet-Location "$HOME\\aiops-agent-lite"\n@'\n${envSnippet.value}\n'@ | Set-Content .env -Encoding UTF8\npowershell -ExecutionPolicy Bypass -File .\\stop.ps1\npowershell -ExecutionPolicy Bypass -File .\\start.ps1\npowershell -ExecutionPolicy Bypass -File .\\tail-log.ps1`)
const restartCommand = computed(() => installPlatform.value === 'windows'
  ? `Set-Location "$HOME\\aiops-agent-lite"\npowershell -ExecutionPolicy Bypass -File .\\stop.ps1\npowershell -ExecutionPolicy Bypass -File .\\start.ps1\npowershell -ExecutionPolicy Bypass -File .\\tail-log.ps1`
  : `cd ~/aiops-agent-lite\n./stop.sh\n./start.sh\n./tail-log.sh`)
const serviceCommand = computed(() => {
  if (installPlatform.value === 'windows') {
    return `Set-Location "$HOME\\aiops-agent-lite"\npowershell -ExecutionPolicy Bypass -File .\\install-service.ps1`
  }
  if (installPlatform.value === 'macos') {
    return `cd ~/aiops-agent-lite\n./install-launchd.sh\n./tail-log.sh`
  }
  return `cd ~/aiops-agent-lite\nsudo ./install-service.sh\njournalctl -u aiops-agent-lite -f`
})

watch(detailDialogVisible, (visible) => {
  if (!visible) {
    detailTab.value = 'overview'
    memberAssignUserId.value = null
  }
})

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

function openDetailDialog(target) {
  selectedTargetId.value = target.id
  detailDialogVisible.value = true
  detailTab.value = 'overview'
  loadTargetContext(target.id)
}

async function loadTargetContext(id) {
  await Promise.all([
    loadThresholds(id),
    loadSubscription(id),
    canManageTargets.value ? loadMembers(id) : Promise.resolve(),
    canManageTargets.value ? fetchAvailableUsers() : Promise.resolve()
  ])
}

async function fetchTargets() {
  loading.value = true
  try {
    const { data } = await getTargets()
    targets.value = Array.isArray(data) ? data : []
    if (selectedTargetId.value && !targets.value.some((item) => item.id === selectedTargetId.value)) {
      selectedTargetId.value = null
      detailDialogVisible.value = false
    }
  } catch (_e) {
    ElMessage.error(t('loadFailed'))
  } finally {
    loading.value = false
  }
}

async function fetchAvailableUsers() {
  if (!canManageTargets.value) return
  try {
    const { data } = await getUsers({ page: 0, size: 200 })
    availableUsers.value = data?.content || []
  } catch (_e) {
    availableUsers.value = []
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
    if (savedId) {
      const savedTarget = targets.value.find((item) => item.id === savedId)
      if (savedTarget) openDetailDialog(savedTarget)
    }
  } catch (_e) {
    ElMessage.error(isEdit.value ? t('updateFailed') : t('createFailed'))
  } finally {
    saving.value = false
  }
}

async function toggleEnabled(row) {
  if (!canManageTargets.value) return
  const value = row.enabled
  try {
    await updateTarget(row.id, { enabled: value })
    await fetchTargets()
    if (detailDialogVisible.value && selectedTarget.value?.id === row.id) {
      const refreshed = targets.value.find((item) => item.id === row.id)
      if (refreshed) selectedTargetId.value = refreshed.id
    }
    ElMessage.success(value ? t('enabledSuccess') : t('disabledSuccess'))
  } catch (_e) {
    row.enabled = !value
    ElMessage.error(t('statusUpdateFailed'))
  }
}

async function rotateKey(row) {
  if (!canManageTargets.value) return
  try {
    await ElMessageBox.confirm(t('rotateConfirm', { name: row.name }), t('rotateTitle'), {
      type: 'warning', confirmButtonText: t('rotateContinue'), cancelButtonText: t('rotateCancel')
    })
    const { data } = await rotateTargetKey(row.id)
    await fetchTargets()
    const refreshed = targets.value.find((item) => item.id === row.id)
    if (refreshed) selectedTargetId.value = refreshed.id
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

async function loadSubscription(id) {
  if (!id) return
  subscriptionLoading.value = true
  try {
    const { data } = await getTargetSubscription(id)
    subscriptionForm.enabled = Boolean(data?.enabled)
    subscriptionForm.subscriberCount = Number(data?.subscriberCount || 0)
  } catch (_e) {
    ElMessage.error(t('subscriptionLoadFailed'))
  } finally {
    subscriptionLoading.value = false
  }
}

async function loadMembers(id) {
  if (!canManageTargets.value || !id) return
  memberLoading.value = true
  try {
    const { data } = await getTargetMembers(id)
    targetMembers.value = Array.isArray(data) ? data : []
  } catch (_e) {
    ElMessage.error(t('memberLoadFailed'))
  } finally {
    memberLoading.value = false
  }
}

async function assignMember(enabled) {
  if (!selectedTarget.value?.id || !memberAssignUserId.value) return
  memberSaving.value = true
  try {
    const { data } = await updateTargetMember(selectedTarget.value.id, {
      userId: memberAssignUserId.value,
      enabled
    })
    subscriptionForm.subscriberCount = Number(data?.subscriberCount || subscriptionForm.subscriberCount)
    const target = targets.value.find((item) => item.id === selectedTarget.value.id)
    if (target) target.subscriberCount = subscriptionForm.subscriberCount
    await loadMembers(selectedTarget.value.id)
    memberAssignUserId.value = null
    ElMessage.success(t('memberSaved'))
  } catch (_e) {
    ElMessage.error(t('memberSaveFailed'))
  } finally {
    memberSaving.value = false
  }
}

async function removeMember(row) {
  if (!selectedTarget.value?.id || !row?.id) return
  memberSaving.value = true
  try {
    const { data } = await updateTargetMember(selectedTarget.value.id, {
      userId: row.id,
      enabled: false
    })
    subscriptionForm.subscriberCount = Number(data?.subscriberCount || subscriptionForm.subscriberCount)
    const target = targets.value.find((item) => item.id === selectedTarget.value.id)
    if (target) target.subscriberCount = subscriptionForm.subscriberCount
    await loadMembers(selectedTarget.value.id)
    ElMessage.success(t('memberSaved'))
  } catch (_e) {
    ElMessage.error(t('memberSaveFailed'))
  } finally {
    memberSaving.value = false
  }
}

async function saveThresholds(id) {
  if (!canManageTargets.value || !id) return
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
  if (!canManageTargets.value) return
  try {
    await deleteTarget(id)
    ElMessage.success(t('deleted'))
    detailDialogVisible.value = false
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

onMounted(fetchTargets)
</script>

<style scoped>
.targets-page {
  gap: 16px;
}

.targets-hero {
  background: radial-gradient(900px 220px at 0 0, rgba(14, 165, 233, 0.16), transparent 60%), var(--hero-bg);
}

.target-inventory-card {
  padding: 18px;
}

.inventory-head {
  gap: 16px;
  margin-bottom: 18px;
}

.target-filters {
  display: grid;
  grid-template-columns: minmax(220px, 320px) auto;
  gap: 12px;
  align-items: center;
}

.target-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 14px;
}

.target-card {
  border: 1px solid var(--line);
  border-radius: 18px;
  padding: 16px;
  background: linear-gradient(180deg, var(--panel-soft), rgba(15, 23, 42, 0.04));
  cursor: pointer;
  transition: transform 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease;
}

.target-card:hover {
  transform: translateY(-2px);
  border-color: var(--line-strong);
  box-shadow: 0 16px 36px rgba(15, 23, 42, 0.08);
}

.target-card.offline {
  opacity: 0.84;
}

.target-card.disabled {
  opacity: 0.66;
}

.target-card-top,
.target-name-row,
.target-card-footer,
.detail-summary-top,
.action-card-head,
.command-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.target-name-row {
  justify-content: flex-start;
  align-items: center;
}

.target-name-row h3,
.detail-summary-top h2,
.detail-card-head h3,
.action-card-head h3 {
  margin: 0;
  color: var(--text-1);
}

.target-card-top p,
.detail-summary-top p,
.detail-card-head p,
.action-card-head p,
.detail-eyebrow {
  margin: 4px 0 0;
  color: var(--text-3);
  font-size: 12px;
}

.status-dot {
  width: 9px;
  height: 9px;
  border-radius: 999px;
  background: var(--warning);
  box-shadow: 0 0 14px rgba(245, 158, 11, 0.65);
}

.status-dot.online {
  background: var(--success);
  box-shadow: 0 0 14px rgba(16, 185, 129, 0.72);
}

.status-dot.disabled {
  background: #64748b;
  box-shadow: none;
}

.target-stat-row {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin-top: 14px;
}

.target-stat-row div,
.pill-item,
.overview-list div,
.evidence-list div,
.subscription-state-box {
  border: 1px solid var(--line);
  border-radius: 14px;
  padding: 12px;
  background: rgba(2, 6, 23, 0.04);
}

.target-stat-row span,
.pill-item span,
.overview-list span,
.evidence-list span {
  display: block;
  color: var(--text-3);
  font-size: 11px;
}

.target-stat-row strong,
.pill-item strong,
.overview-list strong,
.evidence-list strong {
  display: block;
  margin-top: 6px;
  color: var(--text-1);
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.target-card-footer {
  margin-top: 14px;
  align-items: center;
}

.target-card-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  color: var(--brand) !important;
}

.detail-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.3fr) minmax(320px, 0.9fr);
  gap: 16px;
  margin-bottom: 18px;
}

.detail-summary-card,
.detail-action-card,
.detail-card {
  border: 1px solid var(--line);
  border-radius: 18px;
  padding: 18px;
  background: var(--panel-soft);
}

.detail-summary-top {
  align-items: flex-start;
}

.detail-eyebrow {
  text-transform: uppercase;
  letter-spacing: 0.12em;
}

.detail-pill-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 16px;
}

.quick-action-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-top: 16px;
}

.detail-section-grid {
  display: grid;
  gap: 16px;
}

.detail-section-grid.two-col {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.detail-card-head {
  margin-bottom: 14px;
}

.overview-list,
.evidence-list {
  display: grid;
  gap: 10px;
}

.subscription-state-box {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.subscription-state-box p {
  margin: 6px 0 0;
  color: var(--text-3);
  font-size: 12px;
}

.install-steps {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 14px;
}

.step-box {
  border: 1px solid var(--line);
  border-radius: 14px;
  padding: 12px;
  background: rgba(255, 255, 255, 0.02);
}

.step-box span {
  display: inline-grid;
  place-items: center;
  width: 24px;
  height: 24px;
  border-radius: 999px;
  background: rgba(14, 165, 233, 0.16);
  color: var(--brand);
  font-weight: 800;
}

.step-box p {
  margin: 8px 0 0;
  color: var(--text-2);
  font-size: 12px;
  line-height: 1.5;
}

.endpoint-grid {
  display: grid;
  gap: 10px;
  margin-bottom: 14px;
}

.platform-switch {
  margin-bottom: 14px;
}

.command-card {
  border: 1px solid var(--line);
  border-radius: 16px;
  padding: 14px;
  background: var(--code-pre-bg);
  margin-bottom: 12px;
}

.command-card.soft {
  background: var(--panel-soft);
}

.command-head {
  align-items: center;
  margin-bottom: 8px;
  color: var(--text-2);
  font-size: 12px;
  font-weight: 700;
}

.command-card pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
  color: var(--code-text);
  font-size: 12px;
  line-height: 1.55;
}

.threshold-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 12px;
}

.threshold-form :deep(.el-form-item) {
  margin-bottom: 12px;
}

.threshold-form :deep(.el-form-item__content) {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.threshold-form span {
  color: var(--text-3);
  font-size: 12px;
}

.member-selector-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 12px;
  margin-bottom: 14px;
}

.ops-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.ops-grid button {
  border: 1px solid var(--line);
  border-radius: 14px;
  padding: 14px;
  background: rgba(255, 255, 255, 0.02);
  color: var(--text-1);
  text-align: left;
  font-weight: 700;
  cursor: pointer;
}

.ops-grid button:hover {
  border-color: var(--line-strong);
  color: var(--brand);
}

.danger-card {
  border-color: rgba(239, 68, 68, 0.28);
}

.target-note {
  margin-top: 12px;
}

@media (max-width: 1100px) {
  .detail-hero,
  .detail-section-grid.two-col {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .target-filters,
  .target-stat-row,
  .detail-pill-grid,
  .quick-action-grid,
  .install-steps,
  .member-selector-row,
  .ops-grid {
    grid-template-columns: 1fr;
  }

  .target-grid {
    grid-template-columns: 1fr;
  }
}
</style>
