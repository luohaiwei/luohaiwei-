import request from './request'

// 获取分类列表
export function getCategoryList(params) {
  return request({
    url: '/category/list',
    method: 'get',
    params
  })
}

// 获取所有分类（下拉用）
export function getAllCategories() {
  return request({
    url: '/category/all',
    method: 'get'
  })
}

// 添加分类
export function addCategory(data) {
  return request({
    url: '/category',
    method: 'post',
    data
  })
}

// 更新分类
export function updateCategory(data) {
  return request({
    url: '/category',
    method: 'put',
    data
  })
}

// 删除分类
export function deleteCategory(id) {
  return request({
    url: `/category/${id}`,
    method: 'delete'
  })
}
