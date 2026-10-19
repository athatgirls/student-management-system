// Only classified courses with valid scores and positive credits take part in X and Y.
export const calculateStudyGrade = records => {
  const totals = {
    degree: { weighted: 0, credits: 0 },
    non_degree: { weighted: 0, credits: 0 }
  }
  for (const record of records || []) {
    const group = totals[record.courseType]
    const score = Number(record.score)
    const credit = Number(record.credit)
    if (!group || record.score === null || record.score === undefined || record.credit === null || record.credit === undefined ||
        !Number.isFinite(score) || score < 0 || score > 100 || !Number.isFinite(credit) || credit <= 0) continue
    group.weighted += score * credit
    group.credits += credit
  }
  if (!totals.degree.credits || !totals.non_degree.credits) return null
  const x = totals.degree.weighted / totals.degree.credits
  const y = totals.non_degree.weighted / totals.non_degree.credits
  return { x, y, score: x * 0.7 + y * 0.3 }
}
