"use client";

import React from "react";
import {
  TrendingUp,
  AlertCircle,
  CheckCircle2,
  Info,
} from "@/components/icons";
import { useTranslations } from "next-intl";
import {
  Card,
  Col,
  Divider,
  Progress,
  Row,
  Skeleton,
  Space,
  Tag,
  Typography,
} from "antd";

const { Title, Text } = Typography;

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

type HealthTone = "success" | "primary" | "warning" | "error";

interface HealthLevel {
  label: string;
  description: string;
  tone: HealthTone;
  icon: React.ElementType;
}

const TONE_COLOR: Record<HealthTone, string> = {
  success: "var(--ant-color-success)",
  primary: "var(--ant-color-primary)",
  warning: "var(--ant-color-warning)",
  error: "var(--ant-color-error)",
};

const TONE_BG: Record<HealthTone, string> = {
  success: "var(--ant-color-success-bg)",
  primary: "var(--ant-color-primary-bg)",
  warning: "var(--ant-color-warning-bg)",
  error: "var(--ant-color-error-bg)",
};

const FACTOR_BAR: Record<string, string> = {
  "bg-primary": "var(--ant-color-primary)",
  "bg-primary-light": "var(--ant-color-primary-bg)",
};

const labelStyle: React.CSSProperties = {
  fontSize: 12,
  fontWeight: 700,
  color: "var(--ant-color-text-secondary)",
  textTransform: "uppercase",
  letterSpacing: "0.1em",
};

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
        className={className}
        style={{ position: "relative", overflow: "hidden" }}
      >
        <Space direction="vertical" size={16} style={{ width: "100%" }}>
          <Title level={3} id="financial-health-title" style={{ margin: 0 }}>
            {t("financialHealthScore")}
          </Title>
          <Skeleton active title={false} paragraph={false} style={{ minHeight: 280 }} />
        </Space>
      </Card>
    );
  }

  if (score == null) {
    return (
      <Card
        role="region"
        aria-labelledby="financial-health-title"
        className={className}
        style={{ position: "relative", overflow: "hidden" }}
      >
        <Space direction="vertical" size={16} style={{ width: "100%" }}>
          <Title level={3} id="financial-health-title" style={{ margin: 0 }}>
            {t("financialHealthScore")}
          </Title>
          <div
            style={{
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              minHeight: 200,
            }}
          >
            <Text type="secondary" strong style={labelStyle}>
              Belum ada data
            </Text>
          </div>
        </Space>
      </Card>
    );
  }

  const getHealthLevel = (score: number): HealthLevel => {
    if (score >= 85) {
      return {
        label: t("financialHealthExcellent"),
        description:
          "Kesehatan finansial Anda sangat baik. Pertahankan kebiasaan baik ini!",
        tone: "success",
        icon: CheckCircle2,
      };
    }
    if (score >= 70) {
      return {
        label: t("financialHealthGood"),
        description:
          "Kesehatan finansial Anda baik. Terus tingkatkan penghematan.",
        tone: "primary",
        icon: CheckCircle2,
      };
    }
    if (score >= 50) {
      return {
        label: t("financialHealthFair"),
        description:
          "Kesehatan finansial Anda cukup. Pertimbangkan untuk mengurangi pengeluaran.",
        tone: "warning",
        icon: Info,
      };
    }
    if (score >= 30) {
      return {
        label: t("financialHealthPoor"),
        description:
          "Kesehatan finansial Anda kurang. Segera tinjau kembali anggaran Anda.",
        tone: "error",
        icon: AlertCircle,
      };
    }
    return {
      label: t("financialHealthVeryPoor"),
      description:
        "Kesehatan finansial Anda sangat kurang. Prioritaskan perbaikan segera.",
      tone: "error",
      icon: AlertCircle,
    };
  };

  const healthLevel = getHealthLevel(score);
  const HealthIcon = healthLevel.icon;
  const scoreChange = previousScore != null ? score - previousScore : 0;
  const isImprovement = scoreChange > 0;

  const circumference = 2 * Math.PI * 54; // radius = 54
  const strokeDasharray = circumference;
  const strokeDashoffset = circumference - (score / 100) * circumference;

  return (
    <Card
      role="region"
      aria-labelledby="financial-health-title"
      className={className}
      style={{ position: "relative", overflow: "hidden" }}
    >
      {/* Decorative background glow */}
      <div
        style={{
          position: "absolute",
          top: 0,
          right: 0,
          width: 128,
          height: 128,
          backgroundColor: "var(--ant-color-primary-bg)",
          borderRadius: "50%",
          filter: "blur(48px)",
          pointerEvents: "none",
        }}
      />

      <Space
        direction="vertical"
        size={24}
        style={{ width: "100%", position: "relative" }}
      >
        <Row justify="space-between" align="top">
          <Space direction="vertical" size={4}>
            <Title level={3} id="financial-health-title" style={{ margin: 0 }}>
              {t("financialHealthScore")}
            </Title>
            <Text type="secondary" strong style={labelStyle}>
              Update terakhir:{" "}
              {new Date().toLocaleDateString(undefined, {
                day: "numeric",
                month: "short",
                year: "numeric",
              })}
            </Text>
          </Space>
          {previousScore != null && (
            <Tag
              icon={
                isImprovement ? (
                  <TrendingUp style={{ width: 16, height: 16 }} aria-hidden="true" />
                ) : (
                  <AlertCircle style={{ width: 16, height: 16 }} aria-hidden="true" />
                )
              }
              role="status"
              aria-live="polite"
              aria-label={`Skor berubah ${isImprovement ? "meningkat" : "menurun"} ${Math.abs(scoreChange)} poin`}
              style={{
                backgroundColor: isImprovement
                  ? "var(--ant-color-success-bg)"
                  : "var(--ant-color-error-bg)",
                color: isImprovement
                  ? "var(--ant-color-success)"
                  : "var(--ant-color-error)",
                border: "none",
                fontSize: 12,
                fontWeight: 700,
                padding: "8px 12px",
                borderRadius: 12,
              }}
            >
              {isImprovement ? "+" : ""}
              {scoreChange}
            </Tag>
          )}
        </Row>

        {/* Score Display with Circular Progress */}
        <Row gutter={[32, 32]} align="middle">
          <Col xs={24} xl={10}>
            <div
              style={{
                position: "relative",
                width: 192,
                height: 192,
                maxWidth: "100%",
                margin: "0 auto",
              }}
            >
              <svg
                style={{ width: "100%", height: "100%", transform: "rotate(-90deg)" }}
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
                  stroke="var(--ant-color-fill-secondary)"
                  strokeWidth="6"
                />
                <circle
                  cx="60"
                  cy="60"
                  r="54"
                  fill="none"
                  stroke={
                    score >= 70
                      ? "var(--ant-color-success)"
                      : score >= 50
                        ? "var(--ant-color-warning)"
                        : "var(--ant-color-error)"
                  }
                  strokeWidth="10"
                  strokeLinecap="round"
                  strokeDasharray={strokeDasharray}
                  strokeDashoffset={strokeDashoffset}
                />
              </svg>

              <div
                style={{
                  position: "absolute",
                  inset: 0,
                  display: "flex",
                  flexDirection: "column",
                  alignItems: "center",
                  justifyContent: "center",
                }}
              >
                <Text
                  strong
                  style={{
                    fontSize: 30,
                    lineHeight: 1,
                    fontVariantNumeric: "tabular-nums",
                  }}
                >
                  {score}
                </Text>
                <Text type="secondary" strong style={{ ...labelStyle, marginTop: 4 }}>
                  dari 100
                </Text>
              </div>
            </div>
          </Col>

          <Col xs={24} xl={14}>
            <Space
              direction="vertical"
              size={16}
              style={{ width: "100%", textAlign: "center" }}
            >
              <div>
                <Tag
                  icon={
                    <HealthIcon
                      style={{ width: 20, height: 20 }}
                      aria-hidden="true"
                    />
                  }
                  style={{
                    backgroundColor: TONE_BG[healthLevel.tone],
                    color: TONE_COLOR[healthLevel.tone],
                    border: "none",
                    fontSize: 12,
                    fontWeight: 700,
                    textTransform: "uppercase",
                    letterSpacing: "0.1em",
                    padding: "8px 16px",
                    borderRadius: 12,
                  }}
                >
                  {healthLevel.label}
                </Tag>
              </div>
              <Text type="secondary" style={{ fontSize: 14 }}>
                {healthLevel.description}
              </Text>
            </Space>
          </Col>
        </Row>

        {/* Score Factors */}
        <Divider style={{ margin: 0 }} />
        <Row gutter={[24, 24]}>
          {factors && factors.length > 0 ? (
            factors.map((f) => (
              <Col xs={24} sm={8} key={f.label}>
                <ScoreFactor
                  label={f.label}
                  value={f.value}
                  color={f.color}
                  ariaLabel={`Faktor ${f.label}`}
                />
              </Col>
            ))
          ) : (
            <>
              <Col xs={24} sm={8}>
                <ScoreFactor
                  label="Tabungan"
                  value={0}
                  color="bg-primary"
                  ariaLabel="Faktor tabungan"
                />
              </Col>
              <Col xs={24} sm={8}>
                <ScoreFactor
                  label="Investasi"
                  value={0}
                  color="bg-primary"
                  ariaLabel="Faktor investasi"
                />
              </Col>
              <Col xs={24} sm={8}>
                <ScoreFactor
                  label="Pengeluaran"
                  value={0}
                  color="bg-primary-light"
                  ariaLabel="Faktor pengeluaran"
                />
              </Col>
            </>
          )}
        </Row>
      </Space>
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
    <Space direction="vertical" size={12} style={{ width: "100%", textAlign: "center" }}>
      <Progress
        percent={value}
        showInfo={false}
        strokeColor={FACTOR_BAR[color] ?? "var(--ant-color-primary)"}
        aria-label={ariaLabel}
      />
      <Space direction="vertical" size={4} style={{ width: "100%" }}>
        <Text type="secondary" strong style={labelStyle}>
          {label}
        </Text>
        <Text strong style={{ fontSize: 14, fontVariantNumeric: "tabular-nums" }}>
          {value}
        </Text>
      </Space>
    </Space>
  );
}
