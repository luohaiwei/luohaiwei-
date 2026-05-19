import request from './request'

// 获取维修工单列表
export function getRepairList(params) {
  return request({
    url: '/repair/list',
    method: 'get',
    params
  })
}

// 维修记录查询（已完成/已关闭，条件筛选，RM-004；独立路径 /repair-record/list）
export function getRepairRecords(params) {
  return request({
    url: '/repair-record/list',
    method: 'get',
    params
  })
}

// 创建报修
export function createRepair(data) {
  return request({
    url: '/repair',
    method: 'post',
    data
  })
}

// 上传故障图片（可选）
export function uploadRepairImage(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/repair/upload-image',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

// 处理工单
export function handleRepair(id, handlerId) {
  return request({
    url: `/repair/${id}/handle`,
    method: 'put',
    params: { handlerId }
  })
}

// 维护人员接单
export function acceptRepair(id) {
  return request({
    url: `/repair/${id}/accept`,
    method: 'put'
  })
}

// 完成工单
export function completeRepair(id, data) {
  return request({
    url: `/repair/${id}/complete`,
    method: 'put',
    params: data
  })
}

// ========== 全局维修工单监控（系统管理员） ==========
export function getGlobalRepairOrders(params) {
  return request({ url: '/repair/global', method: 'get', params })
}
export function getRepairOrderDetail(id) {
  return request({ url: `/repair/${id}`, method: 'get' })
}
export function reassignRepair(id, assigneeId) {
  return request({ url: `/repair/${id}/reassign`, method: 'put', params: { assigneeId } })
}
export function adjustRepairPriority(id, priority) {
  return request({ url: `/repair/${id}/priority`, method: 'put', params: { priority } })
}

// ========== 待分配工单（实验室管理员） ==========

// 获取待分配工单列表（assigned_to IS NULL）
export function getPendingRepairs(params) {
  return request({ url: '/repair/pending', method: 'get', params })
}

// 获取所有维护人员列表
export function getMaintainers() {
  return request({ url: '/repair/maintainers', method: 'get' })
}

// 分配工单给维护人员
export function assignRepair(data) {
  return request({ url: '/repair/assign', method: 'post', data })
}

