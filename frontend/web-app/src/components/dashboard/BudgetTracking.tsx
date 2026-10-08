"use client";

/* eslint-disable no-restricted-syntax -- display percentage uses Number for chart width, not Money arithmetic (ADR-0047 display only) */

import React from "react";
import {
  Plus,
  AlertTriangle,
  CheckCircle2,
  Edit,
  Trash2,
} from "@/components/icons";
import { useTranslations, useLocale } from "next-intl";

import {
  Alert,
  Button,
  Card,
  Col,
  Collapse,
  Divider,
  Progress,
  Row,
  Skeleton,
  Space,
  Tag,
  Typography,
} from "antd";

const { Title, Text } = Typography;

interface Budget {
  id: string;
  category: string;
  limit: number;
  spent: number;
  remaining: number;
  percentage: number;
  status: "safe" | "warning" | "danger" | "exceeded";
  icon?: React.ElementType;
}

interface BudgetTrackingProps {
  budgets?: Budget[];
  currency?: string;
  className?: string;
  isLoading?: boolean;
}

type BudgetStatus = Budget["status"];

const STATUS_STYLE: Record<BudgetStatus, React.CSSProperties> = {
  safe: {
    backgroundColor: "var(--ant-color-success-bg)",
    color: "var(--ant-color-success)",
  },
  warning: {
    backgroundColor: "var(--ant-color-warning-bg)",
    color: "var(--ant-color-warning)",
  },
  danger: {
    backgroundColor: "var(--ant-color-error-bg)",
    color: "var(--ant-color-error)",
  },
  exceeded: {
    backgroundColor: "var(--ant-color-error-bg)",
    color: "var(--ant-color-error)",
  },
};

const PROGRESS_COLOR: Record<BudgetStatus, string> = {
  safe: "var(--ant-color-success)",
  warning: "var(--ant-color-warning)",
  danger: "var(--ant-color-error)",
  exceeded: "var(--ant-color-error)",
};

const labelStyle: React.CSSProperties = {
  fontSize: 12,
  fontWeight: 700,
  color: "var(--ant-color-text-secondary)",
  textTransform: "uppercase",
  letterSpacing: "0.1em",
};

