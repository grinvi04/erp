import { test, expect } from '@playwright/test'

// 인증 게이트 스모크 — 백엔드/Keycloak 없이 프론트엔드만으로 검증한다.
// (dashboard) 레이아웃의 auth()가 세션 없는 요청을 /login으로 리다이렉트한다.

test.describe('인증 게이트', () => {
  // 4개 모듈 + 공통 화면의 대표 보호 라우트. (dashboard) 그룹 전체가 레이아웃에서
  // 게이트되므로 모듈별로 골고루 표본을 둔다.
  const protectedRoutes = [
    '/',
    '/approvals',
    '/analytics',
    '/audit',
    '/iam',
    '/hr/employees',
    '/hr/departments',
    '/finance/accounts',
    '/finance/invoices',
    '/finance/journal-entries',
    '/finance/reports',
    '/finance/fx',
    '/inventory/items',
    '/inventory/movements',
    '/crm/accounts',
    '/crm/opportunities',
  ]

  for (const route of protectedRoutes) {
    test(`미인증 사용자는 ${route} 접근 시 /login으로 리다이렉트된다`, async ({ page }) => {
      await page.goto(route)
      // 레이아웃은 plain /login으로 보내지만, 미들웨어에 authorized 콜백이 추가되면
      // /login?callbackUrl=... 형태가 될 수 있어 쿼리 유무 모두 허용한다.
      await expect(page).toHaveURL(/\/login(\?|$)/)
    })
  }

  test('로그인 페이지가 Keycloak 로그인 UI를 렌더한다', async ({ page }) => {
    await page.goto('/login')
    await expect(page.getByText('ERP System')).toBeVisible()
    await expect(page.getByText(/Keycloak 계정으로 로그인/)).toBeVisible()
    await expect(page.getByRole('button', { name: 'Keycloak으로 로그인' })).toBeVisible()
  })
})
