const { test } = require('node:test')
const assert = require('node:assert/strict')
const { readFileSync } = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const { parseSync } = require('@babel/core')
const { ref, computed } = require('vue')
const { createSSRApp } = require('vue')
const { renderToString } = require('@vue/server-renderer')
const { parse, compileScript } = require('@vue/compiler-sfc')
const { transformSync } = require('@babel/core')
const read = file => readFileSync(path.join(__dirname, '../src', file), 'utf8')
const util = vm.createContext({ Date })
for (const file of ['dateTime', 'uploadValidation', 'auditStatus', 'submissionValidation'])
  vm.runInContext(read(`utils/${file}.js`).replace(/export /g, ''), util)
vm.runInContext('Object.assign(globalThis, { formatDate, formatDateTime, isPastLeaveDate })', util)

function handlers(file, names, mocks) {
  const source = read(file).split('<script setup>')[1].split('</script>')[0]
  const nodes = parseSync(source, { configFile: false, babelrc: false }).program.body.filter(node =>
    node.type === 'VariableDeclaration' && node.declarations.some(d => names.includes(d.id.name)))
  assert.equal(nodes.length, names.length)
  const context = vm.createContext(mocks)
  vm.runInContext(nodes.map(n => source.slice(n.start, n.end)).join('\n') +
    `\nglobalThis.handlers = { ${names.join(',')} }`, context)
  return context.handlers
}

function uploadHarness() {
  const props = { limit: 5, modelValue: '' }
  const calls = [], messages = []
  const context = { ref, computed, props, defineProps: () => props,
    defineEmits: () => (event, value) => { calls.push(event); if (event === 'update:modelValue') props.modelValue = value },
    attachmentError: util.attachmentError, FormData: class { append() {} },
    ElMessage: { error: message => messages.push(message) },
    uploadFile: async () => { calls.push('request'); return { code: 200, data: ['/SCSE@hbut/uploads/proof.pdf'] } }
  }
  const source = read('components/RecordAttachments.vue').split('<script setup>')[1].split('</script>')[0].replace(/^import .*$/gm, '')
  const sandbox = vm.createContext(context)
  vm.runInContext(source + '\nglobalThis.api = {beforeUpload, upload, uploading}', sandbox)
  return { ...sandbox.api, props, calls, messages }
}

test('S1 rejects >10MB before request, but accepts exactly 10MB', async () => {
  const h = uploadHarness()
  const oversized = { name: 'large.pdf', size: 10 * 1024 * 1024 + 1 }
  assert.equal(h.beforeUpload(oversized), false)
  let failed = false
  await h.upload({ file: oversized, onError: () => { failed = true } })
  assert.equal(failed, true)
  assert.equal(h.calls.includes('request'), false)
  assert.equal(h.uploading.value, false)
  assert.equal(h.messages[0], '单个文件不能超过 10MB')
  await h.upload({ file: { name: 'proof.pdf', size: 10 * 1024 * 1024 }, onSuccess() {}, onError: assert.fail })
  assert.equal(h.calls.filter(c => c === 'request').length, 1)
  assert.equal(JSON.parse(h.props.modelValue)[0].name, 'proof.pdf')
})

test('S1 empty/unsupported files and excess file count never upload', async () => {
  const h = uploadHarness()
  for (const file of [{ name: 'empty.pdf', size: 0 }, { name: 'x.html', size: 2 }]) assert.equal(h.beforeUpload(file), false)
  h.props.modelValue = JSON.stringify(Array.from({ length: 5 }, (_, i) => ({ url: `/uploads/${i}.pdf` })))
  assert.equal(h.beforeUpload({ name: 'sixth.pdf', size: 1 }), false)
  assert.equal(h.calls.includes('request'), false)
})

test('S1 concurrent upload does not start a second request or clear the active-upload flag', async () => {
  const h = uploadHarness()
  h.uploading.value = true
  let failed = false
  await h.upload({ file: { name: 'proof.pdf', size: 20 }, onError: () => { failed = true } })
  assert.equal(failed, true)
  assert.equal(h.calls.includes('request'), false)
  assert.equal(h.uploading.value, true)
})

test('S2 uses Beijing calendar day: yesterday disabled, today and future allowed', () => {
  const now = new Date('2026-10-20T16:01:00Z') // Beijing has crossed midnight.
  assert.equal(util.isPastLeaveDate('2026-10-20', now), true)
  assert.equal(util.isPastLeaveDate('2026-10-21', now), false)
  assert.equal(util.isPastLeaveDate('2026-10-22', now), false)
  assert.equal(util.isPastLeaveDate(new Date(2026, 9, 20), now), true)
  assert.equal(util.isPastLeaveDate(new Date(2026, 9, 21), now), false)
  assert.match(read('views/Daily.vue'), /:disabled-date="isPastLeaveDate"/)
})

test('S2 submitting a typed past date stops before validation/upload/API', async () => {
  const messages = []
  const { submitCreate } = handlers('views/Daily.vue', ['submitCreate'], {
    moduleKey: { value: 'leave' }, form: { startDate: '2000-01-01' },
    isPastLeaveDate: util.isPastLeaveDate, ElMessage: { warning: m => messages.push(m) }
  })
  await submitCreate()
  assert.deepEqual(messages, ['请假开始日期不能早于今天'])
})

