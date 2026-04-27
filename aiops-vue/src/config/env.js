const trimTrailingSlash = (url) => url.replace(/\/+$/, '')

const apiFromEnv = import.meta.env.VITE_API_BASE_URL
const wsFromEnv = import.meta.env.VITE_WS_BASE_URL

export const API_BASE_URL = trimTrailingSlash(apiFromEnv || 'http://localhost:8080')
export const WS_BASE_URL = trimTrailingSlash(wsFromEnv || API_BASE_URL)
