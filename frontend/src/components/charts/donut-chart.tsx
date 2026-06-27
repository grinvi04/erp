'use client'

import { Cell, Pie, PieChart, ResponsiveContainer, Tooltip } from 'recharts'
import { formatMoneyOne } from '@/lib/money'

export type DonutDatum = { label: string; value: number; color?: string }
export type ValueFormat =
  | { kind: 'number' }
  | { kind: 'money'; currency: string }
  | { kind: 'suffix'; suffix: string }

const PALETTE = ['var(--chart-1)', 'var(--chart-2)', 'var(--chart-3)', 'var(--chart-4)', 'var(--chart-5)']

/**
 * 도넛 차트 (recharts) + 범례 리스트. 분포(파이프라인 단계·리드 상태 등)에.
 * 중앙에 합계 라벨 표시. 색은 차트 토큰 — 라이트/다크 자동.
 */
export function DonutChart({
  data,
  valueFormat = { kind: 'number' },
  size = 168,
  centerLabel,
}: {
  data: DonutDatum[]
  valueFormat?: ValueFormat
  size?: number
  centerLabel?: string
}) {
  const total = data.reduce((s, d) => s + d.value, 0)
  const colored = data.map((d, i) => ({ ...d, color: d.color ?? PALETTE[i % PALETTE.length] }))

  return (
    <div className="flex flex-col items-center gap-5 sm:flex-row sm:gap-6">
      <div className="relative shrink-0" style={{ width: size, height: size }}>
        <ResponsiveContainer width="100%" height="100%">
          <PieChart>
            <Pie
              data={total > 0 ? colored : [{ label: '없음', value: 1, color: 'var(--muted)' }]}
              dataKey="value"
              nameKey="label"
              innerRadius="64%"
              outerRadius="100%"
              paddingAngle={total > 0 ? 2 : 0}
              stroke="var(--card)"
              strokeWidth={2}
              isAnimationActive={false}
            >
              {(total > 0 ? colored : [{ color: 'var(--muted)' }]).map((d, i) => (
                <Cell key={i} fill={d.color} />
              ))}
            </Pie>
            {total > 0 && (
              <Tooltip
                cursor={false}
                content={({ active, payload }) => <DonutTooltip active={active} payload={payload} valueFormat={valueFormat} />}
              />
            )}
          </PieChart>
        </ResponsiveContainer>
        <div className="pointer-events-none absolute inset-0 flex flex-col items-center justify-center">
          <span className="text-xl font-semibold tabular-nums text-foreground">{total.toLocaleString('ko-KR')}</span>
          {centerLabel && <span className="text-[11px] text-muted-foreground">{centerLabel}</span>}
        </div>
      </div>
      <ul className="w-full flex-1 space-y-2 text-sm">
        {colored.map((d) => (
          <li key={d.label} className="flex items-center justify-between gap-2">
            <span className="flex min-w-0 items-center gap-2 text-muted-foreground">
              <span className="h-2.5 w-2.5 shrink-0 rounded-sm" style={{ backgroundColor: d.color }} />
              <span className="truncate">{d.label}</span>
            </span>
            <span className="shrink-0 font-medium tabular-nums text-foreground">{formatValue(d.value, valueFormat)}</span>
          </li>
        ))}
      </ul>
    </div>
  )
}

function formatValue(v: number, fmt: ValueFormat): string {
  switch (fmt.kind) {
    case 'money': return formatMoneyOne(v, fmt.currency)
    case 'suffix': return `${v.toLocaleString('ko-KR')}${fmt.suffix}`
    default: return v.toLocaleString('ko-KR')
  }
}

function DonutTooltip({
  active, payload, valueFormat,
}: {
  active?: boolean
  payload?: ReadonlyArray<{ payload?: { label?: string; value?: number; color?: string } }>
  valueFormat: ValueFormat
}) {
  if (!active || !payload?.length) return null
  const d = payload[0]?.payload
  if (!d) return null
  return (
    <div className="rounded-lg border border-border bg-popover px-3 py-2 text-xs shadow-md">
      <div className="flex items-center gap-2">
        <span className="h-2 w-2 rounded-sm" style={{ backgroundColor: d.color }} />
        <span className="text-muted-foreground">{d.label}</span>
        <span className="ml-1 font-medium tabular-nums text-popover-foreground">{formatValue(d.value ?? 0, valueFormat)}</span>
      </div>
    </div>
  )
}
