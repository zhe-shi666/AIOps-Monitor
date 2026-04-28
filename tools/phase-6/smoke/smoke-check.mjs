#!/usr/bin/env node

/**
 * Phase-6 smoke check.
 *
 * Example:
 * API_BASE_URL=http://localhost:8080 USERNAME=admin PASSWORD=123456 \
 * node tools/phase-6/smoke/smoke-check.mjs
 */

const API_BASE_URL = (process.env.API_BASE_URL || 'http://localhost:8080').replace(/\/+$/, '')
const USERNAME = process.env.USERNAME || ''
const PASSWORD = process.env.PASSWORD || ''
const TOKEN = process.env.TOKEN || ''
const AUTO_REGISTER = String(process.env.AUTO_REGISTER || 'false').toLowerCase() === 'true'

if (!TOKEN && (!USERNAME || !PASSWORD)) {
  console.error('[fatal] missing TOKEN or USERNAME+PASSWORD')
  process.exit(2)
}

async function postJson(path, body) {
  const res = await fetch(`${API_BASE_URL}${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body)
  })
  const text = await res.text()
  let data = null
  if (text) {
    try {
      data = JSON.parse(text)
    } catch {
      data = { raw: text }
    }
  }
  return { ok: res.ok, status: res.status, data }
}

async function ensureToken() {
  if (TOKEN) return TOKEN
  let login = await postJson('/api/auth/login', { username: USERNAME, password: PASSWORD })
  if (!login.ok && AUTO_REGISTER) {
    await postJson('/api/auth/register', { username: USERNAME, email: `${USERNAME}@phase6.local`, password: PASSWORD })
    login = await postJson('/api/auth/login', { username: USERNAME, password: PASSWORD })
  }
  if (!login.ok || !login.data?.token) {
    throw new Error(`login failed: ${login.status} ${JSON.stringify(login.data)}`)
  }
  return login.data.token
}

async function check(name, fn) {
  try {
    await fn()
    console.log(`[pass] ${name}`)
    return true
  } catch (err) {
    console.error(`[fail] ${name}: ${err.message}`)
    return false
  }
}

async function checkGet(path, expectedStatuses = [200], headers = {}) {
  const res = await fetch(`${API_BASE_URL}${path}`, { headers })
  if (!expectedStatuses.includes(res.status)) {
    const body = await res.text()
    throw new Error(`status=${res.status}, body=${body.slice(0, 200)}`)
  }
}

async function main() {
  const token = await ensureToken()
  const authHeader = { Authorization: `Bearer ${token}` }
  const results = []

  results.push(await check('actuator health', async () => {
    await checkGet('/actuator/health', [200])
  }))

  results.push(await check('incidents list', async () => {
    await checkGet('/api/incidents?page=0&size=5', [200], authHeader)
  }))

  results.push(await check('investigations list', async () => {
    await checkGet('/api/investigations?page=0&size=5', [200], authHeader)
  }))

  results.push(await check('anomaly summary', async () => {
    await checkGet('/api/anomalies/summary', [200], authHeader)
  }))

  const passed = results.every(Boolean)
  if (!passed) process.exit(1)
  console.log('[smoke] all checks passed')
}

main().catch((err) => {
  console.error('[fatal]', err.message)
  process.exit(2)
})
