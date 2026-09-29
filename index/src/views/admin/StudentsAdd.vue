<template>
  <div class="students-add">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>添加学生</span>
        </div>
      </template>
      
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
      >
        <el-row :gutter="20">
          <el-col :xs="24" :sm="24" :md="12" :lg="12">
            <el-form-item label="学号" prop="studentId">
              <el-input v-model="form.studentId" placeholder="请输入学号" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="24" :md="12" :lg="12">
            <el-form-item label="姓名" prop="name">
              <el-input v-model="form.name" placeholder="请输入姓名" />
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-row :gutter="20">
          <el-col :xs="24" :sm="24" :md="12" :lg="12">
            <el-form-item label="性别" prop="gender">
              <el-select v-model="form.gender" placeholder="请选择性别" style="width: 100%">
                <el-option label="男" value="男" />
                <el-option label="女" value="女" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="24" :md="12" :lg="12">
            <el-form-item label="年龄" prop="age">
              <el-input-number v-model="form.age" :min="0" :max="100" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-row :gutter="20">
          <el-col :xs="24" :sm="24" :md="12" :lg="12">
            <el-form-item label="专业" prop="major">
              <div class="major-field">
                <el-select v-model="form.major" placeholder="请选择专业" style="width: 100%">
                  <el-option v-for="major in majorOptions" :key="major" :label="major" :value="major" />
                </el-select>
                <el-button type="primary" link @click="showMajorManageDialog = true">管理专业</el-button>
              </div>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="24" :md="12" :lg="12">
            <el-form-item label="年级" prop="grade">
              <el-input v-model="form.grade" placeholder="请输入年级，例如：2024级" />
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-row :gutter="20">
          <el-col :xs="24" :sm="24" :md="12" :lg="12">
            <el-form-item label="班级" prop="className">
              <el-input v-model="form.className" placeholder="请输入班级" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="24" :md="12" :lg="12">
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="form.phone" placeholder="请输入手机号" />
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-row :gutter="20">
          <el-col :xs="24" :sm="24" :md="12" :lg="12">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="form.email" placeholder="请输入邮箱" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="24" :md="12" :lg="12">
            <el-form-item label="宿舍信息" prop="dormitory">
              <el-input v-model="form.dormitory" placeholder="如：南苑 3 栋 502" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :xs="24" :sm="24" :md="12" :lg="12">
            <el-form-item label="状态" prop="status">
              <el-select v-model="form.status" placeholder="请选择状态" style="width: 100%">
                <el-option label="在读" value="在读" />
                <el-option label="毕业" value="毕业" />
                <el-option label="休学" value="休学" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" />
        </el-form-item>
        
        <el-form-item>
          <el-button type="primary" @click="handleSubmit" :loading="loading">保存</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
    <MajorManageDialog v-model:visible="showMajorManageDialog" @changed="loadMajors" />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createStudent } from '@/api/student'
import { getMajors } from '@/api/major'
import MajorManageDialog from '@/components/MajorManageDialog.vue'

const router = useRouter()
const formRef = ref()
const loading = ref(false)
const majorOptions = ref([])
const showMajorManageDialog = ref(false)

const form = reactive({
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
  password: ''
})

const rules = {
  studentId: [{ required: true, message: '请输入学号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  gender: [{ required: true, message: '请选择性别', trigger: 'change' }],
  major: [{ required: true, message: '请选择专业', trigger: 'change' }],
  grade: [{ required: true, message: '请输入年级', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const loadMajors = async () => {
  try {
    const res = await getMajors()
    if (res.code === 200) {
      majorOptions.value = res.data || []
    } else {
      ElMessage.error(res.msg || '加载专业列表失败')
    }
  } catch (error) {
    ElMessage.error('加载专业列表失败')
  }
}

const handleSubmit = async () => {
  try {
    await formRef.value.validate()
    loading.value = true
    
    const res = await createStudent(form)
    if (res.code === 200) {
      ElMessage.success('添加成功')
      router.push('/admin/students/list')
    } else {
      ElMessage.error(res.msg || '添加失败')
    }
  } catch (error) {
    ElMessage.error('表单验证失败')
  } finally {
    loading.value = false
  }
}

const handleReset = () => {
  formRef.value?.resetFields()
}

onMounted(loadMajors)
</script>

<style scoped>
.students-add {
  padding: 20px;
}

.card-header {
  font-weight: 500;
  color: #303133;
}

.major-field {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .students-add {
    padding: 10px;
  }

  :deep(.el-form-item__label) {
    font-size: 13px;
  }

  :deep(.el-input),
  :deep(.el-select) {
    font-size: 14px;
  }

  :deep(.el-dialog) {
    width: 95vw !important;
  }
}

@media (max-width: 480px) {
  .students-add {
    padding: 8px;
  }

  :deep(.el-form-item__label) {
    font-size: 12px;
  }

  :deep(.el-dialog) {
    width: 98vw !important;
  }
}
</style> 
