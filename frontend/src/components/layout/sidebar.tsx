'use client'

import { useState } from 'react'
import Link from 'next/link'
import { usePathname } from 'next/navigation'
import { cn } from '@/lib/utils'
import {
  Users,
  Building2,
  Package,
  TrendingUp,
  LayoutDashboard,
  ChevronRight,
  Briefcase,
  FileText,
  Warehouse,
  BarChart3,
  UserSquare,
  Target,
  Activity,
  GitBranch,
  Inbox,
  PieChart,
  ScrollText,
  ShieldCheck,
  Boxes,
  MapPin,
  Tags,
  Ruler,
  CalendarDays,
  Coins,
} from 'lucide-react'
import { ScrollArea } from '@/components/ui/scroll-area'
import { usePermissions } from '@/components/permissions-provider'
import { PERM } from '@/lib/permissions'

type NavChild = { label: string; href: string; icon: React.ElementType }
type NavItem = { label: string; href?: string; icon: React.ElementType; children?: NavChild[] }

const TOP: NavItem[] = [
  { label: '대시보드', href: '/', icon: LayoutDashboard },
  { label: '결재함', href: '/approvals', icon: Inbox },
  { label: '분석', href: '/analytics', icon: PieChart },
]

const MODULES: (NavItem & { children: NavChild[] })[] = [
  {
    label: '인사',
    icon: Users,
    children: [
      { label: '직원', href: '/hr/employees', icon: Users },
      { label: '부서', href: '/hr/departments', icon: Building2 },
      { label: '직위', href: '/hr/positions', icon: UserSquare },
      { label: '직급', href: '/hr/job-grades', icon: Briefcase },
      { label: '계약', href: '/hr/contracts', icon: FileText },
      { label: '휴가 신청', href: '/hr/leave-requests', icon: FileText },
      { label: '휴가 정책', href: '/hr/leave-policies', icon: CalendarDays },
      { label: '휴가 잔여', href: '/hr/leave-balances', icon: CalendarDays },
    ],
  },
  {
    label: '재무',
    icon: BarChart3,
    children: [
      { label: '회계기간', href: '/finance/fiscal-years', icon: CalendarDays },
      { label: '계정과목', href: '/finance/accounts', icon: Briefcase },
      { label: '전표', href: '/finance/journal-entries', icon: FileText },
      { label: '매입계산서', href: '/finance/invoices', icon: FileText },
      { label: '공급업체', href: '/finance/vendors', icon: Building2 },
      { label: '고객', href: '/finance/customers', icon: Building2 },
      { label: '매출계산서', href: '/finance/ar-invoices', icon: FileText },
      { label: '세금계산서', href: '/finance/tax-invoices', icon: FileText },
      { label: '재무제표', href: '/finance/reports', icon: BarChart3 },
      { label: '회사정보', href: '/finance/company-profile', icon: Building2 },
      { label: 'FX 설정', href: '/finance/fx', icon: Coins },
    ],
  },
  {
    label: '재고',
    icon: Package,
    children: [
      { label: '품목', href: '/inventory/items', icon: Package },
      { label: '품목 분류', href: '/inventory/item-categories', icon: Tags },
      { label: '단위', href: '/inventory/uoms', icon: Ruler },
      { label: '창고', href: '/inventory/warehouses', icon: Warehouse },
      { label: '로케이션', href: '/inventory/locations', icon: MapPin },
      { label: '재고 현황', href: '/inventory/stocks', icon: Boxes },
      { label: '재고 이동', href: '/inventory/movements', icon: TrendingUp },
    ],
  },
  {
    label: 'CRM',
    icon: TrendingUp,
    children: [
      { label: '고객사', href: '/crm/accounts', icon: Building2 },
      { label: '담당자', href: '/crm/contacts', icon: UserSquare },
      { label: '리드', href: '/crm/leads', icon: Target },
      { label: '영업 기회', href: '/crm/opportunities', icon: TrendingUp },
      { label: '파이프라인 단계', href: '/crm/pipeline-stages', icon: GitBranch },
      { label: '활동', href: '/crm/activities', icon: Activity },
      { label: '영업팀', href: '/crm/sales-teams', icon: Users },
    ],
  },
]

export function Sidebar() {
  return (
    <aside className="hidden lg:flex w-64 shrink-0 flex-col border-r border-sidebar-border">
      <SidebarNav />
    </aside>
  )
}

