/* eslint-disable vue/multi-word-component-names */
<template>
  <div class="home">
    <el-card style="margin-bottom: 20px;">
      <div class="welcome-section">
        <div class="welcome-info">
          <h2>欢迎回来，{{ userInfo.name }}！</h2>
          <p>学号：{{ userInfo.studentId }} | 专业：{{ userInfo.major }} | 年级：{{ userInfo.grade }}</p>
        </div>
        <div class="welcome-time">
          <p>{{ currentTime }}</p>
        </div>
      </div>
    </el-card>

    <!-- 个人统计卡片 -->
    <el-row :gutter="20">
      <el-col :xs="12" :sm="12" :md="6" :lg="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon">
              <el-icon><Document /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-number">{{ personalStats.internships }}</div>
              <div class="stat-label">我的实习</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="12" :md="6" :lg="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon">
              <el-icon><Trophy /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-number">{{ personalStats.competitions }}</div>
              <div class="stat-label">我的竞赛</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="12" :md="6" :lg="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon">
              <el-icon><Document /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-number">{{ personalStats.papers }}</div>
              <div class="stat-label">我的论文</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="12" :md="6" :lg="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon">
              <el-icon><Connection /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-number">{{ personalStats.projects }}</div>
              <div class="stat-label">我的项目</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="main-content-row" style="margin-top: 20px;">
      <el-col :xs="24" :sm="24" :md="12" :lg="12" class="recent-activities-col">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>我的最近活动</span>
            </div>
          </template>
          <el-timeline>
            <el-timeline-item
              v-for="activity in displayedRecentActivities"
              :key="activity.id"
              :timestamp="activity.time"
              placement="top"
            >
              <el-card>
                <h4>{{ activity.title }}</h4>
                <p>{{ activity.description }}</p>
              </el-card>
            </el-timeline-item>
          </el-timeline>
          <div v-if="recentActivities.length > maxRecentActivities" class="more-actions">
            <el-button text type="primary" @click="showAllRecentActivities = !showAllRecentActivities">
              {{ showAllRecentActivities ? '收起' : `查看更多 (${recentActivities.length - maxRecentActivities}条)` }}
            </el-button>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="24" :md="12" :lg="12" class="todo-list-col">
        <el-card>
          <template #header>
            <div class="card-header">
              <span class="todo-header">
                待办事项
                <span v-if="pendingTodoCount > 0" class="todo-badge">{{ pendingTodoCount }}</span>
              </span>
            </div>
          </template>
          
          <!-- 任务类型切换按钮 -->
          <div class="todo-tabs" v-if="normalTodos.length > 0 || registrationTodos.length > 0">
            <el-button-group>
              <el-button 
                :type="todoTab === 'all' ? 'primary' : ''" 
                size="small"
                @click="todoTab = 'all'"
              >
                全部
                <span v-if="pendingTodoCount > 0" class="tab-count">({{ pendingTodoCount }})</span>
              </el-button>
              <el-button 
                :type="todoTab === 'normal' ? 'primary' : ''" 
                size="small"
                @click="todoTab = 'normal'"
              >
                普通任务
                <span v-if="normalPendingCount > 0" class="tab-count">({{ normalPendingCount }})</span>
              </el-button>
              <el-button 
                :type="todoTab === 'registration' ? 'primary' : ''" 
                size="small"
                @click="todoTab = 'registration'"
              >
                报名型任务
                <span v-if="registrationPendingCount > 0" class="tab-count">({{ registrationPendingCount }})</span>
              </el-button>
            </el-button-group>
          </div>
          
          <div class="todo-list">
            <!-- 请假记录（始终显示） -->
            <div
              v-for="todo in displayedLeaveTodos"
              :key="todo.id"
              class="todo-item"
              :class="{ 'todo-completed': todo.completed }"
              @click="handleTodoClick(todo)"
            >
              <div class="todo-content">
                <div class="todo-icon-wrapper">
                  <el-icon v-if="todo.completed || todo.type === 'leave'" class="todo-icon completed"><CircleCheck /></el-icon>
                  <el-icon v-else class="todo-icon pending"><Clock /></el-icon>
                  <span v-if="!todo.completed && todo.type !== 'leave'" class="todo-item-dot"></span>
                </div>
                <div class="todo-title-wrapper" style="display: flex; align-items: center; flex: 1;">
                  <span class="todo-title">{{ todo.title }}</span>
                  <el-tag v-if="todo.type === 'leave' && todo.leaveStatus" :type="getLeaveStatusType(todo.leaveStatus)" size="small" style="margin-left: 8px;">
                    {{ getLeaveStatusText(todo.leaveStatus) }}
                  </el-tag>
                </div>
              </div>
              <div class="todo-time-info">
                <div class="todo-time-item" v-if="todo.createTime">
                  <span class="time-label">发布时间：</span>
                  <span class="time-value">{{ todo.createTime }}</span>
                </div>
                <div class="todo-time-item" v-if="todo.type === 'leave'">
                  <span class="time-label">请假时间：</span>
                  <span class="time-value">{{ todo.leaveTimeText }}</span>
                </div>
                <div class="todo-time-item" v-else>
                  <span class="time-label">截止时间：</span>
                  <span class="time-value">{{ todo.deadlineText }}</span>
                </div>
              </div>
            </div>
            
            <!-- 普通任务 -->
            <template v-if="todoTab === 'all' || todoTab === 'normal'">
              <div
                v-for="todo in displayedNormalTodos"
                :key="todo.id"
                class="todo-item"
                :class="{ 'todo-completed': todo.completed }"
                @click="handleTodoClick(todo)"
              >
              <div class="todo-content">
                <div class="todo-icon-wrapper">
                  <el-icon v-if="todo.completed" class="todo-icon completed"><CircleCheck /></el-icon>
                  <el-icon v-else class="todo-icon pending"><Clock /></el-icon>
                  <span v-if="!todo.completed" class="todo-item-dot"></span>
                </div>
                <div class="todo-title-wrapper" style="display: flex; align-items: center; flex: 1;">
                  <span class="todo-title">{{ todo.title }}</span>
                </div>
              </div>
              <div class="todo-time-info">
                <div class="todo-time-item" v-if="todo.createTime">
                  <span class="time-label">发布时间：</span>
                  <span class="time-value">{{ todo.createTime }}</span>
                </div>
                <div class="todo-time-item">
                  <span class="time-label">截止时间：</span>
                  <span class="time-value">{{ todo.deadlineText }}</span>
                </div>
              </div>
              </div>
            </template>
            
            <!-- 报名型任务 -->
            <template v-if="todoTab === 'all' || todoTab === 'registration'">
              <div
                v-for="todo in displayedRegistrationTodos"
                :key="todo.id"
                class="todo-item"
                :class="{ 'todo-completed': todo.completed }"
                @click="handleTodoClick(todo)"
              >
              <div class="todo-content">
                <div class="todo-icon-wrapper">
                  <el-icon v-if="todo.completed" class="todo-icon completed"><CircleCheck /></el-icon>
                  <el-icon v-else class="todo-icon pending"><Clock /></el-icon>
                  <span v-if="!todo.completed" class="todo-item-dot"></span>
                </div>
                <div class="todo-title-wrapper" style="display: flex; align-items: center; flex: 1;">
                  <span class="todo-title">{{ todo.title }}</span>
                  <el-tag v-if="todo.taskCategory === 'registration'" type="warning" size="small" effect="plain" style="margin-left: 8px;">
                    报名型
                  </el-tag>
                  <el-tag 
                    v-if="todo.maxParticipants && todo.currentParticipants >= todo.maxParticipants && !todo.completed" 
                    type="danger" 
                    size="small" 
                    effect="plain"
                    style="margin-left: 8px;"
                  >
                    已满员
                  </el-tag>
                </div>
              </div>
              <div class="todo-time-info">
                <div class="todo-time-item" v-if="todo.createTime">
                  <span class="time-label">发布时间：</span>
                  <span class="time-value">{{ todo.createTime }}</span>
                </div>
                <div class="todo-time-item" v-if="todo.maxParticipants">
                  <span class="time-label">报名情况：</span>
                  <span class="time-value">{{ todo.currentParticipants || 0 }} / {{ todo.maxParticipants }}</span>
                </div>
                <div class="todo-time-item">
                  <span class="time-label">截止时间：</span>
                  <span class="time-value">{{ todo.deadlineText }}</span>
                </div>
              </div>
              </div>
            </template>
            
            <!-- 空状态 -->
            <div v-if="displayedTodoList.length === 0" class="todo-empty">
              <el-empty description="暂无待办事项" :image-size="80" />
            </div>
          </div>
          
          <div v-if="displayedTodoList.length > maxTodoList && (todoTab === 'all' || (todoTab === 'normal' && normalTodos.length > maxTodoList) || (todoTab === 'registration' && registrationTodos.length > maxTodoList))" class="more-actions">
            <el-button text type="primary" @click="showAllTodoList = !showAllTodoList">
              {{ showAllTodoList ? '收起' : `查看更多 (${displayedTodoList.length - maxTodoList}条)` }}
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :xs="24" :sm="24" :md="24" :lg="24">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>快速操作</span>
            </div>
          </template>
          <div class="quick-actions">
            <el-button type="primary" @click="goToInternship" class="quick-action-btn">
              <el-icon><Briefcase /></el-icon>
              <span class="btn-text">申请实习</span>
            </el-button>
            <el-button type="success" @click="goToCompetition" class="quick-action-btn">
              <el-icon><Trophy /></el-icon>
              <span class="btn-text">参加竞赛</span>
            </el-button>
            <el-button type="warning" @click="goToPaper" class="quick-action-btn">
              <el-icon><Document /></el-icon>
              <span class="btn-text">提交论文</span>
            </el-button>
            <el-button type="info" @click="goToProject" class="quick-action-btn">
              <el-icon><Connection /></el-icon>
              <span class="btn-text">项目管理</span>
            </el-button>
            <el-button @click="goToProfile" class="quick-action-btn">
              <el-icon><User /></el-icon>
              <span class="btn-text">个人信息</span>
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useStore } from 'vuex'
import { useRouter } from 'vue-router'
import { 
  User, 
  Briefcase, 
  Trophy, 
  Connection, 
  Document,
  CircleCheck,
  Clock
} from '@element-plus/icons-vue'
import { getPersonalStats } from '@/api/home'
import { getActiveDailyTasks } from '@/api/daily'
import { getProfile, getRecentActivities } from '@/api/student'
import { getMyLeaveRequests } from '@/api/leave'

