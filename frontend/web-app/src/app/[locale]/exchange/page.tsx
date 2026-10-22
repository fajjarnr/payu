"use client";

import { useState, useCallback, useEffect, useRef } from "react";
import type { CSSProperties } from "react";
import {
  Alert,
  Button,
  Card,
  Col,
  Form,
  Input,
  Row,
  Select,
  Space,
  Spin,
  Tag,
  Typography,
} from "antd";
import EmptyState from "@/components/EmptyState";
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
import { useAuthStore } from "@/stores";
import { SUPPORTED_CURRENCIES } from "@/services/FxService";
import {
  compareCurrency,
  formatCurrency as formatMoney,
  formatExactDecimal,
  parseCurrencyExact,
  type Money,
} from "@/lib/currency";
import DashboardLayout from "@/components/DashboardLayout";
import { notify } from "@/lib/notify";
import { useLocale, useTranslations } from "next-intl";
import { SkipLink } from "@/lib/a11y";
import { INK_ON_EMERALD } from '@/theme/tokens';

const labelStyle: CSSProperties = {
  fontSize: 12,
  fontWeight: 700,
  color: "var(--ant-color-text-secondary)",
  textTransform: "uppercase",
  letterSpacing: "0.1em",
};

const capsPrimaryStyle: CSSProperties = {
  ...labelStyle,
  color: "var(--ant-color-primary-text)",
};

