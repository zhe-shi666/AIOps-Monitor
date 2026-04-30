import api from './http'

export const getInvestigations = (params) => api.get('/api/investigations', { params })
export const createInvestigation = (payload) => api.post('/api/investigations', payload || {})
export const getInvestigationQualitySummary = () => api.get('/api/investigations/quality/summary')
export const getInvestigationDetail = (id) => api.get(`/api/investigations/${id}`)
export const getInvestigationTimeline = (id) => api.get(`/api/investigations/${id}/timeline`)
export const closeInvestigation = (id) => api.post(`/api/investigations/${id}/close`)
export const generateInvestigationAi = (id, payload) =>
  api.post(`/api/investigations/${id}/ai-generate`, payload || {}, { timeout: 120000 })
export const generateInvestigationPostmortem = (id, payload) =>
  api.post(`/api/investigations/${id}/postmortem-draft`, payload || {}, { timeout: 120000 })
export const createInvestigationObservation = (id, payload) => api.post(`/api/investigations/${id}/observations`, payload)
export const createInvestigationHypothesis = (id, payload) => api.post(`/api/investigations/${id}/hypotheses`, payload)
export const createInvestigationAction = (id, payload) => api.post(`/api/investigations/${id}/actions`, payload)
export const approveInvestigationAction = (id, actionId) => api.post(`/api/investigations/${id}/actions/${actionId}/approve`)
export const executeInvestigationAction = (id, actionId, payload) => api.post(`/api/investigations/${id}/actions/${actionId}/execute`, payload || {})
export const retryInvestigationAction = (id, actionId, payload) => api.post(`/api/investigations/${id}/actions/${actionId}/retry`, payload || {})
export const rollbackDrillAction = (id, actionId, payload) => api.post(`/api/investigations/${id}/actions/${actionId}/rollback-drill`, payload || {})
export const rollbackExecuteAction = (id, actionId, payload) => api.post(`/api/investigations/${id}/actions/${actionId}/rollback-execute`, payload || {})
export const getInvestigationActionAudits = (id, params) => api.get(`/api/investigations/${id}/actions/audits`, { params })
export const createInvestigationSnapshot = (id, payload) => api.post(`/api/investigations/${id}/snapshots`, payload)