export default function BudgetTracking({
  budgets,
  currency = "Rp",
  className = "",
  isLoading = false,
}: BudgetTrackingProps) {
  const t = useTranslations("dashboard");
  const locale = useLocale();
  // Manual expansion state removed

  const budgetList = budgets ?? [];
  const totalBudget = budgetList.reduce((sum, b) => sum + b.limit, 0);
  const totalSpent = budgetList.reduce((sum, b) => sum + b.spent, 0);
  const totalRemaining = totalBudget - totalSpent;

  const exceededCount = budgetList.filter(
    (b) => b.status === "exceeded",
  ).length;
  const warningCount = budgetList.filter((b) => b.status === "warning").length;

  const getStatusIcon = (status: Budget["status"]) => {
    switch (status) {
      case "safe":
        return CheckCircle2;
      case "warning":
      case "danger":
      case "exceeded":
        return AlertTriangle;
      default:
        return AlertTriangle;
    }
  };

  return (
    <Card
      role="region"
      aria-labelledby="budget-tracking-title"
      className={className}
      style={{ position: "relative", overflow: "hidden", height: "100%" }}
    >
      {/* Decorative background */}
      <div
        style={{
          position: "absolute",
          top: "50%",
          right: 0,
          width: 192,
          height: 192,
          backgroundColor: "var(--ant-color-primary-bg)",
          borderRadius: "50%",
          filter: "blur(48px)",
          transform: "translate(50%, -50%)",
          pointerEvents: "none",
        }}
      />

      <Space direction="vertical" size={24} style={{ width: "100%", position: "relative" }}>
        <Row justify="space-between" align="top">
          <Space direction="vertical" size={4}>
            <Title level={3} id="budget-tracking-title" style={{ margin: 0 }}>
              {t("budgetTracking")}
            </Title>
            <Text type="secondary" strong style={labelStyle}>
              {new Date().toLocaleDateString(undefined, {
                month: "long",
                year: "numeric",
              })}
            </Text>
          </Space>

          <Button
            aria-label="Tambah anggaran"
            size="small"
            type="primary"
            icon={<Plus style={{ width: 16, height: 16 }} />}
          >
            Tambah
          </Button>
        </Row>

        {isLoading ? (
          <Space direction="vertical" size={16} style={{ width: "100%" }}>
            {[1, 2, 3].map((i) => (
              <Skeleton
                key={i}
                active
                title={false}
                paragraph={false}
                style={{ height: 64 }}
              />
            ))}
          </Space>
        ) : budgetList.length === 0 ? (
          <div
            style={{
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              minHeight: 120,
            }}
          >
            <Text type="secondary" strong style={labelStyle}>
              Belum ada anggaran
            </Text>
          </div>
        ) : (
          <Space direction="vertical" size={24} style={{ width: "100%" }}>
            {/* Summary Cards */}
            <Row gutter={[12, 12]}>
              <Col xs={24} sm={8}>
                <SummaryCard
                  label="Total Anggaran"
                  value={totalBudget}
                  currency={currency}
                  locale={locale}
                  backgroundColor="var(--ant-color-success-bg)"
                  color="var(--ant-color-primary)"
                />
              </Col>
              <Col xs={24} sm={8}>
                <SummaryCard
                  label="Terpakai"
                  value={totalSpent}
                  currency={currency}
                  locale={locale}
                  backgroundColor="var(--ant-color-fill-tertiary)"
                  color="var(--ant-color-text)"
                />
              </Col>
              <Col xs={24} sm={8}>
                <SummaryCard
                  label="Sisa"
                  value={totalRemaining}
                  currency={currency}
                  locale={locale}
                  backgroundColor={
                    totalRemaining >= 0
                      ? "var(--ant-color-success-bg)"
                      : "var(--ant-color-error-bg)"
                  }
                  color={
                    totalRemaining >= 0
                      ? "var(--ant-color-primary)"
                      : "var(--ant-color-error)"
                  }
                />
              </Col>
            </Row>

            {/* Alerts */}
            {(exceededCount > 0 || warningCount > 0) && (
              <Alert
                type="error"
                role="alert"
                aria-live="polite"
                showIcon
                icon={
                  <AlertTriangle
                    style={{ width: 16, height: 16 }}
                    aria-hidden="true"
                  />
                }
                message={
                  <Text strong style={{ fontSize: 12 }}>
                    {exceededCount > 0 && warningCount > 0
                      ? `${exceededCount} anggaran terlampaui dan ${warningCount} hampir habis`
                      : exceededCount > 0
                        ? `${exceededCount} anggaran terlampaui`
                        : `${warningCount} anggaran hampir habis`}
                  </Text>
                }
                description={
                  <Text type="secondary" style={{ fontSize: 12 }}>
                    {exceededCount > 0
                      ? "Pertimbangkan untuk mengurangi pengeluaran"
                      : "Berhati-hatilah dengan pengeluaran"}
                  </Text>
                }
              />
            )}

            <Collapse
              accordion
              expandIcon={() => null}
              items={budgetList.map((budget) => {
                const StatusIcon = getStatusIcon(budget.status);
                const panelId = `budget-panel-${budget.id}`;
                return {
                  key: budget.id,
                  style: {
                    backgroundColor: "var(--ant-color-fill-tertiary)",
                    borderRadius: 12,
                    border:
                      budget.status === "exceeded"
                        ? "2px solid var(--ant-color-error-border)"
                        : "none",
                    overflow: "hidden",
                  },
                  label: (
                    <span
                      role="button"
                      aria-expanded={false}
                      aria-controls={panelId}
                      aria-label={`${budget.category}, status ${budget.status}, ${budget.percentage}%`}
                      style={{
                        display: "flex",
                        flex: 1,
                        alignItems: "center",
                        justifyContent: "space-between",
                        padding: "16px",
                        fontSize: 12,
                        fontWeight: 700,
                        textTransform: "uppercase",
                        letterSpacing: "0.1em",
                      }}
                    >
                    <Space size={16} style={{ width: "100%", padding: "8px 0" }}>
                      <div
                        style={{
                          ...STATUS_STYLE[budget.status],
                          width: 40,
                          height: 40,
                          borderRadius: 16,
                          display: "flex",
                          alignItems: "center",
                          justifyContent: "center",
                          flexShrink: 0,
                        }}
                      >
                        <StatusIcon
                          style={{ width: 20, height: 20 }}
                          aria-hidden="true"
                        />
                      </div>
                      <Space
                        direction="vertical"
                        size={8}
                        style={{ flex: 1, minWidth: 0 }}
                      >
                        <Row justify="space-between" align="middle">
                          <Text strong style={{ fontSize: 14 }}>
                            {budget.category}
                          </Text>
                          <Tag
                            style={{
                              fontSize: 12,
                              fontWeight: 700,
                              fontVariantNumeric: "tabular-nums",
                              border: "none",
                              background: "none",
                              margin: 0,
                              padding: 0,
                              color: "var(--ant-color-text-secondary)",
                            }}
                          >
                            {budget.percentage.toFixed(1)}%
                          </Tag>
                        </Row>
                        <Progress
                          percent={Math.min(budget.percentage, 100)}
                          showInfo={false}
                          strokeColor={PROGRESS_COLOR[budget.status]}
                          aria-label={`${budget.category}: ${budget.percentage}%`}
                          aria-valuetext={`${budget.percentage}% (${budget.status})`}
                        />
                      </Space>
                      </Space>
                    </span>
                  ),
                  children: (
                    <div id={panelId}>
                      <Space
                        direction="vertical"
                        size={16}
                        style={{
                          width: "100%",
                          paddingTop: 16,
                          borderTop:
                            "1px solid var(--ant-color-border-secondary)",
                        }}
                      >
                        <Row gutter={[16, 16]}>
                          <Col span={12}>
                            <DetailItem
                              label="Batas Anggaran"
                              value={budget.limit}
                              currency={currency}
                              locale={locale}
                            />
                          </Col>
                          <Col span={12}>
                            <DetailItem
                              label="Terpakai"
                              value={budget.spent}
                              currency={currency}
                              locale={locale}
                            />
                          </Col>
                          <Col span={12}>
                            <DetailItem
                              label={
                                budget.remaining >= 0
                                  ? t("budgetRemaining")
                                  : t("budgetOver")
                              }
                              value={Math.abs(budget.remaining)}
                              currency={currency}
                              locale={locale}
                              color={
                                budget.remaining >= 0
                                  ? "var(--ant-color-primary)"
                                  : "var(--ant-color-error)"
                              }
                            />
                          </Col>
                          <Col span={12}>
                            <DetailItem
                              label="Persentase"
                              value={`${budget.percentage.toFixed(1)}%`}
                              currency=""
                              locale={locale}
                              isPercentage
                            />
                          </Col>
                        </Row>
                        <Row gutter={[8, 8]}>
                          <Col span={12}>
                            <Button
                              aria-label={`Edit anggaran ${budget.category}`}
                              type="default"
                              size="small"
                              block
                              icon={
                                <Edit style={{ width: 14, height: 14 }} />
                              }
                              style={{ minHeight: 44 }}
                            >
                              Edit
                            </Button>
                          </Col>
                          <Col span={12}>
                            <Button
                              aria-label={`Hapus anggaran ${budget.category}`}
                              type="default"
                              size="small"
                              danger
                              block
                              icon={
                                <Trash2 style={{ width: 14, height: 14 }} />
                              }
                              style={{ minHeight: 44 }}
                            >
                              Hapus
                            </Button>
                          </Col>
                        </Row>
                      </Space>
                    </div>
                  ),
                };
              })}
            />

            <Divider style={{ margin: 0 }} />
            <Button
              aria-label="Kelola anggaran"
              type="text"
              block
              style={{ color: "var(--ant-color-primary)" }}
            >
              {t("manageBudgets")}
            </Button>
          </Space>
        )}
      </Space>
    </Card>
  );
}

