"use client";

import { useSearchParams } from "next/navigation";
import { Link } from "@/lib/navigation";
import Image from "next/image";
import { Button, Space, Row, Col, Typography, theme } from "antd";
import {
  CheckCircle2,
  ShieldCheck,
  ArrowRight,
  Lock,
} from "@/components/icons";
import { useTranslations } from "next-intl";
import { Suspense, useEffect } from "react";
import { SkipLink } from "@/lib/a11y";
import { LoadingState } from "@/components/LoadingState";
import { BRAND_PANEL_BG, BRAND_PANEL_FG } from "@/theme/tokens";

const { Title, Text } = Typography;

/**
 * Page wrapper — provides Suspense boundary required by useSearchParams().
 * Next.js 16 bails out of static rendering when useSearchParams() is used
 * outside a Suspense boundary.
 */
export default function LoginPage() {
  return (
    <Suspense fallback={<LoginSkeleton />}>
      <LoginForm />
    </Suspense>
  );
}

function LoginSkeleton() {
  return (
    <>
      <SkipLink />
      <main id="main-content" style={{ minHeight: "100vh", background: "var(--ant-color-bg-layout)", display: "flex", alignItems: "center", justifyContent: "center" }}>
        <LoadingState variant="spinner" />
      </main>
    </>
  );
}

/**
 * LOGIN-003: the browser is redirected to Keycloak's own login page through
 * the BFF OIDC authorize endpoint (authorization-code + PKCE). Credentials
 * never touch the web-app or auth-service — Keycloak authenticates the user.
 */
