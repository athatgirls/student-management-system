const { test } = require('node:test')
const assert = require('node:assert/strict')
const { loadBindings } = require('./helpers/sourceHarness')

const { overviewGroups, getOverviewRecords, getBonusDetails, buildOverviewGroups, overviewValue } = loadBindings('utils/overviewLayout.js', [
  'overviewGroups', 'getOverviewRecords', 'getBonusDetails', 'buildOverviewGroups', 'overviewValue'
])

test('overview groups keep the A/B/C/D weights and minimum numbered record slots', () => {
  const groups = buildOverviewGroups([])
  assert.equal(groups.map(group => group.label).join(','), 'A（30%）,B（20%）,C（35%）,D（15%）')
  assert.equal(groups[0].width, 1)
  assert.equal(groups[1].sections.map(section => section.slots).join(','), '3,2')
  assert.equal(groups[2].sections.map(section => section.slots).join(','), '2,2')
  assert.equal(groups[1].summaries.length, 0)
  assert.equal(groups[2].summaries.length, 0)
  assert.equal(groups[1].sections[0].columns.at(-1).label, '得分')
  assert.equal(groups[2].sections[0].columns.at(-1).label, '得分')
  assert.equal(groups[3].summaries.join(','), '基础分,加分项明细,D合计')
  assert.equal(groups[3].width, 3)
  assert.equal(groups[1].width, 3 * 6 + 2 * 4)
  assert.equal(groups[2].width, 2 * 4 + 2 * 4)
  assert.equal(overviewGroups.length, 4)
})

test('extra papers, patents, projects and competitions add columns without dropping records', () => {
  const student = {
    papers: Array.from({ length: 4 }, (_, i) => ({ paperTitle: `论文${i}` })),
    patents: Array.from({ length: 3 }, (_, i) => ({ patentTitle: `专利${i}` })),
    projects: Array.from({ length: 3 }, (_, i) => ({ projectName: `项目${i}` })),
    competitions: Array.from({ length: 4 }, (_, i) => ({ competitionName: `竞赛${i}` }))
  }
  const groups = buildOverviewGroups([student, {}])
  assert.equal(groups[1].sections.map(section => section.slots).join(','), '4,3')
  assert.equal(groups[2].sections.map(section => section.slots).join(','), '3,4')
  assert.equal(getOverviewRecords(student, 'papers')[3].paperTitle, '论文3')
})

test('D activities combine academic, daily and volunteer records; unknown scores stay uncomputed', () => {
  const student = {
    academic: [{ title: '讲座' }], daily: [{ title: '班会' }], volunteer: [{ serviceName: '志愿服务', serviceHours: 2 }],
    honors: [{ title: '三好学生' }], appointments: [{ position: '班长', organization: '一班' }]
  }
  const records = getOverviewRecords(student, 'activities')
  assert.equal(records.map(record => record.overviewType).join(','), '学术活动,日常活动,志愿活动')
  assert.equal(getBonusDetails(student).map(item => item.category).join(','), '活动,活动,活动,荣誉,任职')
  assert.equal(getBonusDetails(student)[4].text, '班长（一班）')
  assert.equal(overviewValue({ score: 100 }, 'itemScore'), '—')
  assert.equal(buildOverviewGroups([student])[3].width, 3)
})

test('paper overview shows the first three authors by name in signature order', () => {
  assert.equal(overviewGroups[1].sections[0].columns.map(column => column.label).join(','),
    '论文题目,期刊,期刊级别,前三作者排序,得分比例,得分')
  assert.equal(overviewValue({ scoreRatio: '7:3', paperScore: 0 }, 'scoreRatio'), '7:3')
  assert.equal(overviewValue({ scoreRatio: '7:3', paperScore: 0 }, 'paperScore'), 0)
  assert.equal(overviewValue({ scoreRatio: '7:3', scorePending: true }, 'scoreRatio'), '7:3（待审核）')
  assert.equal(overviewValue({ paperScore: 0, scorePending: true }, 'paperScore'), '0（待审核）')
  assert.equal(overviewValue({ paperScore: 0, scorePending: false }, 'paperScore'), 0)
  assert.equal(overviewValue({ studentName: '张三', authorRank: '1', coAuthors: '李四，王五,赵六' }, 'firstThreeAuthors'), '张三、李四、王五')
  assert.equal(overviewValue({ studentName: '张三', authorRank: '2', coAuthors: '李四、王五、赵六' }, 'firstThreeAuthors'), '李四、张三、王五')
  assert.equal(overviewValue({ studentName: '张三', authorRank: '4', coAuthors: '李四,王五,赵六' }, 'firstThreeAuthors'), '李四、王五、赵六')
  assert.equal(overviewValue({ studentName: '张三', authorRank: '5', coAuthors: '李四,王五,赵六' }, 'firstThreeAuthors'), '李四、王五、赵六')
  assert.equal(overviewValue({ studentName: '张三', authorRank: 1, coAuthors: '' }, 'firstThreeAuthors'), '张三')
  assert.equal(overviewValue({ studentName: '张三', authorRank: 2, coAuthors: '' }, 'firstThreeAuthors'), '—')
  assert.equal(overviewValue({ studentName: '张三', authorRank: '未知', coAuthors: '李四' }, 'firstThreeAuthors'), '—')
})
