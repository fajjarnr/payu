import { useTranslations } from 'next-intl';
import { Link } from '@/i18n/navigation';
import { ArrowRight, CheckCircle2, Key, Shield, AlertCircle } from 'lucide-react';

export default function AuthPage() {
  const t = useTranslations('sidebar');

  return (
    <div className="min-h-screen bg-background">
      <header className="border-b border-border bg-card sticky top-0 z-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between h-16 items-center">
            <Link href="/" className="flex items-center space-x-2">
              <div className="w-8 h-8 rounded-full bg-bank-green" />
              <span className="font-black text-xl tracking-tighter">PayU</span>
            </Link>
            <div className="flex space-x-8">
              <Link href="/getting-started" className="text-bank-green font-medium">
                {t('gettingStarted')}
              </Link>
              <Link href="/guides/partner-payments" className="text-muted-foreground hover:text-foreground transition-colors">
                {t('guides')}
              </Link>
              <Link href="/sdk/java" className="text-muted-foreground hover:text-foreground transition-colors">
                {t('sdkExamples')}
              </Link>
            </div>
          </div>
        </div>
      </header>

      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
        <div className="flex gap-12">
          <aside className="w-64 shrink-0">
            <nav className="sticky top-24 space-y-1">
              <Link href="/getting-started" className="block px-4 py-2 rounded-xl text-muted-foreground hover:text-foreground hover:bg-accent/50 transition-colors">
                {t('quickStart')}
              </Link>
              <Link href="/getting-started/auth" className="block px-4 py-2 rounded-xl bg-accent text-bank-green font-medium">
                {t('authentication')}
              </Link>
              <Link href="/getting-started/webhooks" className="block px-4 py-2 rounded-xl text-muted-foreground hover:text-foreground hover:bg-accent/50 transition-colors">
                {t('webhooks')}
              </Link>
              <div className="pt-4">
                <p className="px-4 text-xs font-semibold text-muted-foreground uppercase tracking-wider mb-2">
                  {t('guides')}
                </p>
                <Link href="/guides/partner-payments" className="block px-4 py-2 rounded-xl text-muted-foreground hover:text-foreground hover:bg-accent/50 transition-colors">
                  {t('partnerPayments')}
                </Link>
                <Link href="/guides/qris-payments" className="block px-4 py-2 rounded-xl text-muted-foreground hover:text-foreground hover:bg-accent/50 transition-colors">
                  {t('qrisPayments')}
                </Link>
                <Link href="/guides/bifast-transfers" className="block px-4 py-2 rounded-xl text-muted-foreground hover:text-foreground hover:bg-accent/50 transition-colors">
                  {t('biFastTransfers')}
                </Link>
              </div>
            </nav>
          </aside>

          <main className="flex-1 min-w-0">
            <div className="mb-12">
              <div className="flex items-center gap-2 text-bank-green text-sm font-medium mb-4">
                <span>Getting Started</span>
                <span>/</span>
                <span>{t('authentication')}</span>
              </div>
              <h1 className="font-black text-4xl md:text-5xl tracking-tighter mb-4">
                Autentikasi
              </h1>
              <p className="text-xl text-muted-foreground">
                Pelajari cara mengautentikasi aplikasi Anda dengan API PayU menggunakan OAuth 2.0.
              </p>
            </div>

            <section className="mb-16">
              <h2 className="font-black text-2xl tracking-tight mb-6">
                Ikhtisar OAuth 2.0
              </h2>
              <p className="text-muted-foreground mb-4">
                PayU menggunakan OAuth 2.0 Client Credentials flow untuk autentikasi.
                Setiap permintaan API memerlukan access token yang valid.
              </p>
              <div className="p-6 rounded-3xl border border-border bg-card">
                <div className="flex items-start gap-3">
                  <Shield className="w-5 h-5 text-bank-green shrink-0 mt-0.5" />
                  <div>
                    <h3 className="font-bold mb-2">Keamanan Tingkat Tinggi</h3>
                    <p className="text-sm text-muted-foreground">
                      Semua komunikasi harus menggunakan HTTPS. Token memiliki masa berlaku
                      yang terbatas dan harus diperbarui secara berkala.
                    </p>
                  </div>
                </div>
              </div>
            </section>

            <section className="mb-16">
              <h2 className="font-black text-2xl tracking-tight mb-6">
                Mendapatkan Client Credentials
              </h2>
              <div className="space-y-3">
                {[
                  'Daftar akun developer di PayU Developer Portal',
                  'Buat aplikasi baru untuk mendapatkan Client ID',
                  'Generate Client Secret (simpan dengan aman)',
                  'Konfigurasi webhook URL dan allowed IPs',
                ].map((item, index) => (
                  <div key={index} className="flex items-center gap-3">
                    <CheckCircle2 className="w-5 h-5 text-bank-green shrink-0" />
                    <span>{item}</span>
                  </div>
                ))}
              </div>
            </section>

            <section className="mb-16">
              <h2 className="font-black text-2xl tracking-tight mb-6">
                Request Access Token
              </h2>
              <p className="text-muted-foreground mb-4">
                Gunakan Client ID dan Client Secret untuk mendapatkan access token:
              </p>
              <div className="code-block">
                <pre>
                  <code>{`POST /v1/partner/auth/token
Content-Type: application/json

{
  "client_id": "your_client_id",
  "client_secret": "your_client_secret",
  "grant_type": "client_credentials"
}`}</code>
                </pre>
              </div>
              <div className="mt-6">
                <h3 className="font-bold text-lg mb-4">Response</h3>
                <div className="code-block">
                  <pre>
                    <code>{`{
  "access_token": "eyJhbGciOiJIUzI1NiIs...",
  "token_type": "Bearer",
  "expires_in": 3600,
  "scope": "payments:read payments:write"
}`}</code>
                  </pre>
                </div>
              </div>
            </section>

            <section className="mb-16">
              <h2 className="font-black text-2xl tracking-tight mb-6">
                Menggunakan Access Token
              </h2>
              <p className="text-muted-foreground mb-4">
                Sertakan access token di header Authorization untuk setiap request:
              </p>
              <div className="code-block">
                <pre>
                  <code>{`GET /v1/partner/payments/{payment_id}
Authorization: Bearer {access_token}
Content-Type: application/json`}</code>
                </pre>
              </div>
            </section>

            <section className="mb-16">
              <h2 className="font-black text-2xl tracking-tight mb-6">
                Refresh Token
              </h2>
              <p className="text-muted-foreground mb-4">
                Access token berlaku selama 1 jam (3600 detik).
                Jika token expired, Anda akan menerima response 401 Unauthorized:
              </p>
              <div className="code-block">
                <pre>
                  <code>{`{
  "error": "UNAUTHORIZED",
  "message": "Access token expired or invalid"
}`}</code>
                </pre>
              </div>
              <p className="text-muted-foreground mt-4">
                Request token baru menggunakan endpoint yang sama dengan Client Credentials.
              </p>
            </section>

            <section className="mb-16">
              <h2 className="font-black text-2xl tracking-tight mb-6">
                Error Handling
              </h2>
              <div className="space-y-4">
                {[
                  {
                    code: 'INVALID_CLIENT',
                    description: 'Client ID atau Client Secret tidak valid',
                  },
                  {
                    code: 'INVALID_GRANT',
                    description: 'Grant type tidak didukung',
                  },
                  {
                    code: 'UNAUTHORIZED',
                    description: 'Access token expired atau tidak valid',
                  },
                ].map((error, index) => (
                  <div key={index} className="p-4 rounded-2xl border border-border bg-card">
                    <code className="text-sm font-bold text-red-600">{error.code}</code>
                    <p className="text-sm text-muted-foreground mt-1">{error.description}</p>
                  </div>
                ))}
              </div>
            </section>

            <section className="mb-16">
              <h2 className="font-black text-2xl tracking-tight mb-6">
                Best Practices
              </h2>
              <div className="p-6 rounded-3xl border-2 border-yellow-200 bg-yellow-50 dark:bg-yellow-950/20 mb-6">
                <div className="flex items-start gap-3">
                  <AlertCircle className="w-5 h-5 text-yellow-600 dark:text-yellow-400 shrink-0 mt-0.5" />
                  <div>
                    <h3 className="font-bold mb-2">Penting: Jangan expose Client Secret</h3>
                    <p className="text-sm text-muted-foreground">
                      Client Secret harus disimpan di server-side dan tidak boleh
                      di-hardcode di aplikasi client-side atau repository publik.
                    </p>
                  </div>
                </div>
              </div>
              <div className="space-y-3">
                {[
                  'Simpan Client Secret di environment variables',
                  'Gunakan token cache untuk menghindari request berulang',
                  'Refresh token sebelum expired (misal: 5 menit sebelumnya)',
                  'Gunakan HTTPS untuk semua komunikasi API',
                  'Implementasi retry dengan exponential backoff',
                ].map((item, index) => (
                  <div key={index} className="flex items-center gap-3">
                    <Key className="w-5 h-5 text-bank-green shrink-0" />
                    <span>{item}</span>
                  </div>
                ))}
              </div>
            </section>

            <section className="mb-16">
              <h2 className="font-black text-2xl tracking-tight mb-6">
                Langkah Berikutnya
              </h2>
              <div className="grid md:grid-cols-2 gap-6">
                <Link
                  href="/getting-started/webhooks"
                  className="p-6 rounded-3xl border border-border bg-card hover:shadow-xl transition-shadow group"
                >
                  <h3 className="font-black text-lg mb-2 tracking-tight group-hover:text-bank-green transition-colors">
                    Konfigurasi Webhooks
                  </h3>
                  <p className="text-muted-foreground text-sm">
                    Pelajari cara menerima notifikasi real-time dari PayU
                  </p>
                  <div className="flex items-center gap-2 mt-4 text-bank-green">
                    <span className="text-sm font-medium">Lanjutkan</span>
                    <ArrowRight className="w-4 h-4" />
                  </div>
                </Link>

                <Link
                  href="/guides/partner-payments"
                  className="p-6 rounded-3xl border border-border bg-card hover:shadow-xl transition-shadow group"
                >
                  <h3 className="font-black text-lg mb-2 tracking-tight group-hover:text-bank-green transition-colors">
                    Panduan Pembayaran
                  </h3>
                  <p className="text-muted-foreground text-sm">
                    Implementasi lengkap untuk integrasi pembayaran
                  </p>
                  <div className="flex items-center gap-2 mt-4 text-bank-green">
                    <span className="text-sm font-medium">Lihat Panduan</span>
                    <ArrowRight className="w-4 h-4" />
                  </div>
                </Link>
              </div>
            </section>
          </main>
        </div>
      </div>
    </div>
  );
}
