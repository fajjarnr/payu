'use client';

import React, { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { ChevronDown, ChevronUp, TrendingUp, ShoppingCart, Utensils, Home, Car, Smartphone, HeartPulse } from 'lucide-react';
import { useTranslations } from 'next-intl';
import clsx from 'clsx';

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
}

const defaultCategories: SpendingCategory[] = [
  {
    id: 'food',
    name: 'Makanan & Minuman',
    icon: Utensils,
    amount: 2500000,
    percentage: 35,
    trend: 'up',
    trendValue: 12,
    color: 'bg-chart-1',
  },
  {
    id: 'shopping',
    name: 'Belanja',
    icon: ShoppingCart,
    amount: 1800000,
    percentage: 25,
    trend: 'down',
    trendValue: 8,
    color: 'bg-chart-2',
  },
  {
    id: 'housing',
    name: 'Perumahan',
    icon: Home,
    amount: 1200000,
    percentage: 17,
    trend: 'neutral',
    trendValue: 2,
    color: 'bg-chart-3',
  },
  {
    id: 'transport',
    name: 'Transportasi',
    icon: Car,
    amount: 900000,
    percentage: 13,
    trend: 'up',
    trendValue: 5,
    color: 'bg-chart-green1',
  },
  {
    id: 'bills',
    name: 'Tagihan & Pulsa',
    icon: Smartphone,
    amount: 500000,
    percentage: 7,
    trend: 'down',
    trendValue: 3,
    color: 'bg-chart-green2',
  },
  {
    id: 'health',
    name: 'Kesehatan',
    icon: HeartPulse,
    amount: 300000,
    percentage: 3,
    trend: 'neutral',
    trendValue: 0,
    color: 'bg-chart-green3',
  },
];

