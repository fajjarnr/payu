"use client";

import React from "react";
import { useTranslations } from "next-intl";
import { ArrowUpRight, ArrowDownRight } from "@/components/icons";
import VIPBadge from "@/components/personalization/VIPBadge";
import { Card, Col, Row, Skeleton, Space, Tag, Typography, theme } from "antd";
import { formatCurrency } from "@/lib/currency";
import { CARD_ART_GRADIENT } from "@/theme/tokens";

const { Title, Text } = Typography;

interface BalanceCardProps {
  balance: string | number;
  percentage?: number;
  income?: string | number;
  expense?: string | number;
  incomeChange?: number;
  expenseChange?: number;
  netWorth?: number;
  netWorthChange?: number;
  currency?: string;
  isLoading?: boolean;
}

export default function BalanceCard({
  balance,
  percentage,
  income,
  expense,
  incomeChange,
  expenseChange,
  netWorth,
  netWorthChange,
  currency = "Rp",
  isLoading = false,
}: BalanceCardProps) {
  const t = useTranslations("dashboard");
  const { token } = theme.useToken();
  const bcp47Locale = 'id-ID'; // Single-locale app (Indonesian only)

  if (isLoading) {
    return (
      <div data-testid="balance-card-skeleton">
        <Skeleton active title={false} paragraph={{ rows: 4 }} />
      </div>
    );
  }

  return (
    <Row gutter={[24, 24]} data-testid="balance-card">
      {/* Col 1: Primary Balance & Net Worth */}
      <Col xs={24} xl={10}>
        <Space orientation="vertical" size={24} style={{ width: "100%" }}>
          <Card data-testid="primary-balance-card" style={{ minHeight: 220 }}>
            <Space orientation="vertical" size={16} style={{ width: "100%" }}>
              <Row justify="space-between" align="top">
                <Space orientation="vertical" size={4}>
                  <Text
                    strong
                    type="secondary"
                    style={{
                      fontSize: 12,
                      letterSpacing: "0.1em",
                      textTransform: "uppercase",
                    }}
                  >
                    {t("primaryBalance")}
                  </Text>
                  <Text
                    type="secondary"
                    strong
                    style={{
                      fontSize: 12,
                      letterSpacing: "0.1em",
                      textTransform: "uppercase",
                    }}
                  >
                    {new Date().toLocaleDateString(bcp47Locale, {
                      day: "numeric",
                      month: "short",
                      year: "numeric",
                    })}
                  </Text>
                </Space>
                <Space size={8} align="center">
                  <VIPBadge size="sm" variant="badge" />
                  <div
                    aria-hidden="true"
                    style={{
                      width: 8,
                      height: 8,
                      borderRadius: "50%",
                      backgroundColor: "var(--ant-color-primary)",
                    }}
                  />
                </Space>
              </Row>

              <Space orientation="vertical" size={12}>
                <Title
                  level={2}
                  style={{
                    margin: 0,
                    fontVariantNumeric: "tabular-nums",
                    overflowWrap: "break-word",
                  }}
                >
                  {formatCurrency(balance, {
                    symbol: currency,
                    locale: bcp47Locale,
                  })}
                </Title>
                <Space size={12} align="center">
                  {percentage != null ? (
                    <Tag
                      color={percentage < 0 ? "error" : "success"}
                      variant="filled"
                      icon={
                        percentage < 0 ? (
                          <ArrowDownRight style={{ width: 14, height: 14 }} />
                        ) : (
                          <ArrowUpRight style={{ width: 14, height: 14 }} />
                        )
                      }
                    >
                      {percentage < 0 ? `${percentage}%` : `+${percentage}%`}
                    </Tag>
                  ) : (
                    <Text type="secondary" strong>
                      --
                    </Text>
                  )}
                  <Text
                    type="secondary"
                    strong
                    style={{
                      fontSize: 12,
                      letterSpacing: "0.1em",
                      textTransform: "uppercase",
                    }}
                  >
                    {t("growthFactor")}
                  </Text>
                </Space>
              </Space>
            </Space>
          </Card>

          <Card data-testid="net-worth-card" style={{ minHeight: 220 }}>
            <Space orientation="vertical" size={16} style={{ width: "100%" }}>
              <Row justify="space-between" align="middle">
                <Text
                  type="secondary"
                  strong
                  style={{
                    fontSize: 12,
                    letterSpacing: "0.1em",
                    textTransform: "uppercase",
                  }}
                >
                  {t("netWorth")}
                </Text>
                <div
                  style={{
                    width: 48,
                    height: 48,
                    borderRadius: 12,
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                    backgroundColor: "var(--ant-color-primary-bg)",
                    color: "var(--ant-color-primary-text)",
                    border: "1px solid var(--ant-color-primary-border)",
                  }}
                >
                  <ArrowUpRight style={{ width: 24, height: 24 }} />
                </div>
              </Row>

              <Space orientation="vertical" size={12}>
                <Title
                  level={3}
                  style={{
                    margin: 0,
                    fontVariantNumeric: "tabular-nums",
                    overflowWrap: "break-word",
                  }}
                >
                  {formatCurrency(netWorth ?? 0, {
                    symbol: currency,
                    locale: bcp47Locale,
                  })}
                </Title>
                <Space size={8} align="center">
                  {netWorthChange != null ? (
                    // AA: success-text-active/error carry the 12px delta
                    // instead of the 2.00:1 primary fill color.
                    <Space size={4} align="center">
                      {netWorthChange < 0 ? (
                        <ArrowDownRight
                          style={{
                            width: 14,
                            height: 14,
                            color: "var(--ant-color-error)",
                          }}
                        />
                      ) : (
                        <ArrowUpRight
                          style={{
                            width: 14,
                            height: 14,
                            color: "var(--ant-color-success-text-active)",
                          }}
                        />
                      )}
                      <Text
                        strong
                        style={{
                          fontSize: 12,
                          color:
                            netWorthChange < 0
                              ? "var(--ant-color-error)"
                              : "var(--ant-color-success-text-active)",
                        }}
                      >
                        {netWorthChange < 0
                          ? `${netWorthChange}%`
                          : `+${netWorthChange}%`}
                      </Text>
                    </Space>
                  ) : (
                    <Text type="secondary" strong style={{ fontSize: 12 }}>
                      --
                    </Text>
                  )}
                  <Text
                    type="secondary"
                    strong
                    style={{
                      fontSize: 12,
                      letterSpacing: "0.1em",
                      textTransform: "uppercase",
                    }}
                  >
                    {t("totalGrowth")}
                  </Text>
                </Space>
              </Space>
            </Space>
          </Card>
        </Space>
      </Col>

      {/* Col 2: Visual Card Representation (supporting art) */}
      <Col xs={24} xl={6}>
        <Card
          style={{
            height: "100%",
            minHeight: 320,
            overflow: "hidden",
            position: "relative",
            border: "none",
            background: CARD_ART_GRADIENT,
          }}
          styles={{
            body: {
              padding: 24,
              height: "100%",
              minHeight: 272,
              display: "flex",
              flexDirection: "column",
              justifyContent: "space-between",
            },
          }}
        >
          <Row justify="space-between" align="top">
            <Space size={8} align="center">
              <div
                style={{
                  width: 48,
                  height: 48,
                  borderRadius: 12,
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center",
                  backgroundColor: "rgba(255, 255, 255, 0.2)",
                  border: "1px solid rgba(255, 255, 255, 0.2)",
                  color: "var(--ant-color-text-light-solid)",
                  fontSize: 24,
                  fontWeight: 700,
                }}
              >
                U
              </div>
              <Text
                strong
                style={{
                  fontSize: 24,
                  color: "var(--ant-color-text-light-solid)",
                }}
              >
                PayU
              </Text>
            </Space>
            <Text
              strong
              style={{
                fontSize: 12,
                letterSpacing: "0.1em",
                color: "var(--ant-color-text-light-solid)",
                backgroundColor: "rgba(255, 255, 255, 0.1)",
                border: "1px solid rgba(255, 255, 255, 0.1)",
                borderRadius: token.borderRadius,
                padding: "4px 12px",
              }}
            >
              07/28
            </Text>
          </Row>

          <Space orientation="vertical" size={16} style={{ width: "100%" }}>
            <Text
              strong
              style={{
                fontSize: 20,
                letterSpacing: "0.1em",
                fontFamily: "ui-monospace, monospace",
                color: "var(--ant-color-text-light-solid)",
              }}
            >
              4829 •••• •••• 1928
            </Text>
            <Row justify="space-between" align="bottom">
              <Space orientation="vertical" size={4} style={{ minWidth: 0 }}>
                <Text
                  strong
                  style={{
                    fontSize: 12,
                    letterSpacing: "0.1em",
                    textTransform: "uppercase",
                    color: "var(--ant-color-text-light-solid)",
                  }}
                >
                  {t("cardHolder")}
                </Text>
                <Text
                  strong
                  ellipsis
                  style={{
                    fontSize: 14,
                    letterSpacing: "0.1em",
                    textTransform: "uppercase",
                    color: "var(--ant-color-text-light-solid)",
                  }}
                >
                  {t("cardHolderName")}
                </Text>
              </Space>
              <div style={{ display: "flex", flexShrink: 0 }}>
                <div
                  style={{
                    width: 40,
                    height: 40,
                    borderRadius: "50%",
                    backgroundColor: "rgba(255, 255, 255, 0.45)",
                    border: "1px solid rgba(255, 255, 255, 0.2)",
                  }}
                />
                <div
                  style={{
                    width: 40,
                    height: 40,
                    borderRadius: "50%",
                    backgroundColor: "rgba(255, 255, 255, 0.25)",
                    border: "1px solid rgba(255, 255, 255, 0.2)",
                    marginLeft: -12,
                  }}
                />
              </div>
            </Row>
          </Space>
        </Card>
      </Col>

      {/* Col 3: Income / Expense summaries */}
      <Col xs={24} xl={8}>
        <Row gutter={[24, 24]}>
          <Col xs={12} xl={24}>
            <SummaryItem
              data-testid="income-card"
              label={t("income")}
              amount={income ?? 0}
              change={incomeChange ?? 0}
              isPositive={true}
              currency={currency}
              bcp47Locale={bcp47Locale}
            />
          </Col>
          <Col xs={12} xl={24}>
            <SummaryItem
              data-testid="expense-card"
              label={t("expense")}
              amount={expense ?? 0}
              change={expenseChange ?? 0}
              isPositive={false}
              currency={currency}
              bcp47Locale={bcp47Locale}
            />
          </Col>
        </Row>
      </Col>
    </Row>
  );
}

