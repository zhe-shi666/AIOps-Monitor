import api from './http'

export const getInvestigations = (params) => api.get('/api/investigations', { params })
export const getInvestigationDetail = (id) => api.get(`/api/investigations/${id}`)
export const getInvestigationTimeline = (id) => api.get(`/api/investigations/${id}/timeline`)
export const closeInvestigation = (id) => api.post(`/api/investigations/${id}/close`)
export const createInvestigationAction = (id, payload) => api.post(`/api/investigations/${id}/actions`, payload)
export const approveInvestigationAction = (id, actionId) => api.post(`/api/investigations/${id}/actions/${actionId}/approve`)
export const executeInvestigationAction = (id, actionId, payload) => api.post(`/api/investigations/${id}/actions/${actionId}/execute`, payload || {})
export const createInvestigationSnapshot = (id, payload) => api.post(`/api/investigations/${id}/snapshots`, payload)
