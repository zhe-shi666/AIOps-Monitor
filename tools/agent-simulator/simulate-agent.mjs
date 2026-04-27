#!/usr/bin/env node

import os from 'node:os'

const API_BASE_URL = (process.env.API_BASE_URL || 'http://localhost:8080').replace(/\/+$/, '')
const AGENT_KEY = process.env.AGENT_KEY || ''
const HOSTNAME = process.env.HOSTNAME_OVERRIDE || os.hostname()
const AGENT_VERSION = process.env.AGENT_VERSION || 'sim-1.0.0'
const IP = process.env.AGENT_IP || '127.0.0.1'
const INTERVAL_MS = Number(process.env.INTERVAL_MS || 5000)
const CPU_BASE = Number(process.env.CPU_BASE || 35)
const MEM_BASE = Number(process.env.MEM_BASE || 60)
const DISK_BASE = Number(process.env.DISK_BASE || 55)
const NET_RX_BASE = Number(process.env.NET_RX_BASE || 1048576)
const NET_TX_BASE = Number(process.env.NET_TX_BASE || 786432)
const PROCESS_BASE = Number(process.env.PROCESS_BASE || 180)

if (!AGENT_KEY) {
  console.error('Missing AGENT_KEY. Example: AGENT_KEY=xxxx node tools/agent-simulator/simulate-agent.mjs')
  process.exit(1)
}

function jitter(base, range, min = 0, max = Number.POSITIVE_INFINITY) {
  const value = base + (Math.random() * range * 2 - range)
  return Number(Math.max(min, Math.min(max, value)).toFixed(2))
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
  const payload = {
    agentKey: AGENT_KEY,
    hostname: HOSTNAME,
    cpuUsage: jitter(CPU_BASE, 18, 0, 100),
    memUsage: jitter(MEM_BASE, 12, 0, 100),
    diskUsage: jitter(DISK_BASE, 10, 0, 100),
    netRxBytesPerSec: Number(jitter(NET_RX_BASE, NET_RX_BASE * 0.4, 0).toFixed(0)),
    netTxBytesPerSec: Number(jitter(NET_TX_BASE, NET_TX_BASE * 0.4, 0).toFixed(0)),
    processCount: Number(jitter(PROCESS_BASE, 60, 1).toFixed(0)),
    timestamp: Date.now(),
    ip: IP,
    agentVersion: AGENT_VERSION
  }
  const data = await post('/api/agent/heartbeat', payload)
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
}

async function main() {
  console.log('Agent simulator started', {
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
