/**
 * 클라이언트 CSV 다운로드 — 한국 ERP의 "엑셀 다운로드". 엑셀이 한글을 깨지 않도록 UTF-8 BOM을 붙인다.
 * 현재 화면(로드된 페이지) 데이터를 내보낸다. 전체 데이터셋 내보내기는 lib/export.ts 헬퍼 사용.
 */

// 수식 인젝션 방어 — 셀이 = + - @ TAB CR 로 시작하면 엑셀/시트가 수식으로 실행한다(OWASP CSV Injection).
// 선두에 작은따옴표를 붙여 텍스트로 강제한다(team-harness api-standards.md: CSV/엑셀 export 필수).
export function sanitizeCsvValue(s: string): string {
  return /^[=+\-@\t\r]/.test(s) ? `'${s}` : s
}

// 셀 직렬화: (문자열만) 수식 중화 → RFC4180 인용(콤마·따옴표·줄바꿈).
// 숫자는 수식이 될 수 없으므로 중화 제외 — 음수(-1000)가 텍스트로 깨지지 않게 한다.
export function escapeCsvCell(v: string | number | null | undefined): string {
  if (v == null) return ''
  const cell = typeof v === 'number' ? String(v) : sanitizeCsvValue(v)
  // 콤마·따옴표·LF·CR 포함 시 인용(고립 CR도 일부 파서의 행 분리 오인 방지).
  return /[",\n\r]/.test(cell) ? `"${cell.replace(/"/g, '""')}"` : cell
}

// 헤더+행을 CSV 본문(BOM 제외)으로 직렬화. CRLF 구분.
export function toCsv(headers: string[], rows: (string | number | null | undefined)[][]): string {
  return [headers, ...rows].map((r) => r.map(escapeCsvCell).join(',')).join('\r\n')
}

export function downloadCsv(
  filename: string,
  headers: string[],
  rows: (string | number | null | undefined)[][],
) {
  const csv = toCsv(headers, rows)
  const blob = new Blob(['﻿' + csv], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename.endsWith('.csv') ? filename : `${filename}.csv`
  document.body.appendChild(a)
  a.click()
  a.remove()
  URL.revokeObjectURL(url)
}
