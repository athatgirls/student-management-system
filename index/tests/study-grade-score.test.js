const { test } = require('node:test')
const assert = require('node:assert/strict')
const { loadBindings } = require('./helpers/sourceHarness')
const { calculateStudyGrade } = loadBindings('utils/studyGradeScore.js', ['calculateStudyGrade'])

test('A combines credit-weighted degree and non-degree averages using 70/30', () => {
  const result = calculateStudyGrade([
    { courseType: 'degree', score: 90, credit: 3 },
    { courseType: 'degree', score: 60, credit: 1 },
    { courseType: 'non_degree', score: 80, credit: 2 },
    { courseType: 'non_degree', score: 100, credit: 2 }
  ])
  assert.equal(result.x, 82.5)
  assert.equal(result.y, 90)
  assert.ok(Math.abs(result.score - 84.75) < 1e-9)
})

test('unclassified, zero-credit and invalid grades are excluded; both groups are required', () => {
  const records = [
    { courseType: 'degree', score: 90, credit: 2 },
    { courseType: 'degree', score: 50, credit: 0 },
    { courseType: 'non_degree', score: 70, credit: 2 },
    { score: 100, credit: 3 }
  ]
  assert.equal(calculateStudyGrade(records).score, 84)
  assert.equal(calculateStudyGrade(records.slice(0, 2)), null)
  assert.equal(calculateStudyGrade([{ courseType: 'non_degree', score: 90, credit: 0 }]), null)
})
