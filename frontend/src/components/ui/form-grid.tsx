import * as React from 'react'
import { cn } from '@/lib/utils'

/**
 * 라벨-좌측 폼 그리드 — 한국 ERP 등록/수정 화면 표준. 라벨 셀(우측정렬·회색)과 값 셀을 테두리로 구획한다.
 * `cols`만큼 (라벨|값) 쌍을 한 행에 배치. 값 셀이 한 행을 다 쓰려면 `<FormRow span>`.
 */
export function FormGrid({
  children,
  cols = 2,
  className,
}: {
  children: React.ReactNode
  cols?: 1 | 2
  className?: string
}) {
  return (
    <div
      className={cn(
        'grid overflow-hidden rounded-md border border-border text-sm',
        cols === 2
          ? 'grid-cols-[auto_1fr] sm:grid-cols-[7.5rem_1fr_7.5rem_1fr]'
          : 'grid-cols-[7.5rem_1fr]',
        className,
      )}
    >
      {children}
    </div>
  )
}

/** 라벨 셀 + 값 셀 한 쌍. `span`이면 값 셀이 행 끝까지 늘어난다(2열 그리드 기준). */
export function FormRow({
  label,
  required,
  children,
  span,
}: {
  label: string
  required?: boolean
  children: React.ReactNode
  span?: boolean
}) {
  return (
    <>
      <div className="flex items-center justify-end gap-0.5 border-t border-r border-border bg-muted/30 px-3 py-1.5 text-xs font-medium text-muted-foreground">
        {label}
        {required && <span className="text-destructive">*</span>}
      </div>
      <div
        className={cn(
          'flex items-center border-t border-border px-3 py-1.5',
          span && 'sm:col-span-3',
        )}
      >
        {children}
      </div>
    </>
  )
}
