import { PERM } from './permissions'

const PUBLIC_NAVIGATION_PATHS = new Set(['/', '/approvals', '/analytics'])

const PATH_PERMISSION_RULES: ReadonlyArray<{
  prefix: string
  required: readonly string[]
}> = [
  {
    prefix: '/hr/employees',
    required: [
      PERM.HR_EMPLOYEE_READ,
      PERM.HR_DEPARTMENT_READ,
      PERM.HR_POSITION_READ,
      PERM.HR_JOBGRADE_READ,
    ],
  },
  {
    prefix: '/hr/contracts',
    required: [PERM.HR_EMPLOYEE_READ, PERM.HR_POSITION_READ, PERM.HR_JOBGRADE_READ],
  },
  {
    prefix: '/hr/leave-requests',
    required: [PERM.HR_LEAVE_READ, PERM.HR_EMPLOYEE_READ],
  },
  {
    prefix: '/hr/leave-balances',
    required: [PERM.HR_LEAVE_READ, PERM.HR_EMPLOYEE_READ],
  },
  { prefix: '/hr/departments', required: [PERM.HR_DEPARTMENT_READ] },
  { prefix: '/hr/positions', required: [PERM.HR_POSITION_READ] },
  { prefix: '/hr/job-grades', required: [PERM.HR_JOBGRADE_READ] },
  { prefix: '/hr/leave-policies', required: [PERM.HR_LEAVE_READ] },
  { prefix: '/finance', required: [PERM.FINANCE_READ] },
  { prefix: '/inventory', required: [PERM.INVENTORY_READ] },
  { prefix: '/crm', required: [PERM.CRM_READ] },
  { prefix: '/iam', required: [PERM.IAM_READ] },
  { prefix: '/audit', required: [PERM.AUDIT_READ] },
]

export function isNavigationPathVisible(path: string, permissions: ReadonlySet<string>): boolean {
  if (PUBLIC_NAVIGATION_PATHS.has(path)) return true
  const rule = PATH_PERMISSION_RULES.find(({ prefix }) => matchesPath(path, prefix))
  return rule?.required.every((permission) => permissions.has(permission)) ?? false
}

function matchesPath(path: string, prefix: string): boolean {
  return path === prefix || path.startsWith(`${prefix}/`)
}
