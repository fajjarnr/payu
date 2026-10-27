import { Link } from "@/lib/navigation";
import { getTranslations } from "next-intl/server";
import { Button, Col, Divider, Flex, Row, Tag } from "antd";
import { HeroTitle } from "@/components/landing/HeroTitle";
import { LandingNav } from "@/components/landing/LandingNav";
import { ConverterCard } from "@/components/landing/ConverterCard";
import { LandingIcon, type LandingIconName } from "@/components/landing/LandingIcon";
import { INK_SURFACE, INK_SURFACE_SOFT, INK_SURFACE_BORDER, LIME_ACCENT, SAGE_ON_INK } from "@/theme/tokens";
import type { CSSProperties, ReactNode } from "react";

const CONTENT_WIDTH = 1200;

// Server-safe type: antd `Typography` and `Card` call client-only context at
// module level, so the server landing renders semantic HTML with the same
// Wise roles (display-sm 32/600, display-xs 24/600, body 16/14).
function H2({ children, style }: { children: ReactNode; style?: CSSProperties }) {
  return (
    <h2 style={{ margin: 0, fontSize: 32, fontWeight: 600, letterSpacing: "-0.96px", lineHeight: 1.2, ...style }}>
      {children}
    </h2>
  );
}

function H3({ children, style }: { children: ReactNode; style?: CSSProperties }) {
  return (
    <h3 style={{ margin: 0, fontSize: 24, fontWeight: 600, letterSpacing: "-0.48px", lineHeight: 1.3, ...style }}>
      {children}
    </h3>
  );
}

function Body({ children, style, muted }: { children: ReactNode; style?: CSSProperties; muted?: boolean }) {
  return (
    <p
      style={{
        margin: 0,
        fontSize: 16,
        lineHeight: 1.5,
        color: muted ? "var(--ant-color-text-secondary)" : undefined,
        ...style,
      }}
    >
      {children}
    </p>
  );
}

function ServerCard({ children, style }: { children: ReactNode; style?: CSSProperties }) {
  return (
    <div
      style={{
        background: "var(--ant-color-bg-container)",
        borderRadius: 24,
        padding: 24,
        height: "100%",
        ...style,
      }}
    >
      {children}
    </div>
  );
}

/**
 * Wise re-skin 2026-10-27: the landing page is a Server Component. Client
 * leaves — `LandingNav` (mobile menu + smooth scroll), `ConverterCard`
 * (demo-local amount state), `LandingIcon` (antd icons are client-only).
 * Sections follow wise/DESIGN.md: sage `hero-band` with Manrope display +
 * converter card, white `content-band` feature grid, `card-feature-green`
 * how-it-works, `card-feature-dark` security, CTA, ink `footer`.
 */
