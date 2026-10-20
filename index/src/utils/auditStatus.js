export function auditStatusMeta(status) {
  if (!status || ['pending', '待审核', '待审批', '未审核', '待审'].includes(status))
    return { text: '待审核', type: 'warning', icon: 'Clock' }
  if (['approved', '通过', '已通过', '已批准'].includes(status))
    return { text: '已通过', type: 'success', icon: 'CircleCheck' }
  if (['rejected', '拒绝', '已拒绝', '已驳回'].includes(status))
    return { text: '已驳回', type: 'danger', icon: 'CircleClose' }
  return { text: status, type: 'info', icon: '' }
}
