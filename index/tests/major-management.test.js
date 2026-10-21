const { test } = require('node:test')
const assert = require('node:assert/strict')
const { loadBindings } = require('./helpers/sourceHarness')

// Execute the actual component handlers with isolated UI/API doubles.
function importHarness(response) {
  const calls = []
  const context = {
    isPreviewMode: () => false,
    selectedGrade: { value: '2026级' },
    importForm: { file: 'test-fixture.xlsx' },
    importLoading: { value: false },
    showImportDialog: { value: true },
    uploadRef: { value: { clearFiles() {} } },
    majorOptions: { value: [] },
    FormData: class { append() {} },
    ElMessage: { success() {}, error() {}, warning() {} },
    ElMessageBox: { alert: async () => {} },
    importStudents: async () => response,
    getMajors: async () => { calls.push('majors'); return { code: 200, data: ['新导入专业'] } },
    loadStudents: () => calls.push('students'),
    loadGradeStudentCounts: () => calls.push('counts')
  }
  context.submit = loadBindings('views/admin/StudentsList.vue', ['loadMajors', 'handleImportSubmit'], context).handleImportSubmit
  return { context, calls }
}

test('successful roster import refreshes the actual major options without a page reload', async () => {
  const { context, calls } = importHarness({ code: 200, data: { imported: 1, total: 1 } })
  await context.submit()
  assert.deepEqual(new Set(calls), new Set(['students', 'counts', 'majors']))
  assert.deepEqual(Array.from(context.majorOptions.value), ['新导入专业'])
  assert.equal(context.importLoading.value, false)
})

test('failed roster import does not refresh options or close the dialog', async () => {
  const { context, calls } = importHarness({ code: 400, msg: 'invalid file' })
  await context.submit()
  assert.deepEqual(calls, [])
  assert.equal(context.showImportDialog.value, true)
  assert.equal(context.importLoading.value, false)
})

test('batch major update uses POST and preserves the selected IDs and major', () => {
  const { batchUpdateMajor } = loadBindings('api/student.js', ['batchUpdateMajor'], { request: config => config })
  const data = { ids: ['fixture-id'], major: '人工智能' }
  const config = batchUpdateMajor(data)
  assert.equal(config.method, 'post')
  assert.equal(config.url, '/student/batch-update-major')
  assert.equal(config.data, data)
})
