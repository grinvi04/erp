import { test, expect } from '@playwright/test'

// 인증된 세션(주입된 쿠키)에서 보호 라우트가 /login으로 튕기지 않고 렌더되는지 검증.
// 백엔드 미기동이므로 데이터 fetch는 graceful 실패(safeGet→빈값)하되, 레이아웃/페이지
// 골격은 정상 렌더되어야 한다.

test.describe('인증된 사용자', () => {
  test('대시보드(/)가 /login으로 리다이렉트되지 않고 렌더된다', async ({ page }) => {
    await page.goto('/')
    await expect(page).not.toHaveURL(/\/login/)
    await expect(page.getByRole('heading', { name: '대시보드', level: 1 })).toBeVisible()
  })

  test('사이드바 네비게이션(결재함·분석)이 보인다', async ({ page }) => {
    await page.goto('/')
    await expect(page.getByRole('link', { name: '결재함' })).toBeVisible()
    await expect(page.getByRole('link', { name: '분석' })).toBeVisible()
  })

  test('결재함(/approvals)이 인증 상태로 렌더된다', async ({ page }) => {
    await page.goto('/approvals')
    await expect(page).not.toHaveURL(/\/login/)
    await expect(page.getByRole('heading', { name: '결재함', level: 1 })).toBeVisible()
  })
})
