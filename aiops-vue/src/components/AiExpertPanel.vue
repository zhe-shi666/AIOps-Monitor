<template>
  <div class="ai-expert-panel">
    <div class="ai-expert-head">
      <div>
        <h2>{{ locale === 'zh' ? 'AI 专家中心' : 'AI Expert Console' }}</h2>
        <p>{{ locale === 'zh' ? '调查、诊断与运维协作中枢' : 'Investigation, diagnosis, and ops collaboration hub' }}</p>
      </div>
      <span class="ai-connection" :class="{ online: wsConnected }">
        {{ wsConnected ? (locale === 'zh' ? '在线' : 'Online') : (locale === 'zh' ? '重连中' : 'Reconnecting') }}
      </span>
    </div>

    <div class="ai-note">
      <strong>{{ locale === 'zh' ? '工作流：' : 'Workflow:' }}</strong>
      <span>
        {{
          locale === 'zh'
            ? '先选调查对象，再按总览 → 证据 → 假设 → 动作 → 时间线推进。'
            : 'Pick an investigation, then proceed with Overview → Evidence → Hypothesis → Actions → Timeline.'
        }}
      </span>
    </div>

    <div class="ai-toolbar">
      <div class="toolbar-tabs">
        <button class="tab-btn" :class="{ active: activeTab === 'investigations' }" @click="activeTab = 'investigations'">
          {{ locale === 'zh' ? '调查视图' : 'Investigations' }}
        </button>
        <button class="tab-btn" :class="{ active: activeTab === 'reports' }" @click="activeTab = 'reports'">
          {{ locale === 'zh' ? '报告流' : 'Reports' }}
        </button>
      </div>
      <div class="toolbar-actions">
        <el-button v-if="activeTab === 'investigations'" size="small" plain :loading="loadingInvestigations" @click="refreshInvestigations(false)">
          {{ locale === 'zh' ? '刷新' : 'Refresh' }}
        </el-button>
        <el-button v-else size="small" plain @click="clearReports">
          {{ locale === 'zh' ? '清空' : 'Clear' }}
        </el-button>
      </div>
    </div>

    <div v-if="activeTab === 'investigations'" class="investigation-workbench">
      <aside class="workbench-left">
        <section class="side-section">
          <div class="side-section-head">
            <p class="side-title">{{ locale === 'zh' ? '质量概览' : 'Quality Overview' }}</p>
          </div>
          <div v-if="qualitySummary" class="quality-grid">
            <div class="quality-item">
              <p class="quality-label">{{ locale === 'zh' ? '误报率' : 'False Positive' }}</p>
              <p class="quality-value">{{ formatPercent(qualitySummary.falsePositiveRate) }}</p>
            </div>
            <div class="quality-item">
              <p class="quality-label">{{ locale === 'zh' ? '执行成功率' : 'Run Success' }}</p>
              <p class="quality-value">{{ formatPercent(qualitySummary.actionRunSuccessRate) }}</p>
            </div>
            <div class="quality-item">
              <p class="quality-label">{{ locale === 'zh' ? '平均定位时长(分钟)' : 'Avg Resolve (min)' }}</p>
              <p class="quality-value">{{ qualitySummary.mttrMinutes ?? 0 }}</p>
            </div>
            <div class="quality-item">
              <p class="quality-label">{{ locale === 'zh' ? '进行中调查' : 'Open Investigations' }}</p>
              <p class="quality-value">{{ qualitySummary.openInvestigations ?? 0 }}</p>
            </div>
          </div>
        </section>

        <section class="side-section flex-grow">
          <div class="side-section-head">
            <p class="side-title">{{ locale === 'zh' ? '调查对象' : 'Investigations' }}</p>
            <span class="side-count">{{ investigations.length }}</span>
          </div>
          <div class="investigation-list">
            <button
              v-for="item in investigations"
              :key="item.id"
              class="investigation-item"
              :class="{ active: selectedInvestigationId === item.id }"
              @click="selectInvestigation(item.id)">
              <p class="inv-title">{{ item.title || `Investigation #${item.id}` }}</p>
              <div class="inv-meta">
                <span class="inv-chip severity">{{ item.severity || 'P2' }}</span>
                <span class="inv-chip">{{ item.status || '-' }}</span>
              </div>
            </button>

            <div v-if="!investigations.length && !loadingInvestigations" class="ai-empty inline">
              <p>{{ locale === 'zh' ? '暂无调查对象' : 'No investigations yet' }}</p>
            </div>
          </div>
        </section>
      </aside>

      <section class="workbench-main">
        <div v-if="selectedDetail?.investigation" class="detail-card">
          <div class="case-header">
            <div>
              <p class="detail-title">{{ selectedDetail.investigation.title || `Investigation #${selectedDetail.investigation.id}` }}</p>
              <p class="detail-sub">
                {{ locale === 'zh' ? '状态' : 'Status' }}:
                <span class="tone-highlight">{{ selectedDetail.investigation.status }}</span>
                ·
                {{ locale === 'zh' ? '等级' : 'Severity' }}:
                <span class="tone-highlight">{{ selectedDetail.investigation.severity }}</span>
              </p>
            </div>
            <div class="detail-head-actions">
              <el-button
                size="small"
                type="primary"
                plain
                :loading="aiGenerating"
                @click="runStructuredAnalysis">
                {{ locale === 'zh' ? 'AI 结构化分析' : 'AI Analyze' }}
              </el-button>
              <el-button
                v-if="selectedDetail.investigation.status !== 'CLOSED'"
                size="small"
                type="danger"
                plain
                :loading="closingInvestigation"
                @click="closeCurrentInvestigation">
                {{ locale === 'zh' ? '关闭调查' : 'Close' }}
              </el-button>
            </div>
          </div>

          <div class="overview-grid">
            <div class="overview-item">
              <p class="overview-label">{{ locale === 'zh' ? '证据' : 'Evidence' }}</p>
              <p class="overview-value">{{ observations.length }}</p>
            </div>
            <div class="overview-item">
              <p class="overview-label">{{ locale === 'zh' ? '假设' : 'Hypotheses' }}</p>
              <p class="overview-value">{{ hypotheses.length }}</p>
            </div>
            <div class="overview-item">
              <p class="overview-label">{{ locale === 'zh' ? '动作' : 'Actions' }}</p>
              <p class="overview-value">{{ actionPlans.length }}</p>
            </div>
            <div class="overview-item">
              <p class="overview-label">{{ locale === 'zh' ? '执行记录' : 'Runs' }}</p>
              <p class="overview-value">{{ actionRuns.length }}</p>
            </div>
          </div>

          <div class="process-tabs">
            <button
              v-for="tab in detailTabs"
              :key="tab.key"
              class="process-tab"
              :class="{ active: detailTab === tab.key }"
              @click="detailTab = tab.key">
              <span class="process-tab-content">
                <span>{{ tab.label }}</span>
                <span v-if="tab.count !== null" class="process-tab-count">{{ tab.count }}</span>
              </span>
            </button>
          </div>

          <div class="process-hint">{{ currentDetailTabHint }}</div>

          <div v-if="detailTab === 'overview'" class="process-body">
            <div class="section-title-row">
              <p class="section-title">{{ locale === 'zh' ? 'AI 报告快照' : 'AI Report Snapshot' }}</p>
            </div>
            <div v-if="selectedSnapshotHtml" class="report-snapshot prose" v-html="selectedSnapshotHtml"></div>
            <div v-else class="ai-empty inline">
              <p>{{ locale === 'zh' ? '暂无报告快照' : 'No report snapshot' }}</p>
            </div>

            <div class="snapshot-editor">
              <el-input
                v-model="snapshotDraft"
                type="textarea"
                :rows="4"
                resize="none"
                :placeholder="locale === 'zh' ? '可编辑当前报告，保存为新快照版本...' : 'Edit report and save as a new snapshot version...'" />
              <div class="snapshot-editor-row">
                <el-button
                  size="small"
                  plain
                  :loading="postmortemGenerating"
                  @click="generatePostmortemDraft">
                  {{ locale === 'zh' ? '生成复盘草稿' : 'Generate Postmortem' }}
                </el-button>
                <el-button
                  size="small"
                  type="primary"
                  plain
                  :disabled="!snapshotDraft.trim()"
                  :loading="snapshotSaving"
                  @click="saveSnapshot">
                  {{ locale === 'zh' ? '保存快照' : 'Save Snapshot' }}
                </el-button>
              </div>
            </div>
          </div>

          <div v-else-if="detailTab === 'evidence'" class="process-body">
            <div class="section-title-row">
              <p class="section-title">{{ locale === 'zh' ? '证据观测' : 'Observations' }}</p>
              <span class="section-count">{{ observations.length }}</span>
            </div>

            <div v-if="selectedDetail.investigation.status !== 'CLOSED'" class="observation-create">
              <div class="observation-create-row">
                <el-select v-model="observationDraft.type" size="small" class="observation-type-select">
                  <el-option label="METRIC" value="METRIC" />
                  <el-option label="LOG" value="LOG" />
                  <el-option label="TRACE" value="TRACE" />
                  <el-option label="CHANGE" value="CHANGE" />
                  <el-option label="EVENT" value="EVENT" />
                </el-select>
                <el-input
                  v-model="observationDraft.metricName"
                  size="small"
                  :placeholder="locale === 'zh' ? '指标名，如 cpu.usage' : 'Metric name, e.g. cpu.usage'" />
              </div>
              <div class="observation-create-row">
                <el-input-number
                  v-model="observationDraft.metricValue"
                  size="small"
                  :step="0.1"
                  class="observation-number" />
                <el-input
                  v-model="observationDraft.hostname"
                  size="small"
                  :placeholder="locale === 'zh' ? '主机名（可选）' : 'Hostname (optional)'" />
              </div>
              <el-input
                v-model="observationDraft.sourceRef"
                size="small"
                :placeholder="locale === 'zh' ? '来源引用（日志ID、链路ID、任务ID）' : 'Source ref (log ID, trace ID, job ID)'" />
              <div class="observation-create-row">
                <el-input-number
                  v-model="observationDraft.confidence"
                  size="small"
                  :min="0"
                  :max="1"
                  :step="0.05"
                  :precision="2"
                  class="observation-confidence" />
                <el-button size="small" type="primary" :loading="observationSubmitting" @click="createObservation">
                  {{ locale === 'zh' ? '新增证据' : 'Add Observation' }}
                </el-button>
              </div>
            </div>

            <div class="observation-list">
              <div v-for="item in observations" :key="item.id" class="observation-item">
                <div class="observation-item-head">
                  <p class="observation-title">{{ item.type }} · {{ item.metricName || item.sourceRef || 'N/A' }}</p>
                  <div class="action-tags">
                    <span class="inv-chip">V={{ item.metricValue ?? '-' }}</span>
                    <span class="inv-chip">C={{ typeof item.confidence === 'number' ? item.confidence.toFixed(2) : '-' }}</span>
                  </div>
                </div>
                <p class="observation-meta">
                  {{ formatDateTime(item.observedAt || item.createdAt) }} · {{ item.hostname || 'unknown-host' }}
                </p>
              </div>
              <div v-if="!observations.length" class="ai-empty inline">
                <p>{{ locale === 'zh' ? '暂无证据观测' : 'No observations yet' }}</p>
              </div>
            </div>
          </div>

          <div v-else-if="detailTab === 'hypothesis'" class="process-body">
            <div class="section-title-row">
              <p class="section-title">{{ locale === 'zh' ? '根因假设' : 'Hypotheses' }}</p>
              <span class="section-count">{{ hypotheses.length }}</span>
            </div>

            <div v-if="selectedDetail.investigation.status !== 'CLOSED'" class="hypothesis-create">
              <el-input
                v-model="hypothesisDraft.title"
                size="small"
                :placeholder="locale === 'zh' ? '假设标题，例如：JVM 堆外内存泄漏' : 'Hypothesis title, e.g. JVM off-heap memory leak'" />
              <el-input
                v-model="hypothesisDraft.reasoning"
                type="textarea"
                :rows="2"
                resize="none"
                :placeholder="locale === 'zh' ? '推理依据（指标、日志、变更线索）' : 'Reasoning evidence (metrics, logs, changes)'" />
              <div class="hypothesis-create-row">
                <el-select v-model="hypothesisDraft.status" size="small" class="hypothesis-select">
                  <el-option label="CANDIDATE" value="CANDIDATE" />
                  <el-option label="CONFIRMED" value="CONFIRMED" />
                  <el-option label="REJECTED" value="REJECTED" />
                </el-select>
                <el-input-number
                  v-model="hypothesisDraft.confidence"
                  size="small"
                  :min="0"
                  :max="1"
                  :step="0.05"
                  :precision="2"
                  class="hypothesis-confidence" />
                <el-button size="small" type="primary" :loading="hypothesisSubmitting" @click="createHypothesis">
                  {{ locale === 'zh' ? '新增假设' : 'Add Hypothesis' }}
                </el-button>
              </div>
            </div>

            <div class="hypothesis-list">
              <div v-for="item in hypotheses" :key="item.id" class="hypothesis-item">
                <div class="hypothesis-item-head">
                  <p class="hypothesis-title">#{{ item.rankOrder }} · {{ item.title }}</p>
                  <div class="action-tags">
                    <span class="inv-chip">{{ item.status || 'CANDIDATE' }}</span>
                    <span class="inv-chip">C={{ typeof item.confidence === 'number' ? item.confidence.toFixed(2) : '-' }}</span>
                  </div>
                </div>
                <p v-if="item.reasoning" class="hypothesis-reasoning">{{ item.reasoning }}</p>
              </div>
              <div v-if="!hypotheses.length" class="ai-empty inline">
                <p>{{ locale === 'zh' ? '暂无根因假设' : 'No hypotheses yet' }}</p>
              </div>
            </div>
          </div>

          <div v-else-if="detailTab === 'action'" class="process-body">
            <div class="section-title-row">
              <p class="section-title">{{ locale === 'zh' ? '动作计划' : 'Action Plans' }}</p>
              <span class="section-count">{{ actionPlans.length }}</span>
            </div>

            <div v-if="selectedDetail.investigation.status !== 'CLOSED'" class="action-create">
              <el-input
                v-model="actionDraft.title"
                size="small"
                :placeholder="locale === 'zh' ? '动作标题，例如：回收高内存进程' : 'Action title, e.g. recycle high-memory process'" />
              <el-input
                v-model="actionDraft.commandText"
                type="textarea"
                :rows="2"
                resize="none"
                :placeholder="locale === 'zh' ? '执行命令或 runbook 描述' : 'Command or runbook notes'" />
              <el-input
                v-model="actionDraft.rollbackPlan"
                type="textarea"
                :rows="2"
                resize="none"
                :placeholder="locale === 'zh' ? '回滚方案（HIGH 风险必填）' : 'Rollback plan (required for HIGH risk)'" />
              <div class="action-create-row">
                <el-select v-model="actionDraft.riskLevel" size="small" class="action-select">
                  <el-option label="LOW" value="LOW" />
                  <el-option label="MEDIUM" value="MEDIUM" />
                  <el-option label="HIGH" value="HIGH" />
                </el-select>
                <el-switch
                  v-model="actionDraft.requiresApproval"
                  :disabled="actionDraft.riskLevel !== 'LOW'"
                  :active-text="locale === 'zh' ? '需审批' : 'Approval'"
                  :inactive-text="locale === 'zh' ? '免审批' : 'No approval'" />
                <el-button size="small" type="primary" :loading="actionSubmitting" @click="createActionPlan">
                  {{ locale === 'zh' ? '新增动作' : 'Add Action' }}
                </el-button>
              </div>
            </div>

            <div class="action-list">
              <div v-for="action in actionPlans" :key="action.id" class="action-item">
                <div class="action-item-head">
                  <p class="action-title">{{ action.title }}</p>
                  <div class="action-tags">
                    <span class="inv-chip">{{ action.actionType }}</span>
                    <span class="inv-chip" :class="`risk-${(action.riskLevel || '').toLowerCase()}`">{{ action.riskLevel || 'MEDIUM' }}</span>
                    <span class="inv-chip">{{ action.status }}</span>
                  </div>
                </div>
                <p v-if="action.commandText" class="action-command">{{ action.commandText }}</p>
                <p v-if="action.rollbackPlan" class="action-command rollback-plan">
                  {{ locale === 'zh' ? '回滚：' : 'Rollback: ' }}{{ action.rollbackPlan }}
                </p>
                <div class="action-buttons">
                  <el-button
                    size="small"
                    plain
                    :disabled="action.status === 'APPROVED' || action.status === 'EXECUTED' || action.status === 'FAILED' || !action.requiresApproval"
                    :loading="actionOperatingId === action.id && actionOperatingType === 'approve'"
                    @click="approveAction(action)">
                    {{ locale === 'zh' ? '审批' : 'Approve' }}
                  </el-button>
                  <el-button
                    size="small"
                    type="success"
                    plain
                    :disabled="action.status === 'EXECUTED' || (action.requiresApproval && action.status !== 'APPROVED' && action.status !== 'FAILED')"
                    :loading="actionOperatingId === action.id && actionOperatingType === 'execute'"
                    @click="executeAction(action)">
                    {{ locale === 'zh' ? '执行' : 'Execute' }}
                  </el-button>
                  <el-button
                    size="small"
                    type="warning"
                    plain
                    :disabled="action.status !== 'FAILED'"
                    :loading="actionOperatingId === action.id && actionOperatingType === 'retry'"
                    @click="retryAction(action)">
                    {{ locale === 'zh' ? '重试' : 'Retry' }}
                  </el-button>
                  <el-button
                    size="small"
                    type="info"
                    plain
                    :disabled="!action.rollbackPlan"
                    :loading="actionOperatingId === action.id && actionOperatingType === 'rollbackDrill'"
                    @click="rollbackDrill(action)">
                    {{ locale === 'zh' ? '回滚演练' : 'Rollback Drill' }}
                  </el-button>
                  <el-button
                    size="small"
                    type="danger"
                    plain
                    :disabled="action.status !== 'EXECUTED' || !action.rollbackPlan"
                    :loading="actionOperatingId === action.id && actionOperatingType === 'rollbackExecute'"
                    @click="rollbackExecute(action)">
                    {{ locale === 'zh' ? '执行回滚' : 'Rollback' }}
                  </el-button>
                </div>
              </div>
              <div v-if="!actionPlans.length" class="ai-empty inline">
                <p>{{ locale === 'zh' ? '暂无动作计划' : 'No action plans' }}</p>
              </div>
            </div>
          </div>

          <div v-else-if="detailTab === 'execution'" class="process-body">
            <div class="section-title-row">
              <p class="section-title">{{ locale === 'zh' ? '执行记录' : 'Execution Runs' }}</p>
              <span class="section-count">{{ actionRuns.length }}</span>
            </div>

            <div class="execution-list">
              <div v-for="run in actionRuns" :key="run.id" class="execution-item">
                <div class="execution-item-head">
                  <p class="execution-title">#{{ run.id }} · {{ run.status || '-' }}</p>
                  <div class="action-tags">
                    <span class="inv-chip">{{ run.executionMode || 'MANUAL' }}</span>
                    <span class="inv-chip">{{ run.executor || '-' }}</span>
                  </div>
                </div>
                <p class="execution-time">
                  {{ formatDateTime(run.startedAt || run.createdAt) }} → {{ formatDateTime(run.endedAt || run.createdAt) }}
                </p>
                <p v-if="run.outputText" class="execution-output">{{ run.outputText }}</p>
                <p v-if="run.errorMessage" class="execution-error">{{ run.errorMessage }}</p>
              </div>
              <div v-if="!actionRuns.length" class="ai-empty inline">
                <p>{{ locale === 'zh' ? '暂无执行记录' : 'No execution runs' }}</p>
              </div>
            </div>
          </div>

          <div v-else-if="detailTab === 'governance'" class="process-body">
            <div class="section-title-row">
              <p class="section-title">{{ locale === 'zh' ? '治理审计' : 'Governance Audit' }}</p>
              <span class="section-count">{{ actionAudits.length }}</span>
            </div>

            <div class="execution-list">
              <div v-for="audit in actionAudits" :key="audit.id" class="execution-item">
                <div class="execution-item-head">
                  <p class="execution-title">#{{ audit.id }} · {{ audit.eventType || '-' }}</p>
                  <div class="action-tags">
                    <span class="inv-chip">{{ audit.riskLevel || '-' }}</span>
                    <span class="inv-chip">{{ audit.decision || '-' }}</span>
                  </div>
                </div>
                <p class="execution-time">{{ formatDateTime(audit.createdAt) }} · {{ audit.actor || '-' }}</p>
                <p v-if="audit.detailJson" class="execution-output">{{ audit.detailJson }}</p>
              </div>
              <div v-if="!actionAudits.length" class="ai-empty inline">
                <p>{{ locale === 'zh' ? '暂无治理审计记录' : 'No governance audit records' }}</p>
              </div>
            </div>

            <div class="section-title-row top-gap">
              <p class="section-title">{{ locale === 'zh' ? '回滚记录' : 'Rollback Runs' }}</p>
              <span class="section-count">{{ rollbackRuns.length }}</span>
            </div>
            <div class="execution-list">
              <div v-for="run in rollbackRuns" :key="run.id" class="execution-item">
                <div class="execution-item-head">
                  <p class="execution-title">#{{ run.id }} · {{ run.drillMode ? (locale === 'zh' ? '演练' : 'Drill') : (locale === 'zh' ? '执行' : 'Execute') }} · {{ run.status || '-' }}</p>
                  <div class="action-tags">
                    <span class="inv-chip">{{ run.executionMode || 'MANUAL' }}</span>
                    <span class="inv-chip">{{ run.executor || '-' }}</span>
                  </div>
                </div>
                <p class="execution-time">{{ formatDateTime(run.startedAt || run.createdAt) }} → {{ formatDateTime(run.endedAt || run.createdAt) }}</p>
                <p v-if="run.noteText" class="execution-output">{{ run.noteText }}</p>
                <p v-if="run.outputText" class="execution-output">{{ run.outputText }}</p>
                <p v-if="run.errorMessage" class="execution-error">{{ run.errorMessage }}</p>
              </div>
              <div v-if="!rollbackRuns.length" class="ai-empty inline">
                <p>{{ locale === 'zh' ? '暂无回滚记录' : 'No rollback runs' }}</p>
              </div>
            </div>
          </div>

          <div v-else-if="detailTab === 'timeline'" class="process-body">
            <div class="timeline-head">
              <span>{{ locale === 'zh' ? '调查时间线' : 'Timeline' }}</span>
              <div class="timeline-filter">
                <el-select v-model="timelineCategory" size="small" class="timeline-select">
                  <el-option value="ALL" :label="locale === 'zh' ? '全部' : 'All'" />
                  <el-option value="INVESTIGATION" label="INVESTIGATION" />
                  <el-option value="OBSERVATION" label="OBSERVATION" />
                  <el-option value="HYPOTHESIS" label="HYPOTHESIS" />
                  <el-option value="ACTION_PLAN" label="ACTION_PLAN" />
                  <el-option value="ACTION_RUN" label="ACTION_RUN" />
                  <el-option value="ACTION_AUDIT" label="ACTION_AUDIT" />
                  <el-option value="ROLLBACK_RUN" label="ROLLBACK_RUN" />
                  <el-option value="REPORT_SNAPSHOT" label="REPORT_SNAPSHOT" />
                </el-select>
                <span>{{ filteredTimelineEvents.length }}</span>
              </div>
            </div>

            <div class="timeline-list">
              <div v-for="event in filteredTimelineEvents" :key="`${event.category}-${event.refId}-${event.time}`" class="timeline-item">
                <p class="timeline-time">{{ formatDateTime(event.time) }}</p>
                <p class="timeline-title">{{ event.title }}</p>
                <p class="timeline-detail">{{ event.detail }}</p>
                <p v-if="formatTimelineMeta(event)" class="timeline-meta">{{ formatTimelineMeta(event) }}</p>
              </div>
              <div v-if="!filteredTimelineEvents.length && !loadingDetail" class="ai-empty inline">
                <p>{{ locale === 'zh' ? '暂无时间线事件' : 'No timeline events' }}</p>
              </div>
            </div>
          </div>

          <div v-else class="process-body">
            <div class="ai-empty inline">
              <p>{{ locale === 'zh' ? '请选择流程标签' : 'Select a workflow tab' }}</p>
            </div>
          </div>
        </div>
        <div v-else class="ai-empty">
          <p>{{ loadingDetail ? (locale === 'zh' ? '加载调查详情...' : 'Loading investigation...') : (locale === 'zh' ? '请选择调查对象' : 'Select an investigation') }}</p>
        </div>
      </section>
    </div>

    <div v-else class="ai-report-list">
      <transition-group name="slide-fade" tag="div">
        <div
          v-for="report in reports"
          :key="report.id"
          class="ai-report-card"
          :class="{ risk: report.isRisk }">
          <div class="ai-report-meta">
            <span>{{ report.time }}</span>
            <span v-if="report.isRisk" class="risk-tag">RISK</span>
          </div>
          <div class="ai-report-content prose" v-html="report.html"></div>
        </div>
      </transition-group>
      <div v-if="reports.length === 0" class="ai-empty">
        <p>{{ locale === 'zh' ? '等待 AI 诊断结果...' : 'Waiting for AI diagnostics...' }}</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { marked } from 'marked'
