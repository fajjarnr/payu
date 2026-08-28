'use client';

import { useEffect } from 'react';
import { AlertTriangle, RefreshCw, Home, ArrowLeft, Bug } from 'lucide-react';

interface ErrorProps {
  error: Error & { digest?: string };
  reset: () => void;
}

export default function Error({ error, reset }: ErrorProps) {
  useEffect(() => {
    console.error('[Route Error Boundary]', error);
  }, [error]);

  const handleGoHome = () => {
    const pathLocale = window.location.pathname.match(/^\/(en|id)(\/|$)/);
    const locale = pathLocale ? pathLocale[1] : 'id';
    window.location.href = `/${locale}/dashboard`;
  };

  const handleGoBack = () => {
    window.history.back();
  };

  const handleReset = () => {
    reset();
  };

  return (
    <div className="min-h-[60vh] flex items-center justify-center px-6 sm:px-10 lg:px-12 bg-background">
      <div className="max-w-md w-full bg-card/80 backdrop-blur-xl rounded-2xl p-5 sm:p-6 lg:p-8 border border-border shadow-sm text-center relative overflow-hidden">
        {/* Subtle ambient glow */}
        <div className="absolute top-0 right-0 w-48 h-48 bg-destructive/5 rounded-full blur-3xl" />
        <div className="absolute bottom-0 left-0 w-32 h-32 bg-bank-green/5 rounded-full blur-3xl" />

        <div className="relative z-10">
          {/* Icon */}
          <div className="h-20 w-20 bg-destructive/10 rounded-2xl flex items-center justify-center mx-auto mb-8 border border-destructive/20">
            <AlertTriangle className="h-10 w-10 text-destructive" aria-hidden="true" />
          </div>

          {/* Heading */}
          <h2 className="text-2xl font-bold text-foreground mb-4 font-heading">
            Terjadi Kesalahan
          </h2>

          {/* Description */}
          <p className="text-sm text-muted-foreground font-medium mb-8 leading-relaxed">
            Maaf, terjadi kesalahan yang tidak terduga saat memuat halaman ini. Silakan coba lagi atau kembali ke beranda.
          </p>

          {/* Technical Details (Development Only) */}
          {process.env.NODE_ENV === 'development' && (
            <div className="bg-destructive/10 rounded-2xl p-4 mb-8 text-left border border-destructive/20">
              <div className="flex items-center gap-2 mb-2">
                <Bug className="h-3 w-3 text-destructive" aria-hidden="true" />
                <p className="text-xs font-bold text-muted-foreground tracking-widest uppercase">
                  Detail Teknis
                </p>
              </div>
              <p className="text-xs text-destructive font-mono break-words">
                {error.message}
              </p>
              {error.digest && (
                <p className="text-xs text-muted-foreground font-mono mt-2">
                  Digest: {error.digest}
                </p>
              )}
            </div>
          )}

          {/* Action Buttons */}
          <div className="space-y-3" role="group" aria-label="Tindakan pemulihan error">
            <button
              onClick={handleReset}
              className="w-full bg-foreground text-background py-4 rounded-xl font-bold text-xs tracking-widest hover:bg-bank-green hover:text-white transition-all active:scale-95 shadow-xl flex items-center justify-center gap-2 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
              aria-label="Coba muat ulang halaman ini"
            >
              <RefreshCw className="h-4 w-4" aria-hidden="true" />
              Coba Lagi
            </button>

            <div className="flex gap-3">
              <button
                onClick={handleGoBack}
                className="flex-1 bg-white/5 backdrop-blur-sm py-4 rounded-xl font-bold text-xs tracking-widest border border-border hover:bg-white/10 transition-all active:scale-95 flex items-center justify-center gap-2 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                aria-label="Kembali ke halaman sebelumnya"
              >
                <ArrowLeft className="h-4 w-4" aria-hidden="true" />
                Kembali
              </button>

              <button
                onClick={handleGoHome}
                className="flex-1 bg-white/5 backdrop-blur-sm py-4 rounded-xl font-bold text-xs tracking-widest border border-border hover:bg-white/10 transition-all active:scale-95 flex items-center justify-center gap-2 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                aria-label="Kembali ke beranda dashboard"
              >
                <Home className="h-4 w-4" aria-hidden="true" />
                Beranda
              </button>
            </div>
          </div>

          {/* Support Info */}
          <p className="text-xs text-muted-foreground font-bold tracking-widest mt-8">
            Masalah berlanjut? Hubungi tim dukungan kami.
          </p>
        </div>
      </div>
    </div>
  );
}
