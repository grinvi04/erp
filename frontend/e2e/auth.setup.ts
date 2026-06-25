import { test as setup } from '@playwright/test'
import { encode } from '@auth/core/jwt'
import fs from 'node:fs'
import path from 'node:path'

// next-auth v5(JWT 세션)의 세션 쿠키를 직접 생성해 인증 상태를 주입한다 —
// Keycloak 로그인 없이 인증된 페이지 렌더를 E2E로 검증하기 위함.
// 비밀값/쿠키명은 playwright.config.ts의 webServer env(AUTH_SECRET)와 일치해야 한다.
const AUTH_SECRET = 'e2e-test-secret-not-a-real-credential'
const COOKIE_NAME = 'authjs.session-token' // http(비-secure) 기본 쿠키명
const AUTH_FILE = path.join('e2e', '.auth', 'user.json')

setup('authenticate', async ({ context }) => {
  const oneDay = 24 * 60 * 60
  // auth.ts의 jwt 콜백이 통과시키는 토큰 형태: 만료 미래 + error 없음 → 세션 유효.
  const token = {
    name: 'E2E User',
    email: 'e2e@test.local',
    sub: 'e2e-user-sub',
    accessToken: 'e2e-fake-access-token',
    refreshToken: 'e2e-fake-refresh-token',
    accessTokenExpires: Date.now() + oneDay * 1000,
    tenantId: '1',
  }
  const cookieValue = await encode({
    token, secret: AUTH_SECRET, salt: COOKIE_NAME, maxAge: oneDay,
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
