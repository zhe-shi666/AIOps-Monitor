import api from './http'

export const getTargets = () => api.get('/api/targets')
export const getTargetDetail = (id) => api.get(`/api/targets/${id}`)
export const getTargetThresholds = (id) => api.get(`/api/targets/${id}/thresholds`)
export const getTargetSubscription = (id) => api.get(`/api/targets/${id}/subscription`)
export const getTargetMembers = (id) => api.get(`/api/targets/${id}/members`)
export const createTarget = (data) => api.post('/api/targets', data)
export const updateTarget = (id, data) => api.put(`/api/targets/${id}`, data)
export const updateTargetThresholds = (id, data) => api.put(`/api/targets/${id}/thresholds`, data)
export const updateTargetSubscription = (id, data) => api.put(`/api/targets/${id}/subscription`, data)
export const updateTargetMember = (id, data) => api.put(`/api/targets/${id}/members`, data)
export const rotateTargetKey = (id) => api.post(`/api/targets/${id}/rotate-key`)
export const deleteTarget = (id) => api.delete(`/api/targets/${id}`)