const store = useStore()
const router = useRouter()

// 用户信息
const userInfo = ref({
  name: '',
  studentId: '',
  major: '',
  grade: ''
})

// 加载用户完整信息
const loadUserInfo = async () => {
  try {
    // 先从 store 获取基本信息
    const storeUser = store.state.user || {}
    userInfo.value = {
      name: storeUser.name || '',
      studentId: storeUser.studentId || '',
      major: storeUser.major || '',
      grade: storeUser.grade || ''
    }
    
    // 从后端获取完整的用户信息（包含专业和年级）
    const res = await getProfile()
    if (res.code === 200 && res.data) {
      userInfo.value = {
        name: res.data.name || storeUser.name || '',
        studentId: res.data.studentId || storeUser.studentId || '',
        major: res.data.major || '',
        grade: res.data.grade || ''
      }
      // 更新 store 中的用户信息
      store.commit('setUser', { ...storeUser, ...userInfo.value })
    }
  } catch (e) {
    console.error('获取用户信息失败', e)
    // 如果获取失败，至少显示 store 中的基本信息
    const storeUser = store.state.user || {}
    userInfo.value = {
      name: storeUser.name || '',
      studentId: storeUser.studentId || '',
      major: storeUser.major || '',
      grade: storeUser.grade || ''
    }
  }
}

