'use client';

import React from 'react';
import { useLocale } from 'next-intl';
import { ArrowUpRight, ArrowDownRight } from 'lucide-react';
import VIPBadge from '@/components/personalization/VIPBadge';
import { cn } from '@/lib/utils';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { formatCurrency } from '@/lib/currency';

interface BalanceCardProps {
  balance: string | number;
  percentage?: number;
  income?: string | number;
  expense?: string | number;
  incomeChange?: number;
  expenseChange?: number;
  netWorth?: number;
  netWorthChange?: number;
  currency?: string;
  isLoading?: boolean;
}

export default function BalanceCard({
  balance,
  percentage,
  income,
  expense,
  incomeChange,
  expenseChange,
  netWorth,
  netWorthChange,
  currency = 'Rp',
  isLoading = false,
}: BalanceCardProps) {
  // BUG-FE-008 FIX: Use dynamic locale instead of hardcoded 'id-ID'
  const locale = useLocale();
  const bcp47Locale = locale === 'id' ? 'id-ID' : 'en-US';

  return (
    <div data-testid="balance-card" className="grid grid-cols-1 md:grid-cols-12 xl:grid-cols-3 gap-6">
      {/* Col 1: Primary Balance & Net Worth */}
      <div className="md:col-span-12 lg:col-span-4 xl:col-span-1 flex flex-col gap-6">
        <Card data-testid="primary-balance-card" className="flex flex-col justify-between flex-1 relative overflow-hidden group min-h-[220px]">
          <CardHeader className="flex flex-row items-start justify-between space-y-0 pb-6">
            <div>
              <CardTitle className="text-xs sm:text-sm font-bold text-emerald-500 tracking-[0.2em] uppercase">
                Saldo Utama
              </CardTitle>
              <CardDescription className="mt-2 text-xs sm:text-xs font-bold uppercase tracking-widest opacity-60">
                {new Date().toLocaleDateString(bcp47Locale, { day: 'numeric', month: 'short', year: 'numeric' })}
              </CardDescription>
            </div>
            <div className="flex items-center gap-2">
              <VIPBadge size="sm" variant="badge" />
              <div className="h-2 w-2 bg-primary rounded-full shadow-[0_0_8px_hsl(var(--primary))] animate-pulse" />
            </div>
          </CardHeader>

          <CardContent>
            <div className="space-y-4">
              <h2 className="text-3xl sm:text-4xl lg:text-5xl font-bold text-foreground tabular-nums leading-none tracking-tighter">
                {formatCurrency(balance, { symbol: currency, locale: bcp47Locale })}
              </h2>
              <div className="flex items-center gap-3">
                {percentage != null ? (
                  <span className="text-xs px-3 py-1.5 rounded-xl bg-emerald-500/10 text-emerald-500 font-bold flex items-center gap-1 uppercase tracking-tighter border border-emerald-500/10">
                    <ArrowUpRight className="h-4 w-4" />
                    +{percentage}%
                  </span>
                ) : (
                  <span className="text-xs px-3 py-1.5 rounded-xl bg-muted/50 text-muted-foreground font-bold uppercase tracking-tighter">
                    --
                  </span>
                )}
                <span className="text-xs sm:text-xs text-muted-foreground font-bold uppercase tracking-[0.15em] opacity-70">Faktor Pertumbuhan</span>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card data-testid="net-worth-card" className="flex flex-col justify-between flex-1 relative overflow-hidden group border border-border bg-card shadow-sm hover:shadow-md transition-shadow min-h-[220px]">
          <CardHeader className="flex flex-row items-start justify-between space-y-0 pb-8">
            <CardTitle className="text-xs sm:text-sm font-bold text-muted-foreground tracking-[0.2em] uppercase">
              Kekayaan Bersih
            </CardTitle>
            <div className="w-12 h-12 rounded-2xl bg-emerald-500/10 text-emerald-500 flex items-center justify-center transition-all group-hover:scale-110 shadow-sm border border-emerald-500/10">
              <ArrowUpRight className="h-6 w-6" />
            </div>
          </CardHeader>

          <CardContent>
            <div className="space-y-4">
              <h3 className="text-2xl sm:text-3xl lg:text-4xl font-bold text-foreground tabular-nums leading-none tracking-tight">
                {formatCurrency(netWorth ?? 0, { symbol: currency, locale: bcp47Locale })}
              </h3>
              <div className="flex items-center gap-2">
                {netWorthChange != null ? (
                  <span className="text-xs font-bold text-emerald-500 uppercase tracking-tighter flex items-center gap-1">
                    <ArrowUpRight className="h-4 w-4" />
                    +{netWorthChange}%
                  </span>
                ) : (
                  <span className="text-xs font-bold text-muted-foreground uppercase tracking-tighter">
                    --
                  </span>
                )}
                <span className="text-xs sm:text-xs text-muted-foreground font-bold uppercase tracking-[0.15em] opacity-70">Peningkatan Total</span>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Col 2: Visual Card Representation (Hero) */}
      <div className="md:col-span-12 lg:col-span-8 xl:col-span-1">
        <div className="relative aspect-[1.6/1] md:aspect-auto md:h-full min-h-[300px] xl:min-h-0 rounded-2xl overflow-hidden shadow-glass group border border-white/10">
          {/* Gradient background */}
          <div className="absolute inset-0 card-gradient" />

          {/* Glass overlay */}
          <div className="absolute inset-0 bg-white/5 backdrop-blur-md" />

          {/* Decorative circles */}
          <div className="absolute -top-10 -right-10 w-32 h-32 rounded-full bg-white/20 blur-xl transition-transform group-hover:scale-110 pointer-events-none" />
          <div className="absolute -bottom-10 -left-10 w-40 h-40 rounded-full bg-white/10 blur-2xl pointer-events-none" />

          <div className="relative z-10 p-7 sm:p-10 h-full flex flex-col justify-between text-white">
            <div className="flex justify-between items-start">
              <div className="flex items-center gap-3">
                <div className="h-12 w-12 bg-white/20 backdrop-blur-md rounded-2xl flex items-center justify-center border border-white/20 font-bold text-2xl shadow-lg">
                  U
                </div>
                <span className="text-3xl font-bold uppercase tracking-tighter">PayU</span>
              </div>
              <div className="text-xs font-bold tracking-widest opacity-80 bg-white/10 px-3 py-1.5 rounded-lg border border-white/10">07/28</div>
            </div>

            <div className="space-y-6">
              <div className="text-xl sm:text-3xl lg:text-5xl font-bold tracking-[0.3em] font-mono whitespace-nowrap overflow-hidden drop-shadow-2xl">
                4829 •••• •••• 1928
              </div>
              <div className="flex justify-between items-end">
                <div>
                  <p className="text-xs text-white/60 font-bold uppercase tracking-[0.2em] mb-2">Pemegang Kartu</p>
                  <p className="text-sm sm:text-base lg:text-lg font-bold uppercase tracking-widest">PENGGUNA PAYU</p>
                </div>
                <div className="flex -space-x-3">
                  <div className="w-12 h-12 rounded-full bg-orange-500/80 backdrop-blur-sm border border-white/20 shadow-lg" />
                  <div className="w-12 h-12 rounded-full bg-yellow-400/60 backdrop-blur-sm border border-white/20 shadow-lg" />
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Col 3: Summary Stats (Pemasukan/Pengeluaran) */}
      <div className="md:col-span-12 lg:col-span-12 xl:col-span-1 grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-1 gap-6">
        <SummaryItem
          data-testid="income-card"
          label="Pemasukan"
          amount={income ?? 0}
          change={incomeChange ?? 0}
          isPositive={true}
          currency={currency}
          bcp47Locale={bcp47Locale}
        />
        <SummaryItem
          data-testid="expense-card"
          label="Pengeluaran"
          amount={expense ?? 0}
          change={expenseChange ?? 0}
          isPositive={false}
          currency={currency}
          bcp47Locale={bcp47Locale}
        />
      </div>
    </div>
  );
}

