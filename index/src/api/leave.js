import request from './request'

// 学生提交请假申请
export function createLeaveRequest(data) {
  return request({
    url: '/leave/create',
    method: 'post',
    data
  })
}

// 学生获取自己的请假记录
export function getMyLeaveRequests(studentId) {
  return request({
    url: '/leave/my-leaves',
    method: 'get',
    params: { studentId }
  })
}

// 学生销假
export function checkIn(id, checkInComment, latitude, longitude, address) {
  return request({
    url: `/leave/check-in/${id}`,
    method: 'post',
    data: { 
      checkInComment,
      latitude,
      longitude,
      address
    }
  })
}

// 管理员获取所有待审核的请假
export function getPendingLeaveRequests() {
  return request({
    url: '/leave/pending',
    method: 'get'
  })
}

// 管理员获取所有请假记录
export function getAllLeaveRequests() {
  return request({
    url: '/leave/all',
    method: 'get'
  })
}

// 管理员审核请假
export function auditLeaveRequest(id, auditStatus, auditComment) {
  return request({
    url: `/leave/audit/${id}`,
    method: 'post',
    data: {
      auditStatus,
      auditComment
    }
  })
}

// 管理员更新请假状态
export function updateLeaveStatus(id, status) {
  return request({
    url: `/leave/update-status/${id}`,
    method: 'post',
    data: { status }
  })
}

// 上传文件（请假证据）
export function uploadLeaveEvidence(files) {
  const formData = new FormData()
  files.forEach(file => {
    formData.append('file', file)
  })
  return request({
    url: '/upload',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}
