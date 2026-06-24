'use client'

import { signIn } from 'next-auth/react'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'

export default function LoginPage() {
  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50">
      <Card className="w-full max-w-sm">
        <CardHeader className="text-center">
          <CardTitle className="text-2xl font-bold">ERP System</CardTitle>
          <CardDescription>Keycloak 계정으로 로그인하세요</CardDescription>
        </CardHeader>
        <CardContent>
          <Button
            className="w-full"
            onClick={() => signIn('keycloak', { callbackUrl: '/' })}
          >
            Keycloak으로 로그인
          </Button>
        </CardContent>
      </Card>
    </div>
  )
}
