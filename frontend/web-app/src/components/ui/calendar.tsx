"use client"

import * as React from "react"
import {
  ChevronLeftIcon,
  ChevronRightIcon,
} from "lucide-react"
import { DayButton, DayPicker, getDefaultClassNames } from "react-day-picker"

import { cn } from "@/lib/utils"
import { Button, buttonVariants } from "@/components/ui/button"

function Calendar({
  className,
  classNames,
  showOutsideDays = true,
  ...props
}: React.ComponentProps<typeof DayPicker>) {
  const defaultClassNames = getDefaultClassNames()

  return (
    <DayPicker
      showOutsideDays={showOutsideDays}
      className={cn(
        "bg-card p-4 sm:p-8",
        // JUMBO SCALE: 4.5rem cells for extreme clarity
        "[--cell-size:4.5rem] [--font-size-day:1.125rem] [--font-size-caption:1.25rem]",
        className
      )}
      classNames={{
        root: cn("w-full max-w-full", defaultClassNames.root),
        months: cn("flex flex-col gap-10", defaultClassNames.months),
        month: cn("flex flex-col gap-10", defaultClassNames.month),
        month_caption: cn("relative flex h-14 items-center justify-center mb-4", defaultClassNames.month_caption),
        caption_label: cn("text-lg font-bold tracking-[0.2em] uppercase text-foreground bg-muted/50 px-8 py-3 rounded-2xl", defaultClassNames.caption_label),
        nav: cn("absolute inset-x-0 top-0 flex items-center justify-between", defaultClassNames.nav),
        button_previous: cn(
          buttonVariants({ variant: "outline" }),
          "h-14 w-14 p-0 border-border hover:bg-muted font-bold transition-all active:scale-95 shadow-lg rounded-2xl z-20 bg-card",
          defaultClassNames.button_previous
        ),
        button_next: cn(
          buttonVariants({ variant: "outline" }),
          "h-14 w-14 p-0 border-border hover:bg-muted font-bold transition-all active:scale-95 shadow-lg rounded-2xl z-20 bg-card",
          defaultClassNames.button_next
        ),
        month_grid: cn("w-full border-collapse", defaultClassNames.month_grid),
        weekdays: cn("flex w-full justify-between mb-6", defaultClassNames.weekdays),
        weekday: cn(
          "text-muted-foreground w-[--cell-size] font-bold text-sm tracking-[0.2em] uppercase text-center opacity-30",
          defaultClassNames.weekday
        ),
        week: cn("flex w-full justify-between mt-3", defaultClassNames.week),
        day: cn(
          "w-[--cell-size] h-[--cell-size] text-center p-0 m-0 relative flex items-center justify-center",
          defaultClassNames.day
        ),
        today: cn("relative", defaultClassNames.today),
        outside: cn("text-muted-foreground/10 opacity-10 pointer-events-none", defaultClassNames.outside),
        disabled: cn("text-muted-foreground/10 opacity-10", defaultClassNames.disabled),
        ...classNames,
      }}
      components={{
        Chevron: ({ orientation }) => {
          if (orientation === "left") {
            return <ChevronLeftIcon className="h-7 w-7" />
          }
          return <ChevronRightIcon className="h-7 w-7" />
        },
        DayButton: CalendarDayButton,
      }}
      {...props}
    />
  )
}

function CalendarDayButton({
  className,
  day,
  modifiers,
  ...props
}: React.ComponentProps<typeof DayButton>) {
  const ref = React.useRef<HTMLButtonElement>(null)
  
  return (
    <Button
      ref={ref}
      variant="ghost"
      className={cn(
        "h-[--cell-size] w-[--cell-size] p-0 font-bold text-xl transition-all active:scale-90 rounded-[1.5rem] relative",
        "text-foreground/90 hover:bg-emerald-500/10 hover:text-emerald-600",
        // Today
        modifiers.today && !modifiers.selected && "border-2 border-emerald-500/40 text-emerald-600 bg-emerald-500/5",
        // Selected
        modifiers.selected && "bg-emerald-500 text-white hover:bg-emerald-600 hover:text-white shadow-[0_20px_40px_-10px_rgba(16,185,129,0.5)] ring-8 ring-emerald-500/10 scale-110 z-10 border-emerald-400",
        className
      )}
      {...props}
    />
  )
}

export { Calendar }
