'use client';

import { useEffect } from 'react';

export default function Error({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    console.error('[ErrorBoundary: dashboard]', error);
  }, [error]);

  return (
    <div className="flex min-h-[60vh] flex-col items-center justify-center p-4">
      <div className="text-center">
        <h2 className="mb-2 text-2xl font-semibold text-text-primary">Something went wrong</h2>
        <p className="mb-6 text-text-secondary">
          {error.message || 'An unexpected error occurred. Please try again.'}
        </p>
        <button
          onClick={() => reset()}
          className="rounded-lg bg-primary-dark px-6 py-3 text-surface hover:bg-primary-dark transition-colors"
        >
          Try again
        </button>
      </div>
    </div>
  );
}
