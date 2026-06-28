import { clsx, type ClassValue } from 'clsx'
import { twMerge } from 'tailwind-merge'

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

/**
 * Keycloak sub(UUID) → 사람 이름. names 맵에 없으면 sub 앞 8자로 짧게 표시한다(전체 UUID는 호출부가 title/tooltip으로).
 * 순수 함수 — 클라이언트 컴포넌트에서도 사용한다.
 */
export function formatUserName(
  sub: string | null | undefined,
  names: Record<string, string>,
): string {
  if (!sub) return '—'
  return names[sub] ?? sub.slice(0, 8)
}
