import { apiGet } from '@/lib/api'
import type { Account, BaseCurrency, ExchangeRate, FxGainLossAccounts } from '@/types/finance'
import FxClient from './fx-client'

export const metadata = { title: 'FX 설정 | ERP' }

export default async function FxPage() {
  const [baseCurrency, rates, accounts, fxAccounts] = await Promise.all([
    apiGet<BaseCurrency>('/api/finance/fx/base-currency'),
    apiGet<ExchangeRate[]>('/api/finance/fx/rates'),
    apiGet<Account[]>('/api/finance/accounts'),
    apiGet<FxGainLossAccounts>('/api/finance/fx/gain-loss-accounts'),
  ])
  return (
    <FxClient
      baseCurrency={baseCurrency.baseCurrency}
      rates={rates}
      accounts={accounts}
      fxAccounts={fxAccounts}
    />
  )
}
