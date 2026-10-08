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
import { ChevronDown } from "@/components/icons";
import { Button, Card, Col, Row, Space, Typography } from "antd";

const { Title, Text } = Typography;

const LEGEND_COLORS: Record<string, string> = {
  "bg-primary": "var(--ant-color-primary)",
  "bg-primary-light": "var(--ant-color-primary-bg)",
};

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
    <Space size={12} align="center">
      <div
        style={{
          width: 10,
          height: 10,
          borderRadius: "50%",
          backgroundColor: LEGEND_COLORS[color] ?? "var(--ant-color-primary)",
          flexShrink: 0,
        }}
      />
      <Space direction="vertical" size={0}>
        <Text
          type="secondary"
          strong
          style={{
            fontSize: 12,
            letterSpacing: "0.1em",
            textTransform: "uppercase",
          }}
        >
          {label}
        </Text>
        <Text strong style={{ fontSize: 12 }}>
          {percentage}
        </Text>
      </Space>
    </Space>
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
      <Row gutter={[24, 24]} className={className || undefined}>
        <Col xs={24} lg={10}>
          <Card style={{ minHeight: 300 }}>
            <Text
              type="secondary"
              strong
              style={{
                fontSize: 14,
                letterSpacing: "0.1em",
                textTransform: "uppercase",
              }}
            >
              {t("loading")}
            </Text>
          </Card>
        </Col>
        <Col xs={24} lg={14}>
          <Card style={{ minHeight: 300 }}>
            <Text
              type="secondary"
              strong
              style={{
                fontSize: 14,
                letterSpacing: "0.1em",
                textTransform: "uppercase",
              }}
            >
              {t("loading")}
            </Text>
          </Card>
        </Col>
      </Row>
    );
  }

  const spendingData = spdData.map((d) => ({
    month: d.month,
    amount: d.amount,
  }));
  const leadValue = invData[0]?.value ?? 0;

  return (
    <Row gutter={[24, 24]} className={className || undefined}>
      <Col xs={24} lg={10}>
        <Card>
          <Space direction="vertical" size={24} style={{ width: "100%" }}>
            <Row justify="space-between" align="middle">
              <Title level={3} style={{ margin: 0 }}>
                {t("perfTitle")}
              </Title>
              <Button
                type="default"
                size="small"
                aria-label={t("january2026")}
                style={{ minHeight: 44 }}
              >
                <Space size={8} align="center">
                  <Text
                    type="secondary"
                    strong
                    style={{
                      fontSize: 12,
                      letterSpacing: "0.1em",
                      textTransform: "uppercase",
                    }}
                  >
                    {t("january2026")}
                  </Text>
                  <ChevronDown style={{ width: 16, height: 16 }} />
                </Space>
              </Button>
            </Row>

            <Space direction="vertical" size={8}>
              <Text
                type="secondary"
                strong
                style={{
                  fontSize: 12,
                  letterSpacing: "0.1em",
                  textTransform: "uppercase",
                }}
              >
                {t("totalValue")}
              </Text>
              <Title level={4} style={{ margin: 0, fontVariantNumeric: "tabular-nums" }}>
                {displayTotal}
              </Title>
            </Space>

            <Row gutter={[24, 24]} align="middle">
              <Col xs={24} sm={12}>
                <Space direction="vertical" size={24}>
                  {legend.map((item) => (
                    <ChartLegend
                      key={item.label}
                      color={item.color}
                      label={item.label}
                      percentage={item.percentage}
                    />
                  ))}
                </Space>
              </Col>
              <Col xs={24} sm={12}>
                <div style={{ height: 256, width: "100%" }}>
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
              </Col>
            </Row>
          </Space>
        </Card>
      </Col>

      {/* Spending Overview (Column chart) */}
      <Col xs={24} lg={14}>
        <Card>
          <Space direction="vertical" size={16} style={{ width: "100%" }}>
            <Row justify="space-between" align="middle">
              <Title level={3} style={{ margin: 0 }}>
                {t("spendingOverview")}
              </Title>
              <Button
                type="default"
                size="small"
                aria-label={t("year2026")}
                style={{ minHeight: 44 }}
              >
                <Space size={8} align="center">
                  <Text
                    type="secondary"
                    strong
                    style={{
                      fontSize: 12,
                      letterSpacing: "0.1em",
                      textTransform: "uppercase",
                    }}
                  >
                    {t("year2026")}
                  </Text>
                  <ChevronDown style={{ width: 16, height: 16 }} />
                </Space>
              </Button>
            </Row>

            <div style={{ height: 320, width: "100%", marginTop: 16 }}>
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
          </Space>
        </Card>
      </Col>
    </Row>
  );
}
