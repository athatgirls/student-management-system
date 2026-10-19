<template>
  <div class="student-overview" v-loading="loading">
    <el-card class="overview-toolbar">
      <template #header><div class="toolbar"><strong>{{ isAdmin ? '学生全量信息' : '学生状况一览表' }}</strong><el-button @click="load">刷新</el-button></div></template>
      <div v-if="isAdmin" class="toolbar">
        <el-input v-model="keyword" placeholder="搜索姓名或学号" clearable @keyup.enter="search" />
        <el-input v-model="grade" placeholder="年级" clearable @keyup.enter="search" />
        <el-input v-model="major" placeholder="专业" clearable @keyup.enter="search" />
        <el-button type="primary" @click="search">查询</el-button>
      </div>
      <div v-else class="toolbar"><span>汇总学习、科研、活动、荣誉与任职记录</span><el-button type="primary" @click="editAppointment()">新增任职记录</el-button></div>
    </el-card>
    <el-table v-if="isAdmin" :data="students" stripe @row-click="selectStudent">
      <el-table-column prop="studentId" label="学号" width="140" /><el-table-column prop="name" label="姓名" width="100" />
      <el-table-column prop="grade" label="年级" width="100" /><el-table-column prop="major" label="专业" min-width="140" />
      <el-table-column v-for="section in overviewSections" :key="section.key" :label="section.label" width="95"><template #default="{ row }">{{ (row[section.key] || []).length }}</template></el-table-column>
      <el-table-column label="操作" width="100" fixed="right"><template #default="{ row }"><el-button link type="primary" @click.stop="selectStudent(row)">查看全部</el-button></template></el-table-column>
    </el-table>
    <el-pagination v-if="isAdmin" v-model:current-page="page" :total="total" :page-size="10" layout="total, prev, pager, next" @current-change="load" />
    <el-empty v-if="!loading && !students.length" description="暂无学生信息" />
    <template v-if="selected">
      <el-card class="student-summary">
        <h2>{{ selected.name }} · 状况一览表</h2>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="学号">{{ selected.studentId }}</el-descriptions-item><el-descriptions-item label="年级">{{ selected.grade || '-' }}</el-descriptions-item>
          <el-descriptions-item label="专业">{{ selected.major || '-' }}</el-descriptions-item><el-descriptions-item label="班级">{{ selected.className || '-' }}</el-descriptions-item>
          <el-descriptions-item label="导师">{{ selected.supervisor || '-' }}</el-descriptions-item>
        </el-descriptions>
      </el-card>
      <el-card v-for="section in overviewSections" :key="section.key" class="overview-section">
        <template #header><strong>{{ section.label }}（{{ (selected[section.key] || []).length }}）</strong></template>
        <el-table :data="selected[section.key] || []" stripe empty-text="暂无记录">
          <el-table-column v-for="key in section.columns" :key="key" :label="fieldLabels[key]" min-width="140"><template #default="{ row }">{{ displayValue(key, row[key]) }}</template></el-table-column>
          <el-table-column label="操作" :width="section.key === 'appointments' && !isAdmin ? 180 : 90" fixed="right"><template #default="{ row }">
            <el-button link type="primary" @click="detail = row; detailTitle = section.label + '详情'">详情</el-button>
            <template v-if="section.key === 'appointments' && !isAdmin"><el-button link type="primary" @click="editAppointment(row)">编辑</el-button><el-button link type="danger" @click="removeAppointment(row)">删除</el-button></template>
          </template></el-table-column>
        </el-table>
      </el-card>
    </template>
    <el-dialog :model-value="!!detail" :title="detailTitle" width="min(900px, 95vw)" @close="detail = null">
      <el-descriptions v-if="detail" :column="1" border>
        <el-descriptions-item v-for="[key, value] in detailEntries" :key="key" :label="fieldLabels[key] || key">{{ displayValue(key, value) }}</el-descriptions-item>
        <el-descriptions-item v-for="(value, key) in detail.submissionFields || {}" :key="'submission-' + key" :label="key">
          <RecordAttachments v-if="attachmentValue(value)" :model-value="attachmentValue(value)" />
          <template v-else>{{ displayValue(key, value) }}</template>
        </el-descriptions-item>
        <el-descriptions-item v-if="detail.attachments" label="证明材料"><RecordAttachments :model-value="detail.attachments" /></el-descriptions-item>
        <el-descriptions-item v-if="detail.evidenceUrl" label="证明材料"><el-link :href="getFileUrl(detail.evidenceUrl)" target="_blank" rel="noopener">查看证明材料</el-link></el-descriptions-item>
        <el-descriptions-item v-if="detail.attachmentUrl" label="附件材料"><el-link :href="getFileUrl(detail.attachmentUrl)" target="_blank" rel="noopener">查看附件材料</el-link></el-descriptions-item>
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
import RecordAttachments from '@/components/RecordAttachments.vue'
import { getFileUrl } from '@/utils/imageUrl'
import { overviewSections, fieldLabels, displayValue } from '@/utils/overviewFields'
const route = useRoute()
const isAdmin = computed(() => route.path.startsWith('/admin'))
const students = ref([])
const selected = ref(null)
const loading = ref(false)
const keyword = ref('')
const grade = ref('')
const major = ref('')
const page = ref(1)
const total = ref(0)
const detail = ref(null)
const detailTitle = ref('详情')
const detailEntries = computed(() => Object.entries(detail.value || {}).filter(([key]) => !['id', 'studentDbId', 'userId', 'auditorId', 'attachments', 'evidenceUrl', 'attachmentUrl', 'submissionFields'].includes(key) && !(key === 'content' && detail.value?.submissionFields)))
const attachmentValue = value => {
  try {
    const files = typeof value === 'string' ? JSON.parse(value) : value
    return Array.isArray(files) && files.length && files.every(file => file && typeof file.url === 'string') ? JSON.stringify(files) : ''
  } catch { return '' }
}
const load = async () => {
  loading.value = true
  try {
    const res = await request.get('/student-overview', { params: isAdmin.value ? { keyword: keyword.value, grade: grade.value, major: major.value, page: page.value, size: 10 } : {} })
    if (res.code !== 200) throw new Error(res.msg)
    students.value = res.data.students; total.value = res.data.total
    selected.value = students.value.find(row => row.id === selected.value?.id) || (isAdmin.value ? null : students.value[0])
  } catch (e) { ElMessage.error(e.message || '加载一览表失败') }
  finally { loading.value = false }
}
const selectStudent = row => { selected.value = row }
const search = () => { page.value = 1; selected.value = null; load() }
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
onMounted(load)
</script>
<style scoped>
.student-overview { padding: 20px; }
.overview-toolbar, .student-summary, .overview-section { margin-bottom: 20px; }
.toolbar { display: flex; gap: 12px; align-items: center; justify-content: space-between; flex-wrap: wrap; }
.toolbar .el-input { width: 200px; }
.el-pagination { margin: 16px 0; }
:deep(.el-descriptions__content) { white-space: pre-wrap; overflow-wrap: anywhere; }
@media (max-width: 768px) { .student-overview { padding: 10px; } }
</style>
