import request from './request'

// 校准记录列表
export function getCalibrationList(params) {
  return request({
    url: '/calibration/list',
    method: 'get',
    params
  })
}

// 获取校准报告（单条详情）
export function getCalibrationReport(id) {
  return request({
    url: `/calibration/${id}`,
    method: 'get'
  })
}

// 即将到期的校准设备列表
export function getUpcomingCalibrations(days = 30) {
  return request({
    url: '/calibration/upcoming',
    method: 'get',
    params: { days }
  })
}

// 新增校准记录
export function addCalibration(data) {
  return request({
    url: '/calibration',
    method: 'post',
    data
  })
}

// 更新校准记录
export function updateCalibration(data) {
  return request({
    url: '/calibration',
    method: 'put',
    data
  })
}

// 删除校准记录
export function deleteCalibration(id) {
  return request({
    url: `/calibration/${id}`,
    method: 'delete'
  })
}
