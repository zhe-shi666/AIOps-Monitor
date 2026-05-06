import api from './http'

export const login = (data) => api.post('/api/auth/login', data)
export const register = (data) => api.post('/api/auth/register', data)
export const startPasswordReset = (data) => api.post('/api/auth/password-reset/start', data)
export const confirmPasswordReset = (data) => api.post('/api/auth/password-reset/confirm', data)
export const changePassword = (data) => api.post('/api/auth/change-password', data)
export const getNotificationProfile = () => api.get('/api/auth/me/notification-profile')
export const updateNotificationProfile = (data) => api.put('/api/auth/me/notification-profile', data)
