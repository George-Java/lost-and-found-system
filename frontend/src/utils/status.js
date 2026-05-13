const ITEM_STATUS_MAP = {
  OPEN: '开放中',
  MATCHED: '已匹配',
  CLOSED: '已关闭'
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
