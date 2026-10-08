"use client";

import { usePathname, Link } from "@/lib/navigation";
import { useTranslations } from "next-intl";
import { Home, Repeat, QrCode, CreditCard, Settings } from "@/components/icons";
import { Menu } from "antd";
import type { MenuProps } from "antd";
import { useIsAuthenticated } from "@/stores";

/** Uses the auth store to check authentication status; never accesses tokens from localStorage. */
export default function MobileNav() {
  const t = useTranslations("nav");
  const pathname = usePathname();
  const isAuthenticated = useIsAuthenticated();

  const navItems = [
    { href: "/dashboard", icon: Home, label: t("dashboard") },
    { href: "/transfer", icon: Repeat, label: t("transfers") },
    { href: "/qris", icon: QrCode, label: t("qrPayment") },
    { href: "/cards", icon: CreditCard, label: t("cards") },
    { href: "/settings", icon: Settings, label: t("settings") },
  ];

  // Don't show nav on login or onboarding
  if (pathname.includes("/login") || pathname.includes("/onboarding"))
    return null;

  // Don't show if not authenticated
  if (!isAuthenticated) return null;

  // next-intl `as-needed` keeps `/id` unprefixed: strip a leading locale
  // segment before comparing, so active state works in both locales.
  const barePathname =
    pathname === "/id" ||
    pathname === "/en" ||
    pathname.startsWith("/id/") ||
    pathname.startsWith("/en/")
      ? pathname.replace(/^\/(id|en)(?=\/|$)/, "") || "/"
      : pathname;

  const selectedKeys = navItems
    .filter(
      (item) =>
        barePathname === item.href ||
        (item.href.endsWith("/dashboard") &&
          barePathname.endsWith("/dashboard")),
    )
    .map((item) => item.href);

  const items: MenuProps["items"] = navItems.map(
    ({ href, icon: Icon, label }) => {
      const isActive = selectedKeys.includes(href);
      return {
        key: href,
        label: (
          <Link
            href={href}
            data-testid={`mobile-nav-${label.toLowerCase()}`}
            aria-current={isActive ? "page" : undefined}
            className="flex min-h-[44px] w-full flex-col items-center justify-center gap-1 py-2"
          >
            <Icon
              className={
                isActive
                  ? "h-5 w-5 shrink-0 stroke-[2.5px] sm:h-6 sm:w-6"
                  : "h-5 w-5 shrink-0 stroke-[2px] sm:h-6 sm:w-6"
              }
              aria-hidden="true"
            />
            <span className="w-full truncate px-0.5 text-center text-xs font-bold uppercase leading-none tracking-[0.08em] sm:tracking-[0.12em]">
              {label}
            </span>
          </Link>
        ),
      };
    },
  );

  return (
    <div
      data-testid="mobile-nav"
      role="navigation"
      aria-label={t("mobileNavigation")}
      className="fixed bottom-0 left-0 right-0 z-50 border-t border-border bg-card/95 px-2 pt-2 pb-[max(0.5rem,env(safe-area-inset-bottom))] shadow-[0_-8px_30px_rgba(0,0,0,0.12)] rounded-t-2xl sm:rounded-t-3xl sm:px-4 lg:hidden"
    >
      <Menu
        mode="horizontal"
        disabledOverflow
        selectedKeys={selectedKeys}
        items={items}
        className="flex w-full"
        style={{
          maxWidth: 512,
          margin: "0 auto",
          background: "transparent",
          borderBottom: "none",
          lineHeight: "normal",
        }}
        styles={{
          list: {
            display: "flex",
            flexDirection: "row",
            justifyContent: "space-around",
            width: "100%",
          },
          item: {
            flex: 1,
            display: "flex",
            height: "auto",
            padding: "8px 2px",
          },
        }}
      />
    </div>
  );
}
