'use client'

import { useMemo, useState } from 'react'
import { useRouter } from 'next/navigation'
import { Search, CornerDownLeft } from 'lucide-react'
import { cn } from '@/lib/utils'
import { Dialog, DialogContent, DialogTitle } from '@/components/ui/dialog'

type Route = { label: string; href: string; group?: string }

const ROUTES: Route[] = [
  { label: '대시보드', href: '/' },
  { label: '결재함', href: '/approvals' },
  { label: '분석', href: '/analytics' },
  { label: '직원', href: '/hr/employees', group: '인사' },
  { label: '부서', href: '/hr/departments', group: '인사' },
  { label: '직위', href: '/hr/positions', group: '인사' },
  { label: '직급', href: '/hr/job-grades', group: '인사' },
  { label: '계약', href: '/hr/contracts', group: '인사' },
  { label: '휴가 신청', href: '/hr/leave-requests', group: '인사' },
  { label: '계정과목', href: '/finance/accounts', group: '재무' },
  { label: '전표', href: '/finance/journal-entries', group: '재무' },
  { label: '매입계산서', href: '/finance/invoices', group: '재무' },
  { label: '공급업체', href: '/finance/vendors', group: '재무' },
  { label: '고객', href: '/finance/customers', group: '재무' },
  { label: '매출계산서', href: '/finance/ar-invoices', group: '재무' },
  { label: '재무제표', href: '/finance/reports', group: '재무' },
  { label: 'FX 설정', href: '/finance/fx', group: '재무' },
  { label: '품목', href: '/inventory/items', group: '재고' },
  { label: '창고', href: '/inventory/warehouses', group: '재고' },
  { label: '재고 현황', href: '/inventory/stocks', group: '재고' },
  { label: '재고 이동', href: '/inventory/movements', group: '재고' },
  { label: '고객사', href: '/crm/accounts', group: 'CRM' },
  { label: '리드', href: '/crm/leads', group: 'CRM' },
  { label: '영업 기회', href: '/crm/opportunities', group: 'CRM' },
  { label: '활동', href: '/crm/activities', group: 'CRM' },
  { label: '역할·권한', href: '/iam', group: '관리' },
  { label: '감사 로그', href: '/audit', group: '관리' },
]

export function CommandPalette({
  open,
  onOpenChange,
}: {
  open: boolean
  onOpenChange: (o: boolean) => void
}) {
  const router = useRouter()
  const [q, setQ] = useState('')
  const [active, setActive] = useState(0)

  // 어떤 경로로 닫히든(⌘K 토글 포함) 검색어·선택을 초기화 — effect 대신 렌더 중 open 변화 감지.
  const [prevOpen, setPrevOpen] = useState(open)
  if (prevOpen !== open) {
    setPrevOpen(open)
    if (!open) {
      setQ('')
      setActive(0)
    }
  }

  const results = useMemo(() => {
    const t = q.trim().toLowerCase()
    if (!t) return ROUTES
    return ROUTES.filter(
      (r) => r.label.toLowerCase().includes(t) || (r.group?.toLowerCase().includes(t) ?? false),
    )
  }, [q])

  function close(next: boolean) {
    onOpenChange(next)
    if (!next) {
      setQ('')
      setActive(0)
    }
  }
  function go(href: string) {
    close(false)
    router.push(href)
  }

  return (
    <Dialog open={open} onOpenChange={close}>
      <DialogContent className="sm:max-w-lg gap-0 overflow-hidden p-0" showCloseButton={false}>
        <DialogTitle className="sr-only">페이지 검색</DialogTitle>
        <div className="flex items-center gap-2 border-b border-border px-3">
          <Search className="h-4 w-4 shrink-0 text-muted-foreground" />
          <input
            autoFocus
            value={q}
            onChange={(e) => {
              setQ(e.target.value)
              setActive(0)
            }}
            onKeyDown={(e) => {
              if (e.key === 'ArrowDown') {
                e.preventDefault()
                setActive((a) => Math.min(a + 1, results.length - 1))
              } else if (e.key === 'ArrowUp') {
                e.preventDefault()
                setActive((a) => Math.max(a - 1, 0))
              } else if (e.key === 'Enter' && results[active]) {
                e.preventDefault()
                go(results[active].href)
              }
            }}
            placeholder="페이지 검색…"
            className="h-12 flex-1 bg-transparent text-sm outline-none placeholder:text-muted-foreground"
            aria-label="페이지 검색"
          />
        </div>
        <ul className="max-h-80 overflow-y-auto p-2">
          {results.length === 0 ? (
            <li className="px-3 py-8 text-center text-sm text-muted-foreground">결과가 없습니다</li>
          ) : (
            results.map((r, i) => (
              <li key={r.href}>
                <button
                  type="button"
                  onMouseMove={() => setActive(i)}
                  onClick={() => go(r.href)}
                  className={cn(
                    'flex w-full items-center justify-between gap-2 rounded-md px-3 py-2 text-sm transition-colors',
                    i === active ? 'bg-accent text-accent-foreground' : 'text-foreground',
                  )}
                >
                  <span>{r.label}</span>
                  <span className="flex items-center gap-2">
                    {r.group && <span className="text-xs text-muted-foreground">{r.group}</span>}
                    {i === active && (
                      <CornerDownLeft className="h-3.5 w-3.5 text-muted-foreground" />
                    )}
                  </span>
                </button>
              </li>
            ))
          )}
        </ul>
      </DialogContent>
    </Dialog>
  )
}
