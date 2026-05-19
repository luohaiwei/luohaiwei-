import request from './request'

// 获取所有配置
export const getAllConfigs = () => request({ url: '/sys/config/list', method: 'get' })

// 按分组获取配置
export const getConfigsByGroup = (group) => request({ url: `/sys/config/group/${group}`, method: 'get' })

// 批量保存配置
export const saveConfigs = (configs) => request({ url: '/sys/config/save', method: 'post', data: configs })

// 获取单个配置值
export const getConfigValue = (key) => request({ url: '/sys/config/value', method: 'get', params: { key } })

// 手动备份
export const backupDatabase = () => request({ url: '/sys/config/backup', method: 'post' })

// 获取备份列表
export const getBackupList = () => request({ url: '/sys/config/backup/list', method: 'get' })

// 下载备份
export const downloadBackup = (path) => request({ url: '/sys/config/backup/download', method: 'get', params: { path }, responseType: 'blob' })

// 从备份恢复
export const restoreDatabase = (path) => request({ url: '/sys/config/restore', method: 'post', params: { path } })

// 上传并恢复
export const uploadAndRestore = (formData) => request({ url: '/sys/config/restore/upload', method: 'post', data: formData, headers: { 'Content-Type': 'multipart/form-data' } })

// 获取自动备份配置
export const getAutoBackupConfig = () => request({ url: '/sys/config/backup/auto-config', method: 'get' })

// 保存自动备份配置
export const saveAutoBackupConfig = (data) => request({ url: '/sys/config/backup/auto-config', method: 'put', data })

// 删除备份文件
export const deleteBackup = (path) => request({ url: '/sys/config/backup/delete', method: 'delete', params: { path } })
