import { expect, test } from '@playwright/test'
import { encode } from '@auth/core/jwt'

import { loadCommercialUatConfig } from '../src/lib/commercial-uat'

type Envelope<T> = { success: boolean; data: T }
type Page<T> = { content: T[] }
type Account = { id: number; code: string }
type FiscalYear = { id: number; year: number }
type FiscalPeriod = { id: number; periodNumber: number }
type Party = { id: number }
type Invoice = {
  id: number
  invoiceNo: string
  status: string
  totalAmount: number
  journalEntryId: number | null
}
type JournalEntry = {
  id: number
  status: string
  referenceType: string | null
  referenceId: number | null
  totalDebit: number
  totalCredit: number
}
type TrialBalance = { totalDebit: number; totalCredit: number; excludedEntryCount: number }
type IncomeStatement = { netIncome: number; excludedEntryCount: number }
type BalanceSheet = {
  totalAssets: number
  totalLiabilities: number
  netIncome: number
  balanced: boolean
  excludedEntryCount: number
}
type VatReturn = {
  sales: { taxableSupply: number; taxableVat: number }
  purchases: { supply: number; vat: number }
  payableTax: number
}
type Resource = { id: number }
type Movement = { id: number; movementNo: string; status: string }
type Stock = { locationId: number; qtyOnHand: number }
type AuditLog = { id: number; entityType: string; entityId: number; action: string }
type AuditDetail = AuditLog & {
  beforeData: string | null
  afterData: string | null
  traceId: string | null
}

const config = loadCommercialUatConfig(process.env)
const suffix = config.runId.slice(-8).toUpperCase()
const today = new Date().toLocaleDateString('en-CA', { timeZone: 'Asia/Seoul' })
const year = Number(today.slice(0, 4))
const month = Number(today.slice(5, 7))
const periodStart = `${today.slice(0, 7)}-01`
const periodEnd = new Date(Date.UTC(year, month, 0)).toISOString().slice(0, 10)

let creatorToken: string
let approverToken: string
let tenantBToken: string

test.beforeAll(async () => {
  ;[creatorToken, approverToken, tenantBToken] = await Promise.all([
    userToken(config.creatorUsername, config.creatorPassword),
    userToken(config.approverUsername, config.approverPassword),
    userToken(config.tenantBUsername, config.tenantBPassword),
  ])
})

