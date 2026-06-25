export type AuditAction = 'CREATE' | 'UPDATE' | 'DELETE' | 'VIEW' | 'APPROVE' | 'REJECT'

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
