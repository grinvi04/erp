'use client'
import { useState, useTransition } from 'react'
import { toast } from 'sonner'
import { usePermissions } from '@/components/permissions-provider'
import { PERM } from '@/lib/permissions'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { PageHeader } from '@/components/ui/page-header'
import { FormGrid, FormRow } from '@/components/ui/form-grid'
import { updateCompanyProfile } from './actions'
import type { CompanyProfile } from '@/types/finance'

export default function CompanyProfileClient({ profile }: { profile: CompanyProfile }) {
  const { can } = usePermissions()
  const canWrite = can(PERM.FINANCE_SETTING_WRITE)
  const [isPending, startTransition] = useTransition()

  const [companyName, setCompanyName] = useState(profile.companyName ?? '')
  const [businessNo, setBusinessNo] = useState(profile.businessNo ?? '')
  const [representative, setRepresentative] = useState(profile.representative ?? '')
  const [address, setAddress] = useState(profile.address ?? '')
  const [businessType, setBusinessType] = useState(profile.businessType ?? '')
  const [businessItem, setBusinessItem] = useState(profile.businessItem ?? '')

  const handleSave = () => {
    if (!companyName.trim() || !businessNo.trim()) {
      toast.error('상호와 사업자등록번호는 필수입니다')
      return
    }
    startTransition(async () => {
      try {
        await updateCompanyProfile({
          companyName: companyName.trim(),
          businessNo: businessNo.trim(),
          representative: representative || null,
          address: address || null,
          businessType: businessType || null,
          businessItem: businessItem || null,
        })
        toast.success('회사정보가 저장되었습니다')
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '저장 중 오류가 발생했습니다')
      }
    })
  }

  return (
    <div className="p-5">
      <PageHeader
        title="회사정보"
        description="전자세금계산서 공급자 정보 — 발행 시 이 정보가 공급자 스냅샷으로 기록됩니다"
        className="mb-4"
      />

      <div className="mb-4 rounded-lg border bg-card p-5">
        <FormGrid>
          <FormRow label="상호" required>
            <Input
              value={companyName}
              disabled={!canWrite}
              onChange={(e) => setCompanyName(e.target.value)}
              placeholder="(주)무역상사"
              className="h-8"
            />
          </FormRow>
          <FormRow label="사업자등록번호" required>
            <Input
              value={businessNo}
              disabled={!canWrite}
              onChange={(e) => setBusinessNo(e.target.value)}
              placeholder="000-00-00000"
              className="h-8"
            />
          </FormRow>
          <FormRow label="대표자">
            <Input
              value={representative}
              disabled={!canWrite}
              onChange={(e) => setRepresentative(e.target.value)}
              className="h-8"
            />
          </FormRow>
          <FormRow label="주소" span>
            <Input
              value={address}
              disabled={!canWrite}
              onChange={(e) => setAddress(e.target.value)}
              className="h-8"
            />
          </FormRow>
          <FormRow label="업태">
            <Input
              value={businessType}
              disabled={!canWrite}
              onChange={(e) => setBusinessType(e.target.value)}
              placeholder="예: 도소매"
              className="h-8"
            />
          </FormRow>
          <FormRow label="종목">
            <Input
              value={businessItem}
              disabled={!canWrite}
              onChange={(e) => setBusinessItem(e.target.value)}
              placeholder="예: 전자제품"
              className="h-8"
            />
          </FormRow>
        </FormGrid>
        {canWrite && (
          <div className="mt-4 flex justify-end">
            <Button onClick={handleSave} disabled={isPending}>
              저장
            </Button>
          </div>
        )}
      </div>
    </div>
  )
}