test.describe.serial('상용 UAT — 재무·테넌트 격리', () => {
  test('AP/AR부터 GL·재무제표·부가세까지 실제 업무흐름을 보존한다', async () => {
    const fiscalYear = await ensureFiscalYear()
    const fiscalPeriod = await ensureFiscalPeriod(fiscalYear.id)
    const accounts = {
      cash: await createAccount(`U${suffix}01`, 'UAT 현금', 'ASSET', 'DEBIT'),
      receivables: await createAccount(`U${suffix}02`, 'UAT 외상매출금', 'ASSET', 'DEBIT'),
      vatReceivable: await createAccount(`U${suffix}03`, 'UAT 부가세대급금', 'ASSET', 'DEBIT'),
      payables: await createAccount(`U${suffix}04`, 'UAT 외상매입금', 'LIABILITY', 'CREDIT'),
      vatPayable: await createAccount(`U${suffix}05`, 'UAT 부가세예수금', 'LIABILITY', 'CREDIT'),
      revenue: await createAccount(`U${suffix}06`, 'UAT 매출', 'REVENUE', 'CREDIT'),
      expense: await createAccount(`U${suffix}07`, 'UAT 비용', 'EXPENSE', 'DEBIT'),
    }
    const vatSettings = await json<{ version: number | null }>(
      creatorToken,
      '/api/finance/fx/vat-accounts',
    )
    await json(creatorToken, '/api/finance/fx/vat-accounts', {
      method: 'PUT',
      body: {
        vatReceivableAccountId: accounts.vatReceivable.id,
        vatPayableAccountId: accounts.vatPayable.id,
        version: vatSettings.version,
      },
    })
    const companyProfile = await json<{ version: number | null }>(
      creatorToken,
      '/api/finance/company-profile',
    )
    await json(creatorToken, '/api/finance/company-profile', {
      method: 'PUT',
      body: {
        companyName: 'Commercial UAT Supplier',
        businessNo: '1208147521',
        representative: 'UAT Supplier',
        address: 'Seoul',
        businessType: 'Software',
        businessItem: 'ERP',
        version: companyProfile.version,
      },
    })

    const vendor = await json<Party>(creatorToken, '/api/finance/vendors', {
      method: 'POST',
      expected: 201,
      body: {
        code: `V-${suffix}`,
        name: `UAT Vendor ${suffix}`,
        businessNo: null,
        contactName: 'Vendor UAT',
        contactEmail: `vendor-${suffix.toLowerCase()}@uat.erp.local`,
        contactPhone: null,
        paymentTerms: 30,
        payablesAccountId: accounts.payables.id,
      },
    })
    const customer = await json<Party>(creatorToken, '/api/finance/customers', {
      method: 'POST',
      expected: 201,
      body: {
        code: `C-${suffix}`,
        name: `UAT Customer ${suffix}`,
        businessNo: '1008112348',
        contactName: 'Customer UAT',
        contactEmail: `customer-${suffix.toLowerCase()}@uat.erp.local`,
        contactPhone: null,
        paymentTerms: 30,
        receivablesAccountId: accounts.receivables.id,
        representativeName: 'Buyer UAT',
        address: 'Busan',
        businessType: 'Retail',
        businessItem: 'Goods',
      },
    })

    const before = await reportSnapshot()
    const ap = await createApInvoice(vendor.id, accounts.expense.id)
    await json(creatorToken, `/api/finance/invoices/${ap.id}/submit`, { method: 'POST' })
    await expectStatus(creatorToken, `/api/finance/invoices/${ap.id}/approve`, 403)
    expect((await json<Invoice>(creatorToken, `/api/finance/invoices/${ap.id}`)).status).toBe(
      'PENDING_APPROVAL',
    )
    const approvedAp = await json<Invoice>(
      approverToken,
      `/api/finance/invoices/${ap.id}/approve`,
      { method: 'POST' },
    )
    expect(approvedAp.status).toBe('APPROVED')
    expect(approvedAp.journalEntryId).not.toBeNull()
    const paidAp = await json<Invoice>(approverToken, `/api/finance/invoices/${ap.id}/pay`, {
      method: 'POST',
      body: { amount: 11000, cashAccountId: accounts.cash.id, paymentDate: today },
    })
    expect(paidAp.status).toBe('PAID')

    const ar = await createArInvoice(customer.id, accounts.revenue.id)
    await json(creatorToken, `/api/finance/ar-invoices/${ar.id}/submit`, { method: 'POST' })
    await expectStatus(creatorToken, `/api/finance/ar-invoices/${ar.id}/approve`, 403)
    expect((await json<Invoice>(creatorToken, `/api/finance/ar-invoices/${ar.id}`)).status).toBe(
      'PENDING_APPROVAL',
    )
    const approvedAr = await json<Invoice>(
      approverToken,
      `/api/finance/ar-invoices/${ar.id}/approve`,
      { method: 'POST' },
    )
    expect(approvedAr.status).toBe('APPROVED')
    expect(approvedAr.journalEntryId).not.toBeNull()
    const paidAr = await json<Invoice>(approverToken, `/api/finance/ar-invoices/${ar.id}/pay`, {
      method: 'POST',
      body: { amount: 22000, cashAccountId: accounts.cash.id, paymentDate: today },
    })
    expect(paidAr.status).toBe('PAID')
    await json(creatorToken, `/api/finance/ar-invoices/${ar.id}/tax-invoice`, {
      method: 'POST',
      expected: 201,
      body: { writeDate: today, chargeType: 'RECEIPT', itemName: `UAT ERP ${suffix}` },
    })

    const entries = await json<Page<JournalEntry>>(
      creatorToken,
      `/api/finance/journal-entries?fiscalPeriodId=${fiscalPeriod.id}&size=100`,
    )
    const expectedReferences = new Set([
      `AP_INVOICE:${ap.id}`,
      `AP_PAYMENT:${ap.id}`,
      `AR_INVOICE:${ar.id}`,
      `AR_PAYMENT:${ar.id}`,
    ])
    const runEntries = entries.content.filter((entry) =>
      expectedReferences.has(`${entry.referenceType}:${entry.referenceId}`),
    )
    expect(runEntries).toHaveLength(4)
    for (const entry of runEntries) {
      expect(Number(entry.totalDebit)).toBe(Number(entry.totalCredit))
      await json(approverToken, `/api/finance/journal-entries/${entry.id}/submit`, {
        method: 'POST',
      })
      await expectStatus(
        approverToken,
        `/api/finance/journal-entries/${entry.id}/approve`,
        403,
      )
      const posted = await json<JournalEntry>(
        creatorToken,
        `/api/finance/journal-entries/${entry.id}/approve`,
        { method: 'POST' },
      )
      expect(posted.status).toBe('POSTED')
    }

    const after = await reportSnapshot()
    expect(after.trial.totalDebit - before.trial.totalDebit).toBe(66000)
    expect(after.trial.totalCredit - before.trial.totalCredit).toBe(66000)
    expect(after.trial.excludedEntryCount).toBe(before.trial.excludedEntryCount)
    expect(after.income.netIncome - before.income.netIncome).toBe(10000)
    expect(after.balance.totalAssets - before.balance.totalAssets).toBe(12000)
    expect(after.balance.totalLiabilities - before.balance.totalLiabilities).toBe(2000)
    expect(after.balance.netIncome - before.balance.netIncome).toBe(10000)
    expect(after.balance.balanced).toBe(true)
    expect(after.balance.excludedEntryCount).toBe(before.balance.excludedEntryCount)
    expect(after.vat.sales.taxableSupply - before.vat.sales.taxableSupply).toBe(20000)
    expect(after.vat.sales.taxableVat - before.vat.sales.taxableVat).toBe(2000)
    expect(after.vat.purchases.supply - before.vat.purchases.supply).toBe(10000)
    expect(after.vat.purchases.vat - before.vat.purchases.vat).toBe(1000)
    expect(after.vat.payableTax - before.vat.payableTax).toBe(1000)

    await expectStatus(tenantBToken, `/api/finance/invoices/${ap.id}`, 404, 'GET')
    const isolatedAp = await json<Page<Invoice>>(
      tenantBToken,
      `/api/finance/invoices?from=${today}&to=${today}&size=100`,
    )
    const isolatedAr = await json<Page<Invoice>>(
      tenantBToken,
      `/api/finance/ar-invoices?from=${today}&to=${today}&size=100`,
    )
    expect(isolatedAp.content.some((invoice) => invoice.invoiceNo === ap.invoiceNo)).toBe(false)
    expect(isolatedAr.content.some((invoice) => invoice.invoiceNo === ar.invoiceNo)).toBe(false)
  })

  test('재고 이동·조정 결재와 감사 traceId를 실제 화면까지 추적한다', async ({ page }) => {
    const uom = await json<Resource>(creatorToken, '/api/inventory/uoms', {
      method: 'POST',
      expected: 201,
      body: { code: `U${suffix}`, name: `UAT 단위 ${suffix}` },
    })
    const category = await json<Resource>(creatorToken, '/api/inventory/item-categories', {
      method: 'POST',
      expected: 201,
      body: { code: `CAT-${suffix}`, name: `UAT 카테고리 ${suffix}`, parentId: null },
    })
    const item = await json<Resource>(creatorToken, '/api/inventory/items', {
      method: 'POST',
      expected: 201,
      body: {
        sku: `SKU-${suffix}`,
        name: `UAT 상품 ${suffix}`,
        description: config.runId,
        categoryId: category.id,
        uomId: uom.id,
        costMethod: 'WEIGHTED_AVG',
        standardCost: 1000,
        reorderPoint: 10,
        reorderQty: 50,
        minStock: 5,
        maxStock: 200,
        lotTracked: false,
        serialTracked: false,
      },
    })
    const warehouseA = await createWarehouse(`WA-${suffix}`)
    const warehouseB = await createWarehouse(`WB-${suffix}`)
    const locationA = await createLocation(warehouseA.id, `LA-${suffix}`)
    const locationB = await createLocation(warehouseB.id, `LB-${suffix}`)

    const receipt = await createMovement('RECEIPT', item.id, null, locationA.id, 100)
    expect((await confirmMovement(receipt.id)).status).toBe('CONFIRMED')
    await expectStock(item.id, locationA.id, 100)

    const transfer = await createMovement(
      'TRANSFER',
      item.id,
      locationA.id,
      locationB.id,
      30,
    )
    expect((await confirmMovement(transfer.id)).status).toBe('CONFIRMED')
    await expectStock(item.id, locationA.id, 70)
    await expectStock(item.id, locationB.id, 30)

    const issue = await createMovement('ISSUE', item.id, locationB.id, null, 10)
    expect((await confirmMovement(issue.id)).status).toBe('CONFIRMED')
    await expectStock(item.id, locationB.id, 20)

    const adjustment = await createMovement('ADJUSTMENT', item.id, null, locationB.id, 5)
    expect(
      (await json<Movement>(creatorToken, `/api/inventory/movements/${adjustment.id}/submit`, {
        method: 'POST',
      })).status,
    ).toBe('PENDING_APPROVAL')
    await expectStatus(
      creatorToken,
      `/api/inventory/movements/${adjustment.id}/approve`,
      403,
    )
    expect(
      (await json<Movement>(creatorToken, `/api/inventory/movements/${adjustment.id}`)).status,
    ).toBe('PENDING_APPROVAL')
    const approved = await jsonWithResponse<Movement>(
      approverToken,
      `/api/inventory/movements/${adjustment.id}/approve`,
      { method: 'POST' },
    )
    expect(approved.data.status).toBe('CONFIRMED')
    const traceId = approved.response.headers.get('x-trace-id')
    expect(traceId).toMatch(/^[0-9a-f]{32}$/)
    await expectStock(item.id, locationA.id, 70)
    await expectStock(item.id, locationB.id, 25)

    await expectStatus(tenantBToken, `/api/inventory/movements/${adjustment.id}`, 404, 'GET')
    const tenantBMovements = await json<Page<Movement>>(
      tenantBToken,
      '/api/inventory/movements?size=100',
    )
    expect(tenantBMovements.content.some((movement) => movement.id === adjustment.id)).toBe(false)

    const audit = await json<Page<AuditLog>>(
      creatorToken,
      `/api/audit/logs?entityType=STOCK_MOVEMENT&entityId=${adjustment.id}&action=APPROVE&size=20`,
    )
    expect(audit.content).toHaveLength(1)
    const detail = await json<AuditDetail>(creatorToken, `/api/audit/logs/${audit.content[0].id}`)
    expect(detail.traceId).toBe(traceId)
    expect(detail.entityId).toBe(adjustment.id)

    const accessProfileAudits = await json<Page<AuditLog>>(
      creatorToken,
      '/api/audit/logs?entityType=ACCESS_PROFILE&action=UPDATE&size=20',
    )
    expect(accessProfileAudits.content.length).toBeGreaterThanOrEqual(2)
    const accessProfileDetail = await json<AuditDetail>(
      creatorToken,
      `/api/audit/logs/${accessProfileAudits.content[0].id}`,
    )
    expect(accessProfileDetail.beforeData).toBeNull()
    expect(JSON.parse(accessProfileDetail.afterData ?? '{}')).toMatchObject({ dataScope: 'ALL' })
    expect(accessProfileDetail.traceId).toMatch(/^[0-9a-f]{32}$/)

    const csvResponse = await request(
      creatorToken,
      `/api/audit/logs/export?entityType=STOCK_MOVEMENT&entityId=${adjustment.id}&action=APPROVE`,
    )
    expect(csvResponse.headers.get('content-type')).toContain('text/csv')
    const csv = await csvResponse.text()
    expect(csv).toContain('STOCK_MOVEMENT')
    expect(csv).toContain(String(adjustment.id))

    const pageErrors: Error[] = []
    page.on('pageerror', (error) => pageErrors.push(error))
    await addAuthenticatedCookie(page, creatorToken)
    for (const [pathname, heading] of [
      ['/finance/invoices', '매입계산서'],
      ['/inventory/movements', '재고 이동'],
      ['/approvals', '결재함'],
    ] as const) {
      await page.goto(pathname)
      await expect(page.getByRole('heading', { name: heading })).toBeVisible()
      expect(page.url()).not.toContain('/login')
    }
    await page.goto(`/audit?entityType=STOCK_MOVEMENT&action=APPROVE`)
    await expect(page.getByRole('heading', { name: '감사 로그' })).toBeVisible()
    await page
      .getByRole('row')
      .filter({ hasText: '재고 이동' })
      .filter({ hasText: String(adjustment.id) })
      .click()
    await expect(page.getByText(traceId!)).toBeVisible()
    expect(pageErrors).toEqual([])
  })
})

