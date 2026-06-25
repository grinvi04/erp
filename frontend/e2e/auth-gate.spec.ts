import { test, expect } from '@playwright/test'

// 인증 게이트 스모크 — 백엔드/Keycloak 없이 프론트엔드만으로 검증한다.
// (dashboard) 레이아웃의 auth()가 세션 없는 요청을 /login으로 리다이렉트한다.

test.describe('인증 게이트', () => {
  const protectedRoutes = [
    '/',
    '/approvals',
    '/analytics',
    '/hr/employees',
    '/finance/invoices',
    '/inventory/items',
    '/crm/accounts',
  ]

  for (const route of protectedRoutes) {
    test(`미인증 사용자는 ${route} 접근 시 /login으로 리다이렉트된다`, async ({ page }) => {
      await page.goto(route)
      await expect(page).toHaveURL(/\/login$/)
    })
  }

  test('로그인 페이지가 Keycloak 로그인 UI를 렌더한다', async ({ page }) => {
    await page.goto('/login')
    await expect(page.getByText('ERP System')).toBeVisible()
    await expect(page.getByText('Keycloak 계정으로 로그인하세요')).toBeVisible()
    await expect(page.getByRole('button', { name: 'Keycloak으로 로그인' })).toBeVisible()
  })
})