import SockJS from 'sockjs-client'
import Stomp from 'stompjs'
import { ElMessage } from 'element-plus'
import { WS_BASE_URL } from '../config/env'
import { useAuthStore } from '../stores/auth'
import { useLocaleMode } from '../composables/useLocaleMode'
import {
  approveInvestigationAction,
  closeInvestigation,
  createInvestigationAction,
  createInvestigationHypothesis,
  createInvestigationObservation,
  createInvestigationSnapshot,
  executeInvestigationAction,
  generateInvestigationAi,
  generateInvestigationPostmortem,
  getInvestigationActionAudits,
  getInvestigationDetail,
  getInvestigations,
  getInvestigationQualitySummary,
  getInvestigationTimeline,
  rollbackDrillAction,
  rollbackExecuteAction,
  retryInvestigationAction
} from '../api/investigations'

const auth = useAuthStore()
const { locale } = useLocaleMode()

const activeTab = ref('investigations')
const detailTab = ref('overview')
const reports = ref([])
const wsConnected = ref(false)

const investigations = ref([])
const selectedInvestigationId = ref(null)
const selectedDetail = ref(null)
const timelineEvents = ref([])
const loadingInvestigations = ref(false)
const loadingDetail = ref(false)
const closingInvestigation = ref(false)
const aiGenerating = ref(false)
const postmortemGenerating = ref(false)
const observationSubmitting = ref(false)
const hypothesisSubmitting = ref(false)
const actionSubmitting = ref(false)
const actionOperatingId = ref(null)
const actionOperatingType = ref('')
const snapshotSaving = ref(false)
const snapshotDraft = ref('')
const timelineCategory = ref('ALL')
const qualitySummary = ref(null)
const actionAudits = ref([])

