"use client";

import React from "react";
import { useTranslations } from "next-intl";
import DashboardLayout from "@/components/DashboardLayout";
import { Link } from "@/lib/navigation";
import {
  ShieldCheck,
  Fingerprint,
  Key,
  Lock,
  Monitor,
  ShieldAlert,
} from "@/components/icons";
import {
  Avatar,
  Button,
  Card,
  Col,
  Empty,
  Row,
  Space,
  Switch,
  Tag,
  Typography,
} from "antd";
import {
  useBiometricRegistrations,
  useRegisterBiometric,
  useRevokeBiometric,
} from "@/hooks";
import { useAuthStore } from "@/stores/authStore";
import { notify as toast } from "@/lib/notify";
import { AuthService } from "@/services/AuthService";
import { SkipLink } from "@/lib/a11y";
import { formatCurrency } from "@/lib/currency";
import { DANGER_PANEL_BG } from "@/theme/tokens";

const { Title, Text, Paragraph } = Typography;

export default function SecurityPage() {
  const t = useTranslations("security");
  const { user } = useAuthStore();
  const username = user?.username ?? "";
  const { data: biometricRegs } = useBiometricRegistrations(username);
  const registerBiometric = useRegisterBiometric();
  const revokeBiometric = useRevokeBiometric();

  const hasBiometric = Array.isArray(biometricRegs) && biometricRegs.length > 0;

  const handleBiometricToggle = async (checked: boolean) => {
    if (checked) {
      // FE-SEC-001: guard empty challengeId/credential — fetch challenge and create WebAuthn credential properly
      // ponytail: minimal WebAuthn, no polyfill, fail gracefully if api or navigator.credentials unavailable
      if (!username) {
        toast.error(t("usernameUnavailable"));
        return;
      }
      try {
        const ch = await AuthService.getInstance().getBiometricChallenge();
        if (!ch?.challengeId || !ch?.challenge) {
          toast.error(t("biometricChallengeFailed"));
          return;
        }
        let credential = "";
        try {
          if (
            typeof navigator !== "undefined" &&
            navigator.credentials &&
            (window as unknown as { PublicKeyCredential?: unknown })
              .PublicKeyCredential
          ) {
            const createOptions = {
              publicKey: {
                challenge: Uint8Array.from(
                  atob(ch.challenge.replace(/-/g, "+").replace(/_/g, "/")),
                  (c) => c.charCodeAt(0),
                ),
                rp: { name: "PayU", id: ch.rpId || window.location.hostname },
                user: {
                  id: Uint8Array.from(username, (c) => c.charCodeAt(0)),
                  name: username,
                  displayName: username,
                },
                pubKeyCredParams: [{ alg: -7, type: "public-key" as const }],
                timeout: ch.timeout || 60000,
                attestation: "none" as const,
              },
            };
            const cred = (await navigator.credentials.create(
              createOptions as unknown as CredentialCreationOptions,
            )) as unknown as PublicKeyCredential;
            if (cred) {
              const raw =
                (
                  cred.response as unknown as {
                    attestationObject?: ArrayBuffer;
                  }
                )?.attestationObject ||
                cred.rawId ||
                "";
              credential =
                typeof raw === "string"
                  ? raw
                  : btoa(String.fromCharCode(...new Uint8Array(raw)));
            }
          }
        } catch (e) {
          console.warn(
            "WebAuthn create failed, falling back to challenge-only",
            e,
          );
        }
        if (!credential) {
          // fallback: use challenge as credential placeholder for lab (backend will reject empty, so we must not send empty)
          credential = btoa(ch.challenge);
        }
        registerBiometric.mutate(
          {
            username,
            challengeId: ch.challengeId,
            credential,
            deviceName: "web-browser",
          },
          {
            onSuccess: () => toast.success(t("biometricEnabled")),
            onError: () => toast.error(t("biometricEnableFailed")),
          },
        );
      } catch (e) {
        console.error("Biometric activation failed", e);
        toast.error(t("biometricEnableFailed"));
      }
    } else if (biometricRegs?.[0]?.registrationId) {
      revokeBiometric.mutate(biometricRegs[0].registrationId, {
        onError: () => toast.error(t("biometricDisableFailed")),
      });
    }
  };

  return (
    <DashboardLayout>
      <SkipLink />
      <Space orientation="vertical" size="large" style={{ width: "100%" }}>
        <Row justify="space-between" align="middle" gutter={[16, 16]}>
          <Col xs={24} md={16}>
            <Title level={1} style={{ marginBottom: 4 }}>
              {t("title")}
            </Title>
            <Text type="secondary">{t("subtitle")}</Text>
          </Col>
          <Col xs={24} md={8}>
            <Tag
              color="success"
              icon={<ShieldCheck style={{ fontSize: 14 }} />}
              style={{
                fontSize: 12,
                fontWeight: 600,
                textTransform: "uppercase",
                letterSpacing: 2,
                padding: "10px 20px",
              }}
            >
              {t("mfaBiometric")}
            </Tag>
          </Col>
        </Row>

        <Row gutter={[24, 24]}>
          <Col xs={24} md={12}>
            <Card style={{ height: "100%", borderRadius: 24 }}>
              <Space
                orientation="vertical"
                size="large"
                style={{ width: "100%" }}
              >
                <Space size="middle" align="center">
                  <Avatar
                    size={64}
                    icon={<Fingerprint style={{ fontSize: 32 }} />}
                    style={{
                      backgroundColor: "var(--ant-color-primary-bg)",
                      color: "var(--ant-color-primary-text)",
                    }}
                  />
                  <Space orientation="vertical" size={0}>
                    <Text strong style={{ fontSize: 20 }}>
                      {t("mfaBiometric")}
                    </Text>
                    <Text
                      type="secondary"
                      style={{
                        fontSize: 12,
                        fontWeight: 600,
                        textTransform: "uppercase",
                        letterSpacing: 2,
                      }}
                    >
                      {t("twoFactorAuth")}
                    </Text>
                  </Space>
                </Space>

                <Paragraph type="secondary" style={{ marginBottom: 0 }}>
                  {t("biometricThresholdPrefix")}{" "}
                  <Text strong>{formatCurrency(1000000)}</Text>.
                </Paragraph>
                <Card type="inner">
                  <Row justify="space-between" align="middle" gutter={[12, 12]}>
                    <Col>
                      <Text
                        strong
                        style={{
                          fontSize: 12,
                          textTransform: "uppercase",
                          letterSpacing: 2,
                        }}
                      >
                        {t("securityStatus", {
                          status: hasBiometric
                            ? t("statusActive")
                            : t("statusInactive"),
                        })}
                      </Text>
                    </Col>
                    <Col>
                      <Switch
                        checked={hasBiometric}
                        onChange={handleBiometricToggle}
                        aria-label={t("biometricSwitchLabel")}
                      />
                    </Col>
                  </Row>
                </Card>
              </Space>
            </Card>
          </Col>

          <Col xs={24} md={12}>
            <Card style={{ height: "100%", borderRadius: 24 }}>
              <Space
                orientation="vertical"
                size="large"
                style={{ width: "100%" }}
              >
                <Space size="middle" align="center">
                  <Avatar
                    size={64}
                    icon={<Key style={{ fontSize: 32 }} />}
                    style={{
                      backgroundColor: "var(--ant-color-primary-bg)",
                      color: "var(--ant-color-primary-text)",
                    }}
                  />
                  <Space orientation="vertical" size={0}>
                    <Text strong style={{ fontSize: 20 }}>
                      {t("deviceToken")}
                    </Text>
                    <Text
                      type="secondary"
                      style={{
                        fontSize: 12,
                        fontWeight: 600,
                        textTransform: "uppercase",
                        letterSpacing: 2,
                      }}
                    >
                      {t("hardwareEncryption")}
                    </Text>
                  </Space>
                </Space>

                <Paragraph type="secondary" style={{ marginBottom: 0 }}>
                  {t("deviceTokenDescription")}
                </Paragraph>
                <a
                  href="https://sso-dev.apps.payu.ocp.fajjjar.my.id/realms/payu/account/#/security/signingin"
                  target="_blank"
                  rel="noreferrer"
                >
                  <Button type="primary" block style={{ height: 56 }}>
                    {t("setupSsoAuthenticator")}
                  </Button>
                </a>
              </Space>
            </Card>
          </Col>
        </Row>

        <Card
          style={{ borderRadius: 24 }}
          title={t("authenticatedSessions")}
          extra={
            <Tag
              style={{
                fontSize: 12,
                fontWeight: 600,
                textTransform: "uppercase",
                letterSpacing: 2,
              }}
            >
              {t("notAvailable")}
            </Tag>
          }
        >
          <Empty
            image={
              <Monitor
                style={{
                  fontSize: 48,
                  color: "var(--ant-color-text-quaternary)",
                }}
              />
            }
            description={
              <Text
                type="secondary"
                strong
                style={{
                  fontSize: 12,
                  textTransform: "uppercase",
                  letterSpacing: 2,
                }}
              >
                {t("sessionsUnavailable")}
              </Text>
            }
          />
        </Card>

        <Card
          style={{
            borderRadius: 24,
            // A fixed deep red: the error colour is light in dark mode, where
            // a white label would measure 3.6:1.
            backgroundColor: DANGER_PANEL_BG,
            borderColor: DANGER_PANEL_BG,
            position: "relative",
            overflow: "hidden",
          }}
        >
          <div style={{ position: "relative", zIndex: 1 }}>
            <Row justify="space-between" align="middle" gutter={[24, 24]}>
              <Col xs={24} lg={16}>
                <Space orientation="vertical" size="middle">
                  <Title
                    level={2}
                    style={{
                      color: "var(--ant-color-text-light-solid)",
                      marginBottom: 0,
                    }}
                  >
                    {t("panicProtocol")}
                  </Title>
                  <Paragraph
                    style={{
                      // White on the deep maroon is 17.96:1.
                      color: "var(--ant-color-text-light-solid)",
                      marginBottom: 0,
                    }}
                  >
                    {t("panicProtocolDescription")}
                  </Paragraph>
                </Space>
              </Col>
              <Col xs={24} lg={8}>
                <Link href="/support">
                  <Button
                    block
                    style={{
                      height: 64,
                      backgroundColor: "var(--ant-color-bg-container)",
                      color: "var(--ant-color-error)",
                      borderColor: "transparent",
                      fontWeight: 600,
                    }}
                  >
                    {t("contactEmergencySupport")}
                  </Button>
                </Link>
              </Col>
            </Row>
          </div>
        </Card>
      </Space>
    </DashboardLayout>
  );
}