interface SummaryCardProps {
  label: string;
  value: number;
  currency: string;
  locale: string;
  backgroundColor: string;
  color: string;
}

function SummaryCard({
  label,
  value,
  currency,
  locale,
  backgroundColor,
  color,
}: SummaryCardProps) {
  return (
    <Card size="small" style={{ backgroundColor }}>
      <Space direction="vertical" size={8}>
        <Text strong style={{ ...labelStyle, color }}>
          {label}
        </Text>
        <Text
          strong
          style={{
            fontSize: 18,
            fontVariantNumeric: "tabular-nums",
            color,
          }}
        >
          {currency} {value.toLocaleString(locale)}
        </Text>
      </Space>
    </Card>
  );
}

interface DetailItemProps {
  label: string;
  value: number | string;
  currency: string;
  locale: string;
  color?: string;
  isPercentage?: boolean;
}

function DetailItem({
  label,
  value,
  currency,
  locale,
  color = "var(--ant-color-text)",
  isPercentage = false,
}: DetailItemProps) {
  return (
    <Space direction="vertical" size={4}>
      <Text type="secondary" strong style={labelStyle}>
        {label}
      </Text>
      <Text strong style={{ fontSize: 14, fontVariantNumeric: "tabular-nums", color }}>
        {isPercentage
          ? value
          : `${currency} ${Number(value).toLocaleString(locale)}`}
      </Text>
    </Space>
  );
}
