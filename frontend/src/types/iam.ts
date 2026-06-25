// 백엔드 com.erp.common.security.dto.* 와 일치한다.

export type DataScope = 'ALL' | 'DEPARTMENT' | 'SELF'

export interface Role {
  id: number
  code: string
  name: string
  description: string | null
  permissions: string[]
}

export interface AccessProfile {
  userId: string
  dataScope: DataScope
  departmentId: number | null
  approvalLimit: number | null
}