function LoginForm() {
  const { token } = theme.useToken();
  const t = useTranslations("auth");
  const searchParams = useSearchParams();
  const error = searchParams.get("error");

  useEffect(() => {
    document.title = `${t("loginButton")} | PayU Digital Banking`;
  }, [t]);

  return (
    <>
      <SkipLink />
      <main id="main-content" style={{ minHeight: "100vh", background: "var(--ant-color-bg-layout)" }}>
      <Row style={{ minHeight: "100vh" }}>
        {/* Left Panel - Branding (Hidden on mobile) */}
        <Col
          xs={0}
          lg={12}
          style={{
            // Always dark in both themes — token.colorText inverted the panel in dark mode.
            background: BRAND_PANEL_BG,
            borderRight: `1px solid color-mix(in srgb, ${token.colorTextLightSolid} 10%, transparent)`,
            padding: "32px 24px",
            position: "relative",
            overflow: "hidden",
            color: BRAND_PANEL_FG,
            display: "flex",
            flexDirection: "column",
            justifyContent: "space-between",
          }}
        >
          {/* Background Effects */}
          <div
            style={{
              position: "absolute",
              top: 0,
              right: 0,
              width: 800,
              maxWidth: "100%",
              height: 800,
              background: `color-mix(in srgb, ${token.colorPrimary} 10%, transparent)`,
              borderRadius: "50%",
              filter: "blur(120px)",
              transform: "translate(50%, -50%)",
            }}
            aria-hidden="true"
          />
          <div
            style={{
              position: "absolute",
              bottom: 0,
              left: 0,
              width: 600,
              maxWidth: "100%",
              height: 600,
              background:
                "color-mix(in srgb, var(--ant-color-primary) 90%, transparent)",
              borderRadius: "50%",
              filter: "blur(100px)",
              transform: "translate(-25%, 50%)",
            }}
            aria-hidden="true"
          />

          {/* Pattern Overlay */}
          <div
            style={{
              position: "absolute",
              inset: 0,
              opacity: 0.03,
              backgroundImage: "url(/textures/noise.svg)",
            }}
            aria-hidden="true"
          />

          <div
            style={{ position: "relative", zIndex: 10, textAlign: "center" }}
          >
            <Link
              href="/"
              className="payu-focus-ring"
              style={{
                display: "flex",
                alignItems: "center",
                gap: 12,
                width: "fit-content",
                margin: "0 auto",
              }}
            >
              <div
                style={{
                  width: 40,
                  height: 40,
                  borderRadius: 12,
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center",
                  overflow: "hidden",
                  boxShadow: token.boxShadowSecondary,
                }}
              >
                <Image
                  src="/logo.svg"
                  alt="PayU Brand Logo"
                  width={40}
                  height={40}
                  priority
                />
              </div>
              <span
                style={{
                  fontSize: 24,
                  fontWeight: 700,
                  letterSpacing: "-0.025em",
                  color: BRAND_PANEL_FG,
                }}
              >
                PayU
              </span>
            </Link>
          </div>

          <div
            style={{
              position: "relative",
              zIndex: 10,
              maxWidth: 512,
              margin: "0 auto",
              textAlign: "center",
            }}
          >
            <div
              style={{
                display: "inline-flex",
                alignItems: "center",
                gap: 8,
                padding: "4px 12px",
                borderRadius: 9999,
                background: `color-mix(in srgb, ${token.colorPrimaryActive} 70%, transparent)`,
                border: `1px solid ${token.colorPrimaryBorder}`,
                color: BRAND_PANEL_FG,
                fontSize: 12,
                fontWeight: 700,
                letterSpacing: "0.1em",
                textTransform: "uppercase",
                marginBottom: 16,
              }}
            >
              <ShieldCheck style={{ width: 16, height: 16 }} />
              <span>{t("branding.tag")}</span>
            </div>
            <Title
              level={1}
              style={{
                fontSize: 32,
                fontWeight: 700,
                lineHeight: 1.2,
                letterSpacing: "-0.025em",
                margin: 0,
                // The brand panel is always dark: antd's heading colour would
                // render dark-on-dark here, so the panel's ink is explicit.
                color: BRAND_PANEL_FG,
              }}
            >
              {t("branding.title")}
            </Title>
            <Text
              style={{
                fontSize: 18,
                color: `color-mix(in srgb, ${token.colorTextLightSolid} 75%, transparent)`,
                lineHeight: 1.6,
              }}
            >
              {t("branding.desc")}
            </Text>

            <div style={{ paddingTop: 32 }}>
              <Space direction="vertical" size={16}>
                {[
                  t("branding.features.encryption"),
                  t("branding.features.monitoring"),
                  t("branding.features.qris"),
                ].map((feature, i) => (
                  <div
                    key={i}
                    style={{
                      display: "flex",
                      alignItems: "center",
                      gap: 12,
                      color: token.colorBgContainer,
                    }}
                  >
                    <CheckCircle2
                      style={{
                        width: 20,
                        height: 20,
                        color: token.colorPrimary,
                      }}
                    />
                    <span style={{ fontWeight: 500 }}>{feature}</span>
                  </div>
                ))}
              </Space>
            </div>
          </div>

          <div
            style={{
              position: "relative",
              zIndex: 10,
              color: token.colorTextSecondary,
              fontSize: 12,
              fontFamily: "monospace",
              textAlign: "center",
            }}
          >
            {t("branding.footer")}
          </div>
        </Col>

        {/* Right Panel - Sign in */}
        <Col
          xs={24}
          lg={12}
          style={{
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            padding: "32px 24px",
            background: "var(--ant-color-bg-layout)",
            position: "relative",
          }}
        >
          <div style={{ width: "100%", maxWidth: 420 }}>
            <Space direction="vertical" size={32} style={{ width: "100%" }}>
              <div style={{ textAlign: "center" }}>
                <Space direction="vertical" size={8}>
                  <Title
                    level={2}
                    style={{
                      fontSize: 24,
                      fontWeight: 700,
                      letterSpacing: "-0.025em",
                      margin: 0,
                    }}
                  >
                    {t("welcomeBack")}
                  </Title>
                  <Text type="secondary">{t("subtitle")}</Text>
                </Space>
              </div>

              {error && (
                <div
                  style={{
                    borderRadius: 12,
                    border: `1px solid ${token.colorErrorBorder}`,
                    background: token.colorErrorBg,
                    padding: "12px 16px",
                    fontSize: 14,
                    fontWeight: 500,
                    color: token.colorErrorText,
                  }}
                  role="alert"
                  data-testid="login-error"
                >
                  {t("loginFailed")}
                </div>
              )}

              <Space direction="vertical" size={24} style={{ width: "100%" }}>
                <Button
                  type="primary"
                  htmlType="button"
                  data-testid="login-submit-button"
                  style={{
                    width: "100%",
                    height: 48,
                    fontWeight: 700,
                    fontSize: 16,
                    boxShadow: token.boxShadowSecondary,
                  }}
                  onClick={() => {
                    window.location.href = "/api/auth/authorize";
                  }}
                >
                  <Lock style={{ width: 16, height: 16, marginRight: 8 }} />
                  {t("loginButton")}
                </Button>
                <Text
                  type="secondary"
                  style={{ fontSize: 12, textAlign: "center" }}
                >
                  {t("oidcNote")}
                </Text>
              </Space>

              <div style={{ position: "relative" }} aria-hidden="true">
                <div
                  style={{
                    position: "absolute",
                    inset: 0,
                    display: "flex",
                    alignItems: "center",
                  }}
                >
                  <span
                    style={{
                      width: "100%",
                      borderTop: `1px solid ${token.colorBorderSecondary}`,
                    }}
                  />
                </div>
                <div
                  style={{
                    position: "relative",
                    display: "flex",
                    justifyContent: "center",
                    fontSize: 12,
                    textTransform: "uppercase",
                  }}
                >
                  <span
                    style={{
                      background: "var(--ant-color-bg-layout)",
                      padding: "0 16px",
                      color: token.colorTextSecondary,
                      fontWeight: 500,
                    }}
                  >
                    {t("or")}
                  </span>
                </div>
              </div>

              <div style={{ textAlign: "center", fontSize: 14 }}>
                <Text type="secondary">{t("noAccount")}</Text>{" "}
                <Link
                  href="/onboarding"
                  className="payu-focus-ring"
                  data-testid="register-link"
                  style={{
                    fontWeight: 700,
                    color: token.colorText,
                    textDecoration: "underline",
                    textUnderlineOffset: 3,
                    display: "inline-flex",
                    alignItems: "center",
                  }}
                >
                  {t("registerLink")}{" "}
                  <ArrowRight
                    style={{ width: 12, height: 12, marginLeft: 4 }}
                  />
                </Link>
              </div>
            </Space>
          </div>
        </Col>
      </Row>
      </main>
    </>
  );
}
