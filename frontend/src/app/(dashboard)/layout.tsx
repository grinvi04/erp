import { auth } from '@/lib/auth'
import { redirect } from 'next/navigation'
import { SessionProvider } from 'next-auth/react'
import { Sidebar } from '@/components/layout/sidebar'
import { Header } from '@/components/layout/header'

export default async function DashboardLayout({ children }: { children: React.ReactNode }) {
  const session = await auth()
  if (!session) redirect('/login')

  return (
    <SessionProvider session={session}>
      <div className="flex min-h-screen">
        <Sidebar />
        <div className="flex flex-col flex-1 min-w-0">
          <Header />
          <main className="flex-1 bg-gray-50 overflow-auto">
            {children}
          </main>
        </div>
      </div>
    </SessionProvider>
  )
}
