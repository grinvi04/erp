export const INTERNAL_ACCESS_TOKEN_HEADER = 'x-erp-internal-access-token'
export const INTERNAL_SESSION_HEADER = 'x-erp-internal-session'

export function toPublicSession<T extends object>(
  session: T,
  token: { tenantId?: string; error?: string },
): T & { tenantId: string; error?: string } {
  return {
    ...session,
    tenantId: token.tenantId ?? '',
    error: token.error,
  }
}

export function toServerSession<T extends object>(
  session: T,
  token: { tenantId?: string; error?: string; accessToken?: string },
): ReturnType<typeof toPublicSession<T>> & { serverAccessToken?: string } {
  return {
    ...toPublicSession(session, token),
    serverAccessToken: token.accessToken,
  }
}

export function toBrowserSession(value: unknown): unknown {
  if (!value || typeof value !== 'object' || Array.isArray(value)) return value
  const publicValue = { ...(value as Record<string, unknown>) }
  delete publicValue.serverAccessToken
  return publicValue
}

export function encodeInternalSession(value: unknown): string {
  return encodeURIComponent(JSON.stringify(toBrowserSession(value)))
}

export function decodeInternalSession<T>(value: string | null): T | null {
  if (!value) return null
  try {
    const parsed: unknown = JSON.parse(decodeURIComponent(value))
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? (parsed as T) : null
  } catch {
    return null
  }
}
