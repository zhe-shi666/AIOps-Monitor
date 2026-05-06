import api from './http'

export const getUsers = (params) => api.get('/api/admin/users', { params })
export const getUserDetail = (id) => api.get(`/api/admin/users/${id}`)
export const updateUserRole = (id, role) => api.put(`/api/admin/users/${id}/role`, { role })
export const updateUserStatus = (id, enabled) => api.put(`/api/admin/users/${id}/status`, { enabled })
export const issueTemporaryPassword = (id) => api.post(`/api/admin/users/${id}/temporary-password`)
export const deleteUser = (id) => api.delete(`/api/admin/users/${id}`)
export const getStats = () => api.get('/api/admin/stats')
