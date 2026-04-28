<template>
  <div class="page-wrap">
    <!-- Hero -->
    <section class="hero">
      <div class="hero-inner container">
        <div>
          <div class="hero-title">任务完成度分析</div>
          <div class="hero-sub">查看每个学生的普通任务完成情况</div>
        </div>
      </div>
    </section>

    <div class="container">
      <!-- 筛选和操作栏 -->
      <el-card class="glass" shadow="hover" style="margin-bottom: 20px;">
        <div class="toolbar">
          <div class="toolbar-left">
            <el-select
              v-model="selectedGrade"
              placeholder="选择年级（不选择显示全部）"
              clearable
              style="width: 200px; margin-right: 12px;"
              @change="handleGradeChange"
            >
              <el-option
                v-for="grade in gradeList"
                :key="grade"
                :label="grade"
                :value="grade"
              />
            </el-select>
            <el-button type="primary" :icon="Download" @click="handleExport">导出Excel</el-button>
          </div>
          <div class="toolbar-right">
            <el-input
              v-model="searchKey"
              placeholder="搜索学号/姓名"
              clearable
              style="width: 240px"
              @keyup.enter="applySearch"
              @clear="applySearch"
            />
          </div>
        </div>
      </el-card>

      <!-- 统计卡片 -->
      <el-row :gutter="20" style="margin-bottom: 20px;">
        <el-col :xs="24" :sm="12" :md="6" :lg="6">
          <el-card class="stat-card">
            <div class="stat-title">总学生数</div>
            <div class="stat-value">{{ filteredAnalysis.length }}</div>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12" :md="6" :lg="6">
          <el-card class="stat-card">
            <div class="stat-title">平均完成度</div>
            <div class="stat-value">{{ averageCompletionRate }}%</div>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12" :md="6" :lg="6">
          <el-card class="stat-card">
            <div class="stat-title">全部完成</div>
            <div class="stat-value success">{{ fullyCompletedCount }}</div>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12" :md="6" :lg="6">
          <el-card class="stat-card">
            <div class="stat-title">有未完成任务</div>
            <div class="stat-value warning">{{ incompleteCount }}</div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 按年级分组显示 -->
      <el-card class="glass" shadow="hover">
        <template #header>
          <div class="card-header">
            <div class="card-title">学生任务完成度列表</div>
          </div>
        </template>

        <el-table :data="groupedAnalysis" style="width: 100%" border>
          <el-table-column type="expand">
            <template #default="{ row }">
              <div class="grade-group-detail">
                <el-table :data="row.students" style="width: 100%" border>
                  <el-table-column prop="studentId" label="学号" min-width="120" />
                  <el-table-column prop="name" label="姓名" min-width="100" />
                  <el-table-column prop="major" label="专业" min-width="150" />
                  <el-table-column prop="className" label="班级" min-width="120" />
                  <el-table-column label="需完成任务数" min-width="120" align="center">
                    <template #default="scope">
                      <el-tag type="info" effect="plain">{{ scope.row.totalRequired }}</el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column label="已完成数" min-width="100" align="center">
                    <template #default="scope">
                      <el-tag type="success" effect="plain">{{ scope.row.completedCount }}</el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column label="未完成数" min-width="100" align="center">
                    <template #default="scope">
                      <el-tag type="danger" effect="plain">{{ scope.row.incompleteCount }}</el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column label="完成度" min-width="150" align="center">
                    <template #default="scope">
                      <el-progress 
                        :percentage="scope.row.completionRate" 
                        :color="getProgressColor(scope.row.completionRate)"
                        :stroke-width="20"
                      />
                    </template>
                  </el-table-column>
                  <el-table-column label="操作" min-width="150" align="center">
                    <template #default="scope">
                      <el-button 
                        type="primary" 
                        link 
                        size="small"
                        @click="viewIncompleteTasks(scope.row)"
                      >
                        查看未完成任务
                      </el-button>
                    </template>
                  </el-table-column>
                </el-table>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="grade" label="年级" min-width="120" />
          <el-table-column label="学生数" min-width="100" align="center">
            <template #default="scope">
              {{ scope.row.students.length }}
            </template>
          </el-table-column>
          <el-table-column label="平均完成度" min-width="150" align="center">
            <template #default="scope">
              <el-progress 
                :percentage="scope.row.averageRate" 
                :color="getProgressColor(scope.row.averageRate)"
                :stroke-width="20"
              />
            </template>
          </el-table-column>
          <el-table-column label="全部完成" min-width="100" align="center">
            <template #default="scope">
              <el-tag type="success" effect="plain">
                {{ scope.row.fullyCompleted }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="有未完成" min-width="100" align="center">
            <template #default="scope">
              <el-tag type="warning" effect="plain">
                {{ scope.row.incomplete }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" min-width="150" align="center">
            <template #default="scope">
              <el-button 
                type="primary" 
                link 
                size="small"
                @click="exportGradeAnalysis(scope.row.grade)"
              >
                导出该年级
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>

    <!-- 未完成任务弹窗 -->
    <el-dialog v-model="incompleteTasksVisible" :title="`${currentStudentName} 的未完成任务`" width="800px">
      <el-table :data="incompleteTasks" style="width: 100%" border>
        <el-table-column prop="taskTitle" label="任务标题" min-width="200" />
        <el-table-column prop="description" label="描述" min-width="250" show-overflow-tooltip />
        <el-table-column label="截止时间" width="180">
          <template #default="scope">
            {{ fmtDate(scope.row.deadline) || '无' }}
          </template>
        </el-table-column>
        <el-table-column label="发布时间" width="180">
          <template #default="scope">
            {{ fmtDate(scope.row.createTime) || '无' }}
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Download } from '@element-plus/icons-vue'
import { getTaskCompletionAnalysis, exportTaskCompletionAnalysis, getStudentIncompleteTasks } from '@/api/daily'
import { getAllGrades } from '@/api/student'

const gradeList = ref([])
const selectedGrade = ref(null)
const searchKey = ref('')
const analysisData = ref([])
const incompleteTasksVisible = ref(false)
const incompleteTasks = ref([])
const currentStudentName = ref('')

// 加载年级列表
const loadGradeList = async () => {
  try {
    const res = await getAllGrades()
    if (res.code === 200) {
      gradeList.value = res.data || []
    }
  } catch (e) {
    console.error('加载年级列表失败', e)
  }
}

// 加载任务完成度分析数据
const loadAnalysis = async () => {
  try {
    const res = await getTaskCompletionAnalysis(selectedGrade.value)
    if (res.code === 200) {
      analysisData.value = res.data || []
    }
  } catch (e) {
    ElMessage.error('加载任务完成度分析失败')
  }
}

// 过滤后的分析数据
const filteredAnalysis = computed(() => {
  if (!searchKey.value) return analysisData.value
  const key = searchKey.value.trim().toLowerCase()
  return analysisData.value.filter(item =>
    item.studentId?.toLowerCase().includes(key) ||
    item.name?.toLowerCase().includes(key)
  )
})

// 按年级分组
const groupedAnalysis = computed(() => {
  const groups = {}
  
  filteredAnalysis.value.forEach(item => {
    const grade = item.grade || '未设置年级'
    if (!groups[grade]) {
      groups[grade] = {
        grade,
        students: []
      }
    }
    groups[grade].students.push(item)
  })
  
  // 计算每个年级的统计信息
  return Object.values(groups).map(group => {
    const students = group.students
    const totalRate = students.reduce((sum, s) => sum + (s.completionRate || 0), 0)
    const averageRate = students.length > 0 ? Math.round(totalRate / students.length * 100) / 100 : 0
    const fullyCompleted = students.filter(s => s.incompleteCount === 0).length
    const incomplete = students.filter(s => s.incompleteCount > 0).length
    
    return {
      ...group,
      averageRate,
      fullyCompleted,
      incomplete
    }
  }).sort((a, b) => {
    // 按年级排序（降序）
    return b.grade.localeCompare(a.grade)
  })
})

// 平均完成度
const averageCompletionRate = computed(() => {
  if (filteredAnalysis.value.length === 0) return 0
  const total = filteredAnalysis.value.reduce((sum, item) => sum + (item.completionRate || 0), 0)
  return Math.round(total / filteredAnalysis.value.length * 100) / 100
})

// 全部完成的学生数
const fullyCompletedCount = computed(() => {
  return filteredAnalysis.value.filter(item => item.incompleteCount === 0).length
})

// 有未完成任务的学生数
const incompleteCount = computed(() => {
  return filteredAnalysis.value.filter(item => item.incompleteCount > 0).length
})

// 获取进度条颜色
const getProgressColor = (percentage) => {
  if (percentage >= 80) return '#67c23a'
  if (percentage >= 60) return '#e6a23c'
  if (percentage >= 40) return '#f56c6c'
  return '#909399'
}

// 年级切换
const handleGradeChange = () => {
  loadAnalysis()
}

// 搜索
const applySearch = () => {
  // 搜索逻辑已在computed中实现
}

// 导出Excel
const handleExport = async () => {
  try {
    const res = await exportTaskCompletionAnalysis(selectedGrade.value)
    const url = window.URL.createObjectURL(new Blob([res]))
    const link = document.createElement('a')
    link.href = url
    const filename = selectedGrade.value 
      ? `任务完成度分析_${selectedGrade.value}.xlsx`
      : '任务完成度分析_全部.xlsx'
    link.setAttribute('download', filename)
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    ElMessage.success('导出成功')
  } catch (e) {
    ElMessage.error('导出失败')
  }
}

// 导出指定年级
const exportGradeAnalysis = async (grade) => {
  try {
    const res = await exportTaskCompletionAnalysis(grade)
    const url = window.URL.createObjectURL(new Blob([res]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', `任务完成度分析_${grade}.xlsx`)
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    ElMessage.success('导出成功')
  } catch (e) {
    ElMessage.error('导出失败')
  }
}

// 查看未完成任务
const viewIncompleteTasks = async (student) => {
  try {
    // 需要根据studentId获取学生ID（MongoDB的_id）
    // 这里需要先通过studentId查找学生的完整信息
    const res = await getStudentIncompleteTasks(student.studentId)
    if (res.code === 200) {
      incompleteTasks.value = res.data || []
      currentStudentName.value = student.name
      incompleteTasksVisible.value = true
    }
  } catch (e) {
    ElMessage.error('获取未完成任务失败')
  }
}

// 格式化日期
const fmtDate = (d) => {
  if (!d) return ''
  const date = (d instanceof Date) ? d : new Date(d)
  if (Number.isNaN(+date)) return String(d)
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

onMounted(() => {
  loadGradeList()
  loadAnalysis()
})
</script>

<style scoped>
.page-wrap {
  min-height: 100vh;
  background: linear-gradient(180deg, #f7f9ff, #ffffff 40%);
}

.container {
  max-width: 1600px;
  margin: 0 auto;
  padding: 20px;
}

.hero {
  padding: 28px 0;
  background: radial-gradient(1200px 220px at 50% -40px, #e8eeff, transparent);
}

.hero-inner {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
}

.hero-title {
  font-size: 26px;
  font-weight: 700;
  color: #1f2d3d;
}

.hero-sub {
  color: #6b7280;
  margin-top: 6px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.toolbar-left,
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.stat-card {
  text-align: center;
}

.stat-title {
  font-size: 14px;
  color: #909399;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #303133;
}

.stat-value.success {
  color: #67c23a;
}

.stat-value.warning {
  color: #e6a23c;
}

.grade-group-detail {
  padding: 10px;
}

@media (max-width: 768px) {
  .toolbar {
    flex-direction: column;
    align-items: stretch;
  }
  
  .toolbar-left,
  .toolbar-right {
    width: 100%;
  }
}
</style>
