import { defineConfig, devices } from '@playwright/test'

// E2E는 프론트엔드만으로 검증 가능한 인증 게이트·공개 페이지를 커버한다.
// (백엔드/Keycloak 불필요 — auth()는 세션 쿠키 부재 시 Keycloak 접속 없이 /login으로 보냄)
const PORT = 3100
const BASE_URL = `http://localhost:${PORT}`

export default defineConfig({
  testDir: './e2e',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : undefined,
  reporter: process.env.CI ? 'github' : 'list',
  use: {
    baseURL: BASE_URL,
    trace: 'on-first-retry',
  },
  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
  ],
  webServer: {
    // 빌드된 앱을 기동한다(CI·로컬 모두 사전 `npm run build` 필요).
    command: `npm run start -- -p ${PORT}`,
    url: BASE_URL,
    timeout: 120_000,
    reuseExistingServer: !process.env.CI,
    env: {
      // 테스트 전용 더미 값 — 실제 시크릿 아님. auth() 부트스트랩에만 사용.
      AUTH_SECRET: 'e2e-test-secret-not-a-real-credential',
      AUTH_KEYCLOAK_ID: 'e2e-test-client',
      AUTH_KEYCLOAK_SECRET: 'e2e-test-client-secret',
      KEYCLOAK_ISSUER: 'http://localhost:8180/realms/erp-test',
      AUTH_URL: BASE_URL,
      NEXTAUTH_URL: BASE_URL,
    },
  },
})
