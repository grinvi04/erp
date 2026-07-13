import type { JWT } from 'next-auth/jwt'
import { describe, expect, it, vi } from 'vitest'
import { resolveServerAccessToken } from './server-access-token'

function token(overrides: Partial<JWT> = {}): JWT {
  return {
    accessToken: 'current-token',
    refreshToken: 'refresh-token',
    accessTokenExpires: 200_000,
    tenantId: '1',
    ...overrides,
  }
}

describe('resolveServerAccessToken', () => {
  it('유효한 토큰은 갱신 요청 없이 반환한다', async () => {
    const refresh = vi.fn()

    await expect(resolveServerAccessToken(token(), refresh, 100_000)).resolves.toBe('current-token')
    expect(refresh).not.toHaveBeenCalled()
  })

  it('만료가 임박한 토큰은 현재 요청에서 먼저 갱신한다', async () => {
    const current = token({ accessTokenExpires: 120_000 })
    const refresh = vi.fn().mockResolvedValue(token({ accessToken: 'fresh-token' }))

    await expect(resolveServerAccessToken(current, refresh, 100_000)).resolves.toBe('fresh-token')
    expect(refresh).toHaveBeenCalledWith(current)
  })

  it('갱신 실패 토큰을 백엔드에 전달하지 않는다', async () => {
    const refresh = vi.fn().mockResolvedValue(token({ error: 'RefreshAccessTokenError' }))

    await expect(
      resolveServerAccessToken(token({ accessTokenExpires: 0 }), refresh, 100_000),
    ).resolves.toBeNull()
  })
})
