import { describe, expect, it } from 'vitest'
import type { TenantUser } from '@/types/iam'
import { statusLabel, upsertTenantUser } from './tenant-user-model'

const active: TenantUser = {
  id: 2,
  email: 'admin@example.com',
  userId: 'user-2',
  status: 'ACTIVE',
  failureCode: null,
}

describe('tenant user view model', () => {
  it('labels every backend lifecycle status', () => {
    expect(statusLabel('PENDING')).toBe('초대 처리 중')
    expect(statusLabel('ACTIVE')).toBe('사용 중')
    expect(statusLabel('FAILED')).toBe('초대 실패')
    expect(statusLabel('DISABLED')).toBe('사용 중지')
  })

  it('replaces an updated user and keeps email ordering', () => {
    const result = upsertTenantUser(
      [
        { ...active, id: 1, email: 'z@example.com' },
        { ...active, status: 'DISABLED' },
      ],
      active,
    )

    expect(result.map((user) => `${user.email}:${user.status}`)).toEqual([
      'admin@example.com:ACTIVE',
      'z@example.com:ACTIVE',
    ])
  })

  it('adds a newly invited user in email order', () => {
    const result = upsertTenantUser([{ ...active, id: 3, email: 'z@example.com' }], active)

    expect(result.map((user) => user.email)).toEqual(['admin@example.com', 'z@example.com'])
  })
})
