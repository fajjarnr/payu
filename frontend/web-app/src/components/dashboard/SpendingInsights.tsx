"use client";

import React, { useState } from "react";

import { TrendingUp } from "@/components/icons";
import { useTranslations } from "next-intl";
import {
  Button,
  Card,
  Col,
  Collapse,
  Progress,
  Row,
  Skeleton,
  Space,
  Tag,
  Typography,
} from "antd";

const { Title, Text } = Typography;

interface SpendingCategory {
  id: string;
  name: string;
  icon: React.ElementType;
  amount: number;
  percentage: number;
  trend: "up" | "down" | "neutral";
  trendValue: number;
  color: string;
}

interface SpendingInsightsProps {
  data?: SpendingCategory[];
  currency?: string;
  className?: string;
  isLoading?: boolean;
}

const BERK_BG: Record<string, string> = {
  "bg-chart-1": "var(--ant-color-primary-bg)",
  "bg-chart-2": "var(--ant-color-success-bg)",
};

const labelStyle: React.CSSProperties = {
  fontSize: 12,
  fontWeight: 700,
  color: "var(--ant-color-text-secondary)",
  textTransform: "uppercase",
  letterSpacing: "0.1em",
};

export default function SpendingInsights({
  data,
  currency = "Rp",
  className = "",
  isLoading = false,
}: SpendingInsightsProps) {
  const t = useTranslations("dashboard");
  const [viewMode, setViewMode] = useState<"category" | "monthly">("category");

  const categories = data ?? [];
  const totalSpending = categories.reduce((sum, cat) => sum + cat.amount, 0);
  const highestCategory =
    categories.length > 0
      ? categories.reduce(
          (max, cat) => (cat.amount > max.amount ? cat : max),
          categories[0],
        )
      : null;

  const HighestIcon = highestCategory?.icon;

  return (
    <Card
      role="region"
      aria-labelledby="spending-insights-title"
      className={className}
      style={{ position: "relative", overflow: "hidden", height: "100%" }}
    >
      <div
        style={{
          position: "absolute",
          bottom: 0,
          left: 0,
          width: 160,
          height: 160,
          backgroundColor: "var(--ant-color-primary-bg)",
          borderRadius: "50%",
          filter: "blur(48px)",
          pointerEvents: "none",
        }}
      />

      <Space direction="vertical" size={24} style={{ width: "100%", position: "relative" }}>
        <Row justify="space-between" align="top">
          <Space direction="vertical" size={4}>
            <Title level={3} id="spending-insights-title" style={{ margin: 0 }}>
              {t("spendingInsights")}
            </Title>
            <Text type="secondary" strong style={labelStyle}>
              {new Date().toLocaleDateString(undefined, {
                month: "long",
                year: "numeric",
              })}
            </Text>
          </Space>

          <Space size={4}>
            <Button
              type={viewMode === "category" ? "primary" : "default"}
              onClick={() => setViewMode("category")}
              aria-label="Tampilan per kategori"
              aria-pressed={viewMode === "category"}
            >
              Kategori
            </Button>
            <Button
              type={viewMode === "monthly" ? "primary" : "default"}
              onClick={() => setViewMode("monthly")}
              aria-label="Tampilan bulanan"
              aria-pressed={viewMode === "monthly"}
            >
              Bulanan
            </Button>
          </Space>
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
        ) : categories.length === 0 ? (
          <div
            style={{
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              minHeight: 120,
            }}
          >
            <Text type="secondary" strong style={labelStyle}>
              Belum ada data pengeluaran
            </Text>
          </div>
        ) : (
          <Space direction="vertical" size={24} style={{ width: "100%" }}>
            {/* Summary */}
            <Row gutter={[16, 16]}>
              <Col xs={24} sm={12}>
                <Card
                  size="small"
                  style={{ backgroundColor: "var(--ant-color-fill-tertiary)" }}
                >
                  <Space direction="vertical" size={4}>
                    <Text type="secondary" strong style={labelStyle}>
                      Total Pengeluaran
                    </Text>
                    <Text
                      strong
                      style={{ fontSize: 20, fontVariantNumeric: "tabular-nums" }}
                    >
                      {currency} {totalSpending.toLocaleString("id-ID")}
                    </Text>
                  </Space>
                </Card>
              </Col>
              <Col xs={24} sm={12}>
                <Card
                  size="small"
                  style={{ backgroundColor: "var(--ant-color-fill-tertiary)" }}
                >
                  <Space direction="vertical" size={4}>
                    <Text type="secondary" strong style={labelStyle}>
                      Kategori Terbesar
                    </Text>
                    {highestCategory && (
                      <Space size={8}>
                        {HighestIcon && (
                          <HighestIcon
                            style={{
                              width: 16,
                              height: 16,
                              color: "var(--ant-color-primary)",
                            }}
                          />
                        )}
                        <Text strong>{highestCategory.name}</Text>
                      </Space>
                    )}
                    {highestCategory && (
                      <Text
                        type="secondary"
                        style={{ fontSize: 12, fontVariantNumeric: "tabular-nums" }}
                      >
                        {currency} {highestCategory.amount.toLocaleString("id-ID")}
                      </Text>
                    )}
                  </Space>
                </Card>
              </Col>
            </Row>
            {/* Category List with antd Accordion */}
            <Collapse
              accordion
              aria-label="Daftar kategori pengeluaran"
              expandIcon={() => null}
              items={categories.map((category) => {
                const Icon = category.icon;
                const panelId = `spending-panel-${category.id}`;
                return {
                  key: category.id,
                  style: {
                    backgroundColor: "var(--ant-color-fill-tertiary)",
                    borderRadius: 12,
                    border: "none",
                    overflow: "hidden",
                  },
                  label: (
                    <span
                      role="button"
                      aria-expanded={false}
                      aria-controls={panelId}
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
                          width: 40,
                          height: 40,
                          borderRadius: 16,
                          display: "flex",
                          alignItems: "center",
                          justifyContent: "center",
                          flexShrink: 0,
                          backgroundColor:
                            BERK_BG[category.color] ??
                            "var(--ant-color-primary-bg)",
                        }}
                      >
                        <Icon
                          style={{
                            width: 20,
                            height: 20,
                            color: "var(--ant-color-text-light-solid)",
                          }}
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
                            {category.name}
                          </Text>
                          <Text
                            strong
                            style={{
                              fontSize: 12,
                              fontVariantNumeric: "tabular-nums",
                            }}
                          >
                            {currency} {category.amount.toLocaleString("id-ID")}
                          </Text>
                        </Row>
                        <Progress
                          percent={category.percentage}
                          showInfo={false}
                          aria-label={`${category.name}: ${category.percentage}% dari total`}
                        />
                      </Space>
                      <Tag
                        aria-label={`Tren ${category.trend === "up" ? "naik" : category.trend === "down" ? "turun" : "tetap"} ${category.trendValue}%`}
                        icon={
                          category.trend !== "neutral" ? (
                            <TrendingUp
                              aria-hidden="true"
                              style={{
                                width: 12,
                                height: 12,
                                transform:
                                  category.trend === "down"
                                    ? "rotate(180deg)"
                                    : undefined,
                              }}
                            />
                          ) : undefined
                        }
                        style={{
                          backgroundColor:
                            category.trend === "up"
                              ? "var(--ant-color-error-bg)"
                              : category.trend === "down"
                                ? "var(--ant-color-success-bg)"
                                : "var(--ant-color-fill-secondary)",
                          color:
                            category.trend === "up"
                              ? "var(--ant-color-error)"
                              : category.trend === "down"
                                ? "var(--ant-color-success)"
                                : "var(--ant-color-text-secondary)",
                          border: "none",
                          fontSize: 12,
                          fontWeight: 700,
                          flexShrink: 0,
                        }}
                      >
                        {category.trendValue}%
                      </Tag>
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
                          borderTop: "1px solid var(--ant-color-border-secondary)",
                        }}
                      >
                        <Row gutter={[16, 16]}>
                          <Col span={12}>
                            <Space direction="vertical" size={4}>
                              <Text type="secondary" strong style={labelStyle}>
                                Persentase
                              </Text>
                              <Text strong style={{ fontSize: 12 }}>
                                {category.percentage}% dari total
                              </Text>
                            </Space>
                          </Col>
                          <Col span={12}>
                            <Space direction="vertical" size={4}>
                              <Text type="secondary" strong style={labelStyle}>
                                Status
                              </Text>
                              <Text
                                strong
                                style={{
                                  fontSize: 12,
                                  color:
                                    category.trend === "up"
                                      ? "var(--ant-color-error)"
                                      : "var(--ant-color-success)",
                                }}
                              >
                                {category.trend === "up" ? "Meningkat" : "Menurun"}
                              </Text>
                            </Space>
                          </Col>
                        </Row>
                        <Row gutter={[8, 8]}>
                          <Col span={12}>
                            <Button
                              aria-label={`Lihat transaksi ${category.name}`}
                              size="small"
                              type="primary"
                              block
                              style={{ minHeight: 44 }}
                            >
                              Lihat Transaksi
                            </Button>
                          </Col>
                          <Col span={12}>
                            <Button
                              aria-label={`Set anggaran ${category.name}`}
                              type="default"
                              size="small"
                              block
                              style={{ minHeight: 44 }}
                            >
                              Set Anggaran
                            </Button>
                          </Col>
                        </Row>
                      </Space>
                    </div>
                  ),
                };
              })}
            />
          </Space>
        )}
      </Space>
    </Card>
  );
}
