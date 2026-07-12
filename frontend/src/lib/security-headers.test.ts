import { describe, expect, it } from 'vitest'
import { SECURITY_HEADERS } from './security-headers'

describe('SECURITY_HEADERS — 공개 앱 브라우저 보안 기본선', () => {
  it('MIME 스니핑·프레임 삽입·과도한 referrer·불필요한 장치 권한을 차단한다', () => {
    expect(Object.fromEntries(SECURITY_HEADERS.map(({ key, value }) => [key, value]))).toMatchObject({
      'X-Content-Type-Options': 'nosniff',
      'X-Frame-Options': 'DENY',
      'Referrer-Policy': 'strict-origin-when-cross-origin',
      'Permissions-Policy': 'camera=(), microphone=(), geolocation=()',
    })
  })

  it('민감 응답이 공유 캐시에 저장되지 않도록 기본 캐시 정책을 둔다', () => {
    expect(SECURITY_HEADERS).toContainEqual({
      key: 'Cache-Control',
      value: 'private, no-store, max-age=0',
    })
  })

  it('실행 출처·객체 삽입을 제한하고 HTTPS 재접속을 강제한다', () => {
    const headers = Object.fromEntries(SECURITY_HEADERS.map(({ key, value }) => [key, value]))

    expect(headers['Content-Security-Policy']).toContain("default-src 'self'")
    expect(headers['Content-Security-Policy']).toContain("object-src 'none'")
    expect(headers['Content-Security-Policy']).toContain("frame-ancestors 'none'")
    expect(headers['Strict-Transport-Security']).toBe('max-age=31536000; includeSubDomains')
  })
})
