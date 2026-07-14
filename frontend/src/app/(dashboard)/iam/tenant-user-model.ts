import type { TenantUser, TenantUserStatus } from '@/types/iam'

const STATUS_LABELS: Record<TenantUserStatus, string> = {
  PENDING: '초대 처리 중',
  ACTIVE: '사용 중',
  FAILED: '초대 실패',
  DISABLED: '사용 중지',
}

export function statusLabel(status: TenantUserStatus): string {
  return STATUS_LABELS[status]
}

export function upsertTenantUser(users: TenantUser[], updated: TenantUser): TenantUser[] {
  const next = users.filter((user) => user.id !== updated.id)
  next.push(updated)
  return next.sort((left, right) => left.email.localeCompare(right.email))
}
