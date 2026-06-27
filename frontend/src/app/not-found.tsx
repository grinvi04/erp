import Link from 'next/link'
import { buttonVariants } from '@/components/ui/button'

export default function NotFound() {
  return (
    <div className="flex flex-col items-center justify-center min-h-screen gap-4">
      <h2 className="text-2xl font-semibold">404</h2>
      <p className="text-gray-500">요청하신 페이지를 찾을 수 없습니다.</p>
      <Link href="/" className={buttonVariants({ variant: 'outline' })}>
        홈으로 이동
      </Link>
    </div>
  )
}
