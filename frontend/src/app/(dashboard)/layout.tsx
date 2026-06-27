import { auth } from '@/lib/auth'
import { redirect } from 'next/navigation'
import { SessionProvider } from 'next-auth/react'
import { Sidebar } from '@/components/layout/sidebar'
import { Header } from '@/components/layout/header'
import { PermissionsProvider } from '@/components/permissions-provider'
import { getMyPermissions } from '@/lib/api'

export default async function DashboardLayout({ children }: { children: React.ReactNode }) {
  const session = await auth()
  if (!session || session.error === 'RefreshAccessTokenError') redirect('/login')

  const permissions = await getMyPermissions()

  return (
    <SessionProvider session={session}>
      <PermissionsProvider permissions={permissions}>
        <div className="flex min-h-screen">
          <Sidebar />
          <div className="flex flex-col flex-1 min-w-0">
            <Header />
            <main className="flex-1 overflow-auto bg-muted/30">
              {children}
            </main>
          </div>
        </div>
      </PermissionsProvider>
    </SessionProvider>
  )
}
