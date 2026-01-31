'use client';

import React from 'react';
import { motion } from 'framer-motion';
import { TrendingUp, AlertCircle, CheckCircle2, Info } from 'lucide-react';
import { useTranslations } from 'next-intl';
import clsx from 'clsx';
import { cn } from '@/lib/utils';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';

interface FinancialHealthScoreProps {
  score: number;
  previousScore?: number;
  currency?: string;
  className?: string;
}

interface HealthLevel {
  label: string;
  description: string;
  color: string;
  bgColor: string;
  icon: React.ElementType;
}

export default function FinancialHealthScore({
  score,
  previousScore,
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  currency: _currency = 'Rp',
  className = '',
}: FinancialHealthScoreProps) {
  const t = useTranslations('dashboard');

  // Determine health level based on score
  const getHealthLevel = (score: number): HealthLevel => {
    if (score >= 85) {
      return {
        label: t('financialHealthExcellent'),
        description: 'Kesehatan finansial Anda sangat baik. Pertahankan kebiasaan baik ini!',
        color: 'text-emerald-600',
        bgColor: 'bg-emerald-50 dark:bg-emerald-950/30',
        icon: CheckCircle2,
      };
    }
    if (score >= 70) {
      return {
        label: t('financialHealthGood'),
        description: 'Kesehatan finansial Anda baik. Terus tingkatkan penghematan.',
        color: 'text-primary',
        bgColor: 'bg-success-light dark:bg-success-light/20',
        icon: CheckCircle2,
      };
    }
    if (score >= 50) {
      return {
        label: t('financialHealthFair'),
        description: 'Kesehatan finansial Anda cukup. Pertimbangkan untuk mengurangi pengeluaran.',
        color: 'text-yellow-600',
        bgColor: 'bg-yellow-50 dark:bg-yellow-950/30',
        icon: Info,
      };
    }
    if (score >= 30) {
      return {
        label: t('financialHealthPoor'),
        description: 'Kesehatan finansial Anda kurang. Segera tinjau kembali anggaran Anda.',
        color: 'text-orange-600',
        bgColor: 'bg-orange-50 dark:bg-orange-950/30',
        icon: AlertCircle,
      };
    }
    return {
      label: t('financialHealthVeryPoor'),
      description: 'Kesehatan finansial Anda sangat kurang. Prioritaskan perbaikan segera.',
      color: 'text-destructive',
      bgColor: 'bg-destructive/10',
      icon: AlertCircle,
    };
  };

  const healthLevel = getHealthLevel(score);
  const scoreChange = previousScore ? score - previousScore : 0;
  const isImprovement = scoreChange > 0;

  // Calculate stroke dasharray for circular progress
  const circumference = 2 * Math.PI * 54; // radius = 54
  const strokeDasharray = circumference;
  const strokeDashoffset = circumference - (score / 100) * circumference;

  return (
    <Card className={cn("relative overflow-hidden flex flex-col justify-between group", className)}>
      {/* Decorative background gradient */}
      <div className="absolute top-0 right-0 w-32 h-32 bg-primary/5 rounded-full blur-3xl -translate-y-1/2 translate-x-1/2 pointer-events-none" />

      <CardHeader className="flex flex-row items-start justify-between space-y-0 pb-6">
        <div>
          <CardTitle className="text-sm font-black text-foreground tracking-widest uppercase">
            {t('financialHealthScore')}
          </CardTitle>
          <CardDescription>
            Update terakhir: {new Date().toLocaleDateString('id-ID', { day: 'numeric', month: 'short', year: 'numeric' })}
          </CardDescription>
        </div>
        {previousScore && (
          <motion.div
            initial={{ opacity: 0, y: -10 }}
            animate={{ opacity: 1, y: 0 }}
            className={cn(
              'flex items-center gap-1 px-2.5 py-1 rounded-full text-[10px] font-bold',
              isImprovement ? 'bg-success-light text-primary' : 'bg-destructive/10 text-destructive'
            )}
          >
            {isImprovement ? <TrendingUp className="h-3 w-3" /> : <AlertCircle className="h-3 w-3" />}
            {isImprovement ? '+' : ''}{scoreChange}
          </motion.div>
        )}
      </CardHeader>

      <CardContent>
        {/* Score Display with Circular Progress */}
        <div className="flex items-center gap-6 sm:gap-8 mb-8">
          <div className="relative w-32 h-32 sm:w-36 sm:h-36 flex-shrink-0">
            {/* Circular Progress */}
            <svg
              className="w-full h-full transform -rotate-90"
              viewBox="0 0 120 120"
            >
              <circle
                cx="60"
                cy="60"
                r="54"
                fill="none"
                stroke="hsl(var(--muted))"
                strokeWidth="8"
                className="opacity-20"
              />
              <motion.circle
                cx="60"
                cy="60"
                r="54"
                fill="none"
                stroke={score >= 70 ? 'hsl(var(--primary))' : score >= 50 ? 'hsl(45, 93%, 47%)' : 'hsl(var(--destructive))'}
                strokeWidth="8"
                strokeLinecap="round"
                strokeDasharray={strokeDasharray}
                initial={{ strokeDashoffset: circumference }}
                animate={{ strokeDashoffset }}
                transition={{ duration: 1, ease: 'easeOut' }}
                className="filter drop-shadow-sm"
              />
            </svg>

            <div className="absolute inset-0 flex flex-col items-center justify-center">
              <motion.span
                initial={{ opacity: 0, scale: 0.5 }}
                animate={{ opacity: 1, scale: 1 }}
                transition={{ duration: 0.5, delay: 0.3 }}
                className="text-3xl sm:text-4xl font-black text-foreground tabular-nums"
              >
                {score}
              </motion.span>
              <span className="text-[10px] text-muted-foreground font-medium uppercase tracking-tighter">dari 100</span>
            </div>
          </div>

          <div className="flex-1 space-y-3">
            <div className={cn('inline-flex items-center gap-2 px-3 py-1.5 rounded-lg border border-transparent transition-all hover:border-current', healthLevel.bgColor)}>
              <healthLevel.icon className={cn('h-4 w-4', healthLevel.color)} />
              <span className={cn('text-[10px] font-black uppercase tracking-widest', healthLevel.color)}>
                {healthLevel.label}
              </span>
            </div>
            <p className="text-xs text-muted-foreground leading-relaxed font-medium">
              {healthLevel.description}
            </p>
          </div>
        </div>

        {/* Score Factors */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-6 pt-6 border-t border-border/50">
          <ScoreFactor label="Tabungan" value={85} color="bg-primary" ariaLabel="Faktor tabungan" />
          <ScoreFactor label="Investasi" value={70} color="bg-chart-2" ariaLabel="Faktor investasi" />
          <ScoreFactor label="Pengeluaran" value={60} color="bg-chart-3" ariaLabel="Faktor pengeluaran" />
        </div>
      </CardContent>
    </Card>
  );
}

interface ScoreFactorProps {
  label: string;
  value: number;
  color: string;
  ariaLabel: string;
}

function ScoreFactor({ label, value, color, ariaLabel }: ScoreFactorProps) {
  return (
    <div className="text-center">
      <div className="h-1.5 w-full bg-muted rounded-full mb-2 overflow-hidden">
        <motion.div
          className={cn('h-full rounded-full', color)}
          initial={{ width: 0 }}
          animate={{ width: `${value}%` }}
          transition={{ duration: 0.8, delay: 0.5 }}
          role="progressbar"
          aria-valuenow={value}
          aria-valuemin={0}
          aria-valuemax={100}
          aria-label={ariaLabel}
        />
      </div>
      <p className="text-[10px] text-muted-foreground font-black uppercase tracking-tight">{label}</p>
      <p className="text-xs font-black text-foreground tabular-nums">{value}</p>
    </div>
  );
}
