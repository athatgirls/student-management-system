<template>
  <div class="grade-management">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <div>
            <h2>成绩管理</h2>
            <p>查询并维护学生课程成绩。</p>
          </div>
          <el-button type="primary" @click="router.push('/admin/students/grades/import')">成绩导入</el-button>
        </div>
      </template>

      <div class="filters">
        <el-select v-model="filters.grade" placeholder="选择年级" clearable filterable>
          <el-option v-for="item in grades" :key="item" :label="item" :value="item" />
        </el-select>
        <el-input v-model="filters.studentId" placeholder="按学号查询" clearable @keyup.enter="search" />
        <el-input v-model="filters.course" placeholder="按课程名称查询" clearable @keyup.enter="search" />
        <el-input v-model="filters.semester" placeholder="按学期查询" clearable @keyup.enter="search" />
        <el-button type="primary" :loading="loading" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>

      <el-table v-loading="loading" :data="records" stripe style="width: 100%" empty-text="暂无成绩记录">
        <el-table-column prop="studentId" label="学号" min-width="130" />
        <el-table-column prop="semester" label="学期" min-width="150" />
        <el-table-column prop="course" label="课程名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="score" label="成绩" width="85" />
        <el-table-column prop="credit" label="学分" width="85" />
        <el-table-column label="课程类别" width="110">
          <template #default="{ row }">{{ courseTypeLabels[row.courseType] || '未分类' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">{{ statusLabels[row.status] || row.status || '-' }}</template>
        </el-table-column>
        <el-table-column label="来源" width="110">
          <template #default="{ row }">{{ row.source === 'student' ? '学生填写' : '教务导入' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        class="pagination"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next"
        :total="total"
        @current-change="loadRecords"
        @size-change="changeSize"
      />
    </el-card>

    <el-dialog v-model="editVisible" title="编辑成绩" width="min(520px, 95vw)" :close-on-click-modal="!saving">
      <el-form :model="editForm" label-width="90px">
        <el-form-item label="学号"><el-input :model-value="editForm.studentId" disabled /></el-form-item>
        <el-form-item label="学期" required><el-input v-model="editForm.semester" maxlength="100" /></el-form-item>
        <el-form-item label="课程名称" required><el-input v-model="editForm.course" maxlength="200" /></el-form-item>
        <el-form-item label="成绩" required>
          <el-input-number v-model="editForm.score" :min="0" :max="100" :precision="0" />
        </el-form-item>
        <el-form-item label="学分" required>
          <el-input-number v-model="editForm.credit" :min="0.001" :step="0.5" />
        </el-form-item>
        <el-form-item label="课程类别" required>
          <el-select v-model="editForm.courseType" placeholder="请选择课程类别">
            <el-option label="学位课" value="degree" />
            <el-option label="非学位课" value="non_degree" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button :disabled="saving" @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getManagedGradeRecords, updateManagedGradeRecord } from '@/api/grade'
import { getAllGrades } from '@/api/student'

const router = useRouter()
const filters = reactive({ grade: '', studentId: '', course: '', semester: '' })
const appliedFilters = reactive({ grade: '', studentId: '', course: '', semester: '' })
const grades = ref([])
const records = ref([])
const loading = ref(false)
const saving = ref(false)
const editVisible = ref(false)
const editForm = reactive({ id: '', studentId: '', semester: '', course: '', score: null, credit: null, courseType: '' })
const page = ref(1)
const size = ref(20)
const total = ref(0)
const courseTypeLabels = { degree: '学位课', non_degree: '非学位课' }
const statusLabels = { passed: '已通过', failed: '未通过', not_entered: '未录入' }

const loadRecords = async () => {
  loading.value = true
  try {
    const response = await getManagedGradeRecords({
      studentId: appliedFilters.studentId,
      semester: appliedFilters.semester,
      grade: appliedFilters.grade,
      course: appliedFilters.course,
      page: page.value,
      size: size.value
    })
    if (response.code !== 200) throw new Error(response.msg || '加载成绩失败')
    records.value = response.data.records || []
    total.value = response.data.total || 0
  } catch (error) {
    records.value = []
    total.value = 0
    ElMessage.error(error.message || '加载成绩失败')
  } finally {
    loading.value = false
  }
}

const search = () => {
  appliedFilters.grade = filters.grade
  appliedFilters.studentId = filters.studentId.trim()
  appliedFilters.course = filters.course.trim()
  appliedFilters.semester = filters.semester.trim()
  page.value = 1
  loadRecords()
}

const reset = () => {
  filters.grade = ''
  filters.studentId = ''
  filters.course = ''
  filters.semester = ''
  search()
}

const changeSize = () => {
  page.value = 1
  loadRecords()
}

const openEdit = row => {
  Object.assign(editForm, {
    id: row.id, studentId: row.studentId, semester: row.semester || '', course: row.course || '',
    score: row.score, credit: row.credit, courseType: row.courseType || ''
  })
  editVisible.value = true
}

const saveEdit = async () => {
  if (!editForm.semester.trim() || !editForm.course.trim() ||
      !Number.isInteger(editForm.score) || editForm.score < 0 || editForm.score > 100 ||
      !Number.isFinite(editForm.credit) || editForm.credit <= 0 || !editForm.courseType) {
    ElMessage.warning('请填写有效的学期、课程、成绩、学分和课程类别')
    return
  }
  saving.value = true
  try {
    const response = await updateManagedGradeRecord(editForm.id, {
      semester: editForm.semester.trim(), course: editForm.course.trim(),
      score: editForm.score, credit: editForm.credit, courseType: editForm.courseType
    })
    if (response.code !== 200) throw new Error(response.msg || '保存失败')
    editVisible.value = false
    ElMessage.success('成绩已更新')
    await loadRecords()
  } catch (error) {
    ElMessage.error(error.message || '保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  loadRecords()
  try {
    const response = await getAllGrades()
    if (response.code === 200) grades.value = response.data || []
  } catch (error) {
    ElMessage.error(error.message || '年级列表加载失败')
  }
})
</script>

<style scoped>
.grade-management { padding: 20px; }
.card-header, .filters { display: flex; align-items: center; gap: 12px; }
.card-header { justify-content: space-between; }
.card-header h2 { margin: 0 0 8px; }
.card-header p { margin: 0; color: #606266; }
.filters { margin-bottom: 20px; flex-wrap: wrap; }
.filters .el-input { width: 220px; }
.filters .el-select { width: 160px; }
.pagination { justify-content: flex-end; margin-top: 20px; }
@media (max-width: 768px) {
  .grade-management { padding: 10px; }
  .card-header { align-items: flex-start; flex-direction: column; }
  .filters .el-input, .filters .el-select { width: 100%; }
  .pagination { justify-content: center; flex-wrap: wrap; }
}
</style>
