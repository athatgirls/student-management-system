// Legacy daily tasks stay visible while the new routine category gets its own publication option.
export const taskMatchesTab = (task, tab) => {
  const category = task.activityCategory || 'daily'
  return tab === 'tasks' ? ['daily', 'routine'].includes(category) : category === tab
}
