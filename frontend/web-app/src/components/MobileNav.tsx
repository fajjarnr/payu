"use client";

import { usePathname, Link } from "@/lib/navigation";
import { useTranslations } from "next-intl";
import { Home, Repeat, QrCode, CreditCard, Settings } from "@/components/icons";
import { theme } from "antd";
import { useIsAuthenticated } from "@/stores";
export default function MobileNav() {
  const t = useTranslations("nav");
  const pathname = usePathname();
  const isAuthenticated = useIsAuthenticated();
  const { token } = theme.useToken();

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

  const isActive = (href: string) =>
    barePathname === href ||
    (href.endsWith("/dashboard") && barePathname.endsWith("/dashboard"));

  return (
    <nav
      data-testid="mobile-nav"
      aria-label={t("mobileNavigation")}
      className="fixed bottom-0 left-0 right-0 z-50 border-t px-2 pt-2 pb-[max(0.5rem,env(safe-area-inset-bottom))] rounded-t-2xl sm:rounded-t-3xl sm:px-4 lg:hidden"
      style={{
        backgroundColor: token.colorBgContainer,
        borderTopColor: token.colorBorderSecondary,
        boxShadow: "0 -8px 30px rgba(0,0,0,0.12)",
      }}
    >
      <ul
        className="flex w-full"
        style={{ maxWidth: 512, margin: "0 auto", listStyle: "none", padding: 0 }}
      >
        {navItems.map(({ href, icon: Icon, label }) => {
          const active = isActive(href);
          return (
            <li key={href} style={{ flex: 1, display: "flex" }}>
              <Link
                href={href}
                data-testid={`mobile-nav-${label.toLowerCase()}`}
                aria-current={active ? "page" : undefined}
                aria-label={label}
                className="flex min-h-[44px] w-full flex-col items-center justify-center gap-1 py-2"
                style={{ position: "relative" }}
              >
                {active && (
                  <span
                    data-testid="mobile-nav-active-indicator"
                    aria-hidden="true"
                    style={{
                      position: "absolute",
                      top: 2,
                      width: 24,
                      height: 3,
                      borderRadius: 2,
                      backgroundColor: "var(--ant-color-primary)",
                    }}
                  />
                )}
                <Icon
                  className={
                    active
                      ? "h-5 w-5 shrink-0 stroke-[2.5px] sm:h-6 sm:w-6"
                      : "h-5 w-5 shrink-0 stroke-[2px] sm:h-6 sm:w-6"
                  }
                  aria-hidden="true"
                />
                <span
                  className="w-full truncate px-0.5 text-center text-xs uppercase leading-none tracking-[0.08em] sm:tracking-[0.12em]"
                  style={{ fontWeight: active ? 800 : 700 }}
                >
                  {label}
                </span>
              </Link>
            </li>
          );
        })}
      </ul>
    </nav>
  );
}
