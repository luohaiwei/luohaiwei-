/** 与路由 meta.roles、后端 userType 一致的内置身份 */
export const BUILTIN_USER_TYPES = [
  'SYSTEM_ADMIN',
  'LAB_ADMIN',
  'TEACHER',
  'STUDENT',
  'MAINTAINER'
]

export function isBuiltinUserType(userType) {
  return !!userType && BUILTIN_USER_TYPES.includes(userType)
}
