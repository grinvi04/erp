import { describe, expect, it } from 'vitest'

import { isNavigationPathVisible } from './navigation-access'
import { PERM } from './permissions'

describe('protected navigation access policy', () => {
  it.each([
    [
      '/hr/employees',
      [
        PERM.HR_EMPLOYEE_READ,
        PERM.HR_DEPARTMENT_READ,
        PERM.HR_POSITION_READ,
        PERM.HR_JOBGRADE_READ,
      ],
    ],
    ['/hr/contracts', [PERM.HR_EMPLOYEE_READ, PERM.HR_POSITION_READ, PERM.HR_JOBGRADE_READ]],
    ['/hr/departments', [PERM.HR_DEPARTMENT_READ]],
    ['/hr/positions', [PERM.HR_POSITION_READ]],
    ['/hr/job-grades', [PERM.HR_JOBGRADE_READ]],
    ['/hr/leave-requests', [PERM.HR_LEAVE_READ, PERM.HR_EMPLOYEE_READ]],
    ['/hr/leave-policies', [PERM.HR_LEAVE_READ]],
    ['/hr/leave-balances', [PERM.HR_LEAVE_READ, PERM.HR_EMPLOYEE_READ]],
  ])('requires every permission used by %s', (path, required) => {
    expect(isNavigationPathVisible(path, new Set(required))).toBe(true)
    for (const missing of required) {
      expect(
        isNavigationPathVisible(path, new Set(required.filter((item) => item !== missing))),
      ).toBe(false)
    }
  })

  it.each([
    ['finance', '/finance/vat-return', PERM.FINANCE_READ],
    ['inventory', '/inventory/items', PERM.INVENTORY_READ],
    ['CRM', '/crm/accounts', PERM.CRM_READ],
    ['사용자·권한', '/iam', PERM.IAM_READ],
    ['audit', '/audit', PERM.AUDIT_READ],
  ])('shows the %s path only with its read permission', (_label, path, permission) => {
    expect(isNavigationPathVisible(path, new Set([permission]))).toBe(true)
    expect(isNavigationPathVisible(path, new Set())).toBe(false)
  })

  it('shows no protected paths without permissions', () => {
    const protectedPaths = [
      '/hr/employees',
      '/finance/vat-return',
      '/inventory/items',
      '/crm/accounts',
      '/iam',
      '/audit',
    ]

    expect(protectedPaths.filter((path) => isNavigationPathVisible(path, new Set()))).toEqual([])
  })

  it('allows only explicitly public top-level paths by default', () => {
    for (const path of ['/', '/approvals', '/analytics']) {
      expect(isNavigationPathVisible(path, new Set())).toBe(true)
    }
    expect(isNavigationPathVisible('/unknown-protected-route', new Set())).toBe(false)
  })
})
