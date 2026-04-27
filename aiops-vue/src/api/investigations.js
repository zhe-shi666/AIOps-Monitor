import api from './http'

export const getInvestigations = (params) => api.get('/api/investigations', { params })
export const getInvestigationDetail = (id) => api.get(`/api/investigations/${id}`)
export const getInvestigationTimeline = (id) => api.get(`/api/investigations/${id}/timeline`)
export const closeInvestigation = (id) => api.post(`/api/investigations/${id}/close`)
