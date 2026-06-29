'use server'
import { apiGet, apiPost, apiPut } from '@/lib/api'
import { revalidatePath } from 'next/cache'
import type {
  DepreciationAccounts,
  DepreciationEntry,
  DepreciationMethod,
  DepreciationRunResult,
  FixedAsset,
} from '@/types/finance'

const PATH = '/finance/fixed-assets'

export async function createFixedAsset(data: {
  code: string
  name: string
  acquisitionDate: string
  acquisitionCost: number
  residualValue: number
  usefulLifeMonths: number
  method: DepreciationMethod
  decliningAnnualRate: number | null
  assetAccountId: number
}): Promise<void> {
  await apiPost<FixedAsset>('/api/finance/fixed-assets', data)
  revalidatePath(PATH)
}

export async function disposeFixedAsset(
  id: number,
  data: { disposalDate: string; proceeds: number; proceedsAccountId: number | null },
): Promise<void> {
  await apiPost<FixedAsset>(`/api/finance/fixed-assets/${id}/dispose`, data)
  revalidatePath(PATH)
}

export async function runDepreciation(fiscalPeriodId: number): Promise<DepreciationRunResult> {
  const result = await apiPost<DepreciationRunResult>('/api/finance/fixed-assets/depreciation-run', {
    fiscalPeriodId,
  })
  revalidatePath(PATH)
  return result
}

export async function updateDepreciationAccounts(data: DepreciationAccounts): Promise<void> {
  await apiPut<DepreciationAccounts>('/api/finance/fixed-assets/depreciation-accounts', data)
  revalidatePath(PATH)
}

export async function getDepreciationHistory(id: number): Promise<DepreciationEntry[]> {
  return apiGet<DepreciationEntry[]>(`/api/finance/fixed-assets/${id}/depreciation`)
}
