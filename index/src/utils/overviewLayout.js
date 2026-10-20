// 一览表的列由当前页实际记录数决定；最低保留模板要求的编号栏位。
export const overviewGroups = [
  { key: 'A', label: 'A（30%）', summaries: [], sections: [{ key: 'grades', label: '学习成绩', minSlots: 0, columns: [] }] },
  {
    key: 'B', label: 'B（20%）', summaries: [], sections: [
      { key: 'papers', label: '论文', minSlots: 3, columns: [
        { key: 'paperTitle', label: '论文题目' }, { key: 'journalName', label: '期刊' },
        { key: 'journalLevel', label: '期刊级别' }, { key: 'firstThreeAuthors', label: '前三作者排序' },
        { key: 'scoreRatio', label: '得分比例' }, { key: 'paperScore', label: '得分' }
      ] },
      { key: 'patents', label: '专利', minSlots: 2, columns: [
        { key: 'patentTitle', label: '专利名称' }, { key: 'patentStatus', label: '专利阶段' },
        { key: 'inventorRank', label: '作者排序' }, { key: 'itemScore', label: '得分' }
      ] }
    ]
  },
  {
    key: 'C', label: 'C（35%）', summaries: [], sections: [
      { key: 'projects', label: '项目', minSlots: 2, columns: [
        { key: 'projectName', label: '项目名称' }, { key: 'projectLevel', label: '项目类别' },
        { key: 'ranking', label: '排名' }, { key: 'itemScore', label: '得分' }
      ] },
      { key: 'competitions', label: '竞赛', minSlots: 2, columns: [
        { key: 'competitionName', label: '竞赛名称' }, { key: 'awardLevel', label: '获奖级别' },
        { key: 'ranking', label: '排名' }, { key: 'itemScore', label: '得分' }
      ] }
    ]
  },
  { key: 'D', label: 'D（15%）', summaries: ['基础分', '加分项明细', 'D合计'], sections: [] }
]

export const getOverviewRecords = (student, sectionKey) => {
  if (sectionKey !== 'activities') return student[sectionKey] || []
  return [
    ...(student.academic || []).map(row => ({ ...row, overviewType: '学术活动' })),
    ...(student.daily || []).map(row => ({ ...row, overviewType: '日常活动' })),
    ...(student.volunteer || []).map(row => ({ ...row, overviewType: '志愿活动' }))
  ]
}

export const getBonusDetails = student => [
  ...getOverviewRecords(student, 'activities').map(record => ({ category: '活动', record, text: `${record.overviewType}：${record.title || record.serviceName || '未命名'}` })),
  ...getOverviewRecords(student, 'honors').map(record => ({ category: '荣誉', record, text: record.title || '未命名' })),
  ...getOverviewRecords(student, 'appointments').map(record => ({ category: '任职', record, text: `${record.position || '未命名'}${record.organization ? `（${record.organization}）` : ''}` }))
]

export const buildOverviewGroups = students => overviewGroups.map(group => {
  const sections = group.sections.map(section => ({
    ...section,
    slots: section.key === 'grades' ? 0 : Math.max(section.minSlots, ...students.map(student => getOverviewRecords(student, section.key).length))
  }))
  return {
    ...group, sections,
    width: group.summaries.length + sections.reduce((sum, section) => sum + (section.key === 'grades' ? 1 : section.slots * section.columns.length), 0)
  }
})

export const overviewValue = (record, key) => {
  if (key === 'itemScore') return '—' // 尚无评分规则，不把成绩或排名误当得分。
  if (key === 'scoreRatio' || key === 'paperScore') {
    const value = record[key] === null || record[key] === undefined || record[key] === '' ? '—' : record[key]
    return record.scorePending ? `${value}（待审核）` : value
  }
  if (key === 'firstThreeAuthors') {
    const rank = Number(record.authorRank)
    const authors = (record.coAuthors || '').split(/[,，、]/).map(name => name.trim()).filter(Boolean)
    if (!Number.isInteger(rank) || rank < 1 || !record.studentName || authors.length < Math.min(rank - 1, 3)) return '—'
    if (rank <= 3) authors.splice(rank - 1, 0, record.studentName)
    return authors.slice(0, 3).join('、')
  }
  return record[key]
}
