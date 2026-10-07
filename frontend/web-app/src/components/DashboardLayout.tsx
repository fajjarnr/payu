"use client";

import React, { useState, useSyncExternalStore } from "react";
import { Link, usePathname, useRouter } from "@/lib/navigation";
import {
  LayoutDashboard,
  BarChart3,
  ArrowRightLeft,
  Wallet,
  CreditCard,
  ShieldCheck,
  Settings,
  LifeBuoy,
  Bell,
  Menu as MenuIcon,
  User,
  LogOut,
  QrCode,
  Receipt,
  TrendingUp,
  Calendar,
  History,
  Search,
} from "@/components/icons";
import { useTranslations, useLocale } from "next-intl";
import { useUIStore } from "@/stores";
import {
  Layout,
  Menu,
  Dropdown,
  Badge,
  Avatar,
  Input,
  Button,
  Drawer,
  theme,
} from "antd";
import type { MenuProps } from "antd";
import MobileNav from "./MobileNav";
import LanguageSwitcher from "./LanguageSwitcher";
import ThemeToggle from "./ThemeToggle";
import { PersonalizedGreeting } from "./personalization";
import { useLogout } from "@/hooks";

const { Sider, Header, Content } = Layout;

const emptySubscribe = () => () => {};

interface DashboardLayoutProps {
  children?: React.ReactNode;
  username?: string;
  onLogout?: () => void;
}

