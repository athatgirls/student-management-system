import request from './request'

// 获取所有专业
export function getMajors() {
  return request({
    url: '/major/list',
    method: 'get'
  })
}

// 创建专业
export function createMajor(majorData) {
  return request({
    url: '/major/create',
    method: 'post',
    data: majorData
  })
}

// 按名称删除专业
export function deleteMajorByName(majorName) {
  return request({
    url: '/major/delete-by-name',
    method: 'delete',
    params: { majorName }
  })
}
