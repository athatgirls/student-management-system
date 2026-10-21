const { test } = require('node:test')
const assert = require('node:assert/strict')
const { loadBindings } = require('./helpers/sourceHarness')

const { normalizeRow } = loadBindings('views/admin/GradeImport.vue', [
  'requiredFields', 'headerMap', 'pickValue', 'inferStatus', 'normalizeRow', 'fieldNameMap'
], { parseCourseType: value => value === '学位课' ? 'degree' : '' })

test('grade import rejects missing, zero, negative, nonfinite and malformed credits', () => {
  const base = { 学号: '10240001', 学期: '2026秋', 课程名称: '数学', 成绩: 90, 课程类别: '学位课' }
  for (const credit of ['', '0', '-1', 'Infinity', 'NaN', 'abc']) {
    const row = normalizeRow({ ...base, 学分: credit }, 0)
    assert.equal(row.valid, false, `credit ${credit} should be rejected`)
    assert.match(row.error, /学分/)
  }
  assert.equal(normalizeRow({ ...base, 学分: '2.5' }, 0).valid, true)
})
