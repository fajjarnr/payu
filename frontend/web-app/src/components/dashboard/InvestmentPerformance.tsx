'use client';

import * as React from 'react';
import { useTranslations } from 'next-intl';
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
  targetRoi?: number;
  profit?: number;
  monthlyChange?: number;
  isLoading?: boolean;
}

export default function InvestmentPerformance({
  className,
  roi,
  totalInvestment,
  targetRoi,
  profit,
  monthlyChange,
  isLoading = false,
}: InvestmentPerformanceProps) {
  const t = useTranslations('investments');
  if (isLoading) {
    return (
      <Card className={cn('flex flex-col group overflow-hidden h-full', className)}>
        <CardHeader className="items-start pb-2">
          <CardTitle className="text-sm font-bold text-foreground tracking-widest uppercase">
            {t('perfTitle')}
          </CardTitle>
        </CardHeader>
        <CardContent className="flex-1 flex items-center justify-center min-h-[200px]">
          <p className="text-sm text-muted-foreground font-bold uppercase tracking-widest">{t('loading')}</p>
        </CardContent>
      </Card>
    );
  }

  const displayRoi = roi ?? 0;
  const displayInvestment = totalInvestment ?? 0;
  const displayTarget = targetRoi ?? 0;
  const displayProfit = profit ?? 0;
  const displayMonthlyChange = monthlyChange ?? 0;
  return (
    <Card className={cn('flex flex-col group overflow-hidden h-full', className)}>
      <CardHeader className="items-start pb-2">
        <div className="flex items-center gap-2 mb-1">
          <div className="h-8 w-8 rounded-lg bg-primary/10 flex items-center justify-center">
            <TrendingUp className="h-4 w-4 text-primary" />
          </div>
          <CardTitle className="text-sm font-bold text-foreground tracking-widest uppercase">
            {t('perfTitle')}
          </CardTitle>
        </div>
        <CardDescription>{t('perfYield')}</CardDescription>
      </CardHeader>
      
      <CardContent className="flex-1 pb-4 flex flex-col justify-center">
        <ChartContainer
          config={chartConfig}
          className="mx-auto aspect-square max-h-[220px] w-full"
        >
          <RadialBarChart
            data={[{ ...chartData[0], value: displayRoi }]}
            startAngle={90}
            endAngle={90 + (displayRoi / 20) * 360} // Skala 20% dianggap 100% radial untuk visualisasi
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
                          +{displayRoi}%
                        </tspan>
                        <tspan
                          x={viewBox.cx}
                          y={(viewBox.cy || 0) + 20}
                          className="fill-muted-foreground text-xs font-bold uppercase tracking-widest"
                        >
                          {t('annualRoi')}
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
          <div className="bg-muted/30 p-4 rounded-xl border border-border/50">
            <p className="text-xs font-bold text-muted-foreground uppercase tracking-widest mb-1 flex items-center gap-1">
              <Target className="h-3 w-3" /> {t('target')}
            </p>
            <p className="text-xs font-bold text-foreground">{displayTarget > 0 ? `${displayTarget}%` : '--'}</p>
          </div>
          <div className="bg-muted/30 p-4 rounded-xl border border-border/50">
            <p className="text-xs font-bold text-muted-foreground uppercase tracking-widest mb-1 flex items-center gap-1">
              <ArrowUpRight className="h-3 w-3" /> {t('profit')}
            </p>
            <p className="text-xs font-bold text-primary">
              {displayProfit > 0 ? `Rp ${(displayProfit / 1000000).toFixed(2)}Jt` : 'Rp 0'}
            </p>
          </div>
        </div>
      </CardContent>

      <CardFooter className="flex-col gap-2 pt-0 pb-6 border-t border-border/10">
        {displayMonthlyChange !== 0 && (
        <div className="flex items-center gap-2 leading-none font-bold text-xs uppercase tracking-widest text-primary mt-4">
          {displayMonthlyChange > 0 ? t('up') : t('down')} {Math.abs(displayMonthlyChange)}% {t('thisMonth')} <TrendingUp className="h-3 w-3" />
        </div>
        )}
        <div className="text-xs text-muted-foreground lowercase leading-none">
          {displayInvestment > 0
            ? t('basedOnTotal', { amount: `Rp ${(displayInvestment / 1000000).toFixed(0)}Jt` })
            : t('noData')}
        </div>
      </CardFooter>
    </Card>
  );
}
