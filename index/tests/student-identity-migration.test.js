const { test } = require('node:test')
const assert = require('node:assert/strict')
const { identityCollections, identityPlan } = require('../../scripts/student-identity-migration')
const { readSource, loadBindings } = require('./helpers/sourceHarness')
const students = [{ _id: 'account-a', studentId: '10001', name: '甲' }, { _id: 'account-b', studentId: '10002', name: '乙' }]

test('migration covers the actual Mongo collection names and never touches accounts or files', () => {
  assert.equal(identityCollections.length, 20)
  for (const name of ['competition', 'paper', 'patent', 'project']) assert.ok(identityCollections.includes(name))
  for (const name of ['students', 'admins', 'uploaded_files']) assert.ok(!identityCollections.includes(name))
})

test('migration resolves all legacy identity shapes without mutating its inputs', () => {
  const input = {
    study_records: [{ _id: 'grade', studentDbId: 'account-a', studentId: '10001', score: 92, attachments: 'proof' }],
    honors: [{ _id: 'honor', userId: 'account-a', studentId: '10001' }],
    competition: [{ _id: 'competition', studentId: 'account-a' }],
    employment_intentions: [{ _id: 'account-a', studentId: '10001' }],
    activities: [{ _id: 'activity', participantStudentIds: ['account-a', 'account-b'] }]
  }
  const original = JSON.stringify(input)
  const plan = identityPlan(students, input)
  assert.equal(plan.changes.length, 5)
  assert.equal(JSON.stringify(input), original)
  for (const row of plan.changes.filter(row => row.collection !== 'activities')) {
    assert.equal(row.after.studentAccountId, 'account-a')
    assert.ok(!Object.keys(row.after).some(key => ['score', 'attachments', 'password', '_id'].includes(key)))
  }
  const competition = plan.changes.find(row => row.collection === 'competition')
  assert.equal(competition.before.studentId, 'account-a')
  assert.equal(competition.after.studentId, '10001')
  assert.deepEqual(plan.changes.at(-1).after.participantStudentAccountIds, ['account-a', 'account-b'])
})

test('normalized data is idempotent and no business row is dropped', () => {
  const row = { _id: 'row', studentAccountId: 'account-a', studentId: '10001', studentName: '甲' }
  const plan = identityPlan(students, { paper: [row] })
  assert.equal(plan.changes.length, 0)
  assert.equal(plan.counts.paper, 1)
  assert.equal(plan.students, 2)
})

test('unresolved, conflicting, ambiguous and duplicate ownership stop the whole migration', () => {
  for (const data of [
    { study_records: [{ _id: 'x', studentDbId: 'dirty-value', studentId: '10001' }] },
    { study_records: [{ _id: 'x', studentDbId: 'account-b', studentId: '10001' }] },
    { paper: [{ _id: 'x' }] },
    { activities: [{ _id: 'x', participantStudentIds: ['missing'] }] },
    { activities: [{ _id: 'x', participantStudentIds: ['account-a'], participantStudentAccountIds: ['account-b'] }] },
    { employment_intentions: [{ _id: 'account-b', studentId: '10001' }] },
    { employment_intentions: [
      { _id: 'account-a', studentId: '10001' },
      { _id: 'new', studentAccountId: 'account-a', studentId: '10001' }
    ] }
  ]) assert.throws(() => identityPlan(students, data))
  assert.throws(() => identityPlan([...students, { _id: '10001', studentId: '20001' }], { paper: [{ _id: 'x', studentId: '10001' }] }))
  assert.throws(() => identityPlan([...students, { _id: 'c', studentId: '10001' }], {}))
})

test('activity management counts and enables synchronization using the new account list', () => {
  const source = readSource('views/admin/ActivityManagement.vue')
  assert.ok(!source.includes('participantStudentIds'))
  assert.match(source, /scope\.row\.participantStudentAccountIds\?\.length/)
  assert.match(source, /:disabled="!\(scope\.row\.participantStudentAccountIds \|\| \[\]\)\.length"/)
  const { volunteerSyncMeta } = loadBindings('views/admin/ActivityManagement.vue', ['volunteerSyncMeta'])
  assert.equal(volunteerSyncMeta({ activityCategory: 'volunteer', participantStudentAccountIds: ['account-a'] }).type, 'success')
  assert.equal(volunteerSyncMeta({ activityCategory: 'volunteer', participantStudentAccountIds: [] }).type, 'warning')
})
