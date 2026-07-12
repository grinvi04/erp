import { describe, expect, it } from 'vitest'
import { toPublicSession } from './auth-session'

describe('public auth session boundary', () => {
  it('does not expose the backend bearer token to browser session JSON', () => {
    const privateToken = {
      tenantId: '7',
      error: undefined,
      accessToken: 'secret-bearer-token',
    }
    const result = toPublicSession({ user: { name: '사용자' } }, privateToken)

    expect(result).toEqual({ user: { name: '사용자' }, tenantId: '7', error: undefined })
    expect(result).not.toHaveProperty('accessToken')
  })
})
