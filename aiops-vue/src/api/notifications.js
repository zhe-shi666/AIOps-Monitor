import api from './http'

export const getNotificationChannels = () => api.get('/api/settings/notification-channels')
export const createNotificationChannel = (data) => api.post('/api/settings/notification-channels', data)
export const updateNotificationChannel = (id, data) => api.put(`/api/settings/notification-channels/${id}`, data)
export const updateNotificationChannelEnabled = (id, enabled) => api.put(`/api/settings/notification-channels/${id}/enabled`, { enabled })
export const deleteNotificationChannel = (id) => api.delete(`/api/settings/notification-channels/${id}`)
export const testNotificationChannel = (id) => api.post(`/api/settings/notification-channels/${id}/test`)
