import { test as setup } from '@playwright/test'
import { encode } from '@auth/core/jwt'
import fs from 'node:fs'
import path from 'node:path'

// 백엔드 통합 E2E 전용 세션 셋업 — auth.setup.ts(더미 토큰)와 달리, 실제 Keycloak에서
// password grant로 진짜 accessToken을 받아 next-auth 세션 쿠키를 만든다. 그 쿠키를 들고
// 실 백엔드(JWT 검증·RBAC·테넌트 필터)를 통과하는 데이터 렌더를 검증한다.
//
// 전제: docker compose 인프라 + 백엔드 + 프론트 dev 서버(localhost:3000)가 떠 있어야 한다.
// 모든 자격증명은 env에서 읽는다(하드코딩·커밋 금지). 누락 시 명확히 실패시킨다.
//   E2E_KC_ISSUER     기본 http://localhost:8180/realms/erp
//   E2E_CLIENT_ID     기본 erp-frontend
//   E2E_CLIENT_SECRET (필수) Keycloak 클라이언트 시크릿
//   E2E_USERNAME      기본 admin
//   E2E_PASSWORD      (필수) 로그인 비밀번호
//   AUTH_SECRET       (필수) 실행 중인 dev 서버의 next-auth 시크릿과 동일해야 쿠키가 유효

const COOKIE_NAME = 'authjs.session-token' // http(비-secure) dev 서버 기본 쿠키명
const AUTH_FILE = path.join('e2e', '.auth', 'backend.json')

function requireEnv(name: string): string {
  const v = process.env[name]
  if (!v) {
    throw new Error(
      `[backend.setup] 환경변수 ${name} 가 필요합니다. 실스택 통합 E2E는 자격증명을 env로 주입하세요.`
    )
  }
  return v
}

/** JWT payload의 tenant_id 클레임을 추출한다. */
function tenantIdFromToken(accessToken: string): string {
  const payloadB64 = accessToken.split('.')[1] ?? ''
  const json = Buffer.from(payloadB64, 'base64').toString('utf8')
  const claims = JSON.parse(json) as { tenant_id?: string | number }
  if (claims.tenant_id == null) {
    throw new Error('[backend.setup] accessToken에 tenant_id 클레임이 없습니다.')
  }
  return String(claims.tenant_id)
}

setup('authenticate (real Keycloak)', async ({ context }) => {
  const issuer = process.env.E2E_KC_ISSUER ?? 'http://localhost:8180/realms/erp'
  const clientId = process.env.E2E_CLIENT_ID ?? 'erp-frontend'
  const clientSecret = requireEnv('E2E_CLIENT_SECRET')
  const username = process.env.E2E_USERNAME ?? 'admin'
  const password = requireEnv('E2E_PASSWORD')
  const authSecret = requireEnv('AUTH_SECRET')

  // Keycloak password grant — 실제 accessToken 획득.
  const res = await fetch(`${issuer}/protocol/openid-connect/token`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({
      grant_type: 'password',
      client_id: clientId,
      client_secret: clientSecret,
      username,
      password,
      scope: 'openid',
    }),
  })
  if (!res.ok) {
    throw new Error(
      `[backend.setup] Keycloak password grant 실패: HTTP ${res.status} ${await res.text()}`
    )
  }
  const tokens = (await res.json()) as {
    access_token: string
    refresh_token?: string
    expires_in?: number
  }

  const oneDay = 24 * 60 * 60
  // auth.ts의 jwt 콜백이 통과시키는 토큰 형태와 동일하게 구성하되, accessToken은 실제 토큰.
  const token = {
    name: username,
    email: `${username}@e2e.local`,
    sub: 'e2e-backend-user',
    accessToken: tokens.access_token,
    refreshToken: tokens.refresh_token ?? 'e2e-refresh',
    accessTokenExpires: Date.now() + (tokens.expires_in ?? oneDay) * 1000,
    tenantId: tenantIdFromToken(tokens.access_token),
  }
  const cookieValue = await encode({
    token, secret: authSecret, salt: COOKIE_NAME, maxAge: oneDay,
  })

  await context.addCookies([{
    name: COOKIE_NAME,
    value: cookieValue,
    domain: 'localhost',
    path: '/',
    httpOnly: true,
    sameSite: 'Lax',
    expires: Math.floor(Date.now() / 1000) + oneDay,
  }])

  fs.mkdirSync(path.dirname(AUTH_FILE), { recursive: true })
  await context.storageState({ path: AUTH_FILE })
})
