import { apiGet } from '@/lib/api'
import type { CompanyProfile } from '@/types/finance'
import CompanyProfileClient from './company-profile-client'

export const metadata = { title: '회사정보 | ERP' }

export default async function CompanyProfilePage() {
  const profile = await apiGet<CompanyProfile>('/api/finance/company-profile')
  return <CompanyProfileClient profile={profile} />
}
