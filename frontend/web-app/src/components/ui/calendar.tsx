"use client"

import * as React from "react"
import { cn } from "@/lib/utils"

type CalendarProps = {
  className?: string
  selected?: Date
  onSelect?: (date: Date | undefined) => void
  disabled?: (date: Date) => boolean
  initialFocus?: boolean
  locale?: unknown
  mode?: string
  showOutsideDays?: boolean
} & Omit<React.InputHTMLAttributes<HTMLInputElement>, "onSelect" | "value" | "disabled">

function Calendar({
  className,
  selected,
  onSelect,
  disabled,
  initialFocus: _initialFocus,
  locale: _locale,
  mode: _mode,
  showOutsideDays: _showOutsideDays,
  ...props
}: CalendarProps) {
  const value = selected ? toDateString(selected) : ""
  // compute min from disabled past-dates predicate (transfer page disables < today)
  const min = React.useMemo(() => {
    if (!disabled) return undefined
    // probe: if today disabled? then no min; if yesterday disabled but today not, min = today
    const today = new Date()
    today.setHours(0, 0, 0, 0)
    const yesterday = new Date(today)
    yesterday.setDate(yesterday.getDate() - 1)
    try {
      if (disabled(yesterday) && !disabled(today)) return toDateString(today)
    } catch {}
    return undefined
  }, [disabled])

  return (
    <input
      type="date"
      value={value}
      min={min}
      onChange={(e) => {
        const v = e.target.value
        if (!v) onSelect?.(undefined)
        else {
          const d = new Date(v + "T00:00:00")
          if (disabled?.(d)) return
          onSelect?.(d)
        }
      }}
      className={cn(
        "w-full rounded-xl border border-input bg-background px-4 py-3 text-sm font-medium focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring",
        className
      )}
      {...props}
    />
  )
}

function toDateString(d: Date) {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, "0")
  const day = String(d.getDate()).padStart(2, "0")
  return `${y}-${m}-${day}`
}

export { Calendar }