let stompClient = null
let reconnectTimer = null
let reportCounter = 0
let investigationRefreshTimer = null

const observationDraft = reactive({
  type: 'METRIC',
  metricName: '',
  metricValue: null,
  hostname: '',
  sourceRef: '',
  confidence: 0.8
})

const hypothesisDraft = reactive({
  title: '',
  reasoning: '',
  confidence: 0.7,
  status: 'CANDIDATE'
})

const actionDraft = reactive({
  title: '',
  commandText: '',
  riskLevel: 'MEDIUM',
  requiresApproval: true,
  rollbackPlan: ''
})

const selectedSnapshotHtml = computed(() => {
  const markdown = selectedDetail.value?.latestSnapshot?.reportMarkdown
  return markdown ? marked.parse(markdown) : ''
})
const currentDetailTabHint = computed(() => {
  const zhMap = {
    overview: '总览：先确认 AI 结论与当前状态，再决定是否进入复盘或人工编辑。',
    evidence: '证据：补充可验证的监控/日志/事件，构建后续推理基础。',
    hypothesis: '假设：基于证据归纳根因候选，并逐步确认或排除。',
    action: '动作：形成可执行的处置计划，审批后执行并记录结果。',
    execution: '执行：查看执行结果、输出与错误，判断动作是否生效。',
    governance: '治理：审计审批与执行决策，并完成回滚演练/回滚执行留痕。',
    timeline: '时间线：回看调查全过程，用于复盘与审计。'
  }
  const enMap = {
    overview: 'Overview: validate AI conclusions and current state before deeper operations.',
    evidence: 'Evidence: add verifiable metrics/logs/events as the basis for reasoning.',
    hypothesis: 'Hypothesis: derive and validate root-cause candidates from evidence.',
    action: 'Actions: create executable plans, approve, execute, and track outcomes.',
    execution: 'Execution: inspect outputs and errors to verify action effectiveness.',
    governance: 'Governance: audit decisions and complete rollback drills/executions.',
    timeline: 'Timeline: review the full investigation path for postmortem and audit.'
  }
  const map = locale.value === 'zh' ? zhMap : enMap
  return map[detailTab.value] || map.overview
})

