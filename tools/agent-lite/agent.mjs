#!/usr/bin/env node

import fs from 'node:fs/promises'
import os from 'node:os'
import { execFile } from 'node:child_process'
import { promisify } from 'node:util'

const execFileAsync = promisify(execFile)

const API_BASE_URL = (process.env.API_BASE_URL || '').replace(/\/+$/, '')
const AGENT_KEY = process.env.AGENT_KEY || ''
const HOSTNAME = process.env.HOSTNAME_OVERRIDE || os.hostname()
const AGENT_VERSION = process.env.AGENT_VERSION || 'agent-lite-1.2.0-cross'
const IP = process.env.AGENT_IP || resolvePrimaryIp()
const INTERVAL_MS = Number(process.env.INTERVAL_MS || 5000)
const OBSERVABILITY_ENABLED = (process.env.OBSERVABILITY_ENABLED || 'true') !== 'false'
const LOG_EVERY_N_HEARTBEATS = Math.max(1, Number(process.env.LOG_EVERY_N_HEARTBEATS || 12))
const PLATFORM = os.platform()

let previousCpuTimes = null
let previousNetworkBytes = null
let heartbeatCount = 0

if (!API_BASE_URL) {
  console.error('Missing API_BASE_URL. Example: http://10.0.0.8:8080')
  process.exit(1)
}

if (!AGENT_KEY) {
  console.error('Missing AGENT_KEY. Please use the key from "Target Management".')
  process.exit(1)
}

function resolvePrimaryIp() {
  const interfaces = os.networkInterfaces()
  for (const entries of Object.values(interfaces)) {
    for (const item of entries || []) {
      if (item.family === 'IPv4' && !item.internal) {
        return item.address
      }
    }
  }
  return '127.0.0.1'
}

function round(value, digits = 2) {
  return Number(value.toFixed(digits))
}

function readCpuTimes() {
  return os.cpus().reduce((total, cpu) => {
    for (const [key, value] of Object.entries(cpu.times)) {
      total[key] = (total[key] || 0) + value
    }
    return total
  }, {})
}

function calculateCpuUsage() {
  const current = readCpuTimes()
  if (!previousCpuTimes) {
    previousCpuTimes = current
    return 0
  }

  const idle = current.idle - previousCpuTimes.idle
  const total = Object.keys(current).reduce((sum, key) => sum + current[key] - previousCpuTimes[key], 0)
  previousCpuTimes = current
  if (total <= 0) return 0
  return round(Math.max(0, Math.min(100, (1 - idle / total) * 100)))
}

function calculateMemoryUsage() {
  const total = os.totalmem()
  const free = os.freemem()
  if (!total) return 0
  return round(((total - free) / total) * 100)
}

async function readDiskUsage() {
  if (PLATFORM === 'win32') {
    return readWindowsDiskUsage()
  }

  try {
    const { stdout } = await execFileAsync('df', ['-kP', '/'])
    const lines = stdout.trim().split('\n')
    const parts = lines[1]?.trim().split(/\s+/) || []
    const used = Number(parts[2])
    const available = Number(parts[3])
    if (!Number.isFinite(used) || !Number.isFinite(available) || used + available <= 0) return 0
    return round((used / (used + available)) * 100)
  } catch (_e) {
    return 0
  }
}

async function readNetworkBytes() {
  if (PLATFORM === 'darwin') {
    return readDarwinNetworkBytes()
  }
  if (PLATFORM === 'win32') {
    return readWindowsNetworkBytes()
  }

  try {
    const content = await fs.readFile('/proc/net/dev', 'utf8')
    return content
      .split('\n')
      .slice(2)
      .reduce((total, line) => {
        const [rawName, rawStats] = line.trim().split(':')
        const name = rawName?.trim()
        if (!name || name === 'lo' || !rawStats) return total
        const stats = rawStats.trim().split(/\s+/).map(Number)
        total.rx += Number.isFinite(stats[0]) ? stats[0] : 0
        total.tx += Number.isFinite(stats[8]) ? stats[8] : 0
        return total
      }, { rx: 0, tx: 0 })
  } catch (_e) {
    return { rx: 0, tx: 0 }
  }
}

