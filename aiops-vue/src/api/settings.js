import api from './http'

export const getThresholdSettings = () => api.get('/api/settings/thresholds')
export const updateThresholdSettings = (data) => api.put('/api/settings/thresholds', data)
export const getEscalationPolicy = () => api.get('/api/settings/escalation-policy')
export const updateEscalationPolicy = (data) => api.put('/api/settings/escalation-policy', data)
