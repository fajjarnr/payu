import { useTranslations } from 'next-intl';
import { Link } from '@/i18n/navigation';
import { ArrowRight, CheckCircle2, Terminal } from 'lucide-react';
import { cn } from '@/lib/utils';

export default function GettingStartedPage() {
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
              <Link href="/getting-started" className="block px-4 py-2 rounded-xl bg-accent text-bank-green font-medium">
                {t('quickStart')}
              </Link>
              <Link href="/getting-started/auth" className="block px-4 py-2 rounded-xl text-muted-foreground hover:text-foreground hover:bg-accent/50 transition-colors">
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
              <h1 className="font-black text-4xl md:text-5xl tracking-tighter mb-4">
                Mulai Cepat
              </h1>
              <p className="text-xl text-muted-foreground">
                Pelajari cara mengintegrasikan PayU ke dalam aplikasi Anda dalam waktu kurang dari 5 menit.
              </p>
            </div>

            <section className="mb-16">
              <h2 className="font-black text-2xl tracking-tight mb-6">
                Prasyarat
              </h2>
              <div className="space-y-3">
                {[
                  'Akun developer PayU (gratis)',
                  'Client ID dan Client Secret',
                  'Webhook URL untuk menerima notifikasi',
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
                Langkah 1: Dapatkan Access Token
              </h2>
              <p className="text-muted-foreground mb-4">
                Semua permintaan API memerlukan access token. Dapatkan token dengan autentikasi OAuth2:
              </p>
              <div className="code-block">
                <pre>
                  <code>{`POST /v1/partner/auth/token
Content-Type: application/json

{
  "client_id": "your_client_id",
  "client_secret": "your_client_secret",
  "grant_type": "client_credentials"
}

Response:
{
  "access_token": "eyJhbGciOiJIUzI1NiIs...",
  "token_type": "Bearer",
  "expires_in": 3600
}`}</code>
                </pre>
              </div>
            </section>

            <section className="mb-16">
              <h2 className="font-black text-2xl tracking-tight mb-6">
                Langkah 2: Buat Pembayaran
              </h2>
              <p className="text-muted-foreground mb-4">
                Gunakan access token untuk membuat pembayaran baru:
              </p>
              <div className="code-block">
                <pre>
                  <code>{`POST /v1/partner/payments
Authorization: Bearer {access_token}
Content-Type: application/json

{
  "amount": 150000,
  "currency": "IDR",
  "merchant_reference": "ORD-12345",
  "customer_id": "CUST-001",
  "description": "Pembayaran TokoBapak",
  "payment_method": "QRIS",
  "callback_url": "https://your-app.com/webhook/payment"
}

Response:
{
  "payment_id": "PAY-abc123xyz",
  "status": "PENDING",
  "qr_code": "data:image/png;base64,...",
  "expires_at": "2024-01-23T16:00:00Z"
}`}</code>
                </pre>
              </div>
            </section>

            <section className="mb-16">
              <h2 className="font-black text-2xl tracking-tight mb-6">
                Langkah 3: Cek Status Pembayaran
              </h2>
              <p className="text-muted-foreground mb-4">
                Periksa status pembayaran menggunakan payment ID:
              </p>
              <div className="code-block">
                <pre>
                  <code>{`GET /v1/partner/payments/{payment_id}
Authorization: Bearer {access_token}

Response:
{
  "payment_id": "PAY-abc123xyz",
  "status": "COMPLETED",
  "amount": 150000,
  "currency": "IDR",
  "paid_at": "2024-01-23T15:30:00Z"
}`}</code>
                </pre>
              </div>
            </section>

            <section className="mb-16">
              <h2 className="font-black text-2xl tracking-tight mb-6">
                Langkah Berikutnya
              </h2>
              <div className="grid md:grid-cols-2 gap-6">
                <Link 
                  href="/guides/partner-payments"
                  className="p-6 rounded-3xl border border-border bg-card hover:shadow-xl transition-shadow group"
                >
                  <h3 className="font-black text-lg mb-2 tracking-tight group-hover:text-bank-green transition-colors">
                    Panduan Pembayaran Partner
                  </h3>
                  <p className="text-muted-foreground text-sm">
                    Pelajari implementasi lengkap untuk integrasi partner
                  </p>
                  <div className="flex items-center gap-2 mt-4 text-bank-green">
                    <span className="text-sm font-medium">Lanjutkan</span>
                    <ArrowRight className="w-4 h-4" />
                  </div>
                </Link>

                <Link 
                  href="/sdk/java"
                  className="p-6 rounded-3xl border border-border bg-card hover:shadow-xl transition-shadow group"
                >
                  <h3 className="font-black text-lg mb-2 tracking-tight group-hover:text-bank-green transition-colors">
                    SDK Contoh
                  </h3>
                  <p className="text-muted-foreground text-sm">
                    Gunakan SDK siap pakai untuk Java, Python, dan TypeScript
                  </p>
                  <div className="flex items-center gap-2 mt-4 text-bank-green">
                    <span className="text-sm font-medium">Lihat SDK</span>
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