const observations = computed(() => selectedDetail.value?.observations || [])
const hypotheses = computed(() => selectedDetail.value?.hypotheses || [])
const actionPlans = computed(() => selectedDetail.value?.actionPlans || [])
const actionRuns = computed(() => selectedDetail.value?.actionRuns || [])
const rollbackRuns = computed(() => selectedDetail.value?.rollbackRuns || [])
const detailTabs = computed(() => {
  const zh = locale.value === 'zh'
  return [
    { key: 'overview', label: zh ? '总览' : 'Overview', count: null },
    { key: 'evidence', label: zh ? '证据' : 'Evidence', count: observations.value.length },
    { key: 'hypothesis', label: zh ? '假设' : 'Hypothesis', count: hypotheses.value.length },
    { key: 'action', label: zh ? '动作' : 'Actions', count: actionPlans.value.length },
    { key: 'execution', label: zh ? '执行' : 'Execution', count: actionRuns.value.length },
    { key: 'governance', label: zh ? '治理' : 'Governance', count: actionAudits.value.length },
    { key: 'timeline', label: zh ? '时间线' : 'Timeline', count: timelineEvents.value.length }
  ]
})
const filteredTimelineEvents = computed(() => {
  if (timelineCategory.value === 'ALL') return timelineEvents.value
  return timelineEvents.value.filter((x) => x?.category === timelineCategory.value)
})

function clearReports() {
  reports.value = []
}

function formatTime() {
  return new Date().toLocaleTimeString(locale.value === 'zh' ? 'zh-CN' : 'en-US', { hour12: false })
}

function formatDateTime(value) {
  if (!value) return '-'
  const dt = new Date(value)
  if (Number.isNaN(dt.getTime())) return '-'
  return dt.toLocaleString(locale.value === 'zh' ? 'zh-CN' : 'en-US', { hour12: false })
}

function formatTimelineMeta(event) {
  if (!event?.metadata || typeof event.metadata !== 'object') return ''
  const entries = Object.entries(event.metadata)
    .filter(([, v]) => v !== null && v !== undefined && v !== '' && v !== '-')
    .slice(0, 3)
    .map(([k, v]) => `${k}: ${v}`)
  return entries.join(' | ')
}

