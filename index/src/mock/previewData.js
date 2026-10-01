import { previewAdmin, previewStudent } from '@/utils/previewMode'

const today = new Date()
const toDate = (offset = 0) => {
  const date = new Date(today)
  date.setDate(today.getDate() + offset)
  return date.toISOString().slice(0, 10)
}

const toDateTime = (offset = 0) => `${toDate(offset)} 09:30`

const success = (data = null, msg = '预览数据加载成功') => ({
  code: 200,
  msg,
  message: msg,
  data
})

const defaultPreviewGrades = ['2024级', '2025级', '2026级']
const grades = [...defaultPreviewGrades]

const normalizeGradeName = (name) => {
  const value = String(name || '').trim()
  return /^\d{4}$/.test(value) ? `${value}级` : value
}

const parseBody = (data) => {
  if (!data) return {}
  if (typeof data === 'string') {
    try {
      return JSON.parse(data)
    } catch {
      return {}
    }
  }
  return data
}

const defaultGradeRecords = [
  { id: 'grade-001', studentId: previewStudent.studentId, studentName: previewStudent.name, courseName: 'Web 前端开发', score: 92, credit: 3, semester: '2025-2026-1', status: '已通过' },
  { id: 'grade-002', studentId: previewStudent.studentId, studentName: previewStudent.name, courseName: '数据库系统', score: 88, credit: 3, semester: '2025-2026-1', status: '已通过' }
]

const getPreviewGradeRecords = () => {
  try {
    const stored = JSON.parse(localStorage.getItem('preview_grade_records') || '[]')
    return stored.length ? stored : defaultGradeRecords
  } catch {
    return defaultGradeRecords
  }
}

const students = [
  {
    id: 'stu-preview-001',
    studentId: '10240001',
    name: '张同学',
    gender: '男',
    major: '软件工程',
    grade: '2024级',
    studentClass: '软工2401',
    className: '软工2401',
    dormitory: '南苑 3 栋 502',
    phone: '13800000001',
    email: 'student@example.com',
    status: '正常'
  },
  {
    id: 'stu-preview-002',
    studentId: '10240002',
    name: '李同学',
    gender: '女',
    major: '计算机科学与技术',
    grade: '2025级',
    studentClass: '计科2502',
    className: '计科2502',
    dormitory: '北苑 2 栋 318',
    phone: '13800000003',
    email: 'student2@example.com',
    status: '正常'
  },
  {
    id: 'stu-preview-003',
    studentId: '10240003',
    name: '王同学',
    gender: '男',
    major: '人工智能',
    grade: '2026级',
    studentClass: '人工智能2601',
    className: '人工智能2601',
    dormitory: '东苑 5 栋 616',
    phone: '13800000004',
    email: 'student3@example.com',
    status: '正常'
  }
]

const majors = [...new Set(students.map(student => student.major))].sort()

const dailyTasks = [
  {
    id: 'task-001',
    title: '提交本周学习总结',
    content: '请填写本周课程学习情况和下周计划。',
    taskCategory: 'normal',
    active: true,
    completed: false,
    deadline: toDate(3),
    createTime: toDateTime(-1),
    fields: [
      { fieldName: '本周完成内容', fieldType: 'textarea', required: true },
      { fieldName: '下周计划', fieldType: 'textarea', required: true }
    ]
  },
  {
    id: 'task-002',
    title: '报名参加学院志愿活动',
    content: '面向全院学生招募志愿者。',
    taskCategory: 'registration',
    active: true,
    completed: false,
    deadline: toDate(5),
    createTime: toDateTime(-2),
    maxParticipants: 30,
    currentParticipants: 12,
    fields: [
      { fieldName: '可参加时间', fieldType: 'select', options: ['周六上午', '周六下午', '周日全天'], required: true },
      { fieldName: '联系电话', fieldType: 'input', required: true }
    ]
  }
]

