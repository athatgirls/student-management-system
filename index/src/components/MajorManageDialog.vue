<template>
  <el-dialog v-model="dialogVisible" title="管理专业" width="500px">
    <el-form label-width="90px" @submit.prevent>
      <el-form-item label="专业名称" required>
        <el-input
          v-model="newMajorName"
          placeholder="请输入专业名称"
          clearable
          @keyup.enter="handleAddMajor"
        />
      </el-form-item>
    </el-form>
    <div class="major-list">
      <div v-for="major in majorList" :key="major" class="major-item">
        <span>{{ major }}</span>
        <el-button type="danger" link @click="handleDeleteMajor(major)">删除</el-button>
      </div>
      <el-empty v-if="majorList.length === 0" description="暂无专业" :image-size="80" />
    </div>
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="addLoading" @click="handleAddMajor">确定</el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup>
/* global defineProps, defineEmits */
import { computed, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createMajor, deleteMajorByName, getMajors } from '@/api/major'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:visible', 'changed'])
const dialogVisible = computed({
  get: () => props.visible,
  set: (value) => emit('update:visible', value)
})
const majorList = ref([])
const newMajorName = ref('')
const addLoading = ref(false)

const loadMajors = async () => {
  try {
    const res = await getMajors()
    if (res.code === 200) {
      majorList.value = res.data || []
    } else {
      ElMessage.error(res.msg || '加载专业列表失败')
    }
  } catch (error) {
    ElMessage.error('加载专业列表失败')
  }
}

watch(() => props.visible, (visible) => {
  if (visible) {
    newMajorName.value = ''
    loadMajors()
  }
})

const handleAddMajor = async () => {
  const majorName = (newMajorName.value || '').trim()
  if (!majorName) {
    ElMessage.warning('请输入专业名称')
    return
  }

  addLoading.value = true
  try {
    const res = await createMajor({ majorName })
    if (res.code === 200) {
      ElMessage.success(res.msg || '专业添加成功')
      newMajorName.value = ''
      await loadMajors()
      emit('changed')
    } else {
      ElMessage.error(res.msg || '添加失败')
    }
  } catch (error) {
    ElMessage.error('添加失败')
  } finally {
    addLoading.value = false
  }
}

const handleDeleteMajor = async (majorName) => {
  try {
    await ElMessageBox.confirm(
      `确认删除专业「${majorName}」吗？`,
      '删除专业',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
  } catch (error) {
    return
  }

  try {
    const res = await deleteMajorByName(majorName)
    if (res.code === 200) {
      ElMessage.success(res.msg || '专业已删除')
      await loadMajors()
      emit('changed')
    } else {
      ElMessage.error(res.msg || '删除失败')
    }
  } catch (error) {
    ElMessage.error('删除失败')
  }
}
</script>

<style scoped>
.major-list {
  border-top: 1px solid #e4e7ed;
  max-height: 280px;
  overflow-y: auto;
}

.major-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 44px;
  border-bottom: 1px solid #ebeef5;
}
</style>
