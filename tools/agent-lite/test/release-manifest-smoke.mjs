import { createHmac } from 'node:crypto'
import { readFileSync, statSync } from 'node:fs'
import { resolve } from 'node:path'

const root = resolve(import.meta.dirname, '../../..')
const manifest = JSON.parse(readFileSync(resolve(root, 'dist/agent-release.json'), 'utf8'))
const packagePath = resolve(root, 'dist', manifest.packageName || 'aiops-agent-lite.tar.gz')
const packageStat = statSync(packagePath)

const requiredKeys = ['latestVersion', 'packageName', 'sha256', 'signature', 'supports', 'createdAt']
for (const key of requiredKeys) {
  if (!manifest[key] || (Array.isArray(manifest[key]) && manifest[key].length === 0)) {
    throw new Error(`agent release manifest missing ${key}`)
  }
}

if (!/^[a-f0-9]{64}$/.test(manifest.sha256)) {
  throw new Error('agent release manifest sha256 must be a 64-char hex string')
}

if (!/^[a-f0-9]{64}$/.test(manifest.signature)) {
  throw new Error('agent release manifest signature must be a 64-char hex HMAC')
}

const expectedSignature = createHmac('sha256', process.env.AGENT_SIGNING_KEY || 'local-dev-agent-signing-key-change-me')
  .update(manifest.sha256)
  .digest('hex')

if (manifest.signature !== expectedSignature) {
  throw new Error('agent release manifest signature does not match AGENT_SIGNING_KEY')
}

if (packageStat.size <= 0) {
  throw new Error('agent package is empty')
}

console.log('[ok] agent release manifest smoke passed')
