import Link from 'next/link'
import { buttonVariants } from '@/components/ui/button'

export default function NotFound() {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-4 bg-background text-foreground">
      <h2 className="text-2xl font-semibold">404</h2>
      <p className="text-muted-foreground">요청하신 페이지를 찾을 수 없습니다.</p>
      <Link href="/" className={buttonVariants({ variant: 'outline' })}>
        홈으로 이동
      </Link>
    </div>
  )
}
