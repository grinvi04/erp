'use client'

import * as React from 'react'
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetDescription,
  SheetFooter,
} from '@/components/ui/sheet'
import { cn } from '@/lib/utils'

/**
 * 표준 읽기전용 상세(drill-in) 패널. 목록 행 클릭 → 우측 시트로 헤더·라벨/값·하위 명세를 일관되게 보여준다.
 * 라벨/값은 {@link DetailRow}, 구획은 {@link DetailSection}으로 구성한다.
 */
function DetailSheet({
  open,
  onOpenChange,
  title,
  description,
  children,
  footer,
  className,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
  title: React.ReactNode
  description?: React.ReactNode
  children: React.ReactNode
  footer?: React.ReactNode
  className?: string
}) {
  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent
        side="right"
        className={cn('w-full gap-0 sm:max-w-xl', className)}
      >
        <SheetHeader className="border-b border-border">
          <SheetTitle>{title}</SheetTitle>
          {description ? <SheetDescription>{description}</SheetDescription> : null}
        </SheetHeader>
        <div className="flex-1 overflow-y-auto px-4 py-2">{children}</div>
        {footer ? <SheetFooter className="border-t border-border">{footer}</SheetFooter> : null}
      </SheetContent>
    </Sheet>
  )
}

/** 라벨·값 한 행. 값이 길거나 줄바꿈이 필요하면 children으로 자유 구성. */
function DetailRow({
  label,
  children,
  className,
}: {
  label: React.ReactNode
  children: React.ReactNode
  className?: string
}) {
  return (
    <div
      className={cn(
        'grid grid-cols-[8rem_1fr] items-start gap-3 border-b border-border/50 py-2 last:border-0',
        className,
      )}
    >
      <dt className="text-sm text-muted-foreground">{label}</dt>
      <dd className="text-sm text-foreground">{children}</dd>
    </div>
  )
}

/** 상세 내 구획(제목 + 내용). 제목 없이도 사용 가능. */
function DetailSection({
  title,
  children,
  className,
}: {
  title?: React.ReactNode
  children: React.ReactNode
  className?: string
}) {
  return (
    <section className={cn('py-3', className)}>
      {title ? (
        <h3 className="mb-1.5 text-xs font-medium uppercase tracking-wide text-muted-foreground">
          {title}
        </h3>
      ) : null}
      {children}
    </section>
  )
}

export { DetailSheet, DetailRow, DetailSection }
