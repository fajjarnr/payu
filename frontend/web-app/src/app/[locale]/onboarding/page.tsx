'use client';

import { useEffect } from 'react';

import { useMutation } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { registerUserSchema, RegisterUserRequest } from '@/types';
import api from '@/lib/api';
import { useRouter } from 'next/navigation';
import { 
  Camera, 
  ChevronRight, 
  CheckCircle2, 
  ShieldCheck, 
  ArrowLeft,
  Loader2,
  ScanFace,
  Fingerprint
} from 'lucide-react';
import { useState } from 'react';
import Link from 'next/link';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { motion, AnimatePresence } from 'framer-motion';
import { useTranslations } from 'next-intl';

export default function OnboardingPage() {
  const t = useTranslations('auth.onboarding');
  const router = useRouter();
  const [step, setStep] = useState(1);

  const { register, handleSubmit, formState: { errors } } = useForm<RegisterUserRequest>({
    resolver: zodResolver(registerUserSchema)
  });

  const mutation = useMutation({
    mutationFn: (data: RegisterUserRequest) => api.post('/accounts/register', data),
    onSuccess: () => {
      setStep(3);
      setTimeout(() => router.push('/login'), 2500);
    },
    onError: (error) => {
      console.error('Registration failed:', error);
      alert('Pendaftaran gagal. Silakan coba lagi.');
    }
  });

  useEffect(() => {
    const stepTitles = [t('steps.identity'), t('steps.profile'), t('steps.complete')];
    document.title = `${stepTitles[step - 1]} | PayU Digital Banking`;
  }, [step, t]);

  return (
    <div className="min-h-screen w-full flex bg-background font-inter">
      {/* Left Panel - Branding */}
      <aside className="hidden lg:flex flex-col justify-between w-[45%] bg-zinc-900 border-r border-border/10 p-12 relative overflow-hidden text-white" aria-label="Branding">
        {/* Background Effects */}
        <div className="absolute top-0 right-0 w-[800px] h-[800px] bg-emerald-500/10 rounded-full blur-[120px] -translate-y-1/2 translate-x-1/2" aria-hidden="true" />
        <div className="absolute bottom-0 left-0 w-[600px] h-[600px] bg-blue-600/10 rounded-full blur-[100px] translate-y-1/2 -translate-x-1/4" aria-hidden="true" />
        <div className="absolute inset-0 opacity-[0.03] bg-[url('https://grainy-gradients.vercel.app/noise.svg')]" aria-hidden="true" />

        <div className="relative z-10">
          <Link href="/" className="flex items-center gap-3 w-fit hover:opacity-80 transition-opacity" aria-label={t('back')}>
            <ArrowLeft className="w-5 h-5 text-white/70" />
            <span className="font-medium text-white/90">{t('back')}</span>
          </Link>
        </div>

        <div className="relative z-10 max-w-lg space-y-8">
            <div className="space-y-4">
                <div className="w-16 h-16 bg-emerald-500/10 rounded-2xl flex items-center justify-center border border-emerald-500/20 shadow-lg shadow-emerald-500/10" aria-hidden="true">
                    <ScanFace className="w-8 h-8 text-emerald-400" />
                </div>
                <h1 className="text-4xl font-bold leading-tight tracking-tight">
                    {t('branding.title')}
                </h1>
                <p className="text-zinc-400 leading-relaxed text-lg">
                    {t('branding.desc')}
                </p>
            </div>

            <div className="space-y-6 pt-4">
                <div className="flex items-start gap-4 p-4 rounded-xl bg-white/5 border border-white/10 backdrop-blur-sm">
                    <Fingerprint className="w-6 h-6 text-emerald-400 shrink-0 mt-1" aria-hidden="true" />
                    <div>
                        <h3 className="font-bold text-white mb-1">{t('branding.features.ekyc.title')}</h3>
                        <p className="text-sm text-zinc-400">{t('branding.features.ekyc.desc')}</p>
                    </div>
                </div>
                <div className="flex items-start gap-4 p-4 rounded-xl bg-white/5 border border-white/10 backdrop-blur-sm">
                    <ShieldCheck className="w-6 h-6 text-emerald-400 shrink-0 mt-1" aria-hidden="true" />
                    <div>
                        <h3 className="font-bold text-white mb-1">{t('branding.features.data.title')}</h3>
                        <p className="text-sm text-zinc-400">{t('branding.features.data.desc')}</p>
                    </div>
                </div>
            </div>
        </div>

        <div className="relative z-10 flex items-center gap-2 text-zinc-500 text-xs font-mono">
           <div className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse" aria-hidden="true" />
           {t('branding.system')} • v2.4.0
        </div>
      </aside>

      {/* Right Panel - Form Flow */}
      <main className="flex-1 flex flex-col items-center justify-center p-8 bg-background relative" aria-labelledby="onboarding-title">
        <div className="w-full max-w-[520px]">
            {/* Progress Steps */}
            <nav className="mb-10" aria-label="Registration Progress">
                <div className="flex items-center justify-between relative">
                    <div className="absolute left-0 right-0 top-1/2 h-[2px] bg-muted -z-10" aria-hidden="true" />
                    {[1, 2, 3].map((s) => (
                        <div key={s} 
                             className={`w-10 h-10 rounded-full flex items-center justify-center text-sm font-bold border-4 transition-all duration-500 ${step >= s ? 'bg-emerald-600 border-emerald-100 text-white' : 'bg-background border-muted text-muted-foreground'}`}
                             aria-current={step === s ? 'step' : undefined}
                        >
                            {step > s ? <CheckCircle2 className="w-5 h-5 text-white" aria-label="Complete" /> : s}
                        </div>
                    ))}
                </div>
                <div className="flex justify-between mt-2 text-xs font-bold text-muted-foreground px-1">
                    <span className={step >= 1 ? 'text-emerald-700' : ''}>{t('steps.identity')}</span>
                    <span className={step >= 2 ? 'text-emerald-700' : ''}>{t('steps.profile')}</span>
                    <span className={step >= 3 ? 'text-emerald-700' : ''}>{t('steps.complete')}</span>
                </div>
            </nav>
            
            <AnimatePresence mode="wait">
                {step === 1 && (
                    <motion.div 
                        key="step1"
                        initial={{ opacity: 0, x: 20 }}
                        animate={{ opacity: 1, x: 0 }}
                        exit={{ opacity: 0, x: -20 }}
                        className="space-y-8"
                    >
                        <div className="text-center space-y-2">
                            <h2 id="onboarding-title" className="text-2xl font-bold">{t('step1.title')}</h2>
                            <p className="text-muted-foreground">{t('step1.subtitle')}</p>
                        </div>

                        <div className="border-2 border-dashed border-muted-foreground/25 hover:border-emerald-500/50 hover:bg-emerald-50/50 dark:hover:bg-emerald-900/10 rounded-2xl p-12 transition-all cursor-pointer group flex flex-col items-center justify-center text-center gap-4" tabIndex={0} role="button" aria-label={t('step1.clickToUpload')}>
                            <div className="w-16 h-16 rounded-full bg-emerald-100 dark:bg-emerald-900/30 flex items-center justify-center group-hover:scale-110 transition-transform">
                                <Camera className="w-8 h-8 text-emerald-600" aria-hidden="true" />
                            </div>
                            <div className="space-y-1">
                                <p className="font-bold text-foreground">{t('step1.clickToUpload')}</p>
                                <p className="text-xs text-muted-foreground">{t('step1.formats')}</p>
                            </div>
                        </div>

                        <Button onClick={() => setStep(2)} className="w-full h-12 text-base font-bold bg-emerald-600 hover:bg-emerald-500">
                            {t('step1.button')} <ChevronRight className="ml-2 w-4 h-4" />
                        </Button>
                    </motion.div>
                )}

                {step === 2 && (
                    <motion.div
                        key="step2"
                        initial={{ opacity: 0, x: 20 }}
                        animate={{ opacity: 1, x: 0 }}
                        exit={{ opacity: 0, x: -20 }}
                    >
                         <div className="text-center space-y-2 mb-8">
                            <h2 id="onboarding-title" className="text-2xl font-bold">{t('step2.title')}</h2>
                            <p className="text-muted-foreground">{t('step2.subtitle')}</p>
                        </div>

                        <form onSubmit={handleSubmit((data) => mutation.mutate(data))} className="space-y-5">
                            <div className="grid grid-cols-2 gap-5">
                                <div className="space-y-2 col-span-2">
                                    <Label htmlFor="onboarding-nik" className="font-bold">{t('step2.nik')}</Label>
                                    <Input id="onboarding-nik" {...register('nik')} placeholder={t('step2.nikPlaceholder')} className="h-12" aria-invalid={!!errors.nik} />
                                    {errors.nik && <p className="text-red-500 text-xs font-bold" role="alert">{errors.nik.message}</p>}
                                </div>
                                <div className="space-y-2 col-span-2">
                                    <Label htmlFor="onboarding-fullname" className="font-bold">{t('step2.fullName')}</Label>
                                    <Input id="onboarding-fullname" {...register('fullName')} placeholder={t('step2.fullNamePlaceholder')} className="h-12" aria-invalid={!!errors.fullName} />
                                    {errors.fullName && <p className="text-red-500 text-xs font-bold" role="alert">{errors.fullName.message}</p>}
                                </div>
                                <div className="space-y-2 col-span-2 md:col-span-1">
                                    <Label htmlFor="onboarding-email" className="font-bold">{t('step2.email')}</Label>
                                    <Input id="onboarding-email" {...register('email')} type="email" placeholder={t('step2.emailPlaceholder')} className="h-12" aria-invalid={!!errors.email} />
                                    {errors.email && <p className="text-red-500 text-xs font-bold" role="alert">{errors.email.message}</p>}
                                </div>
                                <div className="space-y-2 col-span-2 md:col-span-1">
                                    <Label htmlFor="onboarding-username" className="font-bold">{t('step2.username')}</Label>
                                    <Input id="onboarding-username" {...register('username')} placeholder={t('step2.usernamePlaceholder')} className="h-12" aria-invalid={!!errors.username} />
                                    {errors.username && <p className="text-red-500 text-xs font-bold" role="alert">{errors.username.message}</p>}
                                </div>
                            </div>
                            
                            <input type="hidden" {...register('externalId')} defaultValue="KTP-PREMIUM-V2" />

                            <div className="pt-4 flex gap-4">
                                <Button type="button" variant="outline" onClick={() => setStep(1)} className="h-12 px-6">
                                    {t('step2.backButton')}
                                </Button>
                                <Button type="submit" className="flex-1 h-12 bg-emerald-600 hover:bg-emerald-500 font-bold" disabled={mutation.isPending}>
                                    {mutation.isPending ? <Loader2 className="animate-spin" /> : t('step2.submitButton')}
                                </Button>
                            </div>
                        </form>
                    </motion.div>
                )}

                {step === 3 && (
                    <motion.div
                        key="step3"
                        initial={{ opacity: 0, scale: 0.95 }}
                        animate={{ opacity: 1, scale: 1 }}
                        className="text-center py-10 space-y-6"
                    >
                        <div className="w-24 h-24 bg-emerald-100 dark:bg-emerald-900/30 rounded-full flex items-center justify-center mx-auto mb-6">
                            <CheckCircle2 className="w-12 h-12 text-emerald-600" />
                        </div>
                        <div className="space-y-2">
                            <h2 id="onboarding-title" className="text-3xl font-bold text-foreground">{t('step3.title')}</h2>
                            <p className="text-muted-foreground max-w-xs mx-auto">
                                {t('step3.subtitle')}
                            </p>
                        </div>
                        <div className="pt-4">
                            <Loader2 className="w-6 h-6 text-emerald-500 animate-spin mx-auto" aria-label="Processing..." />
                        </div>
                    </motion.div>
                )}
            </AnimatePresence>
        </div>
      </main>
    </div>

  );
}
