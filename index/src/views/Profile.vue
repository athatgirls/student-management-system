/* eslint-disable vue/multi-word-component-names */
<template>
  <div class="profile">
    <el-card v-if="!isStudyPage">
      <template #header>
        <div class="card-header">
          <span>个人信息</span>
          <div>
            <el-button type="warning" @click="showChangePasswordDialog = true" style="margin-right: 10px;">
              修改密码
            </el-button>
            <el-button type="primary" @click="handleEdit">
              {{ editMode ? '保存' : '编辑' }}
            </el-button>
          </div>
        </div>
      </template>
      
      <el-form
        ref="formRef"
        :model="profileForm"
        :rules="rules"
        :label-width="isMobile ? '90px' : '120px'"
        :disabled="!editMode"
        class="profile-form"
      >
        <el-row :gutter="20">
          <el-col :xs="24" :sm="24" :md="12" :lg="12">
            <el-form-item label="学号" prop="studentId">
              <el-input v-model="profileForm.studentId" disabled />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="24" :md="12" :lg="12">
            <el-form-item label="姓名" prop="name">
              <el-input v-model="profileForm.name" disabled title="请联系管理员修改" />
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-row :gutter="20">
          <el-col :xs="24" :sm="8" :md="8" :lg="8">
            <el-form-item label="性别" prop="gender">
              <el-select v-model="profileForm.gender" placeholder="请选择性别" style="width: 100%">
                <el-option label="男" value="男" />
                <el-option label="女" value="女" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="8" :md="8" :lg="8">
            <el-form-item label="年龄" prop="age">
              <el-input-number v-model="profileForm.age" disabled style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="8" :md="8" :lg="8">
            <el-form-item label="民族" prop="nation">
              <el-input v-model="profileForm.nation" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :xs="24" :sm="24" :md="12" :lg="12">
            <el-form-item label="出生日期" prop="birthDate">
              <el-date-picker
                v-model="profileForm.birthDate"
                type="date"
                placeholder="选择日期"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="24" :md="12" :lg="12">
            <el-form-item label="宿舍信息" prop="dormitory">
              <el-input v-model="profileForm.dormitory" placeholder="如：南苑 3 栋 502" />
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-row :gutter="20">
          <el-col :xs="24" :sm="24" :md="12" :lg="12">
            <el-form-item label="专业" prop="major">
              <el-input v-model="profileForm.major" placeholder="请输入专业" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="24" :md="12" :lg="12">
            <el-form-item label="年级" prop="grade">
              <el-input v-model="profileForm.grade" disabled placeholder="请联系管理员设置年级" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :xs="24" :sm="24" :md="12" :lg="12">
            <el-form-item label="班级" prop="className">
              <el-input v-model="profileForm.className" placeholder="请输入班级" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="24" :md="12" :lg="12">
            <el-form-item label="学制类型" prop="educationType">
              <el-select v-model="profileForm.educationType" disabled placeholder="请联系管理员设置" style="width: 100%">
                <el-option label="全日制" value="全日制" />
                <el-option label="非全日制" value="非全日制" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-row :gutter="20">
          <el-col :xs="24" :sm="24" :md="12" :lg="12">
            <el-form-item label="手机号码" prop="phone">
              <el-input v-model="profileForm.phone" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="24" :md="12" :lg="12">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="profileForm.email" />
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-row :gutter="20">
          <el-col :xs="24" :sm="24" :md="12" :lg="12">
            <el-form-item label="籍贯" prop="nativePlace">
              <el-input v-model="profileForm.nativePlace" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="24" :md="12" :lg="12">
            <el-form-item label="紧急联系人" prop="emergencyContact">
              <el-input v-model="profileForm.emergencyContact" placeholder="姓名 / 电话" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :xs="24" :sm="24" :md="12" :lg="12">
            <el-form-item label="政治面貌" prop="politicalStatus">
              <el-select v-model="profileForm.politicalStatus" placeholder="请选择政治面貌" style="width: 100%">
                <el-option
                  v-for="status in POLITICAL_STATUS_OPTIONS"
                  :key="status"
                  :label="status"
                  :value="status"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="24" :md="12" :lg="12">
            <el-form-item label="婚姻状况" prop="maritalStatus">
              <el-select v-model="profileForm.maritalStatus" placeholder="请选择婚姻状况" style="width: 100%">
                <el-option label="未婚" value="未婚" />
                <el-option label="已婚" value="已婚" />
                <el-option label="离异" value="离异" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-row :gutter="20">
          <el-col :xs="24" :sm="24" :md="8" :lg="8">
            <el-form-item label="导师" prop="supervisor">
              <el-input v-model="profileForm.supervisor" placeholder="请输入导师" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="24" :md="8" :lg="8">
            <el-form-item label="研究方向" prop="researchDirection">
              <el-input v-model="profileForm.researchDirection" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="24" :md="8" :lg="8">
            <el-form-item label="任职情况" prop="workStatus">
              <el-input v-model="profileForm.workStatus" disabled title="请联系管理员修改" />
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-form-item label="个人简介" prop="introduction">
          <el-input
            v-model="profileForm.introduction"
            type="textarea"
            :rows="4"
            placeholder="请输入个人简介"
          />
        </el-form-item>
      </el-form>
    </el-card>

    <el-card v-if="isStudyPage">
      <template #header>
        <div class="card-header">
          <span>学习记录</span>
          <el-button type="primary" @click="editStudy()">新增学习记录</el-button>
        </div>
      </template>
      
      <el-table :data="studyRecords" style="width: 100%">
        <el-table-column prop="semester" label="学期" width="120" />
        <el-table-column label="课程名称">
          <template #default="scope">
            {{ scope.row.course || scope.row.courseName || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="score" label="成绩" width="100" />
        <el-table-column prop="credit" label="学分" width="100" />
        <el-table-column label="材料"><template #default="{ row }"><RecordAttachments :model-value="row.attachments || ''" /></template></el-table-column>
        <el-table-column label="操作" width="160"><template #default="{ row }">
          <el-button v-if="row.source === 'student'" link type="primary" @click="editStudy(row)">编辑</el-button>
          <span v-else>教务导入（只读）</span>
        </template></el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.status === '已通过' ? 'success' : 'warning'">
              {{ scope.row.status }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
    
    <el-dialog v-model="studyDialog" title="填写学习记录" width="min(560px, 95vw)">
      <el-form label-width="90px">
        <el-form-item label="学期" required><el-input v-model="studyForm.semester" placeholder="例如：2026-2027 第一学期" /></el-form-item>
        <el-form-item label="课程名称" required><el-input v-model="studyForm.course" /></el-form-item>
        <el-form-item label="成绩" required><el-input-number v-model="studyForm.score" :min="0" :max="100" /></el-form-item>
        <el-form-item label="学分" required><el-input-number v-model="studyForm.credit" :min="0" :precision="1" /></el-form-item>
        <el-form-item label="证明材料" required><RecordAttachments v-model="studyForm.attachments" editable /></el-form-item>
      </el-form>
      <template #footer><el-button @click="studyDialog = false">取消</el-button><el-button type="primary" :loading="studySaving" @click="saveStudy">保存</el-button></template>
    </el-dialog>
    <!-- 修改密码对话框 -->
    <el-dialog
      v-model="showChangePasswordDialog"
      title="修改密码"
      width="500px"
      @close="handlePasswordDialogClose"
    >
      <el-form
        ref="passwordFormRef"
        :model="passwordForm"
        :rules="passwordRules"
        label-width="100px"
      >
        <el-form-item label="旧密码" prop="oldPassword">
          <el-input
            v-model="passwordForm.oldPassword"
            type="password"
            placeholder="请输入旧密码"
            show-password
          />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="passwordForm.newPassword"
            type="password"
            placeholder="请输入新密码（至少8位，包含数字和字母）"
            show-password
          />
          <div class="password-tip">
            <p style="margin: 5px 0 0 0; color: #909399; font-size: 12px;">
              密码规则：不少于8位，必须包含数字和字母
            </p>
          </div>
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="passwordForm.confirmPassword"
            type="password"
            placeholder="请再次输入新密码"
            show-password
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showChangePasswordDialog = false">取消</el-button>
        <el-button type="primary" @click="handleChangePassword" :loading="passwordLoading">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed, watch } from 'vue'
import { useStore } from 'vuex'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getProfile, updateProfile, getStudyRecords, changePassword } from '@/api/student'
import { POLITICAL_STATUS_OPTIONS } from '@/constants/politicalStatus'
import RecordAttachments from '@/components/RecordAttachments.vue'
import request from '@/api/request'

const store = useStore()
const route = useRoute()
const formRef = ref()
const passwordFormRef = ref()
const isStudyPage = computed(() => route.path === '/profile/study' || route.query.tab === 'study')
const editMode = ref(false)
const showChangePasswordDialog = ref(false)
const passwordLoading = ref(false)

// 检测是否为移动端
const windowWidth = ref(typeof window !== 'undefined' ? window.innerWidth : 1920)
const isMobile = computed(() => windowWidth.value <= 768)

// 获取当前用户信息
const userInfo = computed(() => store.state.user || {})

const profileForm = reactive({
  studentId: '',
  name: '',
  gender: '',
  age: 0,
  nation: '',
  birthDate: '',
  dormitory: '',
  major: '',
  grade: '',
  className: '',
  educationType: '',
  phone: '',
  email: '',
  nativePlace: '',
  emergencyContact: '',
  politicalStatus: '',
  maritalStatus: '',
  supervisor: '',
  researchDirection: '',
  workStatus: '',
  introduction: ''
})

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
  phone: [
    { required: true, message: '请输入手机号码', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱地址', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ],
  dormitory: [
    { required: true, message: '请输入宿舍信息', trigger: 'blur' }
  ],
  nation: [
    { required: true, message: '请输入民族', trigger: 'blur' }
  ],
  birthDate: [
    { required: true, message: '请选择出生日期', trigger: 'change' }
  ]
}

// 修改密码表单
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// 密码验证规则
const validatePassword = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请输入新密码'))
    return
  }
  if (value.length < 8) {
    callback(new Error('密码长度不能少于8位'))
    return
  }
  // 检查是否包含数字
  if (!/\d/.test(value)) {
    callback(new Error('密码必须包含至少一个数字'))
    return
  }
  // 检查是否包含字母
  if (!/[a-zA-Z]/.test(value)) {
    callback(new Error('密码必须包含至少一个字母'))
    return
  }
  callback()
}

