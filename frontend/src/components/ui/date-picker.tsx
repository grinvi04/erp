'use client'

import * as React from 'react'
import { Popover as PopoverPrimitive } from '@base-ui/react/popover'
import { DayPicker, type Matcher } from 'react-day-picker'
import { ko } from 'react-day-picker/locale'
import 'react-day-picker/style.css'
import { CalendarIcon, XIcon } from 'lucide-react'

import { cn, formatDate } from '@/lib/utils'

/** 'yyyy-MM-dd' → 로컬 Date (UTC 파싱 off-by-one 방지). 빈/형식오류는 undefined. */
function parseISODate(value?: string): Date | undefined {
  if (!value) return undefined
  const m = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value)
  if (!m) return undefined
  const d = new Date(Number(m[1]), Number(m[2]) - 1, Number(m[3]))
  return Number.isNaN(d.getTime()) ? undefined : d
}

/** 로컬 Date → 'yyyy-MM-dd' (toISOString의 UTC 변환 off-by-one 방지). */
function toISODate(d: Date): string {
  const y = d.getFullYear()
  const mo = String(d.getMonth() + 1).padStart(2, '0')
  const da = String(d.getDate()).padStart(2, '0')
  return `${y}-${mo}-${da}`
}

interface DatePickerProps {
  /** 'yyyy-MM-dd' 문자열. native <input type="date">와 동일 계약 — onChange도 같은 형식을 준다. */
  value?: string
  onChange: (value: string) => void
  placeholder?: string
  disabled?: boolean
  /** 선택 가능 하한/상한 'yyyy-MM-dd' */
  min?: string
  max?: string
  id?: string
  className?: string
}

export function DatePicker({
  value,
  onChange,
  placeholder = '날짜 선택',
  disabled,
  min,
  max,
  id,
  className,
}: DatePickerProps) {
  const [open, setOpen] = React.useState(false)
  const selected = parseISODate(value)
  const minDate = parseISODate(min)
  const maxDate = parseISODate(max)

  const startYear = minDate?.getFullYear() ?? 1950
  const endYear = maxDate?.getFullYear() ?? new Date().getFullYear() + 10

  const disabledMatchers = [
    minDate ? { before: minDate } : null,
    maxDate ? { after: maxDate } : null,
  ].filter(Boolean) as Matcher[]

  return (
    <PopoverPrimitive.Root open={open} onOpenChange={setOpen}>
      <div className={cn('relative w-full', className)}>
        <PopoverPrimitive.Trigger
          id={id}
          disabled={disabled}
          data-slot="date-picker-trigger"
          data-placeholder={selected ? undefined : ''}
          className="flex h-8 w-full min-w-0 items-center justify-between gap-2 rounded-lg border border-input bg-transparent py-1 pr-2.5 pl-2.5 text-sm whitespace-nowrap transition-colors outline-none select-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 disabled:cursor-not-allowed disabled:opacity-50 data-placeholder:text-muted-foreground dark:bg-input/30 dark:hover:bg-input/50"
        >
          <span className="truncate">{selected ? formatDate(value) : placeholder}</span>
          <CalendarIcon className="pointer-events-none size-4 shrink-0 text-muted-foreground" />
        </PopoverPrimitive.Trigger>
        {selected && !disabled && (
          <button
            type="button"
            aria-label="날짜 지우기"
            onClick={() => onChange('')}
            className="absolute top-1/2 right-7 -translate-y-1/2 rounded-sm text-muted-foreground transition-colors hover:text-foreground focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none"
          >
            <XIcon className="size-3.5" />
          </button>
        )}
      </div>
      <PopoverPrimitive.Portal>
        <PopoverPrimitive.Positioner sideOffset={4} align="start" className="isolate z-50">
          <PopoverPrimitive.Popup
            data-slot="date-picker-content"
            className="origin-(--transform-origin) rounded-xl bg-popover p-3 text-popover-foreground shadow-md ring-1 ring-foreground/10 duration-100 outline-none data-open:animate-in data-open:fade-in-0 data-open:zoom-in-95 data-closed:animate-out data-closed:fade-out-0 data-closed:zoom-out-95"
          >
            <DayPicker
              mode="single"
              locale={ko}
              selected={selected}
              defaultMonth={selected}
              captionLayout="dropdown"
              startMonth={new Date(startYear, 0)}
              endMonth={new Date(endYear, 11)}
              disabled={disabledMatchers.length ? disabledMatchers : undefined}
              onSelect={(d) => {
                if (d) {
                  onChange(toISODate(d))
                  setOpen(false)
                }
              }}
              // rdp 테마 변수를 OKLCH 디자인 토큰에 바인딩 → 다크모드 자동(.dark에서 토큰 반전)
              className={cn(
                '[--rdp-accent-background-color:var(--primary)]',
                '[--rdp-accent-color:var(--primary-foreground)]',
                '[--rdp-today-color:var(--primary)]',
                '[--rdp-day-width:2.25rem] [--rdp-day-height:2.25rem]',
                'text-sm',
              )}
              classNames={{
                month_caption: 'flex items-center gap-1 px-1 pb-2',
                caption_label: 'font-medium text-sm',
                dropdowns: 'flex items-center gap-1.5',
                dropdown_root:
                  'relative inline-flex items-center gap-1 rounded-md border border-input px-1.5 py-0.5 text-sm focus-within:border-ring focus-within:ring-3 focus-within:ring-ring/50',
                dropdown: 'absolute inset-0 cursor-pointer opacity-0',
                weekday: 'text-xs font-normal text-muted-foreground',
                button_previous:
                  'inline-flex size-7 items-center justify-center rounded-md text-muted-foreground hover:bg-accent hover:text-accent-foreground disabled:opacity-40',
                button_next:
                  'inline-flex size-7 items-center justify-center rounded-md text-muted-foreground hover:bg-accent hover:text-accent-foreground disabled:opacity-40',
                day_button:
                  'rounded-md hover:bg-accent hover:text-accent-foreground focus-visible:ring-3 focus-visible:ring-ring/50 outline-none',
              }}
            />
          </PopoverPrimitive.Popup>
        </PopoverPrimitive.Positioner>
      </PopoverPrimitive.Portal>
    </PopoverPrimitive.Root>
  )
}
