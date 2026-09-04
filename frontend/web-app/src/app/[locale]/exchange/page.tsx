'use client';

import { useState, useCallback, useEffect, useRef } from 'react';
import { useForm, useWatch } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import {
  ArrowRightLeft,
  TrendingUp,
  CheckCircle,
  Clock,
  Info,
  Loader2,
  AlertCircle
} from 'lucide-react';
import { exchangeSchema, type ExchangeRequest } from '@/types';
import { useFxRate, useFxEstimate, useFxConversion, useFxConversions } from '@/hooks';
import { useAuthStore, useUIStore } from '@/stores';
import { SUPPORTED_CURRENCIES } from '@/services/FxService';
import { compareCurrency, formatExactDecimal, parseCurrencyExact, type Money } from '@/lib/currency';
import DashboardLayout from "@/components/DashboardLayout";
import { PageTransition, StaggerContainer, StaggerItem, ButtonMotion } from '@/components/ui/Motion';
import clsx from 'clsx';

export default function ExchangePage() {
  const accountId = useAuthStore((state) => state.accountId);
  const addToast = useUIStore((state) => state.addToast);

  // Form setup
  const { register, handleSubmit, formState: { errors }, setValue, control } = useForm<ExchangeRequest>({
    resolver: zodResolver(exchangeSchema),
    defaultValues: {
      fromCurrency: 'IDR',
      toCurrency: 'USD',
      amount: '',
    }
  });

  // Watch form values
  const fromCurrency = useWatch({ control, name: 'fromCurrency' });
  const toCurrency = useWatch({ control, name: 'toCurrency' });
  const amount = useWatch({ control, name: 'amount' });

  // FX Rate query — gated on the pair only, so the calculator shows a live
  // rate (or a real error) on first paint instead of a perpetual spinner.
  // The amount only gates the conversion *estimate* below.
  const { data: fxRate, isLoading: isLoadingRate, error: rateError, refetch: refetchRate } = useFxRate(
    fromCurrency,
    toCurrency,
    fromCurrency !== toCurrency
  );

  // Estimate conversion (real-time preview)
  const estimateMutation = useFxEstimate();
  const [estimatedAmount, setEstimatedAmount] = useState<Money | null>(null);

  // Execute conversion
  const conversionMutation = useFxConversion();

  // User's conversion history
  const { data: conversions, isLoading: isLoadingConversions } = useFxConversions(!!accountId);

  // Swap currencies
  const handleSwap = useCallback(() => {
    setValue('fromCurrency', toCurrency);
    setValue('toCurrency', fromCurrency);
    setEstimatedAmount(null);
  }, [fromCurrency, toCurrency, setValue]);

  // BUG-FE-022: Use ref for estimateMutation to avoid infinite loop
  // (mutation object is new every render, causing useEffect to re-run)
  const estimateMutationRef = useRef(estimateMutation);
  useEffect(() => { estimateMutationRef.current = estimateMutation; }, [estimateMutation]);

  // Handle amount change with debounce
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

  // React 19 "adjusting state during render" — when the input is cleared or
  // currencies match, reset the previous estimate during render (avoids the
  // cascading-render warning from setState-in-effect).
  if (!(compareCurrency(amount, '0') > 0 && fromCurrency !== toCurrency) && estimatedAmount !== null) {
    setEstimatedAmount(null);
  }

  const displayAmount = estimatedAmount;

  // Format currency
  const formatCurrency = (value: Money | number, currencyCode: string) => {
    const currency = SUPPORTED_CURRENCIES[currencyCode];
    if (!currency) return `${value} ${currencyCode}`;

    return `${currency.symbol}${formatExactDecimal(value, currency.decimalPlaces)}`;
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
        setValue('amount', '');
        setEstimatedAmount(null);
      },
      onError: (error: Error) => {
        // React Query passes an Error-derived value; access axios shape safely.
        const response = (error as { response?: { data?: { message?: string } } }).response;
        addToast(response?.data?.message || 'Exchange failed. Please try again.', 'error');
      }
    });
  };

  // Get currency info
  const fromCurrencyInfo = SUPPORTED_CURRENCIES[fromCurrency];
  const toCurrencyInfo = SUPPORTED_CURRENCIES[toCurrency];

  // Recent conversions (last 5)
  const recentConversions = Array.isArray(conversions) ? conversions.slice(0, 5) : [];

  return (
    <DashboardLayout>
      <PageTransition>
        <div className="space-y-6 lg:space-y-8">
          <StaggerContainer>
            {/* Header */}
            <StaggerItem>
              <div className="mb-6">
                <h2 className="text-3xl font-bold text-foreground tracking-tight">Currency Exchange</h2>
                <p className="text-sm text-muted-foreground font-medium mt-1">
                  Real-time foreign exchange rates with competitive pricing.
                </p>
              </div>
            </StaggerItem>

            <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
              {/* Main Exchange Form */}
              <StaggerItem className="lg:col-span-8 space-y-6">
                <div className="bg-card rounded-2xl p-5 sm:p-6 lg:p-8 border border-border shadow-card relative overflow-hidden">
                  {/* Ambient glow effect */}
                  <div className="absolute top-0 right-0 w-96 h-96 bg-primary/5 rounded-full blur-3xl pointer-events-none" />
                  <div className="absolute bottom-0 left-0 w-64 h-64 bg-primary/3 rounded-full blur-3xl pointer-events-none" />

                  <div className="relative z-10">
                    <h3 className="text-sm font-bold text-foreground mb-6 tracking-widest uppercase">Exchange Calculator</h3>

                    {/* Currency Selector Row */}
                    <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-4 mb-6">
                      {/* From Currency */}
                      <div className="flex-1">
                        <label className="text-xs font-bold text-muted-foreground tracking-widest uppercase mb-3 block">
                          From Currency
                        </label>
                        <div className="relative group">
                          <select
                            {...register('fromCurrency')}
                            className="w-full pl-6 pr-14 h-16 rounded-xl border border-border bg-muted/20 hover:border-primary/30 focus:border-primary focus:ring-4 focus:ring-primary/10 transition-all font-bold text-foreground appearance-none cursor-pointer outline-none"
                            aria-label="From currency"
                          >
                            {Object.values(SUPPORTED_CURRENCIES).map((currency) => (
                              <option key={currency.code} value={currency.code}>
                                {currency.flag} {currency.code} - {currency.name}
                              </option>
                            ))}
                          </select>
                          <div className="absolute right-6 top-1/2 -translate-y-1/2 text-2xl pointer-events-none group-focus-within:scale-110 transition-transform">
                            {fromCurrencyInfo?.flag}
                          </div>
                        </div>
                      </div>

                      {/* Swap Button */}
                        <ButtonMotion className="sm:mt-8">
                          <Button
                            type="button"
                            size="icon"
                            variant="secondary"
                            onClick={handleSwap}
                            className="h-14 w-14 rounded-xl shadow-lg"
                            aria-label="Swap currencies"
                          >
                            <ArrowRightLeft className="h-6 w-6" />
                          </Button>
                        </ButtonMotion>

                      {/* To Currency */}
                      <div className="flex-1">
                        <label className="text-xs font-bold text-muted-foreground tracking-widest uppercase mb-3 block">
                          To Currency
                        </label>
                        <div className="relative group">
                          <select
                            {...register('toCurrency')}
                            className="w-full pl-6 pr-14 h-16 rounded-xl border border-border bg-muted/20 hover:border-primary/30 focus:border-primary focus:ring-4 focus:ring-primary/10 transition-all font-bold text-foreground appearance-none cursor-pointer outline-none"
                            aria-label="To currency"
                          >
                            {Object.values(SUPPORTED_CURRENCIES).map((currency) => (
                              <option key={currency.code} value={currency.code}>
                                {currency.flag} {currency.code} - {currency.name}
                              </option>
                            ))}
                          </select>
                          <div className="absolute right-6 top-1/2 -translate-y-1/2 text-2xl pointer-events-none group-focus-within:scale-110 transition-transform">
                            {toCurrencyInfo?.flag}
                          </div>
                        </div>
                      </div>
                    </div>

                    {/* Amount Input */}
                    <div className="space-y-4 mb-6">
                      <div className="flex justify-between items-center">
                        <label className="text-xs font-bold text-muted-foreground tracking-widest uppercase">
                          Amount
                        </label>
                        {isLoadingRate && fromCurrency !== toCurrency && (
                          <div className="flex items-center gap-2 text-xs text-primary">
                            <Loader2 className="h-3 w-3 animate-spin" />
                            <span className="font-bold tracking-widest uppercase">Updating Rate</span>
                          </div>
                        )}
                      </div>
                      <div className="relative group">
                        <Input
                          {...register('amount')}
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
                      {errors.amount && (
                        <p className="text-destructive text-xs ml-2 font-bold tracking-widest uppercase">
                          {errors.amount.message}
                        </p>
                      )}
                    </div>

                    {/* Rate Display */}
                    {fromCurrency !== toCurrency && (
                      <div className="bg-gradient-to-br from-primary/10 to-primary/5 rounded-xl p-6 border border-primary/20 mb-6">
                        {rateError ? (
                          <div className="flex items-center gap-3 text-destructive">
                            <AlertCircle className="h-5 w-5 flex-shrink-0" />
                            <div>
                              <p className="text-sm font-bold">Unable to fetch exchange rate</p>
                              <button
                                onClick={() => refetchRate()}
                                className="text-xs font-bold underline mt-1"
                              >
                                Try again
                              </button>
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
                    <ButtonMotion className="w-full">
                      <Button
                        type="submit"
                        disabled={conversionMutation.isPending || (fromCurrency === toCurrency) || compareCurrency(amount, '0') <= 0 || !fxRate}
                        className="w-full h-16 rounded-2xl shadow-xl shadow-emerald-500/20"
                        onClick={handleSubmit(onSubmit)}
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
                    </ButtonMotion>
                  </div>
                </div>
              </StaggerItem>

              {/* Sidebar - Recent Conversions & Info */}
              <StaggerItem className="lg:col-span-4 space-y-8">
                {/* Rate Updates Card */}
                <div className="bg-card rounded-2xl p-5 sm:p-6 lg:p-8 border border-border shadow-card">
                  <div className="flex justify-between items-center mb-6">
                    <h3 className="text-xs font-bold text-foreground tracking-widest uppercase">
                      Market Status
                    </h3>
                    <div className="flex items-center gap-2">
                      <div className="h-2 w-2 bg-emerald-500 rounded-full animate-pulse" />
                      <span className="text-xs font-bold text-emerald-600 tracking-widest uppercase">Live</span>
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
                        const statusColor = conversion.status === 'COMPLETED' ? 'text-emerald-500' :
                                           conversion.status === 'PENDING' ? 'text-amber-500' :
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
                <div className="bg-gradient-to-br from-gray-900 to-gray-800 rounded-2xl p-5 sm:p-6 lg:p-8 text-white relative overflow-hidden shadow-2xl">
                  <div className="absolute bottom-[-20px] right-[-20px] opacity-10">
                    <TrendingUp className="h-32 w-32" />
                  </div>
                  <div className="relative z-10">
                    <h3 className="font-bold text-lg mb-2">Need Help?</h3>
                    <p className="text-xs text-gray-400 font-bold tracking-widest uppercase mb-6">
                      Currency exchange support
                    </p>
                    <button className="text-xs font-bold tracking-widest uppercase bg-white/10 px-6 py-3 rounded-xl border border-white/10 hover:bg-white/20 transition-all">
                      Contact Support
                    </button>
                  </div>
                </div>
              </StaggerItem>
            </div>
          </StaggerContainer>
        </div>
      </PageTransition>
    </DashboardLayout>
  );
}
