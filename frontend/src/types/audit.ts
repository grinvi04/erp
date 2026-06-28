// 백엔드 com.erp.common.audit.AuditLog.AuditAction 와 일치한다.
export type AuditAction =
  'CREATE' | 'UPDATE' | 'DELETE' | 'VIEW' | 'APPROVE' | 'REJECT' | 'WITHDRAW' | 'REVERSE'

export const AUDIT_ACTIONS: AuditAction[] = [
  'CREATE',
  'UPDATE',
  'DELETE',
  'VIEW',
  'APPROVE',
  'REJECT',
  'WITHDRAW',
  'REVERSE',
]

// 백엔드 com.erp.common.audit.AuditLogResponse 와 일치한다.
export interface AuditLog {
  id: number
  entityType: string
  entityId: number
  action: AuditAction
  performedBy: string
  performedAt: string
  ipAddress: string | null
}

// 백엔드 com.erp.common.audit.AuditLogDetailResponse 와 일치한다 — 변경 내역(before/after) 포함.
export interface AuditLogDetail extends AuditLog {
  beforeData: string | null
  afterData: string | null
}

// 감사 로그 조회 필터(전부 선택). 백엔드 /api/audit/logs 쿼리 파라미터와 대응한다.
export interface AuditFilters {
  entityType: string
  action: string
  performedBy: string
  from: string // yyyy-MM-dd (없으면 '')
  to: string // yyyy-MM-dd (없으면 '')
}
