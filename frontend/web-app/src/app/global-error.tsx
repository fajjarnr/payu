'use client';

import { AlertOctagon, RefreshCw, Mail, Phone } from '@/components/icons';

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
      <body
        className="antialiased min-h-screen flex items-center justify-center px-6 sm:px-10 lg:px-12 font-sans"
        style={{ backgroundColor: 'var(--ant-color-bg-layout)', color: 'var(--ant-color-text)' }}
      >
        <div
          className="max-w-md w-full rounded-2xl p-5 sm:p-6 lg:p-8 border shadow-2xl text-center relative overflow-hidden"
          style={{ backgroundColor: 'var(--ant-color-bg-container)', borderColor: 'var(--ant-color-border-secondary)' }}
        >
          <div className="absolute top-0 left-1/2 -translate-x-1/2 w-64 h-64 bg-destructive/10 rounded-full blur-3xl" />
          <div
            className="absolute bottom-0 right-0 w-40 h-40 rounded-full blur-3xl"
            style={{ backgroundColor: 'color-mix(in srgb, var(--ant-color-primary) 5%, transparent)' }}
          />

          <div className="relative z-10">
            <div className="h-24 w-24 bg-destructive/10 rounded-2xl flex items-center justify-center mx-auto mb-8 border border-destructive/20">
              <AlertOctagon className="h-12 w-12 text-destructive" aria-hidden="true" />
            </div>

            <h1
              className="text-3xl font-bold mb-4 font-heading"
              style={{ color: 'var(--ant-color-text)' }}
            >
              Kesalahan Kritis
            </h1>

            <p
              className="text-sm font-medium mb-8 leading-relaxed"
              style={{ color: 'var(--ant-color-text-secondary)' }}
            >
              Aplikasi mengalami kesalahan yang tidak dapat dipulihkan secara otomatis. Silakan muat ulang aplikasi atau hubungi dukungan jika masalah berlanjut.
            </p>

            {process.env.NODE_ENV === 'development' && (
              <div className="bg-destructive/10 rounded-2xl p-4 mb-8 text-left border border-destructive/20">
                <p className="text-xs font-bold text-destructive tracking-widest uppercase mb-2">
                  Detail Teknis
                </p>
                <p className="text-xs text-destructive/80 font-mono break-words">
                  {error.message}
                </p>
                {error.digest && (
                  <p
                    className="text-xs font-mono mt-2"
                    style={{ color: 'var(--ant-color-text-secondary)' }}
                  >
                    Digest: {error.digest}
                  </p>
                )}
                <p
                  className="text-xs font-mono mt-1"
                  style={{ color: 'var(--ant-color-text-secondary)' }}
                >
                  Stack: {error.stack?.split('\n').slice(0, 3).join(' | ')}
                </p>
              </div>
            )}

            <div className="space-y-3" role="group" aria-label="Tindakan pemulihan error kritis">
              {/* ponytail: ink (#06281F, Button primaryColor) keeps AA on emerald hover fill (F-01) */}
              <button
                onClick={handleReload}
                className="w-full py-4 rounded-xl font-bold text-xs tracking-widest bg-[var(--ant-color-text)] text-[var(--ant-color-bg-layout)] hover:bg-[var(--ant-color-primary)] hover:text-[#06281F] transition-all active:scale-95 shadow-xl flex items-center justify-center gap-2 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                aria-label="Muat ulang seluruh aplikasi"
              >
                <RefreshCw className="h-4 w-4" aria-hidden="true" />
                Muat Ulang Aplikasi
              </button>

              <button
                onClick={() => reset()}
                className="w-full border py-4 rounded-xl font-bold text-xs tracking-widest transition-all active:scale-95 flex items-center justify-center gap-2 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring hover:bg-[var(--ant-color-fill-secondary)]"
                style={{ borderColor: 'var(--ant-color-border-secondary)', color: 'var(--ant-color-text)', backgroundColor: 'color-mix(in srgb, var(--ant-color-text) 8%, transparent)' }}
                aria-label="Coba pulihkan aplikasi"
              >
                <RefreshCw className="h-4 w-4" aria-hidden="true" />
                Coba Pulihkan
              </button>
            </div>

            <div
              className="mt-8 pt-6 border-t"
              style={{ borderColor: 'var(--ant-color-border-secondary)' }}
            >
              <p
                className="text-xs font-bold tracking-widest mb-4"
                style={{ color: 'var(--ant-color-text-secondary)' }}
              >
                Butuh bantuan?
              </p>
              <div className="flex justify-center gap-6">
                <a
                  href="mailto:support@payu.id"
                  className="flex items-center gap-2 text-xs transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring rounded-xl px-3 py-3 min-h-[44px] hover:text-[var(--ant-color-primary)]"
                  style={{ color: 'var(--ant-color-text-secondary)' }}
                  aria-label="Kirim email ke tim dukungan"
                >
                  <Mail className="h-4 w-4" aria-hidden="true" />
                  support@payu.id
                </a>
                <a
                  href="tel:+6280012345678"
                  className="flex items-center gap-2 text-xs transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring rounded-xl px-3 py-3 min-h-[44px] hover:text-[var(--ant-color-primary)]"
                  style={{ color: 'var(--ant-color-text-secondary)' }}
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
