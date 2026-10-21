const { test } = require('node:test')
const assert = require('node:assert/strict')
const { loadBindings } = require('./helpers/sourceHarness')

test('innovation upload API and student preflight enforce the 10 MB limit', async () => {
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
})
