import { apiGet } from '@/lib/api'
import type { BaseCurrency, ExchangeRate } from '@/types/finance'
import FxClient from './fx-client'

export const metadata = { title: 'FX 설정 | ERP' }

export default async function FxPage() {
  const [baseCurrency, rates] = await Promise.all([
    apiGet<BaseCurrency>('/api/finance/fx/base-currency'),
    apiGet<ExchangeRate[]>('/api/finance/fx/rates'),
  ])
  return <FxClient baseCurrency={baseCurrency.baseCurrency} rates={rates} />
}
