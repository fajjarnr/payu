"use client";

import React from "react";
import {
  TrendingUp,
  AlertCircle,
  CheckCircle2,
  Info,
} from "@/components/icons";
import { useTranslations } from "next-intl";
import { cn } from "@/lib/utils";
import { Card, Progress, Skeleton } from "antd";

interface ScoreFactorData {
  label: string;
  value: number;
  color: string;
}

interface FinancialHealthScoreProps {
  score?: number;
  previousScore?: number;
  factors?: ScoreFactorData[];
  currency?: string;
  className?: string;
  isLoading?: boolean;
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
  factors,

  currency: _currency = "Rp",
  className = "",
  isLoading = false,
}: FinancialHealthScoreProps) {
  const t = useTranslations("dashboard");

  if (isLoading) {
    return (
      <Card
        role="region"
        aria-labelledby="financial-health-title"
        className={cn(
          "relative overflow-hidden flex flex-col justify-between group",
          className,
        )}
        styles={{ body: { display: "contents" } }}
      >
        <div className="flex flex-col space-y-2 p-6">
          <h3
            id="financial-health-title"
            className="text-2xl font-bold leading-none tracking-tight text-base sm:text-lg font-bold text-foreground tracking-widest uppercase"
          >
            {t("financialHealthScore")}
          </h3>
        </div>
        <div className="p-6 pt-0">
          <Skeleton
            title={false}
            paragraph={false}
            className="block animate-pulse rounded-xl bg-muted/50 min-h-[280px] w-full"
          />
        </div>
      </Card>
    );
  }

  if (score == null) {
    return (
      <Card
        role="region"
        aria-labelledby="financial-health-title"
        className={cn(
          "relative overflow-hidden flex flex-col justify-between group",
          className,
        )}
        styles={{ body: { display: "contents" } }}
      >
        <div className="flex flex-col space-y-2 p-6">
          <h3
            id="financial-health-title"
            className="text-2xl font-bold leading-none tracking-tight text-base sm:text-lg font-bold text-foreground tracking-widest uppercase"
          >
            {t("financialHealthScore")}
          </h3>
        </div>
        <div className="p-6 pt-0 flex items-center justify-center min-h-[200px]">
          <p className="text-sm text-muted-foreground font-bold uppercase tracking-widest">
            Belum ada data
          </p>
        </div>
      </Card>
    );
  }

  const getHealthLevel = (score: number): HealthLevel => {
    if (score >= 85) {
      return {
        label: t("financialHealthExcellent"),
        description:
          "Kesehatan finansial Anda sangat baik. Pertahankan kebiasaan baik ini!",
        color: "text-primary-dark",
        bgColor: "bg-primary-light dark:bg-primary-dark/30",
        icon: CheckCircle2,
      };
    }
    if (score >= 70) {
      return {
        label: t("financialHealthGood"),
        description:
          "Kesehatan finansial Anda baik. Terus tingkatkan penghematan.",
        color: "text-primary",
        bgColor: "bg-success-light dark:bg-success-light/20",
        icon: CheckCircle2,
      };
    }
    if (score >= 50) {
      return {
        label: t("financialHealthFair"),
        description:
          "Kesehatan finansial Anda cukup. Pertimbangkan untuk mengurangi pengeluaran.",
        color: "text-secondary dark:text-warning",
        bgColor: "bg-warning dark:bg-warning/30",
        icon: Info,
      };
    }
    if (score >= 30) {
      return {
        label: t("financialHealthPoor"),
        description:
          "Kesehatan finansial Anda kurang. Segera tinjau kembali anggaran Anda.",
        color: "text-white dark:text-accent",
        bgColor: "bg-accent dark:bg-accent/30",
        icon: AlertCircle,
      };
    }
    return {
      label: t("financialHealthVeryPoor"),
      description:
        "Kesehatan finansial Anda sangat kurang. Prioritaskan perbaikan segera.",
      color: "text-destructive",
      bgColor: "bg-destructive/10",
      icon: AlertCircle,
    };
  };

  const healthLevel = getHealthLevel(score);
  const scoreChange = previousScore != null ? score - previousScore : 0;
  const isImprovement = scoreChange > 0;

  const circumference = 2 * Math.PI * 54; // radius = 54
  const strokeDasharray = circumference;
  const strokeDashoffset = circumference - (score / 100) * circumference;

  return (
    <Card
      role="region"
      aria-labelledby="financial-health-title"
      className={cn(
        "relative overflow-hidden flex flex-col justify-between group",
        className,
      )}
      styles={{ body: { display: "contents" } }}
    >
      {/* Decorative background gradient */}
      <div className="absolute top-0 right-0 w-32 h-32 bg-primary/5 rounded-full blur-3xl -translate-y-1/2 translate-x-1/2 pointer-events-none" />

      <div className="flex flex-row items-start justify-between gap-4 p-6 pb-6">
        <div>
          <h3
            id="financial-health-title"
            className="text-2xl font-bold leading-none tracking-tight text-base sm:text-lg font-bold text-foreground tracking-widest uppercase"
          >
            {t("financialHealthScore")}
          </h3>
          <p className="text-xs font-bold text-muted-foreground uppercase tracking-widest mt-1">
            Update terakhir:{" "}
            {new Date().toLocaleDateString(undefined, {
              day: "numeric",
              month: "short",
              year: "numeric",
            })}
          </p>
        </div>
        {previousScore != null && (
          <div
            className={cn(
              "flex items-center gap-2 px-3 py-2 rounded-xl text-xs font-bold shadow-sm",
              isImprovement
                ? "bg-primary/10 text-primary"
                : "bg-destructive/10 text-destructive",
            )}
            role="status"
            aria-live="polite"
            aria-label={`Skor berubah ${isImprovement ? "meningkat" : "menurun"} ${Math.abs(scoreChange)} poin`}
          >
            {isImprovement ? (
              <TrendingUp className="h-4 w-4" aria-hidden="true" />
            ) : (
              <AlertCircle className="h-4 w-4" aria-hidden="true" />
            )}
            {isImprovement ? "+" : ""}
            {scoreChange}
          </div>
        )}
      </div>

      <div className="p-6 pt-0">
        {/* Score Display with Circular Progress */}
        <div className="flex flex-col xl:flex-row items-center gap-6 lg:gap-8 mb-8">
          <div className="relative w-40 h-40 sm:w-48 sm:h-48 flex-shrink-0">
            {/* Circular Progress */}
            <svg
              className="w-full h-full transform -rotate-90"
              viewBox="0 0 120 120"
              role="progressbar"
              aria-label="Skor kesehatan finansial"
              aria-valuemin={0}
              aria-valuemax={100}
              aria-valuenow={score}
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
              <circle
                cx="60"
                cy="60"
                r="54"
                fill="none"
                stroke={
                  score >= 70
                    ? "hsl(var(--primary))"
                    : score >= 50
                      ? "hsl(var(--warning))"
                      : "hsl(var(--destructive))"
                }
                strokeWidth="10"
                strokeLinecap="round"
                strokeDasharray={strokeDasharray}
                strokeDashoffset={strokeDashoffset}
                className="filter drop-shadow-[0_0_8px_rgba(0,208,156,0.3)]"
              />
            </svg>

            <div className="absolute inset-0 flex flex-col items-center justify-center">
              <span className="text-3xl font-bold text-foreground tabular-nums tracking-tighter">
                {score}
              </span>
              <span className="text-xs sm:text-xs text-muted-foreground font-bold uppercase tracking-widest">
                dari 100
              </span>
            </div>
          </div>

          <div className="flex-1 space-y-4 text-center xl:text-left">
            <div
              className={cn(
                "inline-flex items-center gap-3 px-4 py-2 rounded-xl border border-transparent transition-all shadow-sm",
                healthLevel.bgColor,
              )}
            >
              <healthLevel.icon
                className={cn("h-5 w-5", healthLevel.color)}
                aria-hidden="true"
              />
              <span
                className={cn(
                  "text-xs font-bold uppercase tracking-[0.1em]",
                  healthLevel.color,
                )}
              >
                {healthLevel.label}
              </span>
            </div>
            <p className="text-sm sm:text-base text-muted-foreground leading-relaxed font-bold opacity-80 uppercase tracking-tight">
              {healthLevel.description}
            </p>
          </div>
        </div>

        {/* Score Factors */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-6 lg:gap-8 pt-6 border-t border-border/30">
          {factors && factors.length > 0 ? (
            factors.map((f) => (
              <ScoreFactor
                key={f.label}
                label={f.label}
                value={f.value}
                color={f.color}
                ariaLabel={`Faktor ${f.label}`}
              />
            ))
          ) : (
            <>
              <ScoreFactor
                label="Tabungan"
                value={0}
                color="bg-primary"
                ariaLabel="Faktor tabungan"
              />
              <ScoreFactor
                label="Investasi"
                value={0}
                color="bg-primary"
                ariaLabel="Faktor investasi"
              />
              <ScoreFactor
                label="Pengeluaran"
                value={0}
                color="bg-primary-light"
                ariaLabel="Faktor pengeluaran"
              />
            </>
          )}
        </div>
      </div>
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
        percent={value}
        showInfo={false}
        railColor="transparent"
        className="relative h-2 w-full overflow-hidden rounded-full bg-muted/50 h-2 w-full"
        classNames={{
          track: `bg-primary transition-all duration-500 ease-in-out ${color}`,
        }}
        styles={{
          body: { height: "100%" },
          rail: { height: "100%" },
          track: { height: "100%" },
        }}
        aria-label={ariaLabel}
      />
      <div>
        <p className="text-xs sm:text-xs text-muted-foreground font-bold uppercase tracking-widest mb-1">
          {label}
        </p>
        <p className="text-sm sm:text-base font-bold text-foreground tabular-nums tracking-tight">
          {value}
        </p>
      </div>
    </div>
  );
}
