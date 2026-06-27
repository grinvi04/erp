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
      <p className="text-muted-foreground">문제가 발생했습니다. 다시 시도해 주세요.</p>
      {error.digest && (
        <p className="text-xs text-muted-foreground">오류 코드: {error.digest}</p>
      )}
      <Button variant="outline" onClick={reset}>
        다시 시도
      </Button>
    </div>
  )
}