function formatPercent(value) {
  const n = Number(value)
  if (Number.isNaN(n)) return '0.00%'
  return `${n.toFixed(2)}%`
}

function scheduleReconnect() {
  clearTimeout(reconnectTimer)
  reconnectTimer = setTimeout(connectReportsStream, 5000)
}

function connectReportsStream() {
  if (stompClient?.connected) return

  const socket = new SockJS(`${WS_BASE_URL}/ws-monitor`)
  const client = Stomp.over(socket)
  client.debug = null

  const headers = auth.token ? { Authorization: `Bearer ${auth.token}` } : {}
  client.connect(headers, () => {
    wsConnected.value = true
    stompClient = client

    client.subscribe('/topic/ai-reports', (msg) => {
      const raw = msg.body || ''
      reports.value.unshift({
        id: `expert-${++reportCounter}-${Date.now()}`,
        time: formatTime(),
        html: marked.parse(raw),
        isRisk: /有风险|风险|告警|critical|warning|alert/i.test(raw)
      })
      if (reports.value.length > 80) {
        reports.value = reports.value.slice(0, 80)
      }
    })

    client.subscribe('/topic/investigations', async (msg) => {
      let event = null
      try {
        event = JSON.parse(msg.body || '{}')
      } catch (_e) {
        event = null
      }
      const eventMessage = event?.message || (locale.value === 'zh' ? '调查事件已更新' : 'Investigation event updated')
      reports.value.unshift({
        id: `inv-${++reportCounter}-${Date.now()}`,
        time: formatTime(),
        html: marked.parse(`**${event?.eventType || 'INV_EVENT'}**  \n${eventMessage}`),
        isRisk: /P1|CRITICAL|FAILED|风险|告警|critical|warning|alert/i.test(String(msg.body || ''))
      })
      if (reports.value.length > 80) {
        reports.value = reports.value.slice(0, 80)
      }

      const currentId = selectedInvestigationId.value
      const incomingId = Number(event?.investigationId)
      if (currentId && incomingId && currentId === incomingId) {
        await loadInvestigationDetail(currentId, true)
        await loadInvestigationTimeline(currentId, true)
        await loadActionAudits(currentId, true)
      }
      await loadQualitySummary(true)
    })
  }, () => {
    wsConnected.value = false
    stompClient = null
    scheduleReconnect()
  })
}

async function refreshInvestigations(silent = true) {
  if (!silent) loadingInvestigations.value = true
  try {
    const { data } = await getInvestigations({ page: 0, size: 20 })
    investigations.value = data?.content || []

    if (!investigations.value.length) {
      detailTab.value = 'overview'
      selectedInvestigationId.value = null
      selectedDetail.value = null
      timelineEvents.value = []
      actionAudits.value = []
      return
    }

    const exists = investigations.value.some((x) => x.id === selectedInvestigationId.value)
    if (!exists) {
      await selectInvestigation(investigations.value[0].id)
      return
    }

    if (selectedInvestigationId.value) {
      await loadInvestigationDetail(selectedInvestigationId.value, true)
      await loadInvestigationTimeline(selectedInvestigationId.value, true)
      await loadActionAudits(selectedInvestigationId.value, true)
    }
  } catch (_e) {
    if (!silent) {
      ElMessage.error(locale.value === 'zh' ? '加载调查列表失败' : 'Failed to load investigations')
    }
  } finally {
    loadingInvestigations.value = false
  }
}

async function selectInvestigation(id) {
  selectedInvestigationId.value = id
  detailTab.value = 'overview'
  await loadInvestigationDetail(id, false)
  await loadInvestigationTimeline(id, false)
  await loadActionAudits(id, false)
}

async function loadInvestigationDetail(id, silent = false) {
  if (!silent) loadingDetail.value = true
  try {
    const { data } = await getInvestigationDetail(id)
    if (selectedInvestigationId.value === id) {
      selectedDetail.value = data
      actionAudits.value = Array.isArray(data?.recentActionAudits) ? data.recentActionAudits : actionAudits.value
      if (!snapshotDraft.value || !silent) {
        snapshotDraft.value = data?.latestSnapshot?.reportMarkdown || ''
      }
    }
  } catch (_e) {
    if (!silent) {
      ElMessage.error(locale.value === 'zh' ? '加载调查详情失败' : 'Failed to load investigation detail')
    }
  } finally {
    if (!silent) loadingDetail.value = false
  }
}

async function loadInvestigationTimeline(id, silent = false) {
  if (!silent) loadingDetail.value = true
  try {
    const { data } = await getInvestigationTimeline(id)
    if (selectedInvestigationId.value === id) {
      timelineEvents.value = Array.isArray(data) ? data : []
    }
  } catch (_e) {
    if (!silent) {
      ElMessage.error(locale.value === 'zh' ? '加载时间线失败' : 'Failed to load timeline')
    }
  } finally {
    if (!silent) loadingDetail.value = false
  }
}

async function loadActionAudits(id, silent = false) {
  try {
    const { data } = await getInvestigationActionAudits(id, { page: 0, size: 50 })
    if (selectedInvestigationId.value === id) {
      actionAudits.value = Array.isArray(data?.content) ? data.content : []
    }
  } catch (_e) {
    if (!silent) {
      ElMessage.error(locale.value === 'zh' ? '加载治理审计失败' : 'Failed to load governance audits')
    }
  }
}

async function loadQualitySummary(silent = true) {
  try {
    const { data } = await getInvestigationQualitySummary()
    qualitySummary.value = data || null
  } catch (_e) {
    if (!silent) {
      ElMessage.error(locale.value === 'zh' ? '加载质量指标失败' : 'Failed to load quality metrics')
    }
  }
}

async function closeCurrentInvestigation() {
  if (!selectedDetail.value?.investigation?.id) return
  const id = selectedDetail.value.investigation.id
  closingInvestigation.value = true
  try {
    await closeInvestigation(id)
    ElMessage.success(locale.value === 'zh' ? '调查已关闭' : 'Investigation closed')
    await refreshInvestigations(false)
  } catch (_e) {
    ElMessage.error(locale.value === 'zh' ? '关闭调查失败' : 'Failed to close investigation')
  } finally {
    closingInvestigation.value = false
  }
}

async function runStructuredAnalysis() {
  const investigationId = selectedDetail.value?.investigation?.id
  if (!investigationId) return
  aiGenerating.value = true
  try {
    await generateInvestigationAi(investigationId, { includePostmortem: false })
    ElMessage.success(locale.value === 'zh' ? 'AI 结构化分析已完成' : 'AI structured analysis completed')
    await loadInvestigationDetail(investigationId, true)
    await loadInvestigationTimeline(investigationId, true)
    await loadActionAudits(investigationId, true)
    await loadQualitySummary(true)
  } catch (_e) {
    ElMessage.error(locale.value === 'zh' ? 'AI 分析失败' : 'AI analysis failed')
  } finally {
    aiGenerating.value = false
  }
}

async function generatePostmortemDraft() {
  const investigationId = selectedDetail.value?.investigation?.id
  if (!investigationId) return
  postmortemGenerating.value = true
  try {
    const { data } = await generateInvestigationPostmortem(investigationId, {})
    if (data?.markdown) {
      snapshotDraft.value = data.markdown
    }
    ElMessage.success(locale.value === 'zh' ? '复盘草稿已生成' : 'Postmortem draft generated')
    await loadInvestigationDetail(investigationId, true)
    await loadInvestigationTimeline(investigationId, true)
    await loadActionAudits(investigationId, true)
  } catch (_e) {
    ElMessage.error(locale.value === 'zh' ? '生成复盘草稿失败' : 'Failed to generate postmortem')
  } finally {
    postmortemGenerating.value = false
  }
}

