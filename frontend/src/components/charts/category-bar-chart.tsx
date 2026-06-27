'use client'

import {
  Bar, BarChart, CartesianGrid, LabelList, ResponsiveContainer, XAxis, YAxis,
} from 'recharts'

export type CatDatum = { label: string; value: number; display?: string }

/**
 * 가로 막대차트 (recharts) — 분포/순위(부서·카테고리·창고 등).
 * 막대 끝에 display(또는 value) 라벨. 색은 차트 토큰.
 */
export function CategoryBarChart({
  data,
  color = 'var(--chart-1)',
  height,
  labelWidth = 116,
}: {
  data: CatDatum[]
  color?: string
  height?: number
  labelWidth?: number
}) {
  const h = height ?? Math.max(data.length * 40 + 16, 96)
  return (
    <ResponsiveContainer width="100%" height={h}>
      <BarChart data={data} layout="vertical" margin={{ left: 0, right: 56, top: 4, bottom: 4 }} barCategoryGap={10}>
        <CartesianGrid horizontal={false} strokeDasharray="3 3" stroke="var(--border)" />
        <XAxis type="number" hide />
        <YAxis
          type="category"
          dataKey="label"
          width={labelWidth}
          tick={{ fontSize: 12, fill: 'var(--muted-foreground)' }}
          axisLine={false}
          tickLine={false}
        />
        <Bar dataKey="value" radius={[0, 4, 4, 0]} barSize={18} fill={color} isAnimationActive={false}>
          <LabelList dataKey="display" position="right" fill="var(--muted-foreground)" fontSize={11} />
        </Bar>
      </BarChart>
    </ResponsiveContainer>
  )
}
