import 'server-only'
import NextAuth from 'next-auth'
import Keycloak from 'next-auth/providers/keycloak'
import type { Session } from 'next-auth'
import type { JWT } from 'next-auth/jwt'
import { getToken } from 'next-auth/jwt'
import { headers } from 'next/headers'
import { toPublicSession } from '@/lib/auth-session'
import { resolveServerAccessToken } from '@/lib/server-access-token'

declare module 'next-auth' {
  interface Session {
    tenantId: string
    error?: string
  }
}

declare module 'next-auth/jwt' {
  interface JWT {
    accessToken: string
    refreshToken: string
    accessTokenExpires: number
    tenantId: string
    error?: string
  }
}

async function refreshAccessToken(token: JWT): Promise<JWT> {
  try {
    const url = `${process.env.KEYCLOAK_ISSUER}/protocol/openid-connect/token`
    const response = await fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: new URLSearchParams({
        client_id: process.env.AUTH_KEYCLOAK_ID!,
        client_secret: process.env.AUTH_KEYCLOAK_SECRET!,
        grant_type: 'refresh_token',
        refresh_token: token.refreshToken,
      }),
    })
    const refreshed = await response.json()
    if (!response.ok) throw refreshed
    return {
      ...token,
      accessToken: refreshed.access_token,
      refreshToken: refreshed.refresh_token ?? token.refreshToken,
      accessTokenExpires: Date.now() + refreshed.expires_in * 1000,
    }
  } catch {
    return { ...token, error: 'RefreshAccessTokenError' }
  }
}

export async function getServerAccessToken(): Promise<string | null> {
  const requestHeaders = await headers()
  const secret = process.env.AUTH_SECRET
  if (!secret) return null

  for (const cookieName of ['__Secure-authjs.session-token', 'authjs.session-token']) {
    const token = await getToken({
      req: { headers: requestHeaders },
      secret,
      cookieName,
      salt: cookieName,
      secureCookie: cookieName.startsWith('__Secure-'),
    })
    if (token) return resolveServerAccessToken(token, refreshAccessToken)
  }
  return null
}

export const { handlers, auth, signIn, signOut } = NextAuth({
  providers: [
    Keycloak({
      clientId: process.env.AUTH_KEYCLOAK_ID!,
      clientSecret: process.env.AUTH_KEYCLOAK_SECRET!,
      issuer: process.env.KEYCLOAK_ISSUER!,
    }),
  ],
  callbacks: {
    async jwt({ token, account }) {
      if (account) {
        const payload = JSON.parse(
          Buffer.from(account.access_token!.split('.')[1], 'base64').toString(),
        )
        return {
          ...token,
          accessToken: account.access_token!,
          refreshToken: account.refresh_token!,
          accessTokenExpires: (account.expires_at ?? 0) * 1000,
          tenantId: payload.tenant_id ?? payload.org_id ?? '',
        }
      }
      if (token.error === 'RefreshAccessTokenError') {
        return token
      }
      if (Date.now() < (token.accessTokenExpires ?? 0) - 60_000) {
        return token
      }
      return refreshAccessToken(token)
    },
    async session({ session, token }): Promise<Session> {
      return toPublicSession(session, token)
    },
  },
  pages: { signIn: '/login' },
})