async function createObservation() {
  const investigationId = selectedDetail.value?.investigation?.id
  if (!investigationId) return
  if (!observationDraft.metricName.trim() && !observationDraft.sourceRef.trim()) {
    ElMessage.warning(locale.value === 'zh' ? '请至少填写指标名或来源引用' : 'Please provide metric name or source ref')
    return
  }
  observationSubmitting.value = true
  try {
    await createInvestigationObservation(investigationId, {
      type: observationDraft.type,
      metricName: observationDraft.metricName.trim() || undefined,
      metricValue: typeof observationDraft.metricValue === 'number' ? observationDraft.metricValue : undefined,
      hostname: observationDraft.hostname.trim() || undefined,
      sourceRef: observationDraft.sourceRef.trim() || undefined,
      confidence: typeof observationDraft.confidence === 'number' ? observationDraft.confidence : undefined
    })
    observationDraft.type = 'METRIC'
    observationDraft.metricName = ''
    observationDraft.metricValue = null
    observationDraft.hostname = ''
    observationDraft.sourceRef = ''
    observationDraft.confidence = 0.8
    ElMessage.success(locale.value === 'zh' ? '证据观测已写入' : 'Observation added')
    await loadInvestigationDetail(investigationId, true)
    await loadInvestigationTimeline(investigationId, true)
    await loadActionAudits(investigationId, true)
  } catch (_e) {
    ElMessage.error(locale.value === 'zh' ? '新增证据失败' : 'Failed to add observation')
  } finally {
    observationSubmitting.value = false
  }
}

async function createHypothesis() {
  const investigationId = selectedDetail.value?.investigation?.id
  if (!investigationId) return
  if (!hypothesisDraft.title.trim()) {
    ElMessage.warning(locale.value === 'zh' ? '请填写假设标题' : 'Please input hypothesis title')
    return
  }
  hypothesisSubmitting.value = true
  try {
    await createInvestigationHypothesis(investigationId, {
      title: hypothesisDraft.title.trim(),
      reasoning: hypothesisDraft.reasoning.trim() || undefined,
      confidence: typeof hypothesisDraft.confidence === 'number' ? hypothesisDraft.confidence : undefined,
      status: hypothesisDraft.status
    })
    hypothesisDraft.title = ''
    hypothesisDraft.reasoning = ''
    hypothesisDraft.confidence = 0.7
    hypothesisDraft.status = 'CANDIDATE'
    ElMessage.success(locale.value === 'zh' ? '根因假设已创建' : 'Hypothesis created')
    await loadInvestigationDetail(investigationId, true)
    await loadInvestigationTimeline(investigationId, true)
    await loadActionAudits(investigationId, true)
  } catch (_e) {
    ElMessage.error(locale.value === 'zh' ? '创建假设失败' : 'Failed to create hypothesis')
  } finally {
    hypothesisSubmitting.value = false
  }
}

async function createActionPlan() {
  const investigationId = selectedDetail.value?.investigation?.id
  if (!investigationId) return
  if (!actionDraft.title.trim()) {
    ElMessage.warning(locale.value === 'zh' ? '请填写动作标题' : 'Please input action title')
    return
  }
  if (actionDraft.riskLevel === 'HIGH' && !actionDraft.rollbackPlan.trim()) {
    ElMessage.warning(locale.value === 'zh' ? 'HIGH 风险动作必须填写回滚方案' : 'Rollback plan is required for HIGH risk actions')
    return
  }
  if (actionDraft.riskLevel !== 'LOW') {
    actionDraft.requiresApproval = true
  }
  actionSubmitting.value = true
  try {
    await createInvestigationAction(investigationId, {
      actionType: 'RUNBOOK',
      title: actionDraft.title.trim(),
      commandText: actionDraft.commandText.trim() || undefined,
      riskLevel: actionDraft.riskLevel,
      requiresApproval: actionDraft.requiresApproval,
      rollbackPlan: actionDraft.rollbackPlan.trim() || undefined
    })
    actionDraft.title = ''
    actionDraft.commandText = ''
    actionDraft.riskLevel = 'MEDIUM'
    actionDraft.requiresApproval = true
    actionDraft.rollbackPlan = ''
    ElMessage.success(locale.value === 'zh' ? '动作计划已创建' : 'Action created')
    await loadInvestigationDetail(investigationId, true)
    await loadInvestigationTimeline(investigationId, true)
    await loadActionAudits(investigationId, true)
  } catch (_e) {
    ElMessage.error(locale.value === 'zh' ? '创建动作失败' : 'Failed to create action')
  } finally {
    actionSubmitting.value = false
  }
}

async function rollbackDrill(action) {
  const investigationId = selectedDetail.value?.investigation?.id
  if (!investigationId || !action?.id) return
  actionOperatingId.value = action.id
  actionOperatingType.value = 'rollbackDrill'
  try {
    await rollbackDrillAction(investigationId, action.id, {
      status: 'SUCCESS',
      executionMode: 'MANUAL',
      note: 'Rollback drill triggered via AI Expert Panel.',
      outputText: 'Rollback drill passed.'
    })
    ElMessage.success(locale.value === 'zh' ? '回滚演练记录已写入' : 'Rollback drill recorded')
    await loadInvestigationDetail(investigationId, true)
    await loadInvestigationTimeline(investigationId, true)
    await loadActionAudits(investigationId, true)
  } catch (_e) {
    ElMessage.error(locale.value === 'zh' ? '回滚演练失败' : 'Rollback drill failed')
  } finally {
    actionOperatingId.value = null
    actionOperatingType.value = ''
  }
}

async function rollbackExecute(action) {
  const investigationId = selectedDetail.value?.investigation?.id
  if (!investigationId || !action?.id) return
  actionOperatingId.value = action.id
  actionOperatingType.value = 'rollbackExecute'
  try {
    await rollbackExecuteAction(investigationId, action.id, {
      status: 'SUCCESS',
      executionMode: 'MANUAL',
      note: 'Rollback executed via AI Expert Panel.',
      outputText: 'Rollback execution completed.'
    })
    ElMessage.success(locale.value === 'zh' ? '回滚执行完成' : 'Rollback executed')
    await loadInvestigationDetail(investigationId, true)
    await loadInvestigationTimeline(investigationId, true)
    await loadActionAudits(investigationId, true)
  } catch (_e) {
    ElMessage.error(locale.value === 'zh' ? '回滚执行失败' : 'Rollback execution failed')
  } finally {
    actionOperatingId.value = null
    actionOperatingType.value = ''
  }
}

async function approveAction(action) {
  const investigationId = selectedDetail.value?.investigation?.id
  if (!investigationId || !action?.id) return
  actionOperatingId.value = action.id
  actionOperatingType.value = 'approve'
  try {
    await approveInvestigationAction(investigationId, action.id)
    ElMessage.success(locale.value === 'zh' ? '审批完成' : 'Approved')
    await loadInvestigationDetail(investigationId, true)
    await loadInvestigationTimeline(investigationId, true)
    await loadActionAudits(investigationId, true)
  } catch (_e) {
    ElMessage.error(locale.value === 'zh' ? '审批失败' : 'Approve failed')
  } finally {
    actionOperatingId.value = null
    actionOperatingType.value = ''
  }
}

async function executeAction(action) {
  const investigationId = selectedDetail.value?.investigation?.id
  if (!investigationId || !action?.id) return
  actionOperatingId.value = action.id
  actionOperatingType.value = 'execute'
  try {
    await executeInvestigationAction(investigationId, action.id, {
      status: 'SUCCESS',
      executionMode: 'MANUAL',
      outputText: 'Executed via AI Expert Panel.'
    })
    ElMessage.success(locale.value === 'zh' ? '执行记录已写入' : 'Execution recorded')
    await loadInvestigationDetail(investigationId, true)
    await loadInvestigationTimeline(investigationId, true)
    await loadActionAudits(investigationId, true)
  } catch (_e) {
    ElMessage.error(locale.value === 'zh' ? '执行失败' : 'Execute failed')
  } finally {
    actionOperatingId.value = null
    actionOperatingType.value = ''
  }
}

