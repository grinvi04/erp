import { toast } from 'sonner'
import { downloadCsv } from './csv'

type CsvCell = string | number | null | undefined

/**
 * 전체 데이터셋 CSV 내보내기 공통 처리 — 서버액션(fetcher)으로 전체 행을 받아 화면 조회조건(matches)을
 * 재적용하고 다운로드한다. 상한 초과(truncated) 시 실제 상한(limit)을 toast로 경고한다(조용한 잘림 금지).
 * 호출처가 startTransition·try/catch를 소유한다(컴포넌트 컨텍스트).
 */
export async function runCsvExport<T>(
  fetcher: () => Promise<{ rows: T[]; truncated: boolean; limit: number }>,
  opts: {
    filename: string
    columns: string[]
    matches: (row: T) => boolean
    row: (row: T) => CsvCell[]
  },
): Promise<void> {
  const { rows, truncated, limit } = await fetcher()
  downloadCsv(opts.filename, opts.columns, rows.filter(opts.matches).map(opts.row))
  if (truncated) {
    toast.warning(`데이터가 많아 최대 ${limit.toLocaleString('ko-KR')}건까지만 내보냈습니다`)
  }
}
