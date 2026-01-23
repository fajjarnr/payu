import { useTranslations } from 'next-intl';
import { Link } from '@/i18n/navigation';
import { QrCode, Clock, Smartphone } from 'lucide-react';

export default function QrisPaymentsPage() {
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
              <div className="pt-4">
                <p className="px-4 text-xs font-semibold text-muted-foreground uppercase tracking-wider mb-2">
                  {t('guides')}
                </p>
                <Link href="/guides/partner-payments" className="block px-4 py-2 rounded-xl text-muted-foreground hover:text-foreground hover:bg-accent/50 transition-colors">
                  {t('partnerPayments')}
                </Link>
                <Link href="/guides/qris-payments" className="block px-4 py-2 rounded-xl bg-accent text-bank-green font-medium">
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
                <span>{t('qrisPayments')}</span>
              </div>
              <h1 className="font-black text-4xl md:text-5xl tracking-tighter mb-4">
                Integrasi QRIS
              </h1>
              <p className="text-xl text-muted-foreground">
                Terima pembayaran QRIS dari semua e-wallet dan mobile banking di Indonesia.
              </p>
            </div>

            <section className="mb-16">
              <h2 className="font-black text-2xl tracking-tight mb-6">
                Fitur QRIS PayU
              </h2>
              <div className="grid md:grid-cols-3 gap-6">
                <div className="p-6 rounded-3xl border border-border bg-card">
                  <QrCode className="w-10 h-10 text-bank-green mb-4" />
                  <h3 className="font-black text-lg mb-2 tracking-tight">
                    QR Code Statis
                  </h3>
                  <p className="text-sm text-muted-foreground">
                    QR code yang bisa digunakan berulang untuk merchant dengan produk tetap
                  </p>
                </div>
                <div className="p-6 rounded-3xl border border-border bg-card">
                  <Smartphone className="w-10 h-10 text-bank-green mb-4" />
                  <h3 className="font-black text-lg mb-2 tracking-tight">
                    QR Code Dinamis
                  </h3>
                  <p className="text-sm text-muted-foreground">
                    QR code unik untuk setiap transaksi dengan jumlah yang berbeda
                  </p>
                </div>
                <div className="p-6 rounded-3xl border border-border bg-card">
                  <Clock className="w-10 h-10 text-bank-green mb-4" />
                  <h3 className="font-black text-lg mb-2 tracking-tight">
                    Real-time Notifikasi
                  </h3>
                  <p className="text-sm text-muted-foreground">
                    Terima notifikasi pembayaran secara langsung melalui webhook
                  </p>
                </div>
              </div>
            </section>

            <section className="mb-16">
              <h2 className="font-black text-2xl tracking-tight mb-6">
                Membuat QRIS Payment
              </h2>
              <div className="code-block">
                <pre>
                  <code>{`POST /v1/partner/payments
Authorization: Bearer {access_token}
Content-Type: application/json

{
  "amount": 50000,
  "currency": "IDR",
  "merchant_reference": "QRIS-ORD-001",
  "customer_id": "CUST-001",
  "description": "Pembayaran QRIS",
  "payment_method": "QRIS",
  "qris_type": "DYNAMIC",
  "expires_in": 3600,
  "callback_url": "https://your-app.com/webhook/qris"
}

Response:
{
  "payment_id": "PAY-qris123xyz",
  "status": "PENDING",
  "qr_code": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAA...",
  "qr_string": "00020101021226580016ID.CO.QRIS.WWW01189360052002200000303UMI51440014ID.CO.QRIS.WWW0215ID10200200000303UMI5802ID5910PayU Demo6007Jakarta6105101006304...",
  "expires_at": "2024-01-23T16:00:00Z"
}`}</code>
                </pre>
              </div>
            </section>

            <section className="mb-16">
              <h2 className="font-black text-2xl tracking-tight mb-6">
                Tipe QRIS
              </h2>
              <div className="space-y-6">
                <div className="p-6 rounded-3xl border border-border bg-card">
                  <h3 className="font-black text-lg mb-3 tracking-tight">
                    QRIS Statis
                  </h3>
                  <p className="text-muted-foreground mb-4">
                    Cocok untuk merchant dengan produk tetap atau tagihan rutin
                  </p>
                  <div className="code-block">
                    <pre>
                      <code>{`{
  "payment_method": "QRIS",
  "qris_type": "STATIC",
  "amount": 100000,
  "expires_in": null
}`}</code>
                    </pre>
                  </div>
                </div>

                <div className="p-6 rounded-3xl border border-border bg-card">
                  <h3 className="font-black text-lg mb-3 tracking-tight">
                    QRIS Dinamis
                  </h3>
                  <p className="text-muted-foreground mb-4">
                    Untuk transaksi dengan jumlah yang berbeda-beda
                  </p>
                  <div className="code-block">
                    <pre>
                      <code>{`{
  "payment_method": "QRIS",
  "qris_type": "DYNAMIC",
  "amount": 50000,
  "expires_in": 3600
}`}</code>
                    </pre>
                  </div>
                </div>
              </div>
            </section>

            <section className="mb-16">
              <h2 className="font-black text-2xl tracking-tight mb-6">
                Webhook Events
              </h2>
              <div className="space-y-4">
                {[
                  {
                    event: 'qris.pending',
                    description: 'QR code berhasil dibuat, menunggu pembayaran',
                  },
                  {
                    event: 'qris.completed',
                    description: 'Pembayaran berhasil diterima',
                  },
                  {
                    event: 'qris.expired',
                    description: 'QR code expired tanpa pembayaran',
                  },
                ].map((item, index) => (
                  <div key={index} className="p-4 rounded-2xl border border-border bg-card">
                    <code className="text-bank-green text-sm">{item.event}</code>
                    <p className="text-sm text-muted-foreground mt-2">{item.description}</p>
                  </div>
                ))}
              </div>
            </section>

            <section className="mb-16">
              <h2 className="font-black text-2xl tracking-tight mb-6">
                E-wallet yang Didukung
              </h2>
              <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                {['GoPay', 'OVO', 'DANA', 'LinkAja', 'ShopeePay', 'Jenius', 'BCA Mobile', 'Mandiri Online'].map((wallet, index) => (
                  <div key={index} className="p-4 rounded-2xl border border-border bg-card text-center">
                    <span className="text-sm font-medium">{wallet}</span>
                  </div>
                ))}
              </div>
            </section>
          </main>
        </div>
      </div>
    </div>
  );
}
