import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const routerFile = readFileSync(resolve('src/router/index.js'), 'utf8')
const appShellFile = readFileSync(resolve('src/components/AppShell.vue'), 'utf8')
const permissionFile = readFileSync(resolve('src/composables/usePermissions.js'), 'utf8')

const requiredRouteRules = [
  "allowedRoles: ['ADMIN', 'OPS', 'AUDITOR']",
  "allowedRoles: ['ADMIN', 'OPS']",
  'requiresAdmin: true'
]

for (const rule of requiredRouteRules) {
  if (!routerFile.includes(rule)) {
    throw new Error(`router permission rule missing: ${rule}`)
  }
}

const requiredPermissionHelpers = ['canOperate', 'canAudit', 'canManageUsers', 'readOnlyReason']
for (const helper of requiredPermissionHelpers) {
  if (!permissionFile.includes(helper)) {
    throw new Error(`permission helper missing: ${helper}`)
  }
}

const requiredShellGuards = ['canOperate', 'canAudit']
for (const guard of requiredShellGuards) {
  if (!appShellFile.includes(guard)) {
    throw new Error(`AppShell navigation guard missing: ${guard}`)
  }
}

console.log('[ok] frontend permission matrix smoke passed')
