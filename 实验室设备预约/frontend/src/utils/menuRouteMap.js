/**
 * 将权限表中的 path / permission_code 解析为前端路由 path。
 * init.sql 等与 Vue Router 不一致的路径在此做别名映射。
 */

const PATH_TO_ROUTE = {
  // 一级目录（仅分组，无对应路由，跳过）
  '/system': null,
  '/device': null,
  '/booking': null,
  '/repair': null,
  '/statistics': null,
  '/ai-chat': null,
  '/knowledge': null,
  // 子菜单路径（必须有）
  '/system/user': '/user',
  '/system/role': '/sys-role',
  '/system/log': '/sys-log',
  '/device/list': '/device',
  '/device/category': '/category',
  '/device/scrap': '/device-scrap',
  '/device/calibration': '/calibration',
  '/device/health': '/health-record',
  '/device/status': '/device-status-track',
  '/device/track': '/device-status-track',
  '/lab/list': '/laboratory',
  '/laboratory/list': '/laboratory',
  '/booking/list': '/booking',
  '/booking/my': '/booking',
  '/booking/audit': '/audit',
  '/booking/rule': '/booking-rule-global',
  '/booking-rule': '/booking-rule-global',
  '/repair/list': '/repair',
  '/repair/report': '/repair',
  '/repair/record': '/repair-records',
  '/statistics/overview': '/statistics',
  '/statistics/usage': '/statistics',
  '/knowledge/list': '/knowledge-manage',
  // init.sql 旧路径别名（连字符形式，真实存在于数据库）
  '/device-track': '/device-status-track',
  '/device-health': '/health-record',
  '/ai-chat': '/ai-assistant',
  '/role': '/sys-role',
  '/permission': '/sys-permission'
}

const CODE_TO_ROUTE = {
  // 一级目录（仅分组，无对应路由）
  system: null,
  device: null,
  booking: null,
  repair: null,
  statistics: null,
  'ai-chat': null,
  knowledge: null,
  // 子菜单权限码（必须有）
  user: '/user',
  role: '/sys-role',
  log: '/sys-log',
  'device-list': '/device',
  'device-scrap': '/device-scrap',
  'device-health': '/health-record',
  'device-status': '/device-status-track',
  'device-track': '/device-status-track',
  laboratory: '/laboratory',
  'lab-list': '/laboratory',
  booking: '/booking',
  'booking-list': '/booking',
  'booking-my': '/booking',
  'booking-audit': '/audit',
  repair: '/repair',
  'repair-list': '/repair',
  'repair-report': '/repair',
  statistics: '/statistics',
  'stats-overview': '/statistics',
  'stats-usage': '/statistics',
  'ai-chat': '/ai-assistant',
  knowledge: '/knowledge-manage',
  'knowledge-list': '/knowledge-manage',
  // init.sql 旧 permission_code 别名
  'sys-log': '/sys-log',
  'backup-restore': '/backup-restore',
  'category': '/category',
  'booking-rule': '/booking-rule-global',
  'booking-rule-global': '/booking-rule-global',
  'booking-global': '/booking-global',
  'device-entry': '/device-entry',
  'device-usage': '/device-usage',
  'calibration': '/calibration',
  'health-record': '/health-record',
  'repair-records': '/repair-records',
  'repair-global-monitor': '/repair-global-monitor',
  'ai-assistant': '/ai-assistant',
  'sys-config': '/sys-config',
  'sys-permission': '/sys-permission'
}

/** 与 router 中 path 一致（无前导 segment 缺失） */
const KNOWN_FRONT_PATHS = new Set([
  '/dashboard',
  '/user',
  '/sys-role',
  '/sys-permission',
  '/sys-config',
  '/sys-log',
  '/device',
  '/device-entry',
  '/device-usage',
  '/device-status-track',
  '/laboratory',
  '/device-scrap',
  '/category',
  '/booking',
  '/booking-global',
  '/booking-rule-global',
  '/audit',
  '/repair',
  '/repair-records',
  '/repair-global-monitor',
  '/calibration',
  '/health-record',
  '/statistics',
  '/ai-assistant',
  '/knowledge-manage',
  '/backup-restore',
  '/profile'
])

function normalizePath(path) {
  if (path == null || typeof path !== 'string') return ''
  let s = path.trim()
  if (!s) return ''
  if (!s.startsWith('/')) s = `/${s}`
  if (s.length > 1 && s.endsWith('/')) s = s.slice(0, -1)
  return s
}

/**
 * @param {string|null|undefined} path
 * @param {string|null|undefined} permissionCode
 * @returns {string|null} 前端路由，不可解析时 null
 */
export function resolveMenuRoute(path, permissionCode) {
  const p = normalizePath(path)
  const code = permissionCode && String(permissionCode).trim()

  if (p && Object.prototype.hasOwnProperty.call(PATH_TO_ROUTE, p)) {
    const mapped = PATH_TO_ROUTE[p]
    if (mapped) return mapped
    if (code && CODE_TO_ROUTE[code]) return CODE_TO_ROUTE[code]
    return null
  }

  if (p && KNOWN_FRONT_PATHS.has(p)) return p

  if (code && CODE_TO_ROUTE[code]) {
    return CODE_TO_ROUTE[code]
  }

  return null
}