export default async function LandingPage() {
  const t = await getTranslations("landing");

  const features: Array<{ icon: LandingIconName; title: string; desc: string }> = [
    { icon: "barChart3", title: t("slide2.analytics.title"), desc: t("slide2.analytics.desc") },
    { icon: "qrCode", title: t("features.qris.title"), desc: t("features.qris.desc") },
    { icon: "wallet", title: t("features.pockets.title"), desc: t("features.pockets.desc") },
    { icon: "receiptText", title: t("features.bills.title"), desc: t("features.bills.desc") },
    { icon: "globe", title: t("slide2.connectivity.title"), desc: t("slide2.connectivity.desc") },
    { icon: "shield", title: t("slide2.security.title"), desc: t("slide2.security.desc") },
  ];

  const steps = [
    { n: 1, title: t("how.step1.title"), desc: t("how.step1.desc") },
    { n: 2, title: t("how.step2.title"), desc: t("how.step2.desc") },
    { n: 3, title: t("how.step3.title"), desc: t("how.step3.desc") },
  ];

  const securityItems: Array<{ icon: LandingIconName; label: string }> = [
    { icon: "lock", label: t("secure.item1") },
    { icon: "fingerprint", label: t("secure.item2") },
    { icon: "badgeCheck", label: t("secure.item3") },
  ];

  const brandMark = (
    <Flex align="center" gap={6}>
      <span style={{ fontWeight: 700, fontSize: 20, letterSpacing: "-0.02em" }}>PayU</span>
      <span
        aria-hidden="true"
        style={{
          width: 8,
          height: 8,
          borderRadius: "50%",
          background: "var(--ant-color-primary)",
          display: "inline-block",
        }}
      />
    </Flex>
  );

  return (
    <Flex
      vertical
      style={{ minHeight: "100vh", background: "var(--ant-color-bg-container)", color: "var(--ant-color-text)" }}
    >
      <LandingNav brand={brandMark} />

      <main style={{ flex: 1 }}>
        {/* Hero band: sage surface, display headline, converter card right. */}
        <section
          style={{
            background: "var(--ant-color-bg-layout)",
            padding: "48px 0",
          }}
        >
          <Row
            gutter={[{ xs: 0, lg: 40 }, 40]}
            align="middle"
            style={{ maxWidth: CONTENT_WIDTH, margin: "0 auto", padding: "0 16px" }}
          >
            <Col xs={24} lg={13}>
              <div>
                <Tag
                  className="animate-fade-in"
                  style={{
                    margin: 0,
                    border: "none",
                    borderRadius: 9999,
                    padding: "4px 12px",
                    fontSize: 14,
                    fontWeight: 600,
                    background: "var(--ant-color-primary-bg)",
                    color: "var(--ant-color-primary-text)",
                  }}
                >
                  {t("badge")}
                </Tag>
                <h1
                  className="payu-display animate-fade-in"
                  style={{
                    marginTop: 24,
                    marginBottom: 0,
                    fontSize: "clamp(40px, 6vw, 64px)",
                    animationDelay: "80ms",
                  }}
                >
                  <HeroTitle rawTitle={t.raw("heroTitle") as string} />
                </h1>
                <Body
                  muted
                  style={{ marginTop: 24, maxWidth: 448, fontSize: 20, lineHeight: 1.5 }}
                >
                  <span className="animate-fade-in" style={{ display: "block", animationDelay: "160ms" }}>
                    {t("slide4.subtitle")}
                  </span>
                </Body>
                <Flex wrap align="center" gap={12} className="animate-fade-in" style={{ marginTop: 32, animationDelay: "240ms" }}>
                  <Link href={"/onboarding"} style={{ cursor: "pointer", textDecoration: "none" }}>
                    <Button type="primary" size="large">
                      {t("getStarted")}
                    </Button>
                  </Link>
                  <Link href={"/login"} style={{ cursor: "pointer", textDecoration: "none" }}>
                    <Button type="default" size="large">
                      {t("nav.login")}
                    </Button>
                  </Link>
                </Flex>
                <Body muted style={{ marginTop: 16, fontSize: 12 }}>
                  <span className="animate-fade-in" style={{ display: "block", animationDelay: "300ms" }}>
                    {t("hero.freeAdmin")}
                  </span>
                </Body>
              </div>
            </Col>

            <Col xs={24} lg={11}>
              <ConverterCard />
            </Col>
          </Row>
        </section>

        {/* Features: white content band, 3-up grid. */}
        <section id="features" style={{ scrollMarginTop: 96, padding: "48px 0" }}>
          <div style={{ maxWidth: CONTENT_WIDTH, margin: "0 auto", padding: "0 16px" }}>
            <div style={{ maxWidth: 576 }}>
              <Tag
                style={{
                  margin: 0,
                  border: "none",
                  borderRadius: 9999,
                  padding: "4px 12px",
                  fontSize: 14,
                  fontWeight: 600,
                  background: "var(--ant-color-primary-bg)",
                  color: "var(--ant-color-primary-text)",
                }}
              >
                {t("slide2.badge")}
              </Tag>
              <H2 style={{ marginTop: 20 }}>
                {t("slide2.title")}{" "}
                <span style={{ color: "var(--ant-color-primary-text)" }}>{t("slide2.titleHighlight")}</span>
              </H2>
              <Body muted style={{ marginTop: 16 }}>
                {t("slide2.subtitle")}
              </Body>
            </div>

            <Row gutter={[24, 24]} style={{ marginTop: 32 }}>
              {features.map(({ icon, title, desc }) => (
                <Col key={title} xs={24} sm={12} lg={8}>
                  <ServerCard>
                    <Flex
                      align="center"
                      justify="center"
                      style={{
                        width: 44,
                        height: 44,
                        borderRadius: 12,
                        background: "var(--ant-color-primary-bg)",
                        color: "var(--ant-color-primary-text)",
                      }}
                    >
                      <LandingIcon name={icon} style={{ fontSize: 20 }} />
                    </Flex>
                    <H3 style={{ marginTop: 20 }}>
                      {title}
                    </H3>
                    <Body muted style={{ marginTop: 8, fontSize: 14, lineHeight: 1.6 }}>
                      {desc}
                    </Body>
                  </ServerCard>
                </Col>
              ))}
            </Row>
          </div>
        </section>

        {/* How it works: soft-green feature card. */}
        <section id="how" style={{ scrollMarginTop: 96, padding: "0 0 48px" }}>
          <div style={{ maxWidth: CONTENT_WIDTH, margin: "0 auto", padding: "0 16px" }}>
            <div
              className="payu-how-card"
              style={{ background: "var(--ant-color-success-bg)", borderRadius: 24, padding: 24 }}
            >
              <style>{`@media (min-width: 640px) { .payu-how-card { padding: 56px !important; } }`}</style>
              <H2>
                {t("how.title")}
              </H2>
              <Flex component="ol" gap={24} wrap style={{ listStyle: "none", margin: 0, marginTop: 32, padding: 0 }}>
                {steps.map(({ n, title, desc }) => (
                  <li key={n} style={{ flex: "1 1 240px", position: "relative", paddingLeft: 24, borderLeft: "2px solid var(--ant-color-primary)" }}>
                    <span aria-hidden="true" className="payu-display" style={{ display: "block", fontSize: 48 }}>
                      0{n}
                    </span>
                    <H3 style={{ marginTop: 12 }}>
                      {title}
                    </H3>
                    <Body muted style={{ marginTop: 8, fontSize: 14, lineHeight: 1.6 }}>
                      {desc}
                    </Body>
                  </li>
                ))}
              </Flex>
            </div>
          </div>
        </section>

        {/* Security: polarity-flipped dark card, lime text. */}
        <section id="about" style={{ scrollMarginTop: 96, paddingBottom: 48 }}>
          <div style={{ maxWidth: CONTENT_WIDTH, margin: "0 auto", padding: "0 16px" }}>
            <div
              className="payu-dark-card"
              style={{ background: INK_SURFACE, color: LIME_ACCENT, borderRadius: 24, padding: 24 }}
            >
              <style>{`@media (min-width: 640px) { .payu-dark-card { padding: 56px !important; } }`}</style>
              <Row gutter={[48, 32]} align="middle">
                <Col xs={24} lg={12}>
                  <div>
                    <Tag
                      style={{
                        margin: 0,
                        border: "none",
                        borderRadius: 9999,
                        padding: "4px 12px",
                        fontSize: 14,
                        fontWeight: 600,
                        background: LIME_ACCENT,
                        color: INK_SURFACE,
                      }}
                    >
                      {t("slide3.badge")}
                    </Tag>
                    <H2 style={{ marginTop: 20, color: LIME_ACCENT }}>
                      {t("slide3.title")} <span>{t("slide3.titleHighlight")}</span>
                    </H2>
                    <Body style={{ marginTop: 16, maxWidth: 448, lineHeight: 1.6, color: SAGE_ON_INK }}>
                      {t("slide2.security.desc")}
                    </Body>
                  </div>
                </Col>

                <Col xs={24} lg={12}>
                  <Flex component="ul" vertical gap={16} style={{ margin: 0, padding: 0, listStyle: "none" }}>
                    {securityItems.map(({ icon, label }) => (
                      <li key={label}>
                        <Flex
                          align="center"
                          gap={16}
                          style={{ borderRadius: 16, background: INK_SURFACE_SOFT, padding: "16px 20px" }}
                        >
                          <Flex
                            align="center"
                            justify="center"
                            style={{ width: 40, height: 40, flex: "none", borderRadius: 12, background: LIME_ACCENT, color: INK_SURFACE }}
                          >
                            <LandingIcon name={icon} style={{ fontSize: 18 }} />
                          </Flex>
                          <strong style={{ fontSize: 14, fontWeight: 600, color: SAGE_ON_INK }}>
                            {label}
                          </strong>
                        </Flex>
                      </li>
                    ))}
                  </Flex>
                </Col>
              </Row>
            </div>
          </div>
        </section>

        {/* CTA */}
        <section id="support" style={{ scrollMarginTop: 96, padding: "48px 0" }}>
          <Flex
            vertical
            align="center"
            style={{ maxWidth: CONTENT_WIDTH, margin: "0 auto", padding: "0 16px", textAlign: "center" }}
          >
            <h2 className="payu-display" style={{ margin: 0, maxWidth: 672, fontSize: "clamp(32px, 4vw, 48px)" }}>
              {t("slide4.title")}{" "}
              <span style={{ color: "var(--ant-color-primary-text)" }}>{t("slide4.titleHighlight")}</span>
            </h2>
            <Body muted style={{ marginTop: 20, maxWidth: 448 }}>
              {t("slide4.subtitle")}
            </Body>
            <Link href={"/onboarding"} style={{ cursor: "pointer", textDecoration: "none", marginTop: 36 }}>
              <Button type="primary" size="large">
                {t("slide4.button")}
              </Button>
            </Link>
          </Flex>
        </section>
      </main>

      <footer style={{ background: INK_SURFACE, color: SAGE_ON_INK, padding: "48px 0" }}>
        <div style={{ maxWidth: CONTENT_WIDTH, margin: "0 auto", padding: "0 16px" }}>
          <Flex justify="space-between" gap={32} wrap>
            <div style={{ maxWidth: 320 }}>
              {brandMark}
              <Body style={{ marginTop: 12, fontSize: 14, lineHeight: 1.6, color: SAGE_ON_INK }}>
                {t("footer.tagline")}
              </Body>
            </div>
            <Flex vertical gap={8} style={{ fontSize: 14 }}>
              <strong style={{ marginBottom: 4, fontSize: 12, fontWeight: 600, textTransform: "uppercase", letterSpacing: "0.05em", color: LIME_ACCENT }}>
                Legal
              </strong>
              <Link href={"/terms"} style={{ cursor: "pointer", textDecoration: "none", color: SAGE_ON_INK }}>
                {t("slide4.terms")}
              </Link>
              <Link href={"/privacy"} style={{ cursor: "pointer", textDecoration: "none", color: SAGE_ON_INK }}>
                {t("slide4.privacy")}
              </Link>
            </Flex>
          </Flex>
          <Divider style={{ marginTop: 40, marginBottom: 24, borderColor: INK_SURFACE_BORDER }} />
          <p style={{ margin: 0, fontSize: 12, color: SAGE_ON_INK }}>© 2026 PayU. All rights reserved.</p>
        </div>
      </footer>
    </Flex>
  );
}
