import { apiGet } from '@/lib/api'

// 기능 권한 코드 — 백엔드 com.erp.common.security.Permission 과 일치해야 한다.
export const PERM = {
  HR_EMPLOYEE_READ: 'hr:employee:read',
  HR_EMPLOYEE_WRITE: 'hr:employee:write',
  HR_DEPARTMENT_READ: 'hr:department:read',
  HR_DEPARTMENT_WRITE: 'hr:department:write',
  HR_LEAVE_READ: 'hr:leave:read',
  HR_LEAVE_WRITE: 'hr:leave:write',
  HR_POSITION_READ: 'hr:position:read',
  HR_POSITION_WRITE: 'hr:position:write',
  HR_JOBGRADE_READ: 'hr:jobgrade:read',
  HR_JOBGRADE_WRITE: 'hr:jobgrade:write',
  FINANCE_READ: 'finance:read',
  FINANCE_WRITE: 'finance:write',
  INVENTORY_READ: 'inventory:read',
  INVENTORY_WRITE: 'inventory:write',
  CRM_READ: 'crm:read',
  CRM_WRITE: 'crm:write',
  AUDIT_READ: 'audit:read',
} as const

/** 현재 사용자의 권한 코드 목록(UI 게이팅용). 서버 검사가 항상 최종이다. */
export async function getMyPermissions(): Promise<string[]> {
  try {
    return await apiGet<string[]>('/api/me/permissions')
  } catch {
    return []
  }
}
