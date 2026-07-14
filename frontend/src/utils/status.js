const ITEM_STATUS_MAP = {
  PENDING: '待审核',
  OPEN: '开放中',
  MATCHED: '已匹配',
  CLOSED: '已关闭',
  REJECTED: '已驳回',
  DELETED: '已删除'
}

const CLAIM_STATUS_MAP = {
  PENDING: '待审核',
  APPROVED: '已通过',
  REJECTED: '已驳回'
}

const ITEM_TYPE_MAP = {
  LOST: '寻物启事',
  FOUND: '招领启事'
}

export function itemStatusText(status) {
  return ITEM_STATUS_MAP[status] || status || '-'
}

export function claimStatusText(status) {
  return CLAIM_STATUS_MAP[status] || status || '-'
}

export function itemTypeText(type) {
  return ITEM_TYPE_MAP[type] || type || '-'
}

export function tagTypeForItemStatus(status) {
  const map = {
    PENDING: 'warning',
    OPEN: 'success',
    MATCHED: 'warning',
    CLOSED: 'info',
    REJECTED: 'danger',
    DELETED: 'info'
  }
  return map[status] || 'info'
}

export function tagTypeForClaimStatus(status) {
  const map = {
    PENDING: 'warning',
    APPROVED: 'success',
    REJECTED: 'danger'
  }
  return map[status] || 'info'
}

export function tagTypeForItemType(type) {
  const map = {
    LOST: 'danger',
    FOUND: 'primary'
  }
  return map[type] || 'info'
}
