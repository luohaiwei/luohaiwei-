import request from './request'

// ========== 预约全局规则（系统管理员）—— 实际生效的预约规则配置 ==========

/**
 * 获取预约全局规则
 * 包含：基础规则、按角色限额、工作时段
 */
export function getGlobalBookingRules() {
  return request({ url: '/booking-rule/global', method: 'get', skipErrorNotify: true })
}

/**
 * 保存预约全局规则
 * @param {Object} data - { type: 'basic'|'roleLimits'|'timeRange', data: {...} }
 */
export function saveGlobalBookingRules(data) {
  return request({ url: '/booking-rule/global', method: 'put', data })
}
