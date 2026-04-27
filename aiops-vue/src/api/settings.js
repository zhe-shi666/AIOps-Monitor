import api from './http'

export const getThresholdSettings = () => api.get('/api/settings/thresholds')
export const updateThresholdSettings = (data) => api.put('/api/settings/thresholds', data)
