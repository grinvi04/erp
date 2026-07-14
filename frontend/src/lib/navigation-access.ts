import { PERM } from './permissions'

const HR_READ_PERMISSIONS = [
  PERM.HR_EMPLOYEE_READ,
  PERM.HR_DEPARTMENT_READ,
  PERM.HR_LEAVE_READ,
  PERM.HR_POSITION_READ,
  PERM.HR_JOBGRADE_READ,
] as const

export function isNavigationPathVisible(path: string, permissions: ReadonlySet<string>): boolean {
  if (matchesPath(path, '/hr')) {
    return HR_READ_PERMISSIONS.some((permission) => permissions.has(permission))
  }
  if (matchesPath(path, '/finance')) return permissions.has(PERM.FINANCE_READ)
  if (matchesPath(path, '/inventory')) return permissions.has(PERM.INVENTORY_READ)
  if (matchesPath(path, '/crm')) return permissions.has(PERM.CRM_READ)
  if (matchesPath(path, '/iam')) return permissions.has(PERM.IAM_READ)
  if (matchesPath(path, '/audit')) return permissions.has(PERM.AUDIT_READ)
  return true
}

function matchesPath(path: string, prefix: string): boolean {
  return path === prefix || path.startsWith(`${prefix}/`)
}