test('S3 all pending aliases share text/color/icon and every student audit module uses the same component', () => {
  for (const status of ['', 'pending', '待审核', '待审批', '未审核']) {
    const meta = util.auditStatusMeta(status)
    assert.equal(meta.text, '待审核'); assert.equal(meta.type, 'warning'); assert.equal(meta.icon, 'Clock')
  }
  assert.equal(util.auditStatusMeta('已拒绝').type, 'danger')
  assert.equal(util.auditStatusMeta('学生自填').text, '学生自填')
  for (const page of ['Profile', 'Daily', 'Party', 'Competition', 'Internship']) assert.match(read(`views/${page}.vue`), /<AuditStatusTag/)
  assert.match(read('components/AuditStatusTag.vue'), /effect="light" size="small"/)
})

test('S3 the actual status component renders identical Element Plus tags for pending aliases', async () => {
  const { descriptor } = parse(read('components/AuditStatusTag.vue'))
  const compiled = compileScript(descriptor, { id: 'audit-status-test', inlineTemplate: true })
  const code = transformSync(compiled.content, {
    configFile: false, babelrc: false, plugins: [require.resolve('@babel/plugin-transform-modules-commonjs')]
  }).code
  const module = { exports: {} }
  const context = vm.createContext({ module, exports: module.exports,
    require: name => name === '@/utils/auditStatus' ? { auditStatusMeta: util.auditStatusMeta } : require(name)
  })
  vm.runInContext(code, context)
  const renders = []
  for (const status of ['pending', '待审核', '待审批', '未审核']) {
    const app = createSSRApp(module.exports.default, { status })
    app.use(require('element-plus'))
    const html = await renderToString(app)
    assert.match(html, /el-tag--warning/)
    assert.match(html, /el-tag--small/)
    assert.match(html, /el-tag--light/)
    assert.match(html, /<svg/)
    assert.match(html, /待审核/)
    renders.push(html)
  }
  assert.equal(new Set(renders).size, 1)
})

function taskHarness(overrides = {}) {
  const calls = [], messages = []
  const context = {
    currentTask: { value: { id: 'task', fields: [] } }, moduleKey: { value: 'tasks' },
    submitForm: { taskId: 'task', content: '任务说明', fieldData: {}, attachments: '[{"url":"/SCSE@hbut/uploads/proof.pdf","name":"证明.pdf"}]' },
    taskSubmissionUploading: { value: false }, taskSubmitting: { value: false }, submitDialogVisible: { value: true },
    taskDateError: util.taskDateError, shouldShowField: () => true,
    ElMessage: { warning: m => messages.push(m), success() {}, error: assert.fail },
    submitDailyTask: async data => { calls.push(JSON.parse(JSON.stringify(data))); return { code: 200 } },
    loadActiveTasks() {}, loadBusinessData() {}, ...overrides
  }
  return { ...handlers('views/Daily.vue', ['handleSubmitTask'], context), context, calls, messages }
}

test('S4 task submission sends distinct persistent attachments and keeps structured field content', async () => {
  const h = taskHarness({ currentTask: { value: { fields: [{ fieldName: '人数', required: true }] } } })
  h.context.submitForm.fieldData = { 人数: 0 }
  await h.handleSubmitTask()
  assert.equal(h.calls.length, 1)
  assert.deepEqual(JSON.parse(h.calls[0].content), { 人数: 0 })
  assert.equal(JSON.parse(h.calls[0].attachments)[0].name, '证明.pdf')
  assert.equal(h.context.taskSubmitting.value, false)
  assert.equal(h.context.submitDialogVisible.value, false)
})

test('S4 cannot submit while upload is running or send twice while submitting', async () => {
  for (const flag of ['taskSubmissionUploading', 'taskSubmitting']) {
    const h = taskHarness({ [flag]: { value: true } })
    await h.handleSubmitTask()
    assert.equal(h.calls.length, 0)
  }
  const h = taskHarness({ submitDailyTask: async () => { throw new Error('offline') }, ElMessage: { error() {} } })
  await h.handleSubmitTask()
  assert.equal(h.context.taskSubmitting.value, false)
})

test('S4 reopening a submitted task restores its proof files; opening another task clears them', async () => {
  const context = {
    currentTask: { value: {} }, submitForm: { taskId: '', content: '', attachments: 'old', fieldData: {} },
    submitDialogVisible: { value: false },
    getMyDailyTaskSubmission: async id => id === 'one' ? { code: 200, data: { content: '说明', attachments: '[{"url":"/uploads/proof.pdf"}]' } } : { code: 200, data: null }
  }
  const { openSubmit } = handlers('views/Daily.vue', ['openSubmit'], context)
  await openSubmit({ id: 'one' })
  assert.equal(JSON.parse(context.submitForm.attachments).length, 1)
  await openSubmit({ id: 'two' })
  assert.equal(context.submitForm.attachments, '')
})

test('S5 UTC/offset timestamps show Beijing 24-hour time and date-only values keep the calendar date', () => {
  assert.equal(util.formatDateTime('2026-10-20T16:01:02Z'), '2026-10-21 00:01')
  assert.equal(util.formatDateTime('2026-10-20T18:01:02+02:00'), '2026-10-21 00:01')
  assert.equal(util.formatDateTime('2026-10-20T18:01:02'), '2026-10-20 18:01')
  assert.equal(util.formatDate('2026-10-20'), '2026-10-20')
  assert.equal(util.formatDateTime('invalidZ'), '')
  const { displayDetailValue } = handlers('views/Party.vue', ['displayDetailValue'], {
    formatDateTime: util.formatDateTime, formatDate: util.formatDate
  })
  assert.equal(displayDetailValue('2026-10-20T16:01:02Z', 'createTime'), '2026-10-21 00:01')
  assert.equal(displayDetailValue('2026-10-20', 'courseDate'), '2026-10-20')
  assert.equal(displayDetailValue('描述Z', 'content'), '描述Z')
})
