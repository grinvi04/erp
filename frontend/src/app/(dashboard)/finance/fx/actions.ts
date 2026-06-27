'use server'
import { apiPost, apiPut } from '@/lib/api'
import { revalidatePath } from 'next/cache'
import type { BaseCurrency, ExchangeRate, FxGainLossAccounts } from '@/types/finance'

const PATH = '/finance/fx'

export async function updateBaseCurrency(baseCurrency: string): Promise<void> {
  await apiPut<BaseCurrency>('/api/finance/fx/base-currency', { baseCurrency })
  revalidatePath(PATH)
}

export async function updateFxGainLossAccounts(data: {
  fxGainAccountId: number | null
  fxLossAccountId: number | null
}): Promise<void> {
  await apiPut<FxGainLossAccounts>('/api/finance/fx/gain-loss-accounts', data)
  revalidatePath(PATH)
}

export async function createExchangeRate(data: {
  fromCurrency: string
  toCurrency: string
  effectiveDate: string
  rate: number
}): Promise<void> {
  await apiPost<ExchangeRate>('/api/finance/fx/rates', data)
  revalidatePath(PATH)
}
