import Link from 'next/link'
import { ChevronRight } from 'lucide-react'
import { cn } from '@/lib/utils'

/** 차트/콘텐츠 카드 — 제목·설명·우측 액션(또는 드릴다운 링크) + 본문. */
export function ChartCard({
  title,
  description,
  action,
  href,
  linkLabel = '전체 보기',
  children,
  className,
  bodyClassName,
}: {
  title: React.ReactNode
  description?: React.ReactNode
  action?: React.ReactNode
  href?: string
  linkLabel?: string
  children: React.ReactNode
  className?: string
  bodyClassName?: string
}) {
  return (
    <div className={cn('rounded-xl border border-border bg-card p-5 shadow-xs', className)}>
      <div className="mb-4 flex items-start justify-between gap-3">
        <div className="min-w-0">
          <h2 className="font-semibold text-foreground">{title}</h2>
          {description && <p className="mt-0.5 text-xs text-muted-foreground">{description}</p>}
        </div>
        {action ? (
          <div className="shrink-0">{action}</div>
        ) : href ? (
          <Link
            href={href}
            className="inline-flex shrink-0 items-center gap-0.5 text-sm font-medium text-primary transition-colors hover:text-primary/80"
          >
            {linkLabel}
            <ChevronRight className="h-3.5 w-3.5" />
          </Link>
        ) : null}
      </div>
      <div className={bodyClassName}>{children}</div>
    </div>
  )
}
