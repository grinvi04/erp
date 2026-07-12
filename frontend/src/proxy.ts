import { NextResponse } from 'next/server'
import { auth } from '@/lib/auth'
import { buildContentSecurityPolicy } from '@/lib/security-headers'

export const proxy = auth((request) => {
  const nonce = btoa(crypto.randomUUID())
  const contentSecurityPolicy = buildContentSecurityPolicy(
    nonce,
    process.env.NODE_ENV === 'development',
  )
  const requestHeaders = new Headers(request.headers)
  requestHeaders.set('Content-Security-Policy', contentSecurityPolicy)
  requestHeaders.set('x-nonce', nonce)

  const response = NextResponse.next({ request: { headers: requestHeaders } })
  response.headers.set('Content-Security-Policy', contentSecurityPolicy)
  return response
})

export const config = {
  matcher: ['/((?!api/auth|_next/static|_next/image|favicon.ico|.*\\.png$).*)'],
}
