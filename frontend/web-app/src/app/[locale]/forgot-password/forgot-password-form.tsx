"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { Link } from "@/lib/navigation";
import { Button, Card, Input, Space, Typography } from "antd";
import { ArrowLeft } from "@/components/icons";
import { notify as toast } from "@/lib/notify";
import { SkipLink } from "@/lib/a11y";

const { Title, Text } = Typography;

export default function ForgotPasswordPage() {
  const t = useTranslations("auth");
  const [email, setEmail] = useState("");
  const [fieldError, setFieldError] = useState<string | null>(null);
  const [configError, setConfigError] = useState<string | null>(null);

  // Password reset lives in the identity provider; the BFF has no
  // /api/auth/forgot-password route. Keycloak's own reset-credentials screen
  // (the target of its "Forgot Password?" link) is the real flow, and it works
  // because resetPasswordAllowed=true on the realm.
  const validate = (value: string) => {
    if (!value.trim()) return t("emailRequired");
    return null;
  };

  const handleSubmit = (e?: React.FormEvent) => {
    e?.preventDefault();
    const err = validate(email);
    setFieldError(err);
    setConfigError(null);
    if (err) return;
    // Env-only: no hardcoded issuer fallback (a fixed default would leak one
    // environment URL into every build). Missing env = config error, not redirect.
    const issuer = process.env.NEXT_PUBLIC_OIDC_ISSUER;
    if (!issuer) {
      const msg = t("issuerMissing");
      setConfigError(msg);
      toast.error(msg);
      return;
    }
    window.location.href = `${issuer}/login-actions/reset-credentials`;
  };

  return (
    <>
      <SkipLink />
      <main
      id="main-content"
      style={{
        minHeight: "100vh",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        padding: 32,
      }}
    >
      <Card style={{ width: "100%", maxWidth: 420, borderRadius: 12 }}>
        <Space orientation="vertical" size={24} style={{ width: "100%" }}>
          <Space orientation="vertical" size={8} style={{ width: "100%" }}>
            <Link href="/login" className="payu-focus-ring">
              <Space size={8}>
                <ArrowLeft style={{ width: 16, height: 16 }} />
                <Text type="secondary">{t("backToLogin")}</Text>
              </Space>
            </Link>
            <Title level={2} style={{ margin: 0, fontSize: 24 }}>
              {t("forgotPassword")}
            </Title>
            <Text type="secondary">{t("resetSubtitle")}</Text>
          </Space>
          <form onSubmit={handleSubmit} noValidate style={{ width: "100%" }}>
            <Space orientation="vertical" size={16} style={{ width: "100%" }}>
              <Space orientation="vertical" size={8} style={{ width: "100%" }}>
                <label htmlFor="email" style={{ fontSize: 14, fontWeight: 500 }}>
                  {t("email")}
                </label>
                <Input
                  id="email"
                  type="email"
                  placeholder="nama@email.com"
                  value={email}
                  onChange={(e) => {
                    setEmail(e.target.value);
                    if (fieldError) setFieldError(null);
                  }}
                  onBlur={() => setFieldError(validate(email))}
                  size="large"
                  status={fieldError ? "error" : undefined}
                  aria-invalid={fieldError ? true : undefined}
                  aria-describedby={fieldError ? "email-error" : undefined}
                />
                {fieldError && (
                  <Text id="email-error" type="danger" role="alert" style={{ fontSize: 12 }}>
                    {fieldError}
                  </Text>
                )}
              </Space>
              <Button type="primary" htmlType="submit" size="large" block>
                {t("sendInstructions")}
              </Button>
              {configError && (
                <Text type="danger" role="alert" style={{ fontSize: 12 }}>
                  {configError}
                </Text>
              )}
            </Space>
          </form>
        </Space>
      </Card>
      </main>
    </>
  );
}
