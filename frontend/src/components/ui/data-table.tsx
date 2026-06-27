'use client'

import { useMemo, useState } from 'react'
import { ArrowDown, ArrowUp, ChevronsUpDown, X } from 'lucide-react'
import { cn } from '@/lib/utils'
import {
  Table, TableBody, TableCell, TableHead, TableHeader, TableRow,
} from '@/components/ui/table'
import { Checkbox } from '@/components/ui/checkbox'
import { Skeleton } from '@/components/ui/skeleton'
import { EmptyState } from '@/components/ui/empty-state'

export type Column<T> = {
  key: string
  header: React.ReactNode
  cell: (row: T) => React.ReactNode
  /** 정렬 가능하게 하려면 sortValue도 제공 */
  sortable?: boolean
  sortValue?: (row: T) => string | number | null | undefined
  align?: 'left' | 'right' | 'center'
  headerClassName?: string
  cellClassName?: string
}

/**
 * 고급 데이터 테이블 — 컬럼 정렬·행 선택·일괄 액션·로딩 스켈레톤·빈 상태.
 * 정렬은 현재 로드된 행(클라이언트) 기준. 서버 페이지네이션과 함께 쓰면 페이지 단위 정렬.
 */
export function DataTable<T>({
  data,
  columns,
  getRowId,
  selectable = false,
  renderBulkActions,
  onRowClick,
  loading = false,
  skeletonRows = 6,
  empty,
  initialSort,
}: {
  data: T[]
  columns: Column<T>[]
  getRowId: (row: T) => string | number
  selectable?: boolean
  renderBulkActions?: (selected: T[], clear: () => void) => React.ReactNode
  onRowClick?: (row: T) => void
  loading?: boolean
  skeletonRows?: number
  empty?: React.ReactNode
  initialSort?: { key: string; dir: 'asc' | 'desc' }
}) {
  const [sort, setSort] = useState<{ key: string; dir: 'asc' | 'desc' } | null>(initialSort ?? null)
  const [selected, setSelected] = useState<Set<string | number>>(new Set())

  // 데이터(페이지·검색)가 바뀌면 선택 초기화 — 서버 페이지네이션 간 선택 누수 방지.
  // effect 대신 렌더 중 prop 변경 감지(React 권장 "이전 값 저장" 패턴).
  const [prevData, setPrevData] = useState(data)
  if (prevData !== data) {
    setPrevData(data)
    setSelected(new Set())
  }

  const sorted = useMemo(() => {
    if (!sort) return data
    const col = columns.find((c) => c.key === sort.key)
    if (!col?.sortValue) return data
    const sv = col.sortValue
    const arr = [...data].sort((a, b) => {
      const av = sv(a)
      const bv = sv(b)
      if (av == null && bv == null) return 0
      if (av == null) return 1
      if (bv == null) return -1
      if (typeof av === 'number' && typeof bv === 'number') return av - bv
      return String(av).localeCompare(String(bv), 'ko')
    })
    return sort.dir === 'asc' ? arr : arr.reverse()
  }, [data, sort, columns])

  function toggleSort(key: string) {
    setSort((prev) =>
      prev?.key === key
        ? prev.dir === 'asc' ? { key, dir: 'desc' } : null
        : { key, dir: 'asc' },
    )
  }

  const allChecked = sorted.length > 0 && sorted.every((r) => selected.has(getRowId(r)))
  const someChecked = !allChecked && sorted.some((r) => selected.has(getRowId(r)))

  function toggleAll() {
    setSelected((prev) => {
      const n = new Set(prev)
      if (allChecked) sorted.forEach((r) => n.delete(getRowId(r)))
      else sorted.forEach((r) => n.add(getRowId(r)))
      return n
    })
  }
  function toggleRow(id: string | number) {
    setSelected((prev) => {
      const n = new Set(prev)
      if (n.has(id)) n.delete(id)
      else n.add(id)
      return n
    })
  }
  const selectedRows = sorted.filter((r) => selected.has(getRowId(r)))
  const clear = () => setSelected(new Set())
  const colCount = columns.length + (selectable ? 1 : 0)

  return (
    <div className="overflow-hidden rounded-xl border border-border bg-card">
      {selectable && selectedRows.length > 0 && (
        <div className="flex items-center justify-between gap-3 border-b border-border bg-primary/5 px-4 py-2.5">
          <span className="text-sm font-medium text-foreground">{selectedRows.length}개 선택됨</span>
          <div className="flex items-center gap-2">
            {renderBulkActions?.(selectedRows, clear)}
            <button onClick={clear} className="text-muted-foreground transition-colors hover:text-foreground" aria-label="선택 해제">
              <X className="h-4 w-4" />
            </button>
          </div>
        </div>
      )}
      <div className="overflow-x-auto">
        <Table>
          <TableHeader>
            <TableRow className="hover:bg-transparent">
              {selectable && (
                <TableHead className="w-10">
                  <Checkbox
                    checked={allChecked}
                    indeterminate={someChecked}
                    onCheckedChange={() => toggleAll()}
                    aria-label="이 페이지 전체 선택"
                  />
                </TableHead>
              )}
              {columns.map((col) => {
                const isSortable = col.sortable && !!col.sortValue
                const ariaSort = isSortable
                  ? (sort?.key === col.key ? (sort.dir === 'asc' ? 'ascending' : 'descending') : 'none')
                  : undefined
                return (
                <TableHead
                  key={col.key}
                  aria-sort={ariaSort}
                  className={cn(col.align === 'right' && 'text-right', col.align === 'center' && 'text-center', col.headerClassName)}
                >
                  {isSortable ? (
                    <button
                      type="button"
                      onClick={() => toggleSort(col.key)}
                      aria-label={`${typeof col.header === 'string' ? col.header : col.key} 기준 정렬`}
                      className={cn('inline-flex items-center gap-1 font-medium transition-colors hover:text-foreground', col.align === 'right' && 'flex-row-reverse')}
                    >
                      {col.header}
                      {sort?.key === col.key
                        ? (sort.dir === 'asc' ? <ArrowUp className="h-3.5 w-3.5" /> : <ArrowDown className="h-3.5 w-3.5" />)
                        : <ChevronsUpDown className="h-3.5 w-3.5 text-muted-foreground/40" />}
                    </button>
                  ) : (
                    col.header
                  )}
                </TableHead>
                )
              })}
            </TableRow>
          </TableHeader>
          <TableBody>
            {loading ? (
              Array.from({ length: skeletonRows }).map((_, i) => (
                <TableRow key={i} className="hover:bg-transparent">
                  {selectable && <TableCell><Skeleton className="h-4 w-4" /></TableCell>}
                  {columns.map((c) => (
                    <TableCell key={c.key}><Skeleton className="h-4 w-full max-w-[140px]" /></TableCell>
                  ))}
                </TableRow>
              ))
            ) : sorted.length === 0 ? (
              <TableRow className="hover:bg-transparent">
                <TableCell colSpan={colCount} className="p-0">
                  {empty ?? <EmptyState title="데이터가 없습니다" />}
                </TableCell>
              </TableRow>
            ) : (
              sorted.map((row) => {
                const id = getRowId(row)
                const isSel = selected.has(id)
                return (
                  <TableRow
                    key={id}
                    data-state={isSel ? 'selected' : undefined}
                    className={cn(onRowClick && 'cursor-pointer')}
                    onClick={onRowClick ? () => onRowClick(row) : undefined}
                  >
                    {selectable && (
                      <TableCell onClick={(e) => e.stopPropagation()}>
                        <Checkbox checked={isSel} onCheckedChange={() => toggleRow(id)} aria-label="행 선택" />
                      </TableCell>
                    )}
                    {columns.map((col) => (
                      <TableCell
                        key={col.key}
                        className={cn(col.align === 'right' && 'text-right tabular-nums', col.align === 'center' && 'text-center', col.cellClassName)}
                      >
                        {col.cell(row)}
                      </TableCell>
                    ))}
                  </TableRow>
                )
              })
            )}
          </TableBody>
        </Table>
      </div>
    </div>
  )
}
