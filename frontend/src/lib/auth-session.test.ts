import { describe, expect, it } from 'vitest'
import {
  decodeInternalSession,
  encodeInternalSession,
  toBrowserSession,
  toPublicSession,
  toServerSession,
} from './auth-session'

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

  it('passes the refreshed bearer token internally and strips it from browser session JSON', () => {
    const result = toServerSession(
      { user: { name: '사용자' } },
      { tenantId: '7', accessToken: 'fresh-secret-token' },
    )

    expect(result.serverAccessToken).toBe('fresh-secret-token')
    expect(toBrowserSession(result)).toEqual({
      user: { name: '사용자' },
      tenantId: '7',
      error: undefined,
    })
    expect(JSON.stringify(toBrowserSession(result))).not.toContain('fresh-secret-token')
  })

  it('passes only the public session through the internal request header', () => {
    const encoded = encodeInternalSession({
      user: { name: '사용자' },
      tenantId: '7',
      serverAccessToken: 'secret-token',
    })

    expect(decodeInternalSession(encoded)).toEqual({ user: { name: '사용자' }, tenantId: '7' })
    expect(encoded).not.toContain('secret-token')
    expect(decodeInternalSession('%invalid')).toBeNull()
  })
})
