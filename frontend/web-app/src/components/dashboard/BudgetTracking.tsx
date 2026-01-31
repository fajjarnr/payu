'use client';

import React, { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { 
  Plus, 
  AlertTriangle, 
  CheckCircle2, 
  Edit, 
  Trash2,
  ChevronDown as ChevronDownIcon
} from 'lucide-react';
import { useTranslations } from 'next-intl';
import clsx from 'clsx';

import { cn } from '@/lib/utils';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { 
  Accordion, 
  AccordionContent, 
  AccordionItem, 
  AccordionTrigger 
} from '@/components/ui/accordion';
import { Progress } from '@/components/ui/progress';


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
  // Manual expansion state removed

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

      <CardHeader className="flex flex-row items-start justify-between space-y-0 pb-8 shrink-0">
        <div>
          <CardTitle className="text-base sm:text-lg font-bold text-foreground tracking-widest uppercase">
            {t('budgetTracking')}
          </CardTitle>
          <CardDescription className="text-xs sm:text-xs font-bold uppercase tracking-widest opacity-60 mt-1">
            {new Date().toLocaleDateString('id-ID', { month: 'long', year: 'numeric' })}
          </CardDescription>
        </div>

        <Button size="sm" className="hidden sm:flex gap-3 px-6 h-11 text-xs font-bold uppercase tracking-widest">
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
                <p className="text-xs text-muted-foreground">
                  {exceededCount > 0 ? 'Pertimbangkan untuk mengurangi pengeluaran' : 'Berhati-hatilah dengan pengeluaran'}
                </p>
              </div>
            </div>
          </motion.div>
        )}

        <Accordion type="single" collapsible className="space-y-3">
          {budgets.map((budget, index) => {
            const StatusIcon = getStatusIcon(budget.status);

            return (
              <AccordionItem 
                key={budget.id} 
                value={budget.id}
                className={cn(
                  'bg-muted/30 rounded-xl border-none overflow-hidden transition-all',
                  budget.status === 'exceeded' && 'ring-2 ring-destructive/10'
                )}
              >
                <AccordionTrigger className="hover:no-underline px-6 py-10 group/trigger">
                  <div className="flex items-center gap-6 w-full text-left">
                    {/* Status Icon */}
                    <div
                      className={cn(
                        'h-14 w-14 rounded-2xl flex items-center justify-center flex-shrink-0 shadow-lg',
                        getStatusColor(budget.status)
                      )}
                    >
                      <StatusIcon className="h-6 w-6" aria-hidden="true" />
                    </div>

                    {/* Category and Progress */}
                    <div className="flex-1 min-w-0 pr-6">
                      <div className="flex items-center justify-between mb-4">
                        <p className="text-sm sm:text-base font-bold text-foreground uppercase tracking-tight">{budget.category}</p>
                        <span className="text-xs text-muted-foreground tabular-nums font-bold opacity-60">
                          {budget.percentage.toFixed(0)}%
                        </span>
                      </div>

                      {/* Progress bar */}
                      <Progress 
                        value={Math.min(budget.percentage, 100)} 
                        className="h-3" 
                        indicatorClassName={getProgressColor(budget.status)}
                        aria-label={`${budget.category}: ${budget.percentage.toFixed(0)}%`}
                      />
                    </div>
                  </div>
                </AccordionTrigger>

                <AccordionContent className="px-5 pb-5 pt-0">
                  <div className="pt-4 border-t border-border/20 space-y-4">
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
                      <Button variant="outline" size="sm" className="flex-1 h-10 text-xs font-bold uppercase tracking-widest bg-muted/30">
                        <Edit className="h-3.5 w-3.5 mr-2" />
                        Edit
                      </Button>
                      <Button variant="outline" size="sm" className="flex-1 h-10 text-xs font-bold uppercase tracking-widest text-destructive hover:text-white hover:bg-destructive">
                        <Trash2 className="h-3.5 w-3.5 mr-2" />
                        Hapus
                      </Button>
                    </div>
                  </div>
                </AccordionContent>
              </AccordionItem>
            );
          })}
        </Accordion>

        <div className="mt-6 pt-4 border-t border-border">
          <Button variant="ghost" className="w-full text-xs font-bold uppercase tracking-widest text-primary">
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
    <div className={clsx('p-4 rounded-2xl border border-white/5 shadow-sm', color)}>
      <p className="text-xs font-bold uppercase tracking-widest opacity-60 mb-2">{label}</p>
      <p className={clsx('text-base sm:text-lg font-bold tabular-nums tracking-tight', textColor)}>
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
    <div className="space-y-1">
      <p className="text-xs text-muted-foreground font-bold uppercase tracking-wider opacity-60">{label}</p>
      <p className={clsx('text-sm font-bold tabular-nums tracking-tight', valueColor)}>
        {isPercentage ? value : `${currency} ${Number(value).toLocaleString('id-ID')}`}
      </p>
    </div>
  );
}
