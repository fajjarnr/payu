"use client";

import React, { useState } from "react";
import { useTranslations } from "next-intl";
import {
  Search,
  Trash2,
  Clock,
  MessageSquare,
  Gift,
  ShieldAlert,
} from "@/components/icons";
import {
  Button,
  Input,
  Tag,
  Card,
  Space,
  Row,
  Col,
  Typography,
  List,
  Empty,
  Avatar,
  Segmented,
  theme,
} from "antd";
import DashboardLayout from "@/components/DashboardLayout";
import { useNotifications, useMarkNotificationRead } from "@/hooks";
import { useAuthStore } from "@/stores/authStore";
import { useRouter } from "@/lib/navigation";
import { notify as toast } from "@/lib/notify";
import { SkipLink } from "@/lib/a11y";

const { Title, Text } = Typography;

export default function NotificationsPage() {
  const t = useTranslations("notifications");
  const { token } = theme.useToken();
  const { user } = useAuthStore();
  const router = useRouter();
  const userId = user?.id ?? "";
  const [searchTerm, setSearchTerm] = useState("");
  const [filter, setFilter] = useState("ALL");
  // No delete endpoint exists on notification-service; hide locally and let the
  // next refetch restore the server truth.
  const [hidden, setHidden] = useState<string[]>([]);
  const { data: notificationsData } = useNotifications(userId);
  const markRead = useMarkNotificationRead();

  const rawNotifications = Array.isArray(notificationsData)
    ? notificationsData
    : [];
  // BUG-CROSS-032: Map backend field names (body/sentAt) to frontend display fields (content/timestamp)
  const notifications = rawNotifications.map((n) => ({
    id: n.id,
    title: n.title ?? "",
    content: n.body ?? "",
    type: n.channel ?? "IN_APP",
    read: !!n.readAt,
    timestamp: n.sentAt ?? n.createdAt ?? "",
  }));

  const filteredNotifs = notifications.filter(
    (n: {
      id: string;
      title: string;
      content: string;
      type: string;
      read: boolean;
    }) => {
      if (hidden.includes(n.id)) return false;
      const matchesSearch =
        n.title.toLowerCase().includes(searchTerm.toLowerCase()) ||
        n.content.toLowerCase().includes(searchTerm.toLowerCase());
      const matchesFilter =
        filter === "ALL" ||
        n.type === filter ||
        (filter === "UNREAD" && !n.read);
      return matchesSearch && matchesFilter;
    },
  );

  const handleMarkAllRead = () => {
    notifications.filter((n) => !n.read).forEach((n) => markRead.mutate(n.id));
    toast.success(t("allMarkedRead"));
  };

  const removeNotification = (id: string) => setHidden((prev) => [...prev, id]);

  const handleClearAll = () => {
    setHidden((prev) => [...prev, ...filteredNotifs.map((n) => n.id)]);
    toast.info(t("historyCleared"));
  };

  const getIcon = (type: string) => {
    switch (type) {
      case "PROMO":
        return (
          <Gift
            style={{ width: 20, height: 20, color: token.colorPrimary }}
          />
        );
      case "ALERT":
        return (
          <ShieldAlert
            style={{ width: 20, height: 20, color: token.colorError }}
          />
        );
      case "SECURITY":
        return (
          <Clock
            style={{ width: 20, height: 20, color: token.colorWarning }}
          />
        );
      default:
        return (
          <MessageSquare
            style={{ width: 20, height: 20, color: token.colorPrimary }}
          />
        );
    }
  };

  return (
    <DashboardLayout>
      <SkipLink />
      <Space orientation="vertical" size={24} style={{ width: "100%" }}>
        {/* Header */}
        <Card>
          <Row justify="space-between" align="middle" gutter={[16, 16]}>
            <Col>
              <Space orientation="vertical" size={4}>
                <Title
                  level={1}
                  style={{
                    fontWeight: 700,
                    letterSpacing: "-0.025em",
                    margin: 0,
                  }}
                >
                  {t("title")}
                </Title>
                <Text
                  type="secondary"
                  style={{ fontSize: 14, fontWeight: 500 }}
                >
                  {t("subtitle")}
                </Text>
              </Space>
            </Col>
            <Col>
              <Space size={16}>
                <Button
                  type="text"
                  onClick={handleMarkAllRead}
                  style={{
                    fontSize: 12,
                    fontWeight: 700,
                    letterSpacing: "0.1em",
                    textTransform: "uppercase",
                  }}
                >
                  {t("markAllRead")}
                </Button>
                <Button
                  type="text"
                  danger
                  onClick={handleClearAll}
                  style={{
                    fontSize: 12,
                    fontWeight: 700,
                    letterSpacing: "0.1em",
                    textTransform: "uppercase",
                  }}
                >
                  {t("clearAll")}
                </Button>
              </Space>
            </Col>
          </Row>
        </Card>

        {/* Toolbar */}
        <Card>
          <Row gutter={[16, 16]} align="middle">
            <Col flex="auto">
              <Input
                aria-label={t("searchLabel")}
                placeholder={t("searchPlaceholder")}
                prefix={
                  <Search
                    style={{
                      width: 16,
                      height: 16,
                      color: token.colorTextSecondary,
                    }}
                  />
                }
                style={{
                  height: 48,
                  fontWeight: 700,
                  letterSpacing: "0.1em",
                  textTransform: "uppercase",
                }}
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
              />
            </Col>
            <Col>
              <Segmented
                value={filter}
                onChange={(value) => setFilter(value as string)}
                options={[
                  { label: t("filterAll"), value: "ALL" },
                  { label: t("filterUnread"), value: "UNREAD" },
                  { label: t("filterPromo"), value: "PROMO" },
                  { label: t("filterSecurity"), value: "SECURITY" },
                ]}
              />
            </Col>
          </Row>
        </Card>

        {/* Notifications List */}
        {filteredNotifs.length === 0 ? (
          <Card>
            <Empty
              image={Empty.PRESENTED_IMAGE_SIMPLE}
              description={
                <Text
                  type="secondary"
                  style={{
                    fontSize: 14,
                    fontWeight: 700,
                    letterSpacing: "0.1em",
                    textTransform: "uppercase",
                  }}
                >
                  {t("emptyState")}
                </Text>
              }
            />
          </Card>
        ) : (
          <List
            dataSource={filteredNotifs}
            renderItem={(n) => (
              <List.Item
                key={n.id}
                onClick={() => {
                  if (!n.read) markRead.mutate(n.id);
                }}
                style={{
                  cursor: "pointer",
                  background: token.colorBgContainer,
                  border: `1px solid ${token.colorBorderSecondary}`,
                  borderRadius: 12,
                  padding: 24,
                  marginBottom: 16,
                  borderLeft: !n.read
                    ? `4px solid ${token.colorPrimary}`
                    : undefined,
                }}
                actions={[
                  <Button
                    key="read"
                    type="link"
                    disabled={n.read}
                    onClick={(e) => {
                      e.stopPropagation();
                      markRead.mutate(n.id);
                    }}
                    style={{
                      fontSize: 12,
                      fontWeight: 700,
                      color: token.colorPrimaryText,
                      letterSpacing: "0.1em",
                      textTransform: "uppercase",
                    }}
                  >
                    {n.read ? t("alreadyRead") : t("markRead")}
                  </Button>,
                  <Button
                    key="delete"
                    type="text"
                    shape="circle"
                    size="small"
                    onClick={(e) => {
                      e.stopPropagation();
                      removeNotification(n.id);
                      toast.success(t("notificationDeleted"));
                    }}
                    icon={<Trash2 style={{ width: 16, height: 16 }} />}
                    aria-label={t("deleteLabel")}
                  />,
                ]}
              >
                <List.Item.Meta
                  avatar={
                    <Avatar
                      size={56}
                      style={{
                        background: token.colorFillTertiary,
                        border: `1px solid ${token.colorBorderSecondary}`,
                        display: "flex",
                        alignItems: "center",
                        justifyContent: "center",
                      }}
                    >
                      {getIcon(n.type)}
                    </Avatar>
                  }
                  title={
                    <Space size={12} align="center">
                      <Tag
                        variant="filled"
                        color="green"
                        style={{
                          fontSize: 12,
                          fontWeight: 700,
                          letterSpacing: "0.1em",
                          textTransform: "uppercase",
                          padding: "0 8px",
                          border: `1px solid ${token.colorPrimaryBorder}`,
                          color: token.colorPrimaryText,
                        }}
                      >
                        {n.type}
                      </Tag>
                      <Text
                        type="secondary"
                        style={{
                          fontSize: 12,
                          fontWeight: 700,
                          letterSpacing: "0.1em",
                          textTransform: "uppercase",
                        }}
                      >
                        {new Date(n.timestamp).toLocaleTimeString([], {
                          hour: "2-digit",
                          minute: "2-digit",
                        })}
                      </Text>
                    </Space>
                  }
                  description={
                    <Space orientation="vertical" size={4}>
                      <Text
                        strong
                        style={{
                          fontSize: 18,
                          color: !n.read ? token.colorPrimaryText : undefined,
                        }}
                      >
                        {n.title}
                      </Text>
                      <Text
                        type="secondary"
                        style={{ fontSize: 14, lineHeight: 1.6 }}
                      >
                        {n.content}
                      </Text>
                    </Space>
                  }
                />
              </List.Item>
            )}
          />
        )}
      </Space>
    </DashboardLayout>
  );
}
