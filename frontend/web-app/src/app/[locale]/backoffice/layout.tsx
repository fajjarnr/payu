"use client";

import React, { useState } from "react";
import { Link } from "@/lib/navigation";
import { usePathname } from "@/lib/navigation";
import {
  Users,
  AlertTriangle,
  Headphones,
  FileText,
  ClipboardCheck,
  Gift,
  TrendingUp,
  Store,
  LayoutDashboard,
  Search,
  Bell,
  Menu,
} from "@/components/icons";
import { ON_PRIMARY } from "@/theme/tokens";
import ThemeToggle from "@/components/ThemeToggle";
import {
  Avatar,
  Badge,
  Button,
  Flex,
  Input,
  Layout,
  Menu as AntMenu,
  Space,
  Typography,
} from "antd";

const GROUP_ORDER = [
  "CORE",
  "OPERATIONS",
  "PLATFORM",
  "GOVERNANCE",
  "GROWTH",
  "FINANCIAL",
  "ECOSYSTEM",
];

export default function BackofficeLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const pathname = usePathname();
  const [collapsed, setCollapsed] = useState(false);

  const navigation = [
    {
      name: "Command Center",
      href: "/backoffice",
      icon: LayoutDashboard,
      group: "CORE",
    },
    {
      name: "KYC Reviews",
      href: "/backoffice/kyc",
      icon: Users,
      group: "OPERATIONS",
    },
    {
      name: "Fraud Monitoring",
      href: "/backoffice/fraud",
      icon: AlertTriangle,
      group: "OPERATIONS",
    },
    {
      name: "Customer Service",
      href: "/backoffice/customers",
      icon: Headphones,
      group: "OPERATIONS",
    },
    {
      name: "CMS Content",
      href: "/backoffice/cms",
      icon: FileText,
      group: "PLATFORM",
    },
    {
      name: "Compliance Audit",
      href: "/backoffice/compliance",
      icon: ClipboardCheck,
      group: "GOVERNANCE",
    },
    {
      name: "Campaigns",
      href: "/backoffice/campaigns",
      icon: Gift,
      group: "GROWTH",
    },
    {
      name: "FX Rates",
      href: "/backoffice/fx-rates",
      icon: TrendingUp,
      group: "FINANCIAL",
    },
    {
      name: "Partners",
      href: "/backoffice/partners",
      icon: Store,
      group: "ECOSYSTEM",
    },
  ];

  const activeNav = navigation.find(
    (item) =>
      pathname === item.href ||
      (item.href !== "/backoffice" && pathname.startsWith(item.href)),
  );
  const selectedKey = activeNav?.href ?? "/backoffice";

  return (
    <Layout style={{ minHeight: "100vh" }}>
      <style>{`
        .payu-header { height: 72px !important; line-height: 72px !important; padding: 0 16px !important; }
        .payu-backoffice-content { padding: 16px !important; }
        .payu-backoffice-search { display: none !important; }
        @media (min-width: 640px) {
          .payu-header { padding: 0 32px !important; }
          .payu-backoffice-content { padding: 32px !important; }
          .payu-backoffice-search { display: inline-flex !important; }
        }
      `}</style>
      <Layout.Sider
        collapsible
        collapsed={collapsed}
        onCollapse={setCollapsed}
        breakpoint="lg"
        width={320}
        style={{ background: "var(--ant-color-bg-container)" }}
      >
        <Flex align="center" gap={16} style={{ padding: 32 }}>
          <Avatar
            shape="square"
            size={48}
            style={{
              background: "var(--ant-color-primary)",
              // Ink on the lime fill: white is 1.99:1.
              color: ON_PRIMARY,
              fontSize: 24,
              fontWeight: 700,
              flexShrink: 0,
            }}
          >
            U
          </Avatar>
          {!collapsed && (
            <Space orientation="vertical" size={4}>
              <Typography.Text
                strong
                style={{
                  margin: 0,
                  textTransform: "uppercase",
                  fontSize: 18,
                  letterSpacing: "-0.025em",
                }}
              >
                PayU
              </Typography.Text>
              <Typography.Text
                strong
                style={{
                  fontSize: 12,
                  letterSpacing: "0.1em",
                  textTransform: "uppercase",
                  // Lime as text is 1.47:1 on white — the deep positive-green.
                  color: "var(--ant-color-primary-text)",
                }}
              >
                Backoffice
              </Typography.Text>
            </Space>
          )}
        </Flex>
        <AntMenu
          mode="inline"
          selectedKeys={[selectedKey]}
          style={{ borderInlineEnd: "none" }}
          items={GROUP_ORDER.flatMap((group) => {
            const items = navigation.filter((n) => n.group === group);
            if (items.length === 0) return [];
            return [
              {
                key: `group-${group}`,
                type: "group" as const,
                label: collapsed ? undefined : group,
                children: items.map((item) => ({
                  key: item.href,
                  icon: <item.icon style={{ fontSize: 20 }} />,
                  label: <Link href={item.href}>{item.name}</Link>,
                })),
              },
            ];
          })}
        />
      </Layout.Sider>

      <Layout>
        <Layout.Header
          className="payu-header"
          style={{
            background: "var(--ant-color-bg-container)",
            padding: "0 16px",
            borderBottom: "1px solid var(--ant-color-border-secondary)",
          }}
        >
          <Flex
            align="center"
            justify="space-between"
            wrap
            gap={16}
            style={{ height: "100%" }}
          >
            <Flex align="center" gap={24}>
              <Button
                type="text"
                onClick={() => setCollapsed((c) => !c)}
                icon={<Menu style={{ fontSize: 24 }} />}
                aria-label="Buka menu navigasi"
              />
              <Space orientation="vertical" size={4} style={{ lineHeight: 1.4 }}>
                <Typography.Text strong style={{ margin: 0, fontSize: 18 }}>
                  {activeNav?.name || "Dashboard"}
                </Typography.Text>
                <Typography.Text
                  type="secondary"
                  strong
                  style={{
                    fontSize: 12,
                    letterSpacing: "0.1em",
                    textTransform: "uppercase",
                  }}
                >
                  Secured Administrator Access
                </Typography.Text>
              </Space>
            </Flex>

            <Flex align="center" gap={24}>
              <Input
                aria-label="Pencarian Universal Admin"
                placeholder="Universal Admin Search..."
                className="payu-backoffice-search"
                prefix={
                  <Search
                    style={{
                      fontSize: 16,
                      color: "var(--ant-color-text-secondary)",
                    }}
                  />
                }
                style={{ width: "100%", maxWidth: 320, minWidth: 200 }}
              />
              <Badge dot>
                <Button
                  type="text"
                  icon={
                    <Bell
                      style={{
                        fontSize: 20,
                        color: "var(--ant-color-text-secondary)",
                      }}
                    />
                  }
                  aria-label="Notifikasi"
                />
              </Badge>
              {/* The console is the one shell without a theme control, so a
                  user who lands here directly could never reach dark mode. */}
              <ThemeToggle />
              <Flex align="center" gap={16}>
                <Space
                  orientation="vertical"
                  size={0}
                  style={{ textAlign: "right", lineHeight: 1.4 }}
                >
                  <Typography.Text
                    strong
                    style={{ fontSize: 12, textTransform: "uppercase" }}
                  >
                    Administrator
                  </Typography.Text>
                  <Typography.Text
                    strong
                    style={{
                      fontSize: 12,
                      letterSpacing: "0.1em",
                      textTransform: "uppercase",
                      // Lime as text is 1.47:1 on white — the deep positive-green.
                      color: "var(--ant-color-primary-text)",
                    }}
                  >
                    Super User
                  </Typography.Text>
                </Space>
                <Avatar
                  size={48}
                  style={{
                    background: "var(--ant-color-primary-bg)",
                    color: "var(--ant-color-primary-text)",
                    fontWeight: 700,
                  }}
                >
                  AD
                </Avatar>
              </Flex>
            </Flex>
          </Flex>
        </Layout.Header>

        <Layout.Content id="main-content" className="payu-backoffice-content" style={{ overflowY: "auto", background: "var(--ant-color-bg-layout)" }}>
          {children}
        </Layout.Content>
      </Layout>
    </Layout>
  );
}
