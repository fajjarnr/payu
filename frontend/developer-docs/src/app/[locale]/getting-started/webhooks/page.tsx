import { useTranslations } from 'next-intl';
import { Link } from '@/i18n/navigation';
import { ArrowRight, CheckCircle2, Bell, Shield, AlertTriangle, RefreshCw } from 'lucide-react';

export default function WebhooksPage() {
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
              <Link href="/getting-started/auth" className="block px-4 py-2 rounded-xl text-muted-foreground hover:text-foreground hover:bg-accent/50 transition-colors">
                {t('authentication')}
              </Link>
              <Link href="/getting-started/webhooks" className="block px-4 py-2 rounded-xl bg-accent text-bank-green font-medium">
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
                <span>{t('webhooks')}</span>
              </div>
              <h1 className="font-black text-4xl md:text-5xl tracking-tighter mb-4">
                Webhooks
              </h1>
              <p className="text-xl text-muted-foreground">
                Terima notifikasi real-time untuk event pembayaran dan transaksi.
              </p>
            </div>

            <section className="mb-16">
              <h2 className="font-black text-2xl tracking-tight mb-6">
                Apa itu Webhooks?
              </h2>
              <p className="text-muted-foreground mb-4">
                Webhooks memungkinkan PayU mengirim notifikasi ke aplikasi Anda secara real-time
                ketika terjadi event tertentu, seperti pembayaran selesai atau refund diproses.
              </p>
              <div className="p-6 rounded-3xl border border-border bg-card">
                <div className="flex items-start gap-3">
                  <Bell className="w-5 h-5 text-bank-green shrink-0 mt-0.5" />
                  <div>
                    <h3 className="font-bold mb-2">Notifikasi Real-time</h3>
                    <p className="text-sm text-muted-foreground">
                      Tidak perlu polling berulang. Dapatkan update instan saat status
                      pembayaran berubah.
                    </p>
                  </div>
                </div>
              </div>
            </section>

            <section className="mb-16">
              <h2 className="font-black text-2xl tracking-tight mb-6">
                Event Types
              </h2>
              <div className="space-y-4">
                {[
                  {
                    event: 'payment.created',
                    description: 'Pembayaran baru berhasil dibuat',
                  },
                  {
                    event: 'payment.pending',
                    description: 'Pembayaran menunggu konfirmasi customer',
                  },
                  {
                    event: 'payment.completed',
                    description: 'Pembayaran berhasil diselesaikan',
                  },
                  {
                    event: 'payment.failed',
                    description: 'Pembayaran gagal diproses',
                  },
                  {
                    event: 'payment.expired',
                    description: 'Pembayaran melewati batas waktu',
                  },
                  {
                    event: 'payment.refunded',
                    description: 'Refund berhasil diproses',
                  },
                ].map((item, index) => (
                  <div key={index} className="p-4 rounded-2xl border border-border bg-card">
                    <code className="text-sm font-bold text-bank-green">{item.event}</code>
                    <p className="text-sm text-muted-foreground mt-1">{item.description}</p>
                  </div>
                ))}
              </div>
            </section>

            <section className="mb-16">
              <h2 className="font-black text-2xl tracking-tight mb-6">
                Payload Webhook
              </h2>
              <p className="text-muted-foreground mb-4">
                Setiap webhook request memiliki struktur JSON yang konsisten:
              </p>
              <div className="code-block">
                <pre>
                  <code>{`{
  "event_type": "payment.completed",
  "event_id": "evt_abc123xyz",
  "timestamp": "2024-01-23T15:30:00Z",
  "data": {
    "payment_id": "PAY-abc123xyz",
    "status": "COMPLETED",
    "amount": 150000,
    "currency": "IDR",
    "merchant_reference": "ORD-12345",
    "customer_id": "CUST-001",
    "payment_method": "QRIS",
    "paid_at": "2024-01-23T15:30:00Z"
  }
}`}</code>
                </pre>
              </div>
            </section>

            <section className="mb-16">
              <h2 className="font-black text-2xl tracking-tight mb-6">
                Validasi Signature
              </h2>
              <p className="text-muted-foreground mb-4">
                Setiap webhook menyertakan signature di header untuk verifikasi keamanan:
              </p>
              <div className="code-block">
                <pre>
                  <code>{`POST https://your-app.com/webhook/payment
Content-Type: application/json
X-PayU-Signature: sha256=abc123def456...
X-PayU-Event-ID: evt_abc123xyz

{
  "event_type": "payment.completed",
  ...
}`}</code>
                </pre>
              </div>
              <div className="mt-6">
                <h3 className="font-bold text-lg mb-4">Cara Validasi</h3>
                <div className="code-block">
                  <pre>
                    <code>{`// Node.js example
const crypto = require('crypto');

function verifyWebhookSignature(payload, signature, secret) {
  const hmac = crypto.createHmac('sha256', secret);
  hmac.update(JSON.stringify(payload));
  const expectedSignature = 'sha256=' + hmac.digest('hex');

  return crypto.timingSafeEqual(
    Buffer.from(signature),
    Buffer.from(expectedSignature)
  );
}

// Usage
const isValid = verifyWebhookSignature(
  req.body,
  req.headers['x-payu-signature'],
  process.env.WEBHOOK_SECRET
);

if (!isValid) {
  return res.status(401).json({ error: 'Invalid signature' });
}`}</code>
                  </pre>
                </div>
              </div>
            </section>

            <section className="mb-16">
              <h2 className="font-black text-2xl tracking-tight mb-6">
                Konfigurasi Webhook URL
              </h2>
              <div className="space-y-3">
                {[
                  'Login ke PayU Developer Portal',
                  'Pilih aplikasi yang ingin dikonfigurasi',
                  'Masuk ke menu Webhooks',
                  'Tambahkan endpoint URL aplikasi Anda',
                  'Pilih event types yang ingin didengarkan',
                  'Simpan dan test webhook',
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
                Response Requirements
              </h2>
              <p className="text-muted-foreground mb-4">
                Endpoint webhook Anda harus merespon dengan:
              </p>
              <div className="space-y-4">
                <div className="p-4 rounded-2xl border border-border bg-card">
                  <div className="flex items-center gap-2 mb-2">
                    <span className="px-2 py-1 rounded-lg text-xs font-bold bg-green-100 text-green-700">200 OK</span>
                    <span className="text-sm">Response sukses</span>
                  </div>
                  <p className="text-sm text-muted-foreground">
                    PayU akan menganggap webhook berhasil diproses
                  </p>
                </div>
                <div className="p-4 rounded-2xl border border-border bg-card">
                  <div className="flex items-center gap-2 mb-2">
                    <span className="px-2 py-1 rounded-lg text-xs font-bold bg-red-100 text-red-700">4xx/5xx</span>
                    <span className="text-sm">Response gagal</span>
                  </div>
                  <p className="text-sm text-muted-foreground">
                    PayU akan retry pengiriman webhook
                  </p>
                </div>
              </div>
            </section>

            <section className="mb-16">
              <h2 className="font-black text-2xl tracking-tight mb-6">
                Retry Policy
              </h2>
              <p className="text-muted-foreground mb-4">
                Jika endpoint Anda tidak merespon atau merespon error,
                PayU akan melakukan retry dengan exponential backoff:
              </p>
              <div className="space-y-3">
                {[
                  'Retry 1: 5 detik setelah attempt pertama',
                  'Retry 2: 25 detik setelah retry 1',
                  'Retry 3: 2 menit setelah retry 2',
                  'Retry 4: 10 menit setelah retry 3',
                  'Retry 5: 1 jam setelah retry 4',
                ].map((item, index) => (
                  <div key={index} className="flex items-center gap-3">
                    <RefreshCw className="w-5 h-5 text-bank-green shrink-0" />
                    <span>{item}</span>
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
                  <AlertTriangle className="w-5 h-5 text-yellow-600 dark:text-yellow-400 shrink-0 mt-0.5" />
                  <div>
                    <h3 className="font-bold mb-2">Penting: Selalu validasi signature</h3>
                    <p className="text-sm text-muted-foreground">
                      Tanpa validasi signature, endpoint Anda rentan terhadap
                      request palsu dari pihak ketiga.
                    </p>
                  </div>
                </div>
              </div>
              <div className="space-y-3">
                {[
                  'Gunakan HTTPS untuk semua webhook endpoints',
                  'Validasi X-PayU-Signature header setiap request',
                  'Implementasi idempotency untuk menangani duplicate events',
                  'Respon dengan HTTP 200 secepat mungkin (max 5 detik)',
                  'Proses webhook secara asynchronous jika memerlukan waktu lama',
                  'Simpan event_id untuk deduplication',
                ].map((item, index) => (
                  <div key={index} className="flex items-center gap-3">
                    <Shield className="w-5 h-5 text-bank-green shrink-0" />
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
                  href="/guides/partner-payments"
                  className="p-6 rounded-3xl border border-border bg-card hover:shadow-xl transition-shadow group"
                >
                  <h3 className="font-black text-lg mb-2 tracking-tight group-hover:text-bank-green transition-colors">
                    Panduan Pembayaran Partner
                  </h3>
                  <p className="text-muted-foreground text-sm">
                    Pelajari implementasi lengkap integrasi pembayaran
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
                    Lihat contoh implementasi webhook di berbagai bahasa
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
