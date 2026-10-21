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
  Gift,
  Landmark,
  Bank,
  FileSearch,
  Partition,
  Search,
} from "@/components/icons";
import { useTranslations, useLocale } from "next-intl";
import { useUIStore, useNotificationStore } from "@/stores";
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
import { SkipLink } from "@/lib/a11y";
import { INK_ON_EMERALD } from "@/theme/tokens";
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
  const unreadCount = useNotificationStore((s) => s.unreadCount);
  const { token } = theme.useToken();
  const mounted = useSyncExternalStore(
    emptySubscribe,
    () => true,
    () => false,
  );
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [searchOpen, setSearchOpen] = useState(false);

  const overviewMenu = [
    { href: "/dashboard", icon: LayoutDashboard, label: t("dashboard") },
    { href: "/analytics", icon: BarChart3, label: t("analytics") },
  ];

  const transactionMenu = [
    { href: "/transfer", icon: ArrowRightLeft, label: t("transfers") },
    { href: "/transactions", icon: History, label: t("history") },
    { href: "/scheduled-transfers", icon: Calendar, label: t("scheduled") },
    { href: "/qris", icon: QrCode, label: t("qrPayment") },
    { href: "/bills", icon: Receipt, label: t("bills") },
    { href: "/split-bill", icon: Partition, label: t("splitBill") },
  ];

  const financeMenu = [
    { href: "/pockets", icon: Wallet, label: t("accounts") },
    { href: "/cards", icon: CreditCard, label: t("cards") },
    { href: "/investments", icon: TrendingUp, label: t("investments") },
    { href: "/exchange", icon: Landmark, label: t("exchange") },
    { href: "/lending", icon: Bank, label: t("lending") },
    { href: "/rewards", icon: Gift, label: t("rewards") },
  ];

  const otherMenu = [
    { href: "/security", icon: ShieldCheck, label: t("security") },
    { href: "/settings", icon: Settings, label: t("settings") },
    { href: "/notifications", icon: Bell, label: t("notificationsMenu") },
    { href: "/statements", icon: FileSearch, label: t("statements") },
    { href: "/support", icon: LifeBuoy, label: t("support") },
  ];

  const allMenus = [...overviewMenu, ...transactionMenu, ...financeMenu, ...otherMenu];

  // next-intl `as-needed` keeps `/id` unprefixed: strip a leading locale
  // segment before comparing, so active state works in both locales.
  const barePathname =
    pathname === "/id" ||
    pathname === "/en" ||
    pathname.startsWith("/id/") ||
    pathname.startsWith("/en/")
      ? pathname.replace(/^\/(id|en)(?=\/|$)/, "") || "/"
      : pathname;

  // Segment match: child routes highlight their parent menu item; the
  // dashboard special-case survives for locale-stripped root paths.
  const selectedKeys = allMenus
    .filter(
      (item) =>
        barePathname === item.href ||
        (item.href !== "/" && barePathname.startsWith(`${item.href}/`)) ||
        (item.href.endsWith("/dashboard") &&
          barePathname.endsWith("/dashboard")),
    )
    .map((item) => item.href);

  const toMenuItems = (entries: { href: string; icon: React.ElementType; label: string }[]) =>
    entries.map(({ href, icon: Icon, label }) => ({
      key: href,
      icon: <Icon className="h-5 w-5" aria-hidden="true" />,
      label: (
        <Link href={href} onClick={() => setDrawerOpen(false)}>
          {label}
        </Link>
      ),
    }));

  const menuItems: MenuProps["items"] = [
    { type: "group", label: t("overview"), children: toMenuItems(overviewMenu) },
    { type: "group", label: t("transactions"), children: toMenuItems(transactionMenu) },
    { type: "group", label: t("finance"), children: toMenuItems(financeMenu) },
    { type: "group", label: t("others"), children: toMenuItems(otherMenu) },
  ];

  const profileItems: MenuProps["items"] = [
    {
      key: "identity",
      disabled: true,
      label: (
        <div className="py-1">
          <p className="mb-1 text-[10px] font-bold uppercase tracking-[0.3em]" style={{ color: "var(--ant-color-text-tertiary)" }}>
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
    <div
      className="flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl text-2xl font-bold shadow-lg"
      style={{ backgroundColor: "var(--ant-color-primary)", color: INK_ON_EMERALD }}
    >
      U
    </div>
  );

  const sidebarMenu = (label: string) => (
    <nav aria-label={label}>
      <Menu
        mode="inline"
        selectedKeys={selectedKeys}
        items={menuItems}
        onClick={() => setDrawerOpen(false)}
        style={{
          borderInlineEnd: "none",
          background: "transparent",
          // 44px touch floor (DESIGN.md): menu rows stretch to the minimum.
          ["--ant-menu-item-height" as string]: "44px",
        }}
      />
    </nav>
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
        .payu-sidebar-menu .ant-menu-title-content { min-height: 44px; display: inline-flex; align-items: center; }
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
            aria-label={t("homeBrand")}
          >
            {logoMark}
            <span className="text-2xl font-bold uppercase tracking-tighter" style={{ color: token.colorText }}>
              PayU
            </span>
          </Link>
          <div className="payu-sidebar-menu">{sidebarMenu(t("sidebarNav"))}</div>
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
                aria-label={t("openMenu")}
                data-testid="mobile-menu-trigger"
                // The header row squeezes its first group on narrow screens;
                // without this the menu button shrinks to 22px, below the 24px
                // target floor (WCAG 2.5.8) — and it is the only nav on mobile.
                style={{ width: 44, height: 44, flexShrink: 0 }}
              />
              <div className="hidden flex-col justify-center sm:flex">
                <PersonalizedGreeting
                  showTimeBased={true}
                  showSegment={true}
                  className="text-lg font-bold leading-tight"
                />
                <p className="ml-0.5 mt-1 text-[10px] font-bold uppercase tracking-[0.2em]" style={{ color: token.colorTextSecondary }}>
                  {t("aiForecaster")}
                </p>
              </div>
            </div>

            <div className="flex shrink-0 items-center gap-2 sm:gap-4">
              {/* Below sm the header cannot hold every control: the action
                  group alone needs 333px, which squeezes the menu button
                  (the only nav on mobile) out of the 360px viewport. Theme
                  and language stay available in /settings. */}
              <span className="hidden sm:contents">
                <ThemeToggle />
                <LanguageSwitcher />
              </span>

              <Input.Search
                aria-label={t("smartSearchDesktop")}
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
                aria-label={t("openSearch")}
                onClick={() => setSearchOpen(true)}
                style={{ width: 44, height: 44, flexShrink: 0 }}
              />

              <Badge dot={unreadCount > 0} count={unreadCount > 0 ? unreadCount : 0} color={token.colorPrimary} offset={[-4, 4]}>
                <Button
                  type="text"
                  data-testid="notification-button"
                  icon={<Bell className="h-5 w-5" aria-hidden="true" />}
                  aria-label={t("notificationsLabel")}
                  onClick={() => router.push(`/${locale}/notifications`)}
                  style={{ width: 44, height: 44, flexShrink: 0 }}
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
                  aria-label={t("profileMenu")}
                  style={{ width: 48, height: 48, padding: 0, flexShrink: 0 }}
                >
                  <Avatar
                    size={40}
                    icon={<User className="h-5 w-5" aria-hidden="true" />}
                    style={{
                      background: token.colorPrimaryBg,
                      // Deep brand green: the emerald on its own tint is 1.9:1.
                      color: token.colorPrimaryText,
                    }}
                  />
                </Button>
              </Dropdown>
            </div>
          </Header>

          <Content
            id="main-content"
            style={{
              flex: 1,
              overflowY: "auto",
              overflowX: "hidden",
              background: token.colorBgLayout,
            }}
          >
            {/* Inside the main landmark: outside it the skip link was the one
                piece of page content not contained by a landmark (axe region). */}
            <SkipLink href="#main-content" />
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
            // Drawer swaps places with the Sider below lg; its brand visual
            // stays out of the a11y tree so only one logo landmark exists.
            <span aria-hidden="true" className="flex items-center gap-4 px-2 py-4">
              {logoMark}
            </span>
          }
          styles={{
            header: { borderBottom: `1px solid ${token.colorBorderSecondary}` },
            body: { padding: 0, background: token.colorBgContainer },
          }}
        >
          <div className="payu-sidebar-menu">{sidebarMenu(t("sidebarNav"))}</div>
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
            aria-label={t("smartSearchMobile")}
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
