import type { CurrencyAmount } from '@/types/money'

export function formatMoneyOne(amount: number, currency: string): string {
  try {
    return new Intl.NumberFormat('ko-KR', {
      style: 'currency',
      currency,
      maximumFractionDigits: 0,
    }).format(amount)
  } catch {
    return `${currency} ${amount.toLocaleString('ko-KR')}`
  }
}

export function formatMoneyList(items: CurrencyAmount[]): string {
  if (!items || items.length === 0) return formatMoneyOne(0, 'KRW')
  return items.map((i) => formatMoneyOne(i.amount, i.currency)).join(' · ')
}
