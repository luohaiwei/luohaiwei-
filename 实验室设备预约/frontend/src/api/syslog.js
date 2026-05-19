import request from './request'

// 系统日志列表
export function getSysLogList(params) {
  return request({
    url: '/sys/log/list',
    method: 'get',
    params
  })
}

// 用户操作日志
export function getOperationLogs(params) {
  return request({ url: '/sys/log/operation', method: 'get', params })
}

// 登录日志
export function getLoginLogs(params) {
  return request({ url: '/sys/log/login', method: 'get', params })
}

// 设备状态变更日志（独立查询，与 /device/status-logs 同源数据）
export function getDeviceStatusLogs(params) {
  return request({ url: '/sys/log/device-status', method: 'get', params })
}

// 权限变更审计
export function getPermissionAuditLogs(params) {
  return request({ url: '/sys/log/permission', method: 'get', params })
}

// 导出日志
export function exportLog(params) {
  return request({ url: '/sys/log/export', method: 'get', params, responseType: 'blob' })
}

// 清理旧日志
export function cleanupOldLogs(keepCount) {
  return request({ 
    url: '/sys/log/cleanup', 
    method: 'post',
    params: { keepCount }
  })
}
