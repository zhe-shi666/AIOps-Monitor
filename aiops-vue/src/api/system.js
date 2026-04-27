import api from './http'

export const getHardwareInfo = () => api.get('/api/system/hardware')
