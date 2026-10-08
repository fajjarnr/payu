"use client";

import dynamic from "next/dynamic";
import React from "react";
import { useTranslations } from "next-intl";
const Column = dynamic(
  () => import("@ant-design/plots").then((m) => m.Column),
  { ssr: false },
);
const RadialBar = dynamic(
  () => import("@ant-design/plots").then((m) => m.RadialBar),
  { ssr: false },
);
import { cn } from "@/lib/utils";
import { ChevronDown } from "@/components/icons";
import { Card } from "antd";

function ChartLegend({
  color,
  label,
  percentage,
}: {
  color: string;
  label: string;
  percentage: string;
}) {
  return (
    <div className="flex items-center gap-3">
      <div className={cn("w-2.5 h-2.5 rounded-full", color)} />
      <div>
        <p className="text-xs text-muted-foreground font-bold tracking-widest uppercase">
          {label}
        </p>
        <p className="text-xs font-bold text-foreground">{percentage}</p>
      </div>
    </div>
  );
}

// Canvas-rendered charts cannot resolve CSS var() colors, so use literals.
// Primary matches the DESIGN.md primary token (#00D09C).
const PRIMARY = "#00D09C";

interface StatsChartsProps {
  className?: string;
  investmentChartData?: { category: string; value: number; fill: string }[];
  spendingChartData?: { month: string; amount: number }[];
  investmentLegend?: { color: string; label: string; percentage: string }[];
  totalValue?: string;
  isLoading?: boolean;
}

export default function StatsCharts({
  className = "",
  investmentChartData,
  spendingChartData,
  investmentLegend,
  totalValue,
  isLoading = false,
}: StatsChartsProps) {
  const t = useTranslations("investments");
  const invData = investmentChartData ?? [];
  const spdData = spendingChartData ?? [];
  const legend = investmentLegend ?? [
    { color: "bg-primary", label: t("legendStocks"), percentage: "--" },
    { color: "bg-primary", label: t("legendBonds"), percentage: "--" },
    { color: "bg-primary-light", label: t("legendGold"), percentage: "--" },
  ];
  const displayTotal = totalValue ?? "--";

  if (isLoading) {
    return (
      <div
        className={cn(
          "grid grid-cols-1 md:grid-cols-2 lg:grid-cols-12 gap-6 lg:gap-8",
          className,
        )}
      >
        <Card className="lg:col-span-5 flex items-center justify-center min-h-[300px]">
          <p className="text-sm text-muted-foreground font-bold uppercase tracking-widest">
            {t("loading")}
          </p>
        </Card>
        <Card className="lg:col-span-7 flex items-center justify-center min-h-[300px]">
          <p className="text-sm text-muted-foreground font-bold uppercase tracking-widest">
            {t("loading")}
          </p>
        </Card>
      </div>
    );
  }

  const spendingData = spdData.map((d) => ({
    month: d.month,
    amount: Number(d.amount),
  }));
  const leadValue = invData[0]?.value ?? 0;

  return (
    <div
      className={cn(
        "grid grid-cols-1 md:grid-cols-2 lg:grid-cols-12 gap-6 lg:gap-8",
        className,
      )}
    >
      <Card
        className="lg:col-span-5 relative overflow-hidden group"
        styles={{ body: { display: "contents" } }}
      >
        <div className="flex flex-row items-center justify-between p-6">
          <h3 className="text-2xl font-bold leading-none tracking-tight text-base sm:text-lg font-bold text-foreground tracking-widest uppercase">
            {t("perfTitle")}
          </h3>
          <button
            type="button"
            aria-label={t("january2026")}
            className="flex items-center gap-2 text-xs font-bold text-muted-foreground bg-muted/60 px-4 py-2 rounded-xl hover:bg-muted transition-colors uppercase tracking-widest shadow-sm min-h-[44px]"
          >
            {t("january2026")} <ChevronDown className="h-4 w-4" />
          </button>
        </div>

        <div className="p-6 pt-0 space-y-2">
          <p className="text-xs sm:text-xs text-muted-foreground font-bold tracking-widest uppercase">
            {t("totalValue")}
          </p>
          <h4 className="text-2xl sm:text-3xl font-bold text-foreground tabular-nums tracking-tight">
            {displayTotal}
          </h4>
        </div>

        <div className="p-6 pt-0 flex flex-col sm:flex-row items-center justify-between gap-6 lg:gap-8">
          <div className="space-y-6 w-full sm:w-auto">
            {legend.map((item) => (
              <ChartLegend
                key={item.label}
                color={item.color}
                label={item.label}
                percentage={item.percentage}
              />
            ))}
          </div>

          <div className="relative h-64 w-64 flex-shrink-0">
            <RadialBar
              data={invData}
              xField="category"
              yField="value"
              maxAngle={250}
              innerRadius={0.75}
              colorField="category"
              scale={{
                color: {
                  range:
                    invData.length > 0
                      ? invData.map((d) => d.fill || PRIMARY)
                      : [PRIMARY],
                },
              }}
              legend={false}
              tooltip={false}
              label={false}
              annotations={[
                {
                  type: "text",
                  style: {
                    text: `+${leadValue}%`,
                    x: "50%",
                    y: "46%",
                    textAlign: "center",
                    fontSize: 30,
                    fontWeight: 700,
                    fill: "#0f172a",
                  },
                },
                {
                  type: "text",
                  style: {
                    text: "Yield",
                    x: "50%",
                    y: "56%",
                    textAlign: "center",
                    fontSize: 11,
                    fontWeight: 700,
                    fill: "#6b7280",
                  },
                },
              ]}
            />
          </div>
        </div>
      </Card>

      {/* Spending Overview (Column chart) */}
      <Card
        className="lg:col-span-7 group overflow-hidden"
        styles={{ body: { display: "contents" } }}
      >
        <div className="flex flex-row items-center justify-between p-6">
          <h3 className="text-2xl font-bold leading-none tracking-tight text-xl font-bold text-foreground">
            {t("spendingOverview")}
          </h3>
          <button
            type="button"
            aria-label={t("year2026")}
            className="flex items-center gap-2 text-xs font-bold text-muted-foreground bg-muted/60 px-4 py-2 rounded-xl hover:bg-muted transition-colors uppercase tracking-widest shadow-sm min-h-[44px]"
          >
            {t("year2026")} <ChevronDown className="h-4 w-4" />
          </button>
        </div>

        <div className="p-6 pt-0">
          <div className="h-80 w-full mt-4">
            <Column
              data={spendingData}
              xField="month"
              yField="amount"
              style={{
                fill: PRIMARY,
                radiusTopLeft: 8,
                radiusTopRight: 8,
                maxWidth: 40,
              }}
              axis={{
                x: {
                  title: false,
                  labelFill: "#6b7280",
                  labelFontSize: 10,
                  labelFontWeight: 700,
                },
                y: false,
              }}
              label={false}
            />
          </div>
        </div>
      </Card>
    </div>
  );
}
