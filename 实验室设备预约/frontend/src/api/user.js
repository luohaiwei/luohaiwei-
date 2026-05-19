import request from './request';

// 用户列表
export function getUserList(params) {
  return request({
    url: '/user/list',
    method: 'get',
    params
  });
}

// 新增用户
export function addUser(data) {
  return request({
    url: '/user',
    method: 'post',
    data
  });
}

// 更新用户
export function updateUser(data) {
  return request({
    url: '/user',
    method: 'put',
    data
  });
}

// 删除用户
export function deleteUser(id) {
  return request({
    url: `/user/${id}`,
    method: 'delete'
  });
}

// 分配角色
export function assignRole(id, userType) {
  return request({
    url: `/user/${id}/role`,
    method: 'put',
    params: { userType }
  });
}

// 按类型获取用户（用于分配维护人员等）
export function getUsersByType(userType) {
  return request({
    url: '/user/by-type',
    method: 'get',
    params: { userType }
  });
}

// 启用/禁用
export function updateUserStatus(id, status) {
  return request({
    url: `/user/${id}/status`,
    method: 'put',
    params: { status }
  });
}

// 管理员重置用户密码为系统默认初始密码
export function resetUserPassword(id) {
  return request({
    url: `/user/${id}/reset-password`,
    method: 'put'
  });
}

// 管理员清零用户爽约次数（解除禁止预约状态）
export function resetMissedCount(id) {
  return request({
    url: '/user/admin/reset-missed-count',
    method: 'put',
    params: { id }
  });
}

// 导出用户列表（失败时由页面统一提示，避免与 axios 拦截器各弹一次）
export function exportUserList(params) {
  return request({
    url: '/user/export',
    method: 'get',
    params,
    responseType: 'blob',
    skipErrorNotify: true
  });
}

// 批量删除用户
export function batchDeleteUsers(ids) {
  return request({ url: '/user/batch', method: 'delete', data: { ids } });
}

// ========== 角色管理 ==========
export function getRoleList(params) {
  return request({ url: '/role/list', method: 'get', params, skipErrorNotify: true });
}
export function addRole(data) {
  return request({ url: '/role', method: 'post', data });
}
export function updateRole(data) {
  return request({ url: '/role', method: 'put', data });
}
export function deleteRole(id) {
  return request({ url: `/role/${id}`, method: 'delete' });
}
export function getRolePermissions(roleId) {
  return request({ url: `/role/${roleId}/permissions`, method: 'get' });
}
export function saveRolePermissions(roleId, permIds) {
  return request({ url: `/role/${roleId}/permissions`, method: 'put', data: { permIds } });
}

// ========== 权限树 ==========
export function getPermissionTree() {
  return request({ url: '/permission/tree', method: 'get' });
}

// ========== 数据权限 ==========
/** 当前登录用户合并后的数据权限（与后端 DataScopeService 一致，用于按钮显隐） */
export function getMyDataScope() {
  return request({ url: '/permission/my-data-scope', method: 'get' });
}
export function getDataScope(roleId = 0) {
  return request({ url: '/permission/data-scope', method: 'get', params: { roleId } });
}
export function saveDataScope(roleId = 0, data) {
  return request({ url: '/permission/data-scope', method: 'put', params: { roleId }, data });
}
export function getPermLabList() {
  // 学生等角色可能无接口权限：须静默失败，否则 axios 拦截器会先弹出「没有权限」
  return request({ url: '/permission/lab-list', method: 'get', skipErrorNotify: true });
}

// ========== 实验室列表 ==========
export function getLabList() {
  return request({ url: '/laboratory/list', method: 'get', params: { pageNum: 1, pageSize: 1000 } });
}
