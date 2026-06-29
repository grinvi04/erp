'use server'
import { apiPut } from '@/lib/api'
import { revalidatePath } from 'next/cache'
import type { CompanyProfile } from '@/types/finance'

const PATH = '/finance/company-profile'

export async function updateCompanyProfile(data: {
  companyName: string
  businessNo: string
  representative: string | null
  address: string | null
  businessType: string | null
  businessItem: string | null
}): Promise<void> {
  await apiPut<CompanyProfile>('/api/finance/company-profile', data)
  revalidatePath(PATH)
}
