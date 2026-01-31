'use client';

import * as React from 'react';
import { TrendingUp, ArrowUpRight, Target } from 'lucide-react';
import {
  Label,
  PolarGrid,
  PolarRadiusAxis,
  RadialBar,
  RadialBarChart,
} from 'recharts';

import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from '@/components/ui/card';
import {
  ChartContainer,
  type ChartConfig,
} from '@/components/ui/chart';
import { cn } from '@/lib/utils';

export const description = 'Statistik performa investasi dalam format radial';

// Data simulasi performa investasi
const chartData = [
  { category: 'return', value: 12.5, fill: 'var(--color-return)' },
];

const chartConfig = {
  value: {
    label: 'Return',
  },
  return: {
    label: 'ROI',
    color: 'hsl(var(--primary))',
  },
} satisfies ChartConfig;

interface InvestmentPerformanceProps {
  className?: string;
  roi?: number;
  totalInvestment?: number;
}

export default function InvestmentPerformance({
  className,
  roi = 12.5,
  totalInvestment = 150000000,
}: InvestmentPerformanceProps) {
  return (
    <Card className={cn('flex flex-col group overflow-hidden h-full', className)}>
      <CardHeader className="items-start pb-2">
        <div className="flex items-center gap-2 mb-1">
          <div className="h-8 w-8 rounded-lg bg-primary/10 flex items-center justify-center">
            <TrendingUp className="h-4 w-4 text-primary" />
          </div>
          <CardTitle className="text-sm font-bold text-foreground tracking-widest uppercase">
            Performa Investasi
          </CardTitle>
        </div>
        <CardDescription>ROI Tahunan (Yield)</CardDescription>
      </CardHeader>
      
      <CardContent className="flex-1 pb-4 flex flex-col justify-center">
        <ChartContainer
          config={chartConfig}
          className="mx-auto aspect-square max-h-[220px] w-full"
        >
          <RadialBarChart
            data={[{ ...chartData[0], value: roi }]}
            startAngle={90}
            endAngle={90 + (roi / 20) * 360} // Skala 20% dianggap 100% radial untuk visualisasi
            innerRadius={80}
            outerRadius={110}
          >
            <PolarGrid
              gridType="circle"
              radialLines={false}
              stroke="none"
              className="first:fill-muted/20 last:fill-background"
              polarRadius={[86, 74]}
            />
            <RadialBar 
              dataKey="value" 
              background={{ fill: 'hsl(var(--muted)/0.2)' }}
              cornerRadius={10} 
            />
            <PolarRadiusAxis tick={false} tickLine={false} axisLine={false}>
              <Label
                content={({ viewBox }) => {
                  if (viewBox && 'cx' in viewBox && 'cy' in viewBox) {
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
                          className="fill-foreground text-3xl font-bold tabular-nums"
                        >
                          +{roi}%
                        </tspan>
                        <tspan
                          x={viewBox.cx}
                          y={(viewBox.cy || 0) + 20}
                          className="fill-muted-foreground text-xs font-bold uppercase tracking-widest"
                        >
                          Annual ROI
                        </tspan>
                      </text>
                    );
                  }
                }}
              />
            </PolarRadiusAxis>
          </RadialBarChart>
        </ChartContainer>

        <div className="grid grid-cols-2 gap-4 mt-2">
          <div className="bg-muted/30 p-3 rounded-xl border border-border/50">
            <p className="text-xs font-bold text-muted-foreground uppercase tracking-widest mb-1 flex items-center gap-1">
              <Target className="h-3 w-3" /> Target
            </p>
            <p className="text-xs font-bold text-foreground">15.0%</p>
          </div>
          <div className="bg-muted/30 p-3 rounded-xl border border-border/50">
            <p className="text-xs font-bold text-muted-foreground uppercase tracking-widest mb-1 flex items-center gap-1">
              <ArrowUpRight className="h-3 w-3" /> Profit
            </p>
            <p className="text-xs font-bold text-primary">Rp 18.75Jt</p>
          </div>
        </div>
      </CardContent>

      <CardFooter className="flex-col gap-2 pt-0 pb-6 border-t border-border/10">
        <div className="flex items-center gap-2 leading-none font-bold text-xs uppercase tracking-widest text-primary mt-4">
          Naik 2.1% bulan ini <TrendingUp className="h-3 w-3" />
        </div>
        <div className="text-xs text-muted-foreground lowercase leading-none">
          Berdasarkan total investasi Rp {(totalInvestment / 1000000).toFixed(0)}Jt
        </div>
      </CardFooter>
    </Card>
  );
}