async function createWarehouse(code: string): Promise<Resource> {
  return json(creatorToken, '/api/inventory/warehouses', {
    method: 'POST',
    expected: 201,
    body: { code, name: `UAT 창고 ${code}`, address: 'Seoul' },
  })
}

async function createLocation(warehouseId: number, code: string): Promise<Resource> {
  return json(creatorToken, '/api/inventory/locations', {
    method: 'POST',
    expected: 201,
    body: { warehouseId, code, name: `UAT 로케이션 ${code}`, parentId: null, locationType: 'BIN' },
  })
}

async function createMovement(
  movementType: string,
  itemId: number,
  fromLocationId: number | null,
  toLocationId: number | null,
  qty: number,
): Promise<Movement> {
  return json(creatorToken, '/api/inventory/movements', {
    method: 'POST',
    expected: 201,
    body: {
      movementType,
      movementDate: today,
      referenceType: 'COMMERCIAL_UAT',
      referenceId: null,
      note: config.runId,
      lines: [{ itemId, fromLocationId, toLocationId, lotNo: null, serialNo: null, qty, unitCost: 1000 }],
    },
  })
}

async function confirmMovement(id: number): Promise<Movement> {
  return json(creatorToken, `/api/inventory/movements/${id}/confirm`, { method: 'POST' })
}

