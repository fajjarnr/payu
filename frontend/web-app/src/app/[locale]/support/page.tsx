"use client";

import React from "react";
import DashboardLayout from "@/components/DashboardLayout";
import { Link } from "@/lib/navigation";
import {
  LifeBuoy,
  MessageCircle,
  Mail,
  ExternalLink,
  HelpCircle,
  FileText,
} from "@/components/icons";
import { Button, Card, Col, Row, Space, Tag, theme, Typography } from "antd";
import { useTranslations } from "next-intl";

export default function SupportPage() {
  const t = useTranslations("support");
  const { token } = theme.useToken();

  // Live chat and phone have no backend or published number in this deployment;
  // the only support channels that actually exist are the ticket form (POST
  // /support/tickets) and the support mailbox published in the legal terms.
  const supportChannels = [
    {
      label: t("liveChat"),
      desc: t("liveChatDesc"),
      icon: MessageCircle,
      action: t("contactUs"),
      href: "/support/ticket",
      color: "primary",
    },
    {
      label: t("email"),
      desc: t("emailDesc"),
      icon: Mail,
      action: t("sendMessage"),
      href: "mailto:support@payu.fajjjar.my.id",
      color: "secondary",
    },
  ];

  const faqs = [
    {
      title: t("faqItems.identitySync"),
      desc: t("faqItems.identitySyncDesc"),
      icon: HelpCircle,
    },
    {
      title: t("faqItems.transactionLimits"),
      desc: t("faqItems.transactionLimitsDesc"),
      icon: FileText,
    },
    {
      title: t("faqItems.deviceToken"),
      desc: t("faqItems.deviceTokenDesc"),
      icon: HelpCircle,
    },
    {
      title: t("faqItems.fraudPrevention"),
      desc: t("faqItems.fraudPreventionDesc"),
      icon: FileText,
    },
  ];

  const getChannelColor = (color: string) => {
    if (color === "primary") return token.colorPrimary;
    if (color === "secondary") return token.colorPrimaryHover;
    return token.colorText;
  };

  return (
    <DashboardLayout>
      <Space direction="vertical" size={16} style={{ width: "100%" }}>
        {/* Header */}
        <Space direction="vertical" size={4}>
          <Typography.Title level={2} style={{ marginBottom: 0 }}>
            {t("title")}
          </Typography.Title>
          <Typography.Text type="secondary">{t("subtitle")}</Typography.Text>
        </Space>

        {/* Support Channels */}
        <Row gutter={[16, 16]}>
          {supportChannels.map((channel, i) => (
            <Col key={i} xs={24} sm={12} lg={8}>
              <Card
                style={{ height: "100%" }}
                styles={{
                  body: {
                    display: "flex",
                    flexDirection: "column",
                    alignItems: "center",
                    textAlign: "center",
                    height: "100%",
                  },
                }}
              >
                <div
                  style={{
                    width: 80,
                    height: 80,
                    marginBottom: 24,
                    borderRadius: 12,
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                    backgroundColor: getChannelColor(channel.color),
                    color: token.colorTextLightSolid,
                  }}
                >
                  <channel.icon style={{ fontSize: 40 }} />
                </div>
                <Typography.Title level={4} style={{ marginBottom: 12 }}>
                  {channel.label}
                </Typography.Title>
                <Typography.Text
                  type="secondary"
                  style={{ marginBottom: 24, maxWidth: 200 }}
                >
                  {channel.desc}
                </Typography.Text>
                <Button
                  href={channel.href}
                  style={{
                    width: "100%",
                    height: 56,
                    borderRadius: 12,
                    marginTop: "auto",
                  }}
                >
                  {channel.action}
                </Button>
              </Card>
            </Col>
          ))}
        </Row>

        {/* FAQs */}
        <Card>
          <Typography.Title level={4} style={{ marginBottom: 24 }}>
            {t("faqs")}
          </Typography.Title>
          <Row gutter={[16, 16]}>
            {faqs.map((faq, i) => (
              <Col key={i} xs={24} sm={12}>
                <div
                  style={{
                    display: "flex",
                    gap: 24,
                    padding: 24,
                    backgroundColor: token.colorFillQuaternary,
                    borderRadius: 12,
                    cursor: "pointer",
                  }}
                >
                  <div
                    style={{
                      width: 56,
                      height: 56,
                      backgroundColor: token.colorBgContainer,
                      borderRadius: 12,
                      display: "flex",
                      alignItems: "center",
                      justifyContent: "center",
                      flexShrink: 0,
                      border: `1px solid ${token.colorBorder}`,
                    }}
                  >
                    <faq.icon
                      style={{ fontSize: 24, color: token.colorPrimary }}
                    />
                  </div>
                  <div>
                    <div
                      style={{
                        display: "flex",
                        alignItems: "center",
                        gap: 12,
                        marginBottom: 4,
                      }}
                    >
                      <Typography.Text strong style={{ fontSize: 14 }}>
                        {faq.title}
                      </Typography.Text>
                      <ExternalLink
                        style={{ fontSize: 12, color: token.colorPrimary }}
                      />
                    </div>
                    <Typography.Text
                      type="secondary"
                      style={{
                        fontSize: 12,
                        textTransform: "uppercase",
                        letterSpacing: "0.1em",
                      }}
                    >
                      {faq.desc}
                    </Typography.Text>
                  </div>
                </div>
              </Col>
            ))}
          </Row>
        </Card>

        {/* System Status */}
        <Card style={{ position: "relative", overflow: "hidden" }}>
          <Row align="middle" gutter={[16, 16]}>
            <Col xs={24} lg={16}>
              <Space direction="vertical" size={16}>
                <Typography.Title level={3} style={{ marginBottom: 0 }}>
                  Integritas Sistem Aktif.
                </Typography.Title>
                <Space size={12} wrap>
                  {["Gateway", "Backend", "Database", "Streaming"].map(
                    (svc, i) => (
                      <Tag
                        key={i}
                        style={{ padding: "4px 16px", borderRadius: 12 }}
                      >
                        <Typography.Text
                          strong
                          style={{
                            fontSize: 12,
                            textTransform: "uppercase",
                            letterSpacing: "0.1em",
                          }}
                        >
                          {svc}: —
                        </Typography.Text>
                      </Tag>
                    ),
                  )}
                </Space>
                <Typography.Text style={{ color: token.colorTextDisabled }}>
                  Status infrastruktur belum tersedia. Hubungi tim operasional
                  untuk informasi real-time.
                </Typography.Text>
              </Space>
            </Col>
            <Col xs={24} lg={8}>
              <a href="/api/v1/health" target="_blank" rel="noreferrer">
                <Button type="primary" block style={{ height: 64 }}>
                  Cek Detail Infrastruktur
                </Button>
              </a>
            </Col>
          </Row>
        </Card>
      </Space>
    </DashboardLayout>
  );
}
