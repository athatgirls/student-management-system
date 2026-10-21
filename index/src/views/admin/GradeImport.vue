<template>
  <div class="grade-import">
    <el-card class="page-card" shadow="never">
      <div class="page-header">
        <div>
          <h2>成绩导入</h2>
          <p>教秘/管理员上传课程成绩表，系统按学号归档到学生个人学习记录。</p>
        </div>
        <el-button type="primary" plain :icon="Download" :loading="templateLoading" @click="downloadTemplate">
          下载导入模板
        </el-button>
      </div>
    </el-card>

    <el-row :gutter="20" class="summary-row">
      <el-col :xs="24" :sm="8">
        <el-card shadow="hover">
          <div class="summary-item">
            <span>待导入记录</span>
            <strong>{{ parsedRows.length }}</strong>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="8">
        <el-card shadow="hover">
          <div class="summary-item success">
            <span>校验通过</span>
            <strong>{{ validCount }}</strong>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="8">
        <el-card shadow="hover">
          <div class="summary-item danger">
            <span>需要修正</span>
            <strong>{{ invalidCount }}</strong>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="import-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <span>上传成绩表</span>
          <el-tag type="info" effect="plain">支持 .xlsx</el-tag>
        </div>
      </template>

      <el-alert
        title="表头建议：学号、姓名、学期、课程名称、成绩、学分、课程类别（学位课/非学位课）、状态。课程类别必填。"
        type="info"
        :closable="false"
        show-icon
        class="tips"
      />

      <el-upload
        ref="uploadRef"
        drag
        action="#"
        :auto-upload="false"
        :limit="1"
        accept=".xlsx"
        :on-change="handleFileChange"
        :on-remove="handleFileRemove"
      >
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text">拖拽成绩 Excel 到这里，或 <em>点击选择文件</em></div>
        <template #tip>
          <div class="el-upload__tip">建议单次导入一个年级或一个学期，便于教秘核对。</div>
        </template>
      </el-upload>

      <div class="action-bar">
        <el-button @click="clearImport">清空</el-button>
        <el-button type="primary" :disabled="!canSubmit" :loading="submitLoading" @click="submitImport">
          确认导入
        </el-button>
      </div>
    </el-card>

    <el-card class="table-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <span>导入预览</span>
          <el-tag v-if="selectedFile" type="success" effect="plain">{{ selectedFile.name }}</el-tag>
        </div>
      </template>

      <el-table :data="parsedRows" stripe style="width: 100%" max-height="520">
        <el-table-column prop="rowIndex" label="行号" width="80" />
        <el-table-column prop="studentId" label="学号" width="130" />
        <el-table-column prop="studentName" label="姓名" width="100" />
        <el-table-column prop="semester" label="学期" width="130" />
        <el-table-column prop="courseName" label="课程名称" min-width="180" />
        <el-table-column prop="score" label="成绩" width="90" />
        <el-table-column prop="credit" label="学分" width="90" />
        <el-table-column prop="courseType" label="课程类别" width="110"><template #default="{ row }">{{ courseTypeLabels[row.courseType] || row.courseType || '未分类' }}</template></el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.status === '已通过' ? 'success' : 'warning'">
              {{ scope.row.status || '-' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="校验" min-width="180">
          <template #default="scope">
            <el-tag v-if="scope.row.valid" type="success" effect="plain">通过</el-tag>
            <el-tag v-else type="danger" effect="plain">{{ scope.row.error }}</el-tag>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="parsedRows.length === 0" description="请先上传成绩表" />
    </el-card>
    <el-card class="table-card" shadow="hover">
      <template #header>历史课程类别维护</template>
      <div class="action-bar">
        <el-input v-model="recordStudentId" placeholder="输入学号查询教务课程" style="max-width: 240px" clearable @keyup.enter="loadRecords" />
        <el-button type="primary" :loading="recordsLoading" @click="loadRecords">查询</el-button>
      </div>
      <el-table :data="existingRecords" stripe style="width: 100%" max-height="400">
        <el-table-column prop="semester" label="学期" width="150" />
        <el-table-column prop="course" label="课程名称" min-width="180" />
        <el-table-column prop="score" label="成绩" width="85" />
        <el-table-column prop="credit" label="学分" width="85" />
        <el-table-column label="课程类别" width="190"><template #default="{ row }">
          <el-select :model-value="row.courseType || ''" placeholder="未分类" @change="value => assignCourseType(row, value)">
            <el-option label="学位课" value="degree" /><el-option label="非学位课" value="non_degree" />
          </el-select>
        </template></el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Download, UploadFilled } from '@element-plus/icons-vue'
import { importGrades, getGradeTemplate, getGradeRecords, setGradeCourseType } from '@/api/grade'
import { readExcelObjects } from '@/utils/excel'

const uploadRef = ref()
const selectedFile = ref(null)
const parsedRows = ref([])
const submitLoading = ref(false)
const templateLoading = ref(false)
const recordStudentId = ref('')
const existingRecords = ref([])
const recordsLoading = ref(false)
const courseTypeLabels = { degree: '学位课', non_degree: '非学位课' }
const parseCourseType = value => ({ 学位课: 'degree', 非学位课: 'non_degree', degree: 'degree', non_degree: 'non_degree' })[String(value || '').trim()] || ''

const requiredFields = ['studentId', 'semester', 'courseName', 'score', 'courseType']
const validCount = computed(() => parsedRows.value.filter(row => row.valid).length)
const invalidCount = computed(() => parsedRows.value.filter(row => !row.valid).length)
const canSubmit = computed(() => selectedFile.value && parsedRows.value.length > 0 && invalidCount.value === 0)

const headerMap = {
  studentId: ['学号', 'studentId', '学生学号'],
  studentName: ['姓名', 'studentName', '学生姓名'],
  semester: ['学期', 'semester'],
  courseName: ['课程名称', '课程', 'courseName', 'course'],
  score: ['成绩', '分数', 'score'],
  credit: ['学分', 'credit'],
  courseType: ['课程类别', '课程类型', 'courseType'],
  status: ['状态', 'status']
}

const pickValue = (row, keys) => {
  const key = keys.find(item => row[item] !== undefined && row[item] !== null && row[item] !== '')
  return key ? row[key] : ''
}

const inferStatus = (score, status) => {
  if (status) return String(status)
  const numericScore = Number(score)
  if (Number.isNaN(numericScore)) return ''
  return numericScore >= 60 ? '已通过' : '未通过'
}

const normalizeRow = (row, index) => {
  const normalized = {
    rowIndex: index + 2,
    studentId: String(pickValue(row, headerMap.studentId)).trim(),
    studentName: String(pickValue(row, headerMap.studentName)).trim(),
    semester: String(pickValue(row, headerMap.semester)).trim(),
    courseName: String(pickValue(row, headerMap.courseName)).trim(),
    score: pickValue(row, headerMap.score),
    credit: pickValue(row, headerMap.credit),
    courseType: parseCourseType(pickValue(row, headerMap.courseType)),
    status: ''
  }

  normalized.status = inferStatus(normalized.score, pickValue(row, headerMap.status))
  const missing = requiredFields.filter(field => normalized[field] === '' || normalized[field] === undefined)
  const credit = Number(normalized.credit)
  const invalidCredit = normalized.credit === '' || !Number.isFinite(credit) || credit <= 0
  normalized.valid = missing.length === 0 && !invalidCredit
  normalized.error = missing.length ? `缺少：${missing.map(fieldNameMap).join('、')}` : invalidCredit ? '学分必须是大于 0 的有效数字' : ''
  return normalized
}

const fieldNameMap = (field) => ({
  studentId: '学号',
  semester: '学期',
  courseName: '课程名称',
  score: '成绩',
  courseType: '课程类别'
}[field] || field)

const handleFileChange = (file) => {
  selectedFile.value = file.raw
  parseExcel(file.raw)
}

const handleFileRemove = () => {
  selectedFile.value = null
  parsedRows.value = []
}

const parseExcel = async (file) => {
  try {
      const rows = await readExcelObjects(file)
      parsedRows.value = rows.map(normalizeRow).filter(row => row.studentId || row.studentName || row.courseName)
      if (parsedRows.value.length === 0) {
        ElMessage.warning('没有解析到有效成绩数据')
      }
  } catch (error) {
    console.error('解析成绩表失败', error)
    ElMessage.error(error.message || 'Excel 解析失败，请检查文件格式')
  }
}

const clearImport = () => {
  selectedFile.value = null
  parsedRows.value = []
  uploadRef.value?.clearFiles()
}

const submitImport = async () => {
  if (!canSubmit.value) {
    ElMessage.warning('请先上传并修正成绩表')
    return
  }

  const records = parsedRows.value.map(row => ({
    studentId: row.studentId,
    studentName: row.studentName,
    semester: row.semester,
    courseName: row.courseName,
    score: row.score,
    credit: row.credit,
    courseType: row.courseType,
    status: row.status
  }))
  const formData = new FormData()
  formData.append('file', selectedFile.value)
  formData.append('records', JSON.stringify(records))

  submitLoading.value = true
  try {
    const response = await importGrades(formData)
    if (response.code === 200) {
      localStorage.setItem('preview_grade_records', JSON.stringify(records))
      ElMessage.success(response.msg || `成功导入 ${records.length} 条成绩`)
      clearImport()
    } else {
      ElMessage.error(response.msg || '导入失败')
    }
  } catch (error) {
    ElMessage.error('导入失败，请确认后端成绩导入接口是否已开启')
  } finally {
    submitLoading.value = false
  }
}

const downloadTemplate = async () => {
  templateLoading.value = true
  try {
    const blob = await getGradeTemplate()
    if (!(blob instanceof Blob) || blob.type.includes('json')) throw new Error('模板下载失败')
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = '成绩导入模板.xlsx'
    link.click()
    setTimeout(() => URL.revokeObjectURL(url), 1000)
  } catch (error) {
    ElMessage.error(error.message || '模板下载失败，请重试')
  } finally {
    templateLoading.value = false
  }
}

const loadRecords = async () => {
  const studentId = recordStudentId.value.trim()
  if (!studentId) { ElMessage.warning('请先输入学号'); return }
  recordsLoading.value = true
  try {
    const res = await getGradeRecords({ studentId })
    if (res.code !== 200) throw new Error(res.msg)
    existingRecords.value = res.data || []
  } catch (e) { ElMessage.error(e.message || '查询课程失败') }
  finally { recordsLoading.value = false }
}
const assignCourseType = async (row, value) => {
  try {
    const res = await setGradeCourseType(row.id, value)
    if (res.code !== 200) throw new Error(res.msg)
    row.courseType = value
    ElMessage.success('课程类别已更新')
  } catch (e) { ElMessage.error(e.message || '更新失败') }
}
</script>

<style scoped>
.grade-import {
  padding: 20px;
}

.page-card,
.import-card,
.table-card {
  margin-bottom: 20px;
}

.page-header,
.card-header,
.action-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.page-header h2 {
  margin: 0 0 8px;
  color: #303133;
}

.page-header p {
  margin: 0;
  color: #606266;
}

.summary-row {
  margin-bottom: 20px;
}

.summary-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.summary-item span {
  color: #606266;
}

.summary-item strong {
  color: #303133;
  font-size: 32px;
}

.summary-item.success strong {
  color: #67c23a;
}

.summary-item.danger strong {
  color: #f56c6c;
}

.tips {
  margin-bottom: 18px;
}

.action-bar {
  justify-content: flex-end;
  margin-top: 18px;
}

@media (max-width: 768px) {
  .grade-import {
    padding: 10px;
  }

  .page-header,
  .card-header {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
