import request from './request'

// 获取设备列表
export function getDeviceList(params) {
  return request({
    url: '/device/list',
    method: 'get',
    params
  })
}

// 导出设备列表（Excel，筛选条件与列表一致）
export function exportDeviceList(params) {
  return request({
    url: '/device/list/export',
    method: 'get',
    params,
    responseType: 'blob',
    skipErrorNotify: true
  })
}

// 获取设备详情
export function getDeviceDetail(id) {
  return request({
    url: `/device/${id}`,
    method: 'get'
  })
}

// 上传设备图片（multipart，勿手动设置 Content-Type）
export function uploadDeviceImage(file) {
  const fd = new FormData()
  fd.append('file', file)
  return request({
    url: '/device/upload-image',
    method: 'post',
    data: fd
  })
}

// 添加设备
export function addDevice(data) {
  return request({
    url: '/device',
    method: 'post',
    data
  })
}

// 更新设备（兼容后端仅支持 PUT /device 的场景）
export function updateDevice(id, data) {
  return request({
    url: '/device',
    method: 'put',
    data: {
      ...(data || {}),
      id
    }
  })
}

// 删除设备
export function deleteDevice(id) {
  return request({
    url: `/device/${id}`,
    method: 'delete'
  })
}

// 更新设备状态
export function updateDeviceStatus(id, status) {
  return request({
    url: `/device/${id}/status`,
    method: 'put',
    params: { status }
  })
}

// 获取所有设备
export function getAllDevices() {
  return request({
    url: '/device/all',
    method: 'get'
  })
}

// 智能推荐设备（协同过滤）
export function getRecommendDevices(limit = 5) {
  return request({
    url: '/device/recommend',
    method: 'get',
    params: { limit }
  })
}

// 推荐最优预约时段
export function getRecommendTimeSlots(deviceId, date) {
  return request({
    url: `/device/${deviceId}/recommend-slots`,
    method: 'get',
    params: { date }
  })
}

// 单设备状态变更记录（设备详情页；GET /device/{id}/status-logs）
export function getDeviceStatusLogsByDeviceId(deviceId) {
  return request({
    url: `/device/${deviceId}/status-logs`,
    method: 'get',
    skipErrorNotify: true
  })
}

// 全局分页查询设备状态变更日志（设备状态追踪页、仪表盘统计）
export function getGlobalDeviceStatusLogs(params) {
  return request({
    url: '/device/status-logs',
    method: 'get',
    params,
    skipErrorNotify: true
  })
}

// 导出设备状态变更日志
export function exportDeviceStatusLogs(params) {
  return request({ url: '/device/status-logs/export', method: 'get', params, responseType: 'blob' })
}

// ========== 设备报废管理 ==========
export function getScrapList(params) {
  return request({ url: '/device/scrap/list', method: 'get', params })
}
export function getScrapDetail(id) {
  return request({ url: `/device/scrap/${id}`, method: 'get' })
}
export function approveScrap(id) {
  return request({ url: `/device/scrap/${id}/approve`, method: 'put' })
}
export function rejectScrap(id, opinion) {
  return request({ url: `/device/scrap/${id}/reject`, method: 'put', params: { opinion } })
}
export function archiveScrap(id) {
  return request({ url: `/device/scrap/${id}/archive`, method: 'put' })
}
export function submitScrapApplication(data) {
  return request({ url: '/device/scrap', method: 'post', data })
}

export function exportScrapList(params) {
  return request({ url: '/device/scrap/export', method: 'get', params, responseType: 'blob' })
}

// ========== 设备使用记录（DeviceUsage.vue） ==========
export function getDeviceUsageRecords(params) {
  return request({ url: '/device/usage', method: 'get', params })
}

export function exportDeviceUsageRecords(params) {
  return request({ url: '/device/usage/export', method: 'get', params, responseType: 'blob' })
}
