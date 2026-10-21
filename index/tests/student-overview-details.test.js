const { test } = require('node:test')
const assert = require('node:assert/strict')
const vm = require('node:vm')
const Vue = require('vue')
const { parse, compileScript } = require('@vue/compiler-sfc')
const { compile } = require('@vue/compiler-dom')
const { transformSync } = require('@babel/core')
const { renderToString } = require('@vue/server-renderer')
const { readSource, loadBindings } = require('./helpers/sourceHarness')

const fields = loadBindings('utils/overviewFields.js', ['fieldLabels', 'displayValue'])
const layout = loadBindings('utils/overviewLayout.js', ['overviewGroups', 'getOverviewRecords', 'getBonusDetails', 'buildOverviewGroups', 'overviewValue'])
const { calculateStudyGrade } = loadBindings('utils/studyGradeScore.js', ['calculateStudyGrade'])
// Keep dialogs conditional and attachment props observable, without a browser or network requests.
const Dialog = { props: ['modelValue'], setup: (props, { slots }) => () => props.modelValue ? Vue.h('section', { role: 'dialog' }, slots.default?.()) : null }
const Attachments = { props: ['attachments'], setup: props => () => Vue.h('div', { class: 'protected-attachments' }, props.attachments) }
const { descriptor } = parse(readSource('components/StudentGradeDetails.vue'))
const compiled = compileScript(descriptor, { id: 'grade-details-test', inlineTemplate: true })
const code = transformSync(compiled.content, { configFile: false, babelrc: false, plugins: [require.resolve('@babel/plugin-transform-modules-commonjs')] }).code
const componentModule = { exports: {} }
vm.runInNewContext(code, { module: componentModule, exports: componentModule.exports, require: name => {
  if (name === '@/utils/overviewFields') return fields
  if (name === '@/components/PrivateAttachments.vue') return { default: Attachments, __esModule: true }
  return require(name)
} })
const StudentGradeDetails = componentModule.exports.default
const overviewTemplate = parse(readSource('views/StudentOverview.vue')).descriptor.template.content
const overviewRender = new Function('Vue', compile(overviewTemplate, { mode: 'function' }).code)(Vue)

function harness(isAdmin, student) {
  const bindings = loadBindings('views/StudentOverview.vue', ['gradeStudent', 'showGrades', 'detail', 'detailTitle', 'detailEntries', 'showDetail', 'attachmentValue'], { ref: Vue.ref, computed: Vue.computed })
  const context = Vue.reactive({
    ...bindings, ...layout, ...fields, calculateStudyGrade,
    isAdmin, loading: false, load() {}, grade: '', grades: [], studentIdKeyword: '', keyword: '', search() {},
    studentFields: [{ key: 'studentId', label: '学号', width: 130 }],
    students: [student], groups: layout.buildOverviewGroups([student]), slotLabel: String,
    studentField: (row, key) => row[key], page: 1, pageSize: 10, total: 1, totalColumns: 55,
    titleField: key => ({ papers: 'paperTitle', patents: 'patentTitle', projects: 'projectName', competitions: 'competitionName' })[key],
    appointmentDialog: false, appointment: {}, saving: false, uploading: false, changePageSize() {}, saveAppointment() {}
  })
  const render = async () => {
    const app = Vue.createSSRApp({ render: overviewRender, setup: () => context })
    app.config.warnHandler = () => {}
    for (const tag of new Set([...overviewTemplate.matchAll(/<(el-[\w-]+)/g)].map(match => match[1]))) {
      app.component(tag, tag === 'el-dialog' ? Dialog : { setup: (_props, { slots }) => () => Vue.h('div', slots.default?.()) })
    }
    app.component('StudentGradeDetails', StudentGradeDetails)
    app.component('PrivateAttachments', Attachments)
    return renderToString(app)
  }
  return { ...bindings, render }
}

