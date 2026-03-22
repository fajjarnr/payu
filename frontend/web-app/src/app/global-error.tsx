'use client';

export default function GlobalError({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  return (
    <html>
      <body className="bg-gray-950 text-white">
        <div className="flex flex-col items-center justify-center min-h-screen gap-4">
          <h2 className="text-2xl font-bold text-red-500">Critical Error</h2>
          <p className="text-gray-400">{error.message}</p>
          <button onClick={reset} className="px-6 py-3 bg-emerald-600 rounded-lg">
            Restart Application
          </button>
        </div>
      </body>
    </html>
  );
}
