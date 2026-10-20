const { test } = require('node:test')
const assert = require('node:assert/strict')
const { readSource, loadBindings } = require('./helpers/sourceHarness')

test('paper management keeps audit and score maintenance inside the detail dialog', () => {
  const template = readSource('views/admin/innovation/Papers.vue').split('</template>\n\n<script setup>')[0]
  const actions = template.split('label="操作"')[1].split('</el-table-column>')[0]
  assert.match(actions, /handleView\(row\)/)
  assert.match(actions, /handleDelete\(row\)/)
  assert.doesNotMatch(actions, /handleApprove|handleReject|openScoreDialog/)
  const dialog = template.split('title="论文详情"')[1]
  assert.match(dialog, /@click="saveScore"/)
  assert.match(dialog, /handleApprove\(currentRow\)/)
  assert.match(dialog, /handleReject\(currentRow\)/)
})

test('saving a score updates both the detail and list without approving the paper', async () => {
  const existing = { id: 'paper-1', studentName: '张三', auditStatus: '待审核', scorePending: true }
  const allData = { value: [existing] }
  const currentRow = { value: existing }
  const detailDialogVisible = { value: false }
  const scoreForm = { scoreRatio: '', paperScore: null }
  const scoreSaving = { value: false }
  let refreshed = 0
  const { saveScore, handleView } = loadBindings('views/admin/innovation/Papers.vue', ['saveScore', 'handleView'], {
    allData, currentRow, detailDialogVisible, scoreForm, scoreSaving,
    updatePaperScore: async (id, payload) => ({ id, ...payload, auditStatus: '待审核', scorePending: false }),
    filterAndPaginate: () => { refreshed++ },
    ElMessage: { success: () => {}, error: message => { throw new Error(message) } }
  })
  handleView(existing)
  scoreForm.scoreRatio = '7:3'
  scoreForm.paperScore = 0
  await saveScore()
  assert.equal(detailDialogVisible.value, true)
  assert.equal(currentRow.value.studentName, '张三')
  assert.equal(currentRow.value.auditStatus, '待审核')
  assert.equal(currentRow.value.paperScore, 0)
  assert.equal(allData.value[0].scoreRatio, '7:3')
  assert.equal(refreshed, 1)
  assert.equal(scoreSaving.value, false)
})