// 当前时间
const currentTime = ref('')

// 个人统计数据
const personalStats = ref({
  internships: 0,
  competitions: 0,
  papers: 0,
  projects: 0
})

// 最近活动
const recentActivities = ref([])
const maxRecentActivities = 5 // 首页最多显示5条
const showAllRecentActivities = ref(false)

// 待办事项
const todoList = ref([])
const maxTodoList = 8 // 首页最多显示8条
const showAllTodoList = ref(false)
const todoTab = ref('all') // 任务类型切换：all(全部) | normal(普通任务) | registration(报名型任务)

// 计算未完成的任务数量
const pendingTodoCount = computed(() => {
  return todoList.value.filter(todo => !todo.completed).length
})

// 计算普通任务未完成数量
const normalPendingCount = computed(() => {
  return normalTodos.value.filter(todo => !todo.completed).length
})

// 计算报名型任务未完成数量
const registrationPendingCount = computed(() => {
  return registrationTodos.value.filter(todo => !todo.completed).length
})

// 计算显示的最近活动（限制条数）
const displayedRecentActivities = computed(() => {
  if (showAllRecentActivities.value) {
    return recentActivities.value
  }
  return recentActivities.value.slice(0, maxRecentActivities)
})

// 分离请假记录、普通任务和报名型任务
const leaveTodos = computed(() => {
  return todoList.value.filter(todo => todo.type === 'leave')
})

