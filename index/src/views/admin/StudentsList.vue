<template>
  <div class="students-list">
    <div class="students-layout">
      <!-- 左侧年级边栏 -->
      <div class="grade-sidebar">
        <div class="grade-header">
          <h3>年级管理</h3>
          <el-button type="primary" size="small" @click="showCreateGradeDialog = true">
            <el-icon><Plus /></el-icon>
            创建年级
          </el-button>
        </div>
        <div class="grade-list">
          <div 
            class="grade-item"
            :class="{ active: selectedGrade === '' }"
            @click="selectGrade('')"
          >
            <span>全部学生</span>
          </div>
          <div 
            v-for="grade in gradeList" 
            :key="grade"
            class="grade-item"
            :class="{ active: selectedGrade === grade }"
            @click="selectGrade(grade)"
          >
            <span>{{ grade }}</span>
            <el-badge 
              :value="getGradeStudentCount(grade)" 
              :max="99"
              class="grade-badge"
            />
          </div>
        </div>
      </div>

      <!-- 主内容区 -->
      <div class="main-content">
        <el-card>
      <template #header>
        <div class="card-header">
          <span>学生管理</span>
          <div class="header-btns">
            <el-button 
              type="warning" 
              :disabled="!selectedGrade"
              @click="showBatchStatusDialog = true"
            >
              批量修改状态
            </el-button>
            <el-button 
              type="danger" 
              :disabled="selectedStudents.length === 0"
              @click="handleBatchDelete"
            >
              批量删除 ({{ selectedStudents.length }})
            </el-button>
            <el-button 
              type="success" 
              @click="handleImport"
              :disabled="!selectedGrade"
            >
              导入学生
            </el-button>
            <el-button type="primary" @click="handleAdd">添加学生</el-button>
          </div>
        </div>
      </template>
      
      <!-- 搜索栏 -->
      <div class="search-bar">
        <el-row :gutter="isMobile ? 0 : 20">
          <el-col :xs="24" :sm="12" :md="6" :lg="6">
            <el-input
              v-model="searchForm.name"
              placeholder="请输入学生姓名"
              clearable
              @keyup.enter="handleSearch"
            />
          </el-col>
          <el-col :xs="24" :sm="12" :md="6" :lg="6">
            <el-input
              v-model="searchForm.studentId"
              placeholder="请输入学号"
              clearable
              @keyup.enter="handleSearch"
            />
          </el-col>
          <el-col :xs="24" :sm="12" :md="6" :lg="6">
            <el-select v-model="searchForm.major" placeholder="请选择专业" clearable style="width: 100%">
              <el-option label="计算机科学与技术" value="计算机科学与技术" />
              <el-option label="软件工程" value="软件工程" />
              <el-option label="信息安全" value="信息安全" />
            </el-select>
          </el-col>
          <el-col :xs="24" :sm="24" :md="6" :lg="6">
            <div :class="isMobile ? 'mobile-button-group' : ''">
              <el-button type="primary" @click="handleSearch" class="search-btn">搜索</el-button>
              <el-button @click="handleReset" class="reset-btn">重置</el-button>
            </div>
          </el-col>
        </el-row>
      </div>
      
      <!-- 学生列表 -->
      <div class="table-wrapper" :class="{ 'mobile-table': isMobile }">
      <el-table
        :data="students"
        v-loading="loading"
          :style="isMobile ? 'min-width: 500px;' : 'width: 100%'"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" :width="isMobile ? 50 : 55" :class-name="isMobile ? 'hidden-mobile' : ''" />
        <el-table-column prop="studentId" label="学号" :width="isMobile ? 130 : 120" class-name="col-student-id" show-overflow-tooltip />
        <el-table-column prop="name" label="姓名" :width="isMobile ? 80 : 100" class-name="col-name" show-overflow-tooltip />
        <el-table-column prop="major" label="专业" :min-width="isMobile ? 0 : 150" :class-name="isMobile ? 'hidden-mobile' : ''" show-overflow-tooltip />
        <el-table-column prop="grade" label="年级" :width="isMobile ? 70 : 100" class-name="col-grade" />
        <el-table-column prop="className" label="班级" :min-width="isMobile ? 0 : 120" :class-name="isMobile ? 'hidden-mobile' : ''" />
        <el-table-column prop="dormitory" label="宿舍" :min-width="isMobile ? 0 : 120" :class-name="isMobile ? 'hidden-mobile' : ''" show-overflow-tooltip />
        <el-table-column prop="gender" label="性别" :width="isMobile ? 50 : 60" class-name="col-gender" />
        <el-table-column prop="age" label="年龄" :min-width="isMobile ? 0 : 60" :class-name="isMobile ? 'hidden-mobile' : ''" />
        <el-table-column prop="phone" label="手机号" :min-width="isMobile ? 0 : 130" :class-name="isMobile ? 'hidden-mobile' : ''" show-overflow-tooltip />
        <el-table-column prop="email" label="邮箱" :min-width="isMobile ? 0 : 200" :class-name="isMobile ? 'hidden-mobile' : ''" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" :width="isMobile ? 60 : 80" class-name="col-status">
          <template #default="scope">
            <el-tag :type="scope.row.status === '在读' ? 'success' : 'info'" :size="isMobile ? 'small' : 'default'">
              {{ scope.row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" :width="isMobile ? 120 : 280" fixed="right">
          <template #default="scope">
            <el-button 
              v-if="!isMobile"
              size="small" 
              @click="handleEdit(scope.row)"
            >编辑</el-button>
            <el-button 
              v-if="!isMobile"
              size="small" 
              type="warning" 
              @click="handleResetPassword(scope.row)"
            >重置密码</el-button>
            <el-button 
              v-if="!isMobile"
              size="small" 
              type="danger" 
              @click="handleDelete(scope.row)"
            >删除</el-button>
            <el-dropdown v-else @command="(cmd) => handleMobileAction(cmd, scope.row)">
              <el-button size="small" type="primary">
                操作<el-icon class="el-icon--right"><arrow-down /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="edit">编辑</el-dropdown-item>
                  <el-dropdown-item command="resetPassword">重置密码</el-dropdown-item>
                  <el-dropdown-item command="delete" divided>删除</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
      </el-table>
      </div>
      
      <!-- 分页 -->
      <div class="pagination">
        <el-pagination
          :current-page="pagination.currentPage"
          :page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>
      </div>
    </div>
    
    <!-- 添加/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="800px"
      @close="handleDialogClose"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
      >
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="学号" prop="studentId">
              <el-input v-model="form.studentId" placeholder="请输入学号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="姓名" prop="name">
              <el-input v-model="form.name" placeholder="请输入姓名" />
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="性别" prop="gender">
              <el-select v-model="form.gender" placeholder="请选择性别">
                <el-option label="男" value="男" />
                <el-option label="女" value="女" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="年龄" prop="age">
              <el-input-number v-model="form.age" :min="0" :max="100" />
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="专业" prop="major">
              <el-select v-model="form.major" placeholder="请选择专业">
                <el-option label="计算机科学与技术" value="计算机科学与技术" />
                <el-option label="软件工程" value="软件工程" />
                <el-option label="信息安全" value="信息安全" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="年级" prop="grade">
              <el-input v-model="form.grade" placeholder="请输入年级" />
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="班级" prop="className">
              <el-input v-model="form.className" placeholder="请输入班级" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="form.phone" placeholder="请输入手机号" />
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="form.email" placeholder="请输入邮箱" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="宿舍信息" prop="dormitory">
              <el-input v-model="form.dormitory" placeholder="如：南苑 3 栋 502" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
           <el-col :span="12">
             <el-form-item label="状态" prop="status">
               <el-select v-model="form.status" placeholder="请选择状态" @change="handleStatusChange">
                 <el-option label="在读" value="在读" />
                 <el-option label="毕业" value="毕业" />
                 <el-option label="休学" value="休学" />
               </el-select>
             </el-form-item>
           </el-col>
         </el-row>
         
         <el-form-item 
           v-if="form.status && form.status !== '在读'"
           label="状态备注" 
           prop="statusRemark"
           :rules="form.status !== '在读' ? [{ required: true, message: '请输入状态备注', trigger: 'blur' }] : []"
         >
           <el-input 
             v-model="form.statusRemark" 
             type="textarea" 
             :rows="3"
             placeholder="请输入状态备注信息（必填）"
           />
         </el-form-item>
         
         <el-form-item label="密码" prop="password" v-if="!form.id">
           <el-input v-model="form.password" type="password" placeholder="请输入密码" />
         </el-form-item>
      </el-form>
      
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSubmit">确定</el-button>
        </span>
      </template>
    </el-dialog>
    
    <!-- 导入学生对话框 -->
    <el-dialog
      v-model="showImportDialog"
      title="导入学生"
      width="500px"
    >
      <el-alert
        v-if="selectedGrade"
        :title="`将导入到：${selectedGrade}`"
        type="info"
        :closable="false"
        style="margin-bottom: 20px;"
      />
      <el-form :model="importForm" label-width="100px">
        <el-form-item label="选择文件" required>
          <el-upload
            ref="uploadRef"
            action="#"
            :auto-upload="false"
            :show-file-list="true"
            :on-change="handleFileChange"
            accept=".xlsx,.xls"
            :limit="1"
          >
            <template #trigger>
              <el-button type="primary">选择Excel文件</el-button>
            </template>
            <template #tip>
              <div class="el-upload__tip">
                只能上传 .xlsx 或 .xls 文件，且仅需包含"学号"和"姓名"列
              </div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showImportDialog = false">取消</el-button>
          <el-button 
            type="primary" 
            @click="handleImportSubmit"
            :disabled="!selectedGrade || !importForm.file"
            :loading="importLoading"
          >
            确定导入
          </el-button>
        </span>
      </template>
    </el-dialog>

     <!-- 创建年级对话框 -->
     <el-dialog
       v-model="showCreateGradeDialog"
       title="创建年级"
       width="400px"
     >
       <el-form :model="gradeForm" label-width="80px">
         <el-form-item label="年级名称" required>
           <el-input 
             v-model="gradeForm.name" 
             placeholder="例如：2024级"
             @keyup.enter="handleCreateGrade"
           />
         </el-form-item>
       </el-form>
       <template #footer>
         <span class="dialog-footer">
           <el-button @click="showCreateGradeDialog = false">取消</el-button>
           <el-button type="primary" @click="handleCreateGrade">确定</el-button>
         </span>
       </template>
     </el-dialog>

     <!-- 批量修改状态对话框 -->
     <el-dialog
       v-model="showBatchStatusDialog"
       title="批量修改状态"
       width="500px"
     >
       <el-alert
         v-if="selectedGrade"
         :title="`将修改 ${selectedGrade} 所有学生的状态`"
         type="info"
         :closable="false"
         style="margin-bottom: 20px;"
       />
       <el-form :model="batchStatusForm" label-width="100px">
         <el-form-item label="新状态" required>
           <el-select v-model="batchStatusForm.status" placeholder="请选择状态" @change="handleBatchStatusChange" style="width: 100%">
             <el-option label="在读" value="在读" />
             <el-option label="毕业" value="毕业" />
             <el-option label="休学" value="休学" />
           </el-select>
         </el-form-item>
         <el-form-item 
           v-if="batchStatusForm.status && batchStatusForm.status !== '在读'"
           label="状态备注" 
           required
         >
           <el-input 
             v-model="batchStatusForm.statusRemark" 
             type="textarea" 
             :rows="4"
             placeholder="请输入状态备注信息（必填）"
           />
         </el-form-item>
       </el-form>
       <template #footer>
         <span class="dialog-footer">
           <el-button @click="showBatchStatusDialog = false">取消</el-button>
           <el-button 
             type="primary" 
             @click="handleBatchStatusSubmit"
             :disabled="!batchStatusForm.status || (batchStatusForm.status !== '在读' && !batchStatusForm.statusRemark)"
             :loading="batchStatusLoading"
           >
             确定修改
           </el-button>
         </span>
       </template>
     </el-dialog>
   </div>
 </template>

<script setup>
import { ref, reactive, onMounted, computed, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, ArrowDown } from '@element-plus/icons-vue'
import { getStudentList, createStudent, updateStudent, deleteStudent, importStudents, batchDeleteStudents, getAllGrades, batchUpdateStatus, resetStudentPassword } from '@/api/student'
import { createGrade as createGradeApi } from '@/api/grade'

const loading = ref(false)
const students = ref([])
const selectedStudents = ref([])
const dialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref()

// 检测是否为移动端
const windowWidth = ref(typeof window !== 'undefined' ? window.innerWidth : 1920)
const isMobile = computed(() => windowWidth.value <= 768)

// 年级管理
const gradeList = ref([])
const selectedGrade = ref('')
const showCreateGradeDialog = ref(false)
const gradeForm = reactive({
  name: ''
})
const gradeStudentCounts = ref({})

// 搜索表单
const searchForm = reactive({
  name: '',
  studentId: '',
  major: ''
})

// 分页
const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0
})

