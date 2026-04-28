#!/usr/bin/env node

/**
 * Phase-6 HTTP baseline load tool (no third-party deps).
 *
 * Example:
 * API_BASE_URL=http://localhost:8080 \
 * USERNAME=admin PASSWORD=123456 \
 * CONCURRENCY=20 DURATION_SECONDS=90 \
 * node tools/phase-6/load/http-load.mjs
 */

const API_BASE_URL = (process.env.API_BASE_URL || 'http://localhost:8080').replace(/\/+$/, '')
const USERNAME = process.env.USERNAME || ''
const PASSWORD = process.env.PASSWORD || ''
const TOKEN = process.env.TOKEN || ''
const AUTO_REGISTER = String(process.env.AUTO_REGISTER || 'false').toLowerCase() === 'true'
const CONCURRENCY = Number(process.env.CONCURRENCY || 20)
const DURATION_SECONDS = Number(process.env.DURATION_SECONDS || 60)
const REQUEST_TIMEOUT_MS = Number(process.env.REQUEST_TIMEOUT_MS || 5000)
const MAX_ERROR_RATE = Number(process.env.MAX_ERROR_RATE || 0.05)
const MAX_P95_MS = Number(process.env.MAX_P95_MS || 1200)
const REQUEST_PATHS = (process.env.REQUEST_PATHS || '/api/incidents?page=0&size=20,/api/investigations?page=0&size=20,/api/anomalies/summary')
  .split(',')
  .map((x) => x.trim())
  .filter(Boolean)

if (!TOKEN && (!USERNAME || !PASSWORD)) {
  console.error('[fatal] missing TOKEN or USERNAME+PASSWORD')
  process.exit(2)
}

function percentile(sorted, p) {
  if (!sorted.length) return 0
  const idx = Math.min(sorted.length - 1, Math.max(0, Math.floor((sorted.length - 1) * p)))
  return sorted[idx]
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
  return { status: res.status, ok: res.ok, data }
}

async function ensureToken() {
  if (TOKEN) return TOKEN

  let login = await postJson('/api/auth/login', { username: USERNAME, password: PASSWORD })
  if (!login.ok && AUTO_REGISTER) {
    const email = `${USERNAME}@phase6.local`
    await postJson('/api/auth/register', { username: USERNAME, email, password: PASSWORD })
    login = await postJson('/api/auth/login', { username: USERNAME, password: PASSWORD })
  }
  if (!login.ok || !login.data?.token) {
    throw new Error(`login failed: ${login.status} ${JSON.stringify(login.data)}`)
  }
  return login.data.token
}

async function timedGet(path, token) {
  const start = process.hrtime.bigint()
  const ctl = new AbortController()
  const timer = setTimeout(() => ctl.abort(), REQUEST_TIMEOUT_MS)
  try {
    const res = await fetch(`${API_BASE_URL}${path}`, {
      method: 'GET',
      headers: { Authorization: `Bearer ${token}` },
      signal: ctl.signal
    })
    const ms = Number(process.hrtime.bigint() - start) / 1e6
    return { ok: res.ok, status: res.status, ms }
  } catch (_e) {
    const ms = Number(process.hrtime.bigint() - start) / 1e6
    return { ok: false, status: 0, ms }
  } finally {
    clearTimeout(timer)
  }
}

async function run() {
  const token = await ensureToken()
  const endAt = Date.now() + DURATION_SECONDS * 1000
  const latencies = []
  const statusCounter = new Map()
  let okCount = 0
  let failCount = 0
  let totalRequests = 0
  let roundRobin = 0

  async function worker() {
    while (Date.now() < endAt) {
      const path = REQUEST_PATHS[roundRobin % REQUEST_PATHS.length]
      roundRobin += 1
      const result = await timedGet(path, token)
      totalRequests += 1
      latencies.push(result.ms)
      statusCounter.set(result.status, (statusCounter.get(result.status) || 0) + 1)
      if (result.ok) okCount += 1
      else failCount += 1
    }
  }

  const startedAt = Date.now()
  console.log('[load] start', {
    API_BASE_URL,
    CONCURRENCY,
    DURATION_SECONDS,
    REQUEST_TIMEOUT_MS,
    REQUEST_PATHS
  })
  await Promise.all(Array.from({ length: CONCURRENCY }, () => worker()))
  const elapsedMs = Date.now() - startedAt

  latencies.sort((a, b) => a - b)
  const avgMs = latencies.length ? latencies.reduce((a, b) => a + b, 0) / latencies.length : 0
  const summary = {
    totalRequests,
    okCount,
    failCount,
    errorRate: totalRequests ? failCount / totalRequests : 0,
    elapsedMs,
    rps: elapsedMs > 0 ? totalRequests / (elapsedMs / 1000) : 0,
    latencyMs: {
      min: latencies[0] || 0,
      avg: avgMs,
      p50: percentile(latencies, 0.5),
      p90: percentile(latencies, 0.9),
      p95: percentile(latencies, 0.95),
      p99: percentile(latencies, 0.99),
      max: latencies[latencies.length - 1] || 0
    },
    statuses: Object.fromEntries([...statusCounter.entries()].sort((a, b) => a[0] - b[0]))
  }
  console.log('[load] summary')
  console.log(JSON.stringify(summary, null, 2))

  const gateFailed = summary.errorRate > MAX_ERROR_RATE || summary.latencyMs.p95 > MAX_P95_MS
  if (gateFailed) {
    console.error(`[gate] failed: errorRate=${summary.errorRate.toFixed(4)} p95=${summary.latencyMs.p95.toFixed(2)}ms`)
    process.exit(1)
  }
  console.log(`[gate] passed: errorRate<=${MAX_ERROR_RATE}, p95<=${MAX_P95_MS}ms`)
}

run().catch((err) => {
  console.error('[fatal]', err.message)
  process.exit(2)
})
