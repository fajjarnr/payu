'use client';

import React, { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Plus, AlertTriangle, CheckCircle2, TrendingUp, Edit, Trash2 } from 'lucide-react';
import { useTranslations } from 'next-intl';
import clsx from 'clsx';

interface Budget {
  id: string;
  category: string;
  limit: number;
  spent: number;
  remaining: number;
  percentage: number;
  status: 'safe' | 'warning' | 'danger' | 'exceeded';
  icon?: React.ElementType;
}

interface BudgetTrackingProps {
  budgets?: Budget[];
  currency?: string;
  className?: string;
}

const defaultBudgets: Budget[] = [
  {
    id: 'food',
    category: 'Makanan & Minuman',
    limit: 3000000,
    spent: 2500000,
    remaining: 500000,
    percentage: 83.33,
    status: 'warning',
  },
  {
    id: 'shopping',
    category: 'Belanja',
    limit: 2000000,
    spent: 1800000,
    remaining: 200000,
    percentage: 90,
    status: 'warning',
  },
  {
    id: 'transport',
    category: 'Transportasi',
    limit: 1500000,
    spent: 900000,
    remaining: 600000,
    percentage: 60,
    status: 'safe',
  },
  {
    id: 'bills',
    category: 'Tagihan & Pulsa',
    limit: 1000000,
    spent: 500000,
    remaining: 500000,
    percentage: 50,
    status: 'safe',
  },
  {
    id: 'entertainment',
    category: 'Hiburan',
    limit: 800000,
    spent: 900000,
    remaining: -100000,
    percentage: 112.5,
    status: 'exceeded',
  },
];

