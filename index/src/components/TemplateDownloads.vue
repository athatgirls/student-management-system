<template>
  <el-card v-loading="loading">
    <template #header><div class="template-header"><span>{{ management ? '模板管理' : '下载专区' }}</span><el-button @click="load">刷新</el-button></div></template>
    <el-form v-if="management" class="upload-form" label-width="90px" @submit.prevent="submit">
      <el-form-item label="模板名称" required><el-input v-model="title" maxlength="120" /></el-form-item>
      <el-form-item label="模板说明"><el-input v-model="description" type="textarea" maxlength="2000" /></el-form-item>
      <el-form-item label="模板文件" required>
        <input ref="fileInput" type="file" accept=".doc,.docx,.pdf,.xls,.xlsx" @change="file = $event.target.files[0]" />
        <small>支持 Word、PDF、Excel，单个文件不超过 10 MB</small>
      </el-form-item>
      <el-form-item><el-button type="primary" :loading="saving" @click="submit">上传模板</el-button></el-form-item>
    </el-form>
    <el-table :data="templates" stripe empty-text="暂无可下载的模板">
      <el-table-column prop="title" label="模板名称" min-width="160" />
      <el-table-column prop="description" label="说明" min-width="180" />
      <el-table-column prop="originalName" label="文件名" min-width="180" />
      <el-table-column label="大小" width="100"><template #default="{ row }">{{ (row.size / 1024).toFixed(1) }} KB</template></el-table-column>
      <el-table-column label="上传时间" width="170"><template #default="{ row }">{{ row.createTime ? new Date(row.createTime).toLocaleString('zh-CN') : '-' }}</template></el-table-column>
      <el-table-column label="操作" :width="management ? 160 : 100"><template #default="{ row }">
        <el-button v-if="!row.deleting" link type="primary" @click="download(row)">下载</el-button>
        <el-button v-if="management" link type="danger" @click="remove(row)">{{ row.deleting ? '重试删除' : '删除' }}</el-button>
      </template></el-table-column>
    </el-table>
  </el-card>
</template>
<script setup>
import { ref, onMounted, defineProps } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listTemplates, uploadTemplate, deleteTemplate, downloadTemplate } from '@/api/templates'
const props = defineProps({ management: Boolean })
const templates = ref([])
const loading = ref(false)
const saving = ref(false)
const title = ref('')
const description = ref('')
const file = ref(null)
const fileInput = ref(null)
const load = async () => {
  loading.value = true
  try { const res = await listTemplates(props.management); if (res.code !== 200) throw new Error(res.msg); templates.value = res.data || [] }
  catch (e) { ElMessage.error(e.message || '加载模板失败') }
  finally { loading.value = false }
}
const submit = async () => {
  if (!title.value.trim() || !file.value) return ElMessage.warning('请填写模板名称并选择文件')
  if (!/\.(docx?|pdf|xlsx?)$/i.test(file.value.name)) return ElMessage.warning('请选择 Word、PDF 或 Excel 文件')
  if (!file.value.size || file.value.size > 10 * 1024 * 1024) return ElMessage.warning('请选择不超过10MB的非空文件')
  saving.value = true
  try {
    const data = new FormData()
    data.append('title', title.value.trim()); data.append('description', description.value); data.append('file', file.value)
    const res = await uploadTemplate(data)
    if (res.code !== 200) throw new Error(res.msg)
    title.value = ''; description.value = ''; file.value = null; fileInput.value.value = ''
    ElMessage.success('模板上传成功'); await load()
  } catch (e) { ElMessage.error(e.message || '上传失败') }
  finally { saving.value = false }
}
const download = async row => { try { await downloadTemplate(row) } catch (e) { ElMessage.error('下载失败，请重试') } }
const remove = async row => {
  try {
    await ElMessageBox.confirm(`确定删除模板“${row.title}”吗？`, '删除模板', { type: 'warning' })
    const res = await deleteTemplate(row.id)
    if (res.code !== 200) throw new Error(res.msg)
    ElMessage.success('删除成功'); await load()
  } catch (e) { if (e !== 'cancel' && e !== 'close') { ElMessage.error(e.message || '删除失败'); await load() } }
}
onMounted(load)
</script>
<style scoped>
.template-header { display: flex; justify-content: space-between; align-items: center; }
.upload-form { max-width: 700px; margin-bottom: 24px; }
small { display: block; width: 100%; color: #909399; }
</style>
