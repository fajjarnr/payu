import { useTranslations } from 'next-intl';
import { Link } from '@/i18n/navigation';
import { Banknote, Clock, FileCheck } from 'lucide-react';

export default function LendingGuidePage() {
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
                <Link href="/guides/investments" className="block px-4 py-2 rounded-xl text-muted-foreground hover:text-foreground hover:bg-accent/50 transition-colors">
                  Investasi
                </Link>
                <Link href="/guides/lending" className="block px-4 py-2 rounded-xl bg-accent text-bank-green font-medium">
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
                <span>Pinjaman</span>
              </div>
              <h1 className="font-black text-4xl md:text-5xl tracking-tighter mb-4">
                Lending Service API
              </h1>
              <p className="text-xl text-muted-foreground">
                Integrasi layanan pinjaman digital — pengajuan, persetujuan, pencairan, dan pembayaran cicilan.
              </p>
            </div>

            <section className="mb-16">
              <h2 className="font-black text-2xl tracking-tight mb-6">
                Fitur Utama
              </h2>
              <div className="grid md:grid-cols-3 gap-6">
                <div className="p-6 rounded-3xl border border-border bg-card">
                  <Banknote className="w-10 h-10 text-bank-green mb-4" />
                  <h3 className="font-black text-lg mb-2 tracking-tight">
                    Multi-Produk
                  </h3>
                  <p className="text-muted-foreground">
                    Personal Loan, KTA, Kredit Mikro, dan Buy Now Pay Later dalam satu API.
                  </p>
                </div>
                <div className="p-6 rounded-3xl border border-border bg-card">
                  <Clock className="w-10 h-10 text-bank-green mb-4" />
                  <h3 className="font-black text-lg mb-2 tracking-tight">
                    Instant Decision
                  </h3>
                  <p className="text-muted-foreground">
                    Credit scoring dan keputusan pinjaman dalam hitungan detik via ML engine.
                  </p>
                </div>
                <div className="p-6 rounded-3xl border border-border bg-card">
                  <FileCheck className="w-10 h-10 text-bank-green mb-4" />
                  <h3 className="font-black text-lg mb-2 tracking-tight">
                    Compliance OJK
                  </h3>
                  <p className="text-muted-foreground">
                    Sesuai regulasi POJK tentang pinjaman digital dan perlindungan konsumen.
                  </p>
                </div>
              </div>
            </section>

            <section className="mb-16">
              <h2 className="font-black text-2xl tracking-tight mb-6">
                Alur Pinjaman
              </h2>
              <div className="space-y-4">
                <div className="flex items-start gap-4 p-4 rounded-2xl border border-border bg-card">
                  <div className="w-8 h-8 rounded-full bg-bank-green text-white flex items-center justify-center text-sm font-bold shrink-0">1</div>
                  <div>
                    <h3 className="font-bold mb-1">Eligibility Check</h3>
                    <p className="text-sm text-muted-foreground">POST /api/v1/lending/eligibility — Cek kelayakan pinjaman berdasarkan profil & credit score.</p>
                  </div>
                </div>
                <div className="flex items-start gap-4 p-4 rounded-2xl border border-border bg-card">
                  <div className="w-8 h-8 rounded-full bg-bank-green text-white flex items-center justify-center text-sm font-bold shrink-0">2</div>
                  <div>
                    <h3 className="font-bold mb-1">Loan Application</h3>
                    <p className="text-sm text-muted-foreground">POST /api/v1/lending/apply — Submit pengajuan pinjaman dengan dokumen pendukung.</p>
                  </div>
                </div>
                <div className="flex items-start gap-4 p-4 rounded-2xl border border-border bg-card">
                  <div className="w-8 h-8 rounded-full bg-bank-green text-white flex items-center justify-center text-sm font-bold shrink-0">3</div>
                  <div>
                    <h3 className="font-bold mb-1">Approval & Disbursement</h3>
                    <p className="text-sm text-muted-foreground">Approval otomatis/manual → pencairan dana ke wallet peminjam.</p>
                  </div>
                </div>
                <div className="flex items-start gap-4 p-4 rounded-2xl border border-border bg-card">
                  <div className="w-8 h-8 rounded-full bg-bank-green text-white flex items-center justify-center text-sm font-bold shrink-0">4</div>
                  <div>
                    <h3 className="font-bold mb-1">Repayment</h3>
                    <p className="text-sm text-muted-foreground">POST /api/v1/lending/repay — Pembayaran cicilan, early repayment, atau autopay.</p>
                  </div>
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
                    <span className="px-2 py-0.5 rounded-md bg-blue-100 text-blue-800 text-xs font-mono font-bold">POST</span>
                    <code className="text-sm font-mono">/api/v1/lending/eligibility</code>
                  </div>
                  <p className="text-sm text-muted-foreground ml-14">
                    Cek kelayakan pinjaman. Mengembalikan limit, tenor, dan suku bunga yang tersedia.
                  </p>
                </div>
                <div className="p-4 rounded-2xl border border-border bg-card">
                  <div className="flex items-center gap-3 mb-2">
                    <span className="px-2 py-0.5 rounded-md bg-blue-100 text-blue-800 text-xs font-mono font-bold">POST</span>
                    <code className="text-sm font-mono">/api/v1/lending/apply</code>
                  </div>
                  <p className="text-sm text-muted-foreground ml-14">
                    Submit pengajuan pinjaman baru. Memerlukan header <code>X-Idempotency-Key</code>.
                  </p>
                </div>
                <div className="p-4 rounded-2xl border border-border bg-card">
                  <div className="flex items-center gap-3 mb-2">
                    <span className="px-2 py-0.5 rounded-md bg-green-100 text-green-800 text-xs font-mono font-bold">GET</span>
                    <code className="text-sm font-mono">/api/v1/lending/loans</code>
                  </div>
                  <p className="text-sm text-muted-foreground ml-14">
                    Daftar semua pinjaman aktif dan riwayat pinjaman user.
                  </p>
                </div>
                <div className="p-4 rounded-2xl border border-border bg-card">
                  <div className="flex items-center gap-3 mb-2">
                    <span className="px-2 py-0.5 rounded-md bg-green-100 text-green-800 text-xs font-mono font-bold">GET</span>
                    <code className="text-sm font-mono">/api/v1/lending/loans/{'{loanId}'}/schedule</code>
                  </div>
                  <p className="text-sm text-muted-foreground ml-14">
                    Jadwal cicilan pinjaman lengkap dengan status pembayaran tiap periode.
                  </p>
                </div>
                <div className="p-4 rounded-2xl border border-border bg-card">
                  <div className="flex items-center gap-3 mb-2">
                    <span className="px-2 py-0.5 rounded-md bg-blue-100 text-blue-800 text-xs font-mono font-bold">POST</span>
                    <code className="text-sm font-mono">/api/v1/lending/repay</code>
                  </div>
                  <p className="text-sm text-muted-foreground ml-14">
                    Pembayaran cicilan. Mendukung partial payment, full installment, dan early repayment.
                  </p>
                </div>
                <div className="p-4 rounded-2xl border border-border bg-card">
                  <div className="flex items-center gap-3 mb-2">
                    <span className="px-2 py-0.5 rounded-md bg-blue-100 text-blue-800 text-xs font-mono font-bold">POST</span>
                    <code className="text-sm font-mono">/api/v1/lending/simulate</code>
                  </div>
                  <p className="text-sm text-muted-foreground ml-14">
                    Simulasi pinjaman — hitung cicilan, total bunga, dan jadwal pembayaran tanpa submit.
                  </p>
                </div>
              </div>
            </section>

            <section className="mb-16">
              <h2 className="font-black text-2xl tracking-tight mb-6">
                Contoh: Pengajuan Pinjaman
              </h2>
              <div className="rounded-2xl border border-border bg-zinc-950 p-6 overflow-x-auto">
                <pre className="text-sm text-zinc-300 font-mono">
{`POST /api/v1/lending/apply
Authorization: Bearer {access_token}
X-Idempotency-Key: loan-apply-uuid-001
Content-Type: application/json

{
  "productType": "PERSONAL_LOAN",
  "requestedAmount": 10000000,
  "currency": "IDR",
  "tenorMonths": 12,
  "purpose": "EDUCATION",
  "monthlyIncome": 15000000,
  "employmentType": "FULL_TIME"
}

// Response 201 Created
{
  "loanId": "loan-uuid-789",
  "status": "UNDER_REVIEW",
  "requestedAmount": 10000000,
  "approvedAmount": null,
  "interestRate": 12.5,
  "tenorMonths": 12,
  "estimatedMonthlyPayment": 891667,
  "decisionExpectedAt": "2025-02-09T12:00:00Z",
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
                  <code className="text-sm font-mono text-bank-green font-bold">lending.application.approved</code>
                  <p className="text-sm text-muted-foreground mt-1">
                    Pengajuan pinjaman disetujui — siap untuk disbursement.
                  </p>
                </div>
                <div className="p-4 rounded-2xl border border-border bg-card">
                  <code className="text-sm font-mono text-bank-green font-bold">lending.application.rejected</code>
                  <p className="text-sm text-muted-foreground mt-1">
                    Pengajuan ditolak beserta alasan penolakan.
                  </p>
                </div>
                <div className="p-4 rounded-2xl border border-border bg-card">
                  <code className="text-sm font-mono text-bank-green font-bold">lending.disbursement.completed</code>
                  <p className="text-sm text-muted-foreground mt-1">
                    Dana pinjaman telah dicairkan ke wallet peminjam.
                  </p>
                </div>
                <div className="p-4 rounded-2xl border border-border bg-card">
                  <code className="text-sm font-mono text-bank-green font-bold">lending.repayment.received</code>
                  <p className="text-sm text-muted-foreground mt-1">
                    Pembayaran cicilan diterima dan outstanding balance diperbarui.
                  </p>
                </div>
                <div className="p-4 rounded-2xl border border-border bg-card">
                  <code className="text-sm font-mono text-bank-green font-bold">lending.loan.overdue</code>
                  <p className="text-sm text-muted-foreground mt-1">
                    Cicilan melewati jatuh tempo — notifikasi dan denda berlaku.
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
                      <td className="p-4 font-mono">LND_001</td>
                      <td className="p-4">Tidak memenuhi syarat pinjaman</td>
                      <td className="p-4 text-muted-foreground">Cek eligibility terlebih dahulu</td>
                    </tr>
                    <tr>
                      <td className="p-4 font-mono">LND_002</td>
                      <td className="p-4">Sudah ada pinjaman aktif</td>
                      <td className="p-4 text-muted-foreground">Lunasi pinjaman aktif sebelum mengajukan baru</td>
                    </tr>
                    <tr>
                      <td className="p-4 font-mono">LND_003</td>
                      <td className="p-4">Dokumen KYC belum lengkap</td>
                      <td className="p-4 text-muted-foreground">Lengkapi verifikasi KYC</td>
                    </tr>
                    <tr>
                      <td className="p-4 font-mono">LND_004</td>
                      <td className="p-4">Amount di luar limit yang disetujui</td>
                      <td className="p-4 text-muted-foreground">Sesuaikan amount dengan approved limit</td>
                    </tr>
                    <tr>
                      <td className="p-4 font-mono">LND_005</td>
                      <td className="p-4">Tenor tidak tersedia untuk produk</td>
                      <td className="p-4 text-muted-foreground">Pilih tenor dari daftar yang tersedia di eligibility</td>
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
