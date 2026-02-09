import { useTranslations } from 'next-intl';
import { Link } from '@/i18n/navigation';
import { TrendingUp, Shield, PieChart } from 'lucide-react';

export default function InvestmentsGuidePage() {
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
                <Link href="/guides/qris-payments" className="block px-4 py-2 rounded-xl text-muted-foreground hover:text-foreground hover:bg-accent/50 transition-colors">
                  {t('qrisPayments')}
                </Link>
                <Link href="/guides/bifast-transfers" className="block px-4 py-2 rounded-xl text-muted-foreground hover:text-foreground hover:bg-accent/50 transition-colors">
                  {t('biFastTransfers')}
                </Link>
                <Link href="/guides/investments" className="block px-4 py-2 rounded-xl bg-accent text-bank-green font-medium">
                  Investasi
                </Link>
                <Link href="/guides/lending" className="block px-4 py-2 rounded-xl text-muted-foreground hover:text-foreground hover:bg-accent/50 transition-colors">
                  Pinjaman
                </Link>
              </div>
            </nav>
          </aside>

          <main className="flex-1 min-w-0">
            <div className="mb-12">
              <div className="flex items-center gap-2 text-bank-green text-sm font-medium mb-4">
                <span>Panduan Integrasi</span>
                <span>/</span>
                <span>Investasi</span>
              </div>
              <h1 className="font-black text-4xl md:text-5xl tracking-tighter mb-4">
                Investment Service API
              </h1>
              <p className="text-xl text-muted-foreground">
                Kelola produk investasi, portofolio, dan transaksi pembelian/penjualan.
              </p>
            </div>

            <section className="mb-16">
              <h2 className="font-black text-2xl tracking-tight mb-6">
                Fitur Utama
              </h2>
              <div className="grid md:grid-cols-3 gap-6">
                <div className="p-6 rounded-3xl border border-border bg-card">
                  <TrendingUp className="w-10 h-10 text-bank-green mb-4" />
                  <h3 className="font-black text-lg mb-2 tracking-tight">
                    Multi-Produk
                  </h3>
                  <p className="text-muted-foreground">
                    Mendukung Reksa Dana, Deposito, SBN, dan Emas dengan satu API terpadu.
                  </p>
                </div>
                <div className="p-6 rounded-3xl border border-border bg-card">
                  <Shield className="w-10 h-10 text-bank-green mb-4" />
                  <h3 className="font-black text-lg mb-2 tracking-tight">
                    KYC Terintegrasi
                  </h3>
                  <p className="text-muted-foreground">
                    Validasi SID dan profil risiko investor sebelum transaksi.
                  </p>
                </div>
                <div className="p-6 rounded-3xl border border-border bg-card">
                  <PieChart className="w-10 h-10 text-bank-green mb-4" />
                  <h3 className="font-black text-lg mb-2 tracking-tight">
                    Portofolio Real-time
                  </h3>
                  <p className="text-muted-foreground">
                    Tracking NAV dan return portofolio secara real-time.
                  </p>
                </div>
              </div>
            </section>

            <section className="mb-16">
              <h2 className="font-black text-2xl tracking-tight mb-6">
                Endpoint Tersedia
              </h2>
              <div className="space-y-4">
                <div className="p-4 rounded-2xl border border-border bg-card">
                  <div className="flex items-center gap-3 mb-2">
                    <span className="px-2 py-0.5 rounded-md bg-green-100 text-green-800 text-xs font-mono font-bold">GET</span>
                    <code className="text-sm font-mono">/api/v1/investments/products</code>
                  </div>
                  <p className="text-sm text-muted-foreground ml-14">
                    Daftar semua produk investasi yang tersedia. Mendukung filter berdasarkan tipe, risiko, dan return.
                  </p>
                </div>
                <div className="p-4 rounded-2xl border border-border bg-card">
                  <div className="flex items-center gap-3 mb-2">
                    <span className="px-2 py-0.5 rounded-md bg-green-100 text-green-800 text-xs font-mono font-bold">GET</span>
                    <code className="text-sm font-mono">/api/v1/investments/portfolio</code>
                  </div>
                  <p className="text-sm text-muted-foreground ml-14">
                    Ambil portofolio investasi milik user beserta ringkasan return.
                  </p>
                </div>
                <div className="p-4 rounded-2xl border border-border bg-card">
                  <div className="flex items-center gap-3 mb-2">
                    <span className="px-2 py-0.5 rounded-md bg-blue-100 text-blue-800 text-xs font-mono font-bold">POST</span>
                    <code className="text-sm font-mono">/api/v1/investments/buy</code>
                  </div>
                  <p className="text-sm text-muted-foreground ml-14">
                    Pembelian produk investasi. Memerlukan header <code>X-Idempotency-Key</code>.
                  </p>
                </div>
                <div className="p-4 rounded-2xl border border-border bg-card">
                  <div className="flex items-center gap-3 mb-2">
                    <span className="px-2 py-0.5 rounded-md bg-blue-100 text-blue-800 text-xs font-mono font-bold">POST</span>
                    <code className="text-sm font-mono">/api/v1/investments/sell</code>
                  </div>
                  <p className="text-sm text-muted-foreground ml-14">
                    Penjualan/redemption produk investasi. Proses T+2 untuk reksa dana.
                  </p>
                </div>
                <div className="p-4 rounded-2xl border border-border bg-card">
                  <div className="flex items-center gap-3 mb-2">
                    <span className="px-2 py-0.5 rounded-md bg-green-100 text-green-800 text-xs font-mono font-bold">GET</span>
                    <code className="text-sm font-mono">/api/v1/investments/transactions</code>
                  </div>
                  <p className="text-sm text-muted-foreground ml-14">
                    Riwayat transaksi investasi dengan pagination dan filter tanggal.
                  </p>
                </div>
              </div>
            </section>

            <section className="mb-16">
              <h2 className="font-black text-2xl tracking-tight mb-6">
                Contoh: Pembelian Reksa Dana
              </h2>
              <div className="rounded-2xl border border-border bg-zinc-950 p-6 overflow-x-auto">
                <pre className="text-sm text-zinc-300 font-mono">
{`POST /api/v1/investments/buy
Authorization: Bearer {access_token}
X-Idempotency-Key: inv-buy-uuid-001
Content-Type: application/json

{
  "productId": "MF-EQUITY-001",
  "productType": "MUTUAL_FUND",
  "amount": 1000000,
  "currency": "IDR",
  "sourceWalletId": "wallet-uuid-123",
  "riskAcknowledged": true
}

// Response 201 Created
{
  "transactionId": "inv-txn-uuid-456",
  "status": "PROCESSING",
  "productId": "MF-EQUITY-001",
  "productName": "PayU Equity Growth Fund",
  "amount": 1000000,
  "estimatedUnits": 523.45,
  "navDate": "2025-02-09",
  "settlementDate": "2025-02-11",
  "createdAt": "2025-02-09T10:30:00Z"
}`}
                </pre>
              </div>
            </section>

            <section className="mb-16">
              <h2 className="font-black text-2xl tracking-tight mb-6">
                Webhook Events
              </h2>
              <div className="space-y-4">
                <div className="p-4 rounded-2xl border border-border bg-card">
                  <code className="text-sm font-mono text-bank-green font-bold">investment.buy.completed</code>
                  <p className="text-sm text-muted-foreground mt-1">
                    Transaksi pembelian telah selesai dan unit telah dialokasikan ke portofolio investor.
                  </p>
                </div>
                <div className="p-4 rounded-2xl border border-border bg-card">
                  <code className="text-sm font-mono text-bank-green font-bold">investment.sell.completed</code>
                  <p className="text-sm text-muted-foreground mt-1">
                    Redemption selesai dan dana telah dikreditkan ke wallet investor.
                  </p>
                </div>
                <div className="p-4 rounded-2xl border border-border bg-card">
                  <code className="text-sm font-mono text-bank-green font-bold">investment.nav.updated</code>
                  <p className="text-sm text-muted-foreground mt-1">
                    NAV produk telah diperbarui — portofolio investor otomatis direcalculate.
                  </p>
                </div>
              </div>
            </section>

            <section className="mb-16">
              <h2 className="font-black text-2xl tracking-tight mb-6">
                Error Codes
              </h2>
              <div className="overflow-x-auto rounded-2xl border border-border">
                <table className="w-full text-sm">
                  <thead className="bg-muted/50">
                    <tr>
                      <th className="text-left p-4 font-bold">Code</th>
                      <th className="text-left p-4 font-bold">Deskripsi</th>
                      <th className="text-left p-4 font-bold">Solusi</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-border">
                    <tr>
                      <td className="p-4 font-mono">INV_001</td>
                      <td className="p-4">Produk tidak ditemukan</td>
                      <td className="p-4 text-muted-foreground">Cek productId valid di endpoint /products</td>
                    </tr>
                    <tr>
                      <td className="p-4 font-mono">INV_002</td>
                      <td className="p-4">Saldo wallet tidak mencukupi</td>
                      <td className="p-4 text-muted-foreground">Top-up wallet sebelum pembelian</td>
                    </tr>
                    <tr>
                      <td className="p-4 font-mono">INV_003</td>
                      <td className="p-4">Profil risiko tidak sesuai</td>
                      <td className="p-4 text-muted-foreground">Update profil risiko atau pilih produk lain</td>
                    </tr>
                    <tr>
                      <td className="p-4 font-mono">INV_004</td>
                      <td className="p-4">Minimum investasi tidak terpenuhi</td>
                      <td className="p-4 text-muted-foreground">Cek minimum amount di detail produk</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </section>
          </main>
        </div>
      </div>
    </div>
  );
}
