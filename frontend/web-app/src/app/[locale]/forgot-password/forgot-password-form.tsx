"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { Link } from "@/lib/navigation";
import { Button, Card, Input, Space, Typography } from "antd";
import { ArrowLeft } from "@/components/icons";
import { notify as toast } from "@/lib/notify";

const { Title, Text } = Typography;

export default function ForgotPasswordPage() {
  const t = useTranslations("auth");
  const [email, setEmail] = useState("");

  // Password reset lives in the identity provider; the BFF has no
  // /api/auth/forgot-password route. Keycloak's own reset-credentials screen
  // (the target of its "Forgot Password?" link) is the real flow, and it works
  // because resetPasswordAllowed=true on the realm.
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!email.trim()) {
      toast.error("Email wajib diisi");
      return;
    }
    const issuer =
      process.env.NEXT_PUBLIC_OIDC_ISSUER ??
      "https://sso-dev.apps.fajjjar.my.id/realms/payu";
    window.location.href = `${issuer}/login-actions/reset-credentials`;
  };

  return (
    <div
      style={{
        minHeight: "100vh",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        padding: 32,
      }}
    >
      <Card style={{ width: "100%", maxWidth: 420 }}>
        <Space direction="vertical" size={24} style={{ width: "100%" }}>
          <Space direction="vertical" size={8} style={{ width: "100%" }}>
            <Link href="/login">
              <Space size={8}>
                <ArrowLeft style={{ width: 16, height: 16 }} />
                <Text type="secondary">Kembali ke login</Text>
              </Space>
            </Link>
            <Title level={2} style={{ margin: 0 }}>
              {t("forgotPassword")}
            </Title>
            <Text type="secondary">
              Masukkan email Anda untuk menerima instruksi reset password.
            </Text>
          </Space>
          <Space direction="vertical" size={16} style={{ width: "100%" }}>
            <Space direction="vertical" size={8} style={{ width: "100%" }}>
              <label htmlFor="email" style={{ fontSize: 14, fontWeight: 500 }}>
                {t("email")}
              </label>
              <Input
                id="email"
                type="email"
                placeholder="nama@email.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                size="large"
              />
            </Space>
            <Button
              type="primary"
              htmlType="submit"
              size="large"
              block
              onClick={handleSubmit}
            >
              Kirim Instruksi
            </Button>
          </Space>
        </Space>
      </Card>
    </div>
  );
}
