const { test } = require('node:test')
const assert = require('node:assert/strict')
const { readSource, loadBindings } = require('./helpers/sourceHarness')

test('paper, competition, patent and project uploads share the 10 MB preflight check', async () => {
  let requests = 0
  const { uploadFile } = loadBindings('api/competition.js', ['uploadFile'], {
    request: () => { requests++; return Promise.resolve({ code: 200 }) }, Promise
  })
  const limit = 10 * 1024 * 1024
  await assert.rejects(uploadFile({ getAll: () => [{ size: limit + 1 }] }), /不能超过 10MB/)
  assert.equal(requests, 0)
  await uploadFile({ getAll: () => [{ size: limit }] })
  assert.equal(requests, 1)

  const errors = []
  const { beforeUpload } = loadBindings('views/Competition.vue', ['beforeUpload'], {
    ElMessage: { error: message => errors.push(message) }
  })
  assert.equal(beforeUpload({ type: 'image/jpeg', size: limit }), true)
  assert.equal(beforeUpload({ type: 'image/jpeg', size: limit + 1 }), false)
  assert.match(errors[0], /10MB/)

  const types = ['competition', 'paper', 'patent', 'project']
  const page = readSource('views/Competition.vue')
  for (const type of types) {
    assert.ok(page.includes(`handleCustomUpload(options, '${type}')`))
  }
  assert.equal((page.match(/:before-upload="beforeUpload"/g) || []).length, types.length)
})

test('all four innovation pages show attachments only from the view dialog', () => {
  for (const page of ['Competitions', 'Papers', 'Patents', 'Projects']) {
    const source = readSource(`views/admin/innovation/${page}.vue`)
    assert.doesNotMatch(source, /<el-table-column type="expand"/)
    assert.doesNotMatch(source, /PrivateAttachments :attachments="row.attachments"/)
    assert.ok(source.includes('PrivateAttachments :attachments="currentRow.attachments"'), page)
    assert.ok(source.includes('handleView(row)'), page)
    assert.doesNotMatch(source, /:src="getFileUrl\(file\.url/)
  }
  const student = readSource('views/Competition.vue')
  assert.ok(student.includes('PrivateAttachments :attachments="currentDetail.attachments"'))
})
