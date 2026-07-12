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
