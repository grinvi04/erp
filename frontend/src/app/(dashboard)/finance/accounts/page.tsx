import { apiGet } from '@/lib/api'
import type { Account } from '@/types/finance'
import AccountsClient from './accounts-client'

export const metadata = { title: '계정과목 | ERP' }

export default async function AccountsPage() {
  const accounts = await apiGet<Account[]>('/api/finance/accounts')
  return <AccountsClient accounts={accounts} />
}
