import request from './request';

// 获取首页统计数据
export function getDashboard() {
  return request({
    url: '/statistics/dashboard',
    method: 'get'
  });
}

// 获取设备状态分布
export function getDeviceStatus() {
  return request({
    url: '/statistics/device-status',
    method: 'get'
  });
}

// 获取预约趋势
export function getBookingTrend() {
  return request({
    url: '/statistics/booking-trend',
    method: 'get'
  });
}

// 获取设备类型分布
export function getDeviceType() {
  return request({
    url: '/statistics/device-type',
    method: 'get'
  });
}

// 获取预约高峰时段
export function getPeakHours() {
  return request({
    url: '/statistics/peak-hours',
    method: 'get'
  });
}

// 获取平均预约等待时长
export function getAvgWaitTime(period = 'week') {
  return request({
    url: '/statistics/avg-wait-time',
    method: 'get',
    params: { period }
  });
}

// 获取等待时长趋势
export function getWaitTimeTrend(days = 7) {
  return request({
    url: '/statistics/wait-time-trend',
    method: 'get',
    params: { days }
  });
}

// 获取用户活跃度统计
export function getUserActivity(period = 'week', days = 7, limit = 10) {
  return request({
    url: '/statistics/user-activity',
    method: 'get',
    params: { period, days, limit }
  });
}

// 导出Excel
export function exportExcel() {
  return request({
    url: '/statistics/export/excel',
    method: 'get',
    responseType: 'blob'
  });
}

// 导出PDF
export function exportPdf() {
  return request({
    url: '/statistics/export/pdf',
    method: 'get',
    responseType: 'blob'
  });
}

// ========== 系统管理员数据分析 API ==========

// 用户统计分析
export function getUserStatistics() {
  return request({ url: '/statistics/user', method: 'get' });
}

// 设备使用分析
export function getDeviceUsageAnalysis() {
  return request({ url: '/statistics/device-usage', method: 'get' });
}

// 预约全量分析
export function getBookingAnalysis() {
  return request({ url: '/statistics/booking-analysis', method: 'get' });
}

// 维护统计分析
export function getMaintenanceStatistics() {
  return request({ url: '/statistics/maintenance', method: 'get' });
}

// 全量报表导出（可按类型导出）
export function exportReport(params) {
  return request({ url: '/statistics/report/export', method: 'get', params, responseType: 'blob' });
}

// ========== 用户画像分析 API ==========

// 获取用户画像分析数据
export function getUserProfile() {
  return request({
    url: '/statistics/user-profile',
    method: 'get'
  });
}

// ========== 维护人员统计 API ==========

// 获取维护人员个人统计
export function getMaintainerStats() {
  return request({
    url: '/statistics/maintainer-stats',
    method: 'get'
  });
}

// ========== 校准达标率 API ==========

// 获取校准达标率统计
export function getCalibrationRate(months = 6) {
  return request({
    url: '/statistics/calibration-rate',
    method: 'get',
    params: { months }
  });
}

// ========== 智能预测 API ==========

// 获取预约趋势预测数据
export function getBookingPrediction(params) {
  return request({
    url: '/statistics/booking-prediction',
    method: 'get',
    params
  });
}
