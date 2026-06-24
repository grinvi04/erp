import Link from 'next/link'
import { ChevronLeft, ChevronRight } from 'lucide-react'
import { cn } from '@/lib/utils'

interface PaginationBarProps {
  page: number
  totalPages: number
  totalElements: number
  size: number
  basePath: string
  searchParams?: Record<string, string>
}

function pageHref(basePath: string, page: number, size: number, extra: Record<string, string>) {
  const params = new URLSearchParams({ ...extra, page: String(page), size: String(size) })
  return `${basePath}?${params.toString()}`
}

const NavBtn = ({
  href,
  disabled,
  children,
}: {
  href: string
  disabled: boolean
  children: React.ReactNode
}) =>
  disabled ? (
    <span
      className={cn(
        'inline-flex items-center justify-center size-8 rounded-lg text-sm text-gray-300 cursor-not-allowed'
      )}
    >
      {children}
    </span>
  ) : (
    <Link
      href={href}
      className="inline-flex items-center justify-center size-8 rounded-lg text-sm text-gray-600 hover:bg-gray-100 transition-colors"
    >
      {children}
    </Link>
  )

export function PaginationBar({
  page,
  totalPages,
  totalElements,
  size,
  basePath,
  searchParams = {},
}: PaginationBarProps) {
  const start = totalElements === 0 ? 0 : page * size + 1
  const end = Math.min((page + 1) * size, totalElements)

  return (
    <div className="flex items-center justify-between px-4 py-3 border-t text-sm text-gray-600">
      <span>
        {totalElements > 0 ? `${start}–${end} / 전체 ${totalElements}건` : '0건'}
      </span>
      <div className="flex items-center gap-1">
        <NavBtn
          href={pageHref(basePath, page - 1, size, searchParams)}
          disabled={page === 0}
        >
          <ChevronLeft className="h-4 w-4" />
        </NavBtn>
        <span className="px-2 text-sm">
          {page + 1} / {Math.max(totalPages, 1)}
        </span>
        <NavBtn
          href={pageHref(basePath, page + 1, size, searchParams)}
          disabled={page >= totalPages - 1}
        >
          <ChevronRight className="h-4 w-4" />
        </NavBtn>
      </div>
    </div>
  )
}
