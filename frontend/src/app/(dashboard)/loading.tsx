import { Skeleton } from '@/components/ui/skeleton'

// 라우트 레벨 스트리밍 폴백 — no-store 페칭 중 즉시 스켈레톤을 보여준다.
export default function Loading() {
  return (
    <div className="p-6 space-y-4">
      <Skeleton className="h-8 w-48" />
      <Skeleton className="h-64 w-full" />
    </div>
  )
}
