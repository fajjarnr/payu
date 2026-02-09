export default function InvestmentsLoading() {
  return (
    <div className="min-h-screen bg-background animate-pulse">
      <div className="h-16 border-b border-border bg-card" />
      
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="h-8 w-36 bg-muted rounded-xl mb-2" />
        <div className="h-5 w-60 bg-muted/60 rounded-xl mb-8" />

        {/* Portfolio summary skeleton */}
        <div className="grid md:grid-cols-3 gap-6 mb-8">
          {[...Array(3)].map((_, i) => (
            <div key={i} className="p-6 rounded-3xl border border-border bg-card">
              <div className="h-4 w-24 bg-muted rounded mb-3" />
              <div className="h-8 w-40 bg-muted rounded-xl mb-2" />
              <div className="h-3 w-20 bg-muted/60 rounded" />
            </div>
          ))}
        </div>

        {/* Products list skeleton */}
        <div className="rounded-3xl border border-border bg-card p-6">
          <div className="h-6 w-40 bg-muted rounded-xl mb-6" />
          <div className="space-y-4">
            {[...Array(4)].map((_, i) => (
              <div key={i} className="flex items-center gap-4 p-4 rounded-2xl border border-border">
                <div className="w-12 h-12 bg-muted rounded-xl" />
                <div className="flex-1">
                  <div className="h-4 w-48 bg-muted rounded mb-1" />
                  <div className="h-3 w-32 bg-muted/60 rounded" />
                </div>
                <div className="text-right">
                  <div className="h-4 w-20 bg-muted rounded mb-1" />
                  <div className="h-3 w-16 bg-muted/60 rounded" />
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
