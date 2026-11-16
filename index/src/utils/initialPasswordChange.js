// An interrupted first-login flow starts with a fresh login after a reload.
let pendingChange

export function setPendingPasswordChange(change) {
  pendingChange = change
}

export function getPendingPasswordChange() {
  return pendingChange
}

export function clearPendingPasswordChange() {
  pendingChange = undefined
}
