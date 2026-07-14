import { describe, expect, it } from 'vitest'

import { PERM } from './permissions'

type NavigationAccessPolicy = {
  isNavigationPathVisible: (path: string, permissions: ReadonlySet<string>) => boolean
}

async function loadNavigationAccessPolicy(): Promise<NavigationAccessPolicy | null> {
  const modulePath: string = './navigation-access'
  try {
    return (await import(modulePath)) as NavigationAccessPolicy
  } catch {
    return null
  }
}

describe('protected navigation access policy', () => {
  it.each([
    PERM.HR_EMPLOYEE_READ,
    PERM.HR_DEPARTMENT_READ,
    PERM.HR_LEAVE_READ,
    PERM.HR_POSITION_READ,
    PERM.HR_JOBGRADE_READ,
  ])('shows HR paths with any HR read permission (%s)', async (permission) => {
    const policy = await loadNavigationAccessPolicy()

    expect(policy).not.toBeNull()
    expect(policy?.isNavigationPathVisible('/hr/employees', new Set([permission]))).toBe(true)
  })

  it.each([
    ['finance', '/finance/vat-return', PERM.FINANCE_READ],
    ['inventory', '/inventory/items', PERM.INVENTORY_READ],
    ['CRM', '/crm/accounts', PERM.CRM_READ],
    ['IAM', '/iam', PERM.IAM_READ],
    ['audit', '/audit', PERM.AUDIT_READ],
  ])('shows the %s path only with its read permission', async (_label, path, permission) => {
    const policy = await loadNavigationAccessPolicy()

    expect(policy).not.toBeNull()
    expect(policy?.isNavigationPathVisible(path, new Set([permission]))).toBe(true)
    expect(policy?.isNavigationPathVisible(path, new Set())).toBe(false)
  })

  it('shows no protected paths without permissions', async () => {
    const policy = await loadNavigationAccessPolicy()
    const protectedPaths = [
      '/hr/employees',
      '/finance/vat-return',
      '/inventory/items',
      '/crm/accounts',
      '/iam',
      '/audit',
    ]

    expect(policy).not.toBeNull()
    expect(
      protectedPaths.filter((path) => policy?.isNavigationPathVisible(path, new Set())),
    ).toEqual([])
  })
})
