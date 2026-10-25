/* Run with mongosh against disciplinary_construction, while BOTH app containers are stopped.
 * MIS_IDENTITY_MODE=dry-run|apply|verify|rollback; MIS_IDENTITY_RUN=<release>.
 * No student/account/attachment documents are deleted. Never match by name or guess a dirty ID.
 * The private journal stores only touched fields, permitting exact rollback before reopening writes.
 */
'use strict'

const identityCollections = [
  'academic_events', 'alumni', 'appointments', 'competition', 'daily_activities',
  'daily_task_submissions', 'employment_intentions', 'honors', 'ideological_tweets',
  'internship_employment', 'leave_requests', 'paper', 'party_applications',
  'party_courses', 'party_members', 'patent', 'project', 'study_records',
  'thought_reports', 'volunteer_services'
]
const intentionIndex = 'employment_student_account_unique'
const own = (obj, key) => Object.prototype.hasOwnProperty.call(obj, key)
const text = value => value == null ? '' : String(value).trim()
const same = (a, b) => JSON.stringify(a) === JSON.stringify(b)

function identityPlan(students, collections) {
  const accounts = new Map(), numbers = new Map()
  for (const student of students) {
    const account = text(student._id), number = text(student.studentId)
    if (!account || !number || accounts.has(account) || numbers.has(number)) {
      throw new Error('Student roster contains missing or duplicate identifiers; manual review required')
    }
    accounts.set(account, student); numbers.set(number, student)
  }
  const resolve = (value, accountOnly = false) => {
    const key = text(value), a = accounts.get(key), b = accountOnly ? null : numbers.get(key)
    if (!key || (!a && !b) || (a && b && text(a._id) !== text(b._id))) {
      throw new Error('Unresolved or ambiguous student reference; no changes allowed')
    }
    return a || b
  }
  const changes = [], counts = {}, intentions = new Set()
  for (const collection of [...identityCollections, 'activities']) {
    const documents = collections[collection] || []
    counts[collection] = documents.length
    documents.forEach((doc, ordinal) => {
      try {
        let desired
        if (collection === 'activities') {
          const normalize = ids => {
            if (!Array.isArray(ids)) throw new Error('Invalid participant list')
            return ids.map(id => text(resolve(id, true)._id))
          }
          const current = doc.participantStudentAccountIds
          const legacy = doc.participantStudentIds
          if (current == null && legacy == null) return
          const ids = normalize(current == null ? legacy : current)
          if (current != null && legacy != null && !same(ids, normalize(legacy))) {
            throw new Error('Conflicting participant lists')
          }
          desired = { participantStudentAccountIds: ids }
        } else {
          const refs = []
          for (const field of ['studentAccountId', 'studentDbId', 'userId']) {
            if (text(doc[field])) refs.push(resolve(doc[field], true))
          }
          if (text(doc.studentId)) refs.push(resolve(doc.studentId))
          // Before this refactor employment intention _id was the student's account ID.
          if (collection === 'employment_intentions' && !text(doc.studentAccountId)) refs.push(resolve(doc._id, true))
          if (!refs.length || refs.some(ref => text(ref._id) !== text(refs[0]._id))) {
            throw new Error('Missing or conflicting ownership')
          }
          const student = refs[0], account = text(student._id)
          desired = { studentAccountId: account, studentId: text(student.studentId) }
          if (!['party_members', 'party_applications', 'study_records', 'alumni'].includes(collection)) {
            desired.studentName = student.name || ''
          }
          if (collection === 'employment_intentions') {
            if (intentions.has(account)) throw new Error('Duplicate employment intentions; manual review required')
            intentions.add(account)
          }
        }
        const after = {}, before = {}, absent = []
        for (const [key, value] of Object.entries(desired)) {
          if (own(doc, key) && same(doc[key], value)) continue
          after[key] = value
          if (own(doc, key)) before[key] = doc[key]
          else absent.push(key)
        }
        if (Object.keys(after).length) changes.push({ collection, documentId: doc._id, before, absent, after })
      } catch (error) {
        // Only collection and row ordinal are logged, never names, credentials or actual IDs.
        throw new Error(`${collection} row ${ordinal + 1}: ${error.message}`)
      }
    })
  }
  return { changes, counts, students: students.length }
}

