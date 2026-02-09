export default function TransferLoading() {
  return (
    <div className="min-h-screen bg-background animate-pulse">
      <div className="h-16 border-b border-border bg-card" />
      
      <div className="max-w-2xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="h-8 w-40 bg-muted rounded-xl mb-2" />
        <div className="h-5 w-56 bg-muted/60 rounded-xl mb-8" />

        {/* Transfer form skeleton */}
        <div className="rounded-3xl border border-border bg-card p-6 space-y-6">
          <div>
            <div className="h-4 w-24 bg-muted rounded mb-2" />
            <div className="h-12 w-full bg-muted rounded-xl" />
          </div>
          <div>
            <div className="h-4 w-32 bg-muted rounded mb-2" />
            <div className="h-12 w-full bg-muted rounded-xl" />
          </div>
          <div>
            <div className="h-4 w-20 bg-muted rounded mb-2" />
            <div className="h-12 w-full bg-muted rounded-xl" />
          </div>
          <div>
            <div className="h-4 w-28 bg-muted rounded mb-2" />
            <div className="h-12 w-full bg-muted rounded-xl" />
          </div>
          <div className="h-12 w-full bg-muted rounded-full" />
        </div>

        {/* Recent recipients skeleton */}
        <div className="mt-8">
          <div className="h-6 w-36 bg-muted rounded-xl mb-4" />
          <div className="flex gap-4">
            {[...Array(4)].map((_, i) => (
              <div key={i} className="flex flex-col items-center gap-2">
                <div className="w-14 h-14 bg-muted rounded-full" />
                <div className="h-3 w-12 bg-muted rounded" />
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
