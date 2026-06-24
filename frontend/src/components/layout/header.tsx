'use client'

import { signOut, useSession } from 'next-auth/react'
import { Button } from '@/components/ui/button'
import { LogOut, User } from 'lucide-react'

export function Header() {
  const { data: session } = useSession()

  return (
    <header className="h-14 border-b bg-white flex items-center justify-between px-6 shrink-0">
      <div />
      <div className="flex items-center gap-3">
        <User className="h-4 w-4 text-gray-400" />
        <span className="text-sm text-gray-700">{session?.user?.email ?? ''}</span>
        <Button
          variant="ghost"
          size="sm"
          onClick={() => signOut({ callbackUrl: '/login' })}
          className="gap-1 text-gray-500"
        >
          <LogOut className="h-4 w-4" />
          로그아웃
        </Button>
      </div>
    </header>
  )
}