interface SummaryItemProps {
  label: string;
  amount: string | number;
  change: number;
  isPositive: boolean;
  currency: string;
  bcp47Locale: string;
  "data-testid"?: string;
}

function SummaryItem({
  label,
  amount,
  change,
  isPositive,
  currency,
  bcp47Locale,
  "data-testid": testId,
}: SummaryItemProps) {
  const t = useTranslations("dashboard");
  return (
    <Card data-testid={testId} style={{ height: "100%" }}>
      <Space orientation="vertical" size={16} style={{ width: "100%" }}>
        <Row justify="space-between" align="middle">
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
          <div
            style={{
              width: 48,
              height: 48,
              borderRadius: 12,
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              flexShrink: 0,
              backgroundColor: isPositive
                ? "var(--ant-color-success-bg)"
                : "var(--ant-color-error-bg)",
              color: isPositive
                ? "var(--ant-color-success-text)"
                : "var(--ant-color-error)",
            }}
          >
            {isPositive ? (
              <ArrowUpRight style={{ width: 24, height: 24 }} />
            ) : (
              <ArrowDownRight style={{ width: 24, height: 24 }} />
            )}
          </div>
        </Row>

        <Space orientation="vertical" size={8}>
          <Title
            level={4}
            style={{ margin: 0, fontVariantNumeric: "tabular-nums" }}
          >
            {formatCurrency(amount, { symbol: currency, locale: bcp47Locale })}
          </Title>
          <Space size={8} align="center">
            <Tag variant="filled" color={isPositive ? "success" : "error"}>
              {isPositive ? "+" : ""}
              {change}%
            </Tag>
            <Text
              type="secondary"
              strong
              style={{
                fontSize: 12,
                letterSpacing: "0.1em",
                textTransform: "uppercase",
              }}
            >
              {t("thisMonth")}
            </Text>
          </Space>
        </Space>
      </Space>
    </Card>
  );
}