async function retryAction(action) {
  const investigationId = selectedDetail.value?.investigation?.id
  if (!investigationId || !action?.id) return
  actionOperatingId.value = action.id
  actionOperatingType.value = 'retry'
  try {
    await retryInvestigationAction(investigationId, action.id, {
      status: 'SUCCESS',
      executionMode: 'MANUAL',
      outputText: 'Retried via AI Expert Panel.'
    })
    ElMessage.success(locale.value === 'zh' ? '重试记录已写入' : 'Retry recorded')
    await loadInvestigationDetail(investigationId, true)
    await loadInvestigationTimeline(investigationId, true)
    await loadActionAudits(investigationId, true)
    await loadQualitySummary(true)
  } catch (_e) {
    ElMessage.error(locale.value === 'zh' ? '重试失败' : 'Retry failed')
  } finally {
    actionOperatingId.value = null
    actionOperatingType.value = ''
  }
}

async function saveSnapshot() {
  const investigationId = selectedDetail.value?.investigation?.id
  const markdown = snapshotDraft.value.trim()
  if (!investigationId || !markdown) return
  snapshotSaving.value = true
  try {
    await createInvestigationSnapshot(investigationId, {
      format: 'MARKDOWN',
      reportMarkdown: markdown,
      createdBy: 'USER'
    })
    ElMessage.success(locale.value === 'zh' ? '快照已保存' : 'Snapshot saved')
    await loadInvestigationDetail(investigationId, true)
    await loadInvestigationTimeline(investigationId, true)
    await loadActionAudits(investigationId, true)
  } catch (_e) {
    ElMessage.error(locale.value === 'zh' ? '保存快照失败' : 'Failed to save snapshot')
  } finally {
    snapshotSaving.value = false
  }
}

onMounted(async () => {
  connectReportsStream()
  await refreshInvestigations(false)
  await loadQualitySummary(false)
  investigationRefreshTimer = setInterval(() => {
    refreshInvestigations(true)
    loadQualitySummary(true)
  }, 15000)
})

onUnmounted(() => {
  wsConnected.value = false
  clearTimeout(reconnectTimer)
  clearInterval(investigationRefreshTimer)
  if (stompClient?.connected) {
    try {
      stompClient.disconnect()
    } catch (_e) {
      // ignore
    }
  }
  stompClient = null
})
</script>

<style scoped>
.ai-expert-panel {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 18px 16px 18px;
  border-left: 1px solid var(--line);
  background: linear-gradient(180deg, var(--panel-strong), var(--panel));
  min-height: 0;
  min-width: 0;
  container-type: inline-size;
}

.ai-expert-head {
  display: grid;
  grid-template-columns: 1fr auto;
  justify-content: space-between;
  gap: 12px;
}

.ai-expert-head h2 {
  margin: 0;
  font-size: 17px;
  line-height: 1.2;
}

.ai-expert-head p {
  margin: 4px 0 0;
  font-size: 12px;
  color: var(--text-3);
}

.ai-connection {
  font-size: 11px;
  line-height: 1;
  padding: 7px 10px;
  border-radius: 999px;
  border: 1px solid rgba(251, 113, 133, 0.4);
  color: #fb7185;
  background: rgba(251, 113, 133, 0.1);
}

.ai-connection.online {
  border-color: rgba(52, 211, 153, 0.45);
  color: #34d399;
  background: rgba(52, 211, 153, 0.1);
}

.ai-note {
  display: flex;
  align-items: center;
  gap: 8px;
  border-radius: 12px;
  border: 1px solid var(--line);
  background: var(--panel-soft);
  padding: 10px 12px 9px;
  color: var(--text-3);
  font-size: 12px;
  line-height: 1.5;
}

.ai-note strong {
  color: var(--text-1);
}

.ai-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.toolbar-tabs {
  display: flex;
  align-items: center;
  gap: 8px;
}

