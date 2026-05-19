import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import {
  login as loginApi,
  getUserInfo as getUserInfoApi,
  getMenus as getMenusApi,
  register as registerApi,
  changePassword as changePasswordApi
} from '../api/auth'
import { isBuiltinUserType } from '../constants/userRoles'
import { resolveMenuRoute } from '../utils/menuRouteMap'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const _parsedUser = JSON.parse(localStorage.getItem('userInfo') || '{}')
  if (!Array.isArray(_parsedUser.permissions)) _parsedUser.permissions = []
  const userInfo = ref(_parsedUser)
  const userType = ref(localStorage.getItem('userType') || '')

  const dynamicMenusLoaded = ref(false)
  const dynamicMenus = ref([])
  const allowedRoutePaths = ref(new Set())

  const isCustomRole = computed(() => {
    const t = userInfo.value?.userType || userType.value
    return !!t && !isBuiltinUserType(t)
  })

  function pathsFromMenus(items) {
    const s = new Set(['/dashboard', '/profile', '/message-center', '/search'])
    for (const it of items) {
      if (it.route) s.add(it.route)
    }
    if (s.has('/device-status')) s.add('/device-status-track')
    return s
  }

  async function loadMenus() {
    const t = userInfo.value?.userType || userType.value
    if (!t || isBuiltinUserType(t)) {
      dynamicMenus.value = []
      allowedRoutePaths.value = new Set()
      dynamicMenusLoaded.value = true
      return
    }
    try {
      const res = await getMenusApi()
      const rows = Array.isArray(res?.list) ? res.list : []
      const seen = new Map()
      const dropped = []
      for (const row of rows) {
        const path = row.path ?? row.PATH
        const code = row.permissionCode ?? row.permission_code
        const route = resolveMenuRoute(path, code)
        if (!route) {
          dropped.push({ path, code, name: row.permissionName ?? row.permission_name ?? '(无名称)' })
          continue
        }
        const title = row.permissionName ?? row.permission_name ?? route
        const sort = row.sort != null ? Number(row.sort) : 0
        const id = row.id ?? row.ID
        const key = route
        if (!seen.has(key) || (seen.get(key).sort > sort)) {
          seen.set(key, {
            route,
            title,
            icon: row.icon || 'Menu',
            sort,
            id,
            parentId: row.parentId ?? row.parent_id
          })
        }
      }
      if (dropped.length > 0) {
        console.warn('[loadMenus] 以下权限无可用前端路由，已过滤：', dropped)
      }
      const items = [...seen.values()].sort((a, b) => {
        if (a.sort !== b.sort) return a.sort - b.sort
        return (a.id || 0) - (b.id || 0)
      })
      dynamicMenus.value = items
      allowedRoutePaths.value = pathsFromMenus(items)
    } catch {
      dynamicMenus.value = []
      allowedRoutePaths.value = new Set(['/dashboard', '/profile'])
    } finally {
      dynamicMenusLoaded.value = true
    }
  }

  async function login(data) {
    const res = await loginApi(data)
    token.value = res.token
    userInfo.value = {
      id: res.userId,
      userId: res.userId,
      username: res.username,
      realName: res.realName,
      userType: res.userType,
      permissions: Array.isArray(res.permissions) ? res.permissions : []
    }
    userType.value = res.userType
    localStorage.setItem('token', res.token)
    localStorage.setItem('userInfo', JSON.stringify(userInfo.value))
    localStorage.setItem('userType', res.userType)
    dynamicMenusLoaded.value = false
    await loadMenus()
  }

  async function getUserInfo() {
    const res = await getUserInfoApi()
    if (!Array.isArray(res.permissions)) res.permissions = []
    userInfo.value = res
    userType.value = res.userType
    localStorage.setItem('userInfo', JSON.stringify(res))
    localStorage.setItem('userType', res.userType)
    dynamicMenusLoaded.value = false
    await loadMenus()
  }

  async function register(data) {
    return await registerApi(data)
  }

  async function changePassword(data) {
    return await changePasswordApi(data)
  }

  function logout() {
    token.value = ''
    userInfo.value = {}
    userType.value = ''
    dynamicMenus.value = []
    allowedRoutePaths.value = new Set()
    dynamicMenusLoaded.value = false
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
    localStorage.removeItem('userType')
  }

  function canCustomAccessPath(path) {
    const p = (path || '').replace(/\/$/, '') || '/'
    if (['/dashboard', '/profile', '/message-center', '/search'].includes(p)) return true
    if (/^\/device\/\d+$/.test(p) && allowedRoutePaths.value.has('/device')) return true
    return allowedRoutePaths.value.has(p)
  }

  function hasPermission(code) {
    if (!code) return false
    const list = userInfo.value?.permissions
    return Array.isArray(list) && list.includes(code)
  }

  return {
    token,
    userInfo,
    userType,
    dynamicMenus,
    allowedRoutePaths,
    dynamicMenusLoaded,
    isCustomRole,
    loadMenus,
    canCustomAccessPath,
    hasPermission,
    login,
    getUserInfo,
    register,
    changePassword,
    logout
  }
})
