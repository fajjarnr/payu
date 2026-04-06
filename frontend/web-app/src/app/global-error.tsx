'use client';

import { AlertOctagon, RefreshCw, Mail, Phone } from 'lucide-react';

interface GlobalErrorProps {
  error: Error & { digest?: string };
  reset: () => void;
}

export default function GlobalError({ error, reset }: GlobalErrorProps) {
  const handleReload = () => {
    window.location.reload();
  };

  return (
    <html lang="id">
      <body className="antialiased bg-background text-foreground min-h-screen flex items-center justify-center px-6 sm:px-10 lg:px-12 font-sans">
        <div className="max-w-md w-full bg-card/80 backdrop-blur-xl rounded-2xl p-8 border border-border shadow-2xl text-center relative overflow-hidden">
          {/* Ambient glow effects */}
          <div className="absolute top-0 left-1/2 -translate-x-1/2 w-64 h-64 bg-destructive/10 rounded-full blur-3xl" />
          <div className="absolute bottom-0 right-0 w-40 h-40 bg-bank-green/5 rounded-full blur-3xl" />

          <div className="relative z-10">
            {/* Critical Error Icon */}
            <div className="h-24 w-24 bg-destructive/10 rounded-2xl flex items-center justify-center mx-auto mb-8 border border-destructive/20">
              <AlertOctagon className="h-12 w-12 text-destructive" aria-hidden="true" />
            </div>

            {/* Heading */}
            <h1 className="text-3xl font-bold text-foreground mb-4 font-heading">
              Kesalahan Kritis
            </h1>

            {/* Description */}
            <p className="text-sm text-muted-foreground font-medium mb-8 leading-relaxed">
              Aplikasi mengalami kesalahan yang tidak dapat dipulihkan secara otomatis. Silakan muat ulang aplikasi atau hubungi dukungan jika masalah berlanjut.
            </p>

            {/* Technical Details (Development Only) */}
            {process.env.NODE_ENV === 'development' && (
              <div className="bg-destructive/10 rounded-2xl p-4 mb-8 text-left border border-destructive/20">
                <p className="text-xs font-bold text-destructive tracking-widest uppercase mb-2">
                  Detail Teknis
                </p>
                <p className="text-xs text-destructive/80 font-mono break-words">
                  {error.message}
                </p>
                {error.digest && (
                  <p className="text-xs text-muted-foreground font-mono mt-2">
                    Digest: {error.digest}
                  </p>
                )}
                <p className="text-xs text-muted-foreground font-mono mt-1">
                  Stack: {error.stack?.split('\n').slice(0, 3).join(' | ')}
                </p>
              </div>
            )}

            {/* Action Buttons */}
            <div className="space-y-3" role="group" aria-label="Tindakan pemulihan error kritis">
              <button
                onClick={handleReload}
                className="w-full bg-foreground text-background py-4 rounded-xl font-bold text-xs tracking-widest hover:bg-bank-green hover:text-white transition-all active:scale-95 shadow-xl flex items-center justify-center gap-2 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                aria-label="Muat ulang seluruh aplikasi"
              >
                <RefreshCw className="h-4 w-4" aria-hidden="true" />
                Muat Ulang Aplikasi
              </button>

              <button
                onClick={() => reset()}
                className="w-full bg-white/10 backdrop-blur-md border border-border text-foreground py-4 rounded-xl font-bold text-xs tracking-widest hover:bg-white/20 transition-all active:scale-95 flex items-center justify-center gap-2 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                aria-label="Coba pulihkan aplikasi"
              >
                <RefreshCw className="h-4 w-4" aria-hidden="true" />
                Coba Pulihkan
              </button>
            </div>

            {/* Support Contact */}
            <div className="mt-8 pt-6 border-t border-border">
              <p className="text-xs text-muted-foreground font-bold tracking-widest mb-4">
                Butuh bantuan?
              </p>
              <div className="flex justify-center gap-6">
                <a
                  href="mailto:support@payu.id"
                  className="flex items-center gap-2 text-xs text-muted-foreground hover:text-bank-green transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring rounded-xl px-3 py-3 min-h-[44px]"
                  aria-label="Kirim email ke tim dukungan"
                >
                  <Mail className="h-4 w-4" aria-hidden="true" />
                  support@payu.id
                </a>
                <a
                  href="tel:+6280012345678"
                  className="flex items-center gap-2 text-xs text-muted-foreground hover:text-bank-green transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring rounded-xl px-3 py-3 min-h-[44px]"
                  aria-label="Hubungi tim dukungan melalui telepon"
                >
                  <Phone className="h-4 w-4" aria-hidden="true" />
                  0800-123-45678
                </a>
              </div>
            </div>
          </div>
        </div>
      </body>
    </html>
  );
}