async function calculateNetworkBytesPerSec() {
  const current = await readNetworkBytes()
  const now = Date.now()
  if (!previousNetworkBytes) {
    previousNetworkBytes = { ...current, timestamp: now }
    return { rx: 0, tx: 0 }
  }

  const seconds = Math.max((now - previousNetworkBytes.timestamp) / 1000, 1)
  const rx = Math.max(0, (current.rx - previousNetworkBytes.rx) / seconds)
  const tx = Math.max(0, (current.tx - previousNetworkBytes.tx) / seconds)
  previousNetworkBytes = { ...current, timestamp: now }
  return {
    rx: Math.round(rx),
    tx: Math.round(tx)
  }
}

async function readProcessCount() {
  if (PLATFORM === 'win32') {
    return readWindowsProcessCount()
  }

  try {
    const { stdout } = await execFileAsync('ps', ['-A', '-o', 'pid='])
    return stdout.split('\n').filter((line) => line.trim()).length
  } catch (_e) {
    try {
      const entries = await fs.readdir('/proc')
      return entries.filter((name) => /^\d+$/.test(name)).length
    } catch (_fallback) {
      return 0
    }
  }
}

async function readWindowsDiskUsage() {
  try {
    const script = "$d=Get-CimInstance Win32_LogicalDisk -Filter \"DeviceID='C:'\"; if ($d) { Write-Output $d.Size; Write-Output $d.FreeSpace }"
    const { stdout } = await execFileAsync('powershell.exe', ['-NoProfile', '-Command', script])
    const [size, free] = stdout.trim().split(/\r?\n/).map(Number)
    if (!Number.isFinite(size) || !Number.isFinite(free) || size <= 0) return 0
    return round(((size - free) / size) * 100)
  } catch (_e) {
    return 0
  }
}

async function readDarwinNetworkBytes() {
  try {
    const { stdout } = await execFileAsync('netstat', ['-ibn'])
    return stdout
      .split('\n')
      .slice(1)
      .reduce((total, line) => {
        const parts = line.trim().split(/\s+/)
        const name = parts[0]
        if (!name || name === 'lo0' || parts.length < 10) return total
        const rx = Number(parts[6])
        const tx = Number(parts[9])
        total.rx += Number.isFinite(rx) ? rx : 0
        total.tx += Number.isFinite(tx) ? tx : 0
        return total
      }, { rx: 0, tx: 0 })
  } catch (_e) {
    return { rx: 0, tx: 0 }
  }
}

async function readWindowsNetworkBytes() {
  try {
    const script = [
      '$s=Get-NetAdapterStatistics | Measure-Object -Property ReceivedBytes -Sum;',
      '$t=Get-NetAdapterStatistics | Measure-Object -Property SentBytes -Sum;',
      'Write-Output $s.Sum;',
      'Write-Output $t.Sum;'
    ].join(' ')
    const { stdout } = await execFileAsync('powershell.exe', ['-NoProfile', '-Command', script])
    const [rx, tx] = stdout.trim().split(/\r?\n/).map(Number)
    return {
      rx: Number.isFinite(rx) ? rx : 0,
      tx: Number.isFinite(tx) ? tx : 0
    }
  } catch (_e) {
    return { rx: 0, tx: 0 }
  }
}

async function readWindowsProcessCount() {
  try {
    const { stdout } = await execFileAsync('powershell.exe', ['-NoProfile', '-Command', '(Get-Process).Count'])
    const count = Number(stdout.trim())
    return Number.isFinite(count) ? count : 0
  } catch (_e) {
    return 0
  }
}

async function collectMetrics() {
  const network = await calculateNetworkBytesPerSec()
  return {
    cpuUsage: calculateCpuUsage(),
    memUsage: calculateMemoryUsage(),
    diskUsage: await readDiskUsage(),
    netRxBytesPerSec: network.rx,
    netTxBytesPerSec: network.tx,
    processCount: await readProcessCount()
  }
}

