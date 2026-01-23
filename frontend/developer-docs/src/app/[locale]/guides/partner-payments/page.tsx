import { useTranslations } from 'next-intl';
import { Link } from '@/i18n/navigation';
import { ArrowRight, AlertTriangle, CheckCircle2, Copy } from 'lucide-react';
import { cn } from '@/lib/utils';

export default function PartnerPaymentsPage() {
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
              <Link href="/getting-started" className="text-muted-foreground hover:text-foreground transition-colors">
                {t('gettingStarted')}
              </Link>
              <Link href="/guides/partner-payments" className="text-bank-green font-medium">
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
              <div className="pt-4">
                <p className="px-4 text-xs font-semibold text-muted-foreground uppercase tracking-wider mb-2">
                  {t('guides')}
                </p>
                <Link href="/guides/partner-payments" className="block px-4 py-2 rounded-xl bg-accent text-bank-green font-medium">
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
                <span>Panduan Integrasi</span>
                <span>/</span>
                <span>{t('partnerPayments')}</span>
              </div>
              <h1 className="font-black text-4xl md:text-5xl tracking-tighter mb-4">
                Integrasi Pembayaran Partner
              </h1>
              <p className="text-xl text-muted-foreground">
                Panduan lengkap untuk mengintegrasikan PayU sebagai payment gateway untuk aplikasi e-commerce Anda.
              </p>
            </div>

            <section className="mb-16">
              <h2 className="font-black text-2xl tracking-tight mb-6">
                Gambaran Alur
              </h2>
              <div className="p-6 rounded-3xl border border-border bg-card mb-6">
                <ol className="space-y-4">
                  {[
                    'Customer memilih item di aplikasi Anda',
                    'Aplikasi Anda membuat request pembayaran ke PayU',
                    'PayU mengembalikan QR Code atau payment link',
                    'Customer melakukan pembayaran',
                    'PayU mengirim webhook notifikasi ke aplikasi Anda',
                    'Aplikasi Anda mengkonfirmasi pembayaran',
                  ].map((step, index) => (
                    <li key={index} className="flex items-start gap-4">
                      <div className="w-8 h-8 rounded-full bg-bank-green text-white flex items-center justify-center font-bold shrink-0">
                        {index + 1}
                      </div>
                      <span>{step}</span>
                    </li>
                  ))}
                </ol>
              </div>
            </section>

            <section className="mb-16">
              <h2 className="font-black text-2xl tracking-tight mb-6">
                API Endpoints
              </h2>
              <div className="space-y-4">
                {[
                  {
                    method: 'POST',
                    endpoint: '/v1/partner/auth/token',
                    description: 'Dapatkan access token untuk autentikasi',
                  },
                  {
                    method: 'POST',
                    endpoint: '/v1/partner/payments',
                    description: 'Buat pembayaran baru',
                  },
                  {
                    method: 'GET',
                    endpoint: '/v1/partner/payments/{id}',
                    description: 'Cek status pembayaran',
                  },
                  {
                    method: 'POST',
                    endpoint: '/v1/partner/payments/{id}/refund',
                    description: 'Refund pembayaran',
                  },
                ].map((api, index) => (
                  <div key={index} className="p-4 rounded-2xl border border-border bg-card">
                    <div className="flex items-center gap-3 mb-2">
                      <span className={`px-2 py-1 rounded-lg text-xs font-bold ${
                        api.method === 'POST' ? 'bg-blue-100 text-blue-700' : 'bg-green-100 text-green-700'
                      }`}>
                        {api.method}
                      </span>
                      <code className="text-sm">{api.endpoint}</code>
                    </div>
                    <p className="text-sm text-muted-foreground">{api.description}</p>
                  </div>
                ))}
              </div>
            </section>

            <section className="mb-16">
              <h2 className="font-black text-2xl tracking-tight mb-6">
                Implementasi Webhook
              </h2>
              <p className="text-muted-foreground mb-4">
                Terima notifikasi pembayaran secara real-time melalui webhook:
              </p>
              <div className="code-block">
                <pre>
                  <code>{`POST https://your-app.com/webhook/payment
Content-Type: application/json
X-PayU-Signature: sha256=...

{
  "event_type": "payment.completed",
  "payment_id": "PAY-abc123xyz",
  "status": "COMPLETED",
  "amount": 150000,
  "currency": "IDR",
  "merchant_reference": "ORD-12345",
  "customer_id": "CUST-001",
  "paid_at": "2024-01-23T15:30:00Z"
}

// Validasi signature (penting untuk keamanan)
const hmac = crypto.createHmac('sha256', WEBHOOK_SECRET);
hmac.update(JSON.stringify(payload));
const signature = hmac.digest('hex');

if (signature !== receivedSignature) {
  throw new Error('Invalid signature');
}`}</code>
                </pre>
              </div>
            </section>

            <section className="mb-16">
              <h2 className="font-black text-2xl tracking-tight mb-6">
                Security Best Practices
              </h2>
              <div className="p-6 rounded-3xl border-2 border-yellow-200 bg-yellow-50 dark:bg-yellow-950/20 mb-6">
                <div className="flex items-start gap-3">
                  <AlertTriangle className="w-5 h-5 text-yellow-600 dark:text-yellow-400 shrink-0 mt-0.5" />
                  <div>
                    <h3 className="font-bold mb-2">Penting: Selalu validasi webhook signature</h3>
                    <p className="text-sm text-muted-foreground">
                      Setiap webhook request memiliki signature di header X-PayU-Signature. Validasi signature ini 
                      untuk memastikan request berasal dari PayU dan bukan dari pihak ketiga.
                    </p>
                  </div>
                </div>
              </div>

              <div className="space-y-3">
                {[
                  'Gunakan HTTPS untuk semua endpoint webhook',
                  'Validasi X-PayU-Signature header',
                  'Simpan WEBHOOK_SECRET di environment variable',
                  'Implementasi idempotency untuk menangani duplicate webhooks',
                  'Timeout untuk webhook response maksimal 5 detik',
                  'Return HTTP 200 untuk acknowledge webhook',
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
                Error Handling
              </h2>
              <div className="code-block">
                <pre>
                  <code>{`// Error Response Format
{
  "error": {
    "code": "INSUFFICIENT_BALANCE",
    "message": "Saldo tidak mencukupi untuk melakukan transaksi",
    "details": {
      "required": 200000,
      "available": 150000
    }
  }
}

// Common Error Codes
// INVALID_TOKEN: Access token tidak valid atau expired
// INVALID_AMOUNT: Jumlah pembayaran tidak valid
// DUPLICATE_REFERENCE: Reference number sudah digunakan
// PAYMENT_FAILED: Pembayaran gagal diproses`}</code>
                </pre>
              </div>
            </section>

            <section className="mb-16">
              <h2 className="font-black text-2xl tracking-tight mb-6">
                Contoh Implementasi Lengkap
              </h2>
              <div className="p-6 rounded-3xl border border-border bg-card">
                <p className="text-muted-foreground mb-4">
                  Lihat contoh implementasi lengkap di SDK kami:
                </p>
                <div className="flex gap-4">
                  <Link href="/sdk/java" className="px-4 py-2 rounded-xl bg-bank-green text-white font-medium text-sm hover:opacity-90 transition-opacity">
                    Java SDK
                  </Link>
                  <Link href="/sdk/python" className="px-4 py-2 rounded-xl border border-border bg-card text-foreground font-medium text-sm hover:bg-accent transition-colors">
                    Python SDK
                  </Link>
                  <Link href="/sdk/typescript" className="px-4 py-2 rounded-xl border border-border bg-card text-foreground font-medium text-sm hover:bg-accent transition-colors">
                    TypeScript SDK
                  </Link>
                </div>
              </div>
            </section>
          </main>
        </div>
      </div>
    </div>
  );
}