const leaveRequests = [
  {
    id: 'leave-001',
    studentId: previewStudent.studentId,
    studentName: previewStudent.name,
    reason: '参加校外技术交流活动',
    type: '事假',
    startTime: toDate(-1),
    endTime: toDate(1),
    startDate: toDate(-1),
    endDate: toDate(1),
    days: 3,
    evidenceUrls: ['/uploads/preview-leave.pdf'],
    status: 'approved',
    auditStatus: 'approved',
    createTime: toDateTime(-3)
  }
]

const activityItems = [
  {
    id: 'daily-001',
    title: '班级团日活动',
    studentId: previewStudent.studentId,
    studentName: previewStudent.name,
    startDate: toDate(-5),
    endDate: toDate(-5),
    location: '教学楼 A101',
    status: 'pending',
    auditStatus: 'pending',
    description: '围绕专业学习与生涯规划开展交流。'
  }
]

const academicEvents = [
  {
    id: 'academic-001',
    title: '前端工程化讲座',
    studentId: previewStudent.studentId,
    studentName: previewStudent.name,
    startDate: toDate(-10),
    endDate: toDate(-10),
    location: '学术报告厅',
    status: 'approved',
    auditStatus: 'approved',
    description: '参与 Vue 与工程化实践专题讲座。'
  }
]

const honors = [
  {
    id: 'honor-001',
    title: '校级优秀学生',
    studentId: previewStudent.studentId,
    studentName: previewStudent.name,
    awardDate: toDate(-30),
    level: '校级',
    tags: ['综合素质', '学习优秀'],
    status: 'approved',
    auditStatus: 'approved'
  }
]

const internships = [
  {
    id: 'intern-001',
    userId: previewStudent.id,
    studentId: previewStudent.studentId,
    studentName: previewStudent.name,
    studentClass: previewStudent.studentClass,
    major: previewStudent.major,
    grade: previewStudent.grade,
    type: '实习',
    company: '星河软件科技有限公司',
    companyType: '民营企业',
    industry: '互联网/软件',
    position: '前端开发实习生',
    workLocation: '武汉',
    workType: '线下',
    salary: '4000',
    salaryRange: '3k-5k',
    startDate: toDate(-20),
    endDate: toDate(40),
    status: '实习中',
    approvalStatus: 'approved',
    technologyStack: ['Vue', 'Element Plus', 'Axios'],
    satisfactionScore: 4,
    contactPerson: '陈经理',
    contactPhone: '13800001234'
  },
  {
    id: 'intern-002',
    userId: previewStudent.id,
    studentId: previewStudent.studentId,
    studentName: previewStudent.name,
    studentClass: previewStudent.studentClass,
    major: previewStudent.major,
    grade: previewStudent.grade,
    type: '就业',
    company: '云帆智能科技有限公司',
    companyType: '高新技术企业',
    industry: '人工智能',
    position: '产品助理',
    workLocation: '杭州',
    workType: '线下',
    salary: '9000',
    salaryRange: '8k-10k',
    startDate: toDate(30),
    status: '待入职',
    approvalStatus: 'pending',
    technologyStack: ['数据分析', '原型设计'],
    satisfactionScore: 5
  }
]

const competitionDicts = [
  {
    id: 'dict-001',
    competitionName: '中国国际大学生创新大赛',
    competitionLevel: '国家级',
    serialNumber: 'A001',
    active: true
  },
  {
    id: 'dict-002',
    competitionName: '蓝桥杯全国软件和信息技术专业人才大赛',
    competitionLevel: '省部级',
    serialNumber: 'A002',
    active: true
  }
]

const competitions = [
  {
    id: 'competition-001',
    studentId: previewStudent.studentId,
    studentName: previewStudent.name,
    competitionName: '中国国际大学生创新大赛',
    competitionLevel: '国家级',
    awardLevel: '省级银奖',
    awardDate: toDate(-60),
    auditStatus: 'pending',
    auditComment: ''
  }
]

const papers = [
  {
    id: 'paper-001',
    studentId: previewStudent.studentId,
    studentName: previewStudent.name,
    title: '基于 Vue 的学生管理系统前端设计',
    journal: '软件导刊',
    publishDate: toDate(-90),
    authorRank: '第一作者',
    auditStatus: 'approved'
  }
]

