import { apiGet } from '@/lib/api'
import type { CrmAccount } from '@/types/crm'
import type { PageResponse } from '@/types/api'
import ContactsClient from './contacts-client'

export const metadata = { title: '담당자 | ERP' }

export default async function ContactsPage() {
  const data = await apiGet<PageResponse<CrmAccount>>('/api/crm/accounts?size=1000')
  return <ContactsClient accounts={data.content} />
}
