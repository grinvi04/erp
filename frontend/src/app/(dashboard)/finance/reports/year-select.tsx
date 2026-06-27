'use client'

import { useRouter } from 'next/navigation'
import { useTransition } from 'react'
import {
  Select, SelectContent, SelectItem, SelectTrigger, SelectValue,
} from '@/components/ui/select'

/** 회계연도 선택 — searchParam `year`를 갱신해 서버 컴포넌트를 재조회한다. */
export default function YearSelect({ year, years }: { year: number; years: number[] }) {
  const router = useRouter()
  const [pending, startTransition] = useTransition()

  return (
    <Select
      value={String(year)}
      onValueChange={(v) => startTransition(() => router.push(`/finance/reports?year=${v}`))}
    >
      <SelectTrigger className="w-32 bg-card" disabled={pending}>
        <SelectValue />
      </SelectTrigger>
      <SelectContent>
        {years.map((y) => (
          <SelectItem key={y} value={String(y)}>
            {y}년
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  )
}
