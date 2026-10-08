"use client";

import { useState, useCallback, useEffect, useRef } from "react";
import type { CSSProperties } from "react";
import {
  Alert,
  Button,
  Card,
  Col,
  Empty,
  Form,
  Input,
  Row,
  Select,
  Space,
  Spin,
  Tag,
  Typography,
} from "antd";
import { zodFieldRule } from "@/lib/zodForm";
import {
  ArrowRightLeft,
  TrendingUp,
  CheckCircle,
  Clock,
} from "@/components/icons";
import { exchangeSchema, type ExchangeRequest } from "@/types";
import {
  useFxRate,
  useFxEstimate,
  useFxConversion,
  useFxConversions,
} from "@/hooks";
import { useAuthStore, useUIStore } from "@/stores";
import { SUPPORTED_CURRENCIES } from "@/services/FxService";
import {
  compareCurrency,
  formatExactDecimal,
  parseCurrencyExact,
  type Money,
} from "@/lib/currency";
import DashboardLayout from "@/components/DashboardLayout";
import { Link } from "@/lib/navigation";
import { useTranslations } from "next-intl";

const labelStyle: CSSProperties = {
  fontSize: 12,
  fontWeight: 700,
  color: "var(--ant-color-text-secondary)",
  textTransform: "uppercase",
  letterSpacing: "0.1em",
};

const capsPrimaryStyle: CSSProperties = {
  ...labelStyle,
  color: "var(--ant-color-primary)",
};

