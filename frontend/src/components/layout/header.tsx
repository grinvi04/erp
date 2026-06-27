'use client'

import { signOut, useSession } from 'next-auth/react'
import { useTheme } from 'next-themes'
import { Menu, Search, Bell, Sun, Moon, LogOut, UserRound } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Separator } from '@/components/ui/separator'
import { Avatar, AvatarFallback } from '@/components/ui/avatar'
import { Sheet, SheetContent, SheetTitle, SheetTrigger } from '@/components/ui/sheet'
import {
  DropdownMenu, DropdownMenuContent, DropdownMenuItem,
  DropdownMenuLabel, DropdownMenuSeparator, DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { SidebarNav } from '@/components/layout/sidebar'

export function Header() {
  const { data: session } = useSession()
  const email = session?.user?.email ?? ''
  const initials = email ? email.slice(0, 2).toUpperCase() : 'ER'

  return (
    <header className="sticky top-0 z-30 flex h-16 shrink-0 items-center gap-3 border-b border-border bg-card/80 px-4 backdrop-blur-md lg:px-6">
      {/* 모바일 네비 드로어 */}
      <Sheet>
        <SheetTrigger
          render={<Button variant="ghost" size="icon" className="lg:hidden" aria-label="메뉴 열기" />}
        >
          <Menu className="h-5 w-5" />
        </SheetTrigger>
        <SheetContent side="left" className="w-64 border-sidebar-border p-0">
          <SheetTitle className="sr-only">메뉴</SheetTitle>
          <SidebarNav />
        </SheetContent>
      </Sheet>

      {/* 검색 트리거 */}
      <button
        type="button"
        className="group flex h-9 w-full max-w-xs items-center gap-2 rounded-lg border border-border bg-muted/40 px-3 text-sm text-muted-foreground transition-colors hover:bg-muted"
      >
        <Search className="h-4 w-4" />
        <span className="flex-1 text-left">검색…</span>
        <kbd className="hidden items-center gap-0.5 rounded border border-border bg-background px-1.5 font-mono text-[10px] font-medium text-muted-foreground sm:inline-flex">
          ⌘K
        </kbd>
      </button>

      <div className="flex-1" />

      <ThemeToggle />

      <Button variant="ghost" size="icon" className="relative" aria-label="알림">
        <Bell className="h-[18px] w-[18px]" />
        <span className="absolute right-2 top-2 h-2 w-2 rounded-full bg-destructive ring-2 ring-card" />
      </Button>

      <Separator orientation="vertical" className="h-6" />

      <DropdownMenu>
        <DropdownMenuTrigger
          className="flex items-center gap-2 rounded-lg py-1 pl-1 pr-2 outline-none transition-colors hover:bg-muted focus-visible:ring-2 focus-visible:ring-ring"
          aria-label="계정"
        >
          <Avatar className="h-8 w-8">
            <AvatarFallback className="bg-primary/10 text-xs font-semibold text-primary">{initials}</AvatarFallback>
          </Avatar>
          <span className="hidden max-w-[160px] truncate text-sm font-medium md:inline">{email}</span>
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end" className="w-60">
          <DropdownMenuLabel className="flex items-center gap-2.5 py-2">
            <Avatar className="h-9 w-9">
              <AvatarFallback className="bg-primary/10 text-xs font-semibold text-primary">{initials}</AvatarFallback>
            </Avatar>
            <div className="min-w-0">
              <p className="truncate text-sm font-medium">{email || '사용자'}</p>
              <p className="text-xs font-normal text-muted-foreground">로그인됨</p>
            </div>
          </DropdownMenuLabel>
          <DropdownMenuSeparator />
          <DropdownMenuItem disabled>
            <UserRound className="mr-2 h-4 w-4" /> 내 프로필
          </DropdownMenuItem>
          <DropdownMenuSeparator />
          <DropdownMenuItem
            onClick={() => signOut({ callbackUrl: '/login' })}
            className="text-destructive focus:text-destructive"
          >
            <LogOut className="mr-2 h-4 w-4" /> 로그아웃
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>
    </header>
  )
}

function ThemeToggle() {
  const { setTheme, resolvedTheme } = useTheme()
  return (
    <Button
      variant="ghost"
      size="icon"
      onClick={() => setTheme(resolvedTheme === 'dark' ? 'light' : 'dark')}
      aria-label="테마 전환"
    >
      <Sun className="hidden h-[18px] w-[18px] dark:block" />
      <Moon className="h-[18px] w-[18px] dark:hidden" />
    </Button>
  )
}
