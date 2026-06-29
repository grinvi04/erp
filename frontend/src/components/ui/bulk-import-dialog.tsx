'use client'
import { useRef, useState, useTransition } from 'react'
import { useRouter } from 'next/navigation'
import { toast } from 'sonner'
import { DownloadIcon, UploadIcon } from 'lucide-react'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from '@/components/ui/dialog'
import type { BulkImportResult } from '@/types/api'

/**
 * 재사용 CSV 대량 업로드 다이얼로그 — 템플릿 다운로드 · 파일 선택 · 업로드 · 결과(생성 건수 / 실패 행 목록). 엔티티별 서버 액션을 props로 주입한다.
 */
export function BulkImportDialog({
  open,
  onOpenChange,
  title,
  templateFilename,
  uploadAction,
  templateAction,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
  title: string
  templateFilename: string
  uploadAction: (form: FormData) => Promise<BulkImportResult>
  templateAction: () => Promise<string>
}) {
  const router = useRouter()
  const [isPending, startTransition] = useTransition()
  const [result, setResult] = useState<BulkImportResult | null>(null)
  const fileRef = useRef<HTMLInputElement>(null)

  const reset = () => {
    setResult(null)
    if (fileRef.current) fileRef.current.value = ''
  }

  const handleTemplate = () => {
    startTransition(async () => {
      try {
        const csv = await templateAction()
        const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
        const url = URL.createObjectURL(blob)
        const a = document.createElement('a')
        a.href = url
        a.download = templateFilename
        document.body.appendChild(a)
        a.click()
        a.remove()
        URL.revokeObjectURL(url)
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '템플릿 다운로드에 실패했습니다')
      }
    })
  }

  const handleUpload = () => {
    const file = fileRef.current?.files?.[0]
    if (!file) {
      toast.error('CSV 파일을 선택하세요')
      return
    }
    const form = new FormData()
    form.append('file', file)
    startTransition(async () => {
      try {
        const res = await uploadAction(form)
        setResult(res)
        if (res.errors.length === 0) {
          toast.success(`${res.importedCount}건이 등록되었습니다`)
          router.refresh()
        } else {
          toast.error(`검증 실패 — ${res.errors.length}개 행 오류로 등록되지 않았습니다`)
        }
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '업로드에 실패했습니다')
      }
    })
  }

  return (
    <Dialog
      open={open}
      onOpenChange={(o) => {
        if (!o) reset()
        onOpenChange(o)
      }}
    >
      <DialogContent className="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>{title}</DialogTitle>
        </DialogHeader>
        <div className="space-y-3 py-2">
          <p className="text-sm text-muted-foreground">
            템플릿을 받아 작성한 뒤 업로드하세요. 한 행이라도 오류가 있으면 아무것도 등록되지 않고
            오류 행이 표시됩니다.
          </p>
          <Button variant="outline" size="sm" onClick={handleTemplate} disabled={isPending}>
            <DownloadIcon />
            템플릿 다운로드
          </Button>
          <div className="grid gap-1.5">
            <label className="text-xs font-medium text-muted-foreground">CSV 파일</label>
            <input
              ref={fileRef}
              type="file"
              accept=".csv,text/csv"
              className="block w-full text-sm text-foreground file:mr-3 file:rounded-md file:border file:border-border file:bg-muted file:px-3 file:py-1.5 file:text-sm file:text-foreground hover:file:bg-accent"
            />
          </div>

          {result && (
            <div className="rounded-md border border-border">
              <div className="border-b border-border bg-muted/40 px-3 py-2 text-sm">
                총 {result.totalRows}행 · 등록 {result.importedCount}건
                {result.errors.length > 0 && ` · 오류 ${result.errors.length}행`}
              </div>
              {result.errors.length > 0 && (
                <div className="max-h-48 overflow-y-auto">
                  <table className="w-full text-sm">
                    <thead>
                      <tr className="border-b border-border text-left text-muted-foreground">
                        <th className="px-3 py-1.5 font-medium">행</th>
                        <th className="px-3 py-1.5 font-medium">사유</th>
                      </tr>
                    </thead>
                    <tbody>
                      {result.errors.map((e, i) => (
                        <tr key={i} className="border-b border-border/60 last:border-0">
                          <td className="px-3 py-1.5 font-mono text-destructive">{e.rowNumber}</td>
                          <td className="px-3 py-1.5">{e.message}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          )}
        </div>
        <DialogFooter showCloseButton>
          <Button onClick={handleUpload} disabled={isPending}>
            <UploadIcon />
            업로드
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
