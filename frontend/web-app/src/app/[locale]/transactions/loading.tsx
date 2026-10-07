export default function Loading() {
  return (
    <div className="flex min-h-[60vh] items-center justify-center">
      <div className="flex flex-col items-center gap-4">
        <div className="h-10 w-10 animate-spin rounded-full border-4 border-primary-light border-t-primary-dark" />
        <p className="text-sm text-text-secondary">Loading...</p>
      </div>
    </div>
  );
}
