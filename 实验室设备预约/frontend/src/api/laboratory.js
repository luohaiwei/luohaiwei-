import request from './request'

// 获取实验室列表（分页）
export function getLaboratoryList(params) {
  return request({
    url: '/laboratory/list',
    method: 'get',
    params
  })
}

// 获取所有实验室（用于下拉选择）
export function getAllLaboratories() {
  return request({
    url: '/laboratory/all',
    method: 'get'
  })
}

// 获取实验室详情
export function getLaboratoryDetail(id) {
  return request({
    url: `/laboratory/${id}`,
    method: 'get'
  })
}

// 新增实验室
export function addLaboratory(data) {
  return request({
    url: '/laboratory',
    method: 'post',
    data
  })
}

// 更新实验室
export function updateLaboratory(id, data) {
  return request({
    url: `/laboratory/${id}`,
    method: 'put',
    data
  })
}

// 删除实验室
export function deleteLaboratory(id) {
  return request({
    url: `/laboratory/${id}`,
    method: 'delete'
  })
}

// 获取实验室统计信息
export function getLaboratoryStatistics() {
  return request({
    url: '/laboratory/statistics',
    method: 'get'
  })
}
