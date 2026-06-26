'use client'

import { useEffect, useState } from 'react'
import { usePathname, useRouter, useSearchParams } from 'next/navigation'
import { Input } from '@/components/ui/input'

interface SearchInputProps {
  placeholder?: string
  className?: string
}

/**
 * 목록 텍스트 검색 입력. URL 쿼리 `keyword`를 단일 출처로 사용한다.
 * - 300ms 디바운스 후 router.replace로 keyword 갱신(usePathname 기반)
 * - 검색 시 page=0으로 리셋(out-of-range 방지), 기타 쿼리(size 등)는 보존
 * - keyword가 비면 URL에서 제거
 */
export function SearchInput({ placeholder = '검색', className }: SearchInputProps) {
  const router = useRouter()
  const pathname = usePathname()
  const searchParams = useSearchParams()
  const currentKeyword = searchParams.get('keyword') ?? ''
  const [value, setValue] = useState(currentKeyword)

  useEffect(() => {
    const timer = setTimeout(() => {
      const trimmed = value.trim()
      if (trimmed === currentKeyword) return
      const params = new URLSearchParams(searchParams.toString())
      if (trimmed) params.set('keyword', trimmed)
      else params.delete('keyword')
      params.delete('page') // 검색 변경 시 첫 페이지로
      const qs = params.toString()
      router.replace(qs ? `${pathname}?${qs}` : pathname)
    }, 300)
    return () => clearTimeout(timer)
  }, [value, currentKeyword, pathname, router, searchParams])

  return (
    <Input
      type="search"
      value={value}
      onChange={(e) => setValue(e.target.value)}
      placeholder={placeholder}
      className={className}
    />
  )
}
