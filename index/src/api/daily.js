
import request from './request'

// 三个模块对应的后端路径
const URL = {
    academic: 'academic-events',
    daily: 'daily-activities',
    honor: 'honors'
}

// 工具：把 Date 转成 'yyyy-MM-dd'
function d2str(d) {
    if (!d) return ''
    const date = (d instanceof Date) ? d : new Date(d)
    const y = date.getFullYear()
    const m = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    return `${y}-${m}-${day}`
}

// 工具：不同模块的 payload 规范化（后端是 LocalDate / List<String>）
function normalize(moduleKey, form) {
    const p = { ...form }
    if (p.startDate) p.startDate = d2str(p.startDate)
    if (p.endDate) p.endDate = d2str(p.endDate)
    if (p.awardDate) p.awardDate = d2str(p.awardDate)
    if (moduleKey === 'honor' && typeof p.tags === 'string') {
        p.tags = p.tags.split(',').map(s => s.trim()).filter(Boolean)
    }
    return p
}

// 列表（支持搜索/状态/分页）
export function listItems(moduleKey, { studentId, keyword, status, page = 1, size = 10 } = {}) {
    const url = `/${URL[moduleKey]}/list`
    const params = {
        // honor 用 userId，另外两个用 studentId
        ...(moduleKey === 'honor' ? { userId: studentId } : { studentId }),
        keyword,
        status,
        page,
        size
    }
    return request.get(url, { params })
}

// 新增
export function createItem(moduleKey, form) {
    const url = `/${URL[moduleKey]}/create`
    return request.post(url, normalize(moduleKey, form))
}

// 详情
export function getDetail(moduleKey, id) {
    const url = `/${URL[moduleKey]}/${id}`
    return request.get(url)
}

// 审核接口 (Admin)
export function auditItem(moduleKey, data) {
    const url = `/${URL[moduleKey]}/audit`
    return request.post(url, data)
}

// 统计卡片
export function getStudentStats(studentId) {
    return request.get('/api/student-stats', { params: { studentId } }) // -> {data:{...}}
}

/** ========= 日常任务相关 (Admin/Student) ========= */

// 获取所有任务 (Admin)
export function getAllDailyTasks() {
    return request.get('/daily-tasks/all')
}

// 获取激活的任务 (Student)
export function getActiveDailyTasks() {
    return request.get('/daily-tasks/active')
}

// 创建任务 (Admin)
export function createDailyTask(data) {
    return request.post('/daily-tasks/create', data)
}

// 删除任务 (Admin)
export function deleteDailyTask(id) {
    return request.delete(`/daily-tasks/delete/${id}`)
}

// 提交任务 (Student)
export function submitDailyTask(data) {
    return request.post('/daily-tasks/submit', data)
}

// 获取完成情况统计 (Admin)
export function getDailyTaskStats(taskId) {
    return request.get(`/daily-tasks/stats/${taskId}`)
}

// 导出完成情况 (Admin)
export function exportDailyTaskExcel(taskId) {
    return request({
        url: `/daily-tasks/export/${taskId}`,
        method: 'get',
        responseType: 'blob'
    })
}

// 获取自己的提交记录 (Student)
export function getMyDailyTaskSubmission(taskId) {
    return request.get(`/daily-tasks/my-submission/${taskId}`)
}

// 获取任务完成度分析 (Admin)
export function getTaskCompletionAnalysis(grade) {
    const params = grade ? { grade } : {}
    return request.get('/daily-tasks/completion-analysis', { params })
}

// 导出任务完成度分析 (Admin)
export function exportTaskCompletionAnalysis(grade) {
    const params = grade ? { grade } : {}
    return request({
        url: '/daily-tasks/completion-analysis/export',
        method: 'get',
        params,
        responseType: 'blob'
    })
}

// 获取学生未完成的任务列表 (Admin)
export function getStudentIncompleteTasks(studentId) {
    return request.get(`/daily-tasks/student-incomplete-tasks/${studentId}`)
}

/** ========= 活动管理相关 (Admin) ========= */

// 创建活动
export function createActivity(data) {
    return request.post('/activities/create', data)
}

// 获取所有活动
export function getAllActivities() {
    return request.get('/activities/list')
}

// 根据ID获取活动
export function getActivityById(id) {
    return request.get(`/activities/${id}`)
}

// 删除活动
export function deleteActivity(id) {
    return request.delete(`/activities/delete/${id}`)
}

// 导入学生并自动匹配任务
export function importStudentsAndMatchTasks(activityId, students) {
    return request.post(`/activities/${activityId}/import-students`, { students })
}