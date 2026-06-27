'use client'

import { signIn } from 'next-auth/react'
import { Boxes } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'

export default function LoginPage() {
  return (
    <div className="relative flex min-h-screen items-center justify-center overflow-hidden bg-muted/30 px-4">
      {/* 은은한 브랜드 글로우 배경 */}
      <div className="pointer-events-none absolute -top-32 left-1/2 h-80 w-[36rem] -translate-x-1/2 rounded-full bg-primary/10 blur-3xl" />
      <Card className="relative w-full max-w-sm shadow-lg">
        <CardHeader className="items-center text-center">
          <span className="mb-2 flex h-12 w-12 items-center justify-center rounded-xl bg-gradient-to-br from-primary to-chart-5 shadow-sm">
            <Boxes className="h-6 w-6 text-white" />
          </span>
          <CardTitle className="text-2xl font-semibold tracking-tight">ERP System</CardTitle>
          <CardDescription>멀티테넌트 SaaS · Keycloak 계정으로 로그인</CardDescription>
        </CardHeader>
        <CardContent>
          <Button className="w-full" onClick={() => signIn('keycloak', { callbackUrl: '/' })}>
            Keycloak으로 로그인
          </Button>
        </CardContent>
      </Card>
    </div>
  )
}
