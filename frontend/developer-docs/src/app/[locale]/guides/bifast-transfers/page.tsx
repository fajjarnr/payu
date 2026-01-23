import { useTranslations } from 'next-intl';
import { Link } from '@/i18n/navigation';
import { Zap, Clock, Shield } from 'lucide-react';

export default function BifastTransfersPage() {
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
                <Link href="/guides/bifast-transfers" className="block px-4 py-2 rounded-xl bg-accent text-bank-green font-medium">
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
                <span>{t('biFastTransfers')}</span>
              </div>
              <h1 className="font-black text-4xl md:text-5xl tracking-tighter mb-4">
                Integrasi Transfer BI-FAST
              </h1>
              <p className="text-xl text-muted-foreground">
                Transfer uang real-time antar bank di Indonesia dengan BI-FAST.
              </p>
            </div>

            <section className="mb-16">
              <h2 className="font-black text-2xl tracking-tight mb-6">
                Keunggulan BI-FAST
              </h2>
              <div className="grid md:grid-cols-3 gap-6">
                <div className="p-6 rounded-3xl border border-border bg-card">
                  <Zap className="w-10 h-10 text-bank-green mb-4" />
                  <h3 className="font-black text-lg mb-2 tracking-tight">
                    Real-time
                  </h3>
                  <p className="text-sm text-muted-foreground">
                    Dana diterima dalam hitungan detik, 24/7
                  </p>
                </div>
                <div className="p-6 rounded-3xl border border-border bg-card">
                  <Clock className="w-10 h-10 text-bank-green mb-4" />
                  <h3 className="font-black text-lg mb-2 tracking-tight">
                    Biaya Rendah
                  </h3>
                  <p className="text-sm text-muted-foreground">
                    Biaya transfer jauh lebih rendah dari SKN/RTGS
                  </p>
                </div>
                <div className="p-6 rounded-3xl border border-border bg-card">
                  <Shield className="w-10 h-10 text-bank-green mb-4" />
                  <h3 className="font-black text-lg mb-2 tracking-tight">
                    Aman
                  </h3>
                  <p className="text-sm text-muted-foreground">
                    Dikelola langsung oleh Bank Indonesia
                  </p>
                </div>
              </div>
            </section>

            <section className="mb-16">
              <h2 className="font-black text-2xl tracking-tight mb-6">
                Transfer BI-FAST
              </h2>
              <div className="code-block">
                <pre>
                  <code>{`POST /v1/partner/transfers/bifast
Authorization: Bearer {access_token}
Content-Type: application/json

{
  "beneficiary_account": "1234567890",
  "beneficiary_bank_code": "CENAIDJA",
  "amount": 1000000,
  "currency": "IDR",
  "reference_number": "BIFAST-001",
  "sender_name": "John Doe",
  "beneficiary_name": "Jane Smith",
  "description": "Transfer BI-FAST",
  "callback_url": "https://your-app.com/webhook/transfer"
}

Response:
{
  "transaction_id": "TXN-bifast123xyz",
  "status": "COMPLETED",
  "amount": 1000000,
  "currency": "IDR",
  "beneficiary_account": "1234567890",
  "beneficiary_bank_code": "CENAIDJA",
  "created_at": "2024-01-23T15:30:00Z",
  "completed_at": "2024-01-23T15:30:05Z"
}`}</code>
                </pre>
              </div>
            </section>

            <section className="mb-16">
              <h2 className="font-black text-2xl tracking-tight mb-6">
                Bank yang Didukung
              </h2>
              <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
                {[
                  { code: 'CENAIDJA', name: 'BCA' },
                  { code: 'BMRIIDJA', name: 'Bank Mandiri' },
                  { code: 'BBBAIDJA', name: 'Bank BRI' },
                  { code: 'BNINIDJA', name: 'Bank BNI' },
                  { code: 'CITYIDJA', name: 'Citibank' },
                  { code: 'HSBCIDJA', name: 'HSBC' },
                  { code: 'BNIAIDJA', name: 'Bank CIMB Niaga' },
                  { code: 'DMASIDJA', name: 'Bank Danamon' },
                  { code: 'BACAIDJA', name: 'Bank UOB' },
                ].map((bank, index) => (
                  <div key={index} className="p-4 rounded-2xl border border-border bg-card">
                    <code className="text-xs text-bank-green">{bank.code}</code>
                    <p className="text-sm font-medium mt-1">{bank.name}</p>
                  </div>
                ))}
              </div>
            </section>

            <section className="mb-16">
              <h2 className="font-black text-2xl tracking-tight mb-6">
                Limit Transfer
              </h2>
              <div className="p-6 rounded-3xl border border-border bg-card">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b border-border">
                      <th className="text-left py-2">Jenis Transfer</th>
                      <th className="text-left py-2">Minimum</th>
                      <th className="text-left py-2">Maksimum</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr className="border-b border-border/50">
                      <td className="py-3">Retail</td>
                      <td>Rp 10.000</td>
                      <td>Rp 2.500.000</td>
                    </tr>
                    <tr>
                      <td className="py-3">Bulk (Batch)</td>
                      <td>Rp 10.000</td>
                      <td>Rp 10.000.000</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </section>

            <section className="mb-16">
              <h2 className="font-black text-2xl tracking-tight mb-6">
                Webhook Events
              </h2>
              <div className="space-y-4">
                {[
                  {
                    event: 'bifast.pending',
                    description: 'Transfer sedang diproses',
                  },
                  {
                    event: 'bifast.completed',
                    description: 'Transfer berhasil diterima penerima',
                  },
                  {
                    event: 'bifast.failed',
                    description: 'Transfer gagal (saldo tidak cukup, account tidak valid, dll)',
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
                Error Handling
              </h2>
              <div className="code-block">
                <pre>
                  <code>{`// Error Response Format
{
  "error": {
    "code": "INSUFFICIENT_BALANCE",
    "message": "Saldo tidak mencukupi",
    "transaction_id": "TXN-bifast123xyz"
  }
}

// Common Error Codes
// INSUFFICIENT_BALANCE: Saldo tidak cukup
// INVALID_ACCOUNT: Nomor rekening tidak valid
// BANK_NOT_SUPPORTED: Bank tidak mendukung BI-FAST
// LIMIT_EXCEEDED: Melebihi limit transfer
// DUPLICATE_REFERENCE: Nomor referensi sudah digunakan`}</code>
                </pre>
              </div>
            </section>
          </main>
        </div>
      </div>
    </div>
  );
}
