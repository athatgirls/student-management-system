import request from './request'

// 学生登录
export function studentLogin(account, password) {
  return request({
    url: '/student/login',
    method: 'post',
    data: { account, password }
  })
}

// 获取学生列表
export function getStudentList(params) {
  return request({
    url: '/student/list',
    method: 'get',
    params
  })
}

// 根据ID获取学生
export function getStudentById(id) {
  return request({
    url: `/student/${id}`,
    method: 'get'
  })
}

// 创建学生
export function createStudent(studentData) {
  return request({
    url: '/student/create',
    method: 'post',
    data: studentData
  })
}

// 更新学生
export function updateStudent(studentData) {
  return request({
    url: '/student/update',
    method: 'put',
    data: studentData
  })
}

// 删除学生
export function deleteStudent(id) {
  return request({
    url: `/student/delete/${id}`,
    method: 'delete'
  })
}

// 获取当前用户信息（用于页面刷新后恢复）
export function getCurrentUser() {
  return request({
    url: '/student/current-user',
    method: 'get'
  })
}

// 获取个人信息（自动获取当前用户）
export function getProfile() {
  return request({
    url: '/student/profile',
    method: 'get'
  })
}

// 更新个人信息
export function updateProfile(data) {
  return request({
    url: '/student/profile/update',
    method: 'post',
    data
  })
}

// 获取学习记录
export function getStudyRecords() {
  return request({
    url: '/student/study-records',
    method: 'get'
  })
}

// 刷新token
export function refreshToken() {
  return request({
    url: '/student/refresh-token',
    method: 'post'
  })
}

// 导入学生
export function importStudents(formData) {
  return request({
    url: '/student/import',
    method: 'post',
    data: formData,
    timeout: 60000, // 批量导入可能耗时较长，增加超时时间到60秒
    headers: {
      // 清除实例的 JSON 默认头，让浏览器自动添加 multipart boundary。
      'Content-Type': null
    }
  })
}

// 批量删除学生
export function batchDeleteStudents(ids) {
  return request({
    url: '/student/batch-delete',
    method: 'delete',
    data: { ids }
  })
}

// 获取所有年级列表
export function getAllGrades() {
  return request({
    url: '/student/grades',
    method: 'get'
  })
}

// 批量修改状态
export function batchUpdateStatus(data) {
  return request({
    url: '/student/batch-update-status',
    method: 'put',
    data
  })
}

// 批量修改专业
export function batchUpdateMajor(data) {
  return request({
    url: '/student/batch-update-major',
    method: 'put',
    data
  })
}

// 获取最近活动
export function getRecentActivities() {
  return request({
    url: '/student/recent-activities',
    method: 'get'
  })
}

// 管理员重置学生密码（Hbut_加学号后六位，首次登录必须修改）
export function resetStudentPassword(studentId, newPassword = null) {
  return request({
    url: '/student/reset-password',
    method: 'put',
    data: { studentId, newPassword }
  })
}

// 学生修改自己的密码
export function changePassword(oldPassword, newPassword) {
  return request({
    url: '/student/change-password',
    method: 'put',
    data: { oldPassword, newPassword }
  })
}

// 修改初始密码（不需要登录）
export function changeInitialPassword(studentId, initialPassword, newPassword) {
  return request({
    url: '/student/change-initial-password',
    method: 'post',
    data: { studentId, initialPassword, newPassword }
  })
}
