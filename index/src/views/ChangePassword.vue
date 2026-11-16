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
        v-if="studentInfo"
        ref="passwordFormRef"
        :model="passwordForm"
        :rules="passwordRules"
        label-width="100px"
        class="password-form"
      >
        <el-form-item label="学号">
          <el-input :model-value="studentInfo.studentId" disabled />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input :model-value="studentInfo.studentName" disabled />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="passwordForm.newPassword"
            type="password"
            placeholder="请输入新密码（至少8位，包含数字和字母）"
            show-password
            size="large"
            autocomplete="new-password"
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
            autocomplete="new-password"
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
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { changeInitialPassword } from '@/api/student'
import { getPendingPasswordChange, clearPendingPasswordChange } from '@/utils/initialPasswordChange'

const router = useRouter()
const passwordFormRef = ref()
const loading = ref(false)

const studentInfo = getPendingPasswordChange()

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

const handleChangePassword = async () => {
  if (loading.value || !studentInfo?.token) return
  loading.value = true
  try {
    const valid = await passwordFormRef.value.validate().catch(() => false)
    if (!valid) return
    const res = await changeInitialPassword(studentInfo.token, passwordForm.newPassword)
    if (res.code === 200) {
      ElMessage.success('密码修改成功，请使用新密码登录')
      await router.replace('/login')
    } else if (res.code === 410) {
      ElMessage.warning(res.msg)
      await router.replace('/login')
    } else {
      ElMessage.error(res.msg || '密码修改失败')
    }
  } catch (error) {
    ElMessage.error(error.response?.data?.msg || '修改密码失败，请检查网络连接')
  } finally {
    loading.value = false
  }
}

const handleBackToLogin = () => {
  router.replace('/login')
}

onMounted(() => {
  if (!studentInfo?.token) {
    ElMessage.warning('请先登录后再设置新密码')
    router.replace('/login')
  }
})
onUnmounted(clearPendingPasswordChange)
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