// 表单
const form = reactive({
  id: '',
  studentId: '',
  name: '',
  gender: '',
  age: 0,
  major: '',
  grade: '',
  className: '',
  phone: '',
  email: '',
  dormitory: '',
  status: '',
  statusRemark: '',
  password: ''
})

// 批量修改状态
const showBatchStatusDialog = ref(false)
const batchStatusLoading = ref(false)
const batchStatusForm = reactive({
  status: '',
  statusRemark: ''
})

// 表单验证规则
const rules = {
  studentId: [
    { required: true, message: '请输入学号', trigger: 'blur' }
  ],
  name: [
    { required: true, message: '请输入姓名', trigger: 'blur' }
  ],
  gender: [
    { required: true, message: '请选择性别', trigger: 'change' }
  ],
  major: [
    { required: true, message: '请选择专业', trigger: 'change' }
  ],
  grade: [
    { required: true, message: '请输入年级', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' }
  ]
}

// 加载年级列表
const loadGradeList = async () => {
  try {
    // 直接使用后端返回的合并后的年级列表（后端已经处理了标准年级覆盖非标准年级的逻辑）
    const res = await getAllGrades()
    if (res.code === 200) {
      gradeList.value = res.data || []
      // 加载每个年级的学生数量
      await loadGradeStudentCounts()
    }
  } catch (error) {
    console.error('加载年级列表失败', error)
  }
}

// 加载每个年级的学生数量
const loadGradeStudentCounts = async () => {
  const counts = {}
  for (const grade of gradeList.value) {
    try {
      const res = await getStudentList({ page: 1, size: 1, grade })
      if (res.code === 200) {
        counts[grade] = res.data.total || 0
      }
    } catch (error) {
      counts[grade] = 0
    }
  }
  gradeStudentCounts.value = counts
}

// 获取年级学生数量
const getGradeStudentCount = (grade) => {
  return gradeStudentCounts.value[grade] || 0
}

// 选择年级
const selectGrade = (grade) => {
  selectedGrade.value = grade
  pagination.currentPage = 1
  loadStudents()
}

// 创建年级
const handleCreateGrade = async () => {
  if (!gradeForm.name || !gradeForm.name.trim()) {
    ElMessage.warning('请输入年级名称')
    return
  }

  const gradeName = gradeForm.name.trim()
  
  // 检查年级是否已存在
  if (gradeList.value.includes(gradeName)) {
    ElMessage.warning('该年级已存在')
    return
  }

  try {
    // 调用后端API创建年级
    const res = await createGradeApi({ gradeName })
    if (res.code === 200) {
      // 重新加载年级列表
      await loadGradeList()
      
      // 选中新创建的年级
      selectedGrade.value = gradeName
      gradeStudentCounts.value[gradeName] = 0
      
      showCreateGradeDialog.value = false
      gradeForm.name = ''
      
      ElMessage.success('年级创建成功')
      loadStudents()
    } else {
      ElMessage.error(res.msg || '创建失败')
    }
  } catch (error) {
    console.error('创建年级失败', error)
    ElMessage.error('创建年级失败，请重试')
  }
}

// 加载学生列表
const loadStudents = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.currentPage,
      size: pagination.pageSize,
      name: searchForm.name,
      studentId: searchForm.studentId,
      major: searchForm.major,
      grade: selectedGrade.value || undefined // 空字符串时不传年级参数
    }
    const res = await getStudentList(params)
    if (res.code === 200) {
      students.value = res.data.records || []
      pagination.total = res.data.total || 0
      // 更新当前年级的学生数量
      if (selectedGrade.value) {
        gradeStudentCounts.value[selectedGrade.value] = pagination.total
      }
    }
  } catch (error) {
    ElMessage.error('加载学生列表失败')
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pagination.currentPage = 1
  loadStudents()
}