const validateConfirmPassword = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请再次输入新密码'))
    return
  }
  if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const passwordRules = {
  oldPassword: [
    { required: true, message: '请输入旧密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { validator: validatePassword, trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

// 自动计算年龄
const calculateAge = (birthDate) => {
  if (!birthDate) return 0
  const birth = new Date(birthDate)
  const today = new Date()
  let age = today.getFullYear() - birth.getFullYear()
  const monthDiff = today.getMonth() - birth.getMonth()
  if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birth.getDate())) {
    age--
  }
  return age
}

// 监听出生日期变化并自动计算年龄
watch(() => profileForm.birthDate, (newVal) => {
  if (newVal) {
    profileForm.age = calculateAge(newVal)
  }
}, { immediate: true })

const studyRecords = ref([])
const studyDialog = ref(false)
const studySaving = ref(false)
const studyForm = reactive({})
const editStudy = row => {
  Object.keys(studyForm).forEach(k => delete studyForm[k])
  Object.assign(studyForm, row || { semester: '', course: '', score: 0, credit: 0, attachments: '' })
  studyDialog.value = true
}
const saveStudy = async () => {
  if (!studyForm.semester?.trim() || !studyForm.course?.trim() || !JSON.parse(studyForm.attachments || '[]').length) {
    ElMessage.warning('请填写学期、课程名称并上传证明材料'); return
  }
  studySaving.value = true
  try {
    await request.post('/study-records', studyForm)
    studyRecords.value = (await getStudyRecords()).data || []
    studyDialog.value = false; ElMessage.success('保存成功')
  } catch { /* shared interceptor displays errors */ }
  finally { studySaving.value = false }
}

const loadProfile = async () => {
  try {
    // 获取个人信息（自动获取当前用户）
    const profileResponse = await getProfile()
    
    if (profileResponse.code === 200) {
      const profileData = profileResponse.data
      // 将后端数据映射到表单字段
      Object.assign(profileForm, {
        studentId: profileData.studentId || '',
        name: profileData.name || '',
        gender: profileData.gender || '',
        age: profileData.age || 0,
        nation: profileData.nation || '',
        birthDate: profileData.birthDate ? new Date(profileData.birthDate) : '',
        dormitory: profileData.dormitory || profileData.dorm || '',
        major: profileData.major || '',
        grade: profileData.grade || '',
        className: profileData.className || '',
        educationType: profileData.educationType || '',
        phone: profileData.phone || '',
        email: profileData.email || '',
        nativePlace: profileData.nativePlace || '',
        emergencyContact: profileData.emergencyContact || '',
        politicalStatus: profileData.politicalStatus || '',
        maritalStatus: profileData.maritalStatus || '',
        supervisor: profileData.supervisor || '',
        researchDirection: profileData.researchDirection || '',
        workStatus: profileData.workStatus || '',
        introduction: profileData.introduction || ''
      })
    } else {
      ElMessage.error(profileResponse.msg || '获取个人信息失败')
    }

    // 获取学习记录
    const recordsResponse = await getStudyRecords()
    if (recordsResponse.code === 200) {
      studyRecords.value = recordsResponse.data
    }
  } catch (error) {
    console.error('加载个人信息失败:', error)
    ElMessage.error('加载个人信息失败')
    // 如果API调用失败，只显示基本信息
    if (userInfo.value) {
      Object.assign(profileForm, {
        studentId: userInfo.value.studentId || '',
        name: userInfo.value.name || '',
        // 其他敏感信息不显示，需要重新登录获取
      })
    }
  }
}

const handleEdit = async () => {
  if (!editMode.value) {
    editMode.value = true
    return
  }

  try {
    await formRef.value.validate()
  } catch {
    // Field-level messages already explain validation failures.
    return
  }

  try {
    // 准备要更新的数据
    const updateData = {
      ...profileForm,
      birthDate: profileForm.birthDate
        ? (profileForm.birthDate instanceof Date ? profileForm.birthDate : new Date(profileForm.birthDate))
        : null
    }
    
    const response = await updateProfile(updateData)
    if (response.code === 200) {
      ElMessage.success('保存成功')
      editMode.value = false
      // 更新store中的用户信息
      store.commit('setUser', { ...userInfo.value, ...response.data })
    } else {
      ElMessage.error(response.msg || '保存失败')
    }
  } catch (error) {
    console.error('保存失败:', error)
    // Axios errors are shown once by the shared response interceptor.
    if (!error.isAxiosError) ElMessage.error('保存个人信息失败，请稍后重试')
  }
}

// 修改密码
const handleChangePassword = async () => {
  try {
    await passwordFormRef.value.validate()
    
    passwordLoading.value = true
    const response = await changePassword(
      passwordForm.oldPassword,
      passwordForm.newPassword
    )
    
    if (response.code === 200) {
      ElMessage.success('密码修改成功，请重新登录')
      showChangePasswordDialog.value = false
      // 延迟跳转到登录页，让用户看到成功提示
      setTimeout(() => {
        store.dispatch('logout')
        window.location.href = '/login'
      }, 1500)
    } else {
      ElMessage.error(response.msg || '密码修改失败')
    }
  } catch (error) {
    if (error !== false) { // 表单验证失败会返回false
      console.error('修改密码失败:', error)
      ElMessage.error('修改密码失败')
    }
  } finally {
    passwordLoading.value = false
  }
}

// 关闭密码对话框
const handlePasswordDialogClose = () => {
  passwordFormRef.value?.resetFields()
  Object.assign(passwordForm, {
    oldPassword: '',
    newPassword: '',
    confirmPassword: ''
  })
}

onMounted(() => {
  loadProfile()
  // 初始化窗口宽度并监听变化
  if (typeof window !== 'undefined') {
    windowWidth.value = window.innerWidth
    window.addEventListener('resize', () => {
      windowWidth.value = window.innerWidth
    })
  }
})
</script>

<style scoped>
.profile {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .profile {
    padding: 10px;
  }

  .card-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }

  .card-header > div {
    width: 100%;
    display: flex;
    gap: 10px;
  }

  .card-header .el-button {
    flex: 1;
    margin: 0 !important;
  }

  .profile-form :deep(.el-form-item__label) {
    font-size: 13px;
    padding-right: 8px;
    line-height: 32px;
  }

  .profile-form :deep(.el-input__inner),
  .profile-form :deep(.el-select),
  .profile-form :deep(.el-date-editor) {
    font-size: 14px;
  }

  .profile-form :deep(.el-form-item) {
    margin-bottom: 18px;
  }

  .profile-form :deep(.el-row) {
    margin-left: 0 !important;
    margin-right: 0 !important;
  }

  .profile-form :deep(.el-col) {
    padding-left: 0 !important;
    padding-right: 0 !important;
    margin-bottom: 0;
  }

  /* 表格在移动端的适配 */
  :deep(.el-table) {
    font-size: 12px;
  }

  :deep(.el-table th),
  :deep(.el-table td) {
    padding: 8px 4px;
  }
}

