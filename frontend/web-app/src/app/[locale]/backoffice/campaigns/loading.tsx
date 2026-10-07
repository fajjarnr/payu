export default function Loading() {
  return (
    <div className="min-h-[60vh] flex items-center justify-center">
      <div className="flex flex-col items-center gap-4">
        <div className="h-10 w-10 animate-spin rounded-full border-4 border-primary-light border-t-primary-dark" />
        <p className="text-sm text-text-secondary animate-pulse">Memuat data...</p>
      </div>
    </div>
  );
}

