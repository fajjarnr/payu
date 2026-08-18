'use client';

import DashboardLayout from '@/components/DashboardLayout';
import { PageTransition, StaggerContainer, StaggerItem } from '@/components/ui/Motion';
import { useInvestmentAccount } from '@/hooks';
import { useTranslations } from 'next-intl';
import { formatCurrency } from '@/lib/currency';
import { toast } from 'sonner';

export default function InvestmentsPage() {
  const t = useTranslations('investments');
  const { data: account, isLoading: loadingAccount, isError: accountError } = useInvestmentAccount();

  const hasAccount = Boolean(account) && !accountError;

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
                <section data-testid="portfolio-overview-card" className="rounded-3xl border border-border bg-card p-8 shadow-sm">
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
                      onClick={() => toast.info('Katalog produk investasi: Reksadana Pasar Uang & Saham')}
                      data-testid="invest-buy-button"
                      className="rounded-xl bg-primary px-6 py-3 text-xs font-bold uppercase tracking-wider text-primary-foreground shadow-md hover:bg-primary/90 transition-all active:scale-95">
                      Beli Produk
                    </button>
                    <button
                      onClick={() => toast.info('Pilih produk investasi dari portofolio untuk dijual')}
                      data-testid="invest-sell-button"
                      className="rounded-xl border border-border bg-muted/40 px-6 py-3 text-xs font-bold uppercase tracking-wider text-foreground hover:bg-muted/60 transition-all active:scale-95">
                      Jual Produk
                    </button>
                  </div>
                </section>
              </StaggerItem>

              <StaggerItem>
                <section data-testid="investment-performance-empty" className="h-full rounded-3xl border border-border bg-card p-8 shadow-sm">
                  <h3 className="text-xl font-semibold text-foreground">{t('performance')}</h3>
                  <p className="mt-3 text-sm text-muted-foreground">{t('performanceUnavailable')}</p>
                </section>
              </StaggerItem>

              <StaggerItem>
                <section data-testid="investment-risk-empty" className="h-full rounded-3xl border border-border bg-card p-8 shadow-sm">
                  <h3 className="text-xl font-semibold text-foreground">{t('riskLevel')}</h3>
                  <p className="mt-3 text-sm text-muted-foreground">{t('riskUnavailable')}</p>
                </section>
              </StaggerItem>

              <StaggerItem className="lg:col-span-2">
                <section data-testid="investment-products-empty" className="h-full rounded-3xl border border-border bg-card p-8 shadow-sm">
                  <h3 className="text-xl font-semibold text-foreground">{t('portfolio')}</h3>
                  <p className="mt-3 text-sm text-muted-foreground">{t('productsUnavailable')}</p>
                </section>
              </StaggerItem>

              <StaggerItem className="lg:col-span-3">
                <section data-testid="investment-advice-empty" className="rounded-3xl border border-border bg-card p-8 shadow-sm">
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