export default function BudgetTracking({
  budgets = defaultBudgets,
  currency = 'Rp',
  className = '',
}: BudgetTrackingProps) {
  const t = useTranslations('dashboard');
  const [expandedBudget, setExpandedBudget] = useState<string | null>(null);

  const totalBudget = budgets.reduce((sum, b) => sum + b.limit, 0);
  const totalSpent = budgets.reduce((sum, b) => sum + b.spent, 0);
  const totalRemaining = totalBudget - totalSpent;

  const exceededCount = budgets.filter((b) => b.status === 'exceeded').length;
  const warningCount = budgets.filter((b) => b.status === 'warning').length;

  const getStatusColor = (status: Budget['status']) => {
    switch (status) {
      case 'safe':
        return 'bg-success-light text-primary';
      case 'warning':
        return 'bg-yellow-50 dark:bg-yellow-950/30 text-yellow-600';
      case 'danger':
        return 'bg-orange-50 dark:bg-orange-950/30 text-orange-600';
      case 'exceeded':
        return 'bg-destructive/10 text-destructive';
      default:
        return 'bg-muted text-muted-foreground';
    }
  };

  const getProgressColor = (status: Budget['status']) => {
    switch (status) {
      case 'safe':
        return 'bg-primary';
      case 'warning':
        return 'bg-yellow-500';
      case 'danger':
        return 'bg-orange-500';
      case 'exceeded':
        return 'bg-destructive';
      default:
        return 'bg-muted';
    }
  };

  const getStatusIcon = (status: Budget['status']) => {
    switch (status) {
      case 'safe':
        return CheckCircle2;
      case 'warning':
      case 'danger':
        return AlertTriangle;
      case 'exceeded':
        return AlertTriangle;
      default:
        return AlertTriangle;
    }
  };

  return (
    <div
      className={clsx(
        'bg-card p-6 sm:p-8 rounded-xl border border-border shadow-card relative overflow-hidden',
        className
      )}
      role="region"
      aria-labelledby="budget-tracking-title"
    >
      {/* Decorative background */}
      <div className="absolute top-1/2 right-0 w-48 h-48 bg-primary/5 rounded-full blur-3xl translate-x-1/2 -translate-y-1/2" />

      {/* Header */}
      <div className="flex justify-between items-start mb-6">
        <div>
          <h2
            id="budget-tracking-title"
            className="text-xs font-semibold text-muted-foreground tracking-widest mb-1"
          >
            {t('budgetTracking')}
          </h2>
          <p className="text-[10px] text-muted-foreground">
            Bulan ini: {new Date().toLocaleDateString('id-ID', { month: 'long', year: 'numeric' })}
          </p>
        </div>

        <button
          className="flex items-center gap-2 px-4 py-2 bg-primary text-primary-foreground rounded-xl text-xs font-bold hover:bg-primary/90 transition-all shadow-lg shadow-primary/20 focus:outline-none focus:ring-2 focus:ring-primary focus:ring-offset-2"
          aria-label="Tambah anggaran baru"
        >
          <Plus className="h-4 w-4" />
          <span className="hidden sm:inline">Tambah</span>
        </button>
      </div>

      {/* Summary Cards */}
      <div className="grid grid-cols-3 gap-3 mb-6">
        <SummaryCard
          label="Total Anggaran"
          value={totalBudget}
          currency={currency}
          color="bg-success-light"
          textColor="text-primary"
        />
        <SummaryCard
          label="Terpakai"
          value={totalSpent}
          currency={currency}
          color="bg-muted/50"
          textColor="text-foreground"
        />
        <SummaryCard
          label="Sisa"
          value={totalRemaining}
          currency={currency}
          color={totalRemaining >= 0 ? 'bg-success-light' : 'bg-destructive/10'}
          textColor={totalRemaining >= 0 ? 'text-primary' : 'text-destructive'}
        />
      </div>

      {/* Alerts */}
      {(exceededCount > 0 || warningCount > 0) && (
        <motion.div
          initial={{ opacity: 0, y: -10 }}
          animate={{ opacity: 1, y: 0 }}
          className="mb-6 p-4 bg-destructive/5 border border-destructive/20 rounded-xl"
          role="alert"
          aria-live="polite"
        >
          <div className="flex items-center gap-3">
            <div className="h-8 w-8 rounded-full bg-destructive/10 flex items-center justify-center flex-shrink-0">
              <AlertTriangle className="h-4 w-4 text-destructive" aria-hidden="true" />
            </div>
            <div className="flex-1">
              <p className="text-xs font-bold text-foreground">
                {exceededCount > 0 && warningCount > 0
                  ? `${exceededCount} anggaran terlampaui dan ${warningCount} hampir habis`
                  : exceededCount > 0
                  ? `${exceededCount} anggaran terlampaui`
                  : `${warningCount} anggaran hampir habis`}
              </p>
              <p className="text-[10px] text-muted-foreground">
                {exceededCount > 0 ? 'Pertimbangkan untuk mengurangi pengeluaran' : 'Berhati-hatilah dengan pengeluaran'}
              </p>
            </div>
          </div>
        </motion.div>
      )}

      {/* Budget List */}
      <div className="space-y-3" role="list">
        <AnimatePresence mode="popLayout">
          {budgets.map((budget, index) => {
            const StatusIcon = getStatusIcon(budget.status);
            const isExpanded = expandedBudget === budget.id;

            return (
              <motion.div
                key={budget.id}
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -20 }}
                transition={{ duration: 0.3, delay: index * 0.05 }}
                className={clsx(
                  'bg-muted/30 rounded-lg overflow-hidden transition-all',
                  budget.status === 'exceeded' && 'ring-2 ring-destructive/20'
                )}
                role="listitem"
              >
                <button
                  onClick={() => setExpandedBudget(isExpanded ? null : budget.id)}
                  className={clsx(
                    'w-full px-4 py-3 flex items-center gap-3 text-left transition-colors',
                    'hover:bg-muted/50 focus:outline-none focus:ring-2 focus:ring-primary focus:ring-inset rounded-lg'
                  )}
                  aria-expanded={isExpanded}
                  aria-controls={`budget-details-${budget.id}`}
                >
                  {/* Status Icon */}
                  <div
                    className={clsx(
                      'h-10 w-10 rounded-xl flex items-center justify-center flex-shrink-0',
                      getStatusColor(budget.status)
                    )}
                  >
                    <StatusIcon className="h-5 w-5" aria-hidden="true" />
                  </div>

                  {/* Category and Progress */}
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center justify-between mb-1">
                      <p className="text-xs font-bold text-foreground">{budget.category}</p>
                      <div className="flex items-center gap-2">
                        <span className="text-[10px] text-muted-foreground tabular-nums">
                          {currency} {budget.spent.toLocaleString('id-ID')} / {budget.limit.toLocaleString('id-ID')}
                        </span>
                      </div>
                    </div>

                    {/* Progress bar */}
                    <div className="relative h-2 w-full bg-muted rounded-full overflow-hidden">
                      <motion.div
                        className={clsx(
                          'h-full rounded-full transition-colors',
                          getProgressColor(budget.status)
                        )}
                        initial={{ width: 0 }}
                        animate={{ width: `${Math.min(budget.percentage, 100)}%` }}
                        transition={{ duration: 0.8, delay: index * 0.05 }}
                        role="progressbar"
                        aria-valuenow={budget.percentage}
                        aria-valuemin={0}
                        aria-valuemax={100}
                        aria-label={`${budget.category}: ${budget.percentage.toFixed(1)}% terpakai`}
                      />
                    </div>
                  </div>

                  {/* Percentage Badge */}
                  <div
                    className={clsx(
                      'flex-shrink-0 px-2.5 py-1 rounded-full',
                      getStatusColor(budget.status)
                    )}
                  >
                    <span className="text-[10px] font-bold tabular-nums" aria-label={`${budget.percentage.toFixed(1)}% terpakai`}>
                      {budget.percentage.toFixed(0)}%
                    </span>
                  </div>
                </button>

                {/* Expanded Details */}
                <AnimatePresence>
                  {isExpanded && (
                    <motion.div
                      id={`budget-details-${budget.id}`}
                      initial={{ height: 0, opacity: 0 }}
                      animate={{ height: 'auto', opacity: 1 }}
                      exit={{ height: 0, opacity: 0 }}
                      transition={{ duration: 0.3 }}
                      className="overflow-hidden border-t border-border/50"
                    >
                      <div className="px-4 pb-4 pt-3 space-y-3">
                        {/* Details */}
                        <div className="grid grid-cols-2 gap-3">
                          <DetailItem
                            label="Batas Anggaran"
                            value={budget.limit}
                            currency={currency}
                          />
                          <DetailItem
                            label="Terpakai"
                            value={budget.spent}
                            currency={currency}
                          />
                          <DetailItem
                            label={budget.remaining >= 0 ? t('budgetRemaining') : t('budgetOver')}
                            value={Math.abs(budget.remaining)}
                            currency={currency}
                            valueColor={budget.remaining >= 0 ? 'text-primary' : 'text-destructive'}
                          />
                          <DetailItem
                            label="Persentase"
                            value={`${budget.percentage.toFixed(1)}%`}
                            currency=""
                            isPercentage
                          />
                        </div>

                        {/* Actions */}
                        <div className="flex gap-2 pt-2">
                          <button
                            className="flex-1 flex items-center justify-center gap-2 px-3 py-2 bg-primary text-primary-foreground rounded-lg text-[10px] font-bold hover:bg-primary/90 transition-colors focus:outline-none focus:ring-2 focus:ring-primary focus:ring-offset-2"
                            aria-label={`Edit anggaran ${budget.category}`}
                          >
                            <Edit className="h-3.5 w-3.5" />
                            Edit
                          </button>
                          <button
                            className="flex-1 flex items-center justify-center gap-2 px-3 py-2 bg-muted text-foreground rounded-lg text-[10px] font-bold hover:bg-muted/70 transition-colors focus:outline-none focus:ring-2 focus:ring-destructive focus:ring-offset-2"
                            aria-label={`Hapus anggaran ${budget.category}`}
                          >
                            <Trash2 className="h-3.5 w-3.5" />
                            Hapus
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

      {/* View All Link */}
      <div className="mt-6 pt-4 border-t border-border">
        <button
          className="w-full text-center text-xs font-bold text-primary hover:text-primary/80 transition-colors focus:outline-none focus:ring-2 focus:ring-primary focus:ring-inset rounded-lg py-2"
          aria-label={t('manageBudgets')}
        >
          {t('manageBudgets')}
        </button>
      </div>
    </div>
  );
}

interface SummaryCardProps {
  label: string;
  value: number;
  currency: string;
  color: string;
  textColor: string;
}

function SummaryCard({ label, value, currency, color, textColor }: SummaryCardProps) {
  return (
    <div className={clsx('p-3 rounded-lg', color)}>
      <p className="text-[10px] text-muted-foreground font-semibold tracking-wider mb-1">{label}</p>
      <p className={clsx('text-sm font-black tabular-nums', textColor)}>
        {currency} {value.toLocaleString('id-ID')}
      </p>
    </div>
  );
}

interface DetailItemProps {
  label: string;
  value: number | string;
  currency: string;
  valueColor?: string;
  isPercentage?: boolean;
}

function DetailItem({ label, value, currency, valueColor = 'text-foreground', isPercentage = false }: DetailItemProps) {
  return (
    <div>
      <p className="text-[10px] text-muted-foreground">{label}</p>
      <p className={clsx('text-xs font-bold tabular-nums', valueColor)}>
        {isPercentage ? value : `${currency} ${Number(value).toLocaleString('id-ID')}`}
      </p>
    </div>
  );
}
