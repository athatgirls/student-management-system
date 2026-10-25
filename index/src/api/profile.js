import request from './request'

// 获取当前用户个人信息（身份从登录态解析，无需传学生标识）
export function getCurrentUserProfile() {
  return request({
    url: '/student/profile',
    method: 'get'
  })
}

// 更新个人信息
export function updateUserProfile(profileData) {
  return request({
    url: '/student/profile/update',
    method: 'post',
    data: profileData
  })
}

// 获取学习记录
export function getStudyRecords() {
  return request({
    url: '/student/study-records',
    method: 'get'
  })
}
