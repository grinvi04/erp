'use client'

import * as React from 'react'
import { SearchIcon, RotateCcwIcon } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { cn } from '@/lib/utils'

/**
 * 조회 조건 패널 — 한국 ERP 표준. 라벨-좌측 필드들을 한 줄에 배치하고 우측에 [조회]/[초기화] 버튼.
 * `onSearch`는 폼 제출(조회 버튼·Enter) 시 호출. 필드 상태는 호출부가 관리한다.
 */
export function FilterBar({
  children,
  onSearch,
  onReset,
  className,
}: {
  children: React.ReactNode
  onSearch: () => void
  onReset?: () => void
  className?: string
}) {
  return (
    <form
      onSubmit={(e) => {
        e.preventDefault()
        onSearch()
      }}
      className={cn(
        'flex flex-wrap items-end gap-x-5 gap-y-2.5 rounded-lg border border-border bg-card px-4 py-3 shadow-sm',
        className,
      )}
    >
      {children}
      <div className="ml-auto flex items-center gap-2">
        {onReset && (
          <Button type="button" variant="outline" size="sm" onClick={onReset}>
            <RotateCcwIcon />
            초기화
          </Button>
        )}
        <Button type="submit" size="sm">
          <SearchIcon />
          조회
        </Button>
      </div>
    </form>
  )
}

/** 조회 조건 한 칸 — 라벨(좌측, 회색) + 입력. */
export function FilterField({
  label,
  children,
  className,
}: {
  label: string
  children: React.ReactNode
  className?: string
}) {
  return (
    <div className={cn('flex items-center gap-2', className)}>
      <span className="whitespace-nowrap text-xs font-medium text-muted-foreground">{label}</span>
      {children}
    </div>
  )
}
