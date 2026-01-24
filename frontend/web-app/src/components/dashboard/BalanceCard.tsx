'use client';

import React from 'react';
import { ArrowUpRight, ArrowDownRight, RefreshCw } from 'lucide-react';
import clsx from 'clsx';
import { VIPBadge } from '@/components/personalization';

interface BalanceCardProps {
  balance: number;
  percentage?: number;
  currency?: string;
}

export default function BalanceCard({ balance, percentage = 45.2, currency = 'Rp' }: BalanceCardProps) {
  return (
    <div className="grid grid-cols-1 md:grid-cols-12 gap-6">
      {/* Main Balance Info */}
      <div className="md:col-span-4 space-y-6">
        <div className="bg-card p-6 rounded-xl shadow-card border border-border">
          <div className="flex justify-between items-start mb-4">
            <p className="text-xs font-semibold text-muted-foreground tracking-wider">Saldo Utama</p>
            <div className="flex items-center gap-2">
              <VIPBadge size="sm" variant="badge" />
              <div className="h-2 w-2 bg-primary rounded-full shadow-[0_0_8px_hsl(var(--primary))] animate-pulse" />
            </div>
          </div>

          <div className="space-y-1 mb-4">
            <h2 className="text-3xl font-bold text-foreground">
              {currency} {balance.toLocaleString('id-ID')}
            </h2>
            <div className="flex items-center gap-2">
              <span className="text-xs px-2 py-0.5 rounded-full bg-success-light text-primary font-medium flex items-center gap-0.5">
                <ArrowUpRight className="h-3 w-3" />
                +{percentage}%
              </span>
              <span className="text-xs text-muted-foreground">Faktor Pertumbuhan</span>
            </div>
          </div>

          <p className="text-[10px] text-muted-foreground font-medium tracking-widest">{new Date().toLocaleDateString('id-ID', { day: 'numeric', month: 'short', year: 'numeric' })}</p>
        </div>

        <div className="bg-muted/50 p-5 rounded-xl flex items-center justify-between">
          <div>
            <p className="text-xs text-muted-foreground font-medium tracking-wider mb-1">Kekayaan Bersih</p>
            <h3 className="text-xl font-bold text-foreground">
              {currency} {(balance * 1.5).toLocaleString('id-ID')}
            </h3>
          </div>
          <span className="text-xs px-2 py-0.5 rounded-full bg-primary text-primary-foreground font-medium">
            +18%
          </span>
        </div>
      </div>

      {/* Visual Card Representation (Glassmorphism) */}
      <div className="md:col-span-5">
        <div className="relative aspect-[1.6/1] rounded-xl overflow-hidden shadow-glass group">
          {/* Gradient background */}
          <div className="absolute inset-0 card-gradient" />

          {/* Glass overlay */}
          <div className="absolute inset-0 bg-white/5 backdrop-blur-md" />

          {/* Decorative circles as per design system */}
          <div className="absolute -top-10 -right-10 w-32 h-32 rounded-full bg-white/20 blur-xl transition-transform group-hover:scale-110" />
          <div className="absolute -bottom-10 -left-10 w-40 h-40 rounded-full bg-white/10 blur-2xl" />

          <div className="relative z-10 p-7 h-full flex flex-col justify-between text-white">
            <div className="flex justify-between items-start">
              <div className="flex items-center gap-2">
                <div className="h-10 w-10 bg-white/20 backdrop-blur-md rounded-xl flex items-center justify-center border border-white/20 font-black text-xl">
                  U
                </div>
                <span className="text-2xl font-black ">PayU</span>
              </div>
              <div className="text-xs font-mono opacity-80 bg-white/10 px-2 py-1 rounded-md">07/28</div>
            </div>

            <div className="space-y-4">
              <div className="text-xl sm:text-2xl font-bold tracking-[0.2em] font-mono whitespace-nowrap overflow-hidden">
                4829 •••• •••• 1928
              </div>
              <div className="flex justify-between items-end">
                <div>
                  <p className="text-[10px] text-white/60 font-medium tracking-widest">Pemegang Kartu</p>
                  <p className="text-sm font-bold">PENGGUNA PAYU</p>
                </div>
                <div className="flex -space-x-3">
                  <div className="w-8 h-8 rounded-full bg-orange-500/80 backdrop-blur-sm" />
                  <div className="w-8 h-8 rounded-full bg-yellow-400/60 backdrop-blur-sm" />
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
    <div className="bg-card p-6 rounded-xl shadow-card border border-border flex flex-col justify-between flex-1 relative overflow-hidden group">
      <div className="flex justify-between items-start mb-2">
        <p className="text-xs font-semibold text-muted-foreground tracking-wider">{label}</p>
        <div className={clsx(
          "w-8 h-8 rounded-full flex items-center justify-center transition-transform group-hover:scale-110",
          isPositive ? "bg-success-light" : "bg-destructive/10"
        )}>
          {isPositive ? <ArrowUpRight className="h-4 w-4 text-primary" /> : <ArrowDownRight className="h-4 w-4 text-destructive" />}
        </div>
      </div>

      <div className="space-y-1">
        <h4 className="text-2xl font-bold text-foreground">
          {currency} {amount.toLocaleString('id-ID')}
        </h4>
        <div className="flex items-center gap-1.5">
          <span className={clsx(
            "text-xs font-bold",
            isPositive ? "text-primary" : "text-destructive"
          )}>
            {isPositive ? '+' : ''}{change}%
          </span>
          <span className="text-xs text-muted-foreground">Bulan ini</span>
        </div>
      </div>
    </div>
  );
}