interface SummaryItemProps {
  label: string;
  amount: string | number;
  change: number;
  isPositive: boolean;
  currency: string;
  bcp47Locale: string;
  'data-testid'?: string;
}

function SummaryItem({ label, amount, change, isPositive, currency, bcp47Locale, 'data-testid': testId }: SummaryItemProps) {
  return (
    <Card data-testid={testId} className="flex flex-col justify-between h-full relative overflow-hidden group border border-border bg-card shadow-sm hover:shadow-md transition-shadow">
      <CardHeader className="flex flex-row items-start justify-between space-y-0 pb-10">
        <CardTitle className="text-xs sm:text-sm font-bold text-muted-foreground tracking-[0.2em] uppercase">
          {label}
        </CardTitle>
        <div className={cn(
          "w-12 h-12 rounded-2xl flex items-center justify-center transition-all group-hover:scale-110 shadow-sm border border-white/5",
          isPositive ? "bg-emerald-500/10 text-emerald-500" : "bg-destructive/10 text-destructive"
        )}>
          {isPositive ? <ArrowUpRight className="h-6 w-6" /> : <ArrowDownRight className="h-6 w-6" />}
        </div>
      </CardHeader>

      <CardContent>
        <div className="space-y-4">
          <h4 className="text-2xl sm:text-3xl font-bold text-foreground tabular-nums tracking-tight">
            {formatCurrency(amount, { symbol: currency, locale: bcp47Locale })}
          </h4>
          <div className="flex items-center gap-2">
            <span className={cn(
              "text-xs font-bold px-2 py-0.5 rounded-lg",
              isPositive ? "bg-emerald-500/10 text-emerald-500" : "bg-destructive/10 text-destructive"
            )}>
              {isPositive ? '+' : ''}{change}%
            </span>
            <span className="text-xs sm:text-xs text-muted-foreground font-bold uppercase tracking-[0.15em] opacity-70">Bulan ini</span>
          </div>
        </div>
      </CardContent>
    </Card>
  );
}
