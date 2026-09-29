const { test } = require('node:test')
const assert = require('node:assert/strict')
const { readFileSync } = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const { parseSync } = require('@babel/core')

// Execute the actual component handlers with isolated UI/API doubles.
function importHarness(response) {
  const source = readFileSync(path.join(__dirname, '../src/views/admin/StudentsList.vue'), 'utf8')
    .split('<script setup>')[1].split('</script>')[0]
  const ast = parseSync(source, { configFile: false, babelrc: false })
  const selected = ast.program.body.filter(node => node.type === 'VariableDeclaration' &&
    node.declarations.some(declaration => ['loadMajors', 'handleImportSubmit'].includes(declaration.id.name)))
  assert.equal(selected.length, 2)
  const calls = []
  const context = vm.createContext({
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
  })
  vm.runInContext(selected.map(node => source.slice(node.start, node.end)).join('\n') +
    '\nglobalThis.submit = handleImportSubmit', context)
  return { context, calls }
}

test('successful roster import refreshes the actual major options without a page reload', async () => {
  const { context, calls } = importHarness({ code: 200, data: { imported: 1, total: 1 } })
  await context.submit()
  assert.deepEqual(calls, ['students', 'counts', 'majors'])
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
  const source = readFileSync(path.join(__dirname, '../src/api/student.js'), 'utf8')
  const ast = parseSync(source, { configFile: false, babelrc: false })
  const exported = ast.program.body.find(node => node.type === 'ExportNamedDeclaration' &&
    node.declaration?.id?.name === 'batchUpdateMajor')
  assert.ok(exported)
  const context = vm.createContext({ request: config => config })
  vm.runInContext(source.slice(exported.declaration.start, exported.declaration.end), context)
  const data = { ids: ['fixture-id'], major: '人工智能' }
  const config = context.batchUpdateMajor(data)
  assert.equal(config.method, 'post')
  assert.equal(config.url, '/student/batch-update-major')
  assert.equal(config.data, data)
})
