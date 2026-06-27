import { cn } from '@/lib/utils'

/** 차트/콘텐츠 카드 — 제목·설명·우측 액션 슬롯 + 본문. */
export function ChartCard({
  title,
  description,
  action,
  children,
  className,
  bodyClassName,
}: {
  title: React.ReactNode
  description?: React.ReactNode
  action?: React.ReactNode
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
        {action && <div className="shrink-0">{action}</div>}
      </div>
      <div className={bodyClassName}>{children}</div>
    </div>
  )
}