const patents = [
  {
    id: 'patent-001',
    studentId: previewStudent.studentId,
    studentName: previewStudent.name,
    patentName: '一种教学数据可视化系统',
    patentType: '软件著作权',
    patentNumber: '2026SR000001',
    grantDate: toDate(-45),
    auditStatus: 'approved'
  }
]

const projects = [
  {
    id: 'project-001',
    studentId: previewStudent.studentId,
    studentName: previewStudent.name,
    projectName: '学生综合信息管理平台',
    projectLevel: '校级',
    role: '负责人',
    startDate: toDate(-120),
    endDate: toDate(60),
    auditStatus: 'pending'
  }
]

const alumni = {
  id: 'alumni-001',
  student: {
    studentId: previewStudent.studentId,
    name: previewStudent.name
  },
  enrollmentDate: '2020-09-01',
  graduationDate: '2024-06-30',
  workLocation: {
    province: '湖北省',
    city: '武汉市',
    district: '洪山区'
  },
  workField: '互联网/软件',
  workPlace: '星河软件科技有限公司',
  jobType: '前端工程师',
  isPublic: true
}

const partyMembers = [
  {
    id: 'party-member-001',
    studentId: previewStudent.studentId,
    studentName: previewStudent.name,
    branch: '软件工程学生党支部',
    joinDate: toDate(-300),
    isRegular: false,
    auditStatus: 'pending',
    status: 'pending'
  }
]

const partyApplications = [
  {
    id: 'party-application-001',
    studentId: previewStudent.studentId,
    studentName: previewStudent.name,
    currentStage: 'activist',
    applyDate: toDate(-180),
    auditStatus: 'approved',
    status: 'approved'
  }
]

const thoughtReports = [
  {
    id: 'report-001',
    studentId: previewStudent.studentId,
    title: '思想汇报示例',
    reportType: '季度汇报',
    submitDate: toDate(-20),
    reportDate: toDate(-20),
    materials: [{ name: '思想汇报.pdf', url: '/uploads/preview-report.pdf' }],
    status: 'pending',
    auditStatus: 'pending'
  }
]

const partyCourses = [
  {
    id: 'course-001',
    studentId: previewStudent.studentId,
    courseName: '入党积极分子培训',
    courseType: '线下课程',
    instructor: '王老师',
    studyDate: toDate(-35),
    status: 'approved',
    auditStatus: 'approved'
  }
]

const volunteerServices = [
  {
    id: 'service-001',
    studentId: previewStudent.studentId,
    serviceName: '社区志愿活动',
    serviceType: '社区服务',
    organization: '学院青年志愿者协会',
    serviceDate: toDate(-12),
    hours: 4,
    status: 'approved',
    auditStatus: 'approved'
  }
]

const admins = [
  previewAdmin,
  {
    id: 'admin-preview-002',
    adminId: 'admin-preview-002',
    username: 'teacher',
    name: '辅导员',
    realName: '辅导员',
    role: 'admin',
    userType: 'admin',
    email: 'teacher@example.com',
    phone: '13800000005'
  }
]

function normalizeUrl(config = {}) {
  const rawUrl = config.url || ''
  try {
    const url = new URL(rawUrl, config.baseURL || window.location.origin)
    return url.pathname
      .replace('/SCSE@hbut/msi', '')
      .replace(/^\/msi/, '') || '/'
  } catch (e) {
    return rawUrl.split('?')[0]
  }
}

function maybeBlob(config) {
  if (config.responseType === 'blob' && typeof Blob !== 'undefined') {
    return new Blob(['前端预览模式导出文件'], { type: 'text/plain;charset=utf-8' })
  }
  return null
}

function findById(list, id) {
  return list.find(item => String(item.id) === String(id)) || list[0] || null
}

