const PREVIEW_FLAG = 'frontend_preview_mode'
const PREVIEW_USER_TYPE = 'frontend_preview_user_type'

export const previewStudent = {
  id: 'stu-preview-001',
  userId: 'stu-preview-001',
  studentId: '10240001',
  name: '张同学',
  userType: 'student',
  major: '软件工程',
  grade: '2024级',
  studentClass: '软工2401',
  className: '软工2401',
  email: 'student@example.com',
  phone: '13800000001',
  dormitory: '南苑 3 栋 502'
}

export const previewAdmin = {
  id: 'admin-preview-001',
  userId: 'admin-preview-001',
  adminId: 'admin-preview-001',
  username: 'admin',
  name: '管理员',
  realName: '管理员',
  userType: 'admin',
  role: 'super_admin',
  email: 'admin@example.com',
  phone: '13800000002'
}

export function isPreviewMode() {
  return localStorage.getItem(PREVIEW_FLAG) === '1'
}

export function enablePreviewMode(userType = 'student') {
  const user = userType === 'admin' ? previewAdmin : previewStudent

  localStorage.setItem(PREVIEW_FLAG, '1')
  localStorage.setItem(PREVIEW_USER_TYPE, user.userType)
  localStorage.setItem('token', `preview-token-${user.userType}`)
  localStorage.setItem('token_expire', String(Date.now() + 24 * 60 * 60 * 1000))
  localStorage.setItem('userId', user.studentId || user.adminId || user.id)
  localStorage.setItem('userInfo', JSON.stringify(user))

  return user
}

export function disablePreviewMode() {
  localStorage.removeItem(PREVIEW_FLAG)
  localStorage.removeItem(PREVIEW_USER_TYPE)
  localStorage.removeItem('token')
  localStorage.removeItem('token_expire')
  localStorage.removeItem('userId')
  localStorage.removeItem('userInfo')
}

export function getPreviewUserType(path = '') {
  if (path.startsWith('/admin')) return 'admin'
  if (path && path !== '/preview') return 'student'
  return localStorage.getItem(PREVIEW_USER_TYPE) || 'student'
}

export function getPreviewUser(path = '') {
  return getPreviewUserType(path) === 'admin' ? previewAdmin : previewStudent
}

export function ensurePreviewSession(path = '', store) {
  const user = enablePreviewMode(getPreviewUserType(path))
  if (store) {
    store.commit('setUser', user)
    store.commit('setToken', localStorage.getItem('token'))
  }
  return user
}
