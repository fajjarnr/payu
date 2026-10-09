"use client";

import { useSearchParams } from "next/navigation";
import { Link } from "@/lib/navigation";
import Image from "next/image";
import { Button, Space, Row, Col, Typography } from "antd";
import {
  CheckCircle2,
  ShieldCheck,
  ArrowRight,
  Lock,
} from "@/components/icons";
import { useTranslations } from "next-intl";
import { Suspense, useEffect } from "react";

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
    <div style={{ minHeight: "100vh", background: "hsl(var(--background))" }}>
      <Row style={{ minHeight: "100vh" }}>
        <Col xs={0} lg={12} style={{ background: "#1A1A2E" }} />
        <Col
          xs={24}
          lg={12}
          style={{
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            padding: 32,
          }}
        >
          <div style={{ width: "100%", maxWidth: 420 }}>
            <div
              style={{
                height: 40,
                width: 192,
                background: "hsl(var(--muted))",
                borderRadius: 12,
                marginBottom: 32,
              }}
            />
            <div
              style={{
                height: 20,
                width: 256,
                background: "hsl(var(--muted) / 0.6)",
                borderRadius: 12,
                marginBottom: 48,
              }}
            />
            <div
              style={{
                height: 48,
                background:
                  "color-mix(in srgb, var(--color-primary-dark) 50%, transparent)",
                borderRadius: 8,
              }}
            />
          </div>
        </Col>
      </Row>
    </div>
  );
}

/**
 * LOGIN-003: the browser is redirected to Keycloak's own login page through
 * the BFF OIDC authorize endpoint (authorization-code + PKCE). Credentials
 * never touch the web-app or auth-service — Keycloak authenticates the user.
 */
function LoginForm() {
  const t = useTranslations("auth");
  const searchParams = useSearchParams();
  const error = searchParams.get("error");

  useEffect(() => {
    document.title = `${t("loginButton")} | PayU Digital Banking`;
  }, [t]);

  return (
    <div style={{ minHeight: "100vh", background: "hsl(var(--background))" }}>
      <Row style={{ minHeight: "100vh" }}>
        {/* Left Panel - Branding (Hidden on mobile) */}
        <Col
          xs={0}
          lg={12}
          style={{
            background: "#1A1A2E",
            borderRight: "1px solid hsl(var(--border) / 0.1)",
            padding: "32px 24px",
            position: "relative",
            overflow: "hidden",
            color: "#FFFFFF",
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
              height: 800,
              background: "hsl(var(--primary) / 0.1)",
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
              height: 600,
              background:
                "color-mix(in srgb, var(--color-bank-green) 90%, transparent)",
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
                  boxShadow: "0 10px 15px -3px hsl(var(--primary) / 0.2)",
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
                  color: "#FFFFFF",
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
                background:
                  "color-mix(in srgb, var(--color-primary-dark) 70%, transparent)",
                border: "1px solid hsl(var(--primary) / 0.3)",
                color: "var(--color-primary-light)",
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
                fontSize: 48,
                fontWeight: 700,
                lineHeight: 1.2,
                letterSpacing: "-0.025em",
                margin: 0,
              }}
            >
              {t("branding.title")}
            </Title>
            <Text
              style={{
                fontSize: 18,
                color: "var(--color-border)",
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
                      color: "#F8F9FA",
                    }}
                  >
                    <CheckCircle2
                      style={{
                        width: 20,
                        height: 20,
                        color: "hsl(var(--primary))",
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
              color: "var(--color-border)",
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
            background: "hsl(var(--background))",
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
                      fontSize: 30,
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
                    border: "1px solid hsl(var(--destructive) / 0.3)",
                    background: "hsl(var(--destructive) / 0.1)",
                    padding: "12px 16px",
                    fontSize: 14,
                    fontWeight: 500,
                    color: "hsl(var(--destructive))",
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
                    boxShadow:
                      "0 10px 15px -3px var(--color-primary-dark) / 0.2)",
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
                      borderTop: "1px solid hsl(var(--border))",
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
                      background: "hsl(var(--background))",
                      padding: "0 16px",
                      color: "hsl(var(--muted-foreground))",
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
                  data-testid="register-link"
                  style={{
                    fontWeight: 700,
                    color: "var(--color-primary-dark)",
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
    </div>
  );
}
