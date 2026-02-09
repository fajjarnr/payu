export default function DashboardLoading() {
  return (
    <div className="min-h-screen bg-background animate-pulse">
      {/* Header skeleton */}
      <div className="h-16 border-b border-border bg-card" />
      
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Greeting skeleton */}
        <div className="h-8 w-48 bg-muted rounded-xl mb-2" />
        <div className="h-5 w-64 bg-muted/60 rounded-xl mb-8" />

        {/* Balance card skeleton */}
        <div className="p-6 rounded-3xl border border-border bg-card mb-8">
          <div className="h-4 w-24 bg-muted rounded mb-3" />
          <div className="h-10 w-56 bg-muted rounded-xl mb-4" />
          <div className="flex gap-3">
            <div className="h-10 w-28 bg-muted rounded-full" />
            <div className="h-10 w-28 bg-muted rounded-full" />
            <div className="h-10 w-28 bg-muted rounded-full" />
          </div>
        </div>

        {/* Quick actions skeleton */}
        <div className="grid grid-cols-4 gap-4 mb-8">
          {[...Array(4)].map((_, i) => (
            <div key={i} className="p-4 rounded-2xl border border-border bg-card flex flex-col items-center gap-2">
              <div className="w-12 h-12 bg-muted rounded-full" />
              <div className="h-3 w-16 bg-muted rounded" />
            </div>
          ))}
        </div>

        {/* Recent transactions skeleton */}
        <div className="rounded-3xl border border-border bg-card p-6">
          <div className="h-6 w-40 bg-muted rounded-xl mb-6" />
          <div className="space-y-4">
            {[...Array(5)].map((_, i) => (
              <div key={i} className="flex items-center gap-4">
                <div className="w-10 h-10 bg-muted rounded-full" />
                <div className="flex-1">
                  <div className="h-4 w-32 bg-muted rounded mb-1" />
                  <div className="h-3 w-24 bg-muted/60 rounded" />
                </div>
                <div className="h-4 w-20 bg-muted rounded" />
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
