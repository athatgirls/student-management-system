export const overviewSections = [
  { key: 'grades', label: '成绩', title: 'course', columns: ['semester', 'course', 'score', 'credit', 'status'] },
  { key: 'papers', label: '论文', title: 'paperTitle', columns: ['paperTitle', 'journalName', 'journalLevel', 'authorRank', 'publicationStatus'] },
  { key: 'competitions', label: '竞赛', title: 'competitionName', columns: ['competitionName', 'competitionLevel', 'awardLevel', 'ranking', 'awardDate'] },
  { key: 'patents', label: '专利', title: 'patentTitle', columns: ['patentTitle', 'patentType', 'patentNumber', 'inventorRank', 'patentStatus'] },
  { key: 'projects', label: '项目', title: 'projectName', columns: ['projectName', 'projectLevel', 'ranking', 'startDate', 'endDate'] },
  { key: 'academic', label: '学术活动', title: 'title', columns: ['title', 'organizer', 'startDate', 'endDate', 'auditStatus'] },
  { key: 'daily', label: '日常活动', title: 'title', columns: ['title', 'level', 'startDate', 'endDate', 'auditStatus'] },
  { key: 'volunteer', label: '志愿活动', title: 'serviceName', columns: ['serviceName', 'serviceType', 'serviceHours', 'serviceDate', 'auditStatus'] },
  { key: 'honors', label: '荣誉', title: 'title', columns: ['title', 'category', 'level', 'awardOrg', 'awardDate'] },
  { key: 'appointments', label: '任职', title: 'position', columns: ['position', 'organization', 'startDate', 'endDate', 'description'] }
]
export const fieldLabels = {
  semester: '学期', course: '课程名称', score: '成绩', credit: '学分', courseType: '课程类别', status: '状态', source: '来源',
  paperTitle: '论文题目', journalName: '会议/期刊名称', journalLevel: '会议/期刊级别', authorRank: '作者排名', coAuthors: '合作作者', doi: 'DOI号',
  publicationStatus: '发表状态', publicationDate: '见刊时间', acceptanceDate: '接收时间',
  competitionName: '竞赛名称', competitionLevel: '竞赛级别', awardLevel: '获奖等级', ranking: '排名', awardDate: '获奖日期', teamMembers: '团队成员', instructorName: '指导老师',
  patentTitle: '专利名称', patentType: '专利类型', patentNumber: '专利号', inventorRank: '发明人排名', patentStatus: '专利状态', authorizationDate: '授权日期', applicationDate: '受理日期', coInventors: '合作发明人',
  projectName: '项目名称', projectLevel: '项目级别', startDate: '开始日期', endDate: '结束日期',
  title: '名称', organizer: '主办单位', location: '地点', description: '说明', content: '内容', level: '级别', category: '类别', awardOrg: '授予单位', tags: '标签', type: '活动类型', activityType: '活动类型', speaker: '主讲人', isPublic: '公开展示',
  position: '职务名称', organization: '任职/服务单位', serviceName: '服务名称', serviceType: '服务类型', serviceHours: '服务时长', serviceDate: '服务日期', serviceLocation: '服务地点', serviceDescription: '服务描述', contactPerson: '联系人', contactPhone: '联系电话',
  auditStatus: '审核状态', auditComment: '审核意见', auditorName: '审核人', auditTime: '审核时间', createTime: '创建时间', updateTime: '更新时间', remark: '备注', studentId: '学号', studentName: '姓名', submissionTime: '提交时间', deadline: '截止时间', taskCategory: '任务类型'
}
export const displayValue = (key, value) => {
  if (value === null || value === undefined || value === '') return '-'
  if (typeof value === 'boolean') return value ? '是' : '否'
  if (key.endsWith('Date') && typeof value === 'number') return new Date(value).toLocaleDateString('zh-CN')
  if (['createTime', 'updateTime', 'auditTime', 'submissionTime', 'deadline'].includes(key)) return new Date(value).toLocaleString('zh-CN')
  if (Array.isArray(value)) return value.join('、')
  if (typeof value === 'object') return JSON.stringify(value)
  return ({ degree: '学位课', non_degree: '非学位课', pending: '待审核', approved: '已通过', rejected: '已驳回', passed: '已通过', failed: '未通过', not_entered: '未录入', student: '学生填写', official: '教务导入', profile: '个人资料', completed: '已完成', submitted: '已提交', task: '日常任务', normal: '普通任务', registration: '报名活动' })[value] || value
}