const normalTodos = computed(() => {
  return todoList.value.filter(todo => todo.type === 'task' && (!todo.taskCategory || todo.taskCategory === 'normal'))
})

const registrationTodos = computed(() => {
  return todoList.value.filter(todo => todo.type === 'task' && todo.taskCategory === 'registration')
})

// 计算显示的待办事项（限制条数）
const displayedLeaveTodos = computed(() => {
  return leaveTodos.value
})

const displayedNormalTodos = computed(() => {
  if (showAllTodoList.value) {
    return normalTodos.value
  }
  return normalTodos.value.slice(0, maxTodoList)
})

const displayedRegistrationTodos = computed(() => {
  if (showAllTodoList.value) {
    return registrationTodos.value
  }
  return registrationTodos.value.slice(0, maxTodoList)
})

// 根据当前选择的标签页，计算显示的任务列表
const displayedTodoList = computed(() => {
  let list = []
  
  // 请假记录始终显示
  list.push(...displayedLeaveTodos.value)
  
  // 根据选择的标签页添加任务
  if (todoTab.value === 'all') {
    list.push(...displayedNormalTodos.value)
    list.push(...displayedRegistrationTodos.value)
  } else if (todoTab.value === 'normal') {
    list.push(...displayedNormalTodos.value)
  } else if (todoTab.value === 'registration') {
    list.push(...displayedRegistrationTodos.value)
  }
  
  return list
})

// 请假状态文本
const getLeaveStatusText = (status, leave) => {
  // 如果状态是on_leave，需要根据结束日期判断是否应该显示"待销假"
  if (status === 'on_leave' && leave && leave.endDate) {
    const endDate = new Date(leave.endDate)
    const today = new Date()
    today.setHours(0, 0, 0, 0)
    const tomorrow = new Date(today)
    tomorrow.setDate(tomorrow.getDate() + 1)
    const dayAfterTomorrow = new Date(today)
    dayAfterTomorrow.setDate(dayAfterTomorrow.getDate() + 2)
    
    endDate.setHours(0, 0, 0, 0)
    
    // 如果结束日期是今天或明天，显示"待销假"
    if (endDate <= tomorrow && endDate >= today) {
      return '待销假'
    }
    // 如果结束日期是昨天，显示"待销假"
    const yesterday = new Date(today)
    yesterday.setDate(yesterday.getDate() - 1)
    if (endDate.getTime() === yesterday.getTime()) {
      return '待销假'
    }
    // 如果结束日期是2天前或更早，显示"逾期未销假"
    if (endDate < yesterday) {
      return '逾期未销假'
    }
  }
  
  const map = {
    pending: '待审核',
    approved: '已批准',
    rejected: '已拒绝',
    on_leave: '假期中',
    pending_check_in: '待销假',
    completed: '已销假',
    overdue: '逾期未销假'
  }
  return map[status] || status
}

// 判断请假是否逾期（结束日期超过2天前）
const _isLeaveOverdue = (leave) => {
  if (!leave || !leave.endDate) return false
  const endDate = new Date(leave.endDate)
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const twoDaysAgo = new Date(today)
  twoDaysAgo.setDate(twoDaysAgo.getDate() - 2)
  endDate.setHours(0, 0, 0, 0)
  return endDate < twoDaysAgo
}

