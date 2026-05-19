import request from './request'

// AI问答
export function aiChat(data) {
  return request({
    url: '/ai/chat',
    method: 'post',
    data
  })
}

// 获取聊天历史
export function getChatHistory(limit = 50) {
  return request({
    url: '/ai/history',
    method: 'get',
    params: { limit }
  })
}

// 获取知识库分类
export function getCategories() {
  return request({
    url: '/ai/categories',
    method: 'get'
  })
}

// 获取分类知识列表
export function getCategoryList(category) {
  return request({
    url: `/ai/category/${category}`,
    method: 'get'
  })
}

// 获取危险操作提醒
export function getDangerAlerts() {
  return request({
    url: '/ai/danger-alerts',
    method: 'get'
  })
}

// ========== 设备详情关联知识 ==========
export function getKnowledgeByDeviceId(deviceId) {
  return request({
    url: `/ai/device/${deviceId}`,
    method: 'get'
  })
}

// ========== 知识图谱 ==========
export function getGraphData() {
  return request({
    url: '/knowledge-graph/data',
    method: 'get'
  })
}

export function getGraphStats() {
  return request({
    url: '/knowledge-graph/stats',
    method: 'get'
  })
}

export function getNodeNeighbors(nodeId) {
  return request({
    url: `/knowledge-graph/node/${nodeId}/neighbors`,
    method: 'get'
  })
}

// ========== 知识图谱手动管理 ==========
export function getGraphManageOptions() {
  return request({ url: '/knowledge-graph/manage/options', method: 'get' })
}

export function getGraphNodeList(params) {
  return request({ url: '/knowledge-graph/manage/nodes', method: 'get', params })
}
export function addGraphNode(data) {
  return request({ url: '/knowledge-graph/manage/nodes', method: 'post', data })
}
export function updateGraphNode(id, data) {
  return request({ url: `/knowledge-graph/manage/nodes/${id}`, method: 'put', data })
}
export function updateGraphNodeStatus(id, status) {
  return request({ url: `/knowledge-graph/manage/nodes/${id}/status`, method: 'put', data: { status } })
}
export function deleteGraphNode(id) {
  return request({ url: `/knowledge-graph/manage/nodes/${id}`, method: 'delete' })
}

export function getGraphEdgeList(params) {
  return request({ url: '/knowledge-graph/manage/edges', method: 'get', params })
}
export function addGraphEdge(data) {
  return request({ url: '/knowledge-graph/manage/edges', method: 'post', data })
}
export function updateGraphEdge(id, data) {
  return request({ url: `/knowledge-graph/manage/edges/${id}`, method: 'put', data })
}
export function updateGraphEdgeStatus(id, status) {
  return request({ url: `/knowledge-graph/manage/edges/${id}/status`, method: 'put', data: { status } })
}
export function deleteGraphEdge(id) {
  return request({ url: `/knowledge-graph/manage/edges/${id}`, method: 'delete' })
}

// ========== AI交互日志审计（系统管理员） ==========
export function getAiChatLogs(params) {
  return request({ url: '/ai/logs', method: 'get', params })
}
export function getAiChatLogDetail(id) {
  return request({ url: `/ai/logs/${id}`, method: 'get' })
}
export function exportAiChatLogs(params) {
  return request({ url: '/ai/logs/export', method: 'get', params, responseType: 'blob' })
}
