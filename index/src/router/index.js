import { createRouter, createWebHistory } from 'vue-router'
import MainLayout from '../components/Layout.vue'
import AdminLayout from '../components/AdminLayout.vue'
import { getCurrentUser } from '@/api/student'
import { getCurrentAdmin } from '@/api/admin'
import store from '@/store'
import { ensurePreviewSession, isPreviewMode } from '@/utils/previewMode'

const routes = [
  {
    path: '/preview',
    name: 'preview',
    component: () => import('../views/Preview.vue'),
    meta: { title: '前端预览模式' }
  },
  {
    path: '/',
    component: MainLayout,
    redirect: '/home',
    meta: { requiresAuth: true, userType: 'student' },
    children: [
      {
        path: '/home',
        name: 'home',
        component: () => import('../views/Home.vue'),
        meta: { title: '主页面' }
      },
      {
        path: '/profile',
        name: 'profile',
        component: () => import('../views/Profile.vue'),
        meta: { title: '个人信息模块' }
      },
      {
        path: '/profile/study',
        name: 'profileStudy',
        component: () => import('../views/Profile.vue'),
        meta: { title: '学习记录', parentTitle: '个人信息模块', parentPath: '/profile' }
      },
      {
        path: '/daily',
        name: 'daily',
        component: () => import('../views/Daily.vue'),
        meta: { title: '日常管理模块' }
      },
      {
        path: '/daily/academic',
        name: 'dailyAcademic',
        component: () => import('../views/Daily.vue'),
        meta: { title: '学术活动', parentTitle: '日常管理模块', parentPath: '/daily/academic' }
      },
      {
        path: '/daily/daily',
        name: 'dailyActivity',
        component: () => import('../views/Daily.vue'),
        meta: { title: '日常活动', parentTitle: '日常管理模块', parentPath: '/daily/academic' }
      },
      {
        path: '/daily/leave',
        name: 'dailyLeave',
        component: () => import('../views/Daily.vue'),
        meta: { title: '请假管理', parentTitle: '日常管理模块', parentPath: '/daily/academic' }
      },
      {
        path: '/daily/honor',
        name: 'dailyHonor',
        component: () => import('../views/Daily.vue'),
        meta: { title: '荣誉', parentTitle: '日常管理模块', parentPath: '/daily/academic' }
      },
      {
        path: '/daily/tasks',
        name: 'dailyTasks',
        component: () => import('../views/Daily.vue'),
        meta: { title: '日常任务', parentTitle: '日常管理模块', parentPath: '/daily/academic' }
      },
      {
        path: '/internship',
        name: 'internship',
        component: () => import('../views/Internship.vue'),
        meta: { title: '实习就业模块' }
      },
      {
        path: '/internship/intent',
        name: 'internshipIntent',
        component: () => import('../views/Internship.vue'),
        meta: { title: '就业意向采集', parentTitle: '实习就业模块', parentPath: '/internship/intent' }
      },
      {
        path: '/internship/records',
        name: 'internshipRecords',
        component: () => import('../views/Internship.vue'),
        meta: { title: '分类展示与记录', parentTitle: '实习就业模块', parentPath: '/internship/intent' }
      },
      {
        path: '/competition',
        name: 'competition',
        component: () => import('../views/Competition.vue'),
        meta: { title: '科创竞赛模块' }
      },
      {
        path: '/competition/competition',
        name: 'competitionCompetition',
        component: () => import('../views/Competition.vue'),
        meta: { title: '学科竞赛', parentTitle: '科创竞赛模块', parentPath: '/competition/competition' }
      },
      {
        path: '/competition/paper',
        name: 'competitionPaper',
        component: () => import('../views/Competition.vue'),
        meta: { title: '发表文章', parentTitle: '科创竞赛模块', parentPath: '/competition/competition' }
      },
      {
        path: '/competition/patent',
        name: 'competitionPatent',
        component: () => import('../views/Competition.vue'),
        meta: { title: '专利', parentTitle: '科创竞赛模块', parentPath: '/competition/competition' }
      },
      {
        path: '/competition/project',
        name: 'competitionProject',
        component: () => import('../views/Competition.vue'),
        meta: { title: '项目', parentTitle: '科创竞赛模块', parentPath: '/competition/competition' }
      },
      {
        path: '/alumni',
        name: 'alumni',
        component: () => import('../views/Alumni.vue'),
        meta: { title: '校友管理模块' }
      },
      {
        path: '/party',
        name: 'party',
        component: () => import('../views/Party.vue'),
        meta: { title: '党员管理模块' }
      },
      {
        path: '/party/member',
        name: 'partyMember',
        component: () => import('../views/Party.vue'),
        meta: { title: '党员信息/申请入党', parentTitle: '党员管理模块', parentPath: '/party/member' }
      },
      {
        path: '/party/report',
        name: 'partyReport',
        component: () => import('../views/Party.vue'),
        meta: { title: '思想汇报', parentTitle: '党员管理模块', parentPath: '/party/member' }
      },
      {
        path: '/party/course',
        name: 'partyCourse',
        component: () => import('../views/Party.vue'),
        meta: { title: '微党课', parentTitle: '党员管理模块', parentPath: '/party/member' }
      },
      {
        path: '/party/service',
        name: 'partyService',
        component: () => import('../views/Party.vue'),
        meta: { title: '志愿服务', parentTitle: '党员管理模块', parentPath: '/party/member' }
      }
    ]
  },
  
  // 管理员路由
  {
    path: '/admin',
    component: AdminLayout,
    redirect: '/admin/dashboard',
    meta: { requiresAuth: true, userType: 'admin' },
    children: [
      {
        path: '/admin/dashboard',
        name: 'adminDashboard',
        component: () => import('../views/admin/Dashboard.vue'),
        meta: { title: '管理员仪表板' }
      },
      // ==================== 学生管理 ====================
      {
        path: '/admin/students/list',
        name: 'adminStudentsList',
        component: () => import('../views/admin/StudentsList.vue'),
        meta: { title: '学生列表' }
      },
      {
        path: '/admin/students/add',
        name: 'adminStudentsAdd',
        component: () => import('../views/admin/StudentsAdd.vue'),
        meta: { title: '添加学生' }
      },
      {
        path: '/admin/students/grades/import',
        name: 'adminGradeImport',
        component: () => import('../views/admin/GradeImport.vue'),
        meta: { title: '成绩导入' }
      },
      {
        path: '/admin/students',
        name: 'adminStudents',
        component: () => import('../views/admin/StudentsList.vue'),
        meta: { title: '学生管理' }
      },
      // ==================== 校友管理 ====================
      {
        path: '/admin/alumni/list',
        name: 'adminAlumniList',
        component: () => import('../views/admin/AlumniList.vue'),
        meta: { title: '校友列表' }
      },
      {
        path: '/admin/alumni/add',
        name: 'adminAlumniAdd',
        component: () => import('../views/admin/AlumniAdd.vue'),
        meta: { title: '添加校友' }
      },
      // ==================== 实习就业 ====================
      {
        path: '/admin/internship/list',
        name: 'adminInternshipList',
        component: () => import('../views/admin/InternshipList.vue'),
        meta: { title: '实习就业列表' }
      },
      {
        path: '/admin/internship/analysis',
        name: 'adminInternshipAnalysis',
        component: () => import('../views/admin/InternshipAnalysis.vue'),
        meta: { title: '实习就业分析' }
      },
      // ==================== 科创竞赛 ====================
      // 竞赛管理
      {
        path: '/admin/innovation/competitions/list',
        name: 'adminInnovationCompetitionsList',
        component: () => import('../views/admin/innovation/Competitions.vue'),
        meta: { title: '竞赛列表' }
      },
      {
        path: '/admin/innovation/competitions/dict',
        name: 'adminInnovationCompetitionsDict',
        component: () => import('../views/admin/innovation/CompetitionDict.vue'),
        meta: { title: '竞赛字典管理' }
      },
      
      // 项目管理
      {
        path: '/admin/innovation/projects/list',
        name: 'adminInnovationProjectsList',
        component: () => import('../views/admin/innovation/Projects.vue'),
        meta: { title: '项目列表' }
      },
      
      // 论文管理
      {
        path: '/admin/innovation/papers/list',
        name: 'adminInnovationPapersList',
        component: () => import('../views/admin/innovation/Papers.vue'),
        meta: { title: '论文列表' }
      },
      
      // 专利管理
      {
        path: '/admin/innovation/patents/list',
        name: 'adminInnovationPatentsList',
        component: () => import('../views/admin/innovation/Patents.vue'),
        meta: { title: '专利列表' }
      },
      
      // 数据统计
      {
        path: '/admin/innovation/statistics',
        name: 'adminInnovationStatistics',
        component: () => import('../views/admin/innovation/Statistics.vue'),
        meta: { title: '科创数据统计' }
      },
      // ==================== 党员管理 ====================
      {
        path: '/admin/party/management',
        name: 'PartyManagement',
        component: () => import('../views/admin/PartyManagement.vue'),
        meta: { title: '党员管理', requiresAuth: true, requiresAdmin: true }
      },
      {
        path: '/admin/party/list',
        name: 'adminPartyList',
        component: () => import('../views/admin/PartyList.vue'),
        meta: { title: '党员列表' }
      },
      {
        path: '/admin/party/add',
        name: 'adminPartyAdd',
        component: () => import('../views/admin/PartyAdd.vue'),
        meta: { title: '添加党员' }
      },
      // ==================== 日常管理 ====================
      {
        path: '/admin/daily/dailycontrol',
        name: 'dailycontrol',
        component: () => import('../views/admin/DailyControl.vue'),
        meta: { title: '活动审批列表' }
      },
      {
        path: '/admin/daily/task-completion-analysis',
        name: 'taskCompletionAnalysis',
        component: () => import('../views/admin/TaskCompletionAnalysis.vue'),
        meta: { title: '任务完成度分析' }
      },
      {
        path: '/admin/daily/activity-management',
        name: 'activityManagement',
        component: () => import('../views/admin/ActivityManagement.vue'),
        meta: { title: '活动管理' }
      },
      
      // ==================== 管理员管理 ====================
      {
        path: '/admin/admins',
        name: 'adminAdmins',
        component: () => import('../views/admin/Admins.vue'),
        meta: { title: '管理员管理' }
      },
      {
        path: '/admin/profile',
        name: 'adminProfile',
        component: () => import('../views/admin/Profile.vue'),
        meta: { title: '管理员个人信息' }
      },
    ]
  },
  
  // 登录页面
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/Login.vue'),
    meta: { title: '登录' }
  },
  
  // 修改初始密码页面（无需认证）
  {
    path: '/change-password',
    name: 'changePassword',
    component: () => import('../views/ChangePassword.vue'),
    meta: { title: '修改初始密码' }
  },
  
  // 校友-学生页面
  {
    path: '/alumni-student',
    name: 'alumniStudent',
    component: () => import('../views/Alumni.vue'),
    meta: { title: '校友-学生' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 全局前置守卫：未登录自动跳转到登录页
router.beforeEach(async (to, from, next) => {
  // 前端预览模式：不用后端、不用真实登录，直接使用本地假用户和假数据。
  if (to.query.preview === '1' || to.path === '/preview' || isPreviewMode()) {
    ensurePreviewSession(to.path, store)
    next()
    return
  }

  // 登录页面和修改密码页面无需认证
  if (to.path === '/login' || to.path === '/change-password') {
    next()
    return
  }
  
  const token = localStorage.getItem('token')
  
  // 如果没有token，跳转到登录页
  if (!token) {
    if (from.path !== '/login') {
      console.log('Token不存在，跳转到登录页')
    }
    next('/login')
    return
  }
  
  // 检查token是否过期
  const expire = localStorage.getItem('token_expire')
  const isExpired = expire && Date.now() > Number(expire)
  
  if (isExpired) {
    if (from.path !== '/login') {
      console.log('Token已过期，跳转到登录页')
    }
    localStorage.removeItem('token')
    localStorage.removeItem('token_expire')
    next('/login')
    return
  }
  
  // 获取用户信息（从store）
  let user = store.state.user
  
  // 如果没有用户信息，尝试从后端获取
  if (!user) {
    try {
      // 根据当前路由判断用户类型，调用对应的API
      let response
      if (to.path.startsWith('/admin')) {
        // 管理员路由，调用管理员API
        response = await getCurrentAdmin()
      } else {
        // 学生路由，调用学生API
        response = await getCurrentUser()
      }
      
      if (response.code === 200) {
        user = response.data
        store.commit('setUser', user)
      } else {
        // 获取用户信息失败，清除token并跳转到登录页
        localStorage.removeItem('token')
        localStorage.removeItem('token_expire')
        next('/login')
        return
      }
    } catch (error) {
      console.error('获取用户信息失败:', error)
      localStorage.removeItem('token')
      localStorage.removeItem('token_expire')
      next('/login')
      return
    }
  }
  
  const userType = user ? user.userType : null
  
  // 检查用户类型和路由权限
  if (to.meta.requiresAuth) {
    if (to.meta.userType && to.meta.userType !== userType) {
      // 用户类型不匹配，重定向到对应的首页
      if (userType === 'admin') {
        next('/admin/dashboard')
      } else {
        next('/home')
      }
      return
    }
  }
  
  next()
})

export default router