// 请假状态类型
const getLeaveStatusType = (status, leave) => {
  // 如果状态是on_leave，需要根据结束日期判断类型
  if (status === 'on_leave' && leave && leave.endDate) {
    const endDate = new Date(leave.endDate)
    const today = new Date()
    today.setHours(0, 0, 0, 0)
    const tomorrow = new Date(today)
    tomorrow.setDate(tomorrow.getDate() + 1)
    
    endDate.setHours(0, 0, 0, 0)
    
    // 如果结束日期是今天或明天，显示warning（待销假）
    if (endDate <= tomorrow && endDate >= today) {
      return 'warning'
    }
    // 如果结束日期是昨天，显示warning（待销假）
    const yesterday = new Date(today)
    yesterday.setDate(yesterday.getDate() - 1)
    if (endDate.getTime() === yesterday.getTime()) {
      return 'warning'
    }
    // 如果结束日期是2天前或更早，显示danger（逾期未销假）
    if (endDate < yesterday) {
      return 'danger'
    }
  }
  
  const map = {
    pending: 'warning',
    approved: 'success',
    rejected: 'danger',
    on_leave: 'info',
    pending_check_in: 'warning',
    completed: 'success',
    overdue: 'danger'
  }
  return map[status] || 'info'
}

// 加载待办任务
const loadTodoList = async () => {
  try {
    const allTodos = []
    
    // 1. 加载已审核通过的请假记录（未销假）
    try {
      const studentId = userInfo.value.studentId || store.state.user?.studentId
      if (studentId) {
        const leaveRes = await getMyLeaveRequests(studentId)
        if (leaveRes.code === 200 && leaveRes.data) {
          const approvedLeaves = leaveRes.data.filter(leave => 
            leave.auditStatus === 'approved' && leave.status !== 'completed'
          )
          
          // 将请假记录转换为待办事项格式
          const leaveTodos = approvedLeaves.map(leave => {
            const startDate = leave.startDate ? new Date(leave.startDate) : null
            const endDate = leave.endDate ? new Date(leave.endDate) : null
            let leaveTimeText = ''
            if (startDate && endDate) {
              leaveTimeText = `${startDate.toLocaleDateString('zh-CN')} 至 ${endDate.toLocaleDateString('zh-CN')}（${leave.days || 0}天）`
            } else if (startDate) {
              leaveTimeText = startDate.toLocaleDateString('zh-CN')
            }
            
            return {
              id: leave.id,
              type: 'leave', // 标记为请假类型
              title: `请假：${leave.reason || '请假申请'}`,
              completed: false, // 请假记录在未销假前都显示为未完成
              leaveStatus: leave.status, // 请假状态
              leaveData: leave, // 保存完整的请假数据，用于动态判断状态
              leaveTimeText: leaveTimeText,
              createTime: leave.createTime ? new Date(leave.createTime).toLocaleDateString('zh-CN') : null,
              priority: 0 // 请假记录优先级最高，置顶显示
            }
          })
          
          allTodos.push(...leaveTodos)
        }
      }
    } catch (e) {
      console.error('加载请假记录失败', e)
    }
    
    // 2. 加载日常任务
    const res = await getActiveDailyTasks()
    if (res.code === 200) {
      // 映射任务数据，保留deadline用于排序
      const mappedTasks = res.data.map(task => {
        const deadlineDate = task.deadline ? new Date(task.deadline) : null
        const createTimeDate = task.createTime ? new Date(task.createTime) : null
        
        return {
          id: task.id,
          type: 'task', // 标记为任务类型
          title: task.title,
          completed: task.completed,
          deadline: deadlineDate,
          deadlineText: deadlineDate ? deadlineDate.toLocaleDateString('zh-CN') : '长期',
          createTime: createTimeDate ? createTimeDate.toLocaleDateString('zh-CN') : null,
          taskCategory: task.taskCategory, // 任务类别
          maxParticipants: task.maxParticipants, // 最大报名人数
          currentParticipants: task.currentParticipants, // 当前报名人数
          priority: 1 // 日常任务优先级较低
        }
      })
      
      allTodos.push(...mappedTasks)
    }
    
    // 3. 排序：请假记录置顶，然后按原有逻辑排序
    todoList.value = allTodos.sort((a, b) => {
      // 先按优先级排序：请假记录（priority=0）在最前面
      if (a.priority !== b.priority) {
        return a.priority - b.priority
      }
      
      // 如果都是请假记录，按创建时间降序（最新的在前）
      if (a.type === 'leave' && b.type === 'leave') {
        const timeA = a.createTime ? new Date(a.createTime).getTime() : 0
        const timeB = b.createTime ? new Date(b.createTime).getTime() : 0
        return timeB - timeA
      }
      
      // 如果都是日常任务，按原有逻辑排序
      if (a.type === 'task' && b.type === 'task') {
        // 先按完成状态排序：未完成的在前（false < true）
        if (a.completed !== b.completed) {
          return a.completed ? 1 : -1
        }
        
        // 如果都是未完成，按截止时间升序排序（越临近越靠前）
        if (!a.completed && !b.completed) {
          if (!a.deadline && !b.deadline) return 0 // 都没有截止时间，保持原顺序
          if (!a.deadline) return 1 // a没有截止时间，排后面
          if (!b.deadline) return -1 // b没有截止时间，排后面
          return a.deadline.getTime() - b.deadline.getTime() // 升序：越早的截止时间越靠前
        }
        
        // 如果都是已完成，按截止时间降序排序（最近完成的在前）
        if (a.completed && b.completed) {
          if (!a.deadline && !b.deadline) return 0
          if (!a.deadline) return 1
          if (!b.deadline) return -1
          return b.deadline.getTime() - a.deadline.getTime() // 降序：最近完成的在前
        }
      }
      
      return 0
    })
  } catch (e) {
    console.error('加载待办事项失败', e)
  }
}

