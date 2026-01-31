'use client';

import React from 'react';
import { motion } from 'framer-motion';
import { TrendingUp, AlertCircle, CheckCircle2, Info } from 'lucide-react';
import { useTranslations } from 'next-intl';
import clsx from 'clsx';
import { cn } from '@/lib/utils';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Progress } from '@/components/ui/progress';

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

      <CardHeader className="flex flex-row items-start justify-between space-y-0 pb-8">
        <div>
          <CardTitle className="text-base sm:text-lg font-bold text-foreground tracking-widest uppercase">
            {t('financialHealthScore')}
          </CardTitle>
          <CardDescription className="text-xs sm:text-xs font-bold uppercase tracking-widest opacity-60 mt-1">
            Update terakhir: {new Date().toLocaleDateString('id-ID', { day: 'numeric', month: 'short', year: 'numeric' })}
          </CardDescription>
        </div>
        {previousScore && (
          <motion.div
            initial={{ opacity: 0, y: -10 }}
            animate={{ opacity: 1, y: 0 }}
            className={cn(
              'flex items-center gap-2 px-3 py-1.5 rounded-xl text-xs font-bold shadow-sm',
              isImprovement ? 'bg-emerald-500/10 text-emerald-500' : 'bg-destructive/10 text-destructive'
            )}
          >
            {isImprovement ? <TrendingUp className="h-4 w-4" /> : <AlertCircle className="h-4 w-4" />}
            {isImprovement ? '+' : ''}{scoreChange}
          </motion.div>
        )}
      </CardHeader>

      <CardContent>
        {/* Score Display with Circular Progress */}
        <div className="flex flex-col xl:flex-row items-center gap-10 mb-12">
          <div className="relative w-40 h-40 sm:w-48 sm:h-48 flex-shrink-0">
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
                strokeWidth="6"
                className="opacity-10"
              />
              <motion.circle
                cx="60"
                cy="60"
                r="54"
                fill="none"
                stroke={score >= 70 ? 'hsl(var(--primary))' : score >= 50 ? 'hsl(45, 93%, 47%)' : 'hsl(var(--destructive))'}
                strokeWidth="10"
                strokeLinecap="round"
                strokeDasharray={strokeDasharray}
                initial={{ strokeDashoffset: circumference }}
                animate={{ strokeDashoffset }}
                transition={{ duration: 1, ease: 'easeOut' }}
                className="filter drop-shadow-[0_0_8px_rgba(16,185,129,0.3)]"
              />
            </svg>

            <div className="absolute inset-0 flex flex-col items-center justify-center">
              <motion.span
                initial={{ opacity: 0, scale: 0.5 }}
                animate={{ opacity: 1, scale: 1 }}
                transition={{ duration: 0.5, delay: 0.3 }}
                className="text-4xl sm:text-4xl lg:text-6xl font-bold text-foreground tabular-nums tracking-tighter"
              >
                {score}
              </motion.span>
              <span className="text-xs sm:text-xs text-muted-foreground font-bold uppercase tracking-widest opacity-60">dari 100</span>
            </div>
          </div>

          <div className="flex-1 space-y-4 text-center xl:text-left">
            <div className={cn('inline-flex items-center gap-3 px-4 py-2 rounded-xl border border-transparent transition-all shadow-sm', healthLevel.bgColor)}>
              <healthLevel.icon className={cn('h-5 w-5', healthLevel.color)} />
              <span className={cn('text-xs font-bold uppercase tracking-[0.2em]', healthLevel.color)}>
                {healthLevel.label}
              </span>
            </div>
            <p className="text-sm sm:text-base text-muted-foreground leading-relaxed font-bold opacity-80 uppercase tracking-tight">
              {healthLevel.description}
            </p>
          </div>
        </div>

        {/* Score Factors */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-8 pt-10 border-t border-border/30">
          <ScoreFactor label="Tabungan" value={85} color="bg-emerald-500" ariaLabel="Faktor tabungan" />
          <ScoreFactor label="Investasi" value={70} color="bg-emerald-400" ariaLabel="Faktor investasi" />
          <ScoreFactor label="Pengeluaran" value={60} color="bg-emerald-300" ariaLabel="Faktor pengeluaran" />
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
    <div className="text-center space-y-3">
      <Progress 
        value={value} 
        className="h-2 w-full" 
        indicatorClassName={color}
        aria-label={ariaLabel}
      />
      <div>
        <p className="text-xs sm:text-xs text-muted-foreground font-bold uppercase tracking-widest opacity-60 mb-1">{label}</p>
        <p className="text-sm sm:text-base font-bold text-foreground tabular-nums tracking-tight">{value}</p>
      </div>
    </div>
  );
}