async function expectStock(itemId: number, locationId: number, expectedQty: number) {
  const stocks = await json<Page<Stock>>(
    creatorToken,
    `/api/inventory/stocks/by-item?itemId=${itemId}&size=100`,
  )
  const stock = stocks.content.find((entry) => entry.locationId === locationId)
  expect(Number(stock?.qtyOnHand ?? 0)).toBe(expectedQty)
}

async function addAuthenticatedCookie(page: import('@playwright/test').Page, accessToken: string) {
  const cookieName = 'authjs.session-token'
  const claims = jwtClaims(accessToken)
  const oneDay = 24 * 60 * 60
  const cookieValue = await encode({
    token: {
      name: config.creatorUsername,
      email: `${config.creatorUsername}@uat.erp.local`,
      sub: claims.sub,
      accessToken,
      refreshToken: 'commercial-uat-refresh-not-used',
      accessTokenExpires: Date.now() + oneDay * 1000,
      tenantId: String(claims.tenant_id),
    },
    secret: config.authSecret,
    salt: cookieName,
    maxAge: oneDay,
  })
  await page.context().addCookies([
    {
      name: cookieName,
      value: cookieValue,
      domain: 'localhost',
      path: '/',
      httpOnly: true,
      sameSite: 'Lax',
      expires: Math.floor(Date.now() / 1000) + oneDay,
    },
  ])
}

