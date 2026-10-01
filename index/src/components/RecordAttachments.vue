<template>
  <div>
    <el-upload v-if="editable" :http-request="upload" :show-file-list="false" accept=".pdf,.doc,.docx,.png,.jpg,.jpeg">
      <el-button :loading="uploading">上传证明材料</el-button>
      <template #tip><small>支持 PDF、Word、图片，单个文件不超过 10 MB</small></template>
    </el-upload>
    <div v-for="(file, i) in files" :key="file.url">
      <el-link :href="getFileUrl(file.url)" target="_blank" rel="noopener">{{ file.name || '查看材料' }}</el-link>
      <el-button v-if="editable" link type="danger" @click="remove(i)">移除</el-button>
    </div>
  </div>
</template>
<script setup>
import { computed, ref, defineProps, defineEmits } from 'vue'
import { ElMessage } from 'element-plus'
import { uploadFile } from '@/api/competition'
import { getFileUrl } from '@/utils/imageUrl'
const props = defineProps({ modelValue: { type: String, default: '' }, editable: Boolean, limit: { type: Number, default: 5 } })
const emit = defineEmits(['update:modelValue'])
const uploading = ref(false)
const files = computed(() => { try { const v = JSON.parse(props.modelValue || '[]'); return Array.isArray(v) ? v : [] } catch { return [] } })
const remove = i => emit('update:modelValue', JSON.stringify(files.value.filter((_, n) => n !== i)))
const upload = async ({ file, onSuccess, onError }) => {
  uploading.value = true
  try {
    if (files.value.length >= props.limit) throw new Error(`最多上传 ${props.limit} 个文件，请先移除旧文件`)
    const data = new FormData(); data.append('file', file)
    const res = await uploadFile(data)
    if (res.code !== 200 || !res.data?.length) throw new Error(res.msg || '上传失败')
    emit('update:modelValue', JSON.stringify([...files.value, ...res.data.map(url => ({ url, name: file.name }))]))
    onSuccess(res)
  } catch (e) { ElMessage.error(e.message || '上传失败'); onError(e) }
  finally { uploading.value = false }
}
</script>
