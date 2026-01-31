'use client';

import React from 'react';
import { ArrowUpRight, ArrowDownRight } from 'lucide-react';
import { VIPBadge } from '@/components/personalization';
import { cn } from '@/lib/utils';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';

interface BalanceCardProps {
  balance: number;
  percentage?: number;
  currency?: string;
}

export default function BalanceCard({ balance, percentage = 45.2, currency = 'Rp' }: BalanceCardProps) {
  return (
    <div className="grid grid-cols-1 md:grid-cols-12 gap-6">
      {/* Main Balance Info */}
      <div className="md:col-span-4 flex flex-col gap-6">
        <Card className="flex flex-col justify-between flex-1 relative overflow-hidden group">
          <CardHeader className="flex flex-row items-start justify-between space-y-0 pb-6">
            <div>
              <CardTitle className="text-sm font-black text-muted-foreground tracking-widest uppercase">
                Saldo Utama
              </CardTitle>
              <CardDescription className="mt-1">
                {new Date().toLocaleDateString('id-ID', { day: 'numeric', month: 'short', year: 'numeric' })}
              </CardDescription>
            </div>
            <div className="flex items-center gap-2">
              <VIPBadge size="sm" variant="badge" />
              <div className="h-2 w-2 bg-primary rounded-full shadow-[0_0_8px_hsl(var(--primary))] animate-pulse" />
            </div>
          </CardHeader>

          <CardContent>
            <div className="space-y-3">
              <h2 className="text-3xl font-black text-foreground tabular-nums leading-none">
                {currency} {balance.toLocaleString('id-ID')}
              </h2>
              <div className="flex items-center gap-2">
                <span className="text-xs px-2.5 py-1 rounded-lg bg-success-light text-primary font-black flex items-center gap-0.5 uppercase tracking-tighter">
                  <ArrowUpRight className="h-3.5 w-3.5" />
                  +{percentage}%
                </span>
                <span className="text-[10px] text-muted-foreground font-bold uppercase tracking-widest">Faktor Pertumbuhan</span>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card className="flex flex-col justify-between flex-1 bg-muted/50 border-white/5 relative overflow-hidden group">
          <CardHeader className="flex flex-row items-start justify-between space-y-0 pb-6">
            <CardTitle className="text-sm font-black text-muted-foreground tracking-widest uppercase">
              Kekayaan Bersih
            </CardTitle>
            <div className="w-10 h-10 rounded-2xl bg-primary/10 text-primary flex items-center justify-center transition-all group-hover:scale-110 shadow-sm border border-primary/10">
              <ArrowUpRight className="h-5 w-5" />
            </div>
          </CardHeader>

          <CardContent>
            <div className="space-y-3">
              <h3 className="text-2xl font-black text-foreground tabular-nums leading-none">
                {currency} {(balance * 1.5).toLocaleString('id-ID')}
              </h3>
              <div className="flex items-center gap-1.5">
                <span className="text-xs font-black text-primary uppercase tracking-tighter">
                  +18% 
                </span>
                <span className="text-[10px] text-muted-foreground font-bold uppercase tracking-widest">Peningkatan Total</span>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Visual Card Representation (Glassmorphism) */}
      <div className="md:col-span-5">
        <div className="relative aspect-[1.6/1] rounded-2xl overflow-hidden shadow-glass group border border-white/10">
          {/* Gradient background */}
          <div className="absolute inset-0 card-gradient" />

          {/* Glass overlay */}
          <div className="absolute inset-0 bg-white/5 backdrop-blur-md" />

          {/* Decorative circles as per design system */}
          <div className="absolute -top-10 -right-10 w-32 h-32 rounded-full bg-white/20 blur-xl transition-transform group-hover:scale-110 pointer-events-none" />
          <div className="absolute -bottom-10 -left-10 w-40 h-40 rounded-full bg-white/10 blur-2xl pointer-events-none" />

          <div className="relative z-10 p-7 h-full flex flex-col justify-between text-white">
            <div className="flex justify-between items-start">
              <div className="flex items-center gap-3">
                <div className="h-12 w-12 bg-white/20 backdrop-blur-md rounded-2xl flex items-center justify-center border border-white/20 font-black text-2xl shadow-lg">
                  U
                </div>
                <span className="text-3xl font-black uppercase tracking-tighter">PayU</span>
              </div>
              <div className="text-[10px] font-black tracking-widest opacity-80 bg-white/10 px-3 py-1.5 rounded-lg border border-white/10">07/28</div>
            </div>

            <div className="space-y-5">
              <div className="text-xl sm:text-2xl font-black tracking-[0.25em] font-mono whitespace-nowrap overflow-hidden drop-shadow-md">
                4829 •••• •••• 1928
              </div>
              <div className="flex justify-between items-end">
                <div>
                  <p className="text-[10px] text-white/60 font-black uppercase tracking-[0.2em] mb-1">Pemegang Kartu</p>
                  <p className="text-sm font-black uppercase tracking-widest">PENGGUNA PAYU</p>
                </div>
                <div className="flex -space-x-3">
                  <div className="w-10 h-10 rounded-full bg-orange-500/80 backdrop-blur-sm border border-white/20" />
                  <div className="w-10 h-10 rounded-full bg-yellow-400/60 backdrop-blur-sm border border-white/20" />
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Summary Stats */}
      <div className="md:col-span-3 flex flex-col gap-6">
        <SummaryItem
          label="Pemasukan"
          amount={75200000}
          change={+6.5}
          isPositive={true}
          currency={currency}
        />
        <SummaryItem
          label="Pengeluaran"
          amount={42750000}
          change={-4.2}
          isPositive={false}
          currency={currency}
        />
      </div>
    </div>
  );
}

function SummaryItem({ label, amount, change, isPositive, currency }: { label: string; amount: number; change: number; isPositive: boolean; currency: string }) {
  return (
    <Card className="flex flex-col justify-between h-full relative overflow-hidden group">
      <CardHeader className="flex flex-row items-start justify-between space-y-0 pb-6">
        <CardTitle className="text-sm font-black text-muted-foreground tracking-widest uppercase">
          {label}
        </CardTitle>
        <div className={cn(
          "w-10 h-10 rounded-2xl flex items-center justify-center transition-all group-hover:scale-110 shadow-sm",
          isPositive ? "bg-success-light text-primary" : "bg-destructive/10 text-destructive"
        )}>
          {isPositive ? <ArrowUpRight className="h-5 w-5" /> : <ArrowDownRight className="h-5 w-5" />}
        </div>
      </CardHeader>

      <CardContent>
        <div className="space-y-2">
          <h4 className="text-2xl font-black text-foreground tabular-nums">
            {currency} {amount.toLocaleString('id-ID')}
          </h4>
          <div className="flex items-center gap-1.5">
            <span className={cn(
              "text-xs font-black",
              isPositive ? "text-primary" : "text-destructive"
            )}>
              {isPositive ? '+' : ''}{change}%
            </span>
            <span className="text-[10px] text-muted-foreground font-bold uppercase tracking-widest">Bulan ini</span>
          </div>
        </div>
      </CardContent>
    </Card>
  );
}
