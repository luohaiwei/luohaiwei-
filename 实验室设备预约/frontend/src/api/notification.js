import request from './request'

// 获取我的消息列表
export function getMyMessages(params) {
  return request({
    url: '/notification/my',
    method: 'get',
    params
  })
}

// 获取未读消息数量
export function getUnreadCount() {
  return request({
    url: '/notification/unread-count',
    method: 'get',
    skipErrorNotify: true
  })
}

// 标记单条消息为已读
export function markAsRead(id) {
  return request({
    url: `/notification/read/${id}`,
    method: 'put'
  })
}

// 全部标记为已读
export function markAllAsRead() {
  return request({
    url: '/notification/read-all',
    method: 'put'
  })
}

// 删除消息
export function deleteMessage(id) {
  return request({
    url: `/notification/${id}`,
    method: 'delete'
  })
}
