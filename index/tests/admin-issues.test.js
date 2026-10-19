const { test } = require('node:test')
const assert = require('node:assert/strict')
const { readdirSync } = require('node:fs')
const path = require('node:path')
const { sourceRoot, readSource: read, loadBindings: handlers } = require('./helpers/sourceHarness')

test('all frontend business requests use GET or POST', () => {
  for (const file of readdirSync(path.join(sourceRoot, 'api'))) {
    assert.doesNotMatch(read('api/' + file), /method:\s*['"](?:put|delete|patch)['"]|request\.(?:put|delete|patch)\(/i, file)
  }
})

test('dashboard approval submits the business ID, decision and rejection reason', async () => {
  const calls = []
  const busy = { value: [] }
  const actions = handlers('views/admin/Dashboard.vue', ['submitApproval'], {
    auditingIds: busy,
    ElMessageBox: { confirm: async () => {}, prompt: async () => ({ value: ' 补充材料 ' }) },
    ElMessage: { success() {}, error: message => calls.push(['error', message]) },
    auditLeaveRequest: async (...args) => { calls.push(args); return { code: 200 } },
    thoughtReportApi: {}, partyCourseApi: {}, volunteerServiceApi: {}, partyApplicationApi: {},
    loadStatistics: async () => calls.push('refresh'),
    window: { dispatchEvent: () => calls.push('counters') }, Event: class {}
  })
  const approval = { id: 'leave_db-id', dataId: 'db-id', dataType: 'leave', title: '请假申请' }
  await actions.submitApproval(approval, true)
  await actions.submitApproval(approval, false)
  assert.deepEqual(calls.filter(Array.isArray), [['db-id', 'approved', ''], ['db-id', 'rejected', '补充材料']])
  assert.equal(busy.value.length, 0)
})

test('dashboard approval links open the matching record in its business page', () => {
  const destinations = []
  const { openApproval } = handlers('views/admin/Dashboard.vue', ['approvalRoutes', 'approvalTabs', 'openApproval'], {
    router: { push: target => destinations.push(target) },
    ElMessage: { warning: message => assert.fail(message) }
  })
  for (const [type, path, tab] of [
    ['leave', '/admin/daily/leave'],
    ['partyApplication', '/admin/party/management', 'application'],
    ['thoughtReport', '/admin/party/management', 'report'],
    ['partyCourse', '/admin/party/management', 'course'],
    ['volunteerService', '/admin/daily/volunteer', 'service'],
    ['competition', '/admin/innovation/competitions/list'],
    ['project', '/admin/innovation/projects/list'],
    ['paper', '/admin/innovation/papers/list'],
    ['patent', '/admin/innovation/patents/list']
  ]) {
    openApproval({ dataType: type, dataId: 'record-id' })
    assert.equal(destinations.at(-1).path, path)
    assert.equal(destinations.at(-1).query.approvalId, 'record-id')
    assert.equal(destinations.at(-1).query.approvalTab, tab)
  }
})

test('daily approval deep link opens the matching row after data loads', () => {
  const opened = [], warnings = []
  const { openLinkedReview } = handlers('views/admin/DailyControl.vue', ['openedApprovalId', 'openLinkedReview'], {
    route: { query: { approvalId: 'db-id' } },
    allRows: { leave: [{ id: 'db-id', reason: '事假' }] },
    openReview: row => opened.push(row.id),
    ElMessage: { warning: message => warnings.push(message) }
  })
  openLinkedReview('leave')
  openLinkedReview('leave')
  assert.deepEqual(opened, ['db-id'])
  assert.deepEqual(warnings, [])
})

test('competition dictionary combines actual level/status fields before pagination', () => {
  const tableData = { value: [] }
  const pagination = { current: 1, size: 1 }
  const actions = handlers('views/admin/innovation/CompetitionDict.vue', ['filterAndPaginate'], {
    allData: { value: [
      { competitionName: '数学竞赛', competitionLevel: '重点赛事', competitionStatus: '启用' },
      { competitionName: '编程竞赛', competitionLevel: '重大赛事', competitionStatus: '启用' },
      { competitionName: '编程竞赛', competitionLevel: '重大赛事', competitionStatus: '禁用' }
    ] }, searchForm: { name: '竞赛', level: '重大赛事', status: '启用' }, pagination, tableData
  })
  actions.filterAndPaginate()
  assert.equal(pagination.total, 1)
  assert.equal(tableData.value[0].competitionName, '编程竞赛')
})

test('approval locks before confirmation and releases the lock on cancellation', async () => {
  let cancel, confirmations = 0, writes = 0
  const busy = { value: [] }
  const actions = handlers('views/admin/Dashboard.vue', ['submitApproval'], {
    auditingIds: busy,
    ElMessageBox: { confirm: () => { confirmations++; return new Promise((_, reject) => { cancel = reject }) } },
    ElMessage: { error: message => assert.fail(message) },
    auditLeaveRequest: () => { writes++ }
  })
  const approval = { id: 'leave_fixture', dataId: 'fixture', dataType: 'leave' }
  const first = actions.submitApproval(approval, true)
  await actions.submitApproval(approval, true)
  assert.equal(confirmations, 1)
  cancel('cancel')
  await first
  assert.equal(writes, 0)
  assert.equal(busy.value.length, 0)
})

test('internship saves multiselect values as strings and blank dates as null', async () => {
  let payload
  const form = { technologyStack: ['Java', 'Python'], frameworks: ['Spring'], databases: [],
    cloudPlatforms: [], developmentTools: ['Git'], startDate: '2026-10-18', endDate: '' }
  const actions = handlers('views/admin/InternshipList.vue', ['handleSubmit'], {
    form, formRef: { value: { validate: async () => true } },
    addInternship: async data => { payload = data; return { code: 200 } },
    ElMessage: { success() {}, error: message => assert.fail(message) },
    dialogVisible: { value: true }, loadInternshipList() {}, loadStatistics() {}
  })
  await actions.handleSubmit()
  assert.equal(payload.technologyStack, 'Java,Python')
  assert.equal(payload.frameworks, 'Spring')
  assert.equal(payload.endDate, null)
  assert.equal(payload.startDate, '2026-10-18T00:00:00.000Z')
  assert.deepEqual(form.technologyStack, ['Java', 'Python'])
})

test('invalid activity deadlines stop before creating a task', async () => {
  let warnings = 0, writes = 0
  const form = { title: '活动', activityTime: '2026-11-01T10:00:00', location: '教室',
    taskMode: 'create', taskDeadline: '2026-11-01T10:00:00' }
  const actions = handlers('views/admin/ActivityManagement.vue', ['handleCreateActivity'], {
    activityForm: form, activityAttachmentUploading: { value: false },
    createDailyTask: async () => { writes++ }, createActivity: async () => { writes++ },
    ElMessage: { warning: () => { warnings++ } }
  })
  await actions.handleCreateActivity()
  form.taskDeadline = '2026-10-31T10:00:00'
  await actions.handleCreateActivity()
  assert.equal(warnings, 2)
  assert.equal(writes, 0)
})

test('publishing trims field names and their date and conditional references together', async () => {
  let saved
  const form = { title: ' 任务 ', type: ' 信息填写 ', fields: [
    { fieldName: ' 离校时间 ', fieldType: 'date', required: true },
    { fieldName: ' 返校时间 ', fieldType: 'date', notBeforeField: ' 离校时间 ', required: true },
    { fieldName: ' 说明 ', fieldType: 'text', conditionalField: ' 离校时间 ', conditionalValue: ' 示例 ' }
  ] }
  const actions = handlers('views/admin/DailyControl.vue', ['handleCreateTask'], {
    taskForm: form, taskFormRef: { value: { validate: async () => true } },
    taskAttachmentUploading: { value: false },
    createDailyTask: async data => { saved = data; return { code: 200 } },
    ElMessage: { success() {}, warning: message => assert.fail(message), error: message => assert.fail(message) },
    closeTaskDialog() {}, loadTasks() {}
  })
  await actions.handleCreateTask()
  assert.equal(saved.title, '任务')
  assert.equal(saved.fields[0].fieldName, '离校时间')
  assert.equal(saved.fields[1].notBeforeField, '离校时间')
  assert.equal(saved.fields[2].conditionalField, '离校时间')
  assert.equal(saved.fields[2].conditionalValue, '示例')
})

test('Excel utility reads actual XLSX first-sheet rows with read-excel-file v9', async () => {
  const { default: writeExcel } = await import('write-excel-file/node')
  const bytes = await writeExcel([
    [{ value: '学号', type: String }, { value: '成绩', type: String }],
    [{ value: '10240001', type: String }, { value: 92, type: Number }]
  ]).toBuffer()
  const source = read('utils/excel.js').replace("import('read-excel-file/browser')", 'load()')
  const readExcel = new Function('load', source.replace(/export /g, '') + '\nreturn readExcelObjects')(
    () => import('read-excel-file/node'))
  const records = await readExcel(bytes)
  assert.equal(records.length, 1)
  assert.equal(records[0]['学号'], '10240001')
  assert.equal(records[0]['成绩'], '92')
})