function jwtClaims(token: string): { sub: string; tenant_id: string | number } {
  const payload = token.split('.')[1]
  if (!payload) throw new Error('[commercial] JWT payload가 없습니다.')
  const claims = JSON.parse(Buffer.from(payload, 'base64url').toString('utf8')) as {
    sub?: string
    tenant_id?: string | number
  }
  if (!claims.sub || claims.tenant_id == null) {
    throw new Error('[commercial] JWT sub/tenant_id claim이 없습니다.')
  }
  return { sub: claims.sub, tenant_id: claims.tenant_id }
}

async function ensureFiscalYear(): Promise<FiscalYear> {
  const years = await json<FiscalYear[]>(creatorToken, '/api/finance/fiscal-years')
  const existing = years.find((item) => item.year === year)
  if (existing) return existing
  return json(creatorToken, '/api/finance/fiscal-years', {
    method: 'POST',
    expected: 201,
    body: { year, startDate: `${year}-01-01`, endDate: `${year}-12-31` },
  })
}

async function ensureFiscalPeriod(fiscalYearId: number): Promise<FiscalPeriod> {
  const periods = await json<FiscalPeriod[]>(
    creatorToken,
    `/api/finance/fiscal-years/${fiscalYearId}/periods`,
  )
  const existing = periods.find((item) => item.periodNumber === month)
  if (existing) return existing
  return json(creatorToken, `/api/finance/fiscal-years/${fiscalYearId}/periods`, {
    method: 'POST',
    expected: 201,
    body: { periodNumber: month, startDate: periodStart, endDate: periodEnd },
  })
}

