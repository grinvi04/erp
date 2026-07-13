import 'server-only'
import NextAuth from 'next-auth'
import Keycloak from 'next-auth/providers/keycloak'
import type { Session } from 'next-auth'
import type { JWT } from 'next-auth/jwt'
import { getToken } from 'next-auth/jwt'
import { headers } from 'next/headers'
import { cache } from 'react'
import { INTERNAL_ACCESS_TOKEN_HEADER, toBrowserSession, toServerSession } from '@/lib/auth-session'
import { resolveServerAccessToken } from '@/lib/server-access-token'

declare module 'next-auth' {
  interface Session {
    tenantId: string
    error?: string
    serverAccessToken?: string
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

async function readServerAccessToken(): Promise<string | null> {
  const requestHeaders = await headers()
  const internalToken = requestHeaders.get(INTERNAL_ACCESS_TOKEN_HEADER)
  if (internalToken) return internalToken

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

export const getServerAccessToken = cache(readServerAccessToken)

const nextAuth = NextAuth({
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
      return toServerSession(session, token)
    },
  },
  pages: { signIn: '/login' },
})

export const { auth, signIn, signOut } = nextAuth

async function sanitizePublicSessionResponse(request: Request, response: Response) {
  if (!new URL(request.url).pathname.endsWith('/session')) return response
  if (!response.headers.get('content-type')?.includes('application/json')) return response

  const headers = new Headers(response.headers)
  headers.delete('content-length')
  return new Response(JSON.stringify(toBrowserSession(await response.json())), {
    status: response.status,
    statusText: response.statusText,
    headers,
  })
}

export const handlers = {
  GET: async (...args: Parameters<typeof nextAuth.handlers.GET>) =>
    sanitizePublicSessionResponse(args[0], await nextAuth.handlers.GET(...args)),
  POST: async (...args: Parameters<typeof nextAuth.handlers.POST>) =>
    sanitizePublicSessionResponse(args[0], await nextAuth.handlers.POST(...args)),
}
