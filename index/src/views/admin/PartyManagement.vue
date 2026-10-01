<template>
  <div class="party-management">

    <el-tabs v-model="activeTab" class="management-tabs" @tab-change="handleTabChange">
      <!-- 入党申请管理 -->
      <el-tab-pane label="入党申请" name="application">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>入党申请管理</span>
              <div class="header-actions">
                <el-button type="success" @click="showBatchImportApplicationDialog = true" style="margin-right: 10px">
                  <el-icon><Upload /></el-icon>
                  批量导入发展中的学生
                </el-button>
                <el-input
                  v-model="applicationSearchKeyword"
                  placeholder="搜索学号或姓名"
                  clearable
                  style="width: 200px; margin-right: 10px"
                  @keyup.enter="loadApplications"
                >
                  <template #prefix>
                    <el-icon><Search /></el-icon>
                  </template>
                </el-input>
                <el-select
                  v-model="applicationFilterStage"
                  placeholder="选择阶段"
                  clearable
                  style="width: 150px; margin-right: 10px"
                  @change="loadApplications"
                >
                  <el-option label="提交申请" value="提交申请" />
                  <el-option label="积极分子" value="积极分子" />
                  <el-option label="发展对象" value="发展对象" />
                  <el-option label="预备党员" value="预备党员" />
                </el-select>
                <el-select
                  v-model="applicationFilterStatus"
                  placeholder="审核状态"
                  clearable
                  style="width: 120px"
                  @change="loadApplications"
                >
                  <el-option label="待审核" value="待审核" />
                  <el-option label="已通过" value="通过" />
                  <el-option label="已拒绝" value="拒绝" />
                </el-select>
              </div>
            </div>
          </template>

          <el-table :data="applicationList" v-loading="applicationLoading" stripe>
            <el-table-column prop="studentId" label="学号" width="120" />
            <el-table-column prop="name" label="姓名" width="100" />
            <el-table-column prop="currentStage" label="当前阶段" width="120">
              <template #default="scope">
                <el-tag :type="getStageType(scope.row.currentStage)" size="small">
                  {{ getStageLabel(scope.row.currentStage) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="applicationDate" label="申请日期" width="120" />
            <el-table-column prop="branch" label="所属支部" width="150" />
            <el-table-column prop="auditStatus" label="审核状态" width="100">
              <template #default="scope">
                <el-tag :type="getAuditStatusType(scope.row.auditStatus)" size="small">
                  {{ scope.row.auditStatus || '待审核' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="submittedMaterials" label="已提交材料" min-width="200">
              <template #default="scope">
                <span v-if="scope.row.submittedMaterials && scope.row.submittedMaterials.length > 0">
                  {{ scope.row.submittedMaterials.length }} 项
                </span>
                <span v-else>0 项</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="200" fixed="right">
              <template #default="scope">
                <el-button size="small" @click="viewApplication(scope.row)">查看</el-button>
                <el-button
                  size="small"
                  type="warning"
                  @click="auditApplication(scope.row)"
                  v-if="scope.row.auditStatus === '待审核'"
                >
                  审核
                </el-button>
                <el-button size="small" type="danger" @click="deleteApplication(scope.row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>

      <!-- 思想汇报管理 -->
      <el-tab-pane label="思想汇报" name="report">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>思想汇报管理</span>
              <div class="header-actions">
                <el-input
                  v-model="reportSearchKeyword"
                  placeholder="搜索标题或学号"
                  clearable
                  style="width: 200px; margin-right: 10px"
                  @keyup.enter="loadReports"
                >
                  <template #prefix>
                    <el-icon><Search /></el-icon>
                  </template>
                </el-input>
                <el-select
                  v-model="reportFilterStatus"
                  placeholder="审核状态"
                  clearable
                  style="width: 120px"
                  @change="loadReports"
                >
                  <el-option label="待审核" value="待审核" />
                  <el-option label="已通过" value="通过" />
                  <el-option label="已拒绝" value="拒绝" />
                </el-select>
              </div>
            </div>
          </template>

          <el-table :data="reportList" v-loading="reportLoading" stripe>
            <el-table-column prop="studentId" label="学号" width="120" />
            <el-table-column prop="title" label="标题" min-width="200" />
            <el-table-column prop="reportType" label="类型" width="100" />
            <el-table-column prop="reportDate" label="汇报日期" width="120" />
            <el-table-column prop="auditStatus" label="审核状态" width="100">
              <template #default="scope">
                <el-tag :type="getAuditStatusType(scope.row.auditStatus)" size="small">
                  {{ scope.row.auditStatus || '待审核' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="提交时间" width="180">
              <template #default="scope">
                {{ formatDateTime(scope.row.createTime) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="200" fixed="right">
              <template #default="scope">
                <el-button size="small" @click="viewReport(scope.row)">查看</el-button>
                <el-button
                  size="small"
                  type="warning"
                  @click="auditReport(scope.row)"
                  v-if="scope.row.auditStatus === '待审核'"
                >
                  审核
                </el-button>
                <el-button size="small" type="danger" @click="deleteReport(scope.row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>

      <!-- 微党课管理 -->
      <el-tab-pane label="微党课" name="course">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>微党课管理</span>
              <div class="header-actions">
                <el-input
                  v-model="courseSearchKeyword"
                  placeholder="搜索标题或学号"
                  clearable
                  style="width: 200px; margin-right: 10px"
                  @keyup.enter="loadCourses"
                >
                  <template #prefix>
                    <el-icon><Search /></el-icon>
                  </template>
                </el-input>
                <el-select
                  v-model="courseFilterStatus"
                  placeholder="审核状态"
                  clearable
                  style="width: 120px"
                  @change="loadCourses"
                >
                  <el-option label="待审核" value="待审核" />
                  <el-option label="已通过" value="通过" />
                  <el-option label="已拒绝" value="拒绝" />
                </el-select>
              </div>
            </div>
          </template>

          <el-table :data="courseList" v-loading="courseLoading" stripe>
            <el-table-column prop="studentId" label="学号" width="120" />
            <el-table-column prop="title" label="微党课/推文题目" min-width="200" />
            <el-table-column prop="courseDate" label="发布日期" width="120" />
            <el-table-column prop="auditStatus" label="审核状态" width="100">
              <template #default="scope">
                <el-tag :type="getAuditStatusType(scope.row.auditStatus)" size="small">
                  {{ scope.row.auditStatus || '待审核' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="提交时间" width="180">
              <template #default="scope">
                {{ formatDateTime(scope.row.createTime) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="200" fixed="right">
              <template #default="scope">
                <el-button size="small" @click="viewCourse(scope.row)">查看</el-button>
                <el-button
                  size="small"
                  type="warning"
                  @click="auditCourse(scope.row)"
                  v-if="scope.row.auditStatus === '待审核'"
                >
                  审核
                </el-button>
                <el-button size="small" type="danger" @click="deleteCourse(scope.row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>

      <!-- 志愿活动管理 -->
      <el-tab-pane label="志愿活动" name="service">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>志愿活动管理</span>
              <div class="header-actions">
                <el-input
                  v-model="serviceSearchKeyword"
                  placeholder="搜索服务名称或学号"
                  clearable
                  style="width: 200px; margin-right: 10px"
                  @keyup.enter="loadServices"
                >
                  <template #prefix>
                    <el-icon><Search /></el-icon>
                  </template>
                </el-input>
                <el-select
                  v-model="serviceFilterStatus"
                  placeholder="审核状态"
                  clearable
                  style="width: 120px"
                  @change="loadServices"
                >
                  <el-option label="待审核" value="待审核" />
                  <el-option label="已通过" value="通过" />
                  <el-option label="已拒绝" value="拒绝" />
                </el-select>
              </div>
            </div>
          </template>

          <el-table :data="serviceList" v-loading="serviceLoading" stripe>
            <el-table-column prop="studentId" label="学号" width="120" />
            <el-table-column prop="serviceName" label="服务名称" min-width="200" />
            <el-table-column prop="serviceType" label="类型" width="120" />
            <el-table-column prop="serviceHours" label="时长" width="100" />
            <el-table-column prop="serviceDate" label="服务日期" width="120" />
            <el-table-column prop="auditStatus" label="审核状态" width="100">
              <template #default="scope">
                <el-tag :type="getAuditStatusType(scope.row.auditStatus)" size="small">
                  {{ scope.row.auditStatus || '待审核' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="提交时间" width="180">
              <template #default="scope">
                {{ formatDateTime(scope.row.createTime) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="200" fixed="right">
              <template #default="scope">
                <el-button size="small" @click="viewService(scope.row)">查看</el-button>
                <el-button
                  size="small"
                  type="warning"
                  @click="auditService(scope.row)"
                  v-if="scope.row.auditStatus === '待审核'"
                >
                  审核
                </el-button>
                <el-button size="small" type="danger" @click="deleteService(scope.row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- 审核对话框 -->
    <el-dialog v-model="showAuditDialog" :title="auditDialogTitle" width="500px">
      <el-form :model="auditForm" label-width="100px">
        <el-form-item label="审核状态" required>
          <el-radio-group v-model="auditForm.auditStatus">
            <el-radio label="通过">通过</el-radio>
            <el-radio label="拒绝">拒绝</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审核意见">
          <el-input
            v-model="auditForm.auditComment"
            type="textarea"
            :rows="4"
            placeholder="请输入审核意见"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAuditDialog = false">取消</el-button>
        <el-button type="primary" @click="handleAuditSubmit" :loading="auditLoading">
          提交审核
        </el-button>
      </template>
    </el-dialog>

    <!-- 查看详情对话框 -->
    <el-dialog v-model="showViewDialog" :title="viewDialogTitle" width="700px">
      <el-descriptions :column="2" border v-if="viewingItem">
        <el-descriptions-item
          v-for="(value, key) in visibleDetail(viewingItem)"
          :key="key"
          :label="getFieldLabel(key)"
          :span="key === 'content' || key === 'description' || key === 'serviceDescription' ? 2 : 1"
        >
          <template v-if="key === 'auditStatus'">
            <el-tag :type="getAuditStatusType(value)" size="small">
              {{ value || '待审核' }}
            </el-tag>
          </template>
          <template v-else-if="key === 'currentStage'">
            <el-tag :type="getStageType(value)" size="small">
              {{ getStageLabel(value) }}
            </el-tag>
          </template>
          <template v-else-if="key === 'submittedMaterials' && Array.isArray(value)">
            <el-table :data="value" size="small" border style="margin-top: 10px">
              <el-table-column prop="materialType" label="材料类型" width="120" />
              <el-table-column prop="materialName" label="材料名称" min-width="150" />
              <el-table-column prop="submitDate" label="提交日期" width="120" />
              <el-table-column prop="status" label="状态" width="100">
                <template #default="scope">
                  <el-tag :type="getAuditStatusType(scope.row.status)" size="small">
                    {{ scope.row.status || '待审核' }}
                  </el-tag>
                </template>
              </el-table-column>
            </el-table>
          </template>
          <template v-else>
            {{ displayDetailValue(value) }}
          </template>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <el-dialog
      v-model="showBatchImportApplicationDialog"
      title="批量导入入党申请"
      width="760px"
      destroy-on-close
    >
      <el-form label-width="100px">
        <el-form-item label="申请阶段" required>
          <el-select v-model="batchImportForm.currentStage" style="width: 100%">
            <el-option label="提交申请" value="提交申请" />
            <el-option label="入党积极分子" value="入党积极分子" />
            <el-option label="发展对象" value="发展对象" />
            <el-option label="预备党员" value="预备党员" />
          </el-select>
        </el-form-item>
      </el-form>

      <el-tabs v-model="applicationImportTab">
        <el-tab-pane label="Excel 导入" name="excel">
          <el-upload
            ref="applicationUploadRef"
            v-model:file-list="applicationFileList"
            drag
            :auto-upload="false"
            :limit="1"
            accept=".xlsx"
            :on-change="handleApplicationFileChange"
          >
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">拖入 Excel 文件，或<em>点击选择</em></div>
            <template #tip>
              <div class="el-upload__tip">表格至少包含“姓名”或“学号”列</div>
            </template>
          </el-upload>
        </el-tab-pane>
        <el-tab-pane label="手动录入" name="manual">
          <div style="margin-bottom: 12px">
            <el-button type="primary" plain @click="addApplicationStudentRow">添加一行</el-button>
            <el-button plain @click="clearApplicationStudentRows">清空</el-button>
          </div>
          <el-table :data="applicationManualStudents" max-height="320" border>
            <el-table-column label="姓名">
              <template #default="scope">
                <el-input v-model="scope.row.name" placeholder="学生姓名" />
              </template>
            </el-table-column>
            <el-table-column label="学号">
              <template #default="scope">
                <el-input v-model="scope.row.studentId" placeholder="学生学号" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="90" align="center">
              <template #default="scope">
                <el-button type="danger" link @click="removeApplicationStudentRow(scope.$index)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>

      <template #footer>
        <el-button @click="showBatchImportApplicationDialog = false">取消</el-button>
        <el-button type="primary" :loading="applicationImportLoading" @click="handleBatchImportApplication">
          开始导入
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="applicationResultDialogVisible" title="导入结果" width="720px">
      <el-alert
        :title="`成功 ${applicationImportResult.matchedCount} 条，失败 ${applicationImportResult.unmatchedCount} 条`"
        :type="applicationImportResult.unmatchedCount ? 'warning' : 'success'"
        :closable="false"
        show-icon
      />
      <el-tabs v-model="applicationResultTab" style="margin-top: 16px">
        <el-tab-pane :label="`成功 (${applicationImportResult.matchedCount})`" name="matched">
          <el-table :data="applicationImportResult.matchedResults" max-height="320">
            <el-table-column prop="name" label="姓名" />
            <el-table-column prop="studentId" label="学号" />
          </el-table>
        </el-tab-pane>
        <el-tab-pane :label="`失败 (${applicationImportResult.unmatchedCount})`" name="unmatched">
          <el-table :data="applicationImportResult.unmatchedResults" max-height="320">
            <el-table-column prop="name" label="姓名" />
            <el-table-column prop="studentId" label="学号" />
            <el-table-column prop="reason" label="原因" />
          </el-table>
        </el-tab-pane>
      </el-tabs>
      <template #footer>
        <el-button type="primary" @click="applicationResultDialogVisible = false">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Upload, UploadFilled } from '@element-plus/icons-vue'
import {
  partyApplicationApi,
  thoughtReportApi,
  partyCourseApi,
  volunteerServiceApi
} from '@/api/party'
import { readExcelObjects } from '@/utils/excel'

// 标签页
const activeTab = ref('application')

// 数据
const applicationList = ref([])
const reportList = ref([])
const courseList = ref([])
const serviceList = ref([])

// 加载状态
const applicationLoading = ref(false)
const reportLoading = ref(false)
const courseLoading = ref(false)
const serviceLoading = ref(false)

// 搜索关键词
const applicationSearchKeyword = ref('')
const applicationFilterStage = ref('')
const applicationFilterStatus = ref('')
const reportSearchKeyword = ref('')
const reportFilterStatus = ref('')
const courseSearchKeyword = ref('')
const courseFilterStatus = ref('')
const serviceSearchKeyword = ref('')
const serviceFilterStatus = ref('')

// 对话框
const showAuditDialog = ref(false)
const showViewDialog = ref(false)
const auditDialogTitle = ref('')
const viewDialogTitle = ref('')
const auditLoading = ref(false)

// 当前审核/查看项
const currentAuditItem = ref(null)
const currentAuditType = ref('')
const viewingItem = ref(null)

// 批量导入入党申请相关
const showBatchImportApplicationDialog = ref(false)
const applicationResultDialogVisible = ref(false)
const applicationImportTab = ref('excel')
const applicationResultTab = ref('matched')
const applicationImportLoading = ref(false)
const applicationFileList = ref([])
const applicationManualStudents = ref([])
const applicationUploadRef = ref(null)

const batchImportForm = reactive({
  currentStage: '提交申请'
})

const applicationImportResult = reactive({
  matchedCount: 0,
  unmatchedCount: 0,
  matchedResults: [],
  unmatchedResults: []
})

// 审核表单
const auditForm = reactive({
  auditStatus: '',
  auditComment: ''
})

// 标签页切换
const handleTabChange = (tab) => {
  if (tab === 'application') {
    loadApplications()
  } else if (tab === 'report') {
    loadReports()
  } else if (tab === 'course') {
    loadCourses()
  } else if (tab === 'service') {
    loadServices()
  }
}

// 加载入党申请
const loadApplications = async () => {
  applicationLoading.value = true
  try {
    const response = await partyApplicationApi.getAll()
    let data = response.data || []
    
    // 搜索过滤
    if (applicationSearchKeyword.value) {
      data = data.filter(item =>
        item.studentId?.includes(applicationSearchKeyword.value) ||
        item.name?.includes(applicationSearchKeyword.value)
      )
    }
    
    // 阶段过滤
    if (applicationFilterStage.value) {
      data = data.filter(item => item.currentStage === applicationFilterStage.value)
    }
    
    // 状态过滤
    if (applicationFilterStatus.value) {
      data = data.filter(item => item.auditStatus === applicationFilterStatus.value)
    }
    
    applicationList.value = data
  } catch (error) {
    console.error('加载入党申请失败:', error)
    ElMessage.error('加载入党申请失败')
  } finally {
    applicationLoading.value = false
  }
}

// 加载思想汇报
const loadReports = async () => {
  reportLoading.value = true
  try {
    const response = await thoughtReportApi.getAllThoughtReports()
    let data = response.data || []
    
    if (reportSearchKeyword.value) {
      data = data.filter(item =>
        item.title?.includes(reportSearchKeyword.value) ||
        item.studentId?.includes(reportSearchKeyword.value)
      )
    }
    
    if (reportFilterStatus.value) {
      data = data.filter(item => item.auditStatus === reportFilterStatus.value)
    }
    
    reportList.value = data
  } catch (error) {
    console.error('加载思想汇报失败:', error)
    ElMessage.error('加载思想汇报失败')
  } finally {
    reportLoading.value = false
  }
}

// 加载微党课
const loadCourses = async () => {
  courseLoading.value = true
  try {
    const response = await partyCourseApi.getAllPartyCourses()
    let data = response.data || []
    
    if (courseSearchKeyword.value) {
      data = data.filter(item =>
        item.title?.includes(courseSearchKeyword.value) ||
        item.studentId?.includes(courseSearchKeyword.value)
      )
    }
    
    if (courseFilterStatus.value) {
      data = data.filter(item => item.auditStatus === courseFilterStatus.value)
    }
    
    courseList.value = data
  } catch (error) {
    console.error('加载微党课失败:', error)
    ElMessage.error('加载微党课失败')
  } finally {
    courseLoading.value = false
  }
}

// 加载志愿活动
const loadServices = async () => {
  serviceLoading.value = true
  try {
    const response = await volunteerServiceApi.getAllVolunteerServices()
    let data = response.data || []
    
    if (serviceSearchKeyword.value) {
      data = data.filter(item =>
        item.serviceName?.includes(serviceSearchKeyword.value) ||
        item.studentId?.includes(serviceSearchKeyword.value)
      )
    }
    
    if (serviceFilterStatus.value) {
      data = data.filter(item => item.auditStatus === serviceFilterStatus.value)
    }
    
    serviceList.value = data
  } catch (error) {
    console.error('加载志愿活动失败:', error)
    ElMessage.error('加载志愿活动失败')
  } finally {
    serviceLoading.value = false
  }
}

// 查看详情
const viewApplication = (row) => {
  viewingItem.value = row
  viewDialogTitle.value = '入党申请详情'
  showViewDialog.value = true
}

const viewReport = (row) => {
  viewingItem.value = row
  viewDialogTitle.value = '思想汇报详情'
  showViewDialog.value = true
}

const viewCourse = (row) => {
  viewingItem.value = row
  viewDialogTitle.value = '微党课详情'
  showViewDialog.value = true
}

const viewService = (row) => {
  viewingItem.value = row
  viewDialogTitle.value = '志愿活动详情'
  showViewDialog.value = true
}

// 审核
const auditApplication = (row) => {
  currentAuditItem.value = row
  currentAuditType.value = 'application'
  auditDialogTitle.value = '审核入党申请'
  auditForm.auditStatus = ''
  auditForm.auditComment = ''
  showAuditDialog.value = true
}

const auditReport = (row) => {
  currentAuditItem.value = row
  currentAuditType.value = 'report'
  auditDialogTitle.value = '审核思想汇报'
  auditForm.auditStatus = ''
  auditForm.auditComment = ''
  showAuditDialog.value = true
}

const auditCourse = (row) => {
  currentAuditItem.value = row
  currentAuditType.value = 'course'
  auditDialogTitle.value = '审核微党课'
  auditForm.auditStatus = ''
  auditForm.auditComment = ''
  showAuditDialog.value = true
}

const auditService = (row) => {
  currentAuditItem.value = row
  currentAuditType.value = 'service'
  auditDialogTitle.value = '审核志愿活动'
  auditForm.auditStatus = ''
  auditForm.auditComment = ''
  showAuditDialog.value = true
}

// 提交审核
const handleAuditSubmit = async () => {
  if (!auditForm.auditStatus) {
    ElMessage.warning('请选择审核状态')
    return
  }

  auditLoading.value = true
  try {
    const auditData = {
      auditStatus: auditForm.auditStatus,
      auditComment: auditForm.auditComment
    }

    if (currentAuditType.value === 'application') {
      await partyApplicationApi.auditPartyApplication(currentAuditItem.value.id, auditData)
      ElMessage.success('审核提交成功')
      await loadApplications()
    } else if (currentAuditType.value === 'report') {
      await thoughtReportApi.auditThoughtReport(currentAuditItem.value.id, auditData)
      ElMessage.success('审核提交成功')
      await loadReports()
    } else if (currentAuditType.value === 'course') {
      await partyCourseApi.auditPartyCourse(currentAuditItem.value.id, auditData)
      ElMessage.success('审核提交成功')
      await loadCourses()
    } else if (currentAuditType.value === 'service') {
      await volunteerServiceApi.auditVolunteerService(currentAuditItem.value.id, auditData)
      ElMessage.success('审核提交成功')
      await loadServices()
    }

    showAuditDialog.value = false
  } catch (error) {
    console.error('审核失败:', error)
    ElMessage.error('审核失败')
  } finally {
    auditLoading.value = false
  }
}

// 删除
const deleteApplication = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该入党申请吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await partyApplicationApi.deletePartyApplication(row.id)
    ElMessage.success('删除成功')
    await loadApplications()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

const deleteReport = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该思想汇报吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await thoughtReportApi.deleteThoughtReport(row.id)
    ElMessage.success('删除成功')
    await loadReports()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

const deleteCourse = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该微党课记录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await partyCourseApi.deletePartyCourse(row.id)
    ElMessage.success('删除成功')
    await loadCourses()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

const deleteService = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该志愿活动记录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await volunteerServiceApi.deleteVolunteerService(row.id)
    ElMessage.success('删除成功')
    await loadServices()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

// 工具函数
const getAuditStatusType = (status) => {
  switch (status) {
    case '待审核':
      return 'warning'
    case '通过':
      return 'success'
    case '拒绝':
      return 'danger'
    default:
      return 'info'
  }
}

const getStageType = (stage) => {
  switch (stage) {
    case '提交申请':
      return 'info'
    case '积极分子':
      return 'warning'
    case '发展对象':
      return 'warning'
    case '预备党员':
      return 'success'
    default:
      return 'info'
  }
}

const getStageLabel = (stage) => {
  const stageMap = {
    '提交申请': '提交申请',
    '积极分子': '积极分子',
    '发展对象': '发展对象',
    '预备党员': '预备党员'
  }
  return stageMap[stage] || stage
}

const formatDateTime = (date) => {
  if (!date) return ''
  return new Date(date).toLocaleString('zh-CN')
}

const displayDetailValue = value => ({ pending: '待审核', approved: '已通过', rejected: '已驳回', submitted: '已提交', completed: '已完成' }[value] || value || '-')
const visibleDetail = row => Object.fromEntries(Object.entries(row || {}).filter(([key, value]) => getFieldLabel(key) !== key && value != null && value !== '' && !['studentId', 'courseType', 'instructor', 'location'].includes(key)))

const getFieldLabel = (key) => {
  const labelMap = {
    studentId: '学号',
    name: '姓名',
    studentName: '学生姓名',
    status: '状态',
    auditTime: '审核时间',
    title: '标题',
    content: '内容',
    description: '推文发布链接',
    reportType: '汇报类型',
    reportDate: '汇报日期',
    courseType: '课程类型',
    courseDate: '发布日期',
    instructor: '授课教师',
    location: '上课地点',
    serviceName: '服务名称',
    serviceDescription: '服务描述',
    serviceType: '服务类型',
    serviceDate: '服务日期',
    serviceHours: '服务时长',
    serviceLocation: '服务地点',
    currentStage: '当前阶段',
    applicationDate: '申请日期',
    branch: '所属支部',
    auditStatus: '审核状态',
    auditComment: '审核意见',
    submittedMaterials: '已提交材料',
    createTime: '创建时间',
    updateTime: '更新时间'
  }
  return labelMap[key] || key
}

// 批量导入入党申请相关函数
const handleApplicationFileChange = async (file) => {
  try {
      const jsonData = await readExcelObjects(file.raw)
      
      // 清空手动输入列表
      applicationManualStudents.value = []
      
      // 将Excel数据转换为手动输入格式
      jsonData.forEach(row => {
        const name = row['姓名'] || row['name'] || ''
        const studentId = row['学号'] || row['studentId'] || ''
        if (name || studentId) {
          applicationManualStudents.value.push({
            name: String(name),
            studentId: String(studentId)
          })
        }
      })
      
      if (applicationManualStudents.value.length > 0) {
        ElMessage.success(`成功读取 ${applicationManualStudents.value.length} 条数据`)
        applicationImportTab.value = 'manual'
      } else {
        ElMessage.warning('Excel文件中没有找到有效数据')
      }
  } catch (error) {
    console.error('解析Excel文件失败:', error)
    ElMessage.error(error.message || '解析Excel文件失败，请检查文件格式')
  }
}

const addApplicationStudentRow = () => {
  applicationManualStudents.value.push({
    name: '',
    studentId: ''
  })
}

const removeApplicationStudentRow = (index) => {
  applicationManualStudents.value.splice(index, 1)
}

const clearApplicationStudentRows = () => {
  applicationManualStudents.value = []
}

const handleBatchImportApplication = async () => {
  // 验证数据
  const validStudents = applicationManualStudents.value.filter(s => s.name || s.studentId)
  if (validStudents.length === 0) {
    ElMessage.warning('请至少输入一条有效数据')
    return
  }
  
  if (!batchImportForm.currentStage) {
    ElMessage.warning('请选择当前阶段')
    return
  }
  
  applicationImportLoading.value = true
  try {
    const res = await partyApplicationApi.batchImportPartyApplications({
      students: validStudents,
      currentStage: batchImportForm.currentStage
    })
    
    if (res.code === 200) {
      applicationImportResult.matchedCount = res.data.matchedCount || 0
      applicationImportResult.unmatchedCount = res.data.unmatchedCount || 0
      applicationImportResult.matchedResults = res.data.matchedResults || []
      applicationImportResult.unmatchedResults = res.data.unmatchedResults || []
      
      applicationResultDialogVisible.value = true
      showBatchImportApplicationDialog.value = false
      
      // 刷新列表
      await loadApplications()
      
      if (applicationImportResult.unmatchedCount === 0) {
        ElMessage.success('批量导入成功')
      } else {
        ElMessage.warning(`导入完成，但有 ${applicationImportResult.unmatchedCount} 条数据失败`)
      }
    } else {
      ElMessage.error(res.msg || '批量导入失败')
    }
  } catch (error) {
    console.error('批量导入失败:', error)
    ElMessage.error('批量导入失败')
  } finally {
    applicationImportLoading.value = false
  }
}

// 初始化
onMounted(() => {
  loadApplications()
})
</script>

<style scoped>
.party-management {
  padding: 20px;
}

.page-header-card {
  margin-bottom: 20px;
  border-radius: 8px;
}

.page-header h2 {
  margin: 0 0 8px 0;
  font-size: 24px;
  font-weight: 600;
  color: #303133;
}

.page-header p {
  margin: 0;
  font-size: 14px;
  color: #909399;
}

.management-tabs {
  margin-top: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.header-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
}

:deep(.el-table) {
  margin-top: 20px;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .party-management {
    padding: 10px;
  }

  .card-header {
    flex-direction: column;
    align-items: stretch;
  }

  .header-actions {
    width: 100%;
    flex-direction: column;
    gap: 10px;
  }

  .header-actions .el-input,
  .header-actions .el-select {
    width: 100% !important;
    margin-right: 0 !important;
  }

  :deep(.el-dialog) {
    width: 95vw !important;
  }
}
</style>
