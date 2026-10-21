<template>
  <el-dialog :model-value="!!student" title="课程成绩明细" width="min(1000px, 95vw)" @close="$emit('close')">
    <template v-if="student">
      <p>{{ student.name }}（{{ student.studentId }}），共 {{ records.length }} 门课程。未分类、未录入及未通过的课程也会列出。</p>
      <div class="grade-details-scroll" role="region" aria-label="全部课程成绩" tabindex="0">
        <table class="grade-details-table">
          <caption>{{ student.name }}的全部课程成绩</caption>
          <thead><tr><th v-for="key in columns" :key="key" scope="col">{{ fieldLabels[key] }}</th><th scope="col">证明材料</th></tr></thead>
          <tbody>
            <tr v-for="(record, index) in records" :key="record.id || index">
              <td v-for="key in columns" :key="key">{{ gradeValue(record, key) }}</td>
              <td><PrivateAttachments v-if="record.attachments" :attachments="record.attachments" /><span v-else>—</span></td>
            </tr>
            <tr v-if="!records.length"><td :colspan="columns.length + 1">暂无课程成绩</td></tr>
          </tbody>
        </table>
      </div>
    </template>
  </el-dialog>
</template>
<script setup>
import { computed, defineProps, defineEmits } from 'vue'
import PrivateAttachments from '@/components/PrivateAttachments.vue'
import { fieldLabels, displayValue } from '@/utils/overviewFields'

const props = defineProps({ student: { type: Object, default: null } })
defineEmits(['close'])
const records = computed(() => Array.isArray(props.student?.grades) ? props.student.grades : [])
const columns = ['semester', 'course', 'score', 'credit', 'courseType', 'status', 'source']
const gradeValue = (record, key) => {
  if (key === 'courseType' && !record[key]) return '未分类'
  if (key === 'source' && !record[key]) return '教务导入'
  return displayValue(key, record[key])
}
</script>
<style scoped>
.grade-details-scroll { max-height: 60vh; overflow: auto; }
.grade-details-scroll:focus-visible { outline: 2px solid #409eff; outline-offset: 2px; }
.grade-details-table { width: 100%; min-width: 760px; border-collapse: collapse; color: #303133; }
.grade-details-table caption { padding: 10px; font-weight: 600; text-align: left; }
.grade-details-table th, .grade-details-table td { padding: 10px; border: 1px solid #dcdfe6; text-align: left; overflow-wrap: anywhere; }
.grade-details-table th { background: #f0f4fa; }
.grade-details-table tbody tr:nth-child(even) { background: #fafafa; }
</style>
