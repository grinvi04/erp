import { NextResponse } from 'next/server'
import { auth } from '@/lib/auth'
import { INTERNAL_ACCESS_TOKEN_HEADER } from '@/lib/auth-session'
import {
  buildContentSecurityPolicy,
  SENSITIVE_RESPONSE_CACHE_CONTROL,
} from '@/lib/security-headers'

export const proxy = auth((request) => {
  const nonce = btoa(crypto.randomUUID())
  const contentSecurityPolicy = buildContentSecurityPolicy(
    nonce,
    process.env.NODE_ENV === 'development',
  )
  const requestHeaders = new Headers(request.headers)
  requestHeaders.set('Content-Security-Policy', contentSecurityPolicy)
  requestHeaders.set('x-nonce', nonce)
  if (request.auth?.serverAccessToken) {
    requestHeaders.set(INTERNAL_ACCESS_TOKEN_HEADER, request.auth.serverAccessToken)
  } else {
    requestHeaders.delete(INTERNAL_ACCESS_TOKEN_HEADER)
  }

  const response = NextResponse.next({ request: { headers: requestHeaders } })
  response.headers.set('Content-Security-Policy', contentSecurityPolicy)
  response.headers.set('Cache-Control', SENSITIVE_RESPONSE_CACHE_CONTROL)
  return response
})

export const config = {
  matcher: ['/((?!api/auth|_next/static|_next/image|favicon.ico|.*\\.png$).*)'],
}
