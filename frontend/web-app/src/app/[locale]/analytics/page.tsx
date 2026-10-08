"use client";

/* eslint-disable no-restricted-syntax -- display percentage uses Number for chart width, not Money arithmetic (ADR-0047 display only) */

import dynamic from "next/dynamic";
import React from "react";
import DashboardLayout from "@/components/DashboardLayout";
import { Link } from "@/lib/navigation";
import {
  TrendingUp,
  TrendingDown,
  Calendar,
  ArrowUpRight,
  Activity,
  Wifi,
  WifiOff,
} from "@/components/icons";
import { useAnalyticsWebSocket, useCashFlow, useSpendingTrends } from "@/hooks";
import { useAuthStore } from "@/stores";
const Column = dynamic(
  () => import("@ant-design/plots").then((m) => m.Column),
  { ssr: false },
);
const Pie = dynamic(() => import("@ant-design/plots").then((m) => m.Pie), {
  ssr: false,
});
import { Button, Card, Space, Row, Col, Typography, Tag, theme } from "antd";

export default function AnalyticsPage() {
  const accountId = useAuthStore((state) => state.accountId);
  const { token } = theme.useToken();
  // FE-AUDIT-006: analytics events are keyed by account_id (backend BUG-AUTH-013),
  // so queries must use accountId — Keycloak sub returns zero rows.
  const { analytics, isConnected } = useAnalyticsWebSocket(
    accountId || undefined,
  );
  const { data: cashFlow } = useCashFlow(accountId || undefined);
  const { data: trends } = useSpendingTrends(accountId || undefined);

  // REST baseline: the WS feed is enhancement-only (it may never connect —
  // no WS proxy exists in this environment), so seed the page from REST.
  // Live WS data takes precedence when present.
  const CATEGORY_COLORS = [
    "#00D09C",
    "#00D09C",
    "#f59e0b",
    "#8b5cf6",
    "#f43f5e",
    "#64748b",
  ];
  const restData =
    cashFlow || trends
      ? {
          totalIncome: cashFlow?.income ?? 0,
          totalExpenses: cashFlow?.expenses ?? trends?.totalSpending ?? 0,
          monthlySavings: cashFlow?.netCashFlow ?? 0,
          investmentRoi: 0,
          incomeChange: 0,
          expenseChange: trends?.monthOverMonthChange ?? 0,
          savingsChange: 0,
          roiChange: 0,
          spendingBreakdown: (
            trends?.categories ??
            cashFlow?.expensesByCategory ??
            []
          ).map((c, i) => ({
            label: c.category,
            amount: c.amount,
            percentage: c.percentage,
            color: CATEGORY_COLORS[i % CATEGORY_COLORS.length],
          })),
        }
      : null;

  // The hooks query a fixed 30-day window; label the window that was actually
  // queried instead of a hardcoded month name.
  const periodLabel = "30 hari terakhir";
  // BUG-FE-062: Replace hardcoded fallback data with zeros/empty state
  const analyticsData = analytics ??
    restData ?? {
      totalIncome: 0,
      totalExpenses: 0,
      monthlySavings: 0,
      investmentRoi: 0,
      incomeChange: 0,
      expenseChange: 0,
      savingsChange: 0,
      roiChange: 0,
      spendingBreakdown: [] as {
        label: string;
        amount: number;
        percentage: number;
        color: string;
      }[],
    };
  // ponytail: Money string HALF_EVEN 4 preferred, number legacy — chart coerces via Number()
  const trajectoryData: {
    day: string;
    masuk: number | string;
    keluar: number | string;
  }[] = (analytics?.trajectoryData ?? []) as {
    day: string;
    masuk: number | string;
    keluar: number | string;
  }[];

  const PIE_PALETTE = [
    "#00D09C",
    "#34d399",
    "#f59e0b",
    "#8b5cf6",
    "#f43f5e",
    "#64748b",
  ];
  const breakdownData = analyticsData.spendingBreakdown.map((cat, i) => ({
    name: cat.label,
    value: cat.amount,
    fill: PIE_PALETTE[i % PIE_PALETTE.length],
  }));

  // Canvas-rendered charts cannot resolve CSS var() colors, so use literals.
  // Primary matches the DESIGN.md primary token (#00D09C).
  const PRIMARY = "#00D09C";
  const TRACK = "#9ca3af";

  return (
    <DashboardLayout>
      <Space direction="vertical" size={16} style={{ width: "100%" }}>
        <Row justify="space-between" align="bottom">
          <div>
            <Typography.Title level={2} style={{ marginBottom: 0 }}>
              Intelijen Keuangan
            </Typography.Title>
            <Typography.Text type="secondary">
              Wawasan mendalam tentang kebiasaan pengeluaran dan pertumbuhan
              kekayaan Anda.
            </Typography.Text>
          </div>
          <Space size={16}>
            <Tag color={isConnected ? "success" : "default"}>
              {isConnected ? (
                <Wifi style={{ width: 16, height: 16 }} />
              ) : (
                <WifiOff style={{ width: 16, height: 16 }} />
              )}
              <Typography.Text strong>
                {isConnected ? "Live Update" : "Offline"}
              </Typography.Text>
            </Tag>
            <Tag bordered={false}>
              <Calendar style={{ width: 16, height: 16 }} /> {periodLabel}
            </Tag>
          </Space>
        </Row>

        <Row gutter={[16, 16]}>
          {[
            {
              label: "Total Pemasukan",
              amount: analyticsData.totalIncome,
              change: analyticsData.incomeChange,
              isPos: true,
              icon: TrendingUp,
            },
            {
              label: "Total Pengeluaran",
              amount: analyticsData.totalExpenses,
              change: analyticsData.expenseChange,
              isPos: false,
              icon: TrendingDown,
            },
            {
              label: "Tabungan Bulanan",
              amount: analyticsData.monthlySavings,
              change: analyticsData.savingsChange,
              isPos: true,
              icon: Activity,
            },
            {
              label: "ROI Investasi",
              amount: analyticsData.investmentRoi,
              change: analyticsData.roiChange,
              isPos: true,
              icon: ArrowUpRight,
            },
          ].map((stat, i) => (
            <Col xs={24} sm={12} lg={6} key={i}>
              <Card>
                <Row justify="space-between" align="top">
                  <div
                    style={{
                      width: 48,
                      height: 48,
                      borderRadius: 12,
                      display: "flex",
                      alignItems: "center",
                      justifyContent: "center",
                      border: `1px solid ${token.colorBorderSecondary}`,
                    }}
                  >
                    <stat.icon
                      style={{
                        width: 24,
                        height: 24,
                        color: stat.isPos
                          ? token.colorPrimary
                          : token.colorError,
                      }}
                    />
                  </div>
                  <Tag color={stat.isPos ? "success" : "error"}>
                    {stat.change > 0 ? "+" : ""}
                    {stat.change}%
                  </Tag>
                </Row>
                <Typography.Text
                  strong
                  style={{
                    fontSize: 12,
                    letterSpacing: "0.2em",
                    display: "block",
                    marginTop: 24,
                  }}
                >
                  {stat.label}
                </Typography.Text>
                <Typography.Title level={3} style={{ marginBottom: 0 }}>
                  Rp {Number(stat.amount).toLocaleString("id-ID")}
                </Typography.Title>
              </Card>
            </Col>
          ))}
        </Row>

        <Row gutter={[16, 16]}>
          <Col xs={24} md={12} lg={16}>
            <Card>
              <Row justify="space-between" align="middle">
                <div>
                  <Typography.Title level={4} style={{ marginBottom: 0 }}>
                    Trajektori Pengeluaran
                  </Typography.Title>
                  <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                    Analisis arus kas harian periode ini
                  </Typography.Text>
                </div>
                <Space size={16}>
                  <Tag color="success">
                    <div
                      style={{
                        width: 8,
                        height: 8,
                        borderRadius: "50%",
                        backgroundColor: token.colorPrimary,
                        display: "inline-block",
                        marginRight: 8,
                      }}
                    />
                    <Typography.Text strong style={{ fontSize: 12 }}>
                      Masuk
                    </Typography.Text>
                  </Tag>
                  <Tag>
                    <div
                      style={{
                        width: 8,
                        height: 8,
                        borderRadius: "50%",
                        backgroundColor: token.colorTextTertiary,
                        display: "inline-block",
                        marginRight: 8,
                      }}
                    />
                    <Typography.Text strong style={{ fontSize: 12 }}>
                      Keluar
                    </Typography.Text>
                  </Tag>
                </Space>
              </Row>

              <div style={{ height: 400, marginTop: 24 }}>
                <Column
                  data={trajectoryData.flatMap((d) => [
                    { day: d.day, type: "Masuk", amount: Number(d.masuk) },
                    { day: d.day, type: "Keluar", amount: Number(d.keluar) },
                  ])}
                  xField="day"
                  yField="amount"
                  colorField="type"
                  group={true}
                  scale={{ color: { range: [PRIMARY, TRACK] } }}
                  style={{
                    radiusTopLeft: 4,
                    radiusTopRight: 4,
                    maxWidth: 32,
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
            </Card>
          </Col>

          <Col xs={24} md={12} lg={8}>
            <Card>
              <Typography.Title level={4} style={{ marginBottom: 0 }}>
                Rincian Pengeluaran
              </Typography.Title>

              <div
                style={{
                  position: "relative",
                  aspectRatio: "1",
                  marginTop: 32,
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center",
                }}
              >
                <div style={{ width: "100%", height: "100%" }}>
                  <Pie
                    data={breakdownData.map((d) => ({
                      name: d.name,
                      value: Number(d.value),
                    }))}
                    angleField="value"
                    colorField="name"
                    scale={{
                      color: { range: breakdownData.map((d) => d.fill) },
                    }}
                    innerRadius={0.65}
                    style={{ lineWidth: 4, stroke: "#fff" }}
                    legend={false}
                    label={false}
                  />
                </div>
                <div
                  style={{
                    position: "absolute",
                    inset: 0,
                    display: "flex",
                    flexDirection: "column",
                    alignItems: "center",
                    justifyContent: "center",
                    pointerEvents: "none",
                  }}
                >
                  <Typography.Text
                    strong
                    style={{ fontSize: 12, letterSpacing: "0.1em" }}
                  >
                    Total Keluar
                  </Typography.Text>
                  <Typography.Title level={3} style={{ marginBottom: 0 }}>
                    Rp{" "}
                    {Number(analyticsData.totalExpenses).toLocaleString(
                      "id-ID",
                      { notation: "compact", compactDisplay: "short" },
                    )}
                  </Typography.Title>
                </div>
              </div>

              <Space direction="vertical" size={16} style={{ marginTop: 32 }}>
                {analyticsData.spendingBreakdown.map((cat, i) => (
                  <Row key={i} justify="space-between" align="middle">
                    <Space size={16}>
                      <div
                        style={{
                          width: 12,
                          height: 12,
                          borderRadius: "50%",
                          backgroundColor: cat.color,
                        }}
                      />
                      <Typography.Text
                        strong
                        style={{ fontSize: 12, letterSpacing: "0.1em" }}
                      >
                        {cat.label}
                      </Typography.Text>
                    </Space>
                    <div style={{ textAlign: "right" }}>
                      <Typography.Text strong style={{ fontSize: 12 }}>
                        Rp{" "}
                        {Number(cat.amount).toLocaleString("id-ID", {
                          notation: "compact",
                          compactDisplay: "short",
                        })}
                      </Typography.Text>
                      <Typography.Text
                        type="secondary"
                        style={{ fontSize: 12, marginLeft: 4 }}
                      >
                        ({cat.percentage}%)
                      </Typography.Text>
                    </div>
                  </Row>
                ))}
              </Space>
            </Card>
          </Col>
        </Row>

        <Card>
          <Row gutter={[16, 16]} align="middle" justify="space-between">
            <div>
              <Typography.Title level={3} style={{ marginBottom: 0 }}>
                Siap untuk menabung otomatis?
              </Typography.Title>
              <Typography.Text type="secondary">
                Sistem AI kami mendeteksi Anda dapat menabung tambahan{" "}
                <Typography.Text strong style={{ color: token.colorPrimary }}>
                  Rp 2.500.000
                </Typography.Text>{" "}
                setiap bulan dengan mengoptimalkan tagihan utilitas dan
                langganan berulang Anda.
              </Typography.Text>
            </div>
            <Link href="/transactions">
              <Button type="primary">Terapkan Optimasi</Button>
            </Link>
          </Row>
        </Card>
      </Space>
    </DashboardLayout>
  );
}
