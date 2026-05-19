import request from './request'

// 登录
export function login(data) {
  return request({
    url: '/auth/login',
    method: 'post',
    data
  })
}

// 注册
export function register(data) {
  return request({
    url: '/auth/register',
    method: 'post',
    data
  })
}

// 获取用户信息
export function getUserInfo() {
  return request({
    url: '/auth/info',
    method: 'get'
  })
}

// 当前用户菜单（角色权限，供自定义角色侧栏）
export function getMenus() {
  return request({
    url: '/auth/menus',
    method: 'get'
  })
}

// 修改密码
export function changePassword(data) {
  return request({
    url: '/auth/password',
    method: 'post',
    data
  })
}

// 更新个人信息
export function updateProfile(data) {
  return request({
    url: '/auth/profile',
    method: 'put',
    data
  })
}

// 通过手机号重置密码
export function resetPasswordByPhone(data) {
  return request({
    url: '/auth/reset-password',
    method: 'post',
    data
  })
}
