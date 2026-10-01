<template>
  <el-card>
    <template #header><div class="intent-header"><span>学生就业意向</span><el-button @click="load">刷新</el-button></div></template>
    <el-input v-model="keyword" placeholder="搜索学号、姓名、城市或意向" clearable style="max-width:360px;margin-bottom:12px" />
    <el-table :data="filtered" v-loading="loading" empty-text="暂无学生提交的就业意向">
      <el-table-column prop="studentId" label="学号" /><el-table-column prop="studentName" label="姓名" />
      <el-table-column prop="intentionType" label="意向方向" /><el-table-column prop="targetCity" label="意向城市" />
      <el-table-column prop="targetIndustry" label="意向行业" /><el-table-column prop="targetPosition" label="意向岗位" />
      <el-table-column prop="expectedSalary" label="期望薪资" />
    </el-table>
  </el-card>
</template>
<script setup>
import { ref, computed, onMounted } from 'vue'
import request from '@/api/request'
const rows = ref([]), keyword = ref(''), loading = ref(false)
const filtered = computed(() => rows.value.filter(row => [row.studentId, row.studentName, row.targetCity, row.intentionType].join(' ').includes(keyword.value.trim())))
const load = async () => { loading.value = true; try { rows.value = await request.get('/employment-intentions/admin/list') } catch { /* displayed centrally */ } finally { loading.value = false } }
onMounted(load)
</script>
<style scoped>.intent-header{display:flex;justify-content:space-between;align-items:center}</style>