@media (max-width: 480px) {
  .profile {
    padding: 8px;
  }

  .card-header {
    gap: 8px;
  }

  .card-header .el-button {
    font-size: 13px;
    padding: 8px 12px;
  }

  .profile-form :deep(.el-form-item__label) {
    font-size: 12px;
    width: 85px !important;
    padding-right: 6px;
  }

  .profile-form :deep(.el-form-item__content) {
    margin-left: 85px !important;
  }

  .profile-form :deep(.el-input__inner),
  .profile-form :deep(.el-select),
  .profile-form :deep(.el-date-editor) {
    font-size: 13px;
    height: 32px;
    line-height: 32px;
  }

  .profile-form :deep(.el-input-number) {
    width: 100%;
  }

  .profile-form :deep(.el-input-number .el-input__inner) {
    width: 100%;
  }

  .profile-form :deep(.el-textarea__inner) {
    font-size: 13px;
    padding: 8px;
  }

  /* 对话框在移动端的适配 */
  :deep(.el-dialog) {
    width: 95% !important;
    margin: 5vh auto 0 !important;
  }

  :deep(.el-dialog__header) {
    padding: 15px 15px 10px;
  }

  :deep(.el-dialog__body) {
    padding: 15px;
  }

  :deep(.el-dialog__footer) {
    padding: 10px 15px 15px;
  }

  /* 表格在移动端的适配 */
  :deep(.el-table) {
    font-size: 11px;
  }

  :deep(.el-table th),
  :deep(.el-table td) {
    padding: 6px 2px;
  }

  :deep(.el-table .cell) {
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
}
</style> 
