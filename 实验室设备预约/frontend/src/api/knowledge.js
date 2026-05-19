import request from './request'

// 知识库管理列表
export const getKnowledgeList = (params) => request({ url: '/ai/manage/list', method: 'get', params })

// 获取知识详情
export const getKnowledge = (id) => request({ url: `/ai/manage/${id}`, method: 'get' })

// 新增知识
export const addKnowledge = (data) => request({ url: '/ai/manage', method: 'post', data })

// 更新知识
export const updateKnowledge = (id, data) => request({ url: `/ai/manage/${id}`, method: 'put', data })

// 删除知识
export const deleteKnowledge = (id) => request({ url: `/ai/manage/${id}`, method: 'delete' })