export default function DashboardLayout({
  children,
  username = "Pengguna",
  onLogout,
}: DashboardLayoutProps) {
  const t = useTranslations("nav");
  const locale = useLocale();
  const pathname = usePathname();
  const router = useRouter();
  const logoutMutation = useLogout();
  const mode = useUIStore((s) => s.theme);
  const { token } = theme.useToken();
  const mounted = useSyncExternalStore(
    emptySubscribe,
    () => true,
    () => false,
  );
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [searchOpen, setSearchOpen] = useState(false);

  const mainMenu = [
    { href: "/dashboard", icon: LayoutDashboard, label: t("dashboard") },
    { href: "/pockets", icon: Wallet, label: t("accounts") },
    { href: "/transfer", icon: ArrowRightLeft, label: t("transfers") },
    { href: "/transactions", icon: History, label: t("history") || "Riwayat" },
    {
      href: "/scheduled-transfers",
      icon: Calendar,
      label: t("scheduled") || "Terjadwal",
    },
    { href: "/exchange", icon: TrendingUp, label: t("exchange") },
    { href: "/qris", icon: QrCode, label: t("qrPayment") },
    { href: "/bills", icon: Receipt, label: t("bills") },
    { href: "/cards", icon: CreditCard, label: t("cards") },
    { href: "/investments", icon: TrendingUp, label: t("investments") },
    { href: "/analytics", icon: BarChart3, label: t("analytics") },
  ];

  const otherMenu = [
    { href: "/security", icon: ShieldCheck, label: t("security") },
    { href: "/settings", icon: Settings, label: t("settings") },
    { href: "/support", icon: LifeBuoy, label: t("support") },
  ];

  const selectedKeys = [
    ...mainMenu
      .filter(
        (item) =>
          pathname === item.href ||
          (item.href.endsWith("/dashboard") && pathname.endsWith("/dashboard")),
      )
      .map((item) => item.href),
    ...otherMenu
      .filter((item) => pathname.includes(item.href))
      .map((item) => item.href),
  ];

  const menuItems: MenuProps["items"] = [
    {
      type: "group",
      label: t("main"),
      children: mainMenu.map(({ href, icon: Icon, label }) => ({
        key: href,
        icon: <Icon className="h-5 w-5" aria-hidden="true" />,
        label,
      })),
    },
    {
      type: "group",
      label: t("others"),
      children: otherMenu.map(({ href, icon: Icon, label }) => ({
        key: href,
        icon: <Icon className="h-5 w-5" aria-hidden="true" />,
        label,
      })),
    },
  ];

  const handleMenuClick: MenuProps["onClick"] = ({ key }) => {
    setDrawerOpen(false);
    router.push(`/${locale}${key}`);
  };

  const profileItems: MenuProps["items"] = [
    {
      key: "identity",
      disabled: true,
      label: (
        <div className="py-1">
          <p className="mb-1 text-[10px] font-bold uppercase tracking-[0.3em] text-primary/50">
            Authenticated User
          </p>
          <p className="truncate text-lg font-bold uppercase tracking-tight">
            {username}
          </p>
        </div>
      ),
    },
    { type: "divider" },
    {
      key: "logout",
      danger: true,
      icon: <LogOut className="h-5 w-5" aria-hidden="true" />,
      label: t("logout"),
      onClick: () => (onLogout ? onLogout() : logoutMutation.mutate()),
    },
  ];

  const logoMark = (
    <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl bg-primary text-2xl font-bold text-surface shadow-lg">
      U
    </div>
  );

  const sidebarMenu = (
    <Menu
      mode="inline"
      selectedKeys={selectedKeys}
      items={menuItems}
      onClick={handleMenuClick}
      style={{ borderInlineEnd: "none", background: "transparent" }}
    />
  );
  return (
    <>
      {/* ponytail: one <style> for vh->dvh fallback + responsive header/content rules;
          inline styles can't express fallback declarations or media queries. */}
      <style>{`
        .payu-full-h { height: 100vh; height: 100dvh; }
        .payu-header { height: 64px !important; padding: 0 16px !important; }
        @media (min-width: 640px) {
          .payu-header { height: 80px !important; padding: 0 24px !important; }
        }
        .payu-content-inner { padding-bottom: calc(96px + env(safe-area-inset-bottom, 0px)) !important; }
        @media (min-width: 1024px) {
          .payu-content-inner { padding-bottom: 24px !important; }
        }
      `}</style>
      <Layout hasSider className="payu-full-h" style={{ overflow: "hidden" }}>
        <Sider
          breakpoint="lg"
          collapsedWidth={0}
          collapsible
          trigger={null}
          width={288}
          theme={mounted && mode === "dark" ? "dark" : "light"}
          className="payu-full-h"
          style={{
            overflowY: "auto",
            background: token.colorBgContainer,
            borderRight: `1px solid ${token.colorBorderSecondary}`,
          }}
        >
          <Link
            href="/"
            className="flex items-center gap-4 px-2 py-4"
            aria-label="PayU"
          >
            {logoMark}
            <span className="text-2xl font-bold uppercase tracking-tighter">
              PayU
            </span>
          </Link>
          {sidebarMenu}
        </Sider>

        <Layout className="payu-full-h" style={{ overflow: "hidden" }}>
          <Header
            className="payu-header"
            style={{
              display: "flex",
              alignItems: "center",
              justifyContent: "space-between",
              gap: 12,
              lineHeight: "normal",
              background: token.colorBgContainer,
              borderBottom: `1px solid ${token.colorBorderSecondary}`,
              position: "sticky",
              top: 0,
              zIndex: 30,
            }}
          >
            <div className="flex min-w-0 flex-1 items-center gap-3">
              <Button
                type="text"
                className="lg:hidden"
                icon={<MenuIcon className="h-5 w-5" aria-hidden="true" />}
                onClick={() => setDrawerOpen(true)}
                aria-label="Buka menu navigasi"
                data-testid="mobile-menu-trigger"
              />
              <div className="hidden flex-col justify-center sm:flex">
                <PersonalizedGreeting
                  showTimeBased={true}
                  showSegment={true}
                  className="text-lg font-bold leading-tight"
                />
                <p className="ml-0.5 mt-1 text-[10px] font-bold uppercase tracking-[0.2em] text-muted-foreground opacity-60">
                  AI Financial Forecaster Active
                </p>
              </div>
            </div>

            <div className="flex shrink-0 items-center gap-2 sm:gap-4">
              <ThemeToggle />
              <LanguageSwitcher />

              <Input.Search
                aria-label="Pencarian cerdas"
                data-testid="search-input"
                placeholder="Pencarian cerdas..."
                allowClear
                className="hidden w-80 xl:flex"
              />

              {/* Below xl the inline search collapses; keyboard users reach it
                through this 44px trigger and the drawer it opens. */}
              <Button
                type="text"
                className="xl:hidden"
                data-testid="search-trigger"
                icon={<Search className="h-5 w-5" aria-hidden="true" />}
                aria-label="Buka pencarian"
                onClick={() => setSearchOpen(true)}
                style={{ width: 44, height: 44 }}
              />

              <Badge dot color={token.colorPrimary} offset={[-4, 4]}>
                <Button
                  type="text"
                  data-testid="notification-button"
                  icon={<Bell className="h-5 w-5" aria-hidden="true" />}
                  aria-label="Notifikasi"
                  onClick={() => router.push(`/${locale}/notifications`)}
                  style={{ width: 44, height: 44 }}
                />
              </Badge>

              <Dropdown
                menu={{ items: profileItems }}
                trigger={["click"]}
                placement="bottomRight"
              >
                <Button
                  type="text"
                  data-testid="profile-menu-trigger"
                  aria-label="Menu profil pengguna"
                  style={{ width: 48, height: 48, padding: 0 }}
                >
                  <Avatar
                    size={40}
                    icon={<User className="h-5 w-5" aria-hidden="true" />}
                    style={{
                      background: token.colorPrimaryBg,
                      color: token.colorPrimary,
                    }}
                  />
                </Button>
              </Dropdown>
            </div>
          </Header>

          <Content
            style={{
              flex: 1,
              overflowY: "auto",
              overflowX: "hidden",
              background: token.colorBgLayout,
            }}
          >
            <div className="payu-content-inner mx-auto w-full max-w-[1200px] px-4 py-4 sm:px-6 sm:py-6">
              {children}
            </div>
          </Content>
        </Layout>

        <Drawer
          placement="left"
          open={drawerOpen}
          onClose={() => setDrawerOpen(false)}
          width={288}
          title={
            <Link
              href="/"
              className="flex items-center gap-4 px-2 py-4"
              aria-label="PayU"
            >
              {logoMark}
            </Link>
          }
          styles={{
            header: { borderBottom: `1px solid ${token.colorBorderSecondary}` },
            body: { padding: 0, background: token.colorBgContainer },
          }}
        >
          {sidebarMenu}
        </Drawer>

        <Drawer
          placement="top"
          open={searchOpen}
          onClose={() => setSearchOpen(false)}
          height="auto"
          title="Pencarian cerdas"
          styles={{ body: { padding: 16 } }}
        >
          <Input.Search
            aria-label="Pencarian cerdas"
            data-testid="search-input-mobile"
            placeholder="Pencarian cerdas..."
            allowClear
            autoFocus
            className="w-full"
          />
        </Drawer>

        <MobileNav />
      </Layout>
    </>
  );
}
