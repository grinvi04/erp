import Link from 'next/link'
import { ArrowDownRight, ArrowUpRight, ChevronRight } from 'lucide-react'
import { cn } from '@/lib/utils'

export type StatTone = 'default' | 'primary' | 'success' | 'warning' | 'destructive'

const CHIP: Record<StatTone, string> = {
  default: 'bg-muted text-foreground/70',
  primary: 'bg-primary/10 text-primary',
  success: 'bg-success/10 text-success',
  warning: 'bg-warning/15 text-warning',
  destructive: 'bg-destructive/10 text-destructive',
}

/**
 * KPI 카드 — 라벨·값·보조라인·틴티드 아이콘 칩·옵션 추세 배지·옵션 링크.
 * 값은 tabular-nums(등폭)로 정렬, 카드는 hover 시 살짝 떠오름.
 */
export function StatCard({
  label,
  value,
  sub,
  icon: Icon,
  tone = 'default',
  trend,
  href,
  className,
}: {
  label: React.ReactNode
  value: React.ReactNode
  sub?: React.ReactNode
  icon?: React.ElementType
  tone?: StatTone
  trend?: { dir: 'up' | 'down'; value: string; good?: boolean }
  href?: string
  className?: string
}) {
  const body = (
    <div
      className={cn(
        'group relative flex h-full flex-col rounded-xl border border-border bg-card p-5 shadow-xs transition-all',
        href && 'hover:-translate-y-0.5 hover:border-primary/30 hover:shadow-md',
        className,
      )}
    >
      <div className="flex items-start justify-between gap-3">
        <span className="text-sm font-medium text-muted-foreground">{label}</span>
        {Icon && (
          <span
            className={cn(
              'flex h-9 w-9 shrink-0 items-center justify-center rounded-lg',
              CHIP[tone],
            )}
          >
            <Icon className="h-[18px] w-[18px]" />
          </span>
        )}
      </div>
      <div className="mt-3 flex flex-wrap items-baseline gap-x-2 gap-y-1">
        <span className="text-2xl font-semibold tracking-tight text-foreground tabular-nums">
          {value}
        </span>
        {trend && <TrendBadge {...trend} />}
      </div>
      {sub && <p className="mt-1 text-xs text-muted-foreground tabular-nums">{sub}</p>}
      {href && (
        <ChevronRight className="absolute bottom-4 right-4 h-4 w-4 text-muted-foreground/0 transition-colors group-hover:text-muted-foreground/60" />
      )}
    </div>
  )

  if (href) {
    return (
      <Link
        href={href}
        className="block focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 rounded-xl"
      >
        {body}
      </Link>
    )
  }
  return body
}

function TrendBadge({ dir, value, good }: { dir: 'up' | 'down'; value: string; good?: boolean }) {
  const positive = good ?? dir === 'up'
  const Icon = dir === 'up' ? ArrowUpRight : ArrowDownRight
  return (
    <span
      className={cn(
        'inline-flex items-center gap-0.5 rounded-full px-1.5 py-0.5 text-xs font-medium tabular-nums',
        positive ? 'bg-success/10 text-success' : 'bg-destructive/10 text-destructive',
      )}
    >
      <Icon className="h-3 w-3" />
      {value}
    </span>
  )
}
