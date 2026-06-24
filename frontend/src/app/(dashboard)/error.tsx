'use client'

import { useEffect } from 'react'
import { Button } from '@/components/ui/button'

export default function DashboardError({
  error,
  reset,
}: {
  error: Error & { digest?: string }
  reset: () => void
}) {
  useEffect(() => {
    console.error(error)
  }, [error])

  return (
    <div className="flex flex-col items-center justify-center h-64 gap-4">
      <p className="text-gray-500">데이터를 불러오는 중 오류가 발생했습니다.</p>
      <p className="text-sm text-red-500">{error.message}</p>
      <Button variant="outline" onClick={reset}>
        다시 시도
      </Button>
    </div>
  )
}