export function SidebarNav() {
  const pathname = usePathname()
  const { can } = usePermissions()

  return (
    <div className="flex h-full flex-col bg-sidebar text-sidebar-foreground">
      <Brand />
      <ScrollArea className="flex-1">
        <nav className="px-3 py-4">
          <div className="space-y-0.5">
            {TOP.map((item) => (
              <NavLink
                key={item.href}
                href={item.href!}
                label={item.label}
                icon={item.icon}
                pathname={pathname}
              />
            ))}
          </div>

          <SectionLabel>모듈</SectionLabel>
          <div className="space-y-0.5">
            {MODULES.map((item) => (
              <NavGroup key={item.label} item={item} pathname={pathname} />
            ))}
          </div>

          {(can(PERM.IAM_READ) || can(PERM.AUDIT_READ)) && (
            <>
              <SectionLabel>관리</SectionLabel>
              <div className="space-y-0.5">
                {can(PERM.IAM_READ) && (
                  <NavLink href="/iam" label="역할·권한" icon={ShieldCheck} pathname={pathname} />
                )}
                {can(PERM.AUDIT_READ) && (
                  <NavLink href="/audit" label="감사 로그" icon={ScrollText} pathname={pathname} />
                )}
              </div>
            </>
          )}
        </nav>
      </ScrollArea>
    </div>
  )
}

function Brand() {
  return (
    <Link
      href="/"
      className="flex h-16 items-center gap-2.5 border-b border-sidebar-border px-4 transition-colors hover:bg-sidebar-accent/40"
    >
      <span className="flex h-9 w-9 items-center justify-center rounded-lg bg-gradient-to-br from-primary to-chart-5 shadow-sm">
        <Boxes className="h-5 w-5 text-white" />
      </span>
      <span className="flex flex-col leading-none">
        <span className="text-[15px] font-semibold tracking-tight text-sidebar-accent-foreground">
          ERP System
        </span>
        <span className="mt-1 text-[11px] font-medium text-sidebar-foreground/55">
          멀티테넌트 SaaS
        </span>
      </span>
    </Link>
  )
}

function SectionLabel({ children }: { children: React.ReactNode }) {
  return (
    <p className="px-3 pb-1.5 pt-5 text-[11px] font-semibold uppercase tracking-wider text-sidebar-foreground/40">
      {children}
    </p>
  )
}

function NavLink({
  href,
  label,
  icon: Icon,
  pathname,
  indent = false,
}: {
  href: string
  label: string
  icon: React.ElementType
  pathname: string
  indent?: boolean
}) {
  const active = pathname === href || (href !== '/' && pathname.startsWith(href))
  return (
    <Link
      href={href}
      className={cn(
        'group relative flex items-center gap-2.5 rounded-md px-3 py-2 text-sm transition-colors',
        indent && 'pl-9',
        active
          ? 'bg-sidebar-accent font-medium text-sidebar-accent-foreground'
          : 'text-sidebar-foreground/75 hover:bg-sidebar-accent/50 hover:text-sidebar-accent-foreground',
      )}
    >
      {active && (
        <span className="absolute left-0 top-1/2 h-5 w-1 -translate-y-1/2 rounded-r-full bg-sidebar-primary" />
      )}
      <Icon
        className={cn(
          'h-4 w-4 shrink-0',
          active
            ? 'text-sidebar-primary'
            : 'text-sidebar-foreground/55 group-hover:text-sidebar-foreground/80',
        )}
      />
      <span className="truncate">{label}</span>
    </Link>
  )
}

function NavGroup({
  item,
  pathname,
}: {
  item: NavItem & { children: NavChild[] }
  pathname: string
}) {
  const Icon = item.icon
  const hasActive = item.children.some((c) => pathname.startsWith(c.href))
  const [open, setOpen] = useState(hasActive)

  return (
    <div>
      <button
        type="button"
        onClick={() => setOpen((o) => !o)}
        className={cn(
          'flex w-full items-center gap-2.5 rounded-md px-3 py-2 text-sm transition-colors',
          'text-sidebar-foreground/75 hover:bg-sidebar-accent/50 hover:text-sidebar-accent-foreground',
        )}
      >
        <Icon
          className={cn(
            'h-4 w-4 shrink-0',
            hasActive ? 'text-sidebar-primary' : 'text-sidebar-foreground/55',
          )}
        />
        <span className="flex-1 text-left font-medium">{item.label}</span>
        <ChevronRight
          className={cn(
            'h-3.5 w-3.5 text-sidebar-foreground/45 transition-transform duration-200',
            open && 'rotate-90',
          )}
        />
      </button>
      {open && (
        <div className="mt-0.5 space-y-0.5">
          {item.children.map((child) => (
            <NavLink
              key={child.href}
              href={child.href}
              label={child.label}
              icon={child.icon}
              pathname={pathname}
              indent
            />
          ))}
        </div>
      )}
    </div>
  )
}
