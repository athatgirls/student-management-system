<template>
  <div class="student-overview" v-loading="loading">
    <el-card class="overview-toolbar">
      <template #header><div class="toolbar"><strong>{{ isAdmin ? '学生全量信息' : '状况一览表' }}</strong><el-button @click="load">刷新</el-button></div></template>
      <div v-if="isAdmin" class="toolbar search-toolbar">
        <el-select v-model="grade" placeholder="全部年级" clearable filterable aria-label="筛选年级">
          <el-option v-for="item in grades" :key="item" :label="item" :value="item" />
        </el-select>
        <el-input v-model="studentIdKeyword" placeholder="搜索学号" clearable @keyup.enter="search" />
        <el-input v-model="keyword" placeholder="搜索姓名" clearable @keyup.enter="search" />
        <el-button type="primary" @click="search">查询</el-button>
      </div>
      <div v-else class="toolbar"><span>左右滑动查看完整信息</span></div>
    </el-card>
    <div class="overview-scroll" role="region" aria-label="状况一览表，可横向滚动" tabindex="0">
      <table class="overview-grid">
        <colgroup>
          <col class="number-column" />
          <col v-for="field in studentFields" :key="field.key" :style="{ width: field.width + 'px' }" />
          <template v-for="group in groups" :key="group.key">
            <col v-for="summary in group.summaries" :key="summary" :class="summary === '加分项明细' ? 'bonus-column' : 'score-column'" />
            <template v-for="section in group.sections" :key="section.key">
              <col v-if="section.key === 'grades'" class="grade-column" />
              <template v-for="slot in section.slots" :key="slot">
                <col v-for="field in section.columns" :key="field.key" :class="field.key === 'itemScore' ? 'score-column' : 'record-column'" />
              </template>
            </template>
          </template>
        </colgroup>
        <thead>
          <tr>
            <th rowspan="3" scope="col">序号</th>
            <th v-for="field in studentFields" :key="field.key" rowspan="3" scope="col">{{ field.label }}</th>
            <th v-for="group in groups" :key="group.key" :colspan="group.width" scope="colgroup" class="group-heading">{{ group.label }}</th>
          </tr>
          <tr>
            <template v-for="group in groups" :key="group.key">
              <th v-for="summary in group.summaries" :key="summary" rowspan="2" scope="col">{{ summary }}</th>
              <template v-for="section in group.sections" :key="section.key">
                <th v-if="section.key === 'grades'" rowspan="2" scope="col">学习成绩</th>
                <th v-for="slot in section.slots" v-else :key="slot" :colspan="section.columns.length" scope="colgroup">{{ section.label }}{{ slotLabel(slot) }}</th>
              </template>
            </template>
          </tr>
          <tr>
            <template v-for="group in groups" :key="group.key">
              <template v-for="section in group.sections" :key="section.key">
                <template v-for="slot in section.slots" :key="slot">
                  <th v-for="field in section.columns" :key="field.key" scope="col">{{ field.label }}</th>
                </template>
              </template>
            </template>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(student, index) in students" :key="student.id">
            <td>{{ isAdmin ? (page - 1) * pageSize + index + 1 : index + 1 }}</td>
            <td v-for="field in studentFields" :key="field.key">{{ studentField(student, field.key) }}</td>
            <template v-for="group in groups" :key="group.key">
              <template v-if="group.key === 'D'">
                <td class="empty-value">—</td>
                <td class="bonus-cell">
                  <div v-for="(item, itemIndex) in getBonusDetails(student)" :key="item.record.id || itemIndex" class="bonus-entry">
                    <span>{{ item.category }}：</span><el-button link type="primary" class="record-link" @click="showDetail({ label: item.category }, item.record)">{{ item.text }}</el-button>
                    <template v-if="item.category === '任职' && item.record.source !== 'profile' && !isAdmin">
                      <el-button link type="primary" @click="editAppointment(item.record)">编辑</el-button>
                      <el-button link type="danger" @click="removeAppointment(item.record)">删除</el-button>
                    </template>
                  </div>
                  <span v-if="!getBonusDetails(student).length" class="empty-value">—</span>
                </td>
                <td class="empty-value">—</td>
              </template>
              <template v-else>
                <td v-for="summary in group.summaries" :key="summary" class="empty-value">—</td>
                <template v-for="section in group.sections" :key="section.key">
                  <td v-if="section.key === 'grades'" class="grade-cell">
                    <template v-if="calculateStudyGrade(student.grades)">
                      <strong>{{ calculateStudyGrade(student.grades).score.toFixed(2) }}</strong>
                      <div class="grade-explanation">X {{ calculateStudyGrade(student.grades).x.toFixed(2) }} × 70% + Y {{ calculateStudyGrade(student.grades).y.toFixed(2) }} × 30%</div>
                    </template>
                    <span v-else class="empty-value">待完善</span>
                    <div><el-button link type="primary" @click="showGrades(student)">查看全部课程（{{ (student.grades || []).length }}）</el-button></div>
                  </td>
                  <template v-for="slot in section.slots" v-else :key="slot">
                    <td v-for="field in section.columns" :key="field.key">
                      <template v-if="getOverviewRecords(student, section.key)[slot - 1]">
                        <el-button v-if="field.key === titleField(section.key)" link type="primary" class="record-link" @click="showDetail(section, getOverviewRecords(student, section.key)[slot - 1])">
                          {{ displayValue(field.key, overviewValue(getOverviewRecords(student, section.key)[slot - 1], field.key)) }}
                        </el-button>
                        <template v-else>{{ displayValue(field.key, overviewValue(getOverviewRecords(student, section.key)[slot - 1], field.key)) }}</template>
                      </template>
                      <span v-else class="empty-value">—</span>
                    </td>
                  </template>
                </template>
              </template>
            </template>
          </tr>
          <tr v-if="!loading && !students.length"><td class="empty-row" :colspan="totalColumns">暂无学生信息</td></tr>
        </tbody>
      </table>
    </div>
    <el-pagination v-if="isAdmin" v-model:current-page="page" v-model:page-size="pageSize" :page-sizes="[10, 20, 50, 100]" :total="total" layout="total, sizes, prev, pager, next" @current-change="load" @size-change="changePageSize" />
    <StudentGradeDetails :student="gradeStudent" @close="gradeStudent = null" />
    <el-dialog :model-value="!!detail" :title="detailTitle" width="min(900px, 95vw)" @close="detail = null">
      <el-descriptions v-if="detail" :column="1" border>
        <el-descriptions-item v-for="[key, value] in detailEntries" :key="key" :label="fieldLabels[key] || key">{{ displayValue(key, value) }}</el-descriptions-item>
        <el-descriptions-item v-for="(value, key) in detail.submissionFields || {}" :key="'submission-' + key" :label="key">
          <PrivateAttachments v-if="attachmentValue(value)" :attachments="attachmentValue(value)" />
          <template v-else>{{ displayValue(key, value) }}</template>
        </el-descriptions-item>
        <el-descriptions-item v-if="detail.attachments" label="证明材料"><PrivateAttachments :attachments="detail.attachments" /></el-descriptions-item>
        <el-descriptions-item v-if="detail.evidenceUrl" label="证明材料"><PrivateAttachments :attachments="detail.evidenceUrl" /></el-descriptions-item>
        <el-descriptions-item v-if="detail.attachmentUrl" label="附件材料"><PrivateAttachments :attachments="detail.attachmentUrl" /></el-descriptions-item>
      </el-descriptions>
    </el-dialog>
    <el-dialog v-model="appointmentDialog" :title="appointment.id ? '编辑任职记录' : '新增任职记录'" width="min(600px, 95vw)" :close-on-click-modal="false">
      <el-form label-width="90px">
        <el-form-item label="职务名称" required><el-input v-model="appointment.position" maxlength="120" /></el-form-item>
        <el-form-item label="任职单位" required><el-input v-model="appointment.organization" maxlength="200" /></el-form-item>
        <el-form-item label="开始日期" required><el-date-picker v-model="appointment.startDate" value-format="YYYY-MM-DD" /></el-form-item>
        <el-form-item label="结束日期"><el-date-picker v-model="appointment.endDate" value-format="YYYY-MM-DD" placeholder="留空表示仍在任职" /></el-form-item>
        <el-form-item label="职责说明"><el-input v-model="appointment.description" type="textarea" :rows="4" maxlength="4000" /></el-form-item>
        <el-form-item label="证明材料"><RecordAttachments v-model="appointment.attachments" editable @uploading="uploading = $event" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="appointmentDialog = false">取消</el-button><el-button type="primary" :disabled="uploading" :loading="saving" @click="saveAppointment">保存</el-button></template>
    </el-dialog>
  </div>