.tab-btn {
  border: 1px solid var(--line);
  background: transparent;
  color: var(--text-3);
  border-radius: 999px;
  padding: 6px 12px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.tab-btn.active {
  border-color: rgba(34, 211, 238, 0.4);
  background: rgba(34, 211, 238, 0.12);
  color: #67e8f9;
}

.investigation-workbench {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: minmax(200px, 24%) minmax(0, 1fr);
  gap: 12px;
  overflow: hidden;
}

.workbench-left,
.workbench-main {
  min-height: 0;
  min-width: 0;
}

.workbench-main {
  overflow: hidden;
}

.workbench-left {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.side-section {
  border: 1px solid var(--line);
  border-radius: 12px;
  background: var(--panel);
  padding: 10px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-height: 0;
}

.side-section.flex-grow {
  flex: 1;
}

.side-section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.side-title {
  margin: 0;
  font-size: 12px;
  color: var(--text-2);
  font-weight: 600;
}

.side-count {
  font-size: 11px;
  color: var(--text-3);
  border: 1px solid var(--line);
  border-radius: 999px;
  padding: 1px 8px;
}

.quality-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.quality-item {
  border: 1px solid var(--line);
  border-radius: 10px;
  background: var(--panel-soft);
  padding: 9px 10px;
}

.quality-label {
  margin: 0;
  font-size: 10px;
  color: var(--text-3);
}

.quality-value {
  margin: 4px 0 0;
  font-size: 14px;
  color: var(--text-1);
  font-weight: 700;
}

.investigation-list {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
  overflow: auto;
  padding-right: 4px;
}

.investigation-item {
  text-align: left;
  border: 1px solid var(--line);
  border-radius: 12px;
  background: var(--panel);
  padding: 8px 10px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.investigation-item.active {
  border-color: rgba(59, 130, 246, 0.5);
  background: rgba(30, 58, 138, 0.28);
}

.inv-title {
  margin: 0;
  font-size: 12px;
  color: var(--text-1);
  line-height: 1.4;
}

.inv-meta {
  margin-top: 6px;
  display: flex;
  gap: 6px;
}

.inv-chip {
  border: 1px solid var(--line);
  border-radius: 999px;
  padding: 1px 8px;
  font-size: 11px;
  color: var(--text-3);
}

.inv-chip.severity {
  border-color: rgba(251, 191, 36, 0.35);
  color: #fbbf24;
}

.detail-card {
  height: 100%;
  border: 1px solid var(--line);
  border-radius: 14px;
  background: var(--panel);
  padding: 12px 12px 10px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 0;
}

.case-header {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  flex-wrap: wrap;
}

.detail-head-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.detail-title {
  margin: 0;
  font-size: 14px;
  color: var(--text-1);
  font-weight: 700;
}

.detail-sub {
  margin: 4px 0 0;
  font-size: 11px;
  color: var(--text-3);
}

.tone-highlight {
  color: #67e8f9;
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
}

.overview-item {
  border: 1px solid var(--line);
  border-radius: 10px;
  background: var(--panel-soft);
  padding: 8px 10px;
}

.overview-label {
  margin: 0;
  font-size: 10px;
  color: var(--text-3);
}

.overview-value {
  margin: 4px 0 0;
  font-size: 13px;
  color: var(--text-1);
  font-weight: 700;
}

.process-tabs {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.process-tab {
  border: 1px solid var(--line);
  background: transparent;
  color: var(--text-3);
  border-radius: 999px;
  padding: 5px 11px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.process-tab-content {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.process-tab-count {
  font-size: 10px;
  color: var(--text-3);
  border: 1px solid var(--line);
  border-radius: 999px;
  padding: 0 6px;
}

.process-tab.active {
  border-color: rgba(59, 130, 246, 0.45);
  background: rgba(59, 130, 246, 0.14);
  color: #93c5fd;
}

.process-tab.active .process-tab-count {
  color: #bfdbfe;
  border-color: rgba(147, 197, 253, 0.45);
}

.process-hint {
  border: 1px solid var(--line);
  border-radius: 10px;
  background: var(--panel-soft);
  padding: 8px 10px;
  font-size: 12px;
  color: var(--text-2);
}

.process-body {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.section-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.section-title {
  margin: 0;
  font-size: 12px;
  color: var(--text-2);
  font-weight: 600;
}

.section-count {
  font-size: 11px;
  color: var(--text-3);
  border: 1px solid var(--line);
  border-radius: 999px;
  padding: 1px 8px;
}

.report-snapshot {
  flex: 1;
  min-height: 0;
  border: 1px solid var(--line);
  border-radius: 10px;
  background: var(--panel-soft);
  padding: 10px;
  overflow: auto;
}

.report-snapshot :deep(*) {
  color: inherit;
}

.snapshot-editor {
  border: 1px solid var(--line);
  border-radius: 10px;
  background: var(--panel-soft);
  padding: 9px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.snapshot-editor-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
}

.observation-create {
  border: 1px solid var(--line);
  border-radius: 10px;
  background: var(--panel-soft);
  padding: 9px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.observation-create-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.observation-type-select {
  width: 140px;
}

.observation-number,
.observation-confidence {
  width: 120px;
}

.observation-list {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
  overflow: auto;
  padding-right: 2px;
}

.observation-item {
  border: 1px solid var(--line);
  border-radius: 10px;
  background: var(--panel-soft);
  padding: 8px 10px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.observation-item-head {
  display: flex;
  justify-content: space-between;
  gap: 8px;
}

.observation-title {
  margin: 0;
  font-size: 12px;
  color: var(--text-1);
}

.observation-meta {
  margin: 0;
  font-size: 10px;
  color: var(--text-3);
}

.hypothesis-create {
  border: 1px solid var(--line);
  border-radius: 10px;
  background: var(--panel-soft);
  padding: 9px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.hypothesis-create-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.hypothesis-select {
  width: 140px;
}

.hypothesis-confidence {
  flex: 1;
  min-width: 110px;
}

.hypothesis-list {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
  overflow: auto;
  padding-right: 2px;
}

.hypothesis-item {
  border: 1px solid var(--line);
  border-radius: 10px;
  background: var(--panel-soft);
  padding: 8px 10px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.hypothesis-item-head {
  display: flex;
  justify-content: space-between;
  gap: 8px;
}

.hypothesis-title {
  margin: 0;
  font-size: 12px;
  color: var(--text-1);
}

.hypothesis-reasoning {
  margin: 0;
  font-size: 11px;
  color: var(--text-2);
  white-space: pre-wrap;
}

.action-create {
  border: 1px solid var(--line);
  border-radius: 10px;
  background: var(--panel-soft);
  padding: 9px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.action-create-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.action-select {
  width: 120px;
}

.action-list {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
  overflow: auto;
  padding-right: 2px;
}

.action-item {
  border: 1px solid var(--line);
  border-radius: 10px;
  background: var(--panel-soft);
  padding: 8px 10px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.action-item-head {
  display: flex;
  justify-content: space-between;
  gap: 8px;
}

.action-title {
  margin: 0;
  font-size: 12px;
  color: var(--text-1);
}

.action-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.inv-chip.risk-low {
  border-color: rgba(74, 222, 128, 0.4);
  color: #4ade80;
}

.inv-chip.risk-medium {
  border-color: rgba(251, 191, 36, 0.4);
  color: #fbbf24;
}

.inv-chip.risk-high {
  border-color: rgba(248, 113, 113, 0.45);
  color: #f87171;
}

.action-command {
  margin: 0;
  font-size: 11px;
  color: var(--text-2);
  white-space: pre-wrap;
}

.action-command.rollback-plan {
  color: var(--text-3);
}

.action-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.execution-list {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
  overflow: auto;
  padding-right: 2px;
}

.top-gap {
  margin-top: 10px;
}

.execution-item {
  border: 1px solid var(--line);
  border-radius: 10px;
  background: var(--panel-soft);
  padding: 8px 10px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.execution-item-head {
  display: flex;
  justify-content: space-between;
  gap: 8px;
}

.execution-title {
  margin: 0;
  font-size: 12px;
  color: var(--text-1);
}

.execution-time {
  margin: 0;
  font-size: 10px;
  color: var(--text-3);
}

.execution-output {
  margin: 0;
  font-size: 11px;
  color: var(--text-2);
  white-space: pre-wrap;
}

.execution-error {
  margin: 0;
  font-size: 11px;
  color: #fb7185;
  white-space: pre-wrap;
}

.timeline-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: var(--text-3);
}

.timeline-filter {
  display: flex;
  align-items: center;
  gap: 8px;
}

.timeline-select {
  width: 150px;
}

.timeline-list {
  flex: 1;
  min-height: 0;
  overflow: auto;
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding-right: 2px;
}

.timeline-item {
  border: 1px solid var(--line);
  border-radius: 10px;
  background: var(--panel-soft);
  padding: 8px 10px;
}

.timeline-time {
  margin: 0;
  font-size: 10px;
  color: var(--text-3);
}

.timeline-title {
  margin: 3px 0 0;
  font-size: 12px;
  color: var(--text-1);
}

.timeline-detail {
  margin: 3px 0 0;
  font-size: 11px;
  color: var(--text-2);
}

.timeline-meta {
  margin: 4px 0 0;
  font-size: 10px;
  color: var(--text-3);
}

.ai-report-list {
  flex: 1;
  min-height: 0;
  overflow: auto;
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding-right: 4px;
}

.ai-report-card {
  border-radius: 14px;
  border: 1px solid var(--line);
  background: var(--panel);
  padding: 12px;
  min-width: 0;
}

.ai-report-card.risk {
  border-color: rgba(251, 113, 133, 0.36);
  background: rgba(127, 29, 29, 0.2);
}

.ai-report-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 11px;
  color: var(--text-3);
}

.risk-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 2px 8px;
  border-radius: 999px;
  border: 1px solid rgba(251, 113, 133, 0.4);
  color: #fb7185;
}

.ai-report-content :deep(*) {
  color: inherit;
}

/* Avoid long markdown/log lines pushing cards outside viewport */
.ai-report-content {
  overflow-wrap: anywhere;
  word-break: break-word;
}

.ai-report-content :deep(pre),
.ai-report-content :deep(code) {
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  word-break: break-word;
}

.ai-empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-3);
  font-size: 12px;
}

.ai-empty.inline {
  min-height: 72px;
}

.slide-fade-enter-active {
  transition: all 0.25s ease-out;
}

.slide-fade-leave-active {
  transition: all 0.2s ease-in;
}

.slide-fade-enter-from,
.slide-fade-leave-to {
  transform: translateY(-6px);
  opacity: 0;
}

@container (max-width: 1200px) {
  .investigation-workbench {
    grid-template-columns: minmax(190px, 30%) minmax(0, 1fr);
  }
}

@container (max-width: 980px) {
  .investigation-workbench {
    grid-template-columns: 1fr;
  }

  .workbench-left {
    max-height: 260px;
  }

  .overview-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@container (max-width: 500px) {
  .process-tabs {
    overflow-x: auto;
    flex-wrap: nowrap;
    padding-bottom: 2px;
    scrollbar-width: none;
  }

  .process-tabs::-webkit-scrollbar {
    display: none;
  }

  .process-tab {
    white-space: nowrap;
    flex-shrink: 0;
  }

  .case-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .detail-head-actions {
    width: 100%;
    justify-content: flex-end;
  }

  .observation-create-row {
    flex-wrap: wrap;
  }

  .observation-type-select {
    width: 100%;
  }

  .observation-number,
  .observation-confidence {
    flex: 1;
    min-width: 0;
    width: auto;
  }

  .hypothesis-create-row {
    flex-wrap: wrap;
  }

  .hypothesis-select {
    width: 100%;
  }

  .hypothesis-confidence {
    flex: 1;
    min-width: 0;
    width: auto;
  }

  .action-create-row {
    flex-wrap: wrap;
  }

  .action-select {
    width: 100%;
  }

  .action-buttons {
    gap: 6px;
  }

  .timeline-select {
    width: 120px;
  }
}

@media (max-width: 1280px) {
  .ai-expert-panel {
    border-left: none;
    border-top: 1px solid var(--line);
    min-height: 500px;
  }
}
</style>