function adminDashboard() {
  return {
    code: 200,
    statistics: {
      totalStudents: 186,
      internshipCount: internships.length,
      competitionCount: competitions.length + papers.length + patents.length + projects.length,
      alumniCount: 58
    },
    majorChartData: [
      { name: '软件工程', value: 78 },
      { name: '计算机科学与技术', value: 56 },
      { name: '人工智能', value: 32 },
      { name: '数据科学与大数据技术', value: 20 }
    ],
    recentActivities: [
      { id: 'activity-001', title: '张同学提交竞赛获奖', description: '等待管理员审核', time: toDateTime(-1) },
      { id: 'activity-002', title: '李同学更新实习信息', description: '新增企业实习记录', time: toDateTime(-2) }
    ],
    pendingApprovals: [
      { id: 'approval-001', title: '竞赛获奖审核', description: '中国国际大学生创新大赛', time: toDateTime(-1) },
      { id: 'approval-002', title: '日常活动审核', description: '班级团日活动', time: toDateTime(-2) }
    ],
    systemInfo: {
      onlineUsers: 12,
      todayVisits: 96,
      uptime: '预览模式运行中',
      dbStatus: '正常'
    }
  }
}

function auditStats() {
  const data = {
    competition: { pending: 1 },
    project: { pending: 1 },
    paper: { pending: 0 },
    patent: { pending: 0 },
    daily: { pending: 2 }
  }
  return { code: 200, data, ...data }
}

function internshipStats(path) {
  if (path.endsWith('/status')) {
    return success([{ name: '实习中', value: 1 }, { name: '待入职', value: 1 }])
  }
  if (path.endsWith('/industry')) {
    return success([{ name: '互联网/软件', value: 1 }, { name: '人工智能', value: 1 }])
  }
  if (path.endsWith('/company-type')) {
    return success([{ name: '民营企业', value: 1 }, { name: '高新技术企业', value: 1 }])
  }
  if (path.endsWith('/approval-status')) {
    return success([{ name: '已通过', value: 1 }, { name: '待审核', value: 1 }])
  }
  if (path.endsWith('/technology-stack')) {
    return success([{ name: 'Vue', value: 1 }, { name: 'Element Plus', value: 1 }, { name: '数据分析', value: 1 }])
  }
  if (path.endsWith('/salary-range')) {
    return success([{ name: '3k-5k', value: 1 }, { name: '8k-10k', value: 1 }])
  }
  if (path.endsWith('/satisfaction')) {
    return success([{ name: '4分', value: 1 }, { name: '5分', value: 1 }])
  }
  if (path.endsWith('/employment-trend')) {
    return success([{ month: '2026-01', count: 6 }, { month: '2026-02', count: 9 }, { month: '2026-03', count: 14 }])
  }
  if (path.endsWith('/top-technology-stacks')) {
    return success([{ name: 'Vue', value: 18 }, { name: 'Java', value: 15 }, { name: 'Python', value: 12 }])
  }
  if (path.endsWith('/top-industries')) {
    return success([{ name: '互联网/软件', value: 21 }, { name: '人工智能', value: 9 }])
  }
  return success({
    totalCount: internships.length,
    activeCount: 1,
    pendingCount: 1,
    approvedCount: 1,
    averageSalary: 6500
  })
}

function listResponse(path) {
  if (path.includes('/academic-events')) return success(academicEvents)
  if (path.includes('/daily-activities')) return success(activityItems)
  if (path.includes('/honors')) return success(honors)
  return success([])
}

function partyResponse(path) {
  if (path.includes('/party-member')) return success(partyMembers)
  if (path.includes('/party-application')) {
    if (path.includes('/student/')) return success(partyApplications[0])
    return success(partyApplications)
  }
  if (path.includes('/thought-report')) return success(thoughtReports)
  if (path.includes('/party-course')) return success(partyCourses)
  if (path.includes('/volunteer-service')) return success(volunteerServices)
  return success([])
}

