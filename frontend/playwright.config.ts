import { defineConfig, devices, type Project } from '@playwright/test'

// 기본 E2E는 프론트엔드만으로 검증 가능한 인증 게이트·인증 렌더 스모크를 커버한다.
// (백엔드/Keycloak 불필요 — auth()는 세션 쿠키 부재 시 Keycloak 접속 없이 /login으로 보냄)
const PORT = 3100
const BASE_URL = `http://localhost:${PORT}`

// 실스택 백엔드 통합 E2E는 env 게이트로만 활성화한다. E2E_BACKEND가 없으면(CI 포함)
// 아래 backend 프로젝트는 projects 배열에서 제외되어 실행 자체가 되지 않는다.
const BACKEND_ENABLED = !!process.env.E2E_BACKEND
// 백엔드 통합은 실 백엔드·Keycloak에 연동된 dev 서버(기본 localhost:3000)를 대상으로 한다.
const BACKEND_BASE_URL = process.env.E2E_BACKEND_URL ?? 'http://localhost:3000'

const projects: Project[] = [
  // 인증 쿠키를 생성해 storageState로 저장.
  { name: 'setup', testMatch: /auth\.setup\.ts/ },
  // 인증 게이트(미인증) 스모크.
  {
    name: 'unauth',
    testMatch: /auth-gate\.spec\.ts/,
    use: { ...devices['Desktop Chrome'] },
  },
  // 인증된 렌더 스모크 — setup이 만든 세션 사용.
  {
    name: 'authed',
    testMatch: /authenticated\.spec\.ts/,
    use: { ...devices['Desktop Chrome'], storageState: 'e2e/.auth/user.json' },
    dependencies: ['setup'],
  },
]

if (BACKEND_ENABLED) {
  // 실 Keycloak password grant로 진짜 세션을 만든다.
  projects.push({ name: 'backend-setup', testMatch: /backend\.setup\.ts/ })
  // 실 백엔드 데이터가 렌더되는지 검증 — 이미 떠 있는 실스택 dev 서버(3000)를 대상으로 한다.
  projects.push({
    name: 'backend',
    testMatch: /backend-integration\.spec\.ts/,
    use: {
      ...devices['Desktop Chrome'],
      baseURL: BACKEND_BASE_URL,
      storageState: 'e2e/.auth/backend.json',
    },
    dependencies: ['backend-setup'],
  })
}

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
  projects,
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
