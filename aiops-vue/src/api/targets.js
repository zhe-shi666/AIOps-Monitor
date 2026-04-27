import api from './http'

export const getTargets = () => api.get('/api/targets')
export const getTargetDetail = (id) => api.get(`/api/targets/${id}`)
export const createTarget = (data) => api.post('/api/targets', data)
export const updateTarget = (id, data) => api.put(`/api/targets/${id}`, data)
export const rotateTargetKey = (id) => api.post(`/api/targets/${id}/rotate-key`)
export const deleteTarget = (id) => api.delete(`/api/targets/${id}`)