</template>
<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/api/request'
import { getAllGrades } from '@/api/student'
import RecordAttachments from '@/components/RecordAttachments.vue'
import PrivateAttachments from '@/components/PrivateAttachments.vue'
import StudentGradeDetails from '@/components/StudentGradeDetails.vue'
import { fieldLabels, displayValue } from '@/utils/overviewFields'
import { buildOverviewGroups, getOverviewRecords, getBonusDetails, overviewValue } from '@/utils/overviewLayout'
import { calculateStudyGrade } from '@/utils/studyGradeScore'
const route = useRoute()
const isAdmin = computed(() => route.path.startsWith('/admin'))
const studentFields = [
  { key: 'studentId', label: '学号', width: 130 },
  { key: 'name', label: '姓名', width: 100 },
  { key: 'laboratory', label: '实验室', width: 130 },
  { key: 'supervisorTeam', label: '导师及所属团队', width: 180 }
]
const students = ref([])
const groups = computed(() => buildOverviewGroups(students.value))
const totalColumns = computed(() => studentFields.length + 1 + groups.value.reduce((sum, group) => sum + group.width, 0))
const studentField = (student, key) => key === 'laboratory' ? '—' : key === 'supervisorTeam' ? `${student.supervisor || '—'} / —` : student[key] || '—'
const titleFields = { papers: 'paperTitle', patents: 'patentTitle', projects: 'projectName', competitions: 'competitionName' }
const titleField = key => titleFields[key]
const numerals = ['一', '二', '三', '四', '五', '六', '七', '八', '九', '十']
const slotLabel = number => numerals[number - 1] || String(number)
const loading = ref(false)
const keyword = ref('')
const grade = ref('')
const grades = ref([])
const studentIdKeyword = ref('')
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const detail = ref(null)
const detailTitle = ref('详情')
const gradeStudent = ref(null)
const showGrades = student => { gradeStudent.value = student }
const detailEntries = computed(() => Object.entries(detail.value || {}).filter(([key]) => !['id', 'studentAccountId', 'auditorId', 'attachments', 'evidenceUrl', 'attachmentUrl', 'submissionFields', 'overviewType'].includes(key) && !(key === 'content' && detail.value?.submissionFields)))
const attachmentValue = value => {
  try {
    const files = typeof value === 'string' ? JSON.parse(value) : value
    return Array.isArray(files) && files.length && files.every(file => file && typeof file.url === 'string') ? JSON.stringify(files) : ''
  } catch { return '' }
}
const load = async () => {
  loading.value = true
  try {
    const res = await request.get('/student-overview', { params: isAdmin.value ? { keyword: keyword.value.trim(), grade: grade.value, studentIdKeyword: studentIdKeyword.value.trim(), page: page.value, size: pageSize.value } : {} })
    if (res.code !== 200) throw new Error(res.msg)
    students.value = res.data.students; total.value = res.data.total
  } catch (e) { ElMessage.error(e.message || '加载一览表失败') }
  finally { loading.value = false }
}
const showDetail = (section, row) => { detail.value = row; detailTitle.value = section.label + '详情' }
const search = () => { page.value = 1; load() }
const changePageSize = () => { page.value = 1; load() }
const loadGrades = async () => {
  try {
    const res = await getAllGrades()
    if (res.code !== 200) throw new Error(res.msg)
    grades.value = res.data || []
  } catch (e) { ElMessage.error(e.message || '加载年级失败') }
}
const appointmentDialog = ref(false)
const appointment = ref({})
const saving = ref(false)
const uploading = ref(false)
const editAppointment = row => { appointment.value = { ...(row || { position: '', organization: '', startDate: '', endDate: null, description: '', attachments: '' }) }; appointmentDialog.value = true }
const saveAppointment = async () => {
  const form = appointment.value
  if (!form.position?.trim() || !form.organization?.trim() || !form.startDate) return ElMessage.warning('请填写职务名称、任职单位和开始日期')
  if (form.endDate && form.endDate < form.startDate) return ElMessage.warning('结束日期不能早于开始日期')
  saving.value = true
  try {
    const res = await request.post(form.id ? `/appointments/${form.id}/update` : '/appointments/create', { ...form, endDate: form.endDate || null })
    if (res.code !== 200) throw new Error(res.msg)
    appointmentDialog.value = false; ElMessage.success('任职记录已保存'); await load()
  } catch (e) { ElMessage.error(e.message || '保存失败') }
  finally { saving.value = false }
}
const removeAppointment = async row => {
  try {
    await ElMessageBox.confirm('确定删除该任职记录吗？', '删除任职记录', { type: 'warning' })
    const res = await request.post(`/appointments/${row.id}/delete`)
    if (res.code !== 200) throw new Error(res.msg)
    ElMessage.success('删除成功'); await load()
  } catch (e) { if (e !== 'cancel' && e !== 'close') ElMessage.error(e.message || '删除失败') }
}
onMounted(() => { load(); if (isAdmin.value) loadGrades() })
</script>
<style scoped>
.student-overview { padding: 20px; min-width: 0; }
.overview-toolbar { margin-bottom: 16px; }
.toolbar { display: flex; gap: 12px; align-items: center; justify-content: space-between; flex-wrap: wrap; }
.search-toolbar { justify-content: flex-start; }
.toolbar .el-input { width: 200px; }
.toolbar .el-select { width: 160px; }
.overview-scroll { max-width: 100%; overflow-x: auto; border: 1px solid #dcdfe6; background: #fff; }
.overview-scroll:focus-visible { outline: 2px solid #409eff; outline-offset: 2px; }
.overview-grid { table-layout: fixed; border-collapse: collapse; width: max-content; font-size: 13px; color: #303133; }
.number-column { width: 60px; }
.score-column { width: 90px; }
.bonus-column { width: 340px; }
.grade-column { width: 240px; }
.record-column { width: 160px; }
.overview-grid th, .overview-grid td { padding: 10px 8px; border-right: 1px solid #dcdfe6; border-bottom: 1px solid #dcdfe6; text-align: center; vertical-align: middle; overflow-wrap: anywhere; }
.overview-grid th { background: #f0f4fa; font-weight: 600; white-space: nowrap; }
.overview-grid th.group-heading { background: #e7eef9; font-size: 15px; }
.overview-grid thead tr:nth-child(2) th { background: #f7f9fc; }
.overview-grid tbody tr:nth-child(even) { background: #fafafa; }
.overview-grid tbody tr:hover { background: #f5f9ff; }
.record-link { height: auto; line-height: 1.5; white-space: normal; overflow-wrap: anywhere; text-align: center; }
.grade-explanation { margin-top: 5px; color: #909399; font-size: 12px; }
.bonus-cell { text-align: left !important; }
.bonus-entry + .bonus-entry { border-top: 1px dashed #dcdfe6; padding-top: 5px; margin-top: 5px; }
.bonus-entry :deep(.el-button + .el-button) { margin-left: 6px; }
.empty-value { color: #909399; }
.empty-row { padding: 30px !important; color: #909399; }
.el-pagination { margin: 16px 0; }
:deep(.el-descriptions__content) { white-space: pre-wrap; overflow-wrap: anywhere; }
@media (max-width: 768px) { .student-overview { padding: 10px; } }
</style>