export default function ExchangePage() {
  const accountId = useAuthStore((state) => state.accountId);
  const t = useTranslations("nav");
  const locale = useLocale();
  const bcp47Locale = locale === "id" ? "id-ID" : "en-US";

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
  // R-06: a failed estimate must be visible, not a silently empty preview.
  const [estimateError, setEstimateError] = useState(false);

  const conversionMutation = useFxConversion();

  const {
    data: conversions,
    isLoading: isLoadingConversions,
    isError: conversionsError,
    refetch: refetchConversions,
  } = useFxConversions(!!accountId);

  const handleSwap = useCallback(() => {
    form.setFieldsValue({ fromCurrency: toCurrency, toCurrency: fromCurrency });
    setEstimatedAmount(null);
    setEstimateError(false);
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
          setEstimateError(false);
        } catch {
          setEstimatedAmount(null);
          setEstimateError(true);
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

  // Per-currency symbol + decimals via the shared helper (SUPPORTED_CURRENCIES
  // holds the symbol/decimal lookup; lib owns the grouping/rounding).
  const formatFxAmount = (value: Money | number, currencyCode: string) => {
    const currency = SUPPORTED_CURRENCIES[currencyCode];
    if (!currency) return `${value} ${currencyCode}`;

    return formatMoney(value, {
      symbol: currency.symbol,
      withDecimals: currency.decimalPlaces > 0,
      locale: bcp47Locale,
    });
  };

  const onValid = (values: ExchangeRequest) => {
    onSubmit(exchangeSchema.parse(values));
  };
  const onSubmit = async (data: ExchangeRequest) => {
    if (!accountId) {
      notify.error(t("exchangeLoginRequired"));
      return;
    }

    if (data.fromCurrency === data.toCurrency) {
      notify.warning(t("exchangeSelectDifferentCurrencies"));
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
          notify.info(
            t("exchangeSuccess", {
              amount: formatFxAmount(
                parseCurrencyExact(data.amount),
                data.fromCurrency,
              ),
              currency: data.toCurrency,
            }),
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
          notify.info(
            response?.data?.message || t("exchangeFailed"),
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
      <SkipLink />
      <Space orientation="vertical" size={24} style={{ width: "100%" }}>
        <div>
          <Typography.Title level={1} style={{ marginTop: 0, marginBottom: 4 }}>
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
                  maxWidth: "100%",
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
                          aria-label={t("exchangeFromCurrencyAria")}
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
                        aria-label={t("exchangeSwapCurrencies")}
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
                          aria-label={t("exchangeToCurrencyAria")}
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
                    orientation="vertical"
                    size={16}
                    style={{ width: "100%", marginTop: 24 }}
                  >
                    <Row justify="space-between" align="middle">
                      <label htmlFor="exchange-amount" style={labelStyle}>
                        {t("exchangeAmount")}
                      </label>
                      {isLoadingRate && fromCurrency !== toCurrency && (
                        <Space size={8}>
                          <Spin size="small" />
                          <Typography.Text strong style={capsPrimaryStyle}>
                            {t("exchangeUpdatingRate")}
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
                        aria-label={t("exchangeAmountAria")}
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
                          message={t("exchangeRateError")}
                          action={
                            <Button
                              type="link"
                              size="small"
                              onClick={() => refetchRate()}
                            >
                              {t("exchangeTryAgain")}
                            </Button>
                          }
                        />
                      ) : fxRate ? (
                        <Space
                          orientation="vertical"
                          size={12}
                          style={{ width: "100%" }}
                        >
                          <Row justify="space-between" align="middle">
                            <Space size={8}>
                              <TrendingUp
                                style={{
                                  width: 16,
                                  height: 16,
                                  color: "var(--ant-color-primary-text)",
                                }}
                              />
                              <Typography.Text strong style={labelStyle}>
                                {t("exchangeCurrentRate")}
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
                                {t("exchangeLive")}
                              </Typography.Text>
                            </Space>
                          </Row>
                          <Space size={12} align="baseline">
                            <Typography.Title level={2} style={{ margin: 0 }}>
                              1 {fromCurrency}
                            </Typography.Title>
                            <Typography.Text type="secondary">
                              =
                            </Typography.Text>
                            <Typography.Title
                              level={2}
                              style={{
                                margin: 0,
                                color: "var(--ant-color-primary-text)",
                              }}
                            >
                              {formatExactDecimal(
                                fxRate.rate,
                                4,
                                bcp47Locale,
                              )}{" "}
                              {toCurrency}
                            </Typography.Title>
                          </Space>
                          <Space size={8}>
                            <Clock style={{ width: 12, height: 12 }} />
                            <Typography.Text
                              type="secondary"
                              style={{ fontSize: 12 }}
                            >
                              {t("exchangeValidUntil")}{" "}
                              {new Date(
                                fxRate.validUntil,
                              ).toLocaleTimeString(bcp47Locale)}
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
                  {estimateError && fromCurrency !== toCurrency && (
                    <Alert
                      type="error"
                      showIcon
                      style={{ marginTop: 24 }}
                      message={t("exchangeRateError")}
                      action={
                        <Button size="small" onClick={() => setEstimateError(false)}>
                          {t("exchangeTryAgain")}
                        </Button>
                      }
                    />
                  )}
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
                            {t("exchangeYouWillReceive")}
                          </Typography.Text>
                          <Typography.Title level={2} style={{ margin: 0 }}>
                            {formatFxAmount(displayAmount, toCurrency)}
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
                              color: "var(--ant-color-primary-text)",
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
                        message={t("exchangeSelectDifferentCurrenciesAlert")}
                      />
                    ) : (
                      <Alert
                        type="info"
                        showIcon
                        message={t("exchangeInformation")}
                        description={
                          <ul
                            style={{
                              margin: "8px 0 0",
                              paddingLeft: 20,
                              fontSize: 12,
                            }}
                          >
                            <li>{t("exchangeRatesUpdated")}</li>
                            <li>{t("exchangeNoHiddenFees")}</li>
                            <li>
                              {t("exchangeMinimumAmount", {
                                amount: 10000,
                                currency: fromCurrency,
                              })}
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
                        ? t("exchangeProcessing")
                        : t("exchangeNow")}
                    </Button>
                  </div>
                </Form>
              </div>
            </Card>
          </Col>

          {/* Sidebar - Recent Conversions & Info */}
          <Col xs={24} lg={8}>
            <Space orientation="vertical" size={24} style={{ width: "100%" }}>
              {/* Rate Updates Card */}
              <Card>
                <Row
                  justify="space-between"
                  align="middle"
                  style={{ marginBottom: 24 }}
                >
                  <Typography.Text strong style={labelStyle}>
                    {t("exchangeMarketStatus")}
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
                      {t("exchangeLive")}
                    </Typography.Text>
                  </Space>
                </Row>
                <Space orientation="vertical" size={16} style={{ width: "100%" }}>
                  <Row justify="space-between" align="middle">
                    <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                      {t("exchangeLastUpdate")}
                    </Typography.Text>
                    <Typography.Text strong style={{ fontSize: 12 }}>
                      {fxRate
                        ? new Date(fxRate.validFrom).toLocaleTimeString(
                            bcp47Locale,
                          )
                        : "--:--:--"}
                    </Typography.Text>
                  </Row>
                  <Row justify="space-between" align="middle">
                    <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                      {t("exchangeNextUpdate")}
                    </Typography.Text>
                    <Typography.Text strong style={{ fontSize: 12 }}>
                      {fxRate
                        ? new Date(
                            new Date(fxRate.validFrom).getTime() + 60000,
                          ).toLocaleTimeString(bcp47Locale)
                        : "--:--:--"}
                    </Typography.Text>
                  </Row>
                  <Row justify="space-between" align="middle">
                    <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                      {t("exchangeSupportedPairs")}
                    </Typography.Text>
                    <Typography.Text
                      strong
                      style={{
                        fontSize: 12,
                        color: "var(--ant-color-primary-text)",
                      }}
                    >
                      {t("exchangeCurrencyCount", {
                        count: Object.keys(SUPPORTED_CURRENCIES).length,
                      })}
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
                  {t("exchangeRecent")}
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
                ) : conversionsError ? (
                  // R-06: a failed history load is not an empty history.
                  <Alert
                    type="error"
                    showIcon
                    message={t("exchangeHistoryError")}
                    action={
                      <Button size="small" onClick={() => void refetchConversions()}>
                        {t("exchangeTryAgain")}
                      </Button>
                    }
                  />
                ) : recentConversions.length === 0 ? (
                  <EmptyState
                    title={t("exchangeHistoryEmpty")}
                    description={t("exchangeHistoryEmptyDesc")}
                  />
                ) : (
                  <Space
                    orientation="vertical"
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
                                bcp47Locale,
                              )}
                            </Typography.Text>
                            <Typography.Text type="secondary">
                              →
                            </Typography.Text>
                            <Typography.Text
                              strong
                              style={{
                                fontSize: 14,
                                color: "var(--ant-color-primary-text)",
                              }}
                            >
                              {toInfo?.symbol}
                              {formatExactDecimal(
                                conversion.toAmount,
                                toInfo?.decimalPlaces,
                                bcp47Locale,
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
                            ).toLocaleString(bcp47Locale)}
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
                    level={2}
                    style={{
                      color: INK_ON_EMERALD,
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
                      color: INK_ON_EMERALD,
                      display: "block",
                      marginBottom: 24,
                    }}
                  >
                    {t("exchangeSupport")}
                  </Typography.Text>
                  <Button
                    ghost
                    href="/support"
                    // Ghost default text is white (1.99:1 on the emerald fill);
                    // ink labels clear AA on the primary surface.
                    style={{ color: INK_ON_EMERALD, borderColor: INK_ON_EMERALD }}
                  >
                    {t("contactSupport")}
                  </Button>
                </div>
              </Card>
            </Space>
          </Col>
        </Row>
      </Space>
    </DashboardLayout>
  );
}