export default function SpendingInsights({
  data = defaultCategories,
  currency = 'Rp',
  className = '',
}: SpendingInsightsProps) {
  const t = useTranslations('dashboard');
  const [expandedCategory, setExpandedCategory] = useState<string | null>(null);
  const [viewMode, setViewMode] = useState<'category' | 'monthly'>('category');

  const totalSpending = data.reduce((sum, cat) => sum + cat.amount, 0);
  const highestCategory = data.reduce((max, cat) => (cat.amount > max.amount ? cat : max), data[0]);

  const toggleCategory = (categoryId: string) => {
    setExpandedCategory(expandedCategory === categoryId ? null : categoryId);
  };

  return (
    <div
      className={clsx(
        'bg-card p-6 sm:p-8 rounded-xl border border-border shadow-card relative overflow-hidden',
        className
      )}
      role="region"
      aria-labelledby="spending-insights-title"
    >
      {/* Decorative background */}
      <div className="absolute bottom-0 left-0 w-40 h-40 bg-primary/5 rounded-full blur-3xl translate-y-1/2 -translate-x-1/2" />

      {/* Header */}
      <div className="flex justify-between items-start mb-6">
        <div>
          <h2
            id="spending-insights-title"
            className="text-xs font-semibold text-muted-foreground tracking-widest mb-1"
          >
            {t('spendingInsights')}
          </h2>
          <p className="text-[10px] text-muted-foreground">
            Periode: {new Date().toLocaleDateString('id-ID', { month: 'long', year: 'numeric' })}
          </p>
        </div>

        {/* View Mode Toggle */}
        <div className="flex gap-1 bg-muted/50 rounded-lg p-1">
          <button
            onClick={() => setViewMode('category')}
            className={clsx(
              'px-3 py-1.5 rounded-md text-[10px] font-bold transition-all',
              viewMode === 'category'
                ? 'bg-card text-foreground shadow-sm'
                : 'text-muted-foreground hover:text-foreground'
            )}
            aria-pressed={viewMode === 'category'}
            aria-label="Tampilan per kategori"
          >
            Kategori
          </button>
          <button
            onClick={() => setViewMode('monthly')}
            className={clsx(
              'px-3 py-1.5 rounded-md text-[10px] font-bold transition-all',
              viewMode === 'monthly'
                ? 'bg-card text-foreground shadow-sm'
                : 'text-muted-foreground hover:text-foreground'
            )}
            aria-pressed={viewMode === 'monthly'}
            aria-label="Tampilan bulanan"
          >
            Bulanan
          </button>
        </div>
      </div>

      {/* Summary */}
      <div className="grid grid-cols-2 gap-4 mb-6">
        <div className="bg-muted/30 rounded-lg p-4">
          <p className="text-[10px] text-muted-foreground font-semibold tracking-wider mb-1">
            Total Pengeluaran
          </p>
          <p className="text-xl sm:text-2xl font-black text-foreground tabular-nums">
            {currency} {totalSpending.toLocaleString('id-ID')}
          </p>
        </div>
        <div className="bg-muted/30 rounded-lg p-4">
          <p className="text-[10px] text-muted-foreground font-semibold tracking-wider mb-1">
            Kategori Terbesar
          </p>
          <div className="flex items-center gap-2">
            <highestCategory.icon className="h-4 w-4 text-primary" />
            <p className="text-sm font-bold text-foreground">{highestCategory.name}</p>
          </div>
          <p className="text-xs text-muted-foreground tabular-nums">
            {currency} {highestCategory.amount.toLocaleString('id-ID')}
          </p>
        </div>
      </div>

      {/* Category List */}
      <div className="space-y-3" role="list" aria-label="Daftar kategori pengeluaran">
        <AnimatePresence mode="popLayout">
          {data.map((category, index) => {
            const Icon = category.icon;
            const isExpanded = expandedCategory === category.id;

            return (
              <motion.div
                key={category.id}
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -20 }}
                transition={{ duration: 0.3, delay: index * 0.05 }}
                className="bg-muted/30 rounded-lg overflow-hidden"
                role="listitem"
              >
                {/* Category Header */}
                <button
                  onClick={() => toggleCategory(category.id)}
                  className={clsx(
                    'w-full px-4 py-3 flex items-center gap-3 text-left transition-colors',
                    'hover:bg-muted/50 focus:outline-none focus:ring-2 focus:ring-primary focus:ring-inset'
                  )}
                  aria-expanded={isExpanded}
                  aria-controls={`category-details-${category.id}`}
                >
                  {/* Icon */}
                  <div
                    className={clsx(
                      'h-10 w-10 rounded-xl flex items-center justify-center flex-shrink-0',
                      category.color
                    )}
                  >
                    <Icon className="h-5 w-5 text-white" aria-hidden="true" />
                  </div>

                  {/* Name and Progress */}
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center justify-between mb-1">
                      <p className="text-xs font-bold text-foreground">{category.name}</p>
                      <p className="text-xs font-bold text-foreground tabular-nums">
                        {currency} {category.amount.toLocaleString('id-ID')}
                      </p>
                    </div>
                    {/* Progress bar */}
                    <div className="h-1.5 w-full bg-muted rounded-full overflow-hidden">
                      <motion.div
                        className={clsx('h-full rounded-full', category.color)}
                        initial={{ width: 0 }}
                        animate={{ width: `${category.percentage}%` }}
                        transition={{ duration: 0.8, delay: index * 0.05 }}
                        role="progressbar"
                        aria-valuenow={category.percentage}
                        aria-valuemin={0}
                        aria-valuemax={100}
                        aria-label={`${category.name}: ${category.percentage}% dari total`}
                      />
                    </div>
                  </div>

                  {/* Trend */}
                  <div
                    className={clsx(
                      'flex items-center gap-1 px-2 py-1 rounded-full text-[10px] font-bold flex-shrink-0',
                      category.trend === 'up'
                        ? 'bg-destructive/10 text-destructive'
                        : category.trend === 'down'
                        ? 'bg-success-light text-primary'
                        : 'bg-muted text-muted-foreground'
                    )}
                    aria-label={`Tren ${category.trend === 'up' ? 'naik' : category.trend === 'down' ? 'turun' : 'stabil'} ${Math.abs(category.trendValue)}%`}
                  >
                    <TrendingUp className={clsx('h-3 w-3', category.trend === 'down' && 'rotate-180')} />
                    {category.trendValue}%
                  </div>

                  {/* Expand/Collapse Icon */}
                  <div className="flex-shrink-0">
                    {isExpanded ? (
                      <ChevronUp className="h-4 w-4 text-muted-foreground" aria-hidden="true" />
                    ) : (
                      <ChevronDown className="h-4 w-4 text-muted-foreground" aria-hidden="true" />
                    )}
                  </div>
                </button>

                {/* Expanded Details */}
                <AnimatePresence>
                  {isExpanded && (
                    <motion.div
                      id={`category-details-${category.id}`}
                      initial={{ height: 0, opacity: 0 }}
                      animate={{ height: 'auto', opacity: 1 }}
                      exit={{ height: 0, opacity: 0 }}
                      transition={{ duration: 0.3 }}
                      className="overflow-hidden"
                    >
                      <div className="px-4 pb-4 pt-2 space-y-3 border-t border-border/50">
                        {/* Percentage of total */}
                        <div className="flex justify-between items-center">
                          <span className="text-[10px] text-muted-foreground">Persentase dari Total</span>
                          <span className="text-xs font-bold text-foreground">{category.percentage}%</span>
                        </div>

                        {/* Trend info */}
                        <div className="flex justify-between items-center">
                          <span className="text-[10px] text-muted-foreground">Perubahan vs Bulan Lalu</span>
                          <span
                            className={clsx(
                              'text-xs font-bold',
                              category.trend === 'up'
                                ? 'text-destructive'
                                : category.trend === 'down'
                                ? 'text-primary'
                                : 'text-muted-foreground'
                            )}
                          >
                            {category.trend === 'up' ? '+' : category.trend === 'down' ? '-' : ''}
                            {category.trendValue}%
                          </span>
                        </div>

                        {/* Actions */}
                        <div className="flex gap-2 pt-2">
                          <button
                            className="flex-1 px-3 py-2 bg-primary text-primary-foreground rounded-lg text-[10px] font-bold hover:bg-primary/90 transition-colors focus:outline-none focus:ring-2 focus:ring-primary focus:ring-offset-2"
                            aria-label={`Lihat detail transaksi ${category.name}`}
                          >
                            Lihat Transaksi
                          </button>
                          <button
                            className="flex-1 px-3 py-2 bg-muted text-foreground rounded-lg text-[10px] font-bold hover:bg-muted/70 transition-colors focus:outline-none focus:ring-2 focus:ring-primary focus:ring-offset-2"
                            aria-label={`Set anggaran untuk ${category.name}`}
                          >
                            Set Anggaran
                          </button>
                        </div>
                      </div>
                    </motion.div>
                  )}
                </AnimatePresence>
              </motion.div>
            );
          })}
        </AnimatePresence>
      </div>
    </div>
  );
}
