'use client';

import { useMutation } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { loginSchema, LoginRequest } from '@/types';
import { useSearchParams } from 'next/navigation';
import { Link, useRouter } from '@/lib/navigation';
import Image from 'next/image';
import { useAuthStore } from '@/stores';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { CheckCircle2, ShieldCheck, ArrowRight, Loader2 } from 'lucide-react';
import { useTranslations } from 'next-intl';
import { motion } from 'framer-motion';
import { Suspense, useEffect } from 'react';
import { toast } from 'sonner';

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
          <div className="space-y-4">
            <div className="h-12 bg-muted/30 rounded-lg" />
            <div className="h-12 bg-muted/30 rounded-lg" />
          </div>
          <div className="h-12 bg-emerald-800/50 rounded-lg" />
        </div>
      </div>
    </div>
  );
}

function LoginForm() {
  const t = useTranslations('auth');
  const searchParams = useSearchParams();
  const router = useRouter();
  const setAuth = useAuthStore((state) => state.setAuth);

  // Read callbackUrl set by middleware when redirecting unauthenticated users.
  // Sanitize: only allow relative paths to prevent open-redirect attacks.
  const rawCallback = searchParams.get('callbackUrl') || '/dashboard';
  const callbackUrl = rawCallback.startsWith('/') && !rawCallback.startsWith('//') ? rawCallback : '/dashboard';

  const { register, handleSubmit, formState: { errors } } = useForm<LoginRequest>({
    resolver: zodResolver(loginSchema)
  });

  const mutation = useMutation({
    mutationFn: async (data: LoginRequest) => {
      const res = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify(data),
      });
      if (!res.ok) {
        const err = await res.json().catch(() => ({ message: 'Login failed' }));
        throw new Error(err.message || t('loginFailed'));
      }
      return res.json();
    },
    onSuccess: (response) => {
      const user = response.data?.user;
      
      if (response.data?.mfa_required) {
        toast.warning(response.data.message || t('mfaRequired'));
        return;
      }
      
      if (user) {
        setAuth(user, user.accountId || user.id);
        toast.success(t('loginSuccess') || 'Login successful!');
        // Use locale-aware router for navigation (BUG-I18N-002)
        router.push(callbackUrl);
      } else {
        toast.error(t('invalidResponse'));
      }
    },
    onError: (error) => {
      console.error('Login failed:', error);
      const msg = error instanceof Error ? error.message : t('loginFailed');
      toast.error(msg);
    },
  });

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

      {/* Right Panel - Form */}
      <main className="flex-1 flex items-center justify-center p-8 bg-background relative" aria-labelledby="login-title">
        <div className="w-full max-w-[420px] space-y-8">
            <div className="text-center lg:text-left space-y-2">
                <h2 id="login-title" className="text-3xl font-bold tracking-tight">{t('welcomeBack')}</h2>
                <p className="text-muted-foreground">{t('subtitle')}</p>
            </div>

            <form onSubmit={handleSubmit((data) => mutation.mutate(data))} className="space-y-6">
                <div className="space-y-4">
                    <div className="space-y-2">
                        <Label htmlFor="username">Username</Label>
                        <Input
                            id="username"
                            data-testid="username-input"
                            placeholder="username123"
                            {...register('username')}
                            className="h-12 bg-muted/30"
                            disabled={mutation.isPending}
                            aria-invalid={!!errors.username}
                        />
                         {errors.username && <p className="text-red-500 text-xs font-medium" role="alert">{errors.username.message}</p>}
                    </div>
                    <div className="space-y-2">
                        <div className="flex items-center justify-between">
                            <Label htmlFor="password">Password</Label>
                            <Link href="/forgot-password" data-testid="forgot-password-link" className="text-xs font-bold text-emerald-600 hover:text-emerald-700 hover:underline">
                                {t('forgotPassword')}
                            </Link>
                        </div>
                        <Input
                            id="password"
                            data-testid="password-input"
                            type="password"
                            placeholder="••••••••"
                            {...register('password')}
                            className="h-12 bg-muted/30"
                            disabled={mutation.isPending}
                            aria-invalid={!!errors.password}
                        />
                        {errors.password && <p className="text-red-500 text-xs font-medium" role="alert">{errors.password.message}</p>}
                    </div>
                </div>

                <Button
                    type="submit"
                    data-testid="login-submit-button"
                    className="w-full h-12 bg-emerald-800 hover:bg-emerald-700 text-white font-bold text-base shadow-lg shadow-emerald-800/20 transition-all active:scale-[0.98]"
                    disabled={mutation.isPending}
                >
                    {mutation.isPending ? (
                        <>
                            <Loader2 className="mr-2 h-4 w-4 animate-spin" /> {t('loggingIn')}
                        </>
                    ) : (
                        t('loginButton')
                    )}
                </Button>
            </form>

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