const student = {
  id: 'db-a', studentId: '1001', name: '测试学生',
  grades: [
    { id: 'g1', course: '学位课程', semester: '2025-2026-1', courseType: 'degree', score: 80, credit: 2, status: 'passed', source: 'official' },
    { id: 'g2', course: '非学位课程', semester: '2025-2026-2', courseType: 'non_degree', score: 90, credit: 1, status: 'passed', source: 'student' },
    { id: 'g3', course: '未分类课程', semester: '2024-2025-1', score: 0, credit: 1, status: 'failed', attachments: '[{"url":"/SCSE@hbut/uploads/grade.pdf","name":"成绩证明.pdf"}]' },
    { id: 'g4', course: '待录入课程', status: 'not_entered' }
  ]
}

for (const isAdmin of [false, true]) {
  test(`${isAdmin ? 'admin' : 'student'} overview opens every course and its proof without changing the weighted score`, async () => {
    const h = harness(isAdmin, student)
    const closed = await h.render()
    assert.match(closed, /83\.00/)
    assert.match(closed, /查看全部课程（4）/)
    assert.doesNotMatch(closed, /2024-2025-1/)
    h.showGrades(student)
    const opened = await h.render()
    for (const text of ['学位课程', '非学位课程', '未分类课程', '待录入课程', '2025-2026-1', '2025-2026-2', '2024-2025-1', '学位课', '非学位课', '未分类', '未通过', '未录入', '成绩证明.pdf', '教务导入', '学生填写']) assert.ok(opened.includes(text), text)
    assert.match(opened, /<td>0<\/td>/)
    assert.match(opened, /83\.00/)
    assert.equal(student.grades.length, 4)
    assert.equal(student.grades[2].courseType, undefined)
  })

  test(`${isAdmin ? 'admin' : 'student'} task activity detail renders separate proof and structured fields together`, async () => {
    const proof = '[{"url":"/SCSE@hbut/uploads/task.pdf","name":"独立任务证明.pdf"}]'
    const activity = { id: 'submission:a', title: '讲座', content: '{"心得":"学习内容"}', submissionFields: { 心得: '学习内容' }, attachments: proof }
    const h = harness(isAdmin, { ...student, academic: [activity] })
    h.showDetail({ label: '活动' }, activity)
    const html = await h.render()
    assert.match(html, /独立任务证明.pdf/)
    assert.match(html, /学习内容/)
    assert.match(html, /证明材料/)
  })
}

test('empty or unclassified-only courses still open their detail and never depend on a calculated score', async () => {
  const h = harness(false, { ...student, grades: [] })
  h.showGrades({ ...student, grades: [] })
  assert.match(await h.render(), /暂无课程成绩/)
  const unclassified = { ...student, grades: [student.grades[2]] }
  const other = harness(false, unclassified)
  other.showGrades(unclassified)
  const html = await other.render()
  assert.match(html, /待完善/)
  assert.match(html, /未分类课程/)
})

test('profile appointments are read-only and coexist with separately editable appointment records', async () => {
  const profile = { ...student, workStatus: ' 班委、助管 ', appointments: [] }
  const own = harness(false, profile)
  const html = await own.render()
  assert.match(html, /班委、助管（个人资料）/)
  assert.doesNotMatch(html, />编辑</)
  assert.doesNotMatch(html, />删除</)
  const item = layout.getBonusDetails(profile).find(row => row.category === '任职')
  own.showDetail({ label: '任职' }, item.record)
  assert.match(await own.render(), /个人资料/)
  assert.equal(profile.appointments.length, 0)

  const detailed = { ...profile, appointments: [{ id: 'appointment-a', position: '负责人', organization: '学院' }] }
  const detailHtml = await harness(false, detailed).render()
  assert.match(detailHtml, /班委、助管（个人资料）/)
  assert.match(detailHtml, /负责人（学院）/)
  assert.match(detailHtml, />编辑</)
  assert.match(detailHtml, />删除</)
  const adminHtml = await harness(true, detailed).render()
  assert.match(adminHtml, /班委、助管（个人资料）/)
  assert.doesNotMatch(adminHtml, />编辑</)
  assert.doesNotMatch(adminHtml, />删除</)
  assert.equal(layout.getBonusDetails({ workStatus: '  ' }).length, 0)
})