export function createPreviewResponse(config = {}) {
  const path = normalizeUrl(config)
  const blob = maybeBlob(config)
  if (blob) return blob

  if (path === '/student/login') {
    return success({ ...previewStudent, token: 'preview-token-student' }, '学生预览登录成功')
  }
  if (path === '/admin/login') {
    return success({ ...previewAdmin, token: 'preview-token-admin' }, '管理员预览登录成功')
  }
  if (path === '/student/current-user' || path === '/student/profile') return success(previewStudent)
  if (path === '/admin/current-user') return success(previewAdmin)
  if (/^\/admin\/[^/]+$/.test(path)) return success(previewAdmin)
  if (path === '/student/refresh-token' || path === '/admin/refresh-token') {
    return success({ token: 'preview-token-refreshed' }, 'Token刷新成功')
  }
  if (path === '/student/grades' || path === '/grade/list') return success(grades)
  if (path === '/major/list') return success(majors)
  if (path === '/major/create') {
    const majorName = String(parseBody(config.data).majorName || '').trim()
    if (!majorName) return { code: 400, data: null, msg: '请选择专业' }
    if (majors.includes(majorName)) return { code: 400, data: null, msg: '该专业已存在' }
    majors.push(majorName)
    majors.sort()
    return success({ majorName }, '专业添加成功')
  }
  if (path === '/major/delete-by-name') {
    const majorName = String((config.params || {}).majorName || '').trim()
    if (!majorName) return { code: 400, data: null, msg: '请选择专业' }
    const index = majors.indexOf(majorName)
    if (index < 0) return { code: 400, data: null, msg: '该专业不存在或非手动添加，无法删除' }
    majors.splice(index, 1)
    return success(null, '专业已删除')
  }
  if (path === '/grade/create') {
    const name = normalizeGradeName(parseBody(config.data).gradeName)
    if (!name) return { code: 400, data: null, msg: '请选择年级' }
    if (grades.includes(name)) return { code: 400, data: null, msg: '该年级已存在' }
    grades.push(name)
    return success({ gradeName: name }, '年级添加成功')
  }
  if (path === '/grade/delete-by-name') {
    const name = normalizeGradeName((config.params || {}).gradeName)
    if (!name) return { code: 400, data: null, msg: '请选择年级' }
    if (defaultPreviewGrades.includes(name)) return { code: 400, data: null, msg: '默认年级不可删除' }
    const index = grades.indexOf(name)
    if (index < 0) return { code: 400, data: null, msg: '该年级不是手动添加的，无法删除' }
    grades.splice(index, 1)
    return success(null, '年级已删除')
  }
  if (path === '/grade/records') return success(getPreviewGradeRecords())
  if (path === '/grade/import') return success({ imported: true }, '成绩导入成功')
  if (path === '/student/list') return success({ records: students, total: students.length })
  if (path === '/student/batch-update-major') {
    const { ids, major } = parseBody(config.data)
    if (!Array.isArray(ids) || ids.length === 0) return { code: 400, data: null, msg: '请先选择学生' }
    if (!major) return { code: 400, data: null, msg: '请选择专业' }
    const idSet = new Set(ids.map(String))
    const matchedStudents = students.filter(student => idSet.has(String(student.id)))
    matchedStudents.forEach(student => {
      student.major = major
    })
    if (!majors.includes(major)) {
      majors.push(major)
      majors.sort()
    }
    return success({ updatedCount: matchedStudents.length }, '批量修改专业成功')
  }
  if (path.startsWith('/student/personal-stats')) {
    return success({ internships: 2, competitions: 1, papers: 1, projects: 1 })
  }
  if (path === '/student/statistics') {
    return success({ totalStudents: students.length, internshipCount: internships.length, competitionCount: competitions.length })
  }
  if (path === '/student/news' || path === '/student/recent-activities') {
    return success([
      { id: 'news-001', title: '完成前端预览模式配置', description: '现在可以不启动后端浏览页面', time: toDateTime(0) },
      { id: 'news-002', title: '新增实习记录', description: '星河软件科技有限公司前端实习', time: toDateTime(-2) }
    ])
  }
  if (path === '/student/chart') {
    return success({ majors: ['软件工程', '计科', '人工智能'], values: [78, 56, 32] })
  }
  if (path === '/student/study-records') {
    return success(getPreviewGradeRecords())
  }
  if (path === '/admin/list') return success(admins)
  if (path === '/admin/audit/dashboard-stats') return adminDashboard()
  if (path === '/admin/audit/stats') return auditStats()

  if (path === '/daily-tasks/active' || path === '/daily-tasks/all') return success(dailyTasks)
  if (path.startsWith('/daily-tasks/my-submission')) return success(null)
  if (path.startsWith('/daily-tasks/stats')) {
    return success({ total: 48, submitted: 36, pending: 12, completionRate: 75 })
  }
  if (path === '/daily-tasks/completion-analysis') {
    return success([
      { studentId: '10240001', studentName: '张同学', totalTasks: 8, completedTasks: 7, completionRate: 87.5, averageRate: 87.5 },
      { studentId: '10240002', studentName: '李同学', totalTasks: 8, completedTasks: 6, completionRate: 75, averageRate: 75 }
    ])
  }
  if (path.startsWith('/daily-tasks/student-incomplete-tasks')) return success([dailyTasks[0]])
  if (path === '/activities/list') {
    return success([
      { id: 'activity-001', name: '学院志愿活动', title: '学院志愿活动', taskId: 'task-002', activityDate: toDate(5), active: true }
    ])
  }
  if (path.includes('/activities/') && path.includes('/import-students')) {
    return success({ success: true, matchedCount: 2, unmatchedCount: 0, matchedResults: students, unmatchedResults: [] })
  }
  if (path.includes('/academic-events') || path.includes('/daily-activities') || path.includes('/honors')) {
    return listResponse(path)
  }
  if (path === '/leave/my-leaves' || path === '/leave/all' || path === '/leave/pending') return success(leaveRequests)

  if (path === '/internship-employment/all' || path === '/internship-employment/my-records') return success(internships)
  if (path.startsWith('/internship-employment/get/')) return success(findById(internships, path.split('/').pop()))
  if (path.startsWith('/internship-employment/statistics')) return internshipStats(path)
  if (path.startsWith('/internship-employment/search') || path.includes('/internship-employment/')) return success(internships)

  if (path.includes('/competitions/student') || path === '/competitions/admin/all' || path === '/admin/competitions' || path === '/competitions/search') {
    return success(competitions)
  }
  if (path.includes('/papers/student') || path === '/papers/admin/all' || path === '/admin/papers') return success(papers)
  if (path.includes('/patents/student') || path === '/patents/admin/all' || path === '/admin/patents') return success(patents)
  if (path.includes('/projects/student') || path === '/projects/admin/all' || path === '/admin/projects') return success(projects)
  if (path === '/competition-dict/active' || path === '/competition-dict/search' || path === '/competition-dict/admin/all') {
    return success(competitionDicts)
  }

  if (path === '/alumni/getMyAlumniInfo') return success(alumni)
  if (path === '/alumni/searchAlumniList') return success([alumni])
  if (path === '/alumni/getTotalCount') return success(1)

  if (
    path.includes('/party-member') ||
    path.includes('/party-application') ||
    path.includes('/thought-report') ||
    path.includes('/party-course') ||
    path.includes('/volunteer-service')
  ) {
    return partyResponse(path)
  }

  if (path === '/upload') {
    return success(['/uploads/preview-file.png'])
  }

  if (path === '/student/import') {
    return { code: 400, data: null, msg: '预览模式不支持导入名单，数据不会保存' }
  }

  if (['post', 'put', 'delete'].includes((config.method || '').toLowerCase())) {
    return success({ id: `preview-${Date.now()}`, success: true }, '预览模式操作成功')
  }

  return success([])
}

export function previewAdapter(config) {
  return Promise.resolve({
    data: createPreviewResponse(config),
    status: 200,
    statusText: 'OK',
    headers: {},
    config,
    request: null
  })
}