export default function ExchangePage() {
  const accountId = useAuthStore((state) => state.accountId);
  const addToast = useUIStore((state) => state.addToast);
  const t = useTranslations("nav");

  const [form] = Form.useForm<ExchangeRequest>();
  const rule = (field: string) => zodFieldRule(form, exchangeSchema, field);

  const fromCurrency = Form.useWatch("fromCurrency", form) ?? "IDR";
  const toCurrency = Form.useWatch("toCurrency", form) ?? "USD";
  const amount = Form.useWatch("amount", form) ?? "";

  // FX Rate query — gated on the pair only, so the calculator shows a live
  // rate (or a real error) on first paint instead of a perpetual spinner.
  // The amount only gates the conversion *estimate* below.
  const {
    data: fxRate,
    isLoading: isLoadingRate,
    error: rateError,
    refetch: refetchRate,
  } = useFxRate(fromCurrency, toCurrency, fromCurrency !== toCurrency);

  const estimateMutation = useFxEstimate();
  const [estimatedAmount, setEstimatedAmount] = useState<Money | null>(null);

  const conversionMutation = useFxConversion();

  const { data: conversions, isLoading: isLoadingConversions } =
    useFxConversions(!!accountId);

  const handleSwap = useCallback(() => {
    form.setFieldsValue({ fromCurrency: toCurrency, toCurrency: fromCurrency });
    setEstimatedAmount(null);
  }, [form, fromCurrency, toCurrency]);

  // BUG-FE-022: Use ref for estimateMutation to avoid infinite loop
  // (mutation object is new every render, causing useEffect to re-run)
  const estimateMutationRef = useRef(estimateMutation);
  useEffect(() => {
    estimateMutationRef.current = estimateMutation;
  }, [estimateMutation]);

  useEffect(() => {
    if (compareCurrency(amount, "0") > 0 && fromCurrency !== toCurrency) {
      const timer = setTimeout(async () => {
        try {
          const result = await estimateMutationRef.current.mutateAsync({
            fromCurrency,
            toCurrency,
            amount: parseCurrencyExact(amount),
          });
          setEstimatedAmount(result.toAmount);
        } catch {
          setEstimatedAmount(null);
        }
      }, 300);

      return () => clearTimeout(timer);
    }
  }, [amount, fromCurrency, toCurrency]);

  // currencies match, reset the previous estimate during render (avoids the
  // cascading-render warning from setState-in-effect).
  if (
    !(compareCurrency(amount, "0") > 0 && fromCurrency !== toCurrency) &&
    estimatedAmount !== null
  ) {
    setEstimatedAmount(null);
  }

  const displayAmount = estimatedAmount;

  const formatCurrency = (value: Money | number, currencyCode: string) => {
    const currency = SUPPORTED_CURRENCIES[currencyCode];
    if (!currency) return `${value} ${currencyCode}`;

    return `${currency.symbol}${formatExactDecimal(value, currency.decimalPlaces)}`;
  };

  const onValid = (values: ExchangeRequest) => {
    onSubmit(exchangeSchema.parse(values));
  };
  const onSubmit = async (data: ExchangeRequest) => {
    if (!accountId) {
      addToast("Please log in to perform currency exchange", "error");
      return;
    }

    if (data.fromCurrency === data.toCurrency) {
      addToast("Please select different currencies", "warning");
      return;
    }

    conversionMutation.mutate(
      {
        fromCurrency: data.fromCurrency,
        toCurrency: data.toCurrency,
        amount: parseCurrencyExact(data.amount),
      },
      {
        onSuccess: () => {
          addToast(
            `Successfully exchanged ${formatCurrency(parseCurrencyExact(data.amount), data.fromCurrency)} to ${data.toCurrency}`,
            "success",
          );
          form.setFieldsValue({ amount: "" });
          setEstimatedAmount(null);
        },
        onError: (error: Error) => {
          // React Query passes an Error-derived value; access axios shape safely.
          const response = (
            error as { response?: { data?: { message?: string } } }
          ).response;
          addToast(
            response?.data?.message || "Exchange failed. Please try again.",
            "error",
          );
        },
      },
    );
  };

  const fromCurrencyInfo = SUPPORTED_CURRENCIES[fromCurrency];

  const recentConversions = Array.isArray(conversions)
    ? conversions.slice(0, 5)
    : [];

  return (
    <DashboardLayout>
      <Space direction="vertical" size={24} style={{ width: "100%" }}>
        <div>
          <Typography.Title level={2} style={{ marginTop: 0, marginBottom: 4 }}>
            {t("currencyExchange")}
          </Typography.Title>
          <Typography.Text type="secondary">
            {t("exchangeSubtitle")}
          </Typography.Text>
        </div>

        <Row gutter={[24, 24]}>
          {/* Main Exchange Form */}
          <Col xs={24} lg={16}>
            <Card style={{ position: "relative", overflow: "hidden" }}>
              {/* Ambient glow effect */}
              <div
                style={{
                  position: "absolute",
                  top: 0,
                  right: 0,
                  width: 384,
                  height: 384,
                  backgroundColor: "var(--ant-color-primary-bg)",
                  borderRadius: "50%",
                  filter: "blur(64px)",
                  pointerEvents: "none",
                }}
              />
              <div style={{ position: "relative", zIndex: 1 }}>
                <Form
                  form={form}
                  onFinish={onValid}
                  initialValues={{
                    fromCurrency: "IDR",
                    toCurrency: "USD",
                    amount: "",
                  }}
                  layout="vertical"
                >
                  <Typography.Text
                    strong
                    style={{
                      ...labelStyle,
                      display: "block",
                      marginBottom: 24,
                    }}
                  >
                    {t("exchangeCalculator")}
                  </Typography.Text>

                  {/* Currency Selector Row */}
                  <Row gutter={[16, 16]} align="bottom">
                    {/* From Currency */}
                    <Col xs={24} md={11}>
                      <Form.Item
                        name="fromCurrency"
                        rules={[rule("fromCurrency")]}
                        style={{ marginBottom: 0 }}
                        label={
                          <label
                            htmlFor="exchange-from-currency"
                            style={labelStyle}
                          >
                            {t("fromCurrency")}
                          </label>
                        }
                      >
                        <Select
                          id="exchange-from-currency"
                          value={fromCurrency}
                          onChange={(v) =>
                            form.setFieldsValue({
                              fromCurrency:
                                v as ExchangeRequest["fromCurrency"],
                            })
                          }
                          style={{ width: "100%" }}
                          aria-label="From currency"
                          options={Object.values(SUPPORTED_CURRENCIES).map(
                            (currency) => ({
                              value: currency.code,
                              label: `${currency.flag} ${currency.code} - ${currency.name}`,
                            }),
                          )}
                        />
                      </Form.Item>
                    </Col>

                    {/* Swap Button */}
                    <Col xs={24} md={2} style={{ textAlign: "center" }}>
                      <Button
                        htmlType="button"
                        shape="circle"
                        onClick={handleSwap}
                        style={{ width: 48, height: 48 }}
                        aria-label="Swap currencies"
                        icon={
                          <ArrowRightLeft style={{ width: 24, height: 24 }} />
                        }
                      />
                    </Col>

                    {/* To Currency */}
                    <Col xs={24} md={11}>
                      <Form.Item
                        name="toCurrency"
                        rules={[rule("toCurrency")]}
                        style={{ marginBottom: 0 }}
                        label={
                          <label
                            htmlFor="exchange-to-currency"
                            style={labelStyle}
                          >
                            {t("toCurrency")}
                          </label>
                        }
                      >
                        <Select
                          id="exchange-to-currency"
                          value={toCurrency}
                          onChange={(v) =>
                            form.setFieldsValue({
                              toCurrency: v as ExchangeRequest["toCurrency"],
                            })
                          }
                          style={{ width: "100%" }}
                          aria-label="To currency"
                          options={Object.values(SUPPORTED_CURRENCIES).map(
                            (currency) => ({
                              value: currency.code,
                              label: `${currency.flag} ${currency.code} - ${currency.name}`,
                            }),
                          )}
                        />
                      </Form.Item>
                    </Col>
                  </Row>

                  {/* Amount Input */}
                  <Space
                    direction="vertical"
                    size={16}
                    style={{ width: "100%", marginTop: 24 }}
                  >
                    <Row justify="space-between" align="middle">
                      <label htmlFor="exchange-amount" style={labelStyle}>
                        Amount
                      </label>
                      {isLoadingRate && fromCurrency !== toCurrency && (
                        <Space size={8}>
                          <Spin size="small" />
                          <Typography.Text strong style={capsPrimaryStyle}>
                            Updating Rate
                          </Typography.Text>
                        </Space>
                      )}
                    </Row>
                    <Form.Item
                      name="amount"
                      rules={[rule("amount")]}
                      style={{ marginBottom: 0 }}
                    >
                      <Input
                        id="exchange-amount"
                        type="number"
                        step="any"
                        min="0"
                        placeholder="0.00"
                        style={{ fontSize: 32, height: 80 }}
                        prefix={
                          <Typography.Text
                            type="secondary"
                            style={{ fontSize: 24, fontWeight: 700 }}
                          >
                            {fromCurrencyInfo?.symbol}
                          </Typography.Text>
                        }
                        aria-label="Amount to exchange"
                      />
                    </Form.Item>
                  </Space>

                  {/* Rate Display */}
                  {fromCurrency !== toCurrency && (
                    <Card
                      size="small"
                      style={{
                        marginTop: 24,
                        backgroundColor: "var(--ant-color-primary-bg)",
                        borderColor: "var(--ant-color-primary-border)",
                      }}
                    >
                      {rateError ? (
                        <Alert
                          type="error"
                          showIcon
                          message="Unable to fetch exchange rate"
                          action={
                            <Button
                              type="link"
                              size="small"
                              onClick={() => refetchRate()}
                            >
                              Try again
                            </Button>
                          }
                        />
                      ) : fxRate ? (
                        <Space
                          direction="vertical"
                          size={12}
                          style={{ width: "100%" }}
                        >
                          <Row justify="space-between" align="middle">
                            <Space size={8}>
                              <TrendingUp
                                style={{
                                  width: 16,
                                  height: 16,
                                  color: "var(--ant-color-primary)",
                                }}
                              />
                              <Typography.Text strong style={labelStyle}>
                                Current Rate
                              </Typography.Text>
                            </Space>
                            <Space size={8}>
                              <span
                                style={{
                                  width: 8,
                                  height: 8,
                                  borderRadius: "50%",
                                  backgroundColor: "var(--ant-color-primary)",
                                }}
                              />
                              <Typography.Text strong style={capsPrimaryStyle}>
                                Live
                              </Typography.Text>
                            </Space>
                          </Row>
                          <Space size={12} align="baseline">
                            <Typography.Title level={3} style={{ margin: 0 }}>
                              1 {fromCurrency}
                            </Typography.Title>
                            <Typography.Text type="secondary">
                              =
                            </Typography.Text>
                            <Typography.Title
                              level={3}
                              style={{
                                margin: 0,
                                color: "var(--ant-color-primary)",
                              }}
                            >
                              {formatExactDecimal(fxRate.rate, 4)} {toCurrency}
                            </Typography.Title>
                          </Space>
                          <Space size={8}>
                            <Clock style={{ width: 12, height: 12 }} />
                            <Typography.Text
                              type="secondary"
                              style={{ fontSize: 12 }}
                            >
                              Valid until{" "}
                              {new Date(fxRate.validUntil).toLocaleTimeString()}
                            </Typography.Text>
                          </Space>
                        </Space>
                      ) : (
                        <div
                          style={{
                            display: "flex",
                            justifyContent: "center",
                            padding: "16px 0",
                          }}
                        >
                          <Spin />
                        </div>
                      )}
                    </Card>
                  )}

                  {/* Conversion Preview */}
                  {displayAmount !== null && fromCurrency !== toCurrency && (
                    <Card
                      size="small"
                      style={{
                        marginTop: 24,
                        borderColor: "var(--ant-color-primary)",
                        borderWidth: 2,
                      }}
                    >
                      <Row justify="space-between" align="middle">
                        <div>
                          <Typography.Text
                            strong
                            style={{
                              ...labelStyle,
                              display: "block",
                              marginBottom: 8,
                            }}
                          >
                            You will receive
                          </Typography.Text>
                          <Typography.Title level={3} style={{ margin: 0 }}>
                            {formatCurrency(displayAmount, toCurrency)}
                          </Typography.Title>
                        </div>
                        <div
                          style={{
                            width: 48,
                            height: 48,
                            borderRadius: "50%",
                            backgroundColor: "var(--ant-color-primary-bg)",
                            display: "flex",
                            alignItems: "center",
                            justifyContent: "center",
                          }}
                        >
                          <CheckCircle
                            style={{
                              width: 24,
                              height: 24,
                              color: "var(--ant-color-primary)",
                            }}
                          />
                        </div>
                      </Row>
                    </Card>
                  )}

                  {/* Info Box */}
                  <div style={{ marginTop: 24 }}>
                    {fromCurrency === toCurrency ? (
                      <Alert
                        type="info"
                        showIcon
                        message="Please select different currencies to perform an exchange."
                      />
                    ) : (
                      <Alert
                        type="info"
                        showIcon
                        message="Exchange Information"
                        description={
                          <ul
                            style={{
                              margin: "8px 0 0",
                              paddingLeft: 20,
                              fontSize: 12,
                            }}
                          >
                            <li>Exchange rates are updated every 60 seconds</li>
                            <li>
                              No hidden fees - the rate you see is the rate you
                              get
                            </li>
                            <li>
                              Minimum exchange amount: 10,000 {fromCurrency}
                            </li>
                          </ul>
                        }
                      />
                    )}
                  </div>

                  {/* CTA Button */}
                  <div style={{ marginTop: 24 }}>
                    <Button
                      type="primary"
                      htmlType="submit"
                      size="large"
                      block
                      loading={conversionMutation.isPending}
                      disabled={
                        conversionMutation.isPending ||
                        fromCurrency === toCurrency ||
                        compareCurrency(amount, "0") <= 0 ||
                        !fxRate
                      }
                    >
                      {conversionMutation.isPending
                        ? "Processing Exchange..."
                        : "Exchange Currency Now"}
                    </Button>
                  </div>
                </Form>
              </div>
            </Card>
          </Col>

          {/* Sidebar - Recent Conversions & Info */}
          <Col xs={24} lg={8}>
            <Space direction="vertical" size={24} style={{ width: "100%" }}>
              {/* Rate Updates Card */}
              <Card>
                <Row
                  justify="space-between"
                  align="middle"
                  style={{ marginBottom: 24 }}
                >
                  <Typography.Text strong style={labelStyle}>
                    Market Status
                  </Typography.Text>
                  <Space size={8}>
                    <span
                      style={{
                        width: 8,
                        height: 8,
                        borderRadius: "50%",
                        backgroundColor: "var(--ant-color-primary)",
                      }}
                    />
                    <Typography.Text strong style={capsPrimaryStyle}>
                      Live
                    </Typography.Text>
                  </Space>
                </Row>
                <Space direction="vertical" size={16} style={{ width: "100%" }}>
                  <Row justify="space-between" align="middle">
                    <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                      Last Update
                    </Typography.Text>
                    <Typography.Text strong style={{ fontSize: 12 }}>
                      {fxRate
                        ? new Date(fxRate.validFrom).toLocaleTimeString()
                        : "--:--:--"}
                    </Typography.Text>
                  </Row>
                  <Row justify="space-between" align="middle">
                    <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                      Next Update
                    </Typography.Text>
                    <Typography.Text strong style={{ fontSize: 12 }}>
                      {fxRate
                        ? new Date(
                            new Date(fxRate.validFrom).getTime() + 60000,
                          ).toLocaleTimeString()
                        : "--:--:--"}
                    </Typography.Text>
                  </Row>
                  <Row justify="space-between" align="middle">
                    <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                      Supported Pairs
                    </Typography.Text>
                    <Typography.Text
                      strong
                      style={{
                        fontSize: 12,
                        color: "var(--ant-color-primary)",
                      }}
                    >
                      {Object.keys(SUPPORTED_CURRENCIES).length} currencies
                    </Typography.Text>
                  </Row>
                </Space>
              </Card>

              {/* Recent Conversions */}
              <Card>
                <Typography.Text
                  strong
                  style={{ ...labelStyle, display: "block", marginBottom: 24 }}
                >
                  Recent Exchanges
                </Typography.Text>

                {isLoadingConversions ? (
                  <div
                    style={{
                      display: "flex",
                      justifyContent: "center",
                      padding: "32px 0",
                    }}
                  >
                    <Spin />
                  </div>
                ) : recentConversions.length === 0 ? (
                  <Empty
                    image={Empty.PRESENTED_IMAGE_SIMPLE}
                    description="No exchange history yet"
                  />
                ) : (
                  <Space
                    direction="vertical"
                    size={16}
                    style={{ width: "100%" }}
                  >
                    {recentConversions.map((conversion) => {
                      const fromInfo =
                        SUPPORTED_CURRENCIES[conversion.fromCurrency];
                      const toInfo =
                        SUPPORTED_CURRENCIES[conversion.toCurrency];
                      const statusColor =
                        conversion.status === "COMPLETED"
                          ? "success"
                          : conversion.status === "PENDING"
                            ? "warning"
                            : conversion.status === "FAILED"
                              ? "error"
                              : "default";

                      return (
                        <Card
                          key={conversion.id}
                          size="small"
                          style={{
                            backgroundColor: "var(--ant-color-fill-alter)",
                          }}
                        >
                          <Row
                            justify="space-between"
                            align="middle"
                            style={{ marginBottom: 8 }}
                          >
                            <Space size={8} style={{ fontSize: 18 }}>
                              <span>{fromInfo?.flag}</span>
                              <ArrowRightLeft
                                style={{
                                  width: 16,
                                  height: 16,
                                  color: "var(--ant-color-text-secondary)",
                                }}
                              />
                              <span>{toInfo?.flag}</span>
                            </Space>
                            <Tag
                              color={statusColor}
                              style={{
                                textTransform: "uppercase",
                                letterSpacing: "0.1em",
                                fontSize: 12,
                                fontWeight: 700,
                              }}
                            >
                              {conversion.status.toLowerCase()}
                            </Tag>
                          </Row>
                          <Space size={8} align="baseline">
                            <Typography.Text strong style={{ fontSize: 14 }}>
                              {fromInfo?.symbol}
                              {formatExactDecimal(
                                conversion.fromAmount,
                                fromInfo?.decimalPlaces,
                              )}
                            </Typography.Text>
                            <Typography.Text type="secondary">
                              →
                            </Typography.Text>
                            <Typography.Text
                              strong
                              style={{
                                fontSize: 14,
                                color: "var(--ant-color-primary)",
                              }}
                            >
                              {toInfo?.symbol}
                              {formatExactDecimal(
                                conversion.toAmount,
                                toInfo?.decimalPlaces,
                              )}
                            </Typography.Text>
                          </Space>
                          <Typography.Text
                            type="secondary"
                            style={{
                              fontSize: 12,
                              display: "block",
                              marginTop: 4,
                            }}
                          >
                            {new Date(
                              conversion.conversionDate,
                            ).toLocaleString()}
                          </Typography.Text>
                        </Card>
                      );
                    })}
                  </Space>
                )}
              </Card>

              {/* Help Card */}
              <Card
                style={{
                  backgroundColor: "var(--ant-color-primary)",
                  border: "none",
                  position: "relative",
                  overflow: "hidden",
                }}
              >
                <div
                  style={{
                    position: "absolute",
                    bottom: -20,
                    right: -20,
                    opacity: 0.1,
                    pointerEvents: "none",
                  }}
                >
                  <TrendingUp
                    style={{
                      width: 128,
                      height: 128,
                      color: "var(--ant-color-text-light-solid)",
                    }}
                  />
                </div>
                <div style={{ position: "relative", zIndex: 1 }}>
                  <Typography.Title
                    level={4}
                    style={{
                      color: "var(--ant-color-text-light-solid)",
                      marginTop: 0,
                      marginBottom: 8,
                    }}
                  >
                    {t("needHelp")}
                  </Typography.Title>
                  <Typography.Text
                    strong
                    style={{
                      ...labelStyle,
                      color: "var(--ant-color-text-light-solid)",
                      display: "block",
                      marginBottom: 24,
                    }}
                  >
                    {t("exchangeSupport")}
                  </Typography.Text>
                  <Link href="/support">
                    <Button ghost>Contact Support</Button>
                  </Link>
                </div>
              </Card>
            </Space>
          </Col>
        </Row>
      </Space>
    </DashboardLayout>
  );
}
