import { expect, test } from '@playwright/test'

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
})

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
