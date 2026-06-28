import { cn } from '@/lib/utils'
import { Label } from '@/components/ui/label'

/**
 * 폼 필드 래퍼 — 라벨·필수표시·인라인 에러/힌트.
 * 에러가 있으면 에러를, 없으면 힌트를 필드 아래에 표시(toast 대신 필드 레벨 피드백).
 */
export function FormField({
  label,
  htmlFor,
  required,
  error,
  hint,
  children,
  className,
}: {
  label?: React.ReactNode
  htmlFor?: string
  required?: boolean
  error?: string
  hint?: React.ReactNode
  children: React.ReactNode
  className?: string
}) {
  return (
    <div className={cn('space-y-1.5', className)}>
      {label && (
        <Label htmlFor={htmlFor} className="text-sm font-medium text-foreground">
          {label}
          {required && <span className="ml-0.5 text-destructive">*</span>}
        </Label>
      )}
      {children}
      {error ? (
        <p className="text-xs font-medium text-destructive">{error}</p>
      ) : hint ? (
        <p className="text-xs text-muted-foreground">{hint}</p>
      ) : null}
    </div>
  )
}
