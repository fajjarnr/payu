'use client';

import { useState, useCallback, useEffect, useRef } from 'react';
import { Button, Input, Form, Select } from 'antd';
import { zodFieldRule } from '@/lib/zodForm';
import {
  ArrowRightLeft,
  TrendingUp,
  CheckCircle,
  Clock,
  Info,
  Loader2,
  AlertCircle
} from '@/components/icons';
import { exchangeSchema, type ExchangeRequest } from '@/types';
import { useFxRate, useFxEstimate, useFxConversion, useFxConversions } from '@/hooks';
import { useAuthStore, useUIStore } from '@/stores';
import { SUPPORTED_CURRENCIES } from '@/services/FxService';
import { compareCurrency, formatExactDecimal, parseCurrencyExact, type Money } from '@/lib/currency';
import DashboardLayout from "@/components/DashboardLayout";
import clsx from 'clsx';
import { useTranslations } from 'next-intl';

export default function ExchangePage() {
  const accountId = useAuthStore((state) => state.accountId);
  const addToast = useUIStore((state) => state.addToast);
  const t = useTranslations('nav');

  const [form] = Form.useForm<ExchangeRequest>();
  const rule = (field: string) => zodFieldRule(form, exchangeSchema, field);

  const fromCurrency = Form.useWatch('fromCurrency', form) ?? 'IDR';
  const toCurrency = Form.useWatch('toCurrency', form) ?? 'USD';
  const amount = Form.useWatch('amount', form) ?? '';

  // FX Rate query — gated on the pair only, so the calculator shows a live
  // rate (or a real error) on first paint instead of a perpetual spinner.
  // The amount only gates the conversion *estimate* below.
  const { data: fxRate, isLoading: isLoadingRate, error: rateError, refetch: refetchRate } = useFxRate(
    fromCurrency,
    toCurrency,
    fromCurrency !== toCurrency
  );

  const estimateMutation = useFxEstimate();
  const [estimatedAmount, setEstimatedAmount] = useState<Money | null>(null);

  const conversionMutation = useFxConversion();

  const { data: conversions, isLoading: isLoadingConversions } = useFxConversions(!!accountId);

  const handleSwap = useCallback(() => {
    form.setFieldsValue({ fromCurrency: toCurrency, toCurrency: fromCurrency });
    setEstimatedAmount(null);
  }, [form, fromCurrency, toCurrency]);

  // BUG-FE-022: Use ref for estimateMutation to avoid infinite loop
  // (mutation object is new every render, causing useEffect to re-run)
  const estimateMutationRef = useRef(estimateMutation);
  useEffect(() => { estimateMutationRef.current = estimateMutation; }, [estimateMutation]);

  useEffect(() => {
    if (compareCurrency(amount, '0') > 0 && fromCurrency !== toCurrency) {
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
  if (!(compareCurrency(amount, '0') > 0 && fromCurrency !== toCurrency) && estimatedAmount !== null) {
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
      addToast('Please log in to perform currency exchange', 'error');
      return;
    }

    if (data.fromCurrency === data.toCurrency) {
      addToast('Please select different currencies', 'warning');
      return;
    }

    conversionMutation.mutate({
      fromCurrency: data.fromCurrency,
      toCurrency: data.toCurrency,
      amount: parseCurrencyExact(data.amount),
    }, {
      onSuccess: () => {
        addToast(`Successfully exchanged ${formatCurrency(parseCurrencyExact(data.amount), data.fromCurrency)} to ${data.toCurrency}`, 'success');
        form.setFieldsValue({ amount: '' });
        setEstimatedAmount(null);
      },
      onError: (error: Error) => {
        // React Query passes an Error-derived value; access axios shape safely.
        const response = (error as { response?: { data?: { message?: string } } }).response;
        addToast(response?.data?.message || 'Exchange failed. Please try again.', 'error');
      }
    });
  };

  const fromCurrencyInfo = SUPPORTED_CURRENCIES[fromCurrency];

  const recentConversions = Array.isArray(conversions) ? conversions.slice(0, 5) : [];

  return (
    <DashboardLayout>

        <div className="space-y-6 lg:space-y-8">

            {/* Header */}

              <div className="mb-6">
                <h1 className="text-2xl font-bold text-foreground tracking-tight">{t('currencyExchange')}</h1>
                <p className="text-sm text-muted-foreground font-medium mt-1">
                  {t('exchangeSubtitle')}
                </p>
              </div>


            <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
              {/* Main Exchange Form */}
              <div className="lg:col-span-8 space-y-6">
                <div className="bg-card rounded-2xl p-5 sm:p-6 lg:p-8 border border-border shadow-card relative overflow-hidden">
                  {/* Ambient glow effect */}
                  <div className="absolute top-0 right-0 w-96 h-96 bg-primary/5 rounded-full blur-3xl pointer-events-none" />
                  <div className="absolute bottom-0 left-0 w-64 h-64 bg-primary/3 rounded-full blur-3xl pointer-events-none" />

                  <Form form={form} onFinish={onValid} initialValues={{ fromCurrency: 'IDR', toCurrency: 'USD', amount: '' }} layout="vertical" className="relative z-10">
                    <h3 className="text-sm font-bold text-foreground mb-6 tracking-widest uppercase">{t('exchangeCalculator')}</h3>

                    {/* Currency Selector Row */}
                    <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-4 mb-6">
                      {/* From Currency */}
                      <div className="flex-1">
                        <Form.Item name="fromCurrency" rules={[rule('fromCurrency')]} className="mb-0" label={<label htmlFor="exchange-from-currency" className="text-xs font-bold text-muted-foreground tracking-widest uppercase mb-3 block">{t('fromCurrency')}</label>}>
                          <Select id="exchange-from-currency"
                            value={fromCurrency}
                            onChange={(v) => form.setFieldsValue({ fromCurrency: v as ExchangeRequest['fromCurrency'] })}
                            className="w-full"
                            aria-label="From currency"
                            options={Object.values(SUPPORTED_CURRENCIES).map((currency) => ({ value: currency.code, label: `${currency.flag} ${currency.code} - ${currency.name}` }))}
                          />
                        </Form.Item>
                      </div>

                      {/* Swap Button */}

                          <Button
                            htmlType="button"
                            shape="circle"
                            onClick={handleSwap}
                            className="h-14 w-14 rounded-xl shadow-lg sm:mt-8"
                            aria-label="Swap currencies"
                            icon={<ArrowRightLeft className="h-6 w-6" />}
                          />


                      {/* To Currency */}
                      <div className="flex-1">
                        <Form.Item name="toCurrency" rules={[rule('toCurrency')]} className="mb-0" label={<label htmlFor="exchange-to-currency" className="text-xs font-bold text-muted-foreground tracking-widest uppercase mb-3 block">To Currency</label>}>
                          <Select id="exchange-to-currency"
                            value={toCurrency}
                            onChange={(v) => form.setFieldsValue({ toCurrency: v as ExchangeRequest['toCurrency'] })}
                            className="w-full"
                            aria-label="To currency"
                            options={Object.values(SUPPORTED_CURRENCIES).map((currency) => ({ value: currency.code, label: `${currency.flag} ${currency.code} - ${currency.name}` }))}
                          />
                        </Form.Item>
                      </div>
                    </div>

                    {/* Amount Input */}
                    <div className="space-y-4 mb-6">
                      <div className="flex justify-between items-center">
                        <label htmlFor="exchange-amount" className="text-xs font-bold text-muted-foreground tracking-widest uppercase">
                          Amount
                        </label>
                        {isLoadingRate && fromCurrency !== toCurrency && (
                          <div className="flex items-center gap-2 text-xs text-primary">
                            <Loader2 className="h-3 w-3 animate-spin" />
                            <span className="font-bold tracking-widest uppercase">Updating Rate</span>
                          </div>
                        )}
                      </div>
                      <Form.Item name="amount" rules={[rule('amount')]} className="mb-0">
                      <div className="relative group">
                        <Input id="exchange-amount"
                          type="number"
                          step="any"
                          min="0"
                          placeholder="0.00"
                          className="pl-16 h-20 text-4xl"
                          aria-label="Amount to exchange"
                        />
                        <div className="absolute left-6 top-1/2 -translate-y-1/2 text-2xl font-bold text-muted-foreground/40 pointer-events-none">
                          {fromCurrencyInfo?.symbol}
                        </div>
                      </div>
                      </Form.Item>
                    </div>

                    {/* Rate Display */}
                    {fromCurrency !== toCurrency && (
                      <div className="bg-gradient-to-br from-primary/10 to-primary/5 rounded-xl p-6 border border-primary/20 mb-6">
                        {rateError ? (
                          <div className="flex items-center gap-3 text-destructive">
                            <AlertCircle className="h-5 w-5 flex-shrink-0" />
                            <div>
                              <p className="text-sm font-bold">Unable to fetch exchange rate</p>
                              <Button type="link" onClick={() => refetchRate()} className="text-xs font-bold underline mt-1">
                                Try again
                              </Button>
                            </div>
                          </div>
                        ) : fxRate ? (
                          <div className="space-y-3">
                            <div className="flex items-center justify-between">
                              <div className="flex items-center gap-2">
                                <TrendingUp className="h-4 w-4 text-primary" />
                                <span className="text-xs font-bold text-muted-foreground tracking-widest uppercase">
                                  Current Rate
                                </span>
                              </div>
                              <div className="flex items-center gap-2">
                                <div className="h-2 w-2 bg-primary rounded-full animate-pulse" />
                                <span className="text-xs font-bold text-primary tracking-widest uppercase">
                                  Live
                                </span>
                              </div>
                            </div>
                            <div className="flex items-baseline gap-3">
                              <span className="text-3xl font-bold text-foreground">
                                1 {fromCurrency}
                              </span>
                              <span className="text-muted-foreground">=</span>
                              <span className="text-3xl font-bold text-primary">
                                {formatExactDecimal(fxRate.rate, 4)} {toCurrency}
                              </span>
                            </div>
                            <div className="flex items-center gap-2 text-xs text-muted-foreground">
                              <Clock className="h-3 w-3" />
                              <span>Valid until {new Date(fxRate.validUntil).toLocaleTimeString()}</span>
                            </div>
                          </div>
                        ) : (
                          <div className="flex items-center justify-center py-4">
                            <Loader2 className="h-6 w-6 animate-spin text-primary" />
                          </div>
                        )}
                      </div>
                    )}

                    {/* Conversion Preview */}
                    {displayAmount !== null && fromCurrency !== toCurrency && (
                      <div className="bg-card rounded-xl p-6 border-2 border-primary/30 mb-6 relative overflow-hidden">
                        <div className="absolute inset-0 bg-gradient-to-r from-primary/5 via-transparent to-primary/5 pointer-events-none" />
                        <div className="relative z-10 flex items-center justify-between">
                          <div>
                            <p className="text-xs font-bold text-muted-foreground tracking-widest uppercase mb-2">
                              You will receive
                            </p>
                            <p className="text-3xl font-bold text-foreground">
                              {formatCurrency(displayAmount, toCurrency)}
                            </p>
                          </div>
                          <div className="h-12 w-12 rounded-full bg-primary/10 flex items-center justify-center">
                            <CheckCircle className="h-6 w-6 text-primary" />
                          </div>
                        </div>
                      </div>
                    )}

                    {/* Info Box */}
                    {fromCurrency === toCurrency ? (
                      <div className="bg-info/10 rounded-xl p-6 border border-info/20 mb-6">
                        <div className="flex items-start gap-3">
                          <Info className="h-5 w-5 text-info flex-shrink-0 mt-0.5" />
                          <p className="text-sm font-medium text-foreground">
                            Please select different currencies to perform an exchange.
                          </p>
                        </div>
                      </div>
                    ) : (
                      <div className="bg-muted/50 rounded-xl p-6 border border-border mb-6">
                        <div className="flex items-start gap-3">
                          <Info className="h-5 w-5 text-muted-foreground flex-shrink-0 mt-0.5" />
                          <div className="text-sm text-muted-foreground">
                            <p className="font-medium text-foreground mb-2">Exchange Information</p>
                            <ul className="space-y-1 text-xs">
                              <li>Exchange rates are updated every 60 seconds</li>
                              <li>No hidden fees - the rate you see is the rate you get</li>
                              <li>Minimum exchange amount: 10,000 {fromCurrency}</li>
                            </ul>
                          </div>
                        </div>
                      </div>
                    )}

                    {/* CTA Button */}

                      <Button
                        type="primary"
                        htmlType="submit"
                        size="large"
                        className="w-full"
                        disabled={conversionMutation.isPending || (fromCurrency === toCurrency) || compareCurrency(amount, '0') <= 0 || !fxRate}
                      >
                        {conversionMutation.isPending ? (
                          <div className="flex items-center gap-2">
                            <Loader2 className="h-5 w-5 animate-spin" />
                            <span>Processing Exchange...</span>
                          </div>
                        ) : (
                          <span>Exchange Currency Now</span>
                        )}
                      </Button>

                  </Form>
                </div>
              </div>

              {/* Sidebar - Recent Conversions & Info */}
              <div className="lg:col-span-4 space-y-8">
                {/* Rate Updates Card */}
                <div className="bg-card rounded-2xl p-5 sm:p-6 lg:p-8 border border-border shadow-card">
                  <div className="flex justify-between items-center mb-6">
                    <h3 className="text-xs font-bold text-foreground tracking-widest uppercase">
                      Market Status
                    </h3>
                    <div className="flex items-center gap-2">
                      <div className="h-2 w-2 bg-primary rounded-full animate-pulse" />
                      <span className="text-xs font-bold text-primary-dark tracking-widest uppercase">Live</span>
                    </div>
                  </div>
                  <div className="space-y-4">
                    <div className="flex items-center justify-between">
                      <span className="text-xs text-muted-foreground">Last Update</span>
                      <span className="text-xs font-bold text-foreground">
                        {fxRate ? new Date(fxRate.validFrom).toLocaleTimeString() : '--:--:--'}
                      </span>
                    </div>
                    <div className="flex items-center justify-between">
                      <span className="text-xs text-muted-foreground">Next Update</span>
                      <span className="text-xs font-bold text-foreground">
                        {fxRate ? new Date(new Date(fxRate.validFrom).getTime() + 60000).toLocaleTimeString() : '--:--:--'}
                      </span>
                    </div>
                    <div className="flex items-center justify-between">
                      <span className="text-xs text-muted-foreground">Supported Pairs</span>
                      <span className="text-xs font-bold text-primary">
                        {Object.keys(SUPPORTED_CURRENCIES).length} currencies
                      </span>
                    </div>
                  </div>
                </div>

                {/* Recent Conversions */}
                <div className="bg-card rounded-2xl p-5 sm:p-6 lg:p-8 border border-border shadow-card">
                  <div className="flex justify-between items-center mb-6">
                    <h3 className="text-xs font-bold text-foreground tracking-widest uppercase">
                      Recent Exchanges
                    </h3>
                  </div>

                  {isLoadingConversions ? (
                    <div className="flex items-center justify-center py-8">
                      <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
                    </div>
                  ) : recentConversions.length === 0 ? (
                    <div className="text-center py-8">
                      <p className="text-sm text-muted-foreground">
                        No exchange history yet
                      </p>
                    </div>
                  ) : (
                    <div className="space-y-4">
                      {recentConversions.map((conversion) => {
                        const fromInfo = SUPPORTED_CURRENCIES[conversion.fromCurrency];
                        const toInfo = SUPPORTED_CURRENCIES[conversion.toCurrency];
                        const statusColor = conversion.status === 'COMPLETED' ? 'text-primary' :
                                           conversion.status === 'PENDING' ? 'text-warning' :
                                           conversion.status === 'FAILED' ? 'text-destructive' : 'text-muted-foreground';

                        return (
                          <div
                            key={conversion.id}
                            className="bg-muted/50 p-4 rounded-xl border border-border hover:border-border/80 transition-all"
                          >
                            <div className="flex items-center justify-between mb-2">
                              <div className="flex items-center gap-2 text-lg">
                                <span>{fromInfo?.flag}</span>
                                <ArrowRightLeft className="h-4 w-4 text-muted-foreground" />
                                <span>{toInfo?.flag}</span>
                              </div>
                              <span className={clsx("text-xs font-bold tracking-widest uppercase", statusColor)}>
                                {conversion.status.toLowerCase()}
                              </span>
                            </div>
                            <div className="flex items-baseline gap-2">
                              <span className="text-sm font-bold text-foreground">
                                {fromInfo?.symbol}{formatExactDecimal(conversion.fromAmount, fromInfo?.decimalPlaces)}
                              </span>
                              <span className="text-muted-foreground">→</span>
                              <span className="text-sm font-bold text-primary">
                                {toInfo?.symbol}{formatExactDecimal(conversion.toAmount, toInfo?.decimalPlaces)}
                              </span>
                            </div>
                            <p className="text-xs text-muted-foreground mt-1">
                              {new Date(conversion.conversionDate).toLocaleString()}
                            </p>
                          </div>
                        );
                      })}
                    </div>
                  )}
                </div>

                {/* Help Card */}
                <div className="bg-gradient-to-br from-text-primary to-text-primary rounded-2xl p-5 sm:p-6 lg:p-8 text-surface relative overflow-hidden shadow-2xl">
                  <div className="absolute bottom-[-20px] right-[-20px] opacity-10">
                    <TrendingUp className="h-32 w-32" />
                  </div>
                  <div className="relative z-10">
                    <h3 className="font-bold text-lg mb-2">{t('needHelp')}</h3>
                    <p className="text-xs text-text-disabled font-bold tracking-widest uppercase mb-6">
                      {t('exchangeSupport')}
                    </p>
                    <Button type="default" className="text-xs font-bold tracking-widest uppercase bg-surface/10 px-6 py-3 rounded-xl border border-surface/10 hover:bg-surface/20 transition-all">
                      Contact Support
                    </Button>
                  </div>
                </div>
              </div>
            </div>

        </div>

    </DashboardLayout>
  );
}