// 更新时间
const updateTime = () => {
  const now = new Date()
  currentTime.value = now.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

// 加载个人数据
const loadPersonalData = async () => {
  try {
    // 获取当前登录学生的ID（从 store 或 userInfo）
    const studentId = userInfo.value.studentId || (store.state.user && store.state.user.studentId)
    if (!studentId) {
      console.error('无法获取当前用户学号')
      // 如果无法获取学号，保持默认值0
      personalStats.value = {
        internships: 0,
        competitions: 0,
        papers: 0,
        projects: 0
      }
      return
    }
    
    const response = await getPersonalStats(studentId)
    if (response.code === 200 && response.data) {
      const data = response.data
      personalStats.value = {
        internships: data.internshipCount || data.internships || 0,
        competitions: data.competitionCount || data.competitions || 0,
        papers: data.paperCount || data.papers || 0,
        projects: data.projectCount || data.projects || 0
      }
    } else {
      // API返回错误，保持默认值0
      console.error('获取个人统计数据失败:', response.msg)
      personalStats.value = {
        internships: 0,
        competitions: 0,
        papers: 0,
        projects: 0
      }
    }
  } catch (error) {
    console.error('加载个人数据失败:', error)
    // 发生异常时，保持默认值0，不显示模拟数据
    personalStats.value = {
      internships: 0,
      competitions: 0,
      papers: 0,
      projects: 0
    }
  }
}

// 加载最近活动
const loadRecentActivities = async () => {
  try {
    const res = await getRecentActivities()
    if (res.code === 200 && res.data) {
      // 格式化时间显示
      recentActivities.value = res.data.map(activity => ({
        id: activity.id,
        title: activity.title,
        description: activity.description,
        time: activity.time ? formatActivityTime(activity.time) : '未知时间'
      }))
    } else {
      // 如果获取失败，显示默认欢迎信息
      recentActivities.value = [{
        id: 'welcome',
        title: '欢迎使用',
        description: '欢迎进入学科建设管理系统，您的学术成就将在这里被记录。',
        time: new Date().toLocaleDateString()
      }]
    }
  } catch (error) {
    console.error('加载最近活动失败:', error)
    // 如果获取失败，显示默认欢迎信息
    recentActivities.value = [{
      id: 'welcome',
      title: '欢迎使用',
      description: '欢迎进入学科建设管理系统，您的学术成就将在这里被记录。',
      time: new Date().toLocaleDateString()
    }]
  }
}

// 格式化活动时间
const formatActivityTime = (timeStr) => {
  if (!timeStr || timeStr === '未知时间') return '未知时间'
  try {
    // 处理不同的时间格式
    const date = new Date(timeStr)
    if (isNaN(date.getTime())) {
      // 如果不是有效日期，尝试解析其他格式
      return timeStr
    }
    return date.toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    })
  } catch (e) {
    return timeStr
  }
}

