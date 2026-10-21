<template>
  <el-row :gutter="20" class="summary-cards">
    <el-col v-for="item in items" :key="item.label" :xs="24" :sm="8">
      <el-card shadow="hover">
        <div class="summary-card">
          <div class="summary-icon" :class="item.tone">
            <el-icon><component :is="icons[item.icon] || Document" /></el-icon>
          </div>
          <div class="summary-content">
            <div class="summary-number">{{ item.value }}</div>
            <div class="summary-label">{{ item.label }}</div>
          </div>
        </div>
      </el-card>
    </el-col>
  </el-row>
</template>

<script setup>
/* global defineProps */
import { Document, Clock, CircleCheck, CircleClose } from '@element-plus/icons-vue'

defineProps({ items: { type: Array, required: true } })
const icons = { total: Document, pending: Clock, approved: CircleCheck, closed: CircleClose }
</script>

<style scoped>
.summary-cards { margin-bottom: 20px; row-gap: 20px; }
.summary-card { display: flex; align-items: center; padding: 10px; }
.summary-icon {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-right: 15px;
  font-size: 24px;
  color: white;
  background-color: #409eff;
}
.summary-icon.warning { background-color: #e6a23c; }
.summary-icon.success { background-color: #67c23a; }
.summary-icon.info { background-color: #909399; }
.summary-content { min-width: 0; }
.summary-number { font-size: 24px; font-weight: bold; color: #333; margin-bottom: 5px; }
.summary-label { font-size: 14px; color: #666; }
@media (max-width: 768px) {
  .summary-cards { margin-bottom: 15px; row-gap: 15px; }
  .summary-card { padding: 0; }
  .summary-icon { width: 45px; height: 45px; font-size: 20px; margin-right: 10px; }
  .summary-number { font-size: 20px; }
}
</style>
