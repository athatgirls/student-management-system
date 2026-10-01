<template>
  <div class="party-page">

    <el-tabs
      v-model="activeTab"
      class="party-tabs"
      :class="{ 'external-tab-mode': isSeparatedSubPage }"
      @tab-change="handleTabChange"
    >
      <!-- 党员信息 / 申请入党信息 -->
      <el-tab-pane :label="isPartyMember ? '党员信息' : '申请入党'" name="member">
        <!-- 如果是党员，显示党员信息 -->
        <el-card v-if="isPartyMember">
          <template #header>
            <div class="card-header">
              <span>我的党员信息</span>
              <el-button v-if="memberInfo.id" type="primary" size="small" @click="editMemberDialog = true">编辑信息</el-button>
            </div>
          </template>
          
          <div v-if="memberInfo.id" class="member-info-container">
            <div class="avatar-section">
              <div class="avatar-wrapper">
                <img v-if="memberInfo.avatar" :src="memberInfo.avatar" class="avatar-img" />
                <el-icon v-else class="avatar-placeholder"><User /></el-icon>
                <div class="upload-overlay" @click="triggerPhotoUpload">
                  <el-icon><Upload /></el-icon>
                  <span>上传照片</span>
                </div>
              </div>
              <input
                ref="photoInput"
                type="file"
                accept="image/*"
                style="display: none"
                @change="handlePhotoChange"
              />
            </div>
            
            <el-descriptions :column="2" border class="member-descriptions">
              <el-descriptions-item label="学号">{{ memberInfo.studentId || '-' }}</el-descriptions-item>
              <el-descriptions-item label="姓名">{{ memberInfo.name || '-' }}</el-descriptions-item>
              <el-descriptions-item label="性别">{{ memberInfo.gender || '-' }}</el-descriptions-item>
              <el-descriptions-item label="民族">{{ memberInfo.nation || '-' }}</el-descriptions-item>
              <el-descriptions-item label="出生日期">{{ memberInfo.birth || '-' }}</el-descriptions-item>
              <el-descriptions-item label="籍贯">{{ memberInfo.nativePlace || '-' }}</el-descriptions-item>
              <el-descriptions-item label="所在班级">{{ memberInfo.className || '-' }}</el-descriptions-item>
              <el-descriptions-item label="所在支部">{{ memberInfo.branch || '-' }}</el-descriptions-item>
              <el-descriptions-item label="入党时间">{{ memberInfo.joinDate || '-' }}</el-descriptions-item>
              <el-descriptions-item label="是否转正">
                <el-tag :type="memberInfo.isRegular === '是' ? 'success' : 'warning'">
                  {{ memberInfo.isRegular || '否' }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="志愿书编号">{{ memberInfo.applicationNo || '-' }}</el-descriptions-item>
              <el-descriptions-item label="党内职务">{{ memberInfo.position || '-' }}</el-descriptions-item>
              <el-descriptions-item label="奖惩情况" :span="2">{{ memberInfo.rewardsPunishments || '-' }}</el-descriptions-item>
              <el-descriptions-item label="审核状态" :span="2">
                <el-tag :type="getAuditStatusType(memberInfo.auditStatus)">
                  {{ memberInfo.auditStatus || '未审核' }}
                </el-tag>
                <span v-if="memberInfo.auditComment" class="audit-comment">（{{ memberInfo.auditComment }}）</span>
              </el-descriptions-item>
            </el-descriptions>
          </div>
          
          <el-empty v-else description="暂无党员信息，请联系管理员添加" />
        </el-card>

        <!-- 如果不是党员，显示申请入党信息 -->
        <el-card v-else>
          <template #header>
            <div class="card-header">
              <span>我的申请入党信息</span>
              <el-button v-if="!applicationInfo.id" type="primary" size="small" @click="showApplicationDialog = true">提交申请</el-button>
            </div>
          </template>
          
          <div v-if="applicationInfo.id" class="application-info">
            <el-alert
              :title="`当前阶段：${getStageLabel(applicationInfo.currentStage)}`"
              :type="getStageType(applicationInfo.currentStage)"
              :closable="false"
              style="margin-bottom: 20px"
            />
            
            <el-descriptions :column="2" border class="member-descriptions">
              <el-descriptions-item label="学号">{{ applicationInfo.studentId || '-' }}</el-descriptions-item>
              <el-descriptions-item label="姓名">{{ applicationInfo.name || '-' }}</el-descriptions-item>
              <el-descriptions-item label="申请日期">{{ applicationInfo.applicationDate || '-' }}</el-descriptions-item>
              <el-descriptions-item label="所属支部">{{ applicationInfo.branch || '-' }}</el-descriptions-item>
              <el-descriptions-item label="审核状态" :span="2">
                <el-tag :type="getAuditStatusType(applicationInfo.auditStatus)">
                  {{ applicationInfo.auditStatus || '待审核' }}
                </el-tag>
                <span v-if="applicationInfo.auditComment" class="audit-comment">（{{ applicationInfo.auditComment }}）</span>
              </el-descriptions-item>
            </el-descriptions>

            <el-divider>已提交材料</el-divider>
            
            <el-table 
              v-if="applicationInfo.submittedMaterials && applicationInfo.submittedMaterials.length > 0"
              :data="applicationInfo.submittedMaterials" 
              stripe 
              style="margin-top: 20px"
            >
              <el-table-column prop="materialType" label="材料类型" width="150" />
              <el-table-column prop="materialName" label="材料名称" min-width="200" />
              <el-table-column prop="submitDate" label="提交日期" width="120" />
              <el-table-column prop="status" label="审核状态" width="100">
                <template #default="scope">
                  <el-tag :type="getAuditStatusType(scope.row.status)" size="small">
                    {{ scope.row.status || '待审核' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="auditComment" label="审核意见" min-width="200" />
              <el-table-column label="操作" width="100">
                <template #default="scope">
                  <el-button v-if="scope.row.fileUrl" link type="primary" size="small" @click="viewMaterial(scope.row.fileUrl)">查看</el-button>
                </template>
              </el-table-column>
            </el-table>
            
            <el-empty v-else description="暂无已提交材料" />
          </div>
          
          <el-empty v-else description="您尚未提交入党申请" />
        </el-card>
      </el-tab-pane>

      <!-- 思想汇报 -->
      <el-tab-pane label="思想汇报" name="report">
        <el-row :gutter="20">
          <el-col :span="8">
            <el-card>
              <template #header>
                <span>提交思想汇报</span>
              </template>
              <el-form :model="reportForm" label-width="100px" label-position="top">
                <el-form-item label="标题" required>
                  <el-input v-model="reportForm.title" placeholder="请输入思想汇报标题" />
                </el-form-item>
                <el-form-item label="内容" required>
                  <el-input v-model="reportForm.content" type="textarea" :rows="6" placeholder="请输入思想汇报内容" />
                </el-form-item>
                <el-form-item label="汇报日期" required>
                  <el-date-picker
                    v-model="reportForm.reportDate"
                    type="date"
                    placeholder="选择日期"
                    style="width: 100%"
                    value-format="YYYY-MM-DD"
                  />
                </el-form-item>
                <el-form-item label="汇报类型" required>
                  <el-select v-model="reportForm.reportType" placeholder="请选择" style="width: 100%">
                    <el-option label="定期汇报" value="定期" />
                    <el-option label="专项汇报" value="专项" />
                  </el-select>
                </el-form-item>
                <el-form-item label="备注">
                  <el-input v-model="reportForm.remark" type="textarea" :rows="2" placeholder="可选" />
                </el-form-item>
                <el-form-item label="汇报材料">
                  <el-upload
                    class="report-upload"
                    :http-request="handleReportMaterialUpload"
                    :before-upload="beforeReportMaterialUpload"
                    :file-list="reportFileList"
                    accept=".pdf,.doc,.docx,.jpg,.jpeg,.png"
                    multiple
                  >
                    <el-button type="primary" plain>
                      <el-icon><Upload /></el-icon>
                      上传PDF/材料
                    </el-button>
                    <template #tip>
                      <div class="el-upload__tip">支持 PDF、Word、图片材料，单个文件不超过 10MB。</div>
                    </template>
                  </el-upload>
                  <div v-if="reportForm.materials.length" class="material-tags">
                    <el-tag v-for="file in reportForm.materials" :key="file.url" effect="plain">
                      {{ file.name }}
                    </el-tag>
                  </div>
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" @click="submitReport" :loading="reportLoading" style="width: 100%">提交</el-button>
                </el-form-item>
              </el-form>
            </el-card>
          </el-col>
          <el-col :span="16">
            <el-card>
              <template #header>
                <div class="card-header">
                  <span>历史记录</span>
                  <el-button link @click="loadReports">刷新</el-button>
                </div>
              </template>
              <el-table :data="reportList" v-loading="reportListLoading" stripe>
                <el-table-column prop="title" label="标题" min-width="150" />
                <el-table-column prop="reportType" label="类型" width="100" />
                <el-table-column prop="reportDate" label="日期" width="120" />
                <el-table-column label="材料" width="90">
                  <template #default="scope">
                    <el-tag v-if="scope.row.materials && scope.row.materials.length" size="small" type="success">已上传</el-tag>
                    <span v-else>-</span>
                  </template>
                </el-table-column>
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
                <el-table-column label="操作" width="120" fixed="right">
                  <template #default="scope">
                    <el-button link type="primary" size="small" @click="viewReportDetail(scope.row)">查看</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </el-card>
          </el-col>
        </el-row>
      </el-tab-pane>

      <!-- 微党课 -->
      <el-tab-pane label="微党课" name="course">
        <el-row :gutter="20">
          <el-col :span="8">
            <el-card>
              <template #header>
                <span>提交党课记录</span>
              </template>
              <el-form :model="courseForm" label-width="100px" label-position="top">
                <el-form-item label="微党课/推文题目" required>
                  <el-input v-model="courseForm.title" placeholder="请输入微党课/推文题目" />
                </el-form-item>
                <el-form-item label="推文发布链接" required>
                  <el-input v-model="courseForm.description" type="textarea" :rows="3" placeholder="请输入推文发布链接" />
                </el-form-item>
                <el-form-item label="发布日期" required>
                  <el-date-picker
                    v-model="courseForm.courseDate"
                    type="date"
                    placeholder="选择日期"
                    style="width: 100%"
                    value-format="YYYY-MM-DD"
                  />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" @click="submitCourse" :loading="courseLoading" style="width: 100%">提交</el-button>
                </el-form-item>
              </el-form>
            </el-card>
          </el-col>
          <el-col :span="16">
            <el-card>
              <template #header>
                <div class="card-header">
                  <span>历史记录</span>
                  <el-button link @click="loadCourses">刷新</el-button>
                </div>
              </template>
              <el-table :data="courseList" v-loading="courseListLoading" stripe>
                <el-table-column prop="title" label="微党课/推文题目" min-width="150" />
                <el-table-column prop="courseDate" label="日期" width="120" />
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
                <el-table-column label="操作" width="120" fixed="right">
                  <template #default="scope">
                    <el-button link type="primary" size="small" @click="viewCourseDetail(scope.row)">查看</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </el-card>
          </el-col>
        </el-row>
      </el-tab-pane>

      <!-- 志愿活动 -->
      <el-tab-pane label="志愿活动" name="service">
        <el-row :gutter="20">
          <el-col :span="8">
            <el-card>
              <template #header>
                <span>提交志愿活动</span>
              </template>
              <el-form :model="serviceForm" label-width="100px" label-position="top">
                <el-form-item label="服务名称" required>
                  <el-input v-model="serviceForm.serviceName" placeholder="请输入服务名称" />
                </el-form-item>
                <el-form-item label="服务描述" required>
                  <el-input v-model="serviceForm.serviceDescription" type="textarea" :rows="3" placeholder="请输入服务描述" />
                </el-form-item>
                <el-form-item label="服务日期" required>
                  <el-date-picker
                    v-model="serviceForm.serviceDate"
                    type="date"
                    placeholder="选择日期"
                    style="width: 100%"
                    value-format="YYYY-MM-DD"
                  />
                </el-form-item>
                <el-form-item label="服务类型" required>
                  <el-select v-model="serviceForm.serviceType" placeholder="请选择" style="width: 100%">
                    <el-option label="社区服务" value="社区服务" />
                    <el-option label="环保服务" value="环保服务" />
                    <el-option label="教育服务" value="教育服务" />
                    <el-option label="医疗健康" value="医疗健康" />
                    <el-option label="文化服务" value="文化服务" />
                  </el-select>
                </el-form-item>
                <el-form-item label="服务时长">
                  <el-input v-model="serviceForm.serviceHours" placeholder="如：2小时" />
                </el-form-item>
                <el-form-item label="服务地点">
                  <el-input v-model="serviceForm.serviceLocation" placeholder="可选" />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" @click="submitService" :loading="serviceLoading" style="width: 100%">提交</el-button>
                </el-form-item>
              </el-form>
            </el-card>
          </el-col>
          <el-col :span="16">
            <el-card>
              <template #header>
                <div class="card-header">
                  <span>历史记录</span>
                  <el-button link @click="loadServices">刷新</el-button>
                </div>
              </template>
              <el-table :data="serviceList" v-loading="serviceListLoading" stripe>
                <el-table-column prop="serviceName" label="服务名称" min-width="150" />
                <el-table-column prop="serviceType" label="类型" width="120" />
                <el-table-column prop="serviceHours" label="时长" width="100" />
                <el-table-column prop="serviceDate" label="日期" width="120" />
                <el-table-column label="来源" width="120"><template #default="scope">{{ scope.row.sourceTaskId ? '任务到场确认' : '学生提交' }}</template></el-table-column>
                <el-table-column prop="status" label="完成状态" width="100" />
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
                <el-table-column label="操作" width="120" fixed="right">
                  <template #default="scope">
                    <el-button link type="primary" size="small" @click="viewServiceDetail(scope.row)">查看</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </el-card>
          </el-col>
        </el-row>
      </el-tab-pane>
    </el-tabs>

    <!-- 详情对话框 -->
    <el-dialog v-model="detailDialogVisible" :title="detailTitle" width="600px">
      <el-descriptions :column="1" border v-if="currentDetail">
        <el-descriptions-item
          v-for="(value, key) in visibleDetail(currentDetail)"
          :key="key"
          :label="getFieldLabel(key)"
        >
          <template v-if="key === 'auditStatus'">
            <el-tag :type="getAuditStatusType(value)" size="small">
              {{ value || '待审核' }}
            </el-tag>
          </template>
          <template v-else>
            <template v-if="key === 'materials' && Array.isArray(value)">
              <el-space wrap>
                <el-link
                  v-for="file in value"
                  :key="file.url"
                  :href="file.url"
                  target="_blank"
                  type="primary"
                >
                  {{ file.name }}
                </el-link>
              </el-space>
            </template>
            <template v-else>
              {{ displayDetailValue(value) }}
            </template>
          </template>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- 提交申请入党对话框 -->
    <el-dialog v-model="showApplicationDialog" title="提交入党申请" width="600px">
      <el-form :model="applicationForm" label-width="120px">
        <el-form-item label="申请日期" required>
          <el-date-picker
            v-model="applicationForm.applicationDate"
            type="date"
            placeholder="选择日期"
            style="width: 100%"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        <el-form-item label="所属支部" required>
          <el-select v-model="applicationForm.branch" placeholder="请选择党支部" style="width: 100%">
            <el-option v-for="branch in PARTY_BRANCHES" :key="branch" :label="branch" :value="branch" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="applicationForm.remark" type="textarea" :rows="3" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showApplicationDialog = false">取消</el-button>
        <el-button type="primary" @click="submitApplication" :loading="applicationLoading">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { PARTY_BRANCHES } from '@/constants/partyBranches'
import { ref, reactive, onMounted, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { User, Upload } from '@element-plus/icons-vue'
import { useStore } from 'vuex'
import { useRoute, useRouter } from 'vue-router'
import { partyMemberApi, thoughtReportApi, partyCourseApi, volunteerServiceApi, partyApplicationApi, uploadPartyFile } from '@/api/party'
import { getProfile } from '@/api/student'

const store = useStore()
const route = useRoute()
const router = useRouter()

// Tab切换
const activeTab = ref('member')
const tabPathMap = {
  member: '/party/member',
  report: '/party/report',
  course: '/party/course',
  service: '/party/service'
}
const isSeparatedSubPage = computed(() => Boolean(route.path.split('/')[2]))

// 学生个人信息（用于判断政治面貌）
const studentProfile = ref(null)
const isPartyMember = computed(() => {
  if (!studentProfile.value || !studentProfile.value.politicalStatus) {
    return false
  }
  const status = studentProfile.value.politicalStatus
  return status === '中共党员' || status === '中共预备党员'
})

// 党员信息
const memberInfo = reactive({
  id: '',
  studentId: '',
  name: '',
  gender: '',
  nation: '',
  birth: '',
  nativePlace: '',
  className: '',
  branch: '',
  joinDate: '',
  isRegular: '',
  applicationNo: '',
  position: '',
  rewardsPunishments: '',
  avatar: '',
  auditStatus: '',
  auditComment: ''
})

// 申请入党信息
const applicationInfo = reactive({
  id: '',
  studentId: '',
  name: '',
  currentStage: '',
  applicationDate: '',
  branch: '',
  submittedMaterials: [],
  auditStatus: '',
  auditComment: ''
})

// 申请入党表单
const applicationForm = reactive({
  applicationDate: '',
  branch: '',
  remark: ''
})
const applicationLoading = ref(false)

const editMemberDialog = ref(false)
const showApplicationDialog = ref(false)
const photoInput = ref(null)

// 思想汇报
const reportForm = reactive({
  title: '',
  content: '',
  reportDate: '',
  reportType: '',
  remark: '',
  materials: []
})
const reportLoading = ref(false)
const reportList = ref([])
const reportListLoading = ref(false)
const reportFileList = ref([])

// 微党课
const courseForm = reactive({
  title: '',
  description: '',
  courseDate: '',
  courseType: '',
  instructor: '',
  location: '',
  duration: '',
  materials: '',
  participants: '',
  remark: ''
})
const courseLoading = ref(false)
const courseList = ref([])
const courseListLoading = ref(false)

// 志愿活动
const serviceForm = reactive({
  serviceName: '',
  serviceDescription: '',
  serviceDate: '',
  serviceType: '',
  serviceHours: '',
  serviceLocation: '',
  organization: '',
  contactPerson: '',
  contactPhone: '',
  remark: ''
})
const serviceLoading = ref(false)
const serviceList = ref([])
const serviceListLoading = ref(false)

// 详情对话框
const detailDialogVisible = ref(false)
const detailTitle = ref('')
const currentDetail = ref(null)

// 获取审核状态类型
const getAuditStatusType = (status) => {
  if (!status) return 'info'
  switch (status) {
    case '待审核':
    case 'pending':
      return 'warning'
    case '通过':
    case 'approved':
      return 'success'
    case '拒绝':
    case 'rejected':
      return 'danger'
    default:
      return 'info'
  }
}

// 格式化日期时间
const formatDateTime = (dateTime) => {
  if (!dateTime) return '-'
  try {
    const date = new Date(dateTime)
    return date.toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    })
  } catch (e) {
    return dateTime
  }
}

// 获取字段标签
const displayDetailValue = value => ({ pending: '待审核', approved: '已通过', rejected: '已驳回', submitted: '已提交', completed: '已完成' }[value] || value || '-')
const visibleDetail = row => Object.fromEntries(Object.entries(row || {}).filter(([key, value]) => getFieldLabel(key) !== key && value != null && value !== '' && !['studentId', 'courseType', 'instructor', 'location'].includes(key)))

const getFieldLabel = (key) => {
  const labelMap = {
    studentName: '学生姓名',
    status: '状态',
    auditTime: '审核时间',
    title: '标题',
    content: '内容',
    description: '推文发布链接',
    reportDate: '汇报日期',
    reportType: '汇报类型',
    remark: '备注',
    courseDate: '发布日期',
    courseType: '课程类型',
    instructor: '授课教师',
    location: '上课地点',
    duration: '课程时长',
    serviceName: '服务名称',
    serviceDescription: '服务描述',
    serviceDate: '服务日期',
    serviceType: '服务类型',
    serviceHours: '服务时长',
    serviceLocation: '服务地点',
    organization: '服务组织',
    contactPerson: '联系人',
    contactPhone: '联系电话',
    auditStatus: '审核状态',
    auditComment: '审核意见',
    createTime: '创建时间',
    updateTime: '更新时间',
    materials: '上传材料'
  }
  return labelMap[key] || key
}

const beforeReportMaterialUpload = (file) => {
  const allowedTypes = [
    'application/pdf',
    'application/msword',
    'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    'image/jpeg',
    'image/png',
    'image/jpg'
  ]
  if (!allowedTypes.includes(file.type)) {
    ElMessage.error('只能上传 PDF、Word 或图片材料')
    return false
  }
  if (file.size / 1024 / 1024 > 10) {
    ElMessage.error('单个文件不能超过 10MB')
    return false
  }
  return true
}

const handleReportMaterialUpload = async ({ file, onSuccess, onError }) => {
  try {
    const formData = new FormData()
    formData.append('file', file)
    const response = await uploadPartyFile(formData)
    const urls = Array.isArray(response?.data) ? response.data : [response?.data || response]
    const material = {
      name: file.name,
      url: urls[0]
    }
    reportForm.materials.push(material)
    onSuccess(response, file)
    ElMessage.success('思想汇报材料上传成功')
  } catch (error) {
    console.error('思想汇报材料上传失败:', error)
    onError(error)
    ElMessage.error('材料上传失败')
  }
}

// 获取学生个人信息（用于判断政治面貌）
const loadStudentProfile = async () => {
  try {
    const response = await getProfile()
    if (response && response.code === 200 && response.data) {
      studentProfile.value = response.data
    } else if (response && response.data) {
      // 兼容直接返回data的情况
      studentProfile.value = response.data
    }
  } catch (error) {
    console.error('获取学生信息失败:', error)
  }
}

// 获取党员信息
const loadMemberInfo = async () => {
  try {
    const currentUser = store.state.user
    if (!currentUser) {
      ElMessage.warning('无法获取用户信息')
      return
    }

    // 优先使用studentId（学号），如果没有则使用id（数据库ID）
    const studentId = currentUser.studentId || currentUser.id
    if (!studentId) {
      ElMessage.warning('无法获取学号信息')
      return
    }

    // 先获取学生个人信息判断政治面貌
    await loadStudentProfile()
    
    // 如果是党员，获取党员信息
    if (isPartyMember.value) {
      const response = await partyMemberApi.getStudentPartyMembers(studentId)
      // request拦截器返回response.data，即ResponseResult对象
      if (response && response.code === 200 && response.data) {
        if (Array.isArray(response.data) && response.data.length > 0) {
          const member = response.data[0]
          Object.assign(memberInfo, member)
        }
      } else if (Array.isArray(response) && response.length > 0) {
        // 兼容直接返回数组的情况
        const member = response[0]
        Object.assign(memberInfo, member)
      }
    } else {
      // 如果不是党员，获取申请入党信息
      const response = await partyApplicationApi.getByStudentId(studentId)
      if (response && response.code === 200 && response.data) {
        Object.assign(applicationInfo, response.data)
      } else if (response && response.id) {
        // 兼容直接返回对象的情况
        Object.assign(applicationInfo, response)
      }
    }
  } catch (error) {
    console.error('获取信息失败:', error)
  }
}

// 获取阶段标签
const getStageLabel = (stage) => {
  const stageMap = {
    '提交申请书': '提交入党申请书',
    '入党积极分子': '入党积极分子',
    '发展对象': '发展对象',
    '预备党员': '预备党员',
    '正式党员': '正式党员'
  }
  return stageMap[stage] || stage || '未开始'
}

// 获取阶段类型（用于Alert颜色）
const getStageType = (stage) => {
  const stageTypeMap = {
    '提交申请书': 'info',
    '入党积极分子': 'warning',
    '发展对象': 'warning',
    '预备党员': 'success',
    '正式党员': 'success'
  }
  return stageTypeMap[stage] || 'info'
}

// 查看材料
const viewMaterial = (fileUrl) => {
  if (fileUrl) {
    window.open(fileUrl, '_blank')
  } else {
    ElMessage.warning('材料文件不存在')
  }
}

// 提交申请入党
const submitApplication = async () => {
  if (!applicationForm.applicationDate || !applicationForm.branch) {
    ElMessage.warning('请填写申请日期和所属支部')
    return
  }

  applicationLoading.value = true
  try {
    const currentUser = store.state.user
    const studentId = currentUser.studentId || currentUser.id
    
    const data = {
      studentId: studentId,
      name: currentUser.name || studentProfile.value?.name || '',
      applicationDate: applicationForm.applicationDate,
      branch: applicationForm.branch,
      currentStage: '提交申请书',
      remark: applicationForm.remark,
      submittedMaterials: []
    }
    
    const response = await partyApplicationApi.addPartyApplication(data)
    if (response && response.code === 200) {
      ElMessage.success('提交成功')
      showApplicationDialog.value = false
      Object.assign(applicationForm, {
        applicationDate: '',
        branch: '',
        remark: ''
      })
      // 重新加载申请信息
      await loadMemberInfo()
    } else {
      ElMessage.error(response?.message || response?.msg || '提交失败')
    }
  } catch (error) {
    console.error('提交申请失败:', error)
    ElMessage.error('提交失败')
  } finally {
    applicationLoading.value = false
  }
}

// 上传照片
const triggerPhotoUpload = () => {
  photoInput.value?.click()
}

const handlePhotoChange = (event) => {
  const file = event.target.files[0]
  if (file) {
    if (file.size > 2 * 1024 * 1024) {
      ElMessage.warning('照片大小不能超过2MB')
      return
    }
    const reader = new FileReader()
    reader.onload = (e) => {
      memberInfo.avatar = e.target.result
      ElMessage.success('照片上传成功（仅本地预览）')
    }
    reader.readAsDataURL(file)
  }
}

// Tab切换
const handleTabChange = (tabName, syncRoute = true) => {
  if (syncRoute && route.path !== tabPathMap[tabName]) {
    router.replace({
      path: tabPathMap[tabName],
      query: route.query
    })
  }
  if (tabName === 'report') {
    loadReports()
  } else if (tabName === 'course') {
    loadCourses()
  } else if (tabName === 'service') {
    loadServices()
  }
}

watch(() => [route.path, route.query.tab], () => {
  const pathTab = route.path.split('/')[2]
  const tab = ['member', 'report', 'course', 'service'].includes(pathTab) ? pathTab : route.query.tab
  if (['member', 'report', 'course', 'service'].includes(tab) && activeTab.value !== tab) {
    activeTab.value = tab
    handleTabChange(tab, false)
  }
}, { immediate: true })

// 思想汇报
const submitReport = async () => {
  if (!reportForm.title || !reportForm.content || !reportForm.reportDate || !reportForm.reportType) {
    ElMessage.warning('请填写完整信息')
    return
  }

  reportLoading.value = true
  try {
    const response = await thoughtReportApi.addThoughtReport(reportForm)
    if (response && response.code === 200) {
      ElMessage.success('提交成功')
      Object.assign(reportForm, {
        title: '',
        content: '',
        reportDate: '',
        reportType: '',
        remark: '',
        materials: []
      })
      reportFileList.value = []
      loadReports()
    } else {
      ElMessage.error(response?.message || response?.msg || '提交失败')
    }
  } catch (error) {
    console.error('提交思想汇报失败:', error)
    ElMessage.error('提交失败')
  } finally {
    reportLoading.value = false
  }
}

const loadReports = async () => {
  reportListLoading.value = true
  try {
    const currentUser = store.state.user
    if (!currentUser) {
      return
    }
    const studentId = currentUser.studentId || currentUser.id
    if (!studentId) {
      return
    }
    const response = await thoughtReportApi.getStudentThoughtReports(studentId)
    // 处理响应格式
    if (Array.isArray(response)) {
      reportList.value = response
    } else if (response && response.code === 200) {
      reportList.value = response.data || []
    } else if (response && response.data) {
      reportList.value = Array.isArray(response.data) ? response.data : []
    }
  } catch (error) {
    console.error('加载思想汇报失败:', error)
  } finally {
    reportListLoading.value = false
  }
}

const viewReportDetail = (row) => {
  detailTitle.value = '思想汇报详情'
  currentDetail.value = row
  detailDialogVisible.value = true
}

// 微党课
const submitCourse = async () => {
  if (!/^https?:\/\/[^\s]+$/i.test(courseForm.description)) {
    ElMessage.warning('请填写以 https:// 或 http:// 开头的推文发布链接'); return
  }
  if (!courseForm.title || !courseForm.description || !courseForm.courseDate) {
    ElMessage.warning('请填写完整信息')
    return
  }

  courseLoading.value = true
  try {
    const response = await partyCourseApi.addPartyCourse(courseForm)
    if (response && response.code === 200) {
      ElMessage.success('提交成功')
      Object.assign(courseForm, {
        title: '',
        description: '',
        courseDate: '',
        courseType: '',
        instructor: '',
        location: '',
        duration: '',
        materials: '',
        participants: '',
        remark: ''
      })
      loadCourses()
    } else {
      ElMessage.error(response?.message || response?.msg || '提交失败')
    }
  } catch (error) {
    console.error('提交微党课失败:', error)
    ElMessage.error('提交失败')
  } finally {
    courseLoading.value = false
  }
}

const loadCourses = async () => {
  courseListLoading.value = true
  try {
    const currentUser = store.state.user
    if (!currentUser) {
      return
    }
    const studentId = currentUser.studentId || currentUser.id
    if (!studentId) {
      return
    }
    const response = await partyCourseApi.getStudentPartyCourses(studentId)
    // 处理响应格式（ResponseResult格式）
    if (response && response.code === 200 && response.data) {
      courseList.value = Array.isArray(response.data) ? response.data : []
    } else if (Array.isArray(response)) {
      // 兼容直接返回数组的情况
      courseList.value = response
    }
  } catch (error) {
    console.error('加载微党课失败:', error)
  } finally {
    courseListLoading.value = false
  }
}

const viewCourseDetail = (row) => {
  detailTitle.value = '微党课详情'
  currentDetail.value = row
  detailDialogVisible.value = true
}

// 志愿活动
const submitService = async () => {
  if (!serviceForm.serviceName || !serviceForm.serviceDescription || !serviceForm.serviceDate || !serviceForm.serviceType) {
    ElMessage.warning('请填写完整信息')
    return
  }

  serviceLoading.value = true
  try {
    const response = await volunteerServiceApi.addVolunteerService(serviceForm)
    if (response && response.code === 200) {
      ElMessage.success('提交成功')
      Object.assign(serviceForm, {
        serviceName: '',
        serviceDescription: '',
        serviceDate: '',
        serviceType: '',
        serviceHours: '',
        serviceLocation: '',
        organization: '',
        contactPerson: '',
        contactPhone: '',
        remark: ''
      })
      loadServices()
    } else {
      ElMessage.error(response?.message || response?.msg || '提交失败')
    }
  } catch (error) {
    console.error('提交志愿活动失败:', error)
    ElMessage.error('提交失败')
  } finally {
    serviceLoading.value = false
  }
}

const loadServices = async () => {
  serviceListLoading.value = true
  try {
    const currentUser = store.state.user
    if (!currentUser) {
      return
    }
    const studentId = currentUser.studentId || currentUser.id
    if (!studentId) {
      return
    }
    const response = await volunteerServiceApi.getStudentVolunteerServices(studentId)
    // 处理响应格式（ResponseResult格式）
    if (response && response.code === 200 && response.data) {
      serviceList.value = Array.isArray(response.data) ? response.data : []
    } else if (Array.isArray(response)) {
      // 兼容直接返回数组的情况
      serviceList.value = response
    }
  } catch (error) {
    console.error('加载志愿活动失败:', error)
  } finally {
    serviceListLoading.value = false
  }
}

const viewServiceDetail = (row) => {
  detailTitle.value = '志愿活动详情'
  currentDetail.value = row
  detailDialogVisible.value = true
}

onMounted(() => {
  loadMemberInfo()
})
</script>

<style scoped>
.party-page {
  padding: 20px;
}

.page-header {
  margin-bottom: 20px;
}

.header-content h2 {
  margin: 0 0 8px 0;
  font-size: 24px;
  color: #303133;
}

.header-content p {
  margin: 0;
  color: #909399;
  font-size: 14px;
}

.party-tabs {
  margin-top: 20px;
}

.external-tab-mode :deep(.el-tabs__header) {
  display: none;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.material-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

.report-upload {
  width: 100%;
}

.member-info-container {
  display: flex;
  gap: 30px;
}

.avatar-section {
  flex-shrink: 0;
}

.avatar-wrapper {
  position: relative;
  width: 200px;
  height: 260px;
  border: 1px dashed #d9d9d9;
  border-radius: 8px;
  overflow: hidden;
  background-color: #fafafa;
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-placeholder {
  font-size: 80px;
  color: #c0c4cc;
}

.upload-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: white;
  opacity: 0;
  transition: opacity 0.3s;
  cursor: pointer;
}

.upload-overlay:hover {
  opacity: 1;
}

.upload-overlay .el-icon {
  font-size: 32px;
  margin-bottom: 8px;
}

.member-descriptions {
  flex: 1;
}

.audit-comment {
  margin-left: 8px;
  color: #909399;
  font-size: 12px;
}

:deep(.el-tabs__content) {
  padding-top: 20px;
}

:deep(.el-card__body) {
  padding: 20px;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .party-page {
    padding: 10px;
  }

  .page-header {
    margin-bottom: 15px;
  }

  .header-content h2 {
    font-size: 18px;
  }

  .header-content p {
    font-size: 13px;
  }

  .member-info-container {
    flex-direction: column;
  }

  .avatar-section {
    margin-bottom: 20px;
    align-self: center;
  }

  .card-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }

  :deep(.el-tabs__item) {
    padding: 0 12px;
    font-size: 13px;
  }

  :deep(.el-descriptions) {
    font-size: 12px;
  }

  :deep(.el-descriptions__label) {
    width: 90px !important;
    font-size: 12px;
  }

  :deep(.el-table) {
    font-size: 12px;
  }

  :deep(.el-form-item__label) {
    font-size: 13px;
  }

  :deep(.el-row) {
    margin: 0 !important;
  }

  :deep(.el-col) {
    padding: 0 !important;
    margin-bottom: 15px;
  }

  .todo-time-info {
    align-items: flex-start;
    margin-top: 8px;
  }
}

@media (max-width: 480px) {
  .party-page {
    padding: 8px;
  }

  .header-content h2 {
    font-size: 16px;
  }

  .header-content p {
    font-size: 12px;
  }

  :deep(.el-tabs__item) {
    padding: 0 8px;
    font-size: 12px;
  }

  :deep(.el-descriptions__label) {
    width: 80px !important;
    font-size: 11px;
  }

  :deep(.el-table) {
    font-size: 11px;
  }

  :deep(.el-card__body) {
    padding: 12px;
  }
}
</style>
