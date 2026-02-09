export default function LendingLoading() {
  return (
    <div className="min-h-screen bg-background animate-pulse">
      <div className="h-16 border-b border-border bg-card" />
      
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="h-8 w-32 bg-muted rounded-xl mb-2" />
        <div className="h-5 w-52 bg-muted/60 rounded-xl mb-8" />

        {/* Loan status skeleton */}
        <div className="p-6 rounded-3xl border border-border bg-card mb-8">
          <div className="h-4 w-28 bg-muted rounded mb-3" />
          <div className="h-8 w-44 bg-muted rounded-xl mb-4" />
          <div className="h-3 w-full bg-muted/30 rounded-full mb-2" />
          <div className="flex justify-between">
            <div className="h-3 w-20 bg-muted/60 rounded" />
            <div className="h-3 w-20 bg-muted/60 rounded" />
          </div>
        </div>

        {/* Products skeleton */}
        <div className="grid md:grid-cols-2 gap-6">
          {[...Array(4)].map((_, i) => (
            <div key={i} className="p-6 rounded-3xl border border-border bg-card">
              <div className="h-5 w-36 bg-muted rounded mb-2" />
              <div className="h-4 w-full bg-muted/60 rounded mb-4" />
              <div className="flex justify-between items-end">
                <div>
                  <div className="h-3 w-16 bg-muted/60 rounded mb-1" />
                  <div className="h-6 w-24 bg-muted rounded" />
                </div>
                <div className="h-10 w-24 bg-muted rounded-full" />
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
