import request from './request'

// 创建年级
export function createGrade(gradeData) {
  return request({
    url: '/grade/create',
    method: 'post',
    data: gradeData
  })
}

// 获取所有年级
export function getAllGrades() {
  return request({
    url: '/grade/list',
    method: 'get'
  })
}

// 导入课程成绩
export function importGrades(formData) {
  return request({
    url: '/grade/import',
    method: 'post',
    data: formData,
    timeout: 60000,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 查询课程成绩导入记录
export function getGradeRecords(params) {
  return request({
    url: '/grade/records',
    method: 'get',
    params
  })
}

// 删除年级
export function deleteGrade(id) {
  return request({
    url: `/grade/delete/${id}`,
    method: 'delete'
  })
}

// 按名称删除年级（仅限手动添加的年级）
export function deleteGradeByName(gradeName) {
  return request({
    url: '/grade/delete-by-name',
    method: 'delete',
    params: { gradeName }
  })
}
