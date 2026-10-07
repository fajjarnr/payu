'use client';

import dynamic from 'next/dynamic';
import * as React from 'react';
import { useTranslations } from 'next-intl';
const RadialBar = dynamic(() => import('@ant-design/plots').then(m => m.RadialBar), { ssr: false });

import { Card } from 'antd';
import { cn } from '@/lib/utils';

export const description = 'Statistik performa investasi dalam format radial';

// Canvas-rendered charts cannot resolve CSS var() colors, so use literals.
// Primary matches the DESIGN.md primary token (#00D09C).
const PRIMARY = '#00D09C';

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
      <Card className={cn('flex flex-col group overflow-hidden h-full', className)} styles={{ body: { display: 'contents' } }}>
        <div className="flex flex-col space-y-1.5 p-6 items-start pb-2">
          <h3 className="text-2xl font-bold leading-none tracking-tight text-sm font-bold text-foreground tracking-widest uppercase">
            {t('perfTitle')}
          </h3>
        </div>
        <div className="p-6 pt-0 flex-1 flex items-center justify-center min-h-[200px]">
          <p className="text-sm text-muted-foreground font-bold uppercase tracking-widest">{t('loading')}</p>
        </div>
      </Card>
    );
  }

  const displayRoi = roi ?? 0;
  const displayInvestment = totalInvestment ?? 0;
  const displayTarget = targetRoi ?? 0;
  const displayProfit = profit ?? 0;
  const displayMonthlyChange = monthlyChange ?? 0;

  // RadialBar full sweep maps to a 20% ROI scale for visualization.
  const maxRoi = 20;
  const fraction = Math.max(0, Math.min(1, displayRoi / maxRoi));

  return (
    <Card className={cn('flex flex-col group overflow-hidden h-full', className)} styles={{ body: { display: 'contents' } }}>
      <div className="flex flex-col space-y-1.5 p-6 items-start pb-2">
        <div className="flex items-center gap-2 mb-1">
          <div className="h-8 w-8 rounded-lg bg-primary/10 flex items-center justify-center">
            <TrendingUp className="h-4 w-4 text-primary" />
          </div>
          <h3 className="text-2xl font-bold leading-none tracking-tight text-sm font-bold text-foreground tracking-widest uppercase">
            {t('perfTitle')}
          </h3>
        </div>
        <p className="text-xs text-muted-foreground font-medium uppercase tracking-[0.1em]">{t('perfYield')}</p>
      </div>

      <div className="p-6 pt-0 flex-1 pb-4 flex flex-col justify-center">
        <div className="mx-auto max-h-[220px] w-full">
          <RadialBar
            data={[{ category: 'return', value: fraction }]}
            xField="category"
            yField="value"
            colorField="category"
            scale={{
              color: { range: [PRIMARY] },
              y: { domain: [0, 1] },
            }}
            innerRadius={0.73}
            legend={false}
            tooltip={false}
            label={false}
            markBackground={{ style: { fill: '#e5e7eb', fillOpacity: 0.5 } }}
            annotations={[
              {
                type: 'text',
                style: {
                  text: `+${displayRoi}%`,
                  x: '50%',
                  y: '46%',
                  textAlign: 'center',
                  fontSize: 28,
                  fontWeight: 700,
                  fill: '#0f172a',
                },
              },
              {
                type: 'text',
                style: {
                  text: t('annualRoi'),
                  x: '50%',
                  y: '56%',
                  textAlign: 'center',
                  fontSize: 11,
                  fontWeight: 700,
                  fill: '#6b7280',
                },
              },
            ]}
          />
        </div>

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
      </div>

      <div className="flex items-center p-6 pt-0 flex-col gap-2 pt-0 pb-6 border-t border-border/10">
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
      </div>
    </Card>
  );
}
