<template>
  <div class="admin-dashboard">
    <!-- 统计卡片 -->
    <el-row :gutter="20" class="statistics-cards">
      <el-col :xs="12" :sm="12" :md="6" :lg="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-icon">
              <el-icon><User /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-number">{{ statistics.totalStudents || 0 }}</div>
              <div class="stat-label">学生总数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="12" :md="6" :lg="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-icon internship">
              <el-icon><Briefcase /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-number">{{ statistics.internshipCount || 0 }}</div>
              <div class="stat-label">实习记录</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="12" :md="6" :lg="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-icon competition">
              <el-icon><Trophy /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-number">{{ statistics.competitionCount || 0 }}</div>
              <div class="stat-label">竞赛记录</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="12" :md="6" :lg="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-icon alumni">
              <el-icon><Connection /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-number">{{ statistics.alumniCount || 0 }}</div>
              <div class="stat-label">校友数量</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表和详细信息 -->
    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :xs="24" :sm="24" :md="16" :lg="16">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>学生专业分布</span>
            </div>
          </template>
          <div class="chart-container">
            <div ref="majorChart" style="height: 300px;"></div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="24" :md="8" :lg="8">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>系统概览</span>
            </div>
          </template>
          <div class="overview-list">
            <div class="overview-item">
              <span class="overview-label">在线用户</span>
              <span class="overview-value">{{ systemInfo.onlineUsers }}</span>
            </div>
            <div class="overview-item">
              <span class="overview-label">今日访问</span>
              <span class="overview-value">{{ systemInfo.todayVisits }}</span>
            </div>
            <div class="overview-item">
              <span class="overview-label">系统运行时间</span>
              <span class="overview-value">{{ systemInfo.uptime }}</span>
            </div>
            <div class="overview-item">
              <span class="overview-label">数据库状态</span>
              <span class="overview-value status-normal">正常</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 最新动态和待审批 -->
    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :xs="24" :sm="24" :md="12" :lg="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>最新动态</span>
            </div>
          </template>
          <el-timeline v-if="recentActivities.length > 0">
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
          <div v-if="recentActivities.length > maxRecentActivities" class="more-actions" style="text-align: center; margin-top: 10px;">
            <el-button text type="primary" @click="showAllRecentActivities = !showAllRecentActivities">
              {{ showAllRecentActivities ? '收起' : `查看更多 (${recentActivities.length - maxRecentActivities}条)` }}
            </el-button>
          </div>
          <el-empty v-else-if="recentActivities.length === 0" description="暂无最近活动" :image-size="80" />
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="24" :md="12" :lg="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>待审批事项</span>
            </div>
          </template>
          <div class="approval-list">
            <div
              v-if="pendingApprovals.length === 0"
              class="empty-approvals"
            >
              <el-empty description="暂无待审批事项" :image-size="80" />
            </div>
            <div
              v-for="approval in pendingApprovals"
              :key="approval.id"
              class="approval-item"
            >
              <div class="approval-info">
                <div class="approval-title">{{ approval.title }}</div>
                <div class="approval-desc">{{ approval.description }}</div>
                <div class="approval-time">{{ approval.time }}</div>
              </div>
              <div class="approval-actions">
                <el-button size="small" type="success" @click="handleApprove(approval)">
                  通过
                </el-button>
                <el-button size="small" type="danger" @click="handleReject(approval)">
                  拒绝
                </el-button>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 快速操作 -->
    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="24">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>快速操作</span>
            </div>
          </template>
          <div class="quick-actions">
            <el-button type="primary" @click="goToStudents">
              <el-icon><User /></el-icon>
              学生管理
            </el-button>
            <el-button type="success" @click="goToInternships">
              <el-icon><Briefcase /></el-icon>
              实习就业管理
            </el-button>
            <el-button type="warning" @click="goToCompetitions">
              <el-icon><Trophy /></el-icon>
              竞赛管理
            </el-button>
            <el-button type="info" @click="goToAlumni">
              <el-icon><Connection /></el-icon>
              校友管理
            </el-button>
            <el-button @click="goToSystem">
              <el-icon><Setting /></el-icon>
              系统设置
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, computed } from 'vue'
import { useRouter } from 'vue-router'
import { 
  User, 
  Briefcase, 
  Trophy, 
  Connection, 
  Setting 
} from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import request from '@/api/request'

