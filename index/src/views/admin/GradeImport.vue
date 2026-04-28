<template>
  <div class="grade-import">
    <el-card class="page-card" shadow="never">
      <div class="page-header">
        <div>
          <h2>成绩导入</h2>
          <p>教秘/管理员上传课程成绩表，系统按学号归档到学生个人学习记录。</p>
        </div>
        <el-button type="primary" plain :icon="Download" @click="downloadTemplate">
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
          <el-tag type="info" effect="plain">支持 .xlsx / .xls</el-tag>
        </div>
      </template>

      <el-alert
        title="表头建议：学号、姓名、学期、课程名称、成绩、学分、状态。姓名可选，系统主要按学号匹配学生。"
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
        accept=".xlsx,.xls"
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
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Download, UploadFilled } from '@element-plus/icons-vue'
import * as XLSX from 'xlsx'
import { importGrades } from '@/api/grade'

const uploadRef = ref()
const selectedFile = ref(null)
const parsedRows = ref([])
const submitLoading = ref(false)

const requiredFields = ['studentId', 'semester', 'courseName', 'score']
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
    status: ''
  }

  normalized.status = inferStatus(normalized.score, pickValue(row, headerMap.status))
  const missing = requiredFields.filter(field => normalized[field] === '' || normalized[field] === undefined)
  normalized.valid = missing.length === 0
  normalized.error = missing.length ? `缺少：${missing.map(fieldNameMap).join('、')}` : ''
  return normalized
}

const fieldNameMap = (field) => ({
  studentId: '学号',
  semester: '学期',
  courseName: '课程名称',
  score: '成绩'
}[field] || field)

const handleFileChange = (file) => {
  selectedFile.value = file.raw
  parseExcel(file.raw)
}

const handleFileRemove = () => {
  selectedFile.value = null
  parsedRows.value = []
}

const parseExcel = (file) => {
  const reader = new FileReader()
  reader.onload = (event) => {
    try {
      const workbook = XLSX.read(event.target.result, { type: 'array' })
      const firstSheetName = workbook.SheetNames[0]
      const worksheet = workbook.Sheets[firstSheetName]
      const rows = XLSX.utils.sheet_to_json(worksheet, { defval: '' })
      parsedRows.value = rows.map(normalizeRow).filter(row => row.studentId || row.studentName || row.courseName)
      if (parsedRows.value.length === 0) {
        ElMessage.warning('没有解析到有效成绩数据')
      }
    } catch (error) {
      console.error('解析成绩表失败', error)
      ElMessage.error('Excel 解析失败，请检查文件格式')
    }
  }
  reader.readAsArrayBuffer(file)
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

const downloadTemplate = () => {
  const rows = [
    ['学号', '姓名', '学期', '课程名称', '成绩', '学分', '状态'],
    ['10240001', '张同学', '2025-2026-1', 'Web 前端开发', 92, 3, '已通过']
  ]
  const worksheet = XLSX.utils.aoa_to_sheet(rows)
  const workbook = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(workbook, worksheet, '成绩导入模板')
  XLSX.writeFile(workbook, '成绩导入模板.xlsx')
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
