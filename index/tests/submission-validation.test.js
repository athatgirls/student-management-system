const { test } = require('node:test')
const assert = require('node:assert/strict')
const { loadBindings } = require('./helpers/sourceHarness')
const { hasAttachments, orderedDates, taskDateError } = loadBindings('utils/submissionValidation.js', [
  'hasAttachments', 'orderedDates', 'taskDateError'
])
test('empty or malformed material lists are not accepted', () => {
  for (const value of ['', null, '[]', '{}', 'invalid', '[{"name":"fake"}]']) assert.equal(hasAttachments(value), false)
  assert.ok(hasAttachments('[{"url":"/uploads/test.pdf"}]'))
})
test('date order validation catches the report example and custom relationships', () => {
  assert.equal(orderedDates('2026-10-03','2026-10-01'),false)
  assert.equal(orderedDates('2026-10-01','2026-10-01'),true)
  assert.ok(taskDateError([{fieldName:'返校时间',fieldType:'date'}], {'离校时间':'2026-10-03','返校时间':'2026-10-01'}))
  assert.ok(taskDateError([{fieldName:'返回',fieldType:'date',notBeforeField:'出发'}], {'出发':'2026-10-03','返回':'2026-10-01'}))
  assert.equal(taskDateError([{fieldName:'返校时间',fieldType:'date'}], {'离校时间':'2026-10-01','返校时间':'2026-10-03'}),'')
})
