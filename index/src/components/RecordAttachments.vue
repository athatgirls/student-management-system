<template>
  <div>
    <el-upload v-if="editable" :http-request="upload" :before-upload="beforeUpload" :show-file-list="false" :disabled="uploading" accept=".pdf,.doc,.docx,.png,.jpg,.jpeg">
      <el-button :loading="uploading">{{ label }}</el-button>
      <template #tip><small>支持 PDF、Word、图片，单个文件不超过 10 MB</small></template>
    </el-upload>
    <div v-for="(file, i) in files" :key="file.url">
      <PrivateAttachments :attachments="[file]" />
      <el-button v-if="editable" link type="danger" @click="remove(i)">移除</el-button>
    </div>
  </div>
</template>
<script setup>
import { computed, ref, defineProps, defineEmits } from 'vue'
import { ElMessage } from 'element-plus'
import { uploadFile } from '@/api/competition'
import PrivateAttachments from '@/components/PrivateAttachments.vue'
import { attachmentError } from '@/utils/uploadValidation'
const props = defineProps({ modelValue: { type: String, default: '' }, editable: Boolean, limit: { type: Number, default: 5 }, label: { type: String, default: '上传证明材料' } })
const emit = defineEmits(['update:modelValue', 'uploading'])
const uploading = ref(false)
const files = computed(() => { try { const v = JSON.parse(props.modelValue || '[]'); return Array.isArray(v) ? v : [] } catch { return [] } })
const remove = i => emit('update:modelValue', JSON.stringify(files.value.filter((_, n) => n !== i)))
const beforeUpload = file => {
  const error = attachmentError(file) || (files.value.length >= props.limit ? `最多上传 ${props.limit} 个文件，请先移除旧文件` : '')
  if (error) { ElMessage.error(error); return false }
  return true
}
const upload = async ({ file, onSuccess, onError }) => {
  if (uploading.value) {
    const error = new Error('请等待当前文件上传完成')
    ElMessage.error(error.message); onError(error); return
  }
  // Also guard direct invocations of the custom uploader before any network request.
  if (!beforeUpload(file)) { onError(new Error('文件校验未通过')); return }
  uploading.value = true
  emit('uploading', true)
  try {
    if (!file.size || file.size > 10 * 1024 * 1024) throw new Error('单个文件必须非空且不超过 10MB')
    if (files.value.length >= props.limit) throw new Error(`最多上传 ${props.limit} 个文件，请先移除旧文件`)
    const data = new FormData(); data.append('file', file)
    const res = await uploadFile(data)
    if (res.code !== 200 || !res.data?.length) throw new Error(res.msg || '上传失败')
    emit('update:modelValue', JSON.stringify([...files.value, ...res.data.map(url => ({ url, name: file.name }))]))
    onSuccess(res)
  } catch (e) { ElMessage.error(e.message || '上传失败'); onError(e) }
  finally { uploading.value = false; emit('uploading', false) }
}
</script>
