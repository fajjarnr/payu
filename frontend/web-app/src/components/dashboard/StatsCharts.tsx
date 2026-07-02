'use client';

import React from 'react';
import { ChevronDown } from 'lucide-react';
import { 
  Bar, 
  BarChart, 
  CartesianGrid, 
  Label, 
  PolarGrid, 
  PolarRadiusAxis, 
  RadialBar, 
  RadialBarChart, 
  XAxis,
  YAxis,
  ResponsiveContainer
} from "recharts"
import { cn } from '@/lib/utils';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { ChartConfig, ChartContainer, ChartTooltip, ChartTooltipContent } from "@/components/ui/chart"

function ChartLegend({ color, label, percentage }: { color: string; label: string; percentage: string }) {
  return (
    <div className="flex items-center gap-3">
      <div className={cn("w-2.5 h-2.5 rounded-full", color)} />
      <div>
        <p className="text-xs text-muted-foreground font-bold tracking-widest uppercase">{label}</p>
        <p className="text-xs font-bold text-foreground">{percentage}</p>
      </div>
    </div>
  );
}

const investmentConfig = {
  value: {
    label: "Return",
  },
  return: {
    label: "ROI",
    color: "hsl(var(--primary))",
  },
} satisfies ChartConfig

interface StatsChartsProps {
  className?: string;
  investmentChartData?: { category: string; value: number; fill: string }[];
  spendingChartData?: { month: string; amount: number }[];
  investmentLegend?: { color: string; label: string; percentage: string }[];
  totalValue?: string;
  isLoading?: boolean;
}

export default function StatsCharts({
  className = '',
  investmentChartData,
  spendingChartData,
  investmentLegend,
  totalValue,
  isLoading = false,
}: StatsChartsProps) {
  const invData = investmentChartData ?? [];
  const spdData = spendingChartData ?? [];
  const legend = investmentLegend ?? [
    { color: 'bg-emerald-500', label: 'Saham', percentage: '--' },
    { color: 'bg-emerald-400', label: 'Obligasi', percentage: '--' },
    { color: 'bg-emerald-300', label: 'Emas Digital', percentage: '--' },
  ];
  const displayTotal = totalValue ?? '--';

  if (isLoading) {
    return (
      <div className={cn("grid grid-cols-1 md:grid-cols-2 lg:grid-cols-12 gap-6 lg:gap-8", className)}>
        <Card className="lg:col-span-5 flex items-center justify-center min-h-[300px]">
          <p className="text-sm text-muted-foreground font-bold uppercase tracking-widest">Memuat...</p>
        </Card>
        <Card className="lg:col-span-7 flex items-center justify-center min-h-[300px]">
          <p className="text-sm text-muted-foreground font-bold uppercase tracking-widest">Memuat...</p>
        </Card>
      </div>
    );
  }
  return (
    <div className={cn("grid grid-cols-1 md:grid-cols-2 lg:grid-cols-12 gap-6 lg:gap-8", className)}>
      <Card className="lg:col-span-5 relative overflow-hidden group">
        <CardHeader className="flex flex-row items-center justify-between pb-6">
          <CardTitle className="text-base sm:text-lg font-bold text-foreground tracking-widest uppercase">
            Performa Investasi
          </CardTitle>
          <div className="flex items-center gap-2 text-xs font-bold text-muted-foreground bg-muted/60 px-5 py-2.5 rounded-xl cursor-pointer hover:bg-muted transition-colors uppercase tracking-widest shadow-sm">
            Januari 2026 <ChevronDown className="h-4 w-4" />
          </div>
        </CardHeader>

        <CardContent className="flex flex-col sm:flex-row items-center justify-between gap-10">
          <div className="space-y-6 w-full sm:w-auto">
            {legend.map((item) => (
              <ChartLegend key={item.label} color={item.color} label={item.label} percentage={item.percentage} />
            ))}
          </div>

          <div className="relative h-64 w-64 flex-shrink-0">
            <ChartContainer
              config={investmentConfig}
              className="mx-auto aspect-square h-full w-full"
            >
              <RadialBarChart
                data={invData}
                startAngle={0}
                endAngle={250}
                innerRadius={90}
                outerRadius={120}
              >
                <PolarGrid
                  gridType="circle"
                  radialLines={false}
                  stroke="none"
                  className="first:fill-muted/20 last:fill-background"
                  polarRadius={[95, 85]}
                />
                <RadialBar 
                  dataKey="value" 
                  background 
                  cornerRadius={12}
                />
                <PolarRadiusAxis tick={false} tickLine={false} axisLine={false}>
                  <Label
                    content={({ viewBox }) => {
                      if (viewBox && "cx" in viewBox && "cy" in viewBox) {
                        return (
                          <text
                            x={viewBox.cx}
                            y={viewBox.cy}
                            textAnchor="middle"
                            dominantBaseline="middle"
                          >
                            <tspan
                              x={viewBox.cx}
                               y={viewBox.cy}
                              className="fill-foreground text-4xl font-bold tabular-nums tracking-tighter"
                            >
                             +{invData[0]?.value ?? 0}%
                            </tspan>
                            <tspan
                              x={viewBox.cx}
                              y={(viewBox.cy || 0) + 30}
                              className="fill-muted-foreground text-xs font-bold uppercase tracking-[0.2em] opacity-60"
                            >
                              Yield
                            </tspan>
                          </text>
                        )
                      }
                    }}
                  />
                </PolarRadiusAxis>
              </RadialBarChart>
            </ChartContainer>
          </div>

          <div className="text-right w-full sm:w-auto shrink-0 space-y-2">
            <p className="text-xs sm:text-xs text-muted-foreground font-bold tracking-widest uppercase opacity-60">Total Nilai</p>
            <h4 className="text-2xl sm:text-3xl font-bold text-foreground tabular-nums tracking-tight">{displayTotal}</h4>
          </div>
        </CardContent>
      </Card>

      {/* Spending Overview (Official Shadcn Bar Chart) */}
      <Card className="lg:col-span-7 group overflow-hidden">
        <CardHeader className="flex flex-row items-center justify-between pb-8">
          <CardTitle className="text-base sm:text-lg font-bold text-foreground tracking-widest uppercase">
            Ikhtisar Pengeluaran
          </CardTitle>
          <div className="flex items-center gap-2 text-xs font-bold text-muted-foreground bg-muted/60 px-5 py-2.5 rounded-xl cursor-pointer hover:bg-muted transition-colors uppercase tracking-widest shadow-sm">
            Tahun 2026 <ChevronDown className="h-4 w-4" />
          </div>
        </CardHeader>

        <CardContent>
          <ChartContainer config={spendingConfig} className="h-80 w-full mt-4">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={spdData} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                <CartesianGrid vertical={false} strokeDasharray="3 3" stroke="hsl(var(--muted)/0.3)" />
                <XAxis
                  dataKey="month"
                  axisLine={false}
                  tickLine={false}
                  tick={{ fill: 'hsl(var(--muted-foreground))', fontSize: 10, fontWeight: 900 }}
                  dy={10}
                />
                <YAxis
                  hide
                />
                <ChartTooltip
                  cursor={false}
                  content={<ChartTooltipContent hideLabel />}
                />
                <Bar
                  dataKey="amount"
                  fill="var(--color-amount)"
                  radius={[8, 8, 0, 0]}
                  barSize={40}
                />
              </BarChart>
            </ResponsiveContainer>
          </ChartContainer>
        </CardContent>
      </Card>
    </div>
  );
}
