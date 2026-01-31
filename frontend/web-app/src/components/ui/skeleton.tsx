import { cn } from "@/lib/utils"

function Skeleton({
  className,
  ...props
}: React.HTMLAttributes<HTMLDivElement>) {
  return (
    <div
      className={cn("animate-pulse rounded-md bg-gray-200 dark:bg-gray-800", className)}
      {...props}
    />
  )
}

// Higher-level utility components for common patterns
const SkeletonCard = ({ className, ...props }: React.HTMLAttributes<HTMLDivElement>) => (
  <div className={cn("bg-card p-8 rounded-xl border border-border shadow-sm space-y-6", className)} {...props}>
    <div className="flex justify-between items-start">
      <Skeleton className="h-12 w-12 rounded-xl" />
      <Skeleton className="h-8 w-20 rounded-full" />
    </div>
    <div className="space-y-3">
      <Skeleton className="h-3 w-32 rounded" />
      <Skeleton className="h-8 w-40 rounded" />
    </div>
  </div>
)

const SkeletonText = ({ lines = 3, className, ...props }: { lines?: number } & React.HTMLAttributes<HTMLDivElement>) => (
  <div className={cn("space-y-2", className)} {...props}>
    {Array.from({ length: lines }).map((_, i) => (
      <Skeleton
        key={i}
        className={cn("h-4 rounded", i === lines - 1 ? "w-3/4" : "w-full")}
      />
    ))}
  </div>
)

const SkeletonTransaction = ({ className, ...props }: React.HTMLAttributes<HTMLDivElement>) => (
  <div className={cn("flex items-center justify-between p-4 space-x-4", className)} {...props}>
    <div className="flex items-center gap-4">
      <Skeleton className="h-12 w-12 rounded-xl" />
      <div className="space-y-2">
        <Skeleton className="h-4 w-32 rounded" />
        <Skeleton className="h-3 w-24 rounded" />
      </div>
    </div>
    <Skeleton className="h-6 w-20 rounded" />
  </div>
)

const SkeletonBalance = ({ className, ...props }: React.HTMLAttributes<HTMLDivElement>) => (
  <div className={cn("space-y-4", className)} {...props}>
    <div className="flex items-center gap-2 mb-2">
      <div className="h-2 w-2 bg-muted-foreground/40 rounded-full animate-pulse" />
      <Skeleton className="h-3 w-24 rounded" />
    </div>
    <Skeleton className="h-16 w-64 rounded" />
  </div>
)

const SkeletonChart = ({ className, ...props }: React.HTMLAttributes<HTMLDivElement>) => (
  <div className={cn("h-[300px] flex items-end justify-between gap-3 px-4", className)} {...props}>
    {Array.from({ length: 12 }).map((_, i) => (
      <div key={i} className="flex-1 space-y-2">
        <Skeleton className="w-full h-full rounded-t-xl" />
      </div>
    ))}
  </div>
)

const SkeletonStatsGrid = ({ count = 4, className, ...props }: { count?: number } & React.HTMLAttributes<HTMLDivElement>) => (
  <div className={cn("grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6", className)} {...props}>
    {Array.from({ length: count }).map((_, i) => (
      <SkeletonCard key={i} />
    ))}
  </div>
)

export { 
  Skeleton, 
  SkeletonCard, 
  SkeletonText, 
  SkeletonTransaction, 
  SkeletonBalance, 
  SkeletonChart,
  SkeletonStatsGrid 
}
