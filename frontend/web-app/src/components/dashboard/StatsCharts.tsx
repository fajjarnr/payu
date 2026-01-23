'use client';

import React from 'react';
import { ChevronDown, TrendingUp } from 'lucide-react';
import clsx from 'clsx';

function ChartLegend({ color, label, percentage }: { color: string; label: string; percentage: string }) {
  return (
    <div className="flex items-center gap-3">
      <div className={clsx("w-2.5 h-2.5 rounded-full", color)} />
      <div>
        <p className="text-[10px] text-muted-foreground font-semibold tracking-wider">{label}</p>
        <p className="text-xs font-bold text-foreground">{percentage}</p>
      </div>
    </div>
  );
}

export default function StatsCharts() {
  return (
    <div className="grid grid-cols-1 md:grid-cols-12 gap-6 mt-12">
      {/* Investment Performance (Donut Chart Pattern) */}
      <div className="md:col-span-5 bg-card p-8 rounded-xl border border-border shadow-card relative overflow-hidden group">
        <div className="flex justify-between items-center mb-10">
          <h3 className="text-xs font-semibold text-muted-foreground tracking-widest">Performa Investasi</h3>
          <div className="flex items-center gap-1.5 text-[10px] font-bold text-muted-foreground bg-muted/50 px-3 py-1.5 rounded-lg cursor-pointer hover:bg-muted transition-colors">
            Januari 2026 <ChevronDown className="h-3.5 w-3.5" />
          </div>
        </div>

        <div className="flex flex-col sm:flex-row items-center justify-between gap-8 h-full">
          <div className="space-y-4 w-full sm:w-auto">
            <ChartLegend color="bg-chart-1" label="Saham" percentage="60%" />
            <ChartLegend color="bg-chart-2" label="Obligasi" percentage="25%" />
            <ChartLegend color="bg-chart-3" label="Emas Digital" percentage="15%" />
          </div>

          <div className="relative h-32 w-32 flex items-center justify-center">
            <svg viewBox="0 0 36 36" className="w-full h-full transform -rotate-90">
              <circle cx="18" cy="18" r="15.9" fill="none" stroke="hsl(var(--muted))" strokeWidth="4" />
              <circle cx="18" cy="18" r="15.9" fill="none" stroke="hsl(var(--chart-green3))" strokeWidth="4" strokeDasharray="100 100" strokeLinecap="round" />
              <circle cx="18" cy="18" r="15.9" fill="none" stroke="hsl(var(--chart-green2))" strokeWidth="4" strokeDasharray="85 100" strokeLinecap="round" />
              <circle cx="18" cy="18" r="15.9" fill="none" stroke="hsl(var(--chart-green1))" strokeWidth="4" strokeDasharray="60 100" strokeLinecap="round" />
            </svg>
            <div className="absolute inset-0 flex flex-col items-center justify-center">
              <div className="w-8 h-8 rounded-lg bg-success-light flex items-center justify-center mb-1 shadow-sm">
                <TrendingUp className="h-4 w-4 text-primary" />
              </div>
              <span className="text-[10px] font-bold text-foreground">+12.5%</span>
            </div>
          </div>

          <div className="text-right w-full sm:w-auto">
            <p className="text-[10px] text-muted-foreground font-semibold tracking-wider mb-1">Total Nilai</p>
            <h4 className="text-xl font-bold text-foreground ">Rp 8.750.000</h4>
          </div>
        </div>
      </div>

      {/* Spending Overview (Bar Chart Pattern) */}
      <div className="md:col-span-7 bg-card p-8 rounded-xl border border-border shadow-card group">
        <div className="flex justify-between items-center mb-10">
          <h3 className="text-xs font-semibold text-muted-foreground tracking-widest">Ikhtisar Pengeluaran</h3>
          <div className="flex items-center gap-1.5 text-[10px] font-bold text-muted-foreground bg-muted/50 px-3 py-1.5 rounded-lg cursor-pointer hover:bg-muted transition-colors">
            Tahun 2026 <ChevronDown className="h-3.5 w-3.5" />
          </div>
        </div>

        <div className="flex items-end justify-between h-40 gap-3 mt-4 px-2">
          {[
            { label: 'Jan', value: 30 },
            { label: 'Feb', value: 45 },
            { label: 'Mar', value: 60 },
            { label: 'Apr', value: 80 },
            { label: 'Mei', value: 100, active: true },
            { label: 'Jun', value: 70 },
            { label: 'Jul', value: 90 },
          ].map((bar, i) => (
            <div key={i} className="flex-1 flex flex-col items-center gap-4">
              <div className="w-full relative group/bar">
                <div
                  className={clsx(
                    "w-full rounded-full transition-all duration-500",
                    bar.active
                      ? "bg-primary shadow-[0_0_15px_hsl(var(--primary)/0.3)]"
                      : "bg-muted group-hover/bar:bg-primary/20"
                  )}
                  style={{ height: `${bar.value}%` }}
                />
                {bar.active && (
                  <div className="absolute -top-10 left-1/2 -translate-x-1/2 bg-foreground text-background text-[10px] font-bold px-2 py-1.5 rounded-lg whitespace-nowrap z-10 shadow-xl animate-bounce">
                    Rp 3.5jt
                  </div>
                )}
              </div>
              <span className="text-[10px] font-bold text-muted-foreground ">{bar.label}</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
