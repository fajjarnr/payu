"use client";

import { Link } from "@/lib/navigation";
import { useTranslations } from "next-intl";
import { Menu, X } from "@/components/icons";
import { Button, Flex } from "antd";
import { useState } from "react";

export const NAV_TARGETS = ["features", "how", "about"] as const;

export interface LandingNavProps {
  /** Brand mark rendered by the server (serializable node). */
  brand: React.ReactNode;
}

/**
 * Landing nav-bar (Wise `nav-bar`): sticky white bar, ink 14/600 links.
 * The ONLY client leaf in the landing header — everything else is server.
 */
export function LandingNav({ brand }: LandingNavProps) {
  const t = useTranslations("landing");
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  const handleNavClick = (e: React.MouseEvent, targetId: string) => {
    e.preventDefault();
    document.getElementById(targetId)?.scrollIntoView({ behavior: "smooth" });
    setMobileMenuOpen(false);
  };

  const navLabels: Record<(typeof NAV_TARGETS)[number], string> = {
    features: t("nav.features"),
    how: t("how.title"),
    about: t("nav.about"),
  };

  return (
    <header
      style={{
        position: "sticky",
        top: 0,
        zIndex: 50,
        borderBottom: `1px solid var(--ant-color-border-secondary)`,
        background: "var(--ant-color-bg-container)",
      }}
    >
      <style>{`
        .payu-landing-bar { height: 72px !important; }
        .payu-landing-links { display: none !important; }
        .payu-landing-ctas { display: none !important; }
        @media (min-width: 640px) {
          .payu-landing-ctas { display: inline-flex !important; }
        }
        @media (min-width: 768px) {
          .payu-landing-links { display: inline-flex !important; }
          .payu-landing-burger { display: none !important; }
        }
      `}</style>
      <Flex
        align="center"
        justify="space-between"
        gap={16}
        className="payu-landing-bar"
        style={{ maxWidth: 1200, margin: "0 auto", padding: "0 16px" }}
      >
        <Link
          href={"/"}
          aria-label="PayU Home"
          style={{ cursor: "pointer", textDecoration: "none", color: "inherit" }}
        >
          {brand}
        </Link>

        <Flex
          component="nav"
          align="center"
          gap={32}
          aria-label="Main"
          className="payu-landing-links"
        >
          {NAV_TARGETS.map((target) => (
            <Button
              key={target}
              type="text"
              href={`#${target}`}
              onClick={(e) => handleNavClick(e, target)}
              style={{ fontWeight: 600, fontSize: 14 }}
            >
              {navLabels[target]}
            </Button>
          ))}
        </Flex>

        <Flex align="center" gap={10}>
          <span className="payu-landing-ctas" style={{ gap: 10, alignItems: "center" }}>
            <Link href={"/login"} style={{ cursor: "pointer", textDecoration: "none" }}>
              <Button type="default">{t("nav.login")}</Button>
            </Link>
            <Link
              href={"/onboarding"}
              style={{ cursor: "pointer", textDecoration: "none" }}
            >
              <Button type="primary">{t("getStarted")}</Button>
            </Link>
          </span>
          <Button
            type="default"
            className="payu-landing-burger"
            onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
            aria-expanded={mobileMenuOpen}
            aria-label={mobileMenuOpen ? "Close menu" : "Open menu"}
            icon={
              mobileMenuOpen ? (
                <X style={{ fontSize: 20 }} />
              ) : (
                <Menu style={{ fontSize: 20 }} />
              )
            }
            style={{ width: 44, height: 44 }}
          />
        </Flex>
      </Flex>

      {mobileMenuOpen && (
        <nav
          aria-label="Mobile"
          style={{
            borderTop: `1px solid var(--ant-color-border-secondary)`,
            background: "var(--ant-color-bg-container)",
          }}
        >
          <Flex
            vertical
            gap={4}
            style={{ maxWidth: 1200, margin: "0 auto", padding: "16px" }}
          >
            {NAV_TARGETS.map((target) => (
              <Button
                key={target}
                type="text"
                href={`#${target}`}
                onClick={(e) => handleNavClick(e, target)}
                style={{
                  justifyContent: "flex-start",
                  height: 44,
                  fontWeight: 600,
                  fontSize: 14,
                }}
              >
                {navLabels[target]}
              </Button>
            ))}
            <Flex gap={10} style={{ marginTop: 8 }}>
              <Link
                href={"/login"}
                style={{ cursor: "pointer", textDecoration: "none", flex: 1 }}
              >
                <Button type="default" block>
                  {t("nav.login")}
                </Button>
              </Link>
              <Link
                href={"/onboarding"}
                style={{ cursor: "pointer", textDecoration: "none", flex: 1 }}
              >
                <Button type="primary" block>
                  {t("getStarted")}
                </Button>
              </Link>
            </Flex>
          </Flex>
        </nav>
      )}
    </header>
  );
}
