import api from './http'

export const getAuditLogs = (params) => api.get('/api/audit-logs', { params })
