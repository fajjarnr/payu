'use client';

import { useSearchParams } from 'next/navigation';
import { Link } from '@/lib/navigation';
import Image from 'next/image';
import { Button } from '@/components/ui/button';
import { CheckCircle2, ShieldCheck, ArrowRight, Lock } from 'lucide-react';
import { useTranslations } from 'next-intl';
import { Suspense, useEffect } from 'react';

/**
 * Page wrapper — provides Suspense boundary required by useSearchParams().
 * Next.js 16 bails out of static rendering when useSearchParams() is used
 * outside a Suspense boundary.
 */
export default function LoginPage() {
  return (
    <Suspense fallback={<LoginSkeleton />}>
      <LoginForm />
    </Suspense>
  );
}

function LoginSkeleton() {
  return (
    <div className="min-h-screen w-full flex bg-background animate-pulse">
      <div className="hidden lg:flex w-1/2 bg-zinc-900" />
      <div className="flex-1 flex items-center justify-center p-8">
        <div className="w-full max-w-[420px] space-y-8">
          <div className="h-10 w-48 bg-muted rounded-xl" />
          <div className="h-5 w-64 bg-muted/60 rounded-xl" />
          <div className="h-12 bg-emerald-800/50 rounded-lg" />
        </div>
      </div>
    </div>
  );
}

/**
 * LOGIN-003: the browser is redirected to Keycloak's own login page through
 * the BFF OIDC authorize endpoint (authorization-code + PKCE). Credentials
 * never touch the web-app or auth-service — Keycloak authenticates the user.
 */
function LoginForm() {
  const t = useTranslations('auth');
  const searchParams = useSearchParams();
  const error = searchParams.get('error');

  useEffect(() => {
    document.title = `${t('loginButton')} | PayU Digital Banking`;
  }, [t]);

  return (
    <div className="min-h-screen w-full flex bg-background font-inter">
      {/* Left Panel - Branding (Hidden on mobile) */}
      <aside className="hidden lg:flex flex-col justify-between w-1/2 bg-zinc-900 border-r border-border/10 p-8 relative overflow-hidden text-white" aria-label="Branding">
        {/* Background Effects */}
        <div className="absolute top-0 right-0 w-[800px] h-[800px] bg-emerald-500/10 rounded-full blur-[120px] -translate-y-1/2 translate-x-1/2" aria-hidden="true" />
        <div className="absolute bottom-0 left-0 w-[600px] h-[600px] bg-bank-green/10 rounded-full blur-[100px] translate-y-1/2 -translate-x-1/4" aria-hidden="true" />

        {/* Pattern Overlay */}
        <div className="absolute inset-0 opacity-[0.03] bg-[url('https://grainy-gradients.vercel.app/noise.svg')]" aria-hidden="true" />

        <div className="relative z-10 text-center">
            <Link href="/" className="flex items-center gap-3 w-fit mx-auto lg:mx-0">
                <div className="w-10 h-10 rounded-xl flex items-center justify-center overflow-hidden shadow-lg shadow-emerald-500/20">
                    <Image src="/logo.svg" alt="PayU Brand Logo" width={40} height={40} />
                </div>
                <span className="text-2xl font-bold tracking-tight text-white">PayU</span>
            </Link>
        </div>

        <div className="relative z-10 max-w-lg space-y-6 mx-auto lg:mx-0 text-center lg:text-left">
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-emerald-900/30 border border-emerald-400/30 text-emerald-300 text-xs font-bold tracking-widest uppercase mb-4">
                <ShieldCheck className="w-4 h-4" />
                <span>{t('branding.tag')}</span>
            </div>
            <h1 className="text-5xl font-bold leading-tight tracking-tight">
               {t('branding.title')}
            </h1>
            <p className="text-lg text-zinc-200 leading-relaxed">
                {t('branding.desc')}
            </p>

            <div className="pt-8 space-y-4">
                {[
                    t('branding.features.encryption'),
                    t('branding.features.monitoring'),
                    t('branding.features.qris')
                ].map((feature, i) => (
                    <div key={i} className="flex items-center gap-3 text-zinc-100">
                        <CheckCircle2 className="w-5 h-5 text-emerald-400" />
                        <span className="font-medium">{feature}</span>
                    </div>
                ))}
            </div>
        </div>

        <div className="relative z-10 text-zinc-300 text-xs font-mono text-center lg:text-left">
            {t('branding.footer')}
        </div>
      </aside>

      {/* Right Panel - Sign in */}
      <main className="flex-1 flex items-center justify-center p-8 bg-background relative" aria-labelledby="login-title">
        <div className="w-full max-w-[420px] space-y-8">
            <div className="text-center lg:text-left space-y-2">
                <h2 id="login-title" className="text-3xl font-bold tracking-tight">{t('welcomeBack')}</h2>
                <p className="text-muted-foreground">{t('subtitle')}</p>
            </div>

            {error && (
              <p className="rounded-xl border border-destructive/30 bg-destructive/10 px-4 py-3 text-sm font-medium text-destructive" role="alert" data-testid="login-error">
                {t('loginFailed')}
              </p>
            )}

            <div className="space-y-6">
                <Button
                    type="button"
                    data-testid="login-submit-button"
                    className="w-full h-12 bg-emerald-800 hover:bg-emerald-700 text-white font-bold text-base shadow-lg shadow-emerald-800/20 transition-all active:scale-[0.98]"
                    onClick={() => { window.location.href = '/api/auth/authorize'; }}
                >
                    <Lock className="mr-2 h-4 w-4" />
                    {t('loginButton')}
                </Button>
                <p className="text-xs text-muted-foreground text-center">
                    {t('oidcNote')}
                </p>
            </div>

            <div className="relative" aria-hidden="true">
                <div className="absolute inset-0 flex items-center">
                    <span className="w-full border-t border-border" />
                </div>
                <div className="relative flex justify-center text-xs uppercase">
                    <span className="bg-background px-4 text-muted-foreground font-medium">{t('or')}</span>
                </div>
            </div>

            <div className="text-center text-sm">
                <span className="text-muted-foreground">{t('noAccount')}</span>{" "}
                <Link href="/onboarding" data-testid="register-link" className="font-bold text-emerald-600 hover:text-emerald-700 hover:underline inline-flex items-center">
                    {t('registerLink')} <ArrowRight className="ml-1 w-3 h-3" />
                </Link>
            </div>
        </div>
      </main>
    </div>

  );
}
