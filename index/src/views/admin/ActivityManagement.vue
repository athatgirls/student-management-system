<template>
  <div class="page-wrap">
    <!-- Hero -->
    <section class="hero">
      <div class="hero-inner container">
        <div>
          <div class="hero-title">活动管理</div>
          <div class="hero-sub">创建活动并导入参与学生，系统将自动匹配到相关任务</div>
        </div>
        <el-button type="primary" :icon="Plus" @click="handleOpenCreateDialog">创建活动</el-button>
      </div>
    </section>

    <div class="container">
      <!-- 活动列表 -->
      <el-card class="glass" shadow="hover">
        <template #header>
          <div class="card-header">
            <div class="card-title">活动列表</div>
          </div>
        </template>

        <el-table :data="activities" style="width: 100%" border>
          <el-table-column prop="title" label="活动标题" min-width="200" />
          <el-table-column label="活动时间" min-width="180">
            <template #default="scope">
              {{ formatDateTime(scope.row.activityTime) }}
            </template>
          </el-table-column>
          <el-table-column prop="location" label="活动地点" min-width="150" />
          <el-table-column label="参与人数" width="100" align="center">
            <template #default="scope">
              {{ scope.row.participantStudentIds?.length || 0 }}
            </template>
          </el-table-column>
          <el-table-column label="关联任务" min-width="200">
            <template #default="scope">
              <span v-if="scope.row.taskId">{{ getTaskTitle(scope.row.taskId) }}</span>
              <span v-else style="color: #909399;">未关联</span>
            </template>
          </el-table-column>
          <el-table-column label="匹配状态" width="120" align="center">
            <template #default="scope">
              <el-tag :type="scope.row.matched ? 'success' : 'info'" effect="plain">
                {{ scope.row.matched ? '已匹配' : '未匹配' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="250" align="center" fixed="right">
            <template #default="scope">
              <el-button 
                type="primary" 
                link 
                size="small"
                @click="handleImportStudents(scope.row)"
                :disabled="scope.row.matched"
              >
                导入学生
              </el-button>
              <el-button 
                type="danger" 
                link 
                size="small"
                @click="handleDelete(scope.row)"
              >
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>

    <!-- 创建活动对话框 -->
    <el-dialog v-model="createDialogVisible" title="创建活动" width="800px">
      <el-form :model="activityForm" label-width="120px">
        <el-form-item label="任务标题" required>
          <el-input v-model="activityForm.title" placeholder="请输入任务标题" />
          <div style="font-size: 12px; color: #909399; margin-top: 5px;">
            此标题将同时作为活动标题和任务标题
          </div>
        </el-form-item>
        <el-form-item label="任务描述">
          <el-input v-model="activityForm.taskDescription" type="textarea" rows="3" placeholder="请输入任务描述" />
        </el-form-item>
        <el-form-item label="活动时间" required>
          <el-date-picker
            v-model="activityForm.activityTime"
            type="datetime"
            placeholder="选择活动时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="活动地点" required>
          <el-input v-model="activityForm.location" placeholder="请输入活动地点" />
        </el-form-item>
        <el-form-item label="截止日期" required>
          <el-date-picker
            v-model="activityForm.taskDeadline"
            type="datetime"
            placeholder="选择截止时间"
            style="width: 100%"
          />
        </el-form-item>
        
        <el-divider>任务设置</el-divider>
        
        <el-form-item label="任务方式" required>
          <el-radio-group v-model="activityForm.taskMode" @change="handleTaskModeChange">
            <el-radio label="create">创建新任务</el-radio>
            <el-radio label="link">关联已有任务</el-radio>
          </el-radio-group>
        </el-form-item>

        <!-- 创建新任务 -->
        <template v-if="activityForm.taskMode === 'create'">
          <el-form-item label="任务类别" required>
            <el-radio-group v-model="activityForm.taskCategory">
              <el-radio label="normal">普通任务</el-radio>
              <el-radio label="registration">报名型任务</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item 
            v-if="activityForm.taskCategory === 'registration'" 
            label="报名人数限制" 
            required
          >
            <el-input-number 
              v-model="activityForm.maxParticipants" 
              :min="1" 
              :max="10000"
              placeholder="请输入最大报名人数"
              style="width: 100%"
            />
          </el-form-item>
        </template>

        <!-- 关联已有任务 -->
        <template v-else>
          <el-form-item label="选择任务" required>
            <el-select
              v-model="activityForm.taskId"
              placeholder="请选择要关联的任务"
              filterable
              style="width: 100%"
              :loading="tasksLoading"
            >
              <el-option
                v-for="task in availableTasks"
                :key="task.id"
                :label="task.title"
                :value="task.id"
              >
                <span>{{ task.title }}</span>
                <span style="color: #8492a6; font-size: 13px; margin-left: 10px;">
                  ({{ task.taskCategory === 'registration' ? '报名型' : '普通任务' }})
                </span>
              </el-option>
            </el-select>
          </el-form-item>
        </template>

        <el-alert
          title="说明"
          type="info"
          :closable="false"
          show-icon
          style="margin-top: 20px;"
        >
          <template #default>
            <div>导入学生后，系统会自动将该任务标记为已完成，不影响学生的其他任务状态</div>
          </template>
        </el-alert>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreateActivity" :loading="createLoading">创建</el-button>
      </template>
    </el-dialog>

    <!-- 导入学生对话框 -->
    <el-dialog v-model="importDialogVisible" title="导入学生" width="800px">
      <div style="margin-bottom: 20px;">
        <el-alert
          title="导入说明"
          type="info"
          :closable="false"
          show-icon
        >
          <template #default>
            <div>1. 可以通过Excel导入或手动输入学生信息</div>
            <div>2. Excel格式：第一行为表头（姓名、学号），从第二行开始为数据</div>
            <div>3. 系统将自动匹配学生并为其完成相关任务</div>
          </template>
        </el-alert>
      </div>

      <el-tabs v-model="importTab">
        <el-tab-pane label="Excel导入" name="excel" :disabled="!xlsxAvailable">
          <div v-if="!xlsxAvailable" style="padding: 20px; text-align: center;">
            <el-alert
              title="Excel导入功能不可用"
              type="warning"
              :closable="false"
              show-icon
            >
              <template #default>
                <div>Excel 组件加载失败，请刷新页面后重试</div>
                  <div style="margin-top: 10px;">
                    <el-button type="primary" size="small" @click="importTab = 'manual'">切换到手动输入</el-button>
                  </div>
                <div style="margin-top: 10px; font-size: 12px; color: #909399;">
                  当前仅支持 .xlsx 文件
                </div>
              </template>
            </el-alert>
          </div>
          <el-upload
            v-else
            ref="uploadRef"
            :auto-upload="false"
            :on-change="handleFileChange"
            :file-list="fileList"
            accept=".xlsx"
            drag
          >
            <el-icon class="el-icon--upload"><upload-filled /></el-icon>
            <div class="el-upload__text">
              将文件拖到此处，或<em>点击上传</em>
            </div>
            <template #tip>
              <div class="el-upload__tip">
                只能上传 .xlsx 文件，第一行为表头（姓名、学号），从第二行开始为数据
              </div>
            </template>
          </el-upload>
        </el-tab-pane>
        <el-tab-pane label="手动输入" name="manual">
          <div style="margin-bottom: 10px;">
            <el-button type="primary" size="small" @click="addStudentRow">添加行</el-button>
            <el-button size="small" @click="clearStudentRows">清空</el-button>
          </div>
          <el-table :data="manualStudents" border style="width: 100%">
            <el-table-column label="姓名" min-width="150">
              <template #default="scope">
                <el-input v-model="scope.row.name" placeholder="请输入姓名" />
              </template>
            </el-table-column>
            <el-table-column label="学号" min-width="150">
              <template #default="scope">
                <el-input v-model="scope.row.studentId" placeholder="请输入学号" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100">
              <template #default="scope">
                <el-button type="danger" link size="small" @click="removeStudentRow(scope.$index)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>

      <template #footer>
        <el-button @click="importDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleImportSubmit" :loading="importLoading">导入并匹配</el-button>
      </template>
    </el-dialog>

    <!-- 导入结果对话框 -->
    <el-dialog v-model="resultDialogVisible" title="导入结果" width="900px">
      <el-alert
        :title="`成功匹配 ${importResult.matchedCount || 0} 人，未匹配 ${importResult.unmatchedCount || 0} 人`"
        :type="importResult.unmatchedCount > 0 ? 'warning' : 'success'"
        :closable="false"
        show-icon
        style="margin-bottom: 20px;"
      />
      
      <el-tabs v-model="resultTab">
        <el-tab-pane :label="`已匹配 (${importResult.matchedResults?.length || 0})`" name="matched">
          <el-table :data="importResult.matchedResults" border style="width: 100%">
            <el-table-column prop="name" label="姓名" width="150" />
            <el-table-column prop="studentId" label="学号" width="150" />
            <el-table-column prop="matchedTaskCount" label="匹配任务数" width="120" align="center">
              <template #default="scope">
                <el-tag type="success">{{ scope.row.matchedTaskCount }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane :label="`未匹配 (${importResult.unmatchedResults?.length || 0})`" name="unmatched">
          <el-table :data="importResult.unmatchedResults" border style="width: 100%">
            <el-table-column prop="name" label="姓名" width="150" />
            <el-table-column prop="studentId" label="学号" width="150" />
            <el-table-column prop="reason" label="原因" />
          </el-table>
        </el-tab-pane>
      </el-tabs>

      <template #footer>
        <el-button type="primary" @click="resultDialogVisible = false">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, UploadFilled } from '@element-plus/icons-vue'
import { 
  createActivity, 
  getAllActivities, 
  deleteActivity, 
  importStudentsAndMatchTasks,
  getAllDailyTasks,
  createDailyTask
} from '@/api/daily'
import { getAllGrades } from '@/api/student'
import { readExcelObjects } from '@/utils/excel'

const xlsxAvailable = true

const activities = ref([])
const createDialogVisible = ref(false)
const importDialogVisible = ref(false)
const resultDialogVisible = ref(false)
const importTab = ref('excel')
const resultTab = ref('matched')
const importLoading = ref(false)
const currentActivityId = ref('')
const fileList = ref([])
const manualStudents = ref([])
const availableTasks = ref([])
const tasksLoading = ref(false)
const createLoading = ref(false)
const gradeList = ref([])

const activityForm = reactive({
  title: '', // 任务标题（同时也是活动标题）
  taskDescription: '', // 任务描述
  activityTime: null, // 活动时间
  location: '', // 活动地点
  taskDeadline: null, // 任务截止日期
  taskMode: 'create', // 'create' 或 'link'
  // 创建新任务相关
  taskCategory: 'normal',
  maxParticipants: null,
  // 关联已有任务
  taskId: ''
})

const importResult = reactive({
  matchedCount: 0,
  unmatchedCount: 0,
  matchedResults: [],
  unmatchedResults: []
})

// 加载活动列表
const loadActivities = async () => {
  try {
    const res = await getAllActivities()
    if (res.code === 200) {
      activities.value = res.data || []
    }
  } catch (e) {
    ElMessage.error('加载活动列表失败')
  }
}

// 加载可用任务列表
const loadAvailableTasks = async () => {
  tasksLoading.value = true
  try {
    const res = await getAllDailyTasks()
    if (res.code === 200) {
      // 显示所有激活的任务（包括普通任务和报名型任务）
      availableTasks.value = (res.data || []).filter(task => task.active)
    }
  } catch (e) {
    console.error('加载任务列表失败', e)
  } finally {
    tasksLoading.value = false
  }
}

// 任务方式改变
const handleTaskModeChange = () => {
  if (activityForm.taskMode === 'link') {
    loadAvailableTasks()
  }
}

// 打开创建对话框
const handleOpenCreateDialog = () => {
  activityForm.title = ''
  activityForm.taskDescription = ''
  activityForm.activityTime = null
  activityForm.location = ''
  activityForm.taskDeadline = null
  activityForm.taskMode = 'create'
  activityForm.taskCategory = 'normal'
  activityForm.maxParticipants = null
  activityForm.taskId = ''
  loadGradeList()
  createDialogVisible.value = true
}

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

// 创建活动
const handleCreateActivity = async () => {
  // 验证基本信息
  if (!activityForm.title || !activityForm.activityTime || !activityForm.location) {
    ElMessage.warning('请填写活动基本信息')
    return
  }

  // 验证任务信息
  let taskId = null
  if (activityForm.taskMode === 'create') {
    // 创建新任务
    if (!activityForm.title || !activityForm.taskDeadline) {
      ElMessage.warning('请填写任务标题和截止日期')
      return
    }
    if (activityForm.taskCategory === 'registration' && !activityForm.maxParticipants) {
      ElMessage.warning('报名型任务需要设置报名人数限制')
      return
    }

    createLoading.value = true
    try {
      // 先创建任务
      const taskData = {
        title: activityForm.title, // 使用统一的标题
        description: activityForm.taskDescription || '',
        deadline: activityForm.taskDeadline,
        type: '信息填写',
        taskCategory: activityForm.taskCategory,
        maxParticipants: activityForm.taskCategory === 'registration' ? activityForm.maxParticipants : null,
        fields: []
      }
      
      const taskRes = await createDailyTask(taskData)
      if (taskRes.code === 200) {
        taskId = taskRes.data.id
      } else {
        ElMessage.error('创建任务失败: ' + (taskRes.msg || '未知错误'))
        createLoading.value = false
        return
      }
    } catch (e) {
      ElMessage.error('创建任务失败')
      createLoading.value = false
      return
    }
  } else {
    // 关联已有任务
    if (!activityForm.taskId) {
      ElMessage.warning('请选择要关联的任务')
      return
    }
    taskId = activityForm.taskId
  }

  // 创建活动
  createLoading.value = true
  try {
    const res = await createActivity({
      title: activityForm.title,
      activityTime: activityForm.activityTime,
      location: activityForm.location,
      taskId: taskId
    })
    if (res.code === 200) {
      ElMessage.success('活动创建成功')
      createDialogVisible.value = false
      loadActivities()
      // 如果创建了新任务，刷新任务列表
      if (activityForm.taskMode === 'create') {
        loadAvailableTasks()
      }
    }
  } catch (e) {
    ElMessage.error('创建活动失败')
  } finally {
    createLoading.value = false
  }
}

// 打开导入学生对话框
const handleImportStudents = (activity) => {
  currentActivityId.value = activity.id
  importTab.value = 'excel'
  fileList.value = []
  manualStudents.value = []
  importDialogVisible.value = true
}

// 文件变化处理
const handleFileChange = async (file) => {
  try {
      const jsonData = await readExcelObjects(file.raw)
      
      if (jsonData.length === 0) {
        ElMessage.warning('Excel文件中没有数据')
        return
      }
      
      manualStudents.value = jsonData.map(row => ({
        name: row['姓名'] || row['name'] || '',
        studentId: String(row['学号'] || row['studentId'] || '')
      })).filter(row => row.name || row.studentId)
      
      if (manualStudents.value.length === 0) {
        ElMessage.warning('未找到有效的学生数据，请检查Excel格式（需要包含"姓名"和"学号"列）')
        return
      }
      
      ElMessage.success(`成功读取 ${manualStudents.value.length} 条数据`)
      importTab.value = 'manual'
  } catch (error) {
    console.error('Excel读取错误:', error)
    ElMessage.error(error.message || '文件读取失败，请检查文件格式')
  }
}

// 添加学生行
const addStudentRow = () => {
  manualStudents.value.push({ name: '', studentId: '' })
}

// 删除学生行
const removeStudentRow = (index) => {
  manualStudents.value.splice(index, 1)
}

// 清空学生行
const clearStudentRows = () => {
  manualStudents.value = []
}

// 提交导入
const handleImportSubmit = async () => {
  let students = []
  
  if (importTab.value === 'excel') {
    if (fileList.value.length === 0) {
      ElMessage.warning('请先上传Excel文件')
      return
    }
    students = manualStudents.value
  } else {
    students = manualStudents.value.filter(s => s.name || s.studentId)
  }

  if (students.length === 0) {
    ElMessage.warning('请至少输入一条学生信息')
    return
  }

  importLoading.value = true
  try {
    const res = await importStudentsAndMatchTasks(currentActivityId.value, students)
    if (res.code === 200 && res.data.success) {
      importResult.matchedCount = res.data.matchedCount
      importResult.unmatchedCount = res.data.unmatchedCount
      importResult.matchedResults = res.data.matchedResults || []
      importResult.unmatchedResults = res.data.unmatchedResults || []
      
      importDialogVisible.value = false
      resultDialogVisible.value = true
      loadActivities()
      
      ElMessage.success('导入完成')
    } else {
      ElMessage.error(res.data?.message || '导入失败')
    }
  } catch (e) {
    ElMessage.error('导入失败')
  } finally {
    importLoading.value = false
  }
}

// 删除活动
const handleDelete = async (activity) => {
  try {
    await ElMessageBox.confirm('确定要删除该活动吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    const res = await deleteActivity(activity.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadActivities()
    }
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

// 格式化日期时间
const formatDateTime = (dateTime) => {
  if (!dateTime) return '-'
  const date = new Date(dateTime)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 获取任务标题
const getTaskTitle = (taskId) => {
  const task = availableTasks.value.find(t => t.id === taskId)
  return task ? task.title : taskId
}

onMounted(() => {
  loadActivities()
  loadAvailableTasks()
})
</script>

<style scoped>
.page-wrap {
  min-height: 100vh;
  background: #f0f2f5;
}

.hero {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 40px 0;
  margin-bottom: 20px;
}

.hero-inner {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.hero-title {
  font-size: 28px;
  font-weight: 700;
  margin-bottom: 8px;
}

.hero-sub {
  font-size: 14px;
  opacity: 0.9;
}

.container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 20px;
}

.glass {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 12px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}
</style>
