'use client';

import React, { useState } from 'react';

import { TrendingUp } from '@/components/icons';
import { useTranslations } from 'next-intl';
import clsx from 'clsx';
import { cn } from '@/lib/utils';
import { Button, Card, Collapse, Progress } from 'antd';



interface SpendingCategory {
  id: string;
  name: string;
  icon: React.ElementType;
  amount: number;
  percentage: number;
  trend: 'up' | 'down' | 'neutral';
  trendValue: number;
  color: string;
}

interface SpendingInsightsProps {
  data?: SpendingCategory[];
  currency?: string;
  className?: string;
  isLoading?: boolean;
}

export default function SpendingInsights({
  data,
  currency = 'Rp',
  className = '',
  isLoading = false,
}: SpendingInsightsProps) {
  const t = useTranslations('dashboard');
  const [viewMode, setViewMode] = useState<'category' | 'monthly'>('category');

  const categories = data ?? [];
  const totalSpending = categories.reduce((sum, cat) => sum + cat.amount, 0);
  const highestCategory = categories.length > 0
    ? categories.reduce((max, cat) => (cat.amount > max.amount ? cat : max), categories[0])
    : null;

  // State for manual expansion removed in favor of Accordion

  return (
    <Card
      role="region"
      aria-labelledby="spending-insights-title"
      className={cn("relative overflow-hidden h-full flex flex-col group", className)}
      styles={{ body: { display: 'contents' } }}
    >
      <div className="absolute bottom-0 left-0 w-40 h-40 bg-primary/5 rounded-full blur-3xl translate-y-1/2 -translate-x-1/2 pointer-events-none" />

      <div className="flex flex-col space-y-1.5 p-6 flex flex-row items-start justify-between space-y-0 pb-6 shrink-0 z-10">
        <div>
          <h3 id="spending-insights-title" className="text-2xl font-bold leading-none tracking-tight text-base sm:text-lg font-bold text-foreground tracking-widest uppercase">
            {t('spendingInsights')}
          </h3>
          <p className="text-xs text-muted-foreground font-medium uppercase tracking-[0.1em]">
            {new Date().toLocaleDateString(undefined, { month: 'long', year: 'numeric' })}
          </p>
        </div>

        {/* View Mode Toggle */}
        <div className="flex gap-1 bg-muted/50 rounded-lg p-1">
          <Button
            type="default"
            onClick={() => setViewMode('category')}
            aria-label="Tampilan per kategori"
            className={clsx(
              'px-3 py-1.5 min-h-[44px] min-w-[44px] text-xs font-extrabold transition-all',
              viewMode === 'category' ? 'bg-card text-foreground shadow-sm' : 'text-muted-foreground'
            )}
            aria-pressed={viewMode === 'category'}
          >
            Kategori
          </Button>
          <Button
            type="default"
            onClick={() => setViewMode('monthly')}
            aria-label="Tampilan bulanan"
            className={clsx(
              'px-3 py-1.5 min-h-[44px] min-w-[44px] text-xs font-extrabold transition-all',
              viewMode === 'monthly' ? 'bg-card text-foreground shadow-sm' : 'text-muted-foreground'
            )}
            aria-pressed={viewMode === 'monthly'}
          >
            Bulanan
          </Button>
        </div>
      </div>

      <div className="p-6 pt-0 flex-1 overflow-y-auto z-10 relative scrollbar-hide">
        {isLoading ? (
          <div className="flex items-center justify-center min-h-[120px]">
            <p className="text-sm text-muted-foreground font-bold uppercase tracking-widest">Memuat...</p>
          </div>
        ) : categories.length === 0 ? (
          <div className="flex items-center justify-center min-h-[120px]">
            <p className="text-sm text-muted-foreground font-bold uppercase tracking-widest">Belum ada data pengeluaran</p>
          </div>
        ) : (
        <>
        {/* Summary */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 mb-6">
          <Card className="bg-muted/50 border-surface/5 shadow-sm" styles={{ body: { display: 'contents' } }}>
            <div className="p-6 pt-0 p-4">
              <p className="text-xs text-muted-foreground font-bold uppercase tracking-wider mb-1">
                Total Pengeluaran
              </p>
              <p className="text-xl font-bold text-foreground tabular-nums">
                {currency} {totalSpending.toLocaleString('id-ID')}
              </p>
            </div>
          </Card>
          <Card className="bg-muted/50 border-surface/5 shadow-sm" styles={{ body: { display: 'contents' } }}>
            <div className="p-6 pt-0 p-4">
              <p className="text-xs text-muted-foreground font-bold uppercase tracking-wider mb-1">
                Kategori Terbesar
              </p>
              {highestCategory && (
              <div className="flex items-center gap-2">
                <highestCategory.icon className="h-4 w-4 text-primary" />
                <p className="text-sm font-bold text-foreground uppercase tracking-tight">{highestCategory.name}</p>
              </div>
              )}
              {highestCategory && (
              <p className="text-xs text-muted-foreground tabular-nums font-medium">
                {currency} {highestCategory.amount.toLocaleString('id-ID')}
              </p>
              )}
            </div>
          </Card>
        </div>
        {/* Category List with Shadcn Accordion */}
        <Collapse
          accordion
          aria-label="Daftar kategori pengeluaran"
          className="space-y-3 pb-2"
          expandIcon={() => null}
          items={categories.map((category) => {
            const Icon = category.icon;
            const panelId = `spending-panel-${category.id}`;
            return {
              key: category.id,
              className: 'bg-muted/30 rounded-xl border-none overflow-hidden',
              label: (
                <span
                  role="button"
                  aria-expanded={false}
                  aria-controls={panelId}
                  className="flex flex-1 items-center justify-between py-4 text-xs font-bold uppercase tracking-[0.1em] hover:no-underline px-4 py-4 group/trigger"
                >
                  <div className="flex items-center gap-3 w-full text-left">
                    <div
                      className={cn(
                        'h-10 w-10 rounded-2xl flex items-center justify-center flex-shrink-0 transition-transform group-hover/trigger:scale-110',
                        category.color
                      )}
                    >
                      <Icon className="h-5 w-5 text-surface" aria-hidden="true" />
                    </div>
                    <div className="flex-1 min-w-0 pr-4">
                      <div className="flex items-center justify-between mb-2">
                        <p className="text-xs font-bold text-foreground uppercase tracking-tight">{category.name}</p>
                        <p className="text-xs font-bold text-foreground tabular-nums">
                          {currency} {category.amount.toLocaleString('id-ID')}
                        </p>
                      </div>
                      <Progress
                        percent={category.percentage}
                        showInfo={false}
                        railColor="transparent"
                        className="relative h-2 w-full overflow-hidden rounded-full bg-muted/50 h-1.5"
                        classNames={{ track: `bg-primary transition-all duration-500 ease-in-out ${category.color}` }}
                        styles={{ body: { height: '100%' }, rail: { height: '100%' }, track: { height: '100%' } }}
                        aria-label={`${category.name}: ${category.percentage}% dari total`}
                      />
                    </div>
                    <div
                      className={cn(
                        'flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-bold flex-shrink-0 mr-4',
                        category.trend === 'up'
                          ? 'bg-destructive/10 text-destructive'
                          : category.trend === 'down'
                          ? 'bg-primary/10 text-primary'
                          : 'bg-muted text-muted-foreground'
                      )}
                      aria-label={`Tren ${category.trend === 'up' ? 'naik' : category.trend === 'down' ? 'turun' : 'tetap'} ${category.trendValue}%`}
                    >
                      <TrendingUp aria-hidden="true" className={cn('h-3 w-3', category.trend === 'down' && 'rotate-180')} />
                      {category.trendValue}%
                    </div>
                  </div>
                </span>
              ),
              children: (
                <div id={panelId} className="px-4 pb-4 pt-0">
                  <div className="space-y-4 pt-4 border-t border-border/10">
                    <div className="grid grid-cols-2 gap-4">
                      <div className="space-y-1">
                        <p className="text-xs text-muted-foreground font-bold uppercase tracking-widest">Persentase</p>
                        <p className="text-xs font-bold text-foreground">{category.percentage}% dari total</p>
                      </div>
                      <div className="space-y-1">
                        <p className="text-xs text-muted-foreground font-bold uppercase tracking-widest">Status</p>
                        <p className={cn(
                          "text-xs font-bold",
                          category.trend === 'up' ? "text-destructive" : "text-primary"
                        )}>
                          {category.trend === 'up' ? 'Meningkat' : 'Menurun'}
                        </p>
                      </div>
                    </div>
                    <div className="flex gap-2">
                      <Button aria-label={`Lihat transaksi ${category.name}`} size="small" type="primary" className="flex-1 text-xs font-bold uppercase tracking-widest min-h-[44px]">
                        Lihat Transaksi
                      </Button>
                      <Button aria-label={`Set anggaran ${category.name}`} type="default" size="small" className="flex-1 text-xs font-bold uppercase tracking-widest min-h-[44px] bg-muted/30">
                        Set Anggaran
                      </Button>
                    </div>
                  </div>
                </div>
              ),
            };
          })}
        />
        </>
        )}
      </div>
    </Card>
  );
}
