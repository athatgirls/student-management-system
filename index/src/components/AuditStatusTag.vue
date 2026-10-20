<template>
  <el-tag :type="meta.type" effect="light" size="small" class="audit-status-tag">
    <el-icon v-if="meta.icon" class="audit-status-icon"><component :is="icons[meta.icon]" /></el-icon>
    {{ meta.text }}
  </el-tag>
</template>
<script setup>
import { computed, defineProps } from 'vue'
import { Clock, CircleCheck, CircleClose } from '@element-plus/icons-vue'
import { auditStatusMeta } from '@/utils/auditStatus'
const props = defineProps({ status: { type: String, default: '' } })
const icons = { Clock, CircleCheck, CircleClose }
const meta = computed(() => auditStatusMeta(props.status))
</script>
<style scoped>
.audit-status-tag { white-space: nowrap; }
.audit-status-icon { margin-right: 4px; vertical-align: middle; }
</style>