const router = useRouter()

// 统计数据
const statistics = ref({
  totalStudents: 0,
  internshipCount: 0,
  competitionCount: 0,
  alumniCount: 0
})

// 系统信息
const systemInfo = ref({
  onlineUsers: 0,
  todayVisits: 0,
  uptime: '系统运行中',
  dbStatus: '正常'
})

// 最近活动
const recentActivities = ref([])
const maxRecentActivities = 5 // 首页最多显示5条
const showAllRecentActivities = ref(false)

// 计算显示的最近活动（限制条数）
const displayedRecentActivities = computed(() => {
  if (showAllRecentActivities.value) {
    return recentActivities.value
  }
  return recentActivities.value.slice(0, maxRecentActivities)
})

// 待审批事项
const pendingApprovals = ref([])

// 图表引用
const majorChart = ref(null)
let chartInstance = null

// 加载统计数据
const loadStatistics = async () => {
  try {
    const res = await request.get('/admin/audit/dashboard-stats')
    
    if (res.code === 200) {
      // 统计数据
      if (res.statistics) {
        statistics.value = res.statistics
      }
      
      // 图表数据
      if (res.majorChartData && Array.isArray(res.majorChartData)) {
        // 过滤掉无效数据，但保留所有有效数据
        const validData = res.majorChartData.filter(item => {
          return item && 
                 item.name && 
                 item.name !== '暂无数据' && 
                 item.name.trim() !== '' &&
                 (item.value !== undefined && item.value !== null)
        })
        if (validData.length > 0) {
          // 使用 nextTick 确保 DOM 已更新
          nextTick(() => {
            updateChart(validData)
          })
        } else {
          // 如果没有有效数据，显示空图表提示
          nextTick(() => {
            updateChart([{ name: '暂无数据', value: 0 }])
          })
        }
      } else {
        // 如果没有数据，初始化空图表
        nextTick(() => {
          updateChart([{ name: '暂无数据', value: 0 }])
        })
      }
      
      // 最近活动
      if (res.recentActivities && Array.isArray(res.recentActivities)) {
        recentActivities.value = res.recentActivities.map(activity => ({
          ...activity,
          time: formatActivityTime(activity.time)
        }))
      } else {
        recentActivities.value = []
      }
      
      // 系统信息
      if (res.systemInfo) {
        systemInfo.value = res.systemInfo
      }
      
      // 待审批事项
      if (res.pendingApprovals && Array.isArray(res.pendingApprovals)) {
        pendingApprovals.value = res.pendingApprovals.map(approval => ({
          ...approval,
          time: formatActivityTime(approval.time)
        }))
      } else {
        pendingApprovals.value = []
      }
    }
  } catch (error) {
    // 静默处理错误
  }
}

