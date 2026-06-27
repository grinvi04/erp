'use client'

// 루트 레이아웃에서 발생한 에러의 최종 폴백. 루트 레이아웃을 대체하므로
// 자체 html/body 태그와 글로벌 스타일을 직접 포함해야 한다.
import './globals.css'

export default function GlobalError({
  error,
  reset,
}: {
  error: Error & { digest?: string }
  reset: () => void
}) {
  return (
    <html lang="ko">
      <body className="bg-background text-foreground">
        <div className="flex flex-col items-center justify-center min-h-screen gap-4">
          <p className="text-muted-foreground">문제가 발생했습니다. 다시 시도해 주세요.</p>
          {error.digest && (
            <p className="text-xs text-muted-foreground">오류 코드: {error.digest}</p>
          )}
          <button onClick={() => reset()} className="px-4 py-2 border rounded-md text-sm">
            다시 시도
          </button>
        </div>
      </body>
    </html>
  )
}
