"use client";

import React, { useState } from "react";
import {
  Alert,
  Avatar,
  Button,
  Card,
  Col,
  Divider,
  Input,
  Row,
  Space,
  Tag,
  Typography,
} from "antd";
import {
  CheckCircle,
  ChevronRight,
  Loader2,
  Trash2,
  User,
} from "@/components/icons";
import { useTranslations } from "next-intl";
import DashboardLayout from "@/components/DashboardLayout";
import { useAuthStore } from "@/stores/authStore";
import { useUpdateUser, useLogout } from "@/hooks";
import { notify as toast } from "@/lib/notify";
import StatementDownloader from "@/components/settings/statement-downloader";

const { Title, Text } = Typography;

interface FormData {
  fullName: string;
  email: string;
  phoneNumber: string;
}

export default function SettingsPage() {
  const t = useTranslations("settings");
  const { user, accountId } = useAuthStore();
  const updateUser = useUpdateUser();
  const logoutMutation = useLogout();

  const [activeTab, setActiveTab] = useState("profile");
  const [formData, setFormData] = useState<FormData>({
    fullName: user?.fullName ?? "",
    email: user?.email ?? "",
    phoneNumber: user?.phoneNumber ?? "",
  });

  const menuItems = [
    {
      label: t("menu.profile"),
      icon: User,
      onClick: () => setActiveTab("profile"),
      active: activeTab === "profile",
    },
    {
      label: t("menu.beneficiaries"),
      icon: ChevronRight,
      onClick: () => setActiveTab("beneficiaries"),
      active: activeTab === "beneficiaries",
    },
    {
      label: t("menu.eStatement"),
      icon: CheckCircle,
      onClick: () => setActiveTab("statements"),
      active: activeTab === "statements",
    },
  ];

  const handleInputChange = (field: keyof FormData, value: string) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
  };

  const handleSubmit = () => {
    if (!user?.id) {
      toast.error(t("profileUpdateError"));
      return;
    }
    updateUser.mutate(
      {
        userId: user.id,
        data: {
          fullName: formData.fullName,
          email: formData.email,
          phoneNumber: formData.phoneNumber,
        },
      },
      {
        onSuccess: () => toast.success(t("profileUpdateSuccess")),
        onError: () => toast.error(t("profileUpdateError")),
      },
    );
  };

  const handleClearSession = () => {
    logoutMutation.mutate();
  };

  return (
    <DashboardLayout>
      <Space direction="vertical" size={24} style={{ width: "100%" }}>
        {/* Header */}
        <Row justify="space-between" align="bottom" gutter={[16, 16]}>
          <Col>
            <Title level={2} style={{ margin: 0 }}>
              {t("header.title")}
            </Title>
            <Text type="secondary" style={{ display: "block", marginTop: 4 }}>
              {t("header.subtitle")}
            </Text>
          </Col>
        </Row>

        <Row gutter={[24, 24]}>
          {/* Sidebar */}
          <Col xs={24} md={12} lg={8}>
            <Space direction="vertical" size={16} style={{ width: "100%" }}>
              {/* Profile Card */}
              <Card>
                <div style={{ position: "relative", overflow: "hidden" }}>
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
                    }}
                  />
                  <Space
                    direction="vertical"
                    size={16}
                    style={{ width: "100%", textAlign: "center" }}
                  >
                    <Avatar
                      size={96}
                      shape="square"
                      style={{
                        backgroundColor: "var(--ant-color-primary)",
                        color: "var(--ant-color-text-light-solid)",
                        fontSize: 36,
                        fontWeight: 700,
                        borderRadius: 16,
                      }}
                    >
                      {formData.fullName
                        ? formData.fullName.charAt(0).toUpperCase()
                        : "P"}
                    </Avatar>
                    <Title level={3} style={{ margin: 0 }}>
                      {formData.fullName || "PENGGUNA PAYU"}
                    </Title>
                    <Tag color="success">{t("premiumMember")}</Tag>
                    <Divider style={{ margin: "40px 0" }} />
                    <Space
                      direction="vertical"
                      size={12}
                      style={{ width: "100%" }}
                    >
                      <Row justify="space-between">
                        <Text type="secondary" strong>
                          {t("accountId")}
                        </Text>
                        <Text strong code>
                          {user?.id?.slice(0, 12) || "PAYU-09228373"}
                        </Text>
                      </Row>
                      <Row justify="space-between">
                        <Text type="secondary" strong>
                          {t("status")}
                        </Text>
                        <Text strong type="success">
                          {t("ekycVerified")}
                        </Text>
                      </Row>
                    </Space>
                  </Space>
                </div>
              </Card>

              {/* Menu Card */}
              <Card>
                <Space direction="vertical" size={8} style={{ width: "100%" }}>
                  {menuItems.map((item, i) => (
                    <Button
                      type="text"
                      key={i}
                      onClick={item.onClick}
                      block
                      style={
                        item.active
                          ? {
                              display: "flex",
                              alignItems: "center",
                              justifyContent: "space-between",
                              padding: "16px 20px",
                              borderRadius: 12,
                              backgroundColor: "var(--ant-color-primary)",
                              color: "var(--ant-color-text-light-solid)",
                              boxShadow: "0 10px 15px -3px rgba(0,0,0,0.1)",
                            }
                          : {
                              display: "flex",
                              alignItems: "center",
                              justifyContent: "space-between",
                              padding: "16px 20px",
                              borderRadius: 12,
                              color: "var(--ant-color-text-secondary)",
                            }
                      }
                    >
                      <span
                        style={{
                          display: "flex",
                          alignItems: "center",
                          gap: 16,
                        }}
                      >
                        <item.icon style={{ width: 20, height: 20 }} />
                        <span
                          style={{
                            fontSize: 12,
                            fontWeight: 700,
                            textTransform: "uppercase",
                            letterSpacing: "0.1em",
                          }}
                        >
                          {item.label}
                        </span>
                      </span>
                      {item.active && (
                        <ChevronRight style={{ width: 16, height: 16 }} />
                      )}
                    </Button>
                  ))}
                </Space>
              </Card>
            </Space>
          </Col>

          {/* Main Settings Form */}
          <Col xs={24} md={12} lg={16}>
            {activeTab === "profile" ? (
              <Card>
                <Space direction="vertical" size={24} style={{ width: "100%" }}>
                  {/* Success Alert */}
                  {updateUser.isSuccess && (
                    <Alert
                      type="success"
                      showIcon
                      icon={
                        <CheckCircle
                          style={{
                            width: 16,
                            height: 16,
                            color: "var(--ant-color-success)",
                          }}
                        />
                      }
                      description={
                        <Text type="success">{t("profileUpdateSuccess")}</Text>
                      }
                    />
                  )}

                  {/* Error Alert */}
                  {updateUser.isError && (
                    <Alert
                      type="error"
                      description={
                        <Text type="danger">{t("profileUpdateError")}</Text>
                      }
                    />
                  )}

                  {/* Personal Details */}
                  <Space
                    direction="vertical"
                    size={24}
                    style={{ width: "100%" }}
                  >
                    <Space size={16}>
                      <div
                        style={{
                          width: 48,
                          height: 48,
                          backgroundColor: "var(--ant-color-primary-bg)",
                          borderRadius: 12,
                          display: "flex",
                          alignItems: "center",
                          justifyContent: "center",
                          border: "1px solid var(--ant-color-primary-border)",
                        }}
                      >
                        <User
                          style={{
                            width: 24,
                            height: 24,
                            color: "var(--ant-color-primary)",
                          }}
                        />
                      </div>
                      <Title level={3} style={{ margin: 0 }}>
                        {t("profileCredentials")}
                      </Title>
                    </Space>

                    <Row gutter={[24, 24]}>
                      <Col xs={24} md={12}>
                        <Space
                          direction="vertical"
                          size={8}
                          style={{ width: "100%" }}
                        >
                          <label
                            htmlFor="settings-fullname"
                            style={{
                              fontSize: 12,
                              fontWeight: 700,
                              color: "var(--ant-color-text-secondary)",
                              textTransform: "uppercase",
                              letterSpacing: "0.1em",
                              marginLeft: 4,
                            }}
                          >
                            {t("form.fullName")}
                          </label>
                          <Input
                            id="settings-fullname"
                            type="text"
                            value={formData.fullName}
                            onChange={(e) =>
                              handleInputChange("fullName", e.target.value)
                            }
                            placeholder={t("form.fullNamePlaceholder")}
                            disabled={updateUser.isPending}
                          />
                        </Space>
                      </Col>
                      <Col xs={24} md={12}>
                        <Space
                          direction="vertical"
                          size={8}
                          style={{ width: "100%" }}
                        >
                          <label
                            htmlFor="settings-email"
                            style={{
                              fontSize: 12,
                              fontWeight: 700,
                              color: "var(--ant-color-text-secondary)",
                              textTransform: "uppercase",
                              letterSpacing: "0.1em",
                              marginLeft: 4,
                            }}
                          >
                            {t("form.contactEmail")}
                          </label>
                          <Input
                            id="settings-email"
                            type="email"
                            value={formData.email}
                            onChange={(e) =>
                              handleInputChange("email", e.target.value)
                            }
                            placeholder="email@contoh.com"
                            disabled={updateUser.isPending}
                          />
                        </Space>
                      </Col>
                      <Col xs={24} md={12}>
                        <Space
                          direction="vertical"
                          size={8}
                          style={{ width: "100%" }}
                        >
                          <label
                            htmlFor="settings-phone"
                            style={{
                              fontSize: 12,
                              fontWeight: 700,
                              color: "var(--ant-color-text-secondary)",
                              textTransform: "uppercase",
                              letterSpacing: "0.1em",
                              marginLeft: 4,
                            }}
                          >
                            {t("form.phone")}
                          </label>
                          <Input
                            id="settings-phone"
                            type="text"
                            value={formData.phoneNumber}
                            onChange={(e) =>
                              handleInputChange("phoneNumber", e.target.value)
                            }
                            placeholder="+62 812-3456-7890"
                            disabled={updateUser.isPending}
                          />
                        </Space>
                      </Col>
                    </Row>
                  </Space>

                  <Divider />

                  <Space size={16} style={{ paddingTop: 40 }}>
                    <Button
                      type="primary"
                      onClick={handleSubmit}
                      disabled={updateUser.isPending || !user?.id}
                    >
                      {updateUser.isPending ? (
                        <>
                          <Loader2
                            style={{
                              width: 16,
                              height: 16,
                              marginRight: 8,
                              animation: "spin 1s linear infinite",
                            }}
                          />
                          {t("saving")}
                        </>
                      ) : (
                        t("syncProfile")
                      )}
                    </Button>
                    <Button danger onClick={handleClearSession}>
                      <Trash2
                        style={{ width: 20, height: 20, marginRight: 4 }}
                      />
                      {t("clearSession")}
                    </Button>
                  </Space>
                </Space>
              </Card>
            ) : activeTab === "beneficiaries" ? (
              <BeneficiaryManager accountId={accountId ?? ""} />
            ) : (
              <Space direction="vertical" size={24} style={{ width: "100%" }}>
                <div>
                  <Title level={2} style={{ margin: 0 }}>
                    {t("menu.eStatement")}
                  </Title>
                  <Text
                    type="secondary"
                    style={{ display: "block", marginTop: 4 }}
                  >
                    {t("eStatementSubtitle")}
                  </Text>
                </div>
                <StatementDownloader />
              </Space>
            )}
          </Col>
        </Row>
      </Space>
    </DashboardLayout>
  );
}

