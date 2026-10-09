"use client";

import { Link } from "@/lib/navigation";
import { useTranslations } from "next-intl";
import {
  BadgeCheck,
  BarChart3,
  CheckCircle2,
  Fingerprint,
  Globe,
  Lock,
  Menu,
  Plus,
  QrCode,
  ReceiptText,
  Shield,
  Wallet,
  X,
  Zap,
} from "@/components/icons";
import {
  Button,
  Card,
  Col,
  Divider,
  Flex,
  Grid,
  Row,
  Tag,
  theme,
  Typography,
} from "antd";
import { Fragment, useState } from "react";

const { Title, Text } = Typography;

const CONTENT_WIDTH = 1080;
const NAV_TARGETS = ["features", "how", "about"] as const;

export default function LandingPage() {
  const t = useTranslations("landing");
  const { token } = theme.useToken();
  const screens = Grid.useBreakpoint();
  const isSm = Boolean(screens.sm);
  const isMd = Boolean(screens.md);
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const rawHeroTitle = t.raw("heroTitle") as string;

  const onPrimary = { color: token.colorTextLightSolid };
  const mutedSurface = { background: token.colorFillTertiary };

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

  const features = [
    {
      icon: BarChart3,
      title: t("slide2.analytics.title"),
      desc: t("slide2.analytics.desc"),
    },
    {
      icon: QrCode,
      title: t("features.qris.title"),
      desc: t("features.qris.desc"),
    },
    {
      icon: Wallet,
      title: t("features.pockets.title"),
      desc: t("features.pockets.desc"),
    },
    {
      icon: ReceiptText,
      title: t("features.bills.title"),
      desc: t("features.bills.desc"),
    },
    {
      icon: Globe,
      title: t("slide2.connectivity.title"),
      desc: t("slide2.connectivity.desc"),
    },
    {
      icon: Shield,
      title: t("slide2.security.title"),
      desc: t("slide2.security.desc"),
    },
  ];

  const steps = [
    { n: 1, title: t("how.step1.title"), desc: t("how.step1.desc") },
    { n: 2, title: t("how.step2.title"), desc: t("how.step2.desc") },
    { n: 3, title: t("how.step3.title"), desc: t("how.step3.desc") },
  ];

  const securityItems = [
    { icon: Lock, label: t("secure.item1") },
    { icon: Fingerprint, label: t("secure.item2") },
    { icon: BadgeCheck, label: t("secure.item3") },
  ];

  const quickActions = [
    { icon: QrCode, label: "QRIS" },
    { icon: Zap, label: "Kirim" },
    { icon: Plus, label: "Top Up" },
    { icon: Wallet, label: "Pocket" },
  ];

  const demoTransactions = [
    { name: "QRIS Merchant", amount: "-Rp45.000", positive: false },
    { name: "Top Up Pocket", amount: "+Rp500.000", positive: true },
    { name: "Transfer", amount: "-Rp120.000", positive: false },
  ];

  const brandMark = (
    <Flex align="center" gap={6}>
      <span style={{ fontWeight: 700, fontSize: 20, letterSpacing: "-0.02em" }}>
        PayU
      </span>
      <span
        aria-hidden="true"
        style={{
          width: 8,
          height: 8,
          borderRadius: "50%",
          background: token.colorPrimary,
          display: "inline-block",
        }}
      />
    </Flex>
  );

  return (
    <Flex
      vertical
      style={{
        minHeight: "100vh",
        background: token.colorBgLayout,
        color: token.colorText,
      }}
    >
      <header
        style={{
          position: "sticky",
          top: 0,
          zIndex: 50,
          borderBottom: `1px solid ${token.colorBorderSecondary}`,
          background: token.colorBgContainer,
        }}
      >
        <Flex
          align="center"
          justify="space-between"
          gap={16}
          style={{
            maxWidth: CONTENT_WIDTH,
            margin: "0 auto",
            height: 64,
            padding: "0 16px",
          }}
        >
          <Link
            href={"/"}
            aria-label="PayU Home"
            style={{
              cursor: "pointer",
              textDecoration: "none",
              color: "inherit",
            }}
          >
            {brandMark}
          </Link>

          {isMd && (
            <Flex component="nav" align="center" gap={32} aria-label="Main">
              {NAV_TARGETS.map((target) => (
                <Button
                  key={target}
                  type="text"
                  href={`#${target}`}
                  onClick={(e) => handleNavClick(e, target)}
                >
                  {navLabels[target]}
                </Button>
              ))}
            </Flex>
          )}

          <Flex align="center" gap={10}>
            {isSm && (
              <>
                <Link
                  href={"/login"}
                  style={{ cursor: "pointer", textDecoration: "none" }}
                >
                  <Button type="default">{t("nav.login")}</Button>
                </Link>
                <Link
                  href={"/onboarding"}
                  style={{ cursor: "pointer", textDecoration: "none" }}
                >
                  <Button type="primary">{t("getStarted")}</Button>
                </Link>
              </>
            )}
            {!isMd && (
              <Button
                type="default"
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
            )}
          </Flex>
        </Flex>

        {mobileMenuOpen && (
          <div
            style={{
              borderTop: `1px solid ${token.colorBorderSecondary}`,
              background: token.colorBgContainer,
            }}
          >
            <Flex
              component="nav"
              vertical
              gap={4}
              aria-label="Mobile"
              style={{
                maxWidth: CONTENT_WIDTH,
                margin: "0 auto",
                padding: "16px",
              }}
            >
              {NAV_TARGETS.map((target) => (
                <Button
                  key={target}
                  type="text"
                  block
                  href={`#${target}`}
                  onClick={(e) => handleNavClick(e, target)}
                  style={{
                    justifyContent: "flex-start",
                    fontSize: 16,
                    fontWeight: 500,
                  }}
                >
                  {navLabels[target]}
                </Button>
              ))}
              <Link
                href={"/login"}
                style={{ cursor: "pointer", textDecoration: "none" }}
              >
                <Button
                  type="text"
                  block
                  style={{
                    justifyContent: "flex-start",
                    fontSize: 16,
                    fontWeight: 500,
                  }}
                >
                  {t("nav.login")}
                </Button>
              </Link>
              <Link
                href={"/onboarding"}
                style={{
                  cursor: "pointer",
                  textDecoration: "none",
                  marginTop: 4,
                }}
              >
                <Button type="primary" block>
                  {t("getStarted")}
                </Button>
              </Link>
            </Flex>
          </div>
        )}
      </header>

      <main style={{ flex: 1 }}>
        {/* Hero */}
        <section style={{ position: "relative", overflow: "hidden" }}>
          <div
            aria-hidden="true"
            style={{
              position: "absolute",
              top: -160,
              right: "-12%",
              width: 520,
              height: 520,
              borderRadius: "50%",
              background: token.colorPrimary,
              opacity: 0.1,
              filter: "blur(48px)",
              pointerEvents: "none",
            }}
          />
          <div
            aria-hidden="true"
            style={{
              position: "absolute",
              bottom: "-30%",
              left: "-10%",
              width: 400,
              height: 400,
              borderRadius: "50%",
              background: token.colorPrimary,
              opacity: 0.05,
              filter: "blur(48px)",
              pointerEvents: "none",
            }}
          />

          <Row
            gutter={[{ xs: 0, lg: 40 }, 56]}
            align="middle"
            style={{
              position: "relative",
              maxWidth: CONTENT_WIDTH,
              margin: "0 auto",
              padding: "40px 16px 64px",
            }}
          >
            <Col xs={24} lg={12}>
              <div>
                <Tag
                  className="animate-fade-in"
                  style={{
                    margin: 0,
                    border: "none",
                    borderRadius: 9999,
                    padding: "6px 14px",
                    fontSize: 12,
                    fontWeight: 600,
                    background: token.colorPrimaryBg,
                    color: token.colorPrimaryText,
                  }}
                >
                  {t("badge")}
                </Tag>
                <Title
                  level={1}
                  className="animate-fade-in"
                  style={{
                    marginTop: 24,
                    marginBottom: 0,
                    fontSize: "clamp(28px, 4vw, 60px)",
                    lineHeight: 1.1,
                    fontWeight: 800,
                    letterSpacing: "-0.025em",
                    animationDelay: "80ms",
                  }}
                >
                  {rawHeroTitle.split(/<br\s*\/?>/i).map((line, index) => (
                    <Fragment key={`${index}-${line}`}>
                      {index > 0 && <br />}
                      {line}
                    </Fragment>
                  ))}
                </Title>
                <Text
                  type="secondary"
                  className="animate-fade-in"
                  style={{
                    display: "block",
                    marginTop: 24,
                    maxWidth: 448,
                    fontSize: 18,
                    lineHeight: 1.6,
                    animationDelay: "160ms",
                  }}
                >
                  {t("slide4.subtitle")}
                </Text>
                <Flex
                  wrap
                  align="center"
                  gap={12}
                  className="animate-fade-in"
                  style={{ marginTop: 32, animationDelay: "240ms" }}
                >
                  <Link
                    href={"/onboarding"}
                    style={{ cursor: "pointer", textDecoration: "none" }}
                  >
                    <Button type="primary" size="large">
                      {t("getStarted")}
                    </Button>
                  </Link>
                  <Link
                    href={"/login"}
                    style={{ cursor: "pointer", textDecoration: "none" }}
                  >
                    <Button type="default" size="large">
                      {t("nav.login")}
                    </Button>
                  </Link>
                </Flex>
                <Text
                  type="secondary"
                  className="animate-fade-in"
                  style={{
                    display: "block",
                    marginTop: 16,
                    fontSize: 12,
                    animationDelay: "300ms",
                  }}
                >
                  {t("hero.freeAdmin")}
                </Text>

                <Flex
                  component="dl"
                  gap={40}
                  style={{
                    margin: 0,
                    marginTop: 40,
                    paddingTop: 24,
                    borderTop: `1px solid ${token.colorBorderSecondary}`,
                  }}
                >
                  <div>
                    <dt className="sr-only">{t("slide3.statsAnnual")}</dt>
                    <dd
                      style={{
                        margin: 0,
                        fontSize: 24,
                        fontWeight: 700,
                        fontVariantNumeric: "tabular-nums",
                        letterSpacing: "-0.025em",
                      }}
                    >
                      ✓
                    </dd>
                    <Text type="secondary" style={{ fontSize: 12 }}>
                      {t("slide3.statsAnnual")}
                    </Text>
                  </div>
                  <div>
                    <dt className="sr-only">{t("slide3.statsTrusted")}</dt>
                    <dd
                      style={{
                        margin: 0,
                        fontSize: 24,
                        fontWeight: 700,
                        fontVariantNumeric: "tabular-nums",
                        letterSpacing: "-0.025em",
                      }}
                    >
                      ✓
                    </dd>
                    <Text type="secondary" style={{ fontSize: 12 }}>
                      {t("slide3.statsTrusted")}
                    </Text>
                  </div>
                  <div style={{ display: isSm ? "block" : "none" }}>
                    <dt className="sr-only">{t("secure.item3")}</dt>
                    <dd
                      style={{
                        margin: 0,
                        display: "flex",
                        alignItems: "center",
                        gap: 6,
                        fontSize: 24,
                        fontWeight: 700,
                        letterSpacing: "-0.025em",
                      }}
                    >
                      ISO
                      <BadgeCheck
                        aria-hidden="true"
                        style={{ fontSize: 20, color: token.colorPrimary }}
                      />
                    </dd>
                    <Text type="secondary" style={{ fontSize: 12 }}>
                      {t("secure.item3")}
                    </Text>
                  </div>
                </Flex>
              </div>
            </Col>

            {/* Phone Mockup — decorative */}
            <Col
              xs={24}
              lg={12}
              aria-hidden="true"
              style={{ display: isSm ? "block" : "none" }}
            >
              <div
                style={{
                  position: "relative",
                  margin: "0 auto",
                  width: 310,
                  maxWidth: "100%",
                  perspective: 1400,
                }}
              >
                <div className="animate-[float-3d_8s_ease-in-out_infinite] motion-reduce:animate-none">
                  <div
                    style={{
                      position: "absolute",
                      inset: -40,
                      borderRadius: "50%",
                      background: token.colorPrimary,
                      opacity: 0.1,
                      filter: "blur(48px)",
                    }}
                  />
                  <div
                    style={{
                      position: "relative",
                      borderRadius: 48,
                      border: `1px solid ${token.colorBorderSecondary}`,
                      background: token.colorBgContainer,
                      padding: 10,
                      boxShadow: token.boxShadowSecondary,
                    }}
                  >
                    <Flex
                      vertical
                      gap={20}
                      style={{
                        borderRadius: 40,
                        background: token.colorBgLayout,
                        padding: "16px 20px 28px",
                      }}
                    >
                      <div
                        style={{
                          margin: "0 auto",
                          width: 64,
                          height: 6,
                          borderRadius: 9999,
                          background: token.colorBorder,
                        }}
                      />
                      <Flex
                        align="center"
                        justify="space-between"
                        style={{ paddingTop: 4 }}
                      >
                        <div>
                          <Text type="secondary" style={{ fontSize: 12 }}>
                            PayU
                          </Text>
                          <Text
                            strong
                            style={{ display: "block", fontSize: 14 }}
                          >
                            Demo Preview
                          </Text>
                        </div>
                        <Flex
                          align="center"
                          justify="center"
                          style={{
                            width: 36,
                            height: 36,
                            borderRadius: "50%",
                            background: token.colorPrimaryBg,
                            color: token.colorPrimaryText,
                            fontSize: 14,
                            fontWeight: 700,
                          }}
                        >
                          P
                        </Flex>
                      </Flex>

                      <div
                        style={{
                          borderRadius: 16,
                          background: token.colorPrimary,
                          padding: 16,
                          ...onPrimary,
                          boxShadow: token.boxShadowSecondary,
                        }}
                      >
                        <p style={{ margin: 0, fontSize: 12, opacity: 0.7 }}>
                          Total Saldo
                        </p>
                        <p
                          style={{
                            margin: 0,
                            marginTop: 2,
                            fontSize: 20,
                            fontWeight: 700,
                            fontVariantNumeric: "tabular-nums",
                          }}
                        >
                          Rp ••• ••• •••
                        </p>
                        <p
                          style={{
                            margin: 0,
                            marginTop: 12,
                            fontSize: 12,
                            fontWeight: 500,
                            opacity: 0.7,
                          }}
                        >
                          •••• 4682
                        </p>
                      </div>

                      <Row gutter={8}>
                        {quickActions.map(({ icon: Icon, label }) => (
                          <Col span={6} key={label}>
                            <Flex vertical align="center" gap={6}>
                              <Flex
                                align="center"
                                justify="center"
                                style={{
                                  width: "100%",
                                  height: 44,
                                  borderRadius: 12,
                                  background: token.colorPrimaryBg,
                                  color: token.colorPrimaryText,
                                }}
                              >
                                <Icon style={{ fontSize: 18 }} />
                              </Flex>
                              <Text type="secondary" style={{ fontSize: 12 }}>
                                {label}
                              </Text>
                            </Flex>
                          </Col>
                        ))}
                      </Row>

                      <Flex vertical gap={12}>
                        {demoTransactions.map((tx) => (
                          <Flex
                            key={tx.name}
                            align="center"
                            justify="space-between"
                          >
                            <Flex align="center" gap={10}>
                              <Flex
                                align="center"
                                justify="center"
                                style={{
                                  width: 32,
                                  height: 32,
                                  borderRadius: "50%",
                                  background: tx.positive
                                    ? token.colorPrimaryBg
                                    : token.colorFillTertiary,
                                  color: tx.positive
                                    ? token.colorPrimaryText
                                    : token.colorTextSecondary,
                                }}
                              >
                                {tx.positive ? (
                                  <Plus style={{ fontSize: 14 }} />
                                ) : (
                                  <QrCode style={{ fontSize: 14 }} />
                                )}
                              </Flex>
                              <Text style={{ fontSize: 12, fontWeight: 500 }}>
                                {tx.name}
                              </Text>
                            </Flex>
                            <Text
                              strong
                              style={{
                                fontSize: 12,
                                fontVariantNumeric: "tabular-nums",
                                color: tx.positive
                                  ? token.colorPrimaryText
                                  : undefined,
                              }}
                            >
                              {tx.amount}
                            </Text>
                          </Flex>
                        ))}
                      </Flex>
                    </Flex>
                  </div>

                  <Card
                    style={{
                      position: "absolute",
                      right: -48,
                      top: 64,
                      boxShadow: token.boxShadowSecondary,
                    }}
                    styles={{ body: { padding: "12px 16px" } }}
                  >
                    <Flex align="center" gap={10}>
                      <CheckCircle2
                        style={{ fontSize: 18, color: token.colorPrimary }}
                      />
                      <div>
                        <Text strong style={{ display: "block", fontSize: 11 }}>
                          Payment Successful
                        </Text>
                        <Text type="secondary" style={{ fontSize: 12 }}>
                          QRIS • Rp45.000
                        </Text>
                      </div>
                    </Flex>
                  </Card>
                </div>
              </div>
            </Col>
          </Row>
        </section>

        {/* Features */}
        <section
          id="features"
          style={{
            scrollMarginTop: 96,
            borderTop: `1px solid ${token.colorBorderSecondary}`,
            padding: "80px 0",
          }}
        >
          <div
            style={{
              maxWidth: CONTENT_WIDTH,
              margin: "0 auto",
              padding: "0 24px",
            }}
          >
            <div style={{ maxWidth: 576 }}>
              <Tag
                style={{
                  margin: 0,
                  border: "none",
                  borderRadius: 9999,
                  padding: "6px 16px",
                  fontSize: 12,
                  fontWeight: 600,
                  background: token.colorPrimaryBg,
                  color: token.colorPrimaryText,
                }}
              >
                {t("slide2.badge")}
              </Tag>
              <Title
                level={2}
                style={{
                  marginTop: 20,
                  marginBottom: 0,
                  fontSize: "clamp(28px, 3vw, 36px)",
                  fontWeight: 800,
                  letterSpacing: "-0.025em",
                }}
              >
                {t("slide2.title")}{" "}
                <span style={{ color: token.colorPrimaryText }}>
                  {t("slide2.titleHighlight")}
                </span>
              </Title>
              <Text
                type="secondary"
                style={{ display: "block", marginTop: 16 }}
              >
                {t("slide2.subtitle")}
              </Text>
            </div>

            <Row gutter={[20, 20]} style={{ marginTop: 48 }}>
              {features.map(({ icon: Icon, title, desc }) => (
                <Col key={title} xs={24} sm={12} lg={8}>
                  <Card hoverable style={{ height: "100%" }}>
                    <Flex
                      align="center"
                      justify="center"
                      style={{
                        width: 44,
                        height: 44,
                        borderRadius: 12,
                        background: token.colorPrimaryBg,
                        color: token.colorPrimaryText,
                      }}
                    >
                      <Icon style={{ fontSize: 20 }} />
                    </Flex>
                    <Title
                      level={4}
                      style={{
                        marginTop: 20,
                        marginBottom: 0,
                        fontWeight: 700,
                      }}
                    >
                      {title}
                    </Title>
                    <Text
                      type="secondary"
                      style={{
                        display: "block",
                        marginTop: 8,
                        fontSize: 14,
                        lineHeight: 1.6,
                      }}
                    >
                      {desc}
                    </Text>
                  </Card>
                </Col>
              ))}
            </Row>
          </div>
        </section>

        {/* How it works */}
        <section id="how" style={{ scrollMarginTop: 96, padding: "80px 0" }}>
          <div
            style={{
              maxWidth: CONTENT_WIDTH,
              margin: "0 auto",
              padding: "0 24px",
            }}
          >
            <Card
              variant="borderless"
              style={{ ...mutedSurface, padding: 0 }}
              styles={{ body: { padding: "56px 56px" } }}
            >
              <Title
                level={2}
                style={{
                  margin: 0,
                  fontSize: "clamp(28px, 3vw, 36px)",
                  fontWeight: 800,
                  letterSpacing: "-0.025em",
                }}
              >
                {t("how.title")}
              </Title>
              <Flex
                component="ol"
                gap={24}
                wrap
                style={{
                  listStyle: "none",
                  margin: 0,
                  marginTop: 48,
                  padding: 0,
                }}
              >
                {steps.map(({ n, title, desc }) => (
                  <li
                    key={n}
                    style={{
                      flex: "1 1 240px",
                      position: "relative",
                      paddingLeft: 24,
                      borderLeft: `2px solid ${token.colorPrimaryBorder}`,
                    }}
                  >
                    <span
                      aria-hidden="true"
                      style={{
                        display: "block",
                        fontSize: 48,
                        fontWeight: 800,
                        letterSpacing: "-0.05em",
                        color: token.colorPrimary,
                        opacity: 0.25,
                      }}
                    >
                      0{n}
                    </span>
                    <Title
                      level={4}
                      style={{
                        marginTop: 12,
                        marginBottom: 0,
                        fontWeight: 700,
                      }}
                    >
                      {title}
                    </Title>
                    <Text
                      type="secondary"
                      style={{
                        display: "block",
                        marginTop: 8,
                        fontSize: 14,
                        lineHeight: 1.6,
                      }}
                    >
                      {desc}
                    </Text>
                  </li>
                ))}
              </Flex>
            </Card>
          </div>
        </section>

        {/* Security / About */}
        <section id="about" style={{ scrollMarginTop: 96, paddingBottom: 80 }}>
          <div
            style={{
              maxWidth: CONTENT_WIDTH,
              margin: "0 auto",
              padding: "0 24px",
            }}
          >
            <Card
              variant="borderless"
              style={{
                position: "relative",
                overflow: "hidden",
                background: token.colorPrimary,
                ...onPrimary,
              }}
              styles={{ body: { padding: "56px" } }}
            >
              <div
                aria-hidden="true"
                style={{
                  position: "absolute",
                  right: -96,
                  top: -96,
                  width: 288,
                  height: 288,
                  borderRadius: "50%",
                  background: token.colorTextLightSolid,
                  opacity: 0.1,
                  filter: "blur(40px)",
                  pointerEvents: "none",
                }}
              />
              <div
                aria-hidden="true"
                style={{
                  position: "absolute",
                  bottom: -112,
                  left: -80,
                  width: 320,
                  height: 320,
                  borderRadius: "50%",
                  background: token.colorTextLightSolid,
                  opacity: 0.1,
                  filter: "blur(40px)",
                  pointerEvents: "none",
                }}
              />

              <Row
                gutter={[48, 48]}
                align="middle"
                style={{ position: "relative" }}
              >
                <Col xs={24} lg={12}>
                  <div>
                    <Tag
                      style={{
                        margin: 0,
                        border: "none",
                        borderRadius: 9999,
                        padding: "6px 16px",
                        fontSize: 12,
                        fontWeight: 600,
                        background: token.colorTextLightSolid,
                        ...onPrimary,
                      }}
                    >
                      {t("slide3.badge")}
                    </Tag>
                    <Title
                      level={2}
                      style={{
                        marginTop: 20,
                        marginBottom: 0,
                        fontSize: "clamp(28px, 3vw, 36px)",
                        fontWeight: 800,
                        letterSpacing: "-0.025em",
                        ...onPrimary,
                      }}
                    >
                      {t("slide3.title")}{" "}
                      <span style={{ opacity: 0.7 }}>
                        {t("slide3.titleHighlight")}
                      </span>
                    </Title>
                    <Text
                      style={{
                        display: "block",
                        marginTop: 16,
                        maxWidth: 448,
                        lineHeight: 1.6,
                        opacity: 0.8,
                        ...onPrimary,
                      }}
                    >
                      {t("slide2.security.desc")}
                    </Text>
                  </div>
                </Col>

                <Col xs={24} lg={12}>
                  <Flex
                    component="ul"
                    vertical
                    gap={16}
                    style={{ margin: 0, padding: 0, listStyle: "none" }}
                  >
                    {securityItems.map(({ icon: Icon, label }) => (
                      <li key={label}>
                        <Flex
                          align="center"
                          gap={16}
                          style={{
                            borderRadius: 16,
                            background: token.colorTextLightSolid,
                            padding: "16px 20px",
                          }}
                        >
                          <Flex
                            align="center"
                            justify="center"
                            style={{
                              width: 40,
                              height: 40,
                              flex: "none",
                              borderRadius: 12,
                              background: token.colorTextLightSolid,
                              ...onPrimary,
                            }}
                          >
                            <Icon aria-hidden="true" style={{ fontSize: 18 }} />
                          </Flex>
                          <Text strong style={{ fontSize: 14, ...onPrimary }}>
                            {label}
                          </Text>
                        </Flex>
                      </li>
                    ))}
                  </Flex>
                </Col>
              </Row>

              <Flex
                gap={56}
                style={{
                  position: "relative",
                  marginTop: 56,
                  paddingTop: 32,
                  borderTop: `1px solid ${token.colorBorderSecondary}`,
                }}
              >
                <div>
                  <p
                    style={{
                      margin: 0,
                      fontSize: 32,
                      fontWeight: 800,
                      fontVariantNumeric: "tabular-nums",
                      letterSpacing: "-0.05em",
                    }}
                  >
                    ✓
                  </p>
                  <Text
                    style={{
                      display: "block",
                      marginTop: 4,
                      fontSize: 14,
                      opacity: 0.7,
                      ...onPrimary,
                    }}
                  >
                    {t("slide3.statsAnnual")}
                  </Text>
                </div>
                <div>
                  <p
                    style={{
                      margin: 0,
                      fontSize: 32,
                      fontWeight: 800,
                      fontVariantNumeric: "tabular-nums",
                      letterSpacing: "-0.05em",
                    }}
                  >
                    ✓
                  </p>
                  <Text
                    style={{
                      display: "block",
                      marginTop: 4,
                      fontSize: 14,
                      opacity: 0.7,
                      ...onPrimary,
                    }}
                  >
                    {t("slide3.statsTrusted")}
                  </Text>
                </div>
              </Flex>
            </Card>
          </div>
        </section>

        {/* CTA */}
        <section
          id="support"
          style={{
            scrollMarginTop: 96,
            borderTop: `1px solid ${token.colorBorderSecondary}`,
            padding: "96px 0",
          }}
        >
          <Flex
            vertical
            align="center"
            style={{
              maxWidth: CONTENT_WIDTH,
              margin: "0 auto",
              padding: "0 24px",
              textAlign: "center",
            }}
          >
            <Title
              level={2}
              style={{
                margin: 0,
                maxWidth: 672,
                fontSize: "clamp(32px, 4vw, 48px)",
                fontWeight: 800,
                letterSpacing: "-0.025em",
              }}
            >
              {t("slide4.title")}{" "}
              <span style={{ color: token.colorPrimaryText }}>
                {t("slide4.titleHighlight")}
              </span>
            </Title>
            <Text
              type="secondary"
              style={{ display: "block", marginTop: 20, maxWidth: 448 }}
            >
              {t("slide4.subtitle")}
            </Text>
            <Link
              href={"/onboarding"}
              style={{
                cursor: "pointer",
                textDecoration: "none",
                marginTop: 36,
              }}
            >
              <Button type="primary" size="large">
                {t("slide4.button")}
              </Button>
            </Link>
          </Flex>
        </section>
      </main>

      <footer
        style={{
          borderTop: `1px solid ${token.colorBorderSecondary}`,
          ...mutedSurface,
        }}
      >
        <div
          style={{
            maxWidth: CONTENT_WIDTH,
            margin: "0 auto",
            padding: "48px 24px",
          }}
        >
          <Flex justify="space-between" gap={32} wrap>
            <div style={{ maxWidth: 320 }}>
              {brandMark}
              <Text
                type="secondary"
                style={{
                  display: "block",
                  marginTop: 12,
                  fontSize: 14,
                  lineHeight: 1.6,
                }}
              >
                {t("footer.tagline")}
              </Text>
            </div>
            <Flex vertical gap={8} style={{ fontSize: 14 }}>
              <Text
                strong
                style={{
                  marginBottom: 4,
                  fontSize: 12,
                  textTransform: "uppercase",
                  letterSpacing: "0.05em",
                  color: token.colorTextTertiary,
                }}
              >
                Legal
              </Text>
              <Link
                href={"/terms"}
                style={{
                  cursor: "pointer",
                  textDecoration: "none",
                  color: token.colorTextSecondary,
                }}
              >
                {t("slide4.terms")}
              </Link>
              <Link
                href={"/privacy"}
                style={{
                  cursor: "pointer",
                  textDecoration: "none",
                  color: token.colorTextSecondary,
                }}
              >
                {t("slide4.privacy")}
              </Link>
            </Flex>
          </Flex>
          <Divider style={{ marginTop: 40, marginBottom: 24 }} />
          <Text type="secondary" style={{ fontSize: 12 }}>
            © 2026 PayU. All rights reserved.
          </Text>
        </div>
      </footer>
    </Flex>
  );
}
