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

// 백엔드 com.erp.common.security.dto.UserLookupResponse 와 일치한다.
// known=false면 IAM이 아는 흔적(감사·역할·프로파일)이 없는 sub — 유령 sub 무단 배정 경고용.
export interface UserLookup {
  userId: string
  known: boolean
  roleCount: number
  hasAccessProfile: boolean
  audited: boolean
}