// 格式化活动时间
const formatActivityTime = (timeStr) => {
  if (!timeStr) return '刚刚'
  try {
    const date = new Date(timeStr)
    if (isNaN(date.getTime())) {
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

// 初始化/更新图表
const updateChart = (data) => {
  if (majorChart.value) {
    if (!chartInstance) {
      chartInstance = echarts.init(majorChart.value)
    }
    
    // 确保数据格式正确
    const chartData = Array.isArray(data) && data.length > 0 
      ? data.map(item => ({
          name: item.name || '未知',
          value: Number(item.value) || 0
        }))
      : [{ value: 0, name: '暂无数据' }]
    
    const option = {
      title: {
        text: '学生专业分布',
        left: 'center'
      },
      tooltip: {
        trigger: 'item',
        formatter: '{a} <br/>{b}: {c} ({d}%)'
      },
      legend: {
        orient: 'vertical',
        left: 'left',
        data: chartData.map(item => item.name)
      },
      series: [
        {
          name: '专业分布',
          type: 'pie',
          radius: '50%',
          data: chartData,
          emphasis: {
            itemStyle: {
              shadowBlur: 10,
              shadowOffsetX: 0,
              shadowColor: 'rgba(0, 0, 0, 0.5)'
            }
          }
        }
      ]
    }
    chartInstance.setOption(option, true) // 使用 true 强制更新
  }
}

const initChart = () => {
  // 初始显示空图表
  updateChart([])
}

// 处理审批
const handleApprove = (_approval) => {
  // 这里可以调用后端API进行审批
}

const handleReject = (_approval) => {
  // 这里可以调用后端API进行审批
}

// 快速操作跳转
const goToStudents = () => {
  router.push('/admin/students/list')
}

const goToInternships = () => {
  router.push('/admin/internship/list')
}

const goToCompetitions = () => {
  router.push('/admin/competition/list')
}

const goToAlumni = () => {
  router.push('/admin/alumni/list')
}

const goToSystem = () => {
  router.push('/admin/system')
}

onMounted(() => {
  // 先初始化图表（显示空图表）
  nextTick(() => {
    initChart()
    // 然后加载数据并更新图表
    loadStatistics()
  })
})
</script>

<style scoped>
.admin-dashboard {
  padding: 20px;
}

.statistics-cards {
  margin-bottom: 20px;
}

.stat-card {
  display: flex;
  align-items: center;
  padding: 10px;
}

.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 15px;
  font-size: 24px;
  color: white;
}

.stat-icon:not(.internship):not(.competition):not(.alumni) {
  background-color: #409eff;
}

.stat-icon.internship {
  background-color: #67c23a;
}

.stat-icon.competition {
  background-color: #e6a23c;
}

.stat-icon.alumni {
  background-color: #f56c6c;
}

.stat-content {
  flex: 1;
}

.stat-number {
  font-size: 24px;
  font-weight: bold;
  color: #333;
  margin-bottom: 5px;
}

.stat-label {
  font-size: 14px;
  color: #666;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chart-container {
  height: 300px;
}

.overview-list {
  max-height: 300px;
  overflow-y: auto;
}

.overview-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}

.overview-item:last-child {
  border-bottom: none;
}

.overview-label {
  font-size: 14px;
  color: #606266;
}

.overview-value {
  font-size: 14px;
  font-weight: bold;
  color: #303133;
}

.status-normal {
  color: #67c23a;
}

.approval-list {
  max-height: 300px;
  overflow-y: auto;
}

.approval-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}

.approval-item:last-child {
  border-bottom: none;
}

.approval-info {
  flex: 1;
}

.approval-title {
  font-size: 14px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 4px;
}

.approval-desc {
  font-size: 12px;
  color: #606266;
  margin-bottom: 4px;
}

.approval-time {
  font-size: 12px;
  color: #909399;
}

.approval-actions {
  display: flex;
  gap: 8px;
}

.quick-actions {
  display: flex;
  gap: 15px;
  flex-wrap: wrap;
}

.quick-actions .el-button {
  display: flex;
  align-items: center;
  gap: 5px;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .admin-dashboard {
    padding: 10px;
  }

  .statistics-cards {
    margin-bottom: 15px;
  }

  .stat-card {
    height: 100px;
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
  }

  .chart-container {
    height: 250px;
  }

  .chart-container div {
    height: 250px !important;
  }

  .card-header {
    font-size: 14px;
  }

  .approval-item {
    flex-direction: column;
    align-items: flex-start;
  }

  .approval-actions {
    width: 100%;
    margin-top: 10px;
    display: flex;
    gap: 10px;
  }

  .approval-actions .el-button {
    flex: 1;
  }

  .quick-actions {
    gap: 10px;
  }

  .quick-actions .el-button {
    flex: 1 1 calc(50% - 5px);
    min-width: calc(50% - 5px);
  }
}

@media (max-width: 480px) {
  .admin-dashboard {
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

  .chart-container {
    height: 200px;
  }

  .chart-container div {
    height: 200px !important;
  }

  .quick-actions .el-button {
    flex: 1 1 100%;
    min-width: 100%;
    margin-bottom: 8px;
  }
}
</style> 