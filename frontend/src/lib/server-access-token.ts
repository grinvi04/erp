import type { JWT } from 'next-auth/jwt'

type TokenRefresher = (token: JWT) => Promise<JWT>

export async function resolveServerAccessToken(
  token: JWT,
  refresh: TokenRefresher,
  now = Date.now(),
): Promise<string | null> {
  if (token.error === 'RefreshAccessTokenError') return null

  if (
    typeof token.accessToken === 'string' &&
    typeof token.accessTokenExpires === 'number' &&
    now < token.accessTokenExpires - 60_000
  ) {
    return token.accessToken
  }

  if (typeof token.refreshToken !== 'string') return null
  const refreshed = await refresh(token)
  if (refreshed.error === 'RefreshAccessTokenError') return null
  return typeof refreshed.accessToken === 'string' ? refreshed.accessToken : null
}
