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
        "bg-card p-4",
        // CSS variables for sizing - ULTRA PREMIUM SCALE
        "[--cell-size:3.5rem] [--font-size-day:1rem] [--font-size-caption:1.125rem]",
        className
      )}
      classNames={{
        root: cn("w-fit", defaultClassNames.root),
        months: cn("relative flex flex-col gap-6 sm:flex-row", defaultClassNames.months),
        month: cn("flex flex-col gap-8", defaultClassNames.month),
        month_caption: cn("relative flex h-12 items-center justify-center", defaultClassNames.month_caption),
        caption_label: cn("text-base font-bold tracking-widest uppercase text-foreground", defaultClassNames.caption_label),
        nav: cn("absolute inset-x-0 top-0 flex items-center justify-between", defaultClassNames.nav),
        button_previous: cn(
          buttonVariants({ variant: "outline" }),
          "h-12 w-12 p-0 border-border hover:bg-muted font-bold transition-all active:scale-95 shadow-sm",
          defaultClassNames.button_previous
        ),
        button_next: cn(
          buttonVariants({ variant: "outline" }),
          "h-12 w-12 p-0 border-border hover:bg-muted font-bold transition-all active:scale-95 shadow-sm",
          defaultClassNames.button_next
        ),
        month_grid: cn("w-full border-collapse", defaultClassNames.month_grid),
        weekdays: cn("flex mb-4", defaultClassNames.weekdays),
        weekday: cn(
          "text-muted-foreground w-[--cell-size] font-bold text-xs tracking-[0.2em] uppercase text-center",
          defaultClassNames.weekday
        ),
        week: cn("flex w-full mt-2", defaultClassNames.week),
        day: cn(
          "w-[--cell-size] h-[--cell-size] text-center text-sm p-0 m-0",
          defaultClassNames.day
        ),
        today: cn("text-emerald-500 font-extrabold scale-110", defaultClassNames.today),
        outside: cn("text-muted-foreground/20 opacity-30", defaultClassNames.outside),
        disabled: cn("text-muted-foreground/10 opacity-20", defaultClassNames.disabled),
        range_start: "bg-emerald-500 text-white rounded-l-2xl",
        range_end: "bg-emerald-500 text-white rounded-r-2xl",
        range_middle: "bg-emerald-500/10 text-emerald-600",
        ...classNames,
      }}
      components={{
        Chevron: ({ orientation }) => {
          if (orientation === "left") {
            return <ChevronLeftIcon className="h-6 w-6" />
          }
          return <ChevronRightIcon className="h-6 w-6" />
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
        "h-[--cell-size] w-[--cell-size] p-0 font-bold text-base transition-all hover:bg-emerald-500/10 hover:text-emerald-600 active:scale-90 rounded-2xl",
        modifiers.selected && "bg-emerald-500 text-white hover:bg-emerald-600 hover:text-white shadow-xl shadow-emerald-500/30 ring-4 ring-emerald-500/10",
        modifiers.today && !modifiers.selected && "border-2 border-emerald-500/30 text-emerald-600 bg-emerald-500/5",
        className
      )}
      {...props}
    />
  )
}

export { Calendar }
