import * as React from "react"

import { cn } from "@/lib/utils"

const Input = React.forwardRef<HTMLInputElement, React.ComponentProps<"input">>(
  ({ className, type, ...props }, ref) => {
    return (
      <input
        type={type}
        className={cn(
          "flex h-14 w-full rounded-xl border border-border bg-muted/20 px-6 py-3 text-sm font-bold text-foreground transition-all shadow-sm placeholder:text-muted-foreground/40 focus:bg-background focus:ring-4 focus:ring-primary/10 focus:border-primary outline-none disabled:cursor-not-allowed disabled:opacity-50",
          className
        )}
        ref={ref}
        {...props}
      />
    )
  }
)
Input.displayName = "Input"

export { Input }