// 重置搜索
const handleReset = () => {
  Object.assign(searchForm, {
    name: '',
    studentId: '',
    major: ''
  })
  handleSearch()
}

// 选择变化
const handleSelectionChange = (selection) => {
  selectedStudents.value = selection
}

// 导入相关
const showImportDialog = ref(false)
const importLoading = ref(false)
const uploadRef = ref()
const importForm = reactive({
  file: null
})

const handleImport = () => {
  if (!selectedGrade.value) {
    ElMessage.warning('请先选择年级')
    return
  }
  showImportDialog.value = true
}

const handleFileChange = (file) => {
  importForm.file = file.raw
}

const handleImportSubmit = async () => {
  if (!selectedGrade.value) {
    ElMessage.warning('请先选择年级')
    return
  }
  if (!importForm.file) {
    ElMessage.warning('请选择文件')
    return
  }

  const formData = new FormData()
  formData.append('file', importForm.file)
  formData.append('grade', selectedGrade.value)

  importLoading.value = true
  try {
    const res = await importStudents(formData)
    if (res.code === 200) {
      ElMessage.success('导入成功')
      showImportDialog.value = false
      importForm.file = null
      uploadRef.value?.clearFiles()
      loadStudents()
      loadGradeStudentCounts()
    } else {
      ElMessage.error(res.msg || '导入失败')
    }
  } catch (error) {
    ElMessage.error('导入出错')
  } finally {
    importLoading.value = false
  }
}

