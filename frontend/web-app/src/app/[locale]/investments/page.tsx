'use client';

import DashboardLayout from '@/components/DashboardLayout';
import { PageTransition, StaggerContainer, StaggerItem } from '@/components/ui/Motion';
import { useInvestmentAccount, useBuyDeposit, useSellInvestment, useCreateInvestmentAccount } from '@/hooks';
import { useTranslations } from 'next-intl';
import { asMoney, formatCurrency } from '@/lib/currency';
import { toast } from 'sonner';

export default function InvestmentsPage() {
  const t = useTranslations('investments');
  const { data: account, isLoading: loadingAccount, isError: accountError } = useInvestmentAccount();
  const buyDeposit = useBuyDeposit();
  const sellInvestment = useSellInvestment();
  const createAccount = useCreateInvestmentAccount();

  const hasAccount = Boolean(account) && !accountError;

  // ponytail: minimal wiring for I1-I5 — buy/sell via real mutations, no extra modal abstraction
  const handleBuy = async () => {
    try {
      let acc = account;
      if (!acc) {
        acc = await createAccount.mutateAsync();
      }
      await buyDeposit.mutateAsync({ accountId: acc.id, amount: asMoney('1000000'), tenure: 12 });
      toast.success('Pembelian deposit berhasil');
    } catch (e: unknown) {
      toast.error(e instanceof Error ? e.message : 'Gagal membeli produk');
    }
  };

  const handleSell = async () => {
    try {
      if (!account) {
        toast.error('Belum ada akun investasi');
        return;
      }
      await sellInvestment.mutateAsync({ accountId: account.id, transactionId: account.id, amount: asMoney('500000') });
      toast.success('Penjualan berhasil');
    } catch (e: unknown) {
      toast.error(e instanceof Error ? e.message : 'Gagal menjual produk');
    }
  };

  return (
    <DashboardLayout>
      <PageTransition>
        <div className="space-y-12">
          <StaggerContainer>
            <StaggerItem>
              <div className="mb-8">
                <h2 className="text-3xl font-bold tracking-tight text-foreground">{t('title')}</h2>
                <p className="mt-2 text-muted-foreground">{t('subtitle')}</p>
              </div>
            </StaggerItem>

            <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
              <StaggerItem className="lg:col-span-2">
                <section data-testid="portfolio-overview-card" className="rounded-3xl border border-border bg-card p-5 sm:p-6 lg:p-8 shadow-sm">
                  <p className="text-sm font-medium text-muted-foreground">{t('accountBalance')}</p>
                  <h3 className="mt-3 text-4xl font-bold tracking-tight text-foreground">
                    {loadingAccount
                      ? '...'
                      : account && !accountError
                        ? formatCurrency(account.balance, { withDecimals: false })
                        : t('accountUnavailable')}
                  </h3>
                  {hasAccount && <p className="mt-3 text-sm text-muted-foreground">{t('accountSource')}</p>}
                  {!loadingAccount && !hasAccount && (
                    <p data-testid="investment-account-empty" className="mt-3 text-sm text-muted-foreground">
                      {t('accountUnavailable')}
                    </p>
                  )}
                  <div className="mt-6 flex flex-wrap gap-4">
                    <button
                      onClick={handleBuy}
                      disabled={buyDeposit.isPending || createAccount.isPending}
                      data-testid="invest-buy-button"
                      className="rounded-xl bg-primary px-6 py-3 text-xs font-bold uppercase tracking-wider text-primary-foreground shadow-md hover:bg-primary/90 transition-all active:scale-95 disabled:opacity-50">
                      {buyDeposit.isPending ? 'Memproses...' : 'Beli Produk'}
                    </button>
                    <button
                      onClick={handleSell}
                      disabled={sellInvestment.isPending}
                      data-testid="invest-sell-button"
                      className="rounded-xl border border-border bg-muted/40 px-6 py-3 text-xs font-bold uppercase tracking-wider text-foreground hover:bg-muted/60 transition-all active:scale-95 disabled:opacity-50">
                      {sellInvestment.isPending ? 'Memproses...' : 'Jual Produk'}
                    </button>
                  </div>
                </section>
              </StaggerItem>

              <StaggerItem>
                <section data-testid="investment-performance-empty" className="h-full rounded-3xl border border-border bg-card p-5 sm:p-6 lg:p-8 shadow-sm">
                  <h3 className="text-xl font-semibold text-foreground">{t('performance')}</h3>
                  <p className="mt-3 text-sm text-muted-foreground">{t('performanceUnavailable')}</p>
                </section>
              </StaggerItem>

              <StaggerItem>
                <section data-testid="investment-risk-empty" className="h-full rounded-3xl border border-border bg-card p-5 sm:p-6 lg:p-8 shadow-sm">
                  <h3 className="text-xl font-semibold text-foreground">{t('riskLevel')}</h3>
                  <p className="mt-3 text-sm text-muted-foreground">{t('riskUnavailable')}</p>
                </section>
              </StaggerItem>

              <StaggerItem className="lg:col-span-2">
                <section data-testid="investment-products-empty" className="h-full rounded-3xl border border-border bg-card p-5 sm:p-6 lg:p-8 shadow-sm">
                  <h3 className="text-xl font-semibold text-foreground">{t('portfolio')}</h3>
                  <p className="mt-3 text-sm text-muted-foreground">{t('productsUnavailable')}</p>
                </section>
              </StaggerItem>

              <StaggerItem className="lg:col-span-3">
                <section data-testid="investment-advice-empty" className="rounded-3xl border border-border bg-card p-5 sm:p-6 lg:p-8 shadow-sm">
                  <h3 className="text-xl font-semibold text-foreground">{t('advice')}</h3>
                  <p className="mt-3 text-sm text-muted-foreground">{t('adviceUnavailable')}</p>
                </section>
              </StaggerItem>
            </div>
          </StaggerContainer>
        </div>
      </PageTransition>
    </DashboardLayout>
  );
}
