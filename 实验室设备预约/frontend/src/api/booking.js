import request from './request';

// 创建预约
export function createBooking(data) {
  return request({
    url: '/booking',
    method: 'post',
    data
  });
}

// 我的预约列表
export function getMyBookings(params) {
  return request({
    url: '/booking/my',
    method: 'get',
    params
  });
}

// 预约详情（后端返回 { success, data }，此处统一解包为预约对象）
export function getBookingDetail(id) {
  return request({
    url: `/booking/${id}`,
    method: 'get'
  }).then((res) => {
    if (res && typeof res === 'object' && res.data !== undefined && res.success !== false) {
      return res.data;
    }
    return res;
  });
}

// 取消预约（可带原因）
export function cancelBooking(id, data) {
  return request({
    url: `/booking/${id}`,
    method: 'delete',
    params: data
  });
}

// 管理员取消他人预约（固定路径 PUT，避免部分环境下 DELETE /{id}/admin-cancel 返回 404）
export function adminCancelBooking(id, data) {
  return request({
    url: '/booking/admin/cancel',
    method: 'put',
    params: { id, ...(data && typeof data === 'object' ? data : {}) }
  });
}

// 使用完成（已通过 → 已完成，释放设备）
export function completeBookingUse(id) {
  return request({
    url: `/booking/${id}/complete-use`,
    method: 'put'
  });
}

// 审核预约
export function auditBooking(id, data) {
  return request({
    url: `/booking/${id}/audit`,
    method: 'put',
    params: data
  });
}

// 待审核列表
export function getPendingAudits(params) {
  return request({
    url: '/booking/pending',
    method: 'get',
    params
  });
}

// 智能冲突检测（简单版）
export function checkConflict(params) {
  return request({
    url: '/booking/check-conflict',
    method: 'get',
    params
  });
}

// 智能冲突检测（增强版，返回详细冲突信息和替代设备建议）
export function checkConflictDetail(params) {
  return request({
    url: '/booking/check-conflict-detail',
    method: 'get',
    params
  });
}

// 申请设备替换
export function applyDeviceReplace(id, data) {
  return request({
    url: `/booking/${id}/replace-device`,
    method: 'put',
    params: data
  });
}

// 获取可替换设备列表
export function getReplaceableDevices(id) {
  return request({
    url: `/booking/${id}/replaceable-devices`,
    method: 'get'
  });
}

// ========== 预约调度管理（实验室管理员） ==========
// 预约调整（管理员直接调整）
export function adjustBooking(id, data) {
  return request({ url: `/booking/${id}/adjust`, method: 'put', params: data });
}

// 学生直接调整预约时间（适用于待审核的预约）
export function studentAdjustBooking(id, data) {
  return request({ url: `/booking/${id}/student-adjust`, method: 'put', params: data });
}

// 批量审核预约
export function batchAuditBooking(data) {
  return request({ url: '/booking/batch-audit', method: 'put', data });
}

// ========== 预约全局列表（系统管理员） ==========
export function getBookingList(params) {
  return request({ url: '/booking/global-list', method: 'get', params });
}
export function forceCloseBooking(id) {
  return request({ url: `/booking/${id}/force-close`, method: 'put' });
}
export function adminCompleteBooking(id) {
  return request({ url: '/booking/admin/complete', method: 'put', params: { id } });
}
export function adminNoShowBooking(id) {
  return request({ url: '/booking/admin/no-show', method: 'put', params: { id } });
}
export function exportBookingList(params) {
  return request({ url: '/booking/export', method: 'get', params, responseType: 'blob' });
}

// ========== 签到签退功能 ==========
// 签到
export function checkInBooking(id) {
  return request({
    url: `/booking/${id}/check-in`,
    method: 'put'
  });
}

// 签退
export function checkOutBooking(id, evaluation) {
  return request({
    url: `/booking/${id}/check-out`,
    method: 'put',
    data: { evaluation }
  });
}