async function post(path, body) {
  const res = await fetch(`${API_BASE_URL}${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body)
  })
  const text = await res.text()
  const payload = text ? JSON.parse(text) : {}
  if (!res.ok) {
    throw new Error(`${path} failed: ${res.status} ${JSON.stringify(payload)}`)
  }
  return payload
}

async function register() {
  const payload = {
    agentKey: AGENT_KEY,
    hostname: HOSTNAME,
    ip: IP,
    agentVersion: AGENT_VERSION
  }
  const data = await post('/api/agent/register', payload)
  console.log('[register]', data)
}

async function heartbeat() {
  const startedAt = Date.now()
  const metrics = await collectMetrics()
  const payload = {
    agentKey: AGENT_KEY,
    hostname: HOSTNAME,
    cpuUsage: metrics.cpuUsage,
    memUsage: metrics.memUsage,
    diskUsage: metrics.diskUsage,
    netRxBytesPerSec: metrics.netRxBytesPerSec,
    netTxBytesPerSec: metrics.netTxBytesPerSec,
    processCount: metrics.processCount,
    timestamp: Date.now(),
    ip: IP,
    agentVersion: AGENT_VERSION
  }
  const data = await post('/api/agent/heartbeat', payload)
  heartbeatCount += 1
  console.log(
    '[heartbeat]',
    new Date().toISOString(),
    `CPU=${payload.cpuUsage}%`,
    `MEM=${payload.memUsage}%`,
    `DISK=${payload.diskUsage}%`,
    `RX=${payload.netRxBytesPerSec}B/s`,
    `TX=${payload.netTxBytesPerSec}B/s`,
    `PROC=${payload.processCount}`,
    data.accepted
  )
  if (OBSERVABILITY_ENABLED) {
    sendObservability(metrics, Date.now() - startedAt).catch((err) => {
      console.error('[observability:error]', err.message)
    })
  }
}

function buildTraceId() {
  return `${Date.now().toString(16)}${Math.random().toString(16).slice(2, 18)}`.slice(0, 32)
}

function buildLabels(metrics) {
  return JSON.stringify({
    ip: IP,
    agentVersion: AGENT_VERSION,
    cpuUsage: metrics.cpuUsage,
    memUsage: metrics.memUsage,
    diskUsage: metrics.diskUsage,
    processCount: metrics.processCount
  })
}

async function sendObservability(metrics, durationMs) {
  const traceId = buildTraceId()
  const now = Date.now()
  const shouldLog = heartbeatCount === 1 || heartbeatCount % LOG_EVERY_N_HEARTBEATS === 0
  const requests = [
    post('/api/agent/traces', {
      agentKey: AGENT_KEY,
      hostname: HOSTNAME,
      serviceName: 'aiops-agent-lite',
      traces: [{
        traceId,
        spanId: 'heartbeat',
        operationName: 'collect-and-send-heartbeat',
        durationMs,
        status: 'OK',
        attributesJson: buildLabels(metrics),
        timestamp: now
      }]
    })
  ]

  if (shouldLog) {
    requests.push(post('/api/agent/logs', {
      agentKey: AGENT_KEY,
      hostname: HOSTNAME,
      serviceName: 'aiops-agent-lite',
      source: 'agent-lite',
      logs: [{
        level: 'INFO',
        message: `agent heartbeat accepted: cpu=${metrics.cpuUsage}% mem=${metrics.memUsage}% disk=${metrics.diskUsage}% rx=${metrics.netRxBytesPerSec}B/s tx=${metrics.netTxBytesPerSec}B/s proc=${metrics.processCount}`,
        traceId,
        labelsJson: buildLabels(metrics),
        timestamp: now
      }]
    }))
  }

  await Promise.allSettled(requests)
}

async function main() {
  console.log('AIOps agent-lite started', {
    metricMode: 'real-cross-platform',
    platform: PLATFORM,
    observabilityEnabled: OBSERVABILITY_ENABLED,
    API_BASE_URL,
    HOSTNAME,
    INTERVAL_MS,
    AGENT_VERSION,
    IP
  })

  await register()
  await heartbeat()
  setInterval(() => {
    heartbeat().catch((err) => {
      console.error('[heartbeat:error]', err.message)
    })
  }, INTERVAL_MS)
}

main().catch((err) => {
  console.error('[fatal]', err.message)
  process.exit(1)
})
