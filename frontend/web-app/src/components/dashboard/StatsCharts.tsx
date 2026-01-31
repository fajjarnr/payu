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
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { ChartConfig, ChartContainer, ChartTooltip, ChartTooltipContent } from "@/components/ui/chart"

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

const investmentData = [
  { category: "return", value: 12.5, fill: "var(--color-return)" },
]

const investmentConfig = {
  value: {
    label: "Return",
  },
  return: {
    label: "ROI",
    color: "hsl(var(--primary))",
  },
} satisfies ChartConfig

const spendingData = [
  { month: "Jan", amount: 3000000 },
  { month: "Feb", amount: 4500000 },
  { month: "Mar", amount: 6000000 },
  { month: "Apr", amount: 8000000 },
  { month: "Mei", amount: 10000000 },
  { month: "Jun", amount: 7000000 },
  { month: "Jul", amount: 9000000 },
]

const spendingConfig = {
  amount: {
    label: "Pengeluaran",
    color: "hsl(var(--primary))",
  },
} satisfies ChartConfig

interface StatsChartsProps {
  className?: string;
}

export default function StatsCharts({ className = '' }: StatsChartsProps) {
  return (
    <div className={cn("grid grid-cols-1 md:grid-cols-2 lg:grid-cols-12 gap-6 lg:gap-8", className)}>
      {/* Investment Performance (Official Shadcn Radial Chart) */}
      <Card className="lg:col-span-5 relative overflow-hidden group">
        <CardHeader className="flex flex-row items-center justify-between pb-2">
          <CardTitle className="text-sm font-black text-foreground tracking-widest uppercase">
            Performa Investasi
          </CardTitle>
          <div className="flex items-center gap-1.5 text-[10px] font-black text-muted-foreground bg-muted/50 px-3 py-1.5 rounded-lg cursor-pointer hover:bg-muted transition-colors uppercase tracking-widest">
            Januari 2026 <ChevronDown className="h-3.5 w-3.5" />
          </div>
        </CardHeader>

        <CardContent className="flex flex-col sm:flex-row items-center justify-between gap-4">
          <div className="space-y-4 w-full sm:w-auto">
            <ChartLegend color="bg-primary" label="Saham" percentage="60%" />
            <ChartLegend color="bg-primary/60" label="Obligasi" percentage="25%" />
            <ChartLegend color="bg-primary/30" label="Emas Digital" percentage="15%" />
          </div>

          <div className="relative h-44 w-44 flex-shrink-0">
            <ChartContainer
              config={investmentConfig}
              className="mx-auto aspect-square h-full w-full"
            >
              <RadialBarChart
                data={investmentData}
                startAngle={0}
                endAngle={250}
                innerRadius={65}
                outerRadius={95}
              >
                <PolarGrid
                  gridType="circle"
                  radialLines={false}
                  stroke="none"
                  className="first:fill-muted/20 last:fill-background"
                  polarRadius={[70, 60]}
                />
                <RadialBar 
                  dataKey="value" 
                  background 
                  cornerRadius={10}
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
                              className="fill-foreground text-2xl font-black tabular-nums"
                            >
                              +12.5%
                            </tspan>
                            <tspan
                              x={viewBox.cx}
                              y={(viewBox.cy || 0) + 20}
                              className="fill-muted-foreground text-[10px] font-black uppercase tracking-widest"
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

          <div className="text-right w-full sm:w-auto shrink-0">
            <p className="text-[10px] text-muted-foreground font-black tracking-widest mb-1 uppercase">Total Nilai</p>
            <h4 className="text-xl font-black text-foreground tabular-nums">Rp 8.750k</h4>
          </div>
        </CardContent>
      </Card>

      {/* Spending Overview (Official Shadcn Bar Chart) */}
      <Card className="lg:col-span-7 group overflow-hidden">
        <CardHeader className="flex flex-row items-center justify-between pb-4">
          <CardTitle className="text-sm font-black text-foreground tracking-widest uppercase">
            Ikhtisar Pengeluaran
          </CardTitle>
          <div className="flex items-center gap-1.5 text-[10px] font-black text-muted-foreground bg-muted/50 px-3 py-1.5 rounded-lg cursor-pointer hover:bg-muted transition-colors uppercase tracking-widest">
            Tahun 2026 <ChevronDown className="h-3.5 w-3.5" />
          </div>
        </CardHeader>

        <CardContent>
          <ChartContainer config={spendingConfig} className="h-48 w-full">
            <BarChart data={spendingData} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
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
                radius={[6, 6, 0, 0]}
                barSize={32}
              />
            </BarChart>
          </ChartContainer>
        </CardContent>
      </Card>
    </div>
  );
}