async function createAccount(
  code: string,
  name: string,
  accountType: string,
  normalBalance: string,
): Promise<Account> {
  return json(creatorToken, '/api/finance/accounts', {
    method: 'POST',
    expected: 201,
    body: { code, name, accountType, normalBalance, parentId: null, isSummary: false },
  })
}

async function createApInvoice(vendorId: number, expenseAccountId: number): Promise<Invoice> {
  return json(creatorToken, '/api/finance/invoices', {
    method: 'POST',
    expected: 201,
    body: {
      invoiceNo: `AP-${suffix}`,
      vendorId,
      invoiceDate: today,
      dueDate: today,
      supplyAmount: 10000,
      taxType: 'TAXABLE',
      currency: 'KRW',
      note: config.runId,
      lines: [{ accountId: expenseAccountId, amount: 10000, description: config.runId }],
    },
  })
}

async function createArInvoice(customerId: number, revenueAccountId: number): Promise<Invoice> {
  return json(creatorToken, '/api/finance/ar-invoices', {
    method: 'POST',
    expected: 201,
    body: {
      invoiceNo: `AR-${suffix}`,
      customerId,
      invoiceDate: today,
      dueDate: today,
      supplyAmount: 20000,
      taxType: 'TAXABLE',
      currency: 'KRW',
      note: config.runId,
      lines: [{ accountId: revenueAccountId, amount: 20000, description: config.runId }],
    },
  })
}

async function reportSnapshot() {
  const [trial, income, balance, vat] = await Promise.all([
    json<TrialBalance>(creatorToken, `/api/finance/reports/trial-balance?year=${year}`),
    json<IncomeStatement>(creatorToken, `/api/finance/reports/income-statement?year=${year}`),
    json<BalanceSheet>(creatorToken, `/api/finance/reports/balance-sheet?year=${year}`),
    json<VatReturn>(creatorToken, `/api/finance/vat-return?from=${today}&to=${today}`),
  ])
  return { trial, income, balance, vat }
}

async function userToken(username: string, password: string): Promise<string> {
  const response = await fetch(`${config.keycloakIssuer}/protocol/openid-connect/token`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({
      grant_type: 'password',
      client_id: process.env.E2E_CLIENT_ID ?? 'erp-frontend',
      client_secret: config.clientSecret,
      username,
      password,
      scope: 'openid',
    }),
  })
  if (!response.ok) throw new Error(`[commercial] 사용자 인증 실패(HTTP ${response.status}).`)
  const body = (await response.json()) as { access_token?: string }
  if (!body.access_token) throw new Error('[commercial] 사용자 인증 응답에 access token이 없습니다.')
  return body.access_token
}

type JsonInit = Omit<RequestInit, 'body'> & { body?: unknown; expected?: number }

async function json<T>(token: string, pathname: string, init: JsonInit = {}): Promise<T> {
  const response = await request(token, pathname, init)
  const envelope = (await response.json()) as Envelope<T>
  if (!envelope.success) throw new Error(`[commercial] 실패 envelope(path=${pathname}).`)
  return envelope.data
}

async function jsonWithResponse<T>(
  token: string,
  pathname: string,
  init: JsonInit = {},
): Promise<{ data: T; response: Response }> {
  const response = await request(token, pathname, init)
  const envelope = (await response.json()) as Envelope<T>
  if (!envelope.success) throw new Error(`[commercial] 실패 envelope(path=${pathname}).`)
  return { data: envelope.data, response }
}

async function expectStatus(
  token: string,
  pathname: string,
  status: number,
  method = 'POST',
) {
  const response = await request(token, pathname, { method, expected: status })
  expect(response.status).toBe(status)
}

async function request(token: string, pathname: string, init: JsonInit = {}) {
  const { body, expected = 200, ...requestInit } = init
  const response = await fetch(`${config.backendUrl}${pathname}`, {
    ...requestInit,
    headers: {
      Authorization: `Bearer ${token}`,
      ...(body === undefined ? {} : { 'Content-Type': 'application/json' }),
      ...requestInit.headers,
    },
    body: body === undefined ? undefined : JSON.stringify(body),
  })
  if (response.status !== expected) {
    throw new Error(
      `[commercial] 예상하지 않은 응답(HTTP ${response.status}, expected=${expected}, path=${pathname}).`,
    )
  }
  return response
}
