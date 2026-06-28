'use client'

import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { formatMoneyOne } from '@/lib/money'

export type BarSeries = { key: string; label: string; color: string }
export type MonthlyRow = { month: string } & Record<string, number | string>
// 서버 컴포넌트→클라이언트로 함수는 못 넘기므로 직렬화 가능한 포맷 디스크립터 사용.
export type ValueFormat =
  { kind: 'number' } | { kind: 'money'; currency: string } | { kind: 'suffix'; suffix: string }

/**
 * 월별 세로 막대차트 (recharts) — 단일/다중 시리즈.
 * 색은 차트 토큰(var(--chart-N))을 받아 라이트·다크에 자동 적응.
 */
export function MonthlyBarChart({
  data,
  series,
  valueFormat = { kind: 'number' },
  height = 240,
}: {
  data: MonthlyRow[]
  series: BarSeries[]
  valueFormat?: ValueFormat
  height?: number
}) {
  return (
    <div>
      {series.length > 1 && (
        <div className="mb-3 flex flex-wrap items-center gap-4 text-xs text-muted-foreground">
          {series.map((s) => (
            <span key={s.key} className="flex items-center gap-1.5">
              <span
                className="inline-block h-2.5 w-2.5 rounded-sm"
                style={{ backgroundColor: s.color }}
              />
              {s.label}
            </span>
          ))}
        </div>
      )}
      <ResponsiveContainer width="100%" height={height}>
        <BarChart data={data} margin={{ top: 8, right: 8, left: -12, bottom: 0 }} barGap={2}>
          <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="var(--border)" />
          <XAxis
            dataKey="month"
            tick={{ fontSize: 12, fill: 'var(--muted-foreground)' }}
            axisLine={{ stroke: 'var(--border)' }}
            tickLine={false}
          />
          <YAxis
            tick={{ fontSize: 11, fill: 'var(--muted-foreground)' }}
            axisLine={false}
            tickLine={false}
            width={52}
            tickFormatter={compact}
          />
          <Tooltip
            cursor={{ fill: 'var(--muted)', opacity: 0.45 }}
            content={({ active, label, payload }) => (
              <ChartTooltip
                active={active}
                label={label}
                payload={payload}
                series={series}
                valueFormat={valueFormat}
              />
            )}
          />
          {series.map((s) => (
            <Bar
              key={s.key}
              dataKey={s.key}
              name={s.label}
              fill={s.color}
              radius={[4, 4, 0, 0]}
              maxBarSize={36}
            />
          ))}
        </BarChart>
      </ResponsiveContainer>
    </div>
  )
}

function compact(v: number): string {
  if (Math.abs(v) >= 1_000_000) return `${(v / 1_000_000).toFixed(1)}M`
  if (Math.abs(v) >= 1_000) return `${(v / 1_000).toFixed(0)}K`
  return `${v}`
}

function formatValue(v: number, fmt: ValueFormat): string {
  switch (fmt.kind) {
    case 'money':
      return formatMoneyOne(v, fmt.currency)
    case 'suffix':
      return `${v.toLocaleString('ko-KR')}${fmt.suffix}`
    default:
      return v.toLocaleString('ko-KR')
  }
}

interface TooltipProps {
  active?: boolean
  label?: string | number
  payload?: ReadonlyArray<{ dataKey?: unknown; value?: unknown }>
  series: BarSeries[]
  valueFormat: ValueFormat
}

function ChartTooltip({ active, label, payload, series, valueFormat }: TooltipProps) {
  if (!active || !payload?.length) return null
  return (
    <div className="rounded-lg border border-border bg-popover px-3 py-2 text-xs shadow-md">
      <p className="mb-1 font-medium text-popover-foreground">{label}</p>
      <div className="space-y-0.5">
        {series.map((s) => {
          const item = payload.find((p) => p.dataKey === s.key)
          if (!item) return null
          return (
            <div key={s.key} className="flex items-center justify-between gap-3">
              <span className="flex items-center gap-1.5 text-muted-foreground">
                <span
                  className="inline-block h-2 w-2 rounded-sm"
                  style={{ backgroundColor: s.color }}
                />
                {s.label}
              </span>
              <span className="font-medium tabular-nums text-popover-foreground">
                {formatValue(Number(item.value ?? 0), valueFormat)}
              </span>
            </div>
          )
        })}
      </div>
    </div>
  )
}
