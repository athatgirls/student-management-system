// Integration test: run ONLY in a dedicated disposable Mongo container with NO published ports.
;(async () => {
const assert = require('node:assert/strict')
const { identityCollections, runIdentityMigration } = require('/migration/student-identity-migration.js')
const database = db.getSiblingDB('disciplinary_construction')
assert.equal((await database.getCollectionNames()).length, 0, 'Fresh isolated database required')
const students = [
  { _id: ObjectId('000000000000000000000001'), studentId: '10001', name: '甲', password: 'unchanged-a' },
  { _id: ObjectId('000000000000000000000002'), studentId: '10002', name: '乙', password: 'unchanged-b' }
]
await database.students.insertMany(students)
const id = students[0]._id.toString()
for (const name of identityCollections) {
  let row = { _id: name === 'employment_intentions' ? students[0]._id : ObjectId(), studentId: '10001', content: 'untouched', attachments: 'untouched' }
  if (['study_records', 'appointments'].includes(name)) row.studentDbId = id
  else if (['internship_employment', 'honors'].includes(name)) row.userId = id
  else if (!['party_members', 'party_applications', 'employment_intentions', 'leave_requests', 'alumni'].includes(name)) row.studentId = id
  await database.getCollection(name).insertOne(row)
}
await database.activities.insertOne({ _id: 'activity', participantStudentIds: [id] })
await database.uploaded_files.insertOne({ _id: 'proof', ownerId: id, size: 123 })
const names = ['students', 'uploaded_files', ...identityCollections, 'activities']
const snapshot = async () => {
  const rows = []
  for (const name of names) rows.push([name, await (await database.getCollection(name).find({})).toArray()])
  return EJSON.stringify(Object.fromEntries(rows))
}
const before = await snapshot()
await runIdentityMigration(database, 'dry-run', 'aaaaaaa', print)
assert.equal(await snapshot(), before)
assert.equal((await database.getCollectionNames()).some(name => name.startsWith('identity_migration_')), false)
await runIdentityMigration(database, 'apply', 'aaaaaaa', print)
await runIdentityMigration(database, 'verify', 'aaaaaaa', print)
for (const name of identityCollections) {
  const row = await database.getCollection(name).findOne({})
  assert.equal(row.studentAccountId, id)
  assert.equal(row.studentId, '10001')
  assert.equal(row.content, 'untouched')
  assert.equal(row.attachments, 'untouched')
  assert.equal(await database.getCollection(name).countDocuments({}), 1)
}
assert.equal(EJSON.stringify(await (await database.students.find({})).toArray()), EJSON.stringify(students))
await assert.rejects(async () => database.employment_intentions.insertOne({ _id: 'duplicate', studentAccountId: id }), /E11000/)
await runIdentityMigration(database, 'rollback', 'aaaaaaa', print)
assert.equal(await snapshot(), before, 'Rollback must restore exact BSON values and absent fields')
await runIdentityMigration(database, 'rollback', 'aaaaaaa', print)
await runIdentityMigration(database, 'apply', 'bbbbbbb', print)
await runIdentityMigration(database, 'verify', 'bbbbbbb', print)
await database.paper.updateOne({}, { $set: { studentId: 'new-edit' } })
await assert.rejects(async () => runIdentityMigration(database, 'rollback', 'bbbbbbb', print), /Rollback conflict/)
await database.paper.updateOne({}, { $set: { studentId: '10001' } })
await runIdentityMigration(database, 'rollback', 'bbbbbbb', print)
assert.equal(await snapshot(), before)
// A conflicting index name forces a real Mongo failure after journal/record writes.
await database.employment_intentions.createIndex({ fixtureConflict: 1 }, { name: 'employment_student_account_unique' })
await assert.rejects(async () => runIdentityMigration(database, 'apply', 'ddddddd', print), error => [85, 86].includes(error.code))
await runIdentityMigration(database, 'rollback', 'ddddddd', print)
assert.equal(await snapshot(), before, 'Interrupted apply must also be reversible')
await database.employment_intentions.dropIndex('employment_student_account_unique')
await database.study_records.updateOne({}, { $set: { studentDbId: 'dirty-value' } })
await assert.rejects(async () => runIdentityMigration(database, 'apply', 'ccccccc', print), /Unresolved/)
assert.equal(await database.getCollection('identity_migration_ccccccc').countDocuments({}), 0)
print('MONGO_IDENTITY_INTEGRATION_OK: dry-run, 20 legacy collections, BSON IDs, index enforcement, exact rollback, conflict refusal')
})().catch(error => { print(error.stack || error.message); quit(1) })