// 处理待办事项点击 - 跳转到任务页面并自动打开填写弹窗
const handleTodoClick = (todo) => {
  if (todo.type === 'leave') {
    // 请假记录，跳转到请假管理页面
    router.push({
      path: '/daily',
      query: {
        tab: 'leave' // 自动切换到请假标签页
      }
    })
  } else {
    // 日常任务，跳转到日常管理页面，并传递任务ID作为查询参数
    router.push({
      path: '/daily',
      query: {
        taskId: todo.id,
        tab: 'tasks' // 自动切换到任务标签页
      }
    })
  }
}

// 快速操作跳转
const goToInternship = () => {
  router.push('/internship')
}

const goToCompetition = () => {
  router.push('/competition')
}

const goToPaper = () => {
  router.push('/competition')
}

const goToProject = () => {
  router.push('/competition')
}

const goToProfile = () => {
  router.push('/profile')
}

onMounted(async () => {
  updateTime()
  await loadUserInfo() // 先加载用户完整信息（包含专业和年级）
  // 确保用户信息加载完成后再加载个人统计数据（需要studentId）
  await loadPersonalData()
  loadTodoList()
  loadRecentActivities() // 加载最近活动
  
  // 每秒更新时间
  setInterval(updateTime, 1000)
})
</script>

<style scoped>
.home {
  min-height: 100%;
  padding: 20px;
}

.welcome-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.welcome-info h2 {
  margin: 0 0 10px 0;
  color: #303133;
}

.welcome-info p {
  margin: 0;
  color: #606266;
  font-size: 14px;
}

.welcome-time p {
  margin: 0;
  color: #909399;
  font-size: 14px;
}

.stat-card {
  height: 120px;
}

.stat-content {
  display: flex;
  align-items: center;
  height: 100%;
}

.stat-icon {
  font-size: 48px;
  color: #409EFF;
  margin-right: 20px;
}

.stat-info {
  flex: 1;
}

.stat-number {
  font-size: 32px;
  font-weight: bold;
  color: #303133;
  line-height: 1;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 8px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.todo-header {
  position: relative;
  display: inline-block;
}

.todo-badge {
  display: inline-block;
  min-width: 18px;
  height: 18px;
  line-height: 18px;
  padding: 0 6px;
  background-color: #f56c6c;
  color: #fff;
  border-radius: 9px;
  font-size: 12px;
  font-weight: bold;
  text-align: center;
  margin-left: 8px;
  vertical-align: middle;
  box-shadow: 0 0 0 2px rgba(245, 108, 108, 0.2);
}

.todo-item-dot {
  position: absolute;
  top: -2px;
  right: -2px;
  width: 8px;
  height: 8px;
  background-color: #f56c6c;
  border-radius: 50%;
  border: 2px solid #fff;
  box-shadow: 0 0 0 1px rgba(245, 108, 108, 0.3);
  flex-shrink: 0;
  z-index: 1;
}

.todo-list {
  max-height: 300px;
  overflow-y: auto;
}

.todo-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: all 0.3s;
  border-radius: 4px;
  margin-bottom: 4px;
}

.todo-item:hover {
  background-color: #f5f7fa;
  transform: translateX(4px);
}

.todo-item.todo-completed {
  opacity: 0.6;
}

.todo-item:last-child {
  border-bottom: none;
}

.todo-tabs {
  margin-bottom: 12px;
  display: flex;
  justify-content: flex-start;
}

.todo-tabs .tab-count {
  margin-left: 4px;
  font-size: 12px;
  opacity: 0.8;
}

.todo-empty {
  padding: 20px;
  text-align: center;
}

.todo-content {
  display: flex;
  align-items: center;
  flex: 1;
  gap: 8px;
}

