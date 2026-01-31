'use client';

import React from 'react';
import { ChevronDown, TrendingUp } from 'lucide-react';
import { cn } from '@/lib/utils';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';

function ChartLegend({ color, label, percentage }: { color: string; label: string; percentage: string }) {
  return (
    <div className="flex items-center gap-3">
      <div className={cn("w-2.5 h-2.5 rounded-full", color)} />
      <div>
        <p className="text-[10px] text-muted-foreground font-black tracking-widest uppercase">{label}</p>
        <p className="text-xs font-black text-foreground">{percentage}</p>
      </div>
    </div>
  );
}

interface StatsChartsProps {
  className?: string;
}

export default function StatsCharts({ className = '' }: StatsChartsProps) {
  return (
    <div className={cn("grid grid-cols-1 md:grid-cols-2 lg:grid-cols-12 gap-6 lg:gap-8", className)}>
      {/* Investment Performance (Donut Chart Pattern) */}
      <Card className="lg:col-span-5 relative overflow-hidden group">
        <CardHeader className="flex flex-row items-center justify-between pb-8">
          <CardTitle className="text-sm font-black text-foreground tracking-widest uppercase">
            Performa Investasi
          </CardTitle>
          <div className="flex items-center gap-1.5 text-[10px] font-black text-muted-foreground bg-muted/50 px-3 py-1.5 rounded-lg cursor-pointer hover:bg-muted transition-colors uppercase tracking-widest">
            Januari 2026 <ChevronDown className="h-3.5 w-3.5" />
          </div>
        </CardHeader>

        <CardContent>
          <div className="flex flex-col sm:flex-row items-center justify-between gap-6">
            <div className="space-y-4 w-full sm:w-auto">
              <ChartLegend color="bg-chart-1" label="Saham" percentage="60%" />
              <ChartLegend color="bg-chart-2" label="Obligasi" percentage="25%" />
              <ChartLegend color="bg-chart-3" label="Emas Digital" percentage="15%" />
            </div>

            <div className="relative h-28 w-28 sm:h-32 sm:w-32 flex items-center justify-center">
              <svg viewBox="0 0 36 36" className="w-full h-full transform -rotate-90">
                <circle cx="18" cy="18" r="15.9" fill="none" stroke="hsl(var(--muted))" strokeWidth="4" />
                <circle cx="18" cy="18" r="15.9" fill="none" stroke="hsl(var(--chart-green3))" strokeWidth="4" strokeDasharray="100 100" strokeLinecap="round" />
                <circle cx="18" cy="18" r="15.9" fill="none" stroke="hsl(var(--chart-green2))" strokeWidth="4" strokeDasharray="85 100" strokeLinecap="round" />
                <circle cx="18" cy="18" r="15.9" fill="none" stroke="hsl(var(--chart-green1))" strokeWidth="4" strokeDasharray="60 100" strokeLinecap="round" />
              </svg>
              <div className="absolute inset-0 flex flex-col items-center justify-center">
                <div className="w-6 h-6 sm:w-8 sm:h-8 rounded-lg bg-success-light flex items-center justify-center mb-1 shadow-sm">
                  <TrendingUp className="h-4 w-4 text-primary" />
                </div>
                <span className="text-[10px] sm:text-xs font-black text-foreground">+12.5%</span>
              </div>
            </div>

            <div className="text-right w-full sm:w-auto shrink-0">
              <p className="text-[10px] text-muted-foreground font-black tracking-widest mb-1 uppercase">Total Nilai</p>
              <h4 className="text-xl font-black text-foreground tabular-nums">Rp 8.750k</h4>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Spending Overview (Bar Chart Pattern) */}
      <Card className="lg:col-span-7 group overflow-hidden">
        <CardHeader className="flex flex-row items-center justify-between pb-8">
          <CardTitle className="text-sm font-black text-foreground tracking-widest uppercase">
            Ikhtisar Pengeluaran
          </CardTitle>
          <div className="flex items-center gap-1.5 text-[10px] font-black text-muted-foreground bg-muted/50 px-3 py-1.5 rounded-lg cursor-pointer hover:bg-muted transition-colors uppercase tracking-widest">
            Tahun 2026 <ChevronDown className="h-3.5 w-3.5" />
          </div>
        </CardHeader>

        <CardContent>
          <div className="flex items-end justify-between h-36 sm:h-40 gap-2 sm:gap-4 mt-4 px-2">
            {[
              { label: 'Jan', value: 30 },
              { label: 'Feb', value: 45 },
              { label: 'Mar', value: 60 },
              { label: 'Apr', value: 80 },
              { label: 'Mei', value: 100, active: true },
              { label: 'Jun', value: 70 },
              { label: 'Jul', value: 90 },
            ].map((bar, i) => (
              <div key={i} className="flex-1 flex flex-col items-center gap-3">
                <div className="w-full relative group/bar h-full flex items-end">
                  <div
                    className={cn(
                      "w-full rounded-t-2xl transition-all duration-700",
                      bar.active
                        ? "bg-primary shadow-[0_4px_20px_hsl(var(--primary)/0.4)]"
                        : "bg-muted/60 group-hover/bar:bg-primary/30"
                    )}
                    style={{ height: `${bar.value}%` }}
                  />
                  {bar.active && (
                    <div className="absolute -top-10 left-1/2 -translate-x-1/2 bg-foreground text-background text-[10px] font-black px-2 py-1.5 rounded-lg whitespace-nowrap z-10 shadow-xl animate-in fade-in slide-in-from-bottom-2 duration-500 uppercase tracking-tighter">
                      Rp 3.5jt
                    </div>
                  )}
                </div>
                <span className="text-[10px] font-black text-muted-foreground uppercase tracking-widest">{bar.label}</span>
              </div>
            ))}
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
