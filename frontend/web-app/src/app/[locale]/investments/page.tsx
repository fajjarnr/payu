'use client';

import React from 'react';
import DashboardLayout from "@/components/DashboardLayout";
import { TrendingUp, PieChart, Landmark, ArrowUpRight, ShieldCheck, Briefcase, Plus, Coins, BarChart3, Target, Loader2 } from 'lucide-react';
import clsx from 'clsx';
import { PageTransition, StaggerContainer, StaggerItem, ButtonMotion } from '@/components/ui/Motion';
import { Button } from '@/components/ui/button';
import { useInvestmentAccount, useGoldHoldings } from '@/hooks';
import { useAuthStore } from '@/stores/authStore';
import { useTranslations } from 'next-intl';

export default function InvestmentsPage() {
  const t = useTranslations('investments');
  const { user } = useAuthStore(); // eslint-disable-line @typescript-eslint/no-unused-vars
  const { data: account, isLoading: loadingAccount } = useInvestmentAccount();
  const { data: goldHoldings } = useGoldHoldings(); // eslint-disable-line @typescript-eslint/no-unused-vars

  const portfolioBalance = account?.balance ?? 0;
  const formatRp = (n: number) => new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR', minimumFractionDigits: 0 }).format(n);

  const investmentProducts = [
    { name: 'Suku Bunga Tetap Plus', type: t('risk.low'), return: '5.5% p.a', icon: Landmark, color: 'text-blue-500', bg: 'bg-blue-500/10' },
    { name: 'Equity Growth Fund', type: t('risk.high'), return: '18.2% p.a', icon: TrendingUp, color: 'text-primary', bg: 'bg-success-light' },
    { name: 'Emas Digital (XAU)', type: 'Stabil', return: 'Harga Pasar', icon: Coins, color: 'text-amber-500', bg: 'bg-amber-500/10' },
  ];

  return (
    <DashboardLayout>
      <PageTransition>
        <div className="space-y-12">
          {/* Header */}
          <StaggerContainer>
            <StaggerItem>
              <div className="flex flex-col sm:flex-row justify-between items-start sm:items-end gap-6 mb-8">
                <div>
                  <h2 className="text-3xl font-bold text-foreground">{t('title')}</h2>
                   <p className="text-sm text-muted-foreground font-medium mt-1">{t('subtitle')}</p>
                </div>
                <ButtonMotion>
                  <Button data-testid="new-investment-button" className="h-14 px-8 shadow-xl shadow-primary/20 flex items-center gap-2">
                    <Plus className="h-4 w-4" /> {t('newInvestment')}
                  </Button>
                </ButtonMotion>
              </div>
            </StaggerItem>

            {/* Portfolio Overview */}
            <div className="grid grid-cols-1 md:grid-cols-12 lg:grid-cols-12 gap-6">
              <StaggerItem className="md:col-span-6 lg:col-span-8">
                <div data-testid="portfolio-overview-card" className="bg-card rounded-xl p-8 sm:p-8 border border-border shadow-card h-full relative overflow-hidden group">
                  <div className="absolute top-0 right-0 w-80 h-80 bg-primary/5 rounded-full blur-3xl -z-0" />

                  <div className="relative z-10 flex flex-col h-full">
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-8 mb-10">
                      <div className="space-y-6">
                        <div>
                          <p className="text-xs font-bold text-muted-foreground tracking-widest uppercase mb-2">Total Portofolio Bersih</p>
                          <h3 className="text-4xl sm:text-4xl lg:text-5xl font-bold text-foreground">{loadingAccount ? '...' : formatRp(portfolioBalance)}</h3>
                        </div>
                        <div className="flex flex-wrap gap-3">
                          <div className="bg-success-light px-4 py-2 rounded-xl flex items-center gap-2 border border-primary/10">
                            <TrendingUp className="h-4 w-4 text-primary" />
                            <span className="text-xs font-bold text-primary tracking-widest">+Rp 12,4 Jt (8.2%)</span>
                          </div>
                          <div className="bg-muted/50 px-4 py-2 rounded-xl flex items-center gap-2 border border-border">
                            <ShieldCheck className="h-4 w-4 text-muted-foreground" />
                            <span className="text-xs font-bold text-muted-foreground tracking-widest uppercase">Terjamin LPS</span>
                          </div>
                        </div>
                      </div>

                      <div className="flex items-end justify-end gap-2 h-32">
                        {loadingAccount ? (
                          <div className="flex items-center justify-center w-full h-full">
                            <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
                          </div>
                        ) : (
                          <p className="text-xs text-muted-foreground font-bold tracking-widest uppercase">Belum ada data performa</p>
                        )}
                      </div>
                    </div>

                    <div className="mt-auto grid grid-cols-3 gap-6 pt-8 border-t border-border">
                      <div>
                        <p className="text-xs font-bold text-muted-foreground tracking-widest uppercase mb-1">Pasar Uang</p>
                        <p className="text-lg font-bold text-foreground">45%</p>
                      </div>
                      <div>
                        <p className="text-xs font-bold text-muted-foreground tracking-widest uppercase mb-1">Saham</p>
                        <p className="text-lg font-bold text-foreground">30%</p>
                      </div>
                      <div>
                        <p className="text-xs font-bold text-muted-foreground tracking-widest uppercase mb-1">Komoditas</p>
                        <p className="text-lg font-bold text-foreground">25%</p>
                      </div>
                    </div>
                  </div>
                </div>
              </StaggerItem>

              <StaggerItem className="md:col-span-6 lg:col-span-4">
                <div className="bg-gradient-to-br from-gray-900 to-gray-800 rounded-xl p-8 sm:p-8 text-white h-full relative overflow-hidden flex flex-col justify-between shadow-2xl group">
                  <div className="relative z-10">
                    <div className="flex justify-between items-start mb-6">
                      <h3 className="text-lg font-bold">Profil Risiko</h3>
                      <div className="h-8 w-8 bg-white/10 rounded-lg flex items-center justify-center border border-white/10">
                        <BarChart3 className="h-4 w-4 text-bank-green" />
                      </div>
                    </div>
                    <div className="flex items-center gap-5 mb-8">
                      <div className="h-14 w-14 rounded-xl bg-white/10 flex items-center justify-center border border-white/10 shadow-xl group-hover:rotate-6 transition-transform duration-500">
                        <Briefcase className="h-7 w-7 text-bank-green" />
                      </div>
                      <div>
                        <p className="text-xl font-bold">Moderat-Agresif</p>
                        <p className="text-xs text-gray-400 font-bold tracking-widest uppercase mt-0.5">ROI 15% / Thn</p>
                      </div>
                    </div>
                    <div className="space-y-2">
                      <div className="flex justify-between text-xs font-bold text-gray-400 tracking-widest uppercase">
                        <span>Konservatif</span>
                        <span>Agresif</span>
                      </div>
                      <div className="w-full bg-white/10 h-2 rounded-full overflow-hidden">
                        <div className="bg-bank-green h-full rounded-full" style={{ width: '75%' }} />
                      </div>
                    </div>
                  </div>
                  <Button data-testid="optimize-portfolio-button" variant="outline" className="relative z-10 w-full h-14 bg-white/5 border border-white/10 text-white hover:bg-white/10 mt-10">Optimasi Portofolio</Button>
                  <PieChart className="absolute bottom-[-40px] right-[-40px] h-48 w-48 text-white/5 -rotate-12 group-hover:rotate-0 transition-transform duration-1000" />
                </div>
              </StaggerItem>
            </div>

            {/* Marketplace Grid */}
            <div className="space-y-8 mt-12">
              <div className="flex justify-between items-center">
                <h3 className="text-xl font-bold text-foreground">{t('portfolio')}</h3>
                <div className="flex gap-2">
                  <Button size="sm" className="px-4">Semua</Button>
                  <Button size="sm" variant="ghost" className="px-4 border border-border">Pasar Uang</Button>
                  <Button size="sm" variant="ghost" className="px-4 border border-border">Emas</Button>
                </div>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                {investmentProducts.map((prod, i) => (
                  <div key={i} data-testid={`investment-product-${i}`} className="bg-card p-8 rounded-xl border border-border shadow-sm hover:shadow-card hover:-translate-y-1 transition-all group cursor-pointer active:scale-[0.98]">
                    <div className="flex justify-between items-start mb-10">
                      <div className={clsx(
                        "h-16 w-16 rounded-xl flex items-center justify-center shadow-lg transition-transform group-hover:scale-110",
                        prod.bg,
                        prod.color
                      )}>
                        <prod.icon className="h-8 w-8" />
                      </div>
                      <div className="h-8 w-8 bg-muted/30 rounded-lg flex items-center justify-center text-muted-foreground group-hover:bg-primary group-hover:text-primary-foreground transition-all">
                        <ArrowUpRight className="h-4 w-4" />
                      </div>
                    </div>
                    <div>
                      <p className="text-xs font-bold text-muted-foreground tracking-widest uppercase mb-2">{prod.type}</p>
                      <h4 className="text-xl font-bold text-foreground mb-6 leading-tight group-hover:text-primary transition-colors">{prod.name}</h4>
                      <div className="flex items-center justify-between p-4 bg-muted/30 rounded-xl border border-border group-hover:border-primary/20 transition-all">
                        <div className="space-y-0.5">
                          <p className="text-xs font-bold text-muted-foreground tracking-widest uppercase">Imbal Hasil</p>
                          <span className="text-lg font-bold text-primary">{prod.return}</span>
                        </div>
                        <ButtonMotion>
                          <Button data-testid={`buy-investment-${i}`} size="icon" className="h-10 w-10 shadow-lg shadow-primary/20">
                            <Plus className="h-4 w-4" />
                          </Button>
                        </ButtonMotion>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>

            {/* Smart Advice Section */}
            <div className="mt-12 bg-primary/5 rounded-xl border border-primary/10 p-8 sm:p-8 relative overflow-hidden group">
              <div className="relative z-10 flex flex-col md:flex-row items-center gap-8">
                <div className="h-20 w-20 bg-primary/20 rounded-2xl flex items-center justify-center shrink-0 border border-primary/20">
                  <Target className="h-10 w-10 text-primary animate-pulse" />
                </div>
                <div className="space-y-4">
                  <h3 className="text-2xl font-bold text-foreground">Target Portofolio Hampir Tercapai.</h3>
                  <p className="text-sm text-muted-foreground font-medium leading-relaxed max-w-2xl">
                    Berdasarkan performa saat ini, Anda diprediksi akan mencapai target <span className="text-primary font-bold">Dana Pensiun</span> dalam 14 bulan lebih cepat. Pertimbangkan untuk merealokasi 5% aset ke produk yang lebih stabil untuk mengunci keuntungan.
                  </p>
                </div>
                <div className="md:ml-auto">
                  <Button data-testid="review-strategy-button" className="h-14 px-8 shadow-xl shadow-primary/20">{t('performance')}</Button>
                </div>
              </div>
            </div>
          </StaggerContainer>
        </div>
      </PageTransition>
    </DashboardLayout>
  );
}
