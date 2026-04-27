import api from './http'

export const getIncidents = (params) => api.get('/api/incidents', { params })
export const updateIncidentStatus = (id, status) => api.put(`/api/incidents/${id}/status`, { status })