async function runIdentityMigration(database, mode, run, log) {
  if (database.getName() !== 'disciplinary_construction') throw new Error('Unexpected database')
  if (!['dry-run', 'apply', 'verify', 'rollback'].includes(mode) || !/^[a-f0-9]{7,40}$/.test(run)) {
    throw new Error('Explicit mode and release ID required')
  }
  const journal = database.getCollection(`identity_migration_${run}`)
  const intent = database.getCollection('employment_intentions')
  const snapshot = async () => {
    const data = {}
    for (const name of [...identityCollections, 'activities']) data[name] = await (await database.getCollection(name).find({})).toArray()
    return identityPlan(await (await database.students.find({}, { _id: 1, studentId: 1, name: 1 })).toArray(), data)
  }
  const valuesMatch = (doc, values, absent = []) => doc &&
    Object.entries(values).every(([key, value]) => own(doc, key) && same(doc[key], value)) &&
    absent.every(key => !own(doc, key))
  if (mode === 'rollback') {
    const meta = await journal.findOne({ _id: 'meta' })
    if (!meta) { log('IDENTITY_ROLLBACK_NOT_NEEDED'); return }
    if (meta.phase === 'rolled-back') { log('IDENTITY_ROLLBACK_ALREADY_DONE'); return }
    if (await database.students.countDocuments({}) !== meta.students) {
      throw new Error('Record counts changed; keep app stopped for manual recovery')
    }
    for (const [name, count] of Object.entries(meta.counts)) {
      if (await database.getCollection(name).countDocuments({}) !== count) throw new Error('Record counts changed; keep app stopped for manual recovery')
    }
    const rows = await (await journal.find({ _id: { $ne: 'meta' } })).toArray()
    // Validate ALL records before any rollback. Never overwrite a concurrent/new edit.
    for (const row of rows) {
      const doc = await database.getCollection(row.collection).findOne({ _id: row.documentId })
      if (!valuesMatch(doc, row.after) && !valuesMatch(doc, row.before, row.absent)) {
        throw new Error('Rollback conflict; keep app stopped and restore from the verified backup manually')
      }
    }
    for (const row of rows) {
      const collection = database.getCollection(row.collection)
      const current = await collection.findOne({ _id: row.documentId })
      if (valuesMatch(current, row.before, row.absent)) continue
      const update = {}
      if (Object.keys(row.before).length) update.$set = row.before
      if (row.absent.length) update.$unset = Object.fromEntries(row.absent.map(key => [key, '']))
      if ((await collection.updateOne({ _id: row.documentId, ...row.after }, update)).matchedCount !== 1) {
        throw new Error('Concurrent rollback conflict; app must remain stopped')
      }
    }
    if (!meta.indexExisted && await intent.exists() && (await intent.getIndexes()).some(index => index.name === intentionIndex)) await intent.dropIndex(intentionIndex)
    await journal.updateOne({ _id: 'meta' }, { $set: { phase: 'rolled-back' } })
    log('IDENTITY_ROLLBACK_OK'); return
  }
  const plan = await snapshot()
  log(JSON.stringify({ mode, students: plan.students, counts: plan.counts, updates: plan.changes.length }))
  if (mode === 'dry-run') return
  if (mode === 'verify') {
    if (plan.changes.length) throw new Error('Identity migration incomplete')
    const index = (await intent.getIndexes()).find(item => item.name === intentionIndex)
    if (!index || !index.unique || !same(index.key, { studentAccountId: 1 }) ||
        !same(index.partialFilterExpression, { studentAccountId: { $type: 2 } })) throw new Error('Unique index missing or invalid')
    log('IDENTITY_VERIFY_OK'); return
  }
  if (await journal.countDocuments({}) !== 0) throw new Error('Migration journal already exists; verify or roll back first')
  const indexes = await intent.exists() ? await intent.getIndexes() : []
  const indexExisted = indexes.some(item => item.name === intentionIndex)
  if (indexes.some(item => same(item.key, { studentAccountId: 1 }) &&
      (item.name !== intentionIndex || !item.unique || !same(item.partialFilterExpression, { studentAccountId: { $type: 2 } })))) {
    throw new Error('Conflicting employment index; manual review required')
  }
  await journal.insertOne({ _id: 'meta', phase: 'prepared', counts: plan.counts, students: plan.students, indexExisted })
  if (plan.changes.length) await journal.insertMany(plan.changes.map((row, i) => ({ _id: i, ...row })))
  for (const row of plan.changes) {
    const collection = database.getCollection(row.collection)
    const doc = await collection.findOne({ _id: row.documentId })
    if (!valuesMatch(doc, row.before, row.absent)) throw new Error('Concurrent modification; app must remain stopped')
    const filter = { _id: row.documentId, ...row.before }
    for (const key of row.absent) filter[key] = { $exists: false }
    if ((await collection.updateOne(filter, { $set: row.after })).matchedCount !== 1) throw new Error('Concurrent write detected')
  }
  await intent.createIndex({ studentAccountId: 1 }, { name: intentionIndex, unique: true,
    partialFilterExpression: { studentAccountId: { $type: 2 } } })
  const after = await snapshot()
  if (after.changes.length || after.students !== plan.students || !same(after.counts, plan.counts)) throw new Error('Post-migration verification failed')
  await journal.updateOne({ _id: 'meta' }, { $set: { phase: 'applied' } })
  log('IDENTITY_APPLY_OK')
}

if (typeof module !== 'undefined') module.exports = { identityCollections, identityPlan, runIdentityMigration }
