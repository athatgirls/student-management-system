/* eslint-disable vue/multi-word-component-names */
<template>
  <div class="student-layout">
    <el-container>
      <div
        v-if="isMobile && sidebarVisible"
        class="sidebar-overlay"
        @click="handleOverlayClick"
      ></div>

      <el-aside
        :width="isMobile ? (sidebarVisible ? '250px' : '0') : '250px'"
        class="sidebar"
        :class="{ 'mobile-sidebar': isMobile }"
      >
        <div class="logo">
          <h2>学生管理系统</h2>
          <p>学生工作台</p>
        </div>

          <el-menu
            :default-active="activeMenu"
            :default-openeds="openedMenus"
            class="sidebar-menu"
            router
            background-color="#304156"
            text-color="#bfcbd9"
            active-text-color="#409EFF"
            :collapse="false"
          >
            <el-menu-item index="/home">
              <el-icon><House /></el-icon>
              <span>主页面</span>
            </el-menu-item>
            <el-sub-menu index="/profile">
              <template #title>
                <el-icon><User /></el-icon>
                <span>个人信息模块</span>
              </template>
              <el-menu-item index="/profile">个人资料</el-menu-item>
              <el-menu-item index="/profile/study">学习记录</el-menu-item>
            </el-sub-menu>
            <el-sub-menu index="/daily">
              <template #title>
                <el-icon><Calendar /></el-icon>
                <span>日常管理模块</span>
              </template>
              <el-menu-item index="/daily/academic">学术活动</el-menu-item>
              <el-menu-item index="/daily/daily">日常活动</el-menu-item>
              <el-menu-item index="/daily/leave">请假管理</el-menu-item>
              <el-menu-item index="/daily/honor">荣誉</el-menu-item>
              <el-menu-item index="/daily/tasks">日常任务</el-menu-item>
            </el-sub-menu>
            <el-sub-menu index="/internship">
              <template #title>
                <el-icon><Briefcase /></el-icon>
                <span>实习就业模块</span>
              </template>
              <el-menu-item index="/internship/intent">就业意向采集</el-menu-item>
              <el-menu-item index="/internship/records">分类展示与记录</el-menu-item>
            </el-sub-menu>
            <el-sub-menu index="/competition">
              <template #title>
                <el-icon><Trophy /></el-icon>
                <span>科创竞赛模块</span>
              </template>
              <el-menu-item index="/competition/competition">学科竞赛</el-menu-item>
              <el-menu-item index="/competition/paper">发表文章</el-menu-item>
              <el-menu-item index="/competition/patent">专利</el-menu-item>
              <el-menu-item index="/competition/project">项目</el-menu-item>
            </el-sub-menu>
            <el-menu-item index="/alumni">
              <el-icon><Connection /></el-icon>
              <span>校友管理模块</span>
            </el-menu-item>
            <el-sub-menu index="/party">
              <template #title>
                <el-icon><Flag /></el-icon>
                <span>党员管理模块</span>
              </template>
              <el-menu-item index="/party/member">党员信息/申请入党</el-menu-item>
              <el-menu-item index="/party/report">思想汇报</el-menu-item>
              <el-menu-item index="/party/course">微党课</el-menu-item>
              <el-menu-item index="/party/service">志愿服务</el-menu-item>
            </el-sub-menu>
          </el-menu>
        </el-aside>

      <el-container>
        <el-header class="header">
          <div class="header-left">
            <el-button
              v-if="isMobile"
              link
              class="menu-toggle"
              @click="sidebarVisible = !sidebarVisible"
            >
              <el-icon><Menu /></el-icon>
            </el-button>
            <el-breadcrumb separator="/">
              <el-breadcrumb-item :to="{ path: '/home' }">首页</el-breadcrumb-item>
              <el-breadcrumb-item v-if="parentTitle" :to="{ path: parentPath }">
                {{ parentTitle }}
              </el-breadcrumb-item>
              <el-breadcrumb-item>{{ pageTitle }}</el-breadcrumb-item>
            </el-breadcrumb>
          </div>

          <div class="header-right">
            <el-dropdown>
              <span class="user-dropdown">
                <el-avatar :size="32">{{ userInitial }}</el-avatar>
                <span class="username">{{ userInfo.name || '学生' }}</span>
                <el-icon><ArrowDown /></el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="router.push('/profile')">个人信息</el-dropdown-item>
                  <el-dropdown-item divided @click="logout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </el-header>

        <el-main class="main-content">
          <div class="page-shell" v-if="route.path !== '/home'">
            <div class="page-title-group">
              <span>{{ parentTitle || '学生端' }}</span>
              <h1>{{ pageTitle }}</h1>
            </div>
          </div>
          <router-view />
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useStore } from 'vuex'
import { ElMessage } from 'element-plus'
import { getCurrentUser } from '@/api/student'
import { 
  House, 
  User, 
  Calendar, 
  Briefcase, 
  Trophy, 
  Connection, 
  ArrowDown,
  Flag,
  Menu
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const store = useStore()

const activeMenu = computed(() => {
  const tab = route.query.tab
  if (route.path === '/daily') return `/daily/${tab || 'academic'}`
  if (route.path === '/internship') return `/internship/${tab || 'intent'}`
  if (route.path === '/competition') return `/competition/${tab || 'competition'}`
  if (route.path === '/party') return `/party/${tab || 'member'}`
  if (route.path === '/profile' && tab === 'study') return '/profile/study'
  return route.path
})

const openedMenus = computed(() => {
  if (route.path.startsWith('/profile')) return ['/profile']
  if (route.path.startsWith('/daily')) return ['/daily']
  if (route.path.startsWith('/internship')) return ['/internship']
  if (route.path.startsWith('/competition')) return ['/competition']
  if (route.path.startsWith('/party')) return ['/party']
  return []
})

// 获取用户信息
const userInfo = computed(() => store.state.user || {})
const userInitial = computed(() => (userInfo.value.name || '学').charAt(0))
const pageTitle = computed(() => route.meta.title || '页面')
const parentTitle = computed(() => route.meta.parentTitle || '')
const parentPath = computed(() => route.meta.parentPath || route.path)

// 移动端适配
const isMobile = ref(window.innerWidth <= 768)
const sidebarVisible = ref(false)

const handleResize = () => {
  isMobile.value = window.innerWidth <= 768
  if (!isMobile.value) {
    sidebarVisible.value = false
  }
}

// 点击遮罩层关闭侧边栏
const handleOverlayClick = () => {
  if (isMobile.value) {
    sidebarVisible.value = false
  }
}

const logout = () => {
  localStorage.removeItem('token')
  store.commit('logout')
  ElMessage.success('退出登录成功')
  router.push('/login')
}

// 定时检查token是否过期
let timer = null
const checkTokenAndLogout = async () => {
  const token = localStorage.getItem('token')
  if (token) {
    // 尝试刷新token
    const refreshSuccess = await store.dispatch('refreshToken')
    if (!refreshSuccess) {
      ElMessage.warning('登录已过期，请重新登录')
      router.push('/login')
    }
  } else {
    ElMessage.warning('登录已过期，请重新登录')
    router.push('/login')
  }
}

onMounted(async () => {
  // 如果store中没有用户信息，尝试从后端获取
  if (!store.state.user && store.state.token) {
    try {
      const response = await getCurrentUser()
      if (response.code === 200) {
        store.commit('setUser', response.data)
      } else {
        // 获取用户信息失败，清除token并跳转到登录页
        store.commit('logout')
        router.push('/login')
      }
    } catch (error) {
      console.error('获取用户信息失败:', error)
      store.commit('logout')
      router.push('/login')
    }
  }
  
  // 启动定时检查token（每30分钟检查一次）
  timer = setInterval(checkTokenAndLogout, 30 * 60 * 1000)
  
  // 监听窗口大小变化
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
.student-layout {
  width: 100%;
  height: 100vh;
}

.sidebar {
  height: 100vh;
  overflow-y: auto;
  color: #bfcbd9;
  background-color: #304156;
  transition: width 0.3s;
}

.logo {
  height: 88px;
  padding: 20px;
  color: #fff;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  background: linear-gradient(135deg, rgba(64, 158, 255, 0.18), rgba(48, 65, 86, 0));
}

.logo h2 {
  margin: 0 0 6px;
  font-size: 18px;
  font-weight: 700;
}

.logo p {
  margin: 0;
  color: #9fb2c8;
  font-size: 12px;
}

.sidebar-menu {
  height: calc(100vh - 88px);
  border-right: none;
}

.header {
  height: 60px;
  padding: 0 20px;
  background: #fff;
  border-bottom: 1px solid #e6e6e6;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-right {
  display: flex;
  align-items: center;
}

.user-dropdown {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #606266;
  cursor: pointer;
}

.username {
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.menu-toggle {
  font-size: 20px;
  color: #303133;
  padding: 0;
}

.main-content {
  min-height: calc(100vh - 60px);
  padding: 20px;
  background: #f0f2f5;
}

.page-shell {
  margin-bottom: 16px;
  padding: 18px 22px;
  border-radius: 10px;
  background: #fff;
  box-shadow: 0 1px 3px rgba(18, 32, 55, 0.06);
}

.page-title-group span {
  color: #909399;
  font-size: 13px;
}

.page-title-group h1 {
  margin: 6px 0 0;
  color: #303133;
  font-size: 22px;
}

.mobile-sidebar {
  position: fixed;
  left: 0;
  top: 60px;
  height: calc(100vh - 60px);
  z-index: 1000;
  overflow-x: hidden;
  transition: width 0.3s;
}

.sidebar-overlay {
  position: fixed;
  top: 60px;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  z-index: 999;
}

:deep(.el-sub-menu .el-menu-item) {
  min-width: 0;
  padding-left: 54px !important;
  background-color: #263445 !important;
}

:deep(.el-sub-menu .el-menu-item:hover),
:deep(.el-menu-item:hover),
:deep(.el-sub-menu__title:hover) {
  background-color: #263445 !important;
}

:deep(.el-menu-item.is-active) {
  background-color: #263445 !important;
  border-right: 3px solid #409eff;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .header {
    padding: 0 12px;
  }

  .main-content {
    padding: 10px;
  }

  .sidebar-menu {
    width: 250px;
  }
}

@media (max-width: 480px) {
  .username {
    display: none;
  }

  .main-content {
    padding: 8px;
  }
}
</style> 
