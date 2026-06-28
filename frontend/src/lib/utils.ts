import { clsx, type ClassValue } from 'clsx'
import { twMerge } from 'tailwind-merge'

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

const DATE_FMT = new Intl.DateTimeFormat('ko-KR', {
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
})
const DATE_TIME_FMT = new Intl.DateTimeFormat('ko-KR', {
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
  hour: '2-digit',
  minute: '2-digit',
})

/** ISO 문자열 → ko-KR 날짜(YYYY. MM. DD.). 빈값·잘못된 값은 '—'. */
export function formatDate(iso: string | null | undefined): string {
  if (!iso) return '—'
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return '—'
  return DATE_FMT.format(d)
}

/** ISO 문자열 → ko-KR 날짜·시각(YYYY. MM. DD. HH:MM). 빈값·잘못된 값은 '—'. */
export function formatDateTime(iso: string | null | undefined): string {
  if (!iso) return '—'
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return '—'
  return DATE_TIME_FMT.format(d)
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