// Inline BeneficiaryManager to avoid circular imports
function BeneficiaryManager({ accountId }: { accountId: string }) {
  const t = useTranslations("settings");
  const [formData, setFormData] = useState({
    accountName: "",
    accountNumber: "",
  });

  const handleSubmit = () => {
    // Mutate beneficiary
  };

  return (
    <Card>
      <Space direction="vertical" size={24} style={{ width: "100%" }}>
        <Title level={3} style={{ margin: 0 }}>
          {t("menu.beneficiaries")}
        </Title>
        <Space direction="vertical" size={16} style={{ width: "100%" }}>
          <label
            htmlFor="beneficiary-name"
            style={{
              fontSize: 12,
              fontWeight: 700,
              color: "var(--ant-color-text-secondary)",
              textTransform: "uppercase",
              letterSpacing: "0.1em",
            }}
          >
            {t("form.accountName")}
          </label>
          <Input
            id="beneficiary-name"
            type="text"
            value={formData.accountName}
            onChange={(e) =>
              setFormData((prev) => ({ ...prev, accountName: e.target.value }))
            }
            placeholder={t("form.accountNamePlaceholder")}
          />
          <label
            htmlFor="beneficiary-number"
            style={{
              fontSize: 12,
              fontWeight: 700,
              color: "var(--ant-color-text-secondary)",
              textTransform: "uppercase",
              letterSpacing: "0.1em",
            }}
          >
            {t("form.accountNumber")}
          </label>
          <Input
            id="beneficiary-number"
            type="text"
            value={formData.accountNumber}
            onChange={(e) =>
              setFormData((prev) => ({
                ...prev,
                accountNumber: e.target.value,
              }))
            }
            placeholder={t("form.accountNumberPlaceholder")}
          />
        </Space>
        <Button type="primary" onClick={handleSubmit}>
          {t("addBeneficiary")}
        </Button>
      </Space>
    </Card>
  );
}