// 批量删除
const handleBatchDelete = async () => {
  if (selectedStudents.value.length === 0) {
    ElMessage.warning('请选择要删除的学生')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedStudents.value.length} 个学生吗？此操作不可恢复！`,
      '批量删除确认',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    const ids = selectedStudents.value.map(s => s.id)
    const res = await batchDeleteStudents(ids)
    if (res.code === 200) {
      ElMessage.success(`成功删除 ${ids.length} 个学生`)
      selectedStudents.value = []
      loadStudents()
    } else {
      ElMessage.error(res.msg || '批量删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('批量删除失败')
    }
  }
}

// 状态变化处理
const handleStatusChange = () => {
  // 如果状态改为"在读"，清空备注
  if (form.status === '在读') {
    form.statusRemark = ''
  }
}

// 批量状态变化处理
const handleBatchStatusChange = () => {
  // 如果状态改为"在读"，清空备注
  if (batchStatusForm.status === '在读') {
    batchStatusForm.statusRemark = ''
  }
}

// 批量修改状态
const handleBatchStatusSubmit = async () => {
  if (!selectedGrade.value) {
    ElMessage.warning('请先选择年级')
    return
  }
  
  if (!batchStatusForm.status) {
    ElMessage.warning('请选择状态')
    return
  }
  
  if (batchStatusForm.status !== '在读' && !batchStatusForm.statusRemark) {
    ElMessage.warning('请输入状态备注')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定要将 ${selectedGrade.value} 的所有学生状态修改为"${batchStatusForm.status}"吗？`,
      '批量修改确认',
      {
        confirmButtonText: '确定修改',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    batchStatusLoading.value = true
    const res = await batchUpdateStatus({
      grade: selectedGrade.value,
      status: batchStatusForm.status,
      statusRemark: batchStatusForm.statusRemark || ''
    })
    
    if (res.code === 200) {
      ElMessage.success('批量修改状态成功')
      showBatchStatusDialog.value = false
      batchStatusForm.status = ''
      batchStatusForm.statusRemark = ''
      loadStudents()
      loadGradeStudentCounts()
    } else {
      ElMessage.error(res.msg || '批量修改失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('批量修改失败')
    }
  } finally {
    batchStatusLoading.value = false
  }
}

// 添加学生
const handleAdd = () => {
  dialogTitle.value = '添加学生'
  Object.assign(form, {
    id: '',
    studentId: '',
    name: '',
    gender: '',
    age: 0,
    major: '',
    grade: '',
    className: '',
    phone: '',
    email: '',
    dormitory: '',
    status: '在读',
    statusRemark: '',
    password: ''
  })
  dialogVisible.value = true
}

// 编辑学生
const handleEdit = (row) => {
  dialogTitle.value = '编辑学生'
  Object.assign(form, row)
  dialogVisible.value = true
}

// 重置密码（重置为初始密码：Hbut_学号后六位）
const handleResetPassword = async (row) => {
  try {
    const lastSix = row.studentId.length > 6 ? row.studentId.substring(row.studentId.length - 6) : row.studentId
    const defaultPassword = `Hbut_${lastSix}`
    
    await ElMessageBox.confirm(
      `确定要将学生"${row.name}"（学号：${row.studentId}）的密码重置为初始密码吗？\n初始密码：${defaultPassword}`,
      '重置密码确认',
      {
        confirmButtonText: '确定重置',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    const res = await resetStudentPassword(row.studentId, null)
    if (res.code === 200) {
      ElMessage.success({
        message: `密码已重置为初始密码：${res.data || defaultPassword}`,
        duration: 5000, // 显示5秒，让管理员有时间记录
        showClose: true
      })
    } else {
      ElMessage.error(res.msg || '密码重置失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('密码重置失败')
    }
  }
}

// 删除学生
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该学生吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    const res = await deleteStudent(row.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadStudents()
    } else {
      ElMessage.error(res.msg || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

// 移动端操作处理
const handleMobileAction = (command, row) => {
  switch (command) {
    case 'edit':
      handleEdit(row)
      break
    case 'resetPassword':
      handleResetPassword(row)
      break
    case 'delete':
      handleDelete(row)
      break
  }
}

// 提交表单
const handleSubmit = async () => {
  try {
    // 如果状态不是"在读"，验证备注是否填写
    if (form.status && form.status !== '在读' && !form.statusRemark) {
      ElMessage.warning('请输入状态备注')
      return
    }
    
    await formRef.value.validate()
    
    // 确保传递所有必要的字段
    const submitData = {
      ...form,
      // 如果状态是"在读"，清空备注
      statusRemark: form.status === '在读' ? '' : form.statusRemark,
      // 确保id字段存在（编辑时）
      id: form.id || undefined
    }
    
    let res
    if (form.id) {
      res = await updateStudent(submitData)
    } else {
      res = await createStudent(submitData)
    }
    
    if (res.code === 200) {
      ElMessage.success(form.id ? '更新成功' : '添加成功')
      dialogVisible.value = false
      loadStudents()
    } else {
      ElMessage.error(res.msg || '操作失败')
    }
  } catch (error) {
    console.error('提交失败:', error)
    ElMessage.error('表单验证失败')
  }
}

// 对话框关闭
const handleDialogClose = () => {
  formRef.value?.resetFields()
}

// 分页大小变化
const handleSizeChange = (size) => {
  pagination.pageSize = size
  loadStudents()
}

// 当前页变化
const handleCurrentChange = (page) => {
  pagination.currentPage = page
  loadStudents()
}

onMounted(async () => {
  await loadGradeList()
  loadStudents()
  // 初始化窗口宽度并监听变化
  if (typeof window !== 'undefined') {
    windowWidth.value = window.innerWidth
    const handleResize = () => {
      windowWidth.value = window.innerWidth
    }
    window.addEventListener('resize', handleResize)
    // 清理函数
    onUnmounted(() => {
      window.removeEventListener('resize', handleResize)
    })
  }
})
</script>

<style scoped>
.students-list {
  padding: 20px;
  height: calc(100vh - 60px);
  overflow: hidden;
}

.students-layout {
  display: flex;
  height: 100%;
  gap: 20px;
}

/* 左侧年级边栏 */
.grade-sidebar {
  width: 200px;
  min-width: 200px;
  background: #fff;
  border-radius: 4px;
  padding: 16px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  overflow-y: auto;
  height: 100%;
}

.grade-header {
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #e4e7ed;
}

.grade-header h3 {
  margin: 0 0 12px 0;
  font-size: 16px;
  font-weight: 500;
  color: #303133;
}

.grade-header .el-button {
  width: 100%;
}

.grade-list {
  max-height: calc(100vh - 200px);
  overflow-y: auto;
}

.grade-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  margin-bottom: 8px;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s;
  border: 1px solid transparent;
  background-color: #f5f7fa;
}

.grade-item:hover {
  background-color: #ecf5ff;
  border-color: #c0c4cc;
}

.grade-item.active {
  background-color: #409eff;
  color: #fff;
  font-weight: 500;
  border-color: #409eff;
}

.grade-item.active .grade-badge {
  background-color: rgba(255, 255, 255, 0.3);
}

.grade-badge {
  margin-left: 10px;
}

/* 主内容区 */
.main-content {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.main-content .el-card {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.main-content .el-card__body {
  flex: 1;
  overflow: auto;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-btns {
  display: flex;
  gap: 10px;
}

.import-upload {
  display: inline-block;
}

.search-bar {
  margin-bottom: 20px;
}

.table-wrapper {
  width: 100%;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
}

.table-wrapper :deep(.el-table) {
  table-layout: fixed;
}

.table-wrapper :deep(.el-table__body-wrapper) {
  overflow-x: auto;
}

/* 移动端表格样式 */
.table-wrapper.mobile-table {
  min-width: 500px;
}

.table-wrapper.mobile-table :deep(.el-table) {
  min-width: 500px;
  table-layout: fixed;
}

.table-wrapper.mobile-table :deep(.el-table th),
.table-wrapper.mobile-table :deep(.el-table td) {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mobile-button-group {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  width: 100%;
}

.mobile-button-group .search-btn,
.mobile-button-group .reset-btn {
  width: 100%;
}

.mobile-button-group .search-btn {
  margin-bottom: 10px;
}

.hidden-mobile {
  display: none !important;
}

.pagination {
  margin-top: 20px;
  text-align: right;
}

.dialog-footer {
  text-align: right;
}

/* 年级侧边栏样式 */
.grade-sidebar {
  padding: 10px;
}

.grade-header {
  margin-bottom: 20px;
}

.grade-list {
  max-height: calc(100vh - 200px);
  overflow-y: auto;
}

.grade-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  margin-bottom: 8px;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s;
  border: 1px solid #e4e7ed;
}

.grade-item:hover {
  background-color: #f5f7fa;
  border-color: #c0c4cc;
}

.grade-item.active {
  background-color: #ecf5ff;
  border-color: #409eff;
  color: #409eff;
  font-weight: 500;
}

.grade-badge {
  margin-left: 10px;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .students-list {
    padding: 10px;
    height: auto;
  }

  .students-layout {
    flex-direction: column;
  }

  .grade-sidebar {
    width: 100%;
    min-width: auto;
    height: auto;
    max-height: 300px;
    margin-bottom: 10px;
  }

  .main-content {
    width: 100%;
  }

  .card-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }

  .header-btns {
    width: 100%;
    display: flex;
    gap: 10px;
  }

  .header-btns .el-button {
    flex: 1;
  }

  .search-bar {
    margin-bottom: 15px;
  }

  .search-bar .el-col {
    margin-bottom: 10px;
  }

  .table-wrapper {
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
  }

  .table-wrapper.mobile-table {
    min-width: 500px;
  }

  .table-wrapper :deep(.el-table) {
    font-size: 12px;
    table-layout: fixed;
    min-width: 500px;
  }

  .table-wrapper :deep(.el-table .hidden-mobile) {
    display: none !important;
  }

  .table-wrapper :deep(.el-table__body-wrapper) {
    overflow-x: auto;
  }

  /* 强制移动端列宽 */
  .table-wrapper :deep(.el-table .col-student-id) {
    width: 130px !important;
    min-width: 130px !important;
    max-width: 130px !important;
  }

  .table-wrapper :deep(.el-table .col-name) {
    width: 80px !important;
    min-width: 80px !important;
    max-width: 80px !important;
  }

  .table-wrapper :deep(.el-table .col-grade) {
    width: 70px !important;
    min-width: 70px !important;
    max-width: 70px !important;
  }

  .table-wrapper :deep(.el-table .col-gender) {
    width: 50px !important;
    min-width: 50px !important;
    max-width: 50px !important;
  }

  .table-wrapper :deep(.el-table .col-status) {
    width: 60px !important;
    min-width: 60px !important;
    max-width: 60px !important;
  }

  :deep(.el-dialog) {
    width: 95vw !important;
  }

  :deep(.el-form-item__label) {
    font-size: 13px;
  }

  :deep(.el-pagination) {
    flex-wrap: wrap;
    justify-content: center;
  }
}

@media (max-width: 480px) {
  .students-list {
    padding: 8px;
  }

  :deep(.el-table) {
    font-size: 11px;
  }

  :deep(.el-dialog) {
    width: 98vw !important;
  }
}
</style> 
