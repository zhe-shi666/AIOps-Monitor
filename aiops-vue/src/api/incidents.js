import api from './http'

export const getIncidents = (params) => api.get('/api/incidents', { params })
export const updateIncidentStatus = (id, status) => api.put(`/api/incidents/${id}/status`, { status })
export const getIncidentDeliveries = (id, params) => api.get(`/api/incidents/${id}/deliveries`, { params })
export const getIncidentContext = (id, params) => api.get(`/api/incidents/${id}/context`, { params })
