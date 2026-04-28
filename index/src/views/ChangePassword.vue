<template>
  <div class="change-password-container">
    <div class="change-password-box">
      <div class="change-password-header">
        <h2>修改初始密码</h2>
        <p>为了账户安全，请先修改初始密码</p>
      </div>
      
      <el-alert
        type="warning"
        :closable="false"
        show-icon
        style="margin-bottom: 20px"
      >
        <template #title>
          <div>
            <p style="margin: 0 0 8px 0; font-weight: bold;">检测到您使用的是初始密码</p>
            <p style="margin: 0; font-size: 13px;">请设置新密码后再登录系统</p>
          </div>
        </template>
      </el-alert>
      
      <el-form
        ref="passwordFormRef"
        :model="passwordForm"
        :rules="passwordRules"
        label-width="100px"
        class="password-form"
      >
        <el-form-item label="学号">
          <el-input v-model="studentInfo.studentId" disabled />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="studentInfo.studentName" disabled />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="passwordForm.newPassword"
            type="password"
            placeholder="请输入新密码（至少8位，包含数字和字母）"
            show-password
            size="large"
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
            size="large"
          />
        </el-form-item>
        <el-form-item>
          <el-button 
            type="primary" 
            @click="handleChangePassword" 
            :loading="loading"
            size="large"
            style="width: 100%"
          >
            确定修改
          </el-button>
        </el-form-item>
        <el-form-item>
          <el-button 
            @click="handleBackToLogin" 
            size="large"
            style="width: 100%"
          >
            返回登录
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { changeInitialPassword } from '@/api/student'

const router = useRouter()
const route = useRoute()
const passwordFormRef = ref()
const loading = ref(false)

// 从路由参数获取学生信息
const studentInfo = reactive({
  studentId: '',
  studentName: ''
})

// 密码表单
const passwordForm = reactive({
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
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { validator: validatePassword, trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

// 修改密码
const handleChangePassword = async () => {
  try {
    await passwordFormRef.value.validate()
    
    if (!studentInfo.studentId) {
      ElMessage.error('学号信息缺失，请返回登录页面重新操作')
      return
    }
    
    loading.value = true
    
    // 调用修改初始密码接口
    const res = await changeInitialPassword(
      studentInfo.studentId,
      passwordForm.newPassword
    )
    
    if (res.code === 200) {
      ElMessage.success({
        message: '密码修改成功！请返回登录页面使用新密码登录',
        duration: 3000
      })
      // 延迟跳转到登录页
      setTimeout(() => {
        router.push('/login')
      }, 2000)
    } else {
      ElMessage.error(res.msg || '密码修改失败')
    }
    
    loading.value = false
  } catch (error) {
    if (error !== false) {
      console.error('修改密码失败:', error)
      // 如果error有response，显示后端返回的错误信息
      if (error.response && error.response.data && error.response.data.msg) {
        ElMessage.error(error.response.data.msg || '密码修改失败')
      } else if (error.msg) {
        ElMessage.error(error.msg || '密码修改失败')
      } else {
        ElMessage.error('修改密码失败，请检查网络连接')
      }
    }
    loading.value = false
  }
}

// 返回登录
const handleBackToLogin = () => {
  router.push('/login')
}

// 页面加载时从路由参数获取学生信息
onMounted(() => {
  if (route.query.studentId) {
    studentInfo.studentId = route.query.studentId
    studentInfo.studentName = route.query.studentName || ''
  } else {
    ElMessage.warning('缺少学生信息，正在返回登录页面')
    setTimeout(() => {
      router.push('/login')
    }, 1500)
  }
})
</script>

<style scoped>
.change-password-container {
  height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 20px;
}

.change-password-box {
  width: 500px;
  max-width: 90vw;
  padding: 40px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
}

.change-password-header {
  text-align: center;
  margin-bottom: 30px;
}

.change-password-header h2 {
  color: #303133;
  margin-bottom: 8px;
  font-size: 24px;
}

.change-password-header p {
  color: #909399;
  font-size: 14px;
}

.password-form {
  margin-top: 20px;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .change-password-container {
    padding: 10px;
  }

  .change-password-box {
    width: 100%;
    max-width: 100%;
    padding: 30px 20px;
  }

  .change-password-header h2 {
    font-size: 20px;
  }

  .change-password-header p {
    font-size: 13px;
  }
}

@media (max-width: 480px) {
  .change-password-box {
    padding: 20px 15px;
  }

  .change-password-header h2 {
    font-size: 18px;
  }

  .change-password-header p {
    font-size: 12px;
  }
}
</style>