.todo-icon-wrapper {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.todo-icon {
  font-size: 18px;
}

.todo-icon.completed {
  color: #67C23A;
}

.todo-icon.pending {
  color: #E6A23C;
}

.todo-title {
  flex: 1;
  color: #303133;
}

.todo-time-info {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
  font-size: 12px;
}

.todo-time-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.time-label {
  color: #909399;
}

.time-value {
  color: #606266;
  font-weight: 500;
}

.quick-actions {
  display: flex;
  gap: 15px;
  flex-wrap: wrap;
}

.quick-actions .el-button {
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  gap: 6px !important;
}

.quick-action-btn {
  flex: 1;
  min-width: 120px;
}

.quick-action-btn :deep(.el-button__inner) {
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  width: 100%;
  gap: 6px;
}

.btn-text {
  margin-left: 0 !important;
  flex-shrink: 0;
}

/* 移动端响应式设计 */
@media (max-width: 768px) {
  .home {
    padding: 12px;
  }

  /* 移动端调整待办事项和最近活动的显示顺序 */
  .main-content-row {
    display: flex;
    flex-direction: column;
  }

  .todo-list-col {
    order: 1;
  }

  .recent-activities-col {
    order: 2;
  }

  .welcome-section {
    flex-direction: column;
    align-items: flex-start;
  }

  .welcome-info h2 {
    font-size: 20px;
    margin-bottom: 8px;
  }

  .welcome-info p {
    font-size: 12px;
    line-height: 1.5;
  }

  .welcome-time {
    width: 100%;
    margin-top: 8px;
  }

  .welcome-time p {
    font-size: 12px;
  }

  .stat-card {
    height: 100px;
    margin-bottom: 10px;
  }

  .stat-icon {
    font-size: 36px;
    margin-right: 12px;
  }

  .stat-number {
    font-size: 24px;
  }

  .stat-label {
    font-size: 12px;
    margin-top: 4px;
  }

  .todo-item {
    flex-direction: column;
    align-items: flex-start;
    padding: 10px;
  }

  .todo-content {
    width: 100%;
    margin-bottom: 8px;
  }

  .todo-time-info {
    width: 100%;
    align-items: flex-start;
    gap: 6px;
  }

  .todo-time-item {
    font-size: 11px;
  }

  /* 移动端快速操作按钮样式 - 独立样式，不影响桌面端 */
  .quick-actions {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 12px;
    width: 100%;
  }

  .quick-actions .quick-action-btn {
    width: 100%;
    max-width: 100%;
    min-width: 100%;
    margin: 0;
    padding: 14px 20px;
    font-size: 15px;
    border-radius: 8px;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
  }

  .quick-actions .quick-action-btn :deep(.el-button__inner) {
    display: flex !important;
    align-items: center !important;
    justify-content: center !important;
    width: 100%;
    gap: 8px;
    margin: 0;
    padding: 0;
  }

  .quick-actions .quick-action-btn :deep(.el-icon) {
    font-size: 20px;
    margin: 0 !important;
    padding: 0;
    flex-shrink: 0;
  }

  .quick-actions .quick-action-btn .btn-text {
    font-size: 15px;
    font-weight: 500;
    margin: 0 !important;
    padding: 0;
    flex-shrink: 0;
    line-height: 1.5;
  }

  .card-header {
    font-size: 14px;
  }

  .todo-header {
    font-size: 14px;
  }

  .more-actions {
    text-align: center;
    margin-top: 15px;
    padding: 10px 0;
  }

  .todo-badge {
    min-width: 16px;
    height: 16px;
    line-height: 16px;
    font-size: 11px;
    padding: 0 4px;
  }

  .todo-title {
    font-size: 13px;
  }
}

@media (max-width: 480px) {
  .home {
    padding: 8px;
  }

  .stat-card {
    height: 90px;
  }

  .stat-icon {
    font-size: 32px;
    margin-right: 10px;
  }

  .stat-number {
    font-size: 20px;
  }

  .stat-label {
    font-size: 11px;
  }

  /* 小屏移动端快速操作按钮样式 */
  .quick-actions {
    gap: 10px;
  }

  .quick-actions .quick-action-btn {
    padding: 12px 18px;
    font-size: 14px;
  }

  .quick-actions .quick-action-btn :deep(.el-icon) {
    font-size: 18px;
  }

  .quick-actions .quick-action-btn .btn-text {
    font-size: 14px;
  }

  .todo-item {
    padding: 8px;
  }

  .todo-title {
    font-size: 12px;
  }

  .todo-time-item {
    font-size: 10px;
  }
}
</style> 
