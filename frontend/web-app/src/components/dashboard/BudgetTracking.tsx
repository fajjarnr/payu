'use client';

import React, { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Plus, AlertTriangle, CheckCircle2, Edit, Trash2 } from 'lucide-react';
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

import { cn } from '@/lib/utils';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Button } from '@/components/ui/button';

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
      case 'exceeded':
        return AlertTriangle;
      default:
        return AlertTriangle;
    }
  };

  return (
    <Card className={cn("relative overflow-hidden h-full flex flex-col group", className)}>
      {/* Decorative background */}
      <div className="absolute top-1/2 right-0 w-48 h-48 bg-primary/5 rounded-full blur-3xl translate-x-1/2 -translate-y-1/2 pointer-events-none" />

      <CardHeader className="flex flex-row items-start justify-between space-y-0 pb-6 shrink-0">
        <div>
          <CardTitle className="text-sm font-black text-foreground tracking-widest uppercase">
            {t('budgetTracking')}
          </CardTitle>
          <CardDescription>
            {new Date().toLocaleDateString('id-ID', { month: 'long', year: 'numeric' })}
          </CardDescription>
        </div>

        <Button size="sm" className="hidden sm:flex gap-2">
          <Plus className="h-4 w-4" />
          <span>Tambah</span>
        </Button>
      </CardHeader>

      <CardContent className="flex-1 overflow-y-auto scrollbar-hide">
        {/* Summary Cards */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 mb-6">
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
                  className={cn(
                    'bg-muted/30 rounded-xl overflow-hidden transition-all border border-transparent',
                    budget.status === 'exceeded' && 'border-destructive/30 ring-2 ring-destructive/10',
                    isExpanded && 'bg-muted/50 border-border/50'
                  )}
                  role="listitem"
                >
                  <Button
                    variant="ghost"
                    onClick={() => setExpandedBudget(isExpanded ? null : budget.id)}
                    className={cn(
                      'w-full px-4 py-8 flex items-center gap-3 text-left transition-colors h-auto justify-start rounded-none',
                      isExpanded && 'bg-muted/50'
                    )}
                    aria-expanded={isExpanded}
                  >
                    {/* Status Icon */}
                    <div
                      className={cn(
                        'h-10 w-10 rounded-xl flex items-center justify-center flex-shrink-0',
                        getStatusColor(budget.status)
                      )}
                    >
                      <StatusIcon className="h-5 w-5" aria-hidden="true" />
                    </div>

                    {/* Category and Progress */}
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center justify-between mb-1.5">
                        <p className="text-xs font-black text-foreground uppercase tracking-tight">{budget.category}</p>
                        <span className="text-[10px] text-muted-foreground tabular-nums font-bold">
                          {budget.percentage.toFixed(0)}%
                        </span>
                      </div>

                      {/* Progress bar */}
                      <div className="relative h-2 w-full bg-muted rounded-full overflow-hidden">
                        <motion.div
                          className={cn(
                            'h-full rounded-full transition-colors',
                            getProgressColor(budget.status)
                          )}
                          initial={{ width: 0 }}
                          animate={{ width: `${Math.min(budget.percentage, 100)}%` }}
                          transition={{ duration: 0.8, delay: index * 0.05 }}
                          role="progressbar"
                        />
                      </div>
                    </div>
                  </Button>

                  <AnimatePresence>
                    {isExpanded && (
                      <motion.div
                        initial={{ height: 0, opacity: 0 }}
                        animate={{ height: 'auto', opacity: 1 }}
                        exit={{ height: 0, opacity: 0 }}
                        className="overflow-hidden border-t border-border/20 bg-muted/20"
                      >
                        <div className="px-5 pb-5 pt-4 space-y-4">
                          <div className="grid grid-cols-2 gap-4">
                            <DetailItem label="Batas Anggaran" value={budget.limit} currency={currency} />
                            <DetailItem label="Terpakai" value={budget.spent} currency={currency} />
                            <DetailItem 
                              label={budget.remaining >= 0 ? t('budgetRemaining') : t('budgetOver')} 
                              value={Math.abs(budget.remaining)} 
                              currency={currency}
                              valueColor={budget.remaining >= 0 ? 'text-primary' : 'text-destructive'}
                            />
                            <DetailItem label="Persentase" value={`${budget.percentage.toFixed(1)}%`} currency="" isPercentage />
                          </div>

                          <div className="flex gap-2">
                            <Button variant="outline" size="sm" className="flex-1">
                              <Edit className="h-3.5 w-3.5" />
                              Edit
                            </Button>
                            <Button variant="outline" size="sm" className="flex-1 text-destructive hover:text-destructive hover:bg-destructive/5">
                              <Trash2 className="h-3.5 w-3.5" />
                              Hapus
                            </Button>
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

        <div className="mt-6 pt-4 border-t border-border">
          <Button variant="ghost" className="w-full text-xs font-black uppercase tracking-widest text-primary">
            {t('manageBudgets')}
          </Button>
        </div>
      </CardContent>
    </Card>
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
