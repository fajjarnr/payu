"use client";

import { useTranslations } from "next-intl";
import { ArrowRightLeft, Plus, QrCode, Wallet, Zap } from "@/components/icons";
import { Button, Card, Col, Flex, Input, Row, Typography } from "antd";
import { useState } from "react";

const { Text } = Typography;

const QUICK_ACTIONS = [
  { icon: QrCode, label: "QRIS" },
  { icon: Zap, label: "Kirim" },
  { icon: Plus, label: "Top Up" },
  { icon: Wallet, label: "Pocket" },
];

const DEMO_TRANSACTIONS = [
  { name: "QRIS Merchant", amount: "-Rp45.000", positive: false },
  { name: "Top Up Pocket", amount: "+Rp500.000", positive: true },
  { name: "Transfer", amount: "-Rp120.000", positive: false },
];

/**
 * Wise `currency-converter-card` on the hero band: white card, 1px ink
 * border, 24px radius. The ONLY interactive leaf in the hero — amount
 * inputs are demo-local state; the real converter lives on /exchange.
 */
export function ConverterCard() {
  const t = useTranslations("landing");
  const [fromAmount, setFromAmount] = useState("1000000");
  const [toAmount, setToAmount] = useState("62.50");

  return (
    <Card
      aria-label={t("slide2.connectivity.title")}
      style={{
        border: "1px solid var(--ant-color-text)",
        borderRadius: 24,
        boxShadow: "none",
      }}
      styles={{ body: { padding: 24 } }}
    >
      <Flex vertical gap={12}>
        <div>
          <Text strong style={{ display: "block", fontSize: 12 }}>
            {t("slide2.connectivity.title")}
          </Text>
          <Text type="secondary" style={{ fontSize: 12 }}>
            {t("slide2.connectivity.desc")}
          </Text>
        </div>
        <div>
          <label
            htmlFor="landing-from-amount"
            style={{ display: "block", fontSize: 12, fontWeight: 600, marginBottom: 4 }}
          >
            IDR
          </label>
          <Input
            id="landing-from-amount"
            inputMode="decimal"
            value={fromAmount}
            onChange={(e) => setFromAmount(e.target.value)}
            suffix={<Text type="secondary">IDR</Text>}
          />
        </div>
        <Flex justify="center" aria-hidden="true">
          <span
            style={{
              display: "inline-flex",
              alignItems: "center",
              justifyContent: "center",
              width: 44,
              height: 44,
              borderRadius: "50%",
              background: "var(--ant-color-bg-layout)",
            }}
          >
            <ArrowRightLeft style={{ fontSize: 18 }} />
          </span>
        </Flex>
        <div>
          <label
            htmlFor="landing-to-amount"
            style={{ display: "block", fontSize: 12, fontWeight: 600, marginBottom: 4 }}
          >
            USD
          </label>
          <Input
            id="landing-to-amount"
            inputMode="decimal"
            value={toAmount}
            onChange={(e) => setToAmount(e.target.value)}
            suffix={<Text type="secondary">USD</Text>}
          />
        </div>
        <Button type="primary" size="large" block href="/exchange">
          {t("getStarted")}
        </Button>

        <Row gutter={8} aria-hidden="true">
          {QUICK_ACTIONS.map(({ icon: Icon, label }) => (
            <Col span={6} key={label}>
              <Flex vertical align="center" gap={6}>
                <Flex
                  align="center"
                  justify="center"
                  style={{
                    width: "100%",
                    height: 44,
                    borderRadius: 12,
                    background: "var(--ant-color-primary-bg)",
                    color: "var(--ant-color-primary-text)",
                  }}
                >
                  <Icon style={{ fontSize: 18 }} />
                </Flex>
                <Text type="secondary" style={{ fontSize: 12 }}>
                  {label}
                </Text>
              </Flex>
            </Col>
          ))}
        </Row>

        <Flex vertical gap={12} aria-hidden="true">
          {DEMO_TRANSACTIONS.map((tx) => (
            <Flex key={tx.name} align="center" justify="space-between">
              <Flex align="center" gap={10}>
                <Flex
                  align="center"
                  justify="center"
                  style={{
                    width: 32,
                    height: 32,
                    borderRadius: "50%",
                    background: tx.positive
                      ? "var(--ant-color-primary-bg)"
                      : "var(--ant-color-fill-tertiary)",
                    color: tx.positive
                      ? "var(--ant-color-primary-text)"
                      : "var(--ant-color-text-secondary)",
                  }}
                >
                  {tx.positive ? (
                    <Plus style={{ fontSize: 14 }} />
                  ) : (
                    <QrCode style={{ fontSize: 14 }} />
                  )}
                </Flex>
                <Text style={{ fontSize: 12, fontWeight: 500 }}>{tx.name}</Text>
              </Flex>
              <Text
                strong
                style={{
                  fontSize: 12,
                  fontVariantNumeric: "tabular-nums",
                  color: tx.positive ? "var(--ant-color-primary-text)" : undefined,
                }}
              >
                {tx.amount}
              </Text>
            </Flex>
          ))}
        </Flex>
      </Flex>
    </Card>
  );
}
