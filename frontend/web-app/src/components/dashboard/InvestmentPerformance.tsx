"use client";

import dynamic from "next/dynamic";
import * as React from "react";
import { useTranslations } from "next-intl";
const RadialBar = dynamic(
  () => import("@ant-design/plots").then((m) => m.RadialBar),
  { ssr: false },
);

import { Card, Col, Divider, Row, Skeleton, Space, Typography } from "antd";
import { TrendingUp, Target, ArrowUpRight } from "@/components/icons";

const { Title, Text } = Typography;

export const description = "Statistik performa investasi dalam format radial";

// Canvas-rendered charts cannot resolve CSS var() colors, so use literals.
// Primary matches the DESIGN.md primary token (#00D09C).
const PRIMARY = "#00D09C";

interface InvestmentPerformanceProps {
  className?: string;
  roi?: number;
  totalInvestment?: number;
  targetRoi?: number;
  profit?: number;
  monthlyChange?: number;
  isLoading?: boolean;
}

const labelStyle: React.CSSProperties = {
  fontSize: 12,
  fontWeight: 700,
  color: "var(--ant-color-text-secondary)",
  textTransform: "uppercase",
  letterSpacing: "0.1em",
};

export default function InvestmentPerformance({
  className,
  roi,
  totalInvestment,
  targetRoi,
  profit,
  monthlyChange,
  isLoading = false,
}: InvestmentPerformanceProps) {
  const t = useTranslations("investments");
  if (isLoading) {
    return (
      <Card className={className} style={{ height: "100%" }}>
        <Space direction="vertical" size={16} style={{ width: "100%" }}>
          <Title level={4} style={{ margin: 0 }}>
            {t("perfTitle")}
          </Title>
          <Skeleton
            active
            title={false}
            paragraph={false}
            style={{ minHeight: 220 }}
          />
        </Space>
      </Card>
    );
  }

  const displayRoi = roi ?? 0;
  const displayInvestment = totalInvestment ?? 0;
  const displayTarget = targetRoi ?? 0;
  const displayProfit = profit ?? 0;
  const displayMonthlyChange = monthlyChange ?? 0;

  // RadialBar full sweep maps to a 20% ROI scale for visualization.
  const maxRoi = 20;
  const fraction = Math.max(0, Math.min(1, displayRoi / maxRoi));

  return (
    <Card className={className} style={{ height: "100%" }}>
      <Space
        direction="vertical"
        size={16}
        style={{ width: "100%", height: "100%" }}
      >
        <Space direction="vertical" size={4} style={{ width: "100%" }}>
          <Space size={8}>
            <div
              style={{
                width: 32,
                height: 32,
                borderRadius: 8,
                backgroundColor: "var(--ant-color-primary-bg)",
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
              }}
            >
              <TrendingUp
                style={{ width: 16, height: 16, color: "var(--ant-color-primary)" }}
              />
            </div>
            <Title level={4} style={{ margin: 0 }}>
              {t("perfTitle")}
            </Title>
          </Space>
          <Text type="secondary" strong style={labelStyle}>
            {t("perfYield")}
          </Text>
        </Space>

        <div style={{ margin: "0 auto", maxHeight: 220, width: "100%" }}>
          <RadialBar
            data={[{ category: "return", value: fraction }]}
            xField="category"
            yField="value"
            colorField="category"
            scale={{
              color: { range: [PRIMARY] },
              y: { domain: [0, 1] },
            }}
            innerRadius={0.73}
            legend={false}
            tooltip={false}
            label={false}
            markBackground={{ style: { fill: "#e5e7eb", fillOpacity: 0.5 } }}
            annotations={[
              {
                type: "text",
                style: {
                  text: `+${displayRoi}%`,
                  x: "50%",
                  y: "46%",
                  textAlign: "center",
                  fontSize: 28,
                  fontWeight: 700,
                  fill: "#0f172a",
                },
              },
              {
                type: "text",
                style: {
                  text: t("annualRoi"),
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

        <Row gutter={[16, 16]}>
          <Col span={12}>
            <Card
              size="small"
              style={{ backgroundColor: "var(--ant-color-fill-tertiary)" }}
            >
              <Space direction="vertical" size={4}>
                <Space size={4}>
                  <Target style={{ width: 12, height: 12 }} />
                  <Text type="secondary" strong style={labelStyle}>
                    {t("target")}
                  </Text>
                </Space>
                <Text strong style={{ fontSize: 12 }}>
                  {displayTarget > 0 ? `${displayTarget}%` : "--"}
                </Text>
              </Space>
            </Card>
          </Col>
          <Col span={12}>
            <Card
              size="small"
              style={{ backgroundColor: "var(--ant-color-fill-tertiary)" }}
            >
              <Space direction="vertical" size={4}>
                <Space size={4}>
                  <ArrowUpRight style={{ width: 12, height: 12 }} />
                  <Text type="secondary" strong style={labelStyle}>
                    {t("profit")}
                  </Text>
                </Space>
                <Text
                  strong
                  style={{ fontSize: 12, color: "var(--ant-color-primary)" }}
                >
                  {displayProfit > 0
                    ? `Rp ${(displayProfit / 1000000).toFixed(2)}Jt`
                    : "Rp 0"}
                </Text>
              </Space>
            </Card>
          </Col>
        </Row>

        <Divider style={{ margin: 0 }} />
        <Space direction="vertical" size={8} align="center" style={{ width: "100%", textAlign: "center" }}>
          {displayMonthlyChange !== 0 && (
            <Space size={8}>
              <Text
                strong
                style={{
                  fontSize: 12,
                  textTransform: "uppercase",
                  letterSpacing: "0.1em",
                  color: "var(--ant-color-primary)",
                }}
              >
                {displayMonthlyChange > 0 ? t("up") : t("down")}{" "}
                {Math.abs(displayMonthlyChange)}% {t("thisMonth")}
              </Text>
              <TrendingUp style={{ width: 12, height: 12 }} />
            </Space>
          )}
          <Text type="secondary" style={{ fontSize: 12 }}>
            {displayInvestment > 0
              ? t("basedOnTotal", {
                  amount: `Rp ${(displayInvestment / 1000000).toFixed(0)}Jt`,
                })
              : t("noData")}
          </Text>
        </Space>
      </Space>
    </Card>
  );
}
