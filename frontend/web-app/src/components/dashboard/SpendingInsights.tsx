'use client';

import React, { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { ChevronDown, ChevronUp, TrendingUp, ShoppingCart, Utensils, Home, Car, Smartphone, HeartPulse } from 'lucide-react';
import { useLocale, useTranslations } from 'next-intl';
import clsx from 'clsx';
import { cn } from '@/lib/utils';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { 
  Accordion, 
  AccordionContent, 
  AccordionItem, 
  AccordionTrigger 
} from '@/components/ui/accordion';
import { Progress } from '@/components/ui/progress';

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
    <Card className={cn("relative overflow-hidden h-full flex flex-col group", className)}>
      {/* Decorative background */}
      <div className="absolute bottom-0 left-0 w-40 h-40 bg-primary/5 rounded-full blur-3xl translate-y-1/2 -translate-x-1/2 pointer-events-none" />

      <CardHeader className="flex flex-row items-start justify-between space-y-0 shrink-0 z-10">
        <div>
          <CardTitle className="text-sm font-bold text-foreground tracking-widest uppercase">
            {t('spendingInsights')}
          </CardTitle>
          <CardDescription>
            {new Date().toLocaleDateString('id-ID', { month: 'long', year: 'numeric' })}
          </CardDescription>
        </div>

        {/* View Mode Toggle */}
        <div className="flex gap-1 bg-muted/50 rounded-lg p-1">
          <Button
            variant={viewMode === 'category' ? "secondary" : "ghost"}
            size="sm"
            onClick={() => setViewMode('category')}
            className={clsx(
              'px-3 py-1.5 h-auto text-xs font-extrabold transition-all',
              viewMode === 'category' ? 'bg-card text-foreground shadow-sm' : 'text-muted-foreground'
            )}
            aria-pressed={viewMode === 'category'}
          >
            Kategori
          </Button>
          <Button
            variant={viewMode === 'monthly' ? "secondary" : "ghost"}
            size="sm"
            onClick={() => setViewMode('monthly')}
            className={clsx(
              'px-3 py-1.5 h-auto text-xs font-extrabold transition-all',
              viewMode === 'monthly' ? 'bg-card text-foreground shadow-sm' : 'text-muted-foreground'
            )}
            aria-pressed={viewMode === 'monthly'}
          >
            Bulanan
          </Button>
        </div>
      </CardHeader>

      <CardContent className="flex-1 overflow-y-auto z-10 relative scrollbar-hide">
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
          <Card className="bg-muted/50 border-white/5 shadow-sm">
            <CardContent className="p-4">
              <p className="text-xs text-muted-foreground font-bold uppercase tracking-wider mb-1">
                Total Pengeluaran
              </p>
              <p className="text-xl font-bold text-foreground tabular-nums">
                {currency} {totalSpending.toLocaleString('id-ID')}
              </p>
            </CardContent>
          </Card>
          <Card className="bg-muted/50 border-white/5 shadow-sm">
            <CardContent className="p-4">
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
            </CardContent>
          </Card>
        </div>
        {/* Category List with Shadcn Accordion */}
        <Accordion type="single" collapsible className="space-y-3 pb-2">
          {categories.map((category, index) => {
            const Icon = category.icon;

            return (
              <AccordionItem 
                key={category.id} 
                value={category.id}
                className="bg-muted/30 rounded-xl border-none overflow-hidden"
              >
                <AccordionTrigger className="hover:no-underline px-4 py-8 group/trigger">
                  <div className="flex items-center gap-3 w-full text-left">
                    {/* Icon */}
                    <div
                      className={cn(
                        'h-10 w-10 rounded-2xl flex items-center justify-center flex-shrink-0 transition-transform group-hover/trigger:scale-110',
                        category.color
                      )}
                    >
                      <Icon className="h-5 w-5 text-white" aria-hidden="true" />
                    </div>

                    {/* Name and Progress */}
                    <div className="flex-1 min-w-0 pr-4">
                      <div className="flex items-center justify-between mb-2">
                        <p className="text-xs font-bold text-foreground uppercase tracking-tight">{category.name}</p>
                        <p className="text-xs font-bold text-foreground tabular-nums">
                          {currency} {category.amount.toLocaleString('id-ID')}
                        </p>
                      </div>
                      {/* Progress bar */}
                      <Progress 
                        value={category.percentage} 
                        className="h-1.5" 
                        indicatorClassName={category.color}
                        aria-label={`${category.name}: ${category.percentage}% dari total`}
                      />
                    </div>

                    {/* Trend */}
                    <div
                      className={cn(
                        'flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-bold flex-shrink-0 mr-4',
                        category.trend === 'up'
                          ? 'bg-destructive/10 text-destructive'
                          : category.trend === 'down'
                          ? 'bg-primary/10 text-primary'
                          : 'bg-muted text-muted-foreground'
                      )}
                    >
                      <TrendingUp className={cn('h-3 w-3', category.trend === 'down' && 'rotate-180')} />
                      {category.trendValue}%
                    </div>
                  </div>
                </AccordionTrigger>

                <AccordionContent className="px-4 pb-4 pt-0">
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
                      <Button size="sm" className="flex-1 text-xs font-bold uppercase tracking-widest h-10">
                        Lihat Detail
                      </Button>
                      <Button variant="outline" size="sm" className="flex-1 text-xs font-bold uppercase tracking-widest h-10 bg-muted/30">
                        Set Budget
                      </Button>
                    </div>
                  </div>
                </AccordionContent>
              </AccordionItem>
            );
          })}
        </Accordion>
        </>
        )}
      </CardContent>
    </Card>
  );
}
