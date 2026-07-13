import { describe, expect, it } from 'vitest'
import {
  buildContentSecurityPolicy,
  SECURITY_HEADERS,
  SENSITIVE_RESPONSE_CACHE_CONTROL,
} from './security-headers'

describe('SECURITY_HEADERS — 공개 앱 브라우저 보안 기본선', () => {
  it('MIME 스니핑·프레임 삽입·과도한 referrer·불필요한 장치 권한을 차단한다', () => {
    expect(
      Object.fromEntries(SECURITY_HEADERS.map(({ key, value }) => [key, value])),
    ).toMatchObject({
      'X-Content-Type-Options': 'nosniff',
      'X-Frame-Options': 'DENY',
      'Referrer-Policy': 'strict-origin-when-cross-origin',
      'Permissions-Policy': 'camera=(), microphone=(), geolocation=()',
    })
  })

  it('정적 자산용 전역 헤더와 민감 응답 캐시 정책을 분리한다', () => {
    expect(
      Object.fromEntries(SECURITY_HEADERS.map(({ key, value }) => [key, value])),
    ).not.toHaveProperty('Cache-Control')
    expect(SENSITIVE_RESPONSE_CACHE_CONTROL).toBe('private, no-store, max-age=0')
  })

  it('요청 nonce로 스크립트를 제한하고 임의 외부 전송을 차단한다', () => {
    const headers = Object.fromEntries(SECURITY_HEADERS.map(({ key, value }) => [key, value]))
    const policy = buildContentSecurityPolicy('test-nonce', false)

    expect(policy).toContain("default-src 'self'")
    expect(policy).toContain("object-src 'none'")
    expect(policy).toContain("frame-ancestors 'none'")
    expect(policy).toContain("script-src 'self' 'nonce-test-nonce' 'strict-dynamic'")
    expect(policy).toContain("connect-src 'self'")
    expect(policy).not.toContain("script-src 'self' 'unsafe-inline'")
    expect(policy).not.toContain('https:')
    expect(policy).not.toContain('wss:')
    expect(headers['Strict-Transport-Security']).toBe('max-age=31536000; includeSubDomains')
  })

  it('개발 모드에서만 Next.js 개발 런타임 출처를 허용한다', () => {
    const policy = buildContentSecurityPolicy('dev-nonce', true)

    expect(policy).toContain("script-src 'self' 'nonce-dev-nonce' 'strict-dynamic' 'unsafe-eval'")
    expect(policy).toContain("connect-src 'self' ws: wss:")
  })
})
