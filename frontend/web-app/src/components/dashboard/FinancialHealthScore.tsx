'use client';

import React from 'react';
import { motion } from 'framer-motion';
import { TrendingUp, AlertCircle, CheckCircle2, Info } from 'lucide-react';
import { useTranslations } from 'next-intl';
import clsx from 'clsx';

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
  currency = 'Rp',
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
    <div
      className={clsx(
        'bg-card p-6 sm:p-8 rounded-xl border border-border shadow-card relative overflow-hidden',
        className
      )}
      role="region"
      aria-labelledby="financial-health-title"
      aria-describedby="financial-health-description"
    >
      {/* Decorative background gradient */}
      <div className="absolute top-0 right-0 w-32 h-32 bg-primary/5 rounded-full blur-3xl -translate-y-1/2 translate-x-1/2" />

      {/* Header */}
      <div className="flex justify-between items-start mb-6">
        <div>
          <h2
            id="financial-health-title"
            className="text-xs font-semibold text-muted-foreground tracking-widest mb-1"
          >
            {t('financialHealthScore')}
          </h2>
          <p className="text-[10px] text-muted-foreground">
            Update terakhir: {new Date().toLocaleDateString('id-ID', { day: 'numeric', month: 'short', year: 'numeric' })}
          </p>
        </div>
        {previousScore && (
          <motion.div
            initial={{ opacity: 0, y: -10 }}
            animate={{ opacity: 1, y: 0 }}
            className={clsx(
              'flex items-center gap-1 px-2.5 py-1 rounded-full text-[10px] font-bold',
              isImprovement ? 'bg-success-light text-primary' : 'bg-destructive/10 text-destructive'
            )}
            aria-label={`Skor berubah ${isImprovement ? 'meningkat' : 'menurun'} ${Math.abs(scoreChange)} poin`}
          >
            {isImprovement ? <TrendingUp className="h-3 w-3" /> : <AlertCircle className="h-3 w-3" />}
            {isImprovement ? '+' : ''}{scoreChange}
          </motion.div>
        )}
      </div>

      {/* Score Display with Circular Progress */}
      <div className="flex items-center gap-6 sm:gap-8 mb-6">
        <div className="relative w-32 h-32 sm:w-36 sm:h-36 flex-shrink-0">
          {/* Circular Progress */}
          <svg
            className="w-full h-full transform -rotate-90"
            viewBox="0 0 120 120"
            role="img"
            aria-label={`Skor kesehatan finansial ${score} dari 100`}
          >
            {/* Background circle */}
            <circle
              cx="60"
              cy="60"
              r="54"
              fill="none"
              stroke="hsl(var(--muted))"
              strokeWidth="8"
              className="opacity-20"
            />
            {/* Progress circle */}
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
              className="filter drop-shadow-lg"
            />
          </svg>

          {/* Score in center */}
          <div className="absolute inset-0 flex flex-col items-center justify-center">
            <motion.span
              initial={{ opacity: 0, scale: 0.5 }}
              animate={{ opacity: 1, scale: 1 }}
              transition={{ duration: 0.5, delay: 0.3 }}
              className="text-3xl sm:text-4xl font-black text-foreground tabular-nums"
            >
              {score}
            </motion.span>
            <span className="text-[10px] text-muted-foreground font-medium">dari 100</span>
          </div>
        </div>

        {/* Health Level Info */}
        <div className="flex-1 space-y-3">
          <div className={clsx('inline-flex items-center gap-2 px-3 py-1.5 rounded-lg', healthLevel.bgColor)}>
            <healthLevel.icon className={clsx('h-4 w-4', healthLevel.color)} />
            <span className={clsx('text-xs font-bold', healthLevel.color)}>
              {healthLevel.label}
            </span>
          </div>
          <p
            id="financial-health-description"
            className="text-xs text-muted-foreground leading-relaxed"
          >
            {healthLevel.description}
          </p>
        </div>
      </div>

      {/* Score Factors (Visual Indicators) */}
      <div className="grid grid-cols-3 gap-3 pt-4 border-t border-border">
        <ScoreFactor
          label="Tabungan"
          value={85}
          color="bg-primary"
          ariaLabel="Faktor tabungan"
        />
        <ScoreFactor
          label="Investasi"
          value={70}
          color="bg-chart-2"
          ariaLabel="Faktor investasi"
        />
        <ScoreFactor
          label="Pengeluaran"
          value={60}
          color="bg-chart-3"
          ariaLabel="Faktor pengeluaran"
        />
      </div>
    </div>
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
          className={clsx('h-full rounded-full', color)}
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
      <p className="text-[10px] text-muted-foreground font-medium">{label}</p>
      <p className="text-xs font-bold text-foreground tabular-nums">{value}</p>
    </div>
  );
}
