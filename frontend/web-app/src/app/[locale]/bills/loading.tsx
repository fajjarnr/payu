export default function BillsLoading() {
  return (
    <div className="min-h-screen bg-background animate-pulse">
      <div className="h-16 border-b border-border bg-card" />
      
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="h-8 w-40 bg-muted rounded-xl mb-2" />
        <div className="h-5 w-48 bg-muted/60 rounded-xl mb-8" />

        {/* Category tabs skeleton */}
        <div className="flex gap-3 mb-8 overflow-x-auto">
          {[...Array(5)].map((_, i) => (
            <div key={i} className="h-10 w-24 bg-muted rounded-full shrink-0" />
          ))}
        </div>

        {/* Bill items skeleton */}
        <div className="space-y-4">
          {[...Array(5)].map((_, i) => (
            <div key={i} className="flex items-center gap-4 p-4 rounded-2xl border border-border bg-card">
              <div className="w-12 h-12 bg-muted rounded-xl" />
              <div className="flex-1">
                <div className="h-4 w-40 bg-muted rounded mb-1" />
                <div className="h-3 w-28 bg-muted/60 rounded" />
              </div>
              <div className="text-right">
                <div className="h-4 w-24 bg-muted rounded mb-1" />
                <div className="h-8 w-16 bg-muted rounded-full" />
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
