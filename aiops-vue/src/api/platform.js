import api from './http'

export const getPlatformMailSettings = () => api.get('/api/platform/mail-settings')
export const updatePlatformMailSettings = (data) => api.put('/api/platform/mail-settings', data)
export const testPlatformMailSettings = (recipientEmail) => api.post('/api/platform/mail-settings/test', { recipientEmail })
