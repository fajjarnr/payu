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
        "bg-card p-4 sm:p-6 mx-auto",
        // SCALE: 3.5rem (56px) to 4rem (64px) for perfect density
        "[--cell-size:4rem] [--font-size-day:1rem] [--font-size-caption:1.125rem]",
        className
      )}
      classNames={{
        root: cn("w-fit", defaultClassNames.root),
        months: cn("relative flex flex-col gap-8", defaultClassNames.months),
        month: cn("flex flex-col gap-6", defaultClassNames.month),
        month_caption: cn("relative flex h-14 items-center justify-center mb-2", defaultClassNames.month_caption),
        caption_label: cn("text-base font-bold tracking-[0.15em] uppercase text-foreground bg-muted/50 px-8 py-2.5 rounded-full border border-border/50", defaultClassNames.caption_label),
        nav: cn("absolute inset-x-0 top-0 flex items-center justify-between px-2", defaultClassNames.nav),
        button_previous: cn(
          buttonVariants({ variant: "outline" }),
          "h-11 w-11 p-0 border-border hover:bg-muted font-bold transition-all active:scale-95 shadow-sm rounded-xl z-20 bg-card",
          defaultClassNames.button_previous
        ),
        button_next: cn(
          buttonVariants({ variant: "outline" }),
          "h-11 w-11 p-0 border-border hover:bg-muted font-bold transition-all active:scale-95 shadow-sm rounded-xl z-20 bg-card",
          defaultClassNames.button_next
        ),
        // Body Grid System
        month_grid: cn("w-full", defaultClassNames.month_grid),
        weekdays: cn("grid grid-cols-7 mb-4", defaultClassNames.weekdays),
        weekday: cn(
          "text-muted-foreground w-[--cell-size] font-bold text-xs tracking-[0.2em] uppercase text-center opacity-40 flex items-center justify-center",
          defaultClassNames.weekday
        ),
        week: cn("grid grid-cols-7 w-full mt-1.5", defaultClassNames.week),
        day: cn(
          "w-[--cell-size] h-[--cell-size] p-0 m-0 flex items-center justify-center",
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
        "h-[calc(var(--cell-size)-0.75rem)] w-[calc(var(--cell-size)-0.75rem)] p-0 font-bold text-base transition-all active:scale-90 rounded-2xl relative",
        "text-foreground/80 hover:bg-emerald-800/10 hover:text-emerald-700",
        // Today
        modifiers.today && !modifiers.selected && "border-2 border-emerald-700/30 text-emerald-700 bg-emerald-800/5",
        // Selected
        modifiers.selected && "bg-emerald-700 text-white hover:bg-emerald-800 hover:text-white shadow-xl shadow-emerald-800/30 ring-4 ring-emerald-800/10 scale-105 z-10",
        className
      )}
      {...props}
    />
  )
}

export { Calendar }
