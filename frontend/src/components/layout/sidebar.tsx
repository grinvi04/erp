'use client'

import Link from 'next/link'
import { usePathname } from 'next/navigation'
import { cn } from '@/lib/utils'
import {
  Users, Building2, Package, TrendingUp, LayoutDashboard,
  ChevronRight, Briefcase, FileText, Warehouse, BarChart3,
  UserSquare, Target, Activity, GitBranch, Inbox,
} from 'lucide-react'

const NAV = [
  {
    label: '대시보드',
    href: '/',
    icon: LayoutDashboard,
  },
  {
    label: '결재함',
    href: '/approvals',
    icon: Inbox,
  },
  {
    label: '인사(HR)',
    icon: Users,
    children: [
      { label: '직원', href: '/hr/employees', icon: Users },
      { label: '부서', href: '/hr/departments', icon: Building2 },
      { label: '휴가 신청', href: '/hr/leave-requests', icon: FileText },
    ],
  },
  {
    label: '재무(Finance)',
    icon: BarChart3,
    children: [
      { label: '계정과목', href: '/finance/accounts', icon: Briefcase },
      { label: '전표', href: '/finance/journal-entries', icon: FileText },
      { label: '매입 인보이스', href: '/finance/invoices', icon: FileText },
      { label: '공급업체', href: '/finance/vendors', icon: Building2 },
    ],
  },
  {
    label: '재고(Inventory)',
    icon: Package,
    children: [
      { label: '품목', href: '/inventory/items', icon: Package },
      { label: '창고', href: '/inventory/warehouses', icon: Warehouse },
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
    ],
  },
]

export function Sidebar() {
  const pathname = usePathname()

  return (
    <aside className="w-60 min-h-screen bg-gray-900 text-gray-100 flex flex-col shrink-0">
      <div className="px-4 py-5 border-b border-gray-700">
        <span className="font-bold text-lg tracking-tight">ERP System</span>
      </div>
      <nav className="flex-1 overflow-y-auto py-4 space-y-1">
        {NAV.map((item) =>
          item.children ? (
            <NavGroup key={item.label} item={item} pathname={pathname} />
          ) : (
            <NavLink key={item.href} href={item.href!} label={item.label} icon={item.icon} pathname={pathname} />
          )
        )}
      </nav>
    </aside>
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
        'flex items-center gap-2 px-4 py-2 text-sm rounded-md mx-2 transition-colors',
        indent && 'pl-8',
        active
          ? 'bg-blue-600 text-white'
          : 'text-gray-300 hover:bg-gray-700 hover:text-white'
      )}
    >
      <Icon className="h-4 w-4 shrink-0" />
      {label}
    </Link>
  )
}

function NavGroup({
  item,
  pathname,
}: {
  item: (typeof NAV)[number] & { children: NonNullable<(typeof NAV)[number]['children']> }
  pathname: string
}) {
  const Icon = item.icon
  const open = item.children.some((c) => pathname.startsWith(c.href))

  return (
    <div>
      <div
        className={cn(
          'flex items-center gap-2 px-4 py-2 mx-2 text-sm rounded-md text-gray-400 select-none',
          open && 'text-gray-200'
        )}
      >
        <Icon className="h-4 w-4 shrink-0" />
        <span className="flex-1 font-medium">{item.label}</span>
        <ChevronRight className={cn('h-3 w-3 transition-transform', open && 'rotate-90')} />
      </div>
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
  )
}
