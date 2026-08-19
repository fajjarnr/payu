'use client';

import { useEffect } from 'react';

import { useMutation } from '@tanstack/react-query';
import KYCService from '@/services/KYCService';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { registerUserSchema, RegisterUserRequest } from '@/types';
import api from '@/lib/api';
import { useRouter } from '@/lib/navigation';
import { 
  Camera, 
  ChevronRight, 
  CheckCircle2, 
  ShieldCheck, 
  ArrowLeft,
  Loader2,
  ScanFace,
  Fingerprint,
  Eye,
  EyeOff,
  AlertCircle
} from 'lucide-react';
import { useState, useRef } from 'react';
import { Link } from '@/lib/navigation';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Stepper } from '@/components/ui/stepper';
import { motion, AnimatePresence } from 'framer-motion';
import { useTranslations } from 'next-intl';
import { toast } from 'sonner';

export default function OnboardingPage() {
  const t = useTranslations('auth.onboarding');
  const router = useRouter();
  const [step, setStep] = useState(1);
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [ktpFile, setKtpFile] = useState<File | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  // Stable per-mount external ID — computed once via useState lazy initializer
  // (acceptable to the React 19 linter, unlike Date.now() in useRef or useMemo).
  const [stableExternalId] = useState(() =>
    `KTP-${Date.now()}-${typeof crypto !== 'undefined' ? crypto.randomUUID().substring(0, 8) : 'rnd'}`
  );

  const fileToBase64 = (file: File) =>
    new Promise<string>((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => {
        const result = reader.result as string;
        resolve(result.includes(',') ? result.split(',')[1] : result);
      };
      reader.onerror = () => reject(new Error('Failed to read file'));
      reader.readAsDataURL(file);
    });

  const { register, handleSubmit, formState: { errors } } = useForm<RegisterUserRequest>({
    resolver: zodResolver(registerUserSchema)
  });

  const mutation = useMutation({
    mutationFn: async (data: RegisterUserRequest) => {
      const { confirmPassword: _confirmPassword, ...payload } = data as RegisterUserRequest & { confirmPassword?: string };
      const res = await api.post('/accounts/register', payload);
      // ponytail: upload KTP to kyc-service if present — Flow #28 minimal, non-blocking KYC
      if (ktpFile) {
        try {
          const base64 = await fileToBase64(ktpFile);
          const userId = (res.data as any)?.data?.id || (res.data as any)?.id || payload.username;
          const start = await KYCService.startVerification({
            userId: String(userId),
            fullName: payload.fullName,
            nik: payload.nik,
            dateOfBirth: '1990-01-01',
            address: 'Indonesia',
            phone: (payload as any).phoneNumber,
          });
          await KYCService.uploadKtp({ verificationId: start.verificationId, ktpImage: base64, nik: payload.nik });
        } catch (kycErr) {
          console.warn('KYC upload failed (non-blocking):', kycErr);
        }
      }
      return res;
    },
    onSuccess: () => {
      setStep(3);
      setTimeout(() => router.push('/login'), 2500);
    },
    onError: (error) => {
      console.error('Registration failed:', error instanceof Error ? error.message : 'Unknown error');
      toast.error(t('registrationFailed'));
    }
  });

  useEffect(() => {
    const stepTitles = [t('steps.identity'), t('steps.profile'), t('steps.complete')];
    document.title = `${stepTitles[step - 1]} | PayU Digital Banking`;
  }, [step, t]);

  return (
    <div className="min-h-screen w-full flex bg-background font-inter">
      {/* Left Panel - Branding */}
      <aside className="hidden lg:flex flex-col justify-between w-[45%] bg-zinc-900 border-r border-border/10 p-8 relative overflow-hidden text-white" aria-label="Branding">
        {/* Background Effects */}
        <div className="absolute top-0 right-0 w-[800px] h-[800px] bg-primary/10 rounded-full blur-[120px] -translate-y-1/2 translate-x-1/2" aria-hidden="true" />
        <div className="absolute bottom-0 left-0 w-[600px] h-[600px] bg-primary/10 rounded-full blur-[100px] translate-y-1/2 -translate-x-1/4" aria-hidden="true" />
        <div className="absolute inset-0 opacity-[0.03] bg-[url('https://grainy-gradients.vercel.app/noise.svg')]" aria-hidden="true" />

        <div className="relative z-10">
          <Link href="/" className="flex items-center gap-3 w-fit hover:opacity-80 transition-opacity" aria-label={t('back')}>
            <ArrowLeft className="w-5 h-5 text-white/80" />
            <span className="font-medium text-white">{t('back')}</span>
          </Link>
        </div>

        <div className="relative z-10 max-w-lg space-y-8">
            <div className="space-y-4">
                <div className="w-16 h-16 bg-primary/10 rounded-2xl flex items-center justify-center border border-primary/30 shadow-lg shadow-primary/10" aria-hidden="true">
                    <ScanFace className="w-8 h-8 text-primary/80" />
                </div>
                <h1 className="text-4xl font-bold leading-tight tracking-tight">
                    {t('branding.title')}
                </h1>
                <p className="text-zinc-200 leading-relaxed text-lg">
                    {t('branding.desc')}
                </p>
            </div>

            <div className="space-y-6 pt-4">
                <div className="flex items-start gap-4 p-4 rounded-xl bg-white/5 border border-white/10 backdrop-blur-sm">
                    <Fingerprint className="w-6 h-6 text-primary/80 shrink-0 mt-1" aria-hidden="true" />
                    <div>
                        <h3 className="font-bold text-white mb-1">{t('branding.features.ekyc.title')}</h3>
                        <p className="text-sm text-zinc-200">{t('branding.features.ekyc.desc')}</p>
                    </div>
                </div>
                <div className="flex items-start gap-4 p-4 rounded-xl bg-white/5 border border-white/10 backdrop-blur-sm">
                    <ShieldCheck className="w-6 h-6 text-primary/80 shrink-0 mt-1" aria-hidden="true" />
                    <div>
                        <h3 className="font-bold text-white mb-1">{t('branding.features.data.title')}</h3>
                        <p className="text-sm text-zinc-300">{t('branding.features.data.desc')}</p>
                    </div>
                </div>
            </div>
        </div>

        <div className="relative z-10 flex items-center gap-2 text-zinc-300 text-xs font-mono">
           <div className="w-2 h-2 rounded-full bg-primary animate-pulse" aria-hidden="true" />
           {t('branding.system')} • v2.4.0
        </div>
      </aside>

      {/* Right Panel - Form Flow */}
      <main className="flex-1 flex flex-col items-center justify-center p-8 bg-background relative" aria-labelledby="onboarding-title">
        <div className="w-full max-w-[520px]">
            {/* Mobile back link */}
            <Link href="/login" className="lg:hidden flex items-center gap-2 text-sm text-muted-foreground hover:text-foreground mb-4 w-fit">
                <ArrowLeft className="w-4 h-4" />
                <span>{t('back')}</span>
            </Link>

            {/* Progress Steps */}
            <nav className="mb-12" aria-label="Registration Progress">
              <Stepper 
                steps={[t('steps.identity'), t('steps.profile'), t('steps.complete')]} 
                currentStep={step - 1} 
              />
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

                        <input
                            type="file"
                            accept="image/*"
                            ref={fileInputRef}
                            className="hidden"
                            onChange={(e) => {
                                if (e.target.files?.[0]) {
                                    setKtpFile(e.target.files[0]);
                                }
                            }}
                        />

                        <div 
                            className={`border-2 border-dashed rounded-2xl p-8 transition-all cursor-pointer group flex flex-col items-center justify-center text-center gap-4 ${
                                !ktpFile 
                                    ? 'border-red-400/40 hover:border-primary/50 bg-red-50/30 dark:border-red-400/30 dark:bg-red-950/20 dark:hover:bg-primary/10' 
                                    : 'border-muted-foreground/25 hover:border-primary/50 hover:bg-primary/5 dark:hover:bg-primary/10'
                            }`}
                            tabIndex={0} 
                            role="button" 
                            aria-label={t('step1.clickToUpload')}
                            onClick={() => fileInputRef.current?.click()}
                            onKeyDown={(e) => {
                                if (e.key === 'Enter' || e.key === ' ') {
                                    e.preventDefault();
                                    fileInputRef.current?.click();
                                }
                            }}
                        >
                            <div className="w-16 h-16 rounded-full bg-primary/10 flex items-center justify-center group-hover:scale-110 transition-transform">
                                <Camera className="w-8 h-8 text-primary" aria-hidden="true" />
                            </div>
                            <div className="space-y-1">
                                <p className="font-bold text-foreground">{t('step1.clickToUpload')}</p>
                                <p className="text-xs text-muted-foreground">{t('step1.formats')}</p>
                                {ktpFile && (
                                    <p className="text-xs text-emerald-600 font-medium">{ktpFile.name}</p>
                                )}
                            </div>
                        </div>

                        <Button 
                            onClick={() => setStep(2)} 
                            className="w-full h-14 text-base font-bold shadow-xl shadow-primary/20"
                            disabled={!ktpFile}
                        >
                            {t('step1.button')} <ChevronRight className="ml-2 w-4 h-4" />
                        </Button>
                        {!ktpFile && (
                            <motion.p
                                initial={{ opacity: 0, y: -4 }}
                                animate={{ opacity: 1, y: 0 }}
                                className="flex items-center gap-2 text-sm text-red-500 dark:text-red-400 font-medium"
                                role="alert"
                            >
                                <AlertCircle className="w-4 h-4 shrink-0" />
                                <span>{t('step1.uploadRequiredHint')}</span>
                            </motion.p>
                        )}
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
                                    <Input 
                                        id="onboarding-nik" 
                                        {...register('nik')} 
                                        placeholder={t('step2.nikPlaceholder')} 
                                        className="h-12" 
                                        aria-invalid={!!errors.nik} 
                                        maxLength={16}
                                        inputMode="numeric"
                                        onKeyPress={(e) => {
                                            if (!/[0-9]/.test(e.key)) {
                                                e.preventDefault();
                                            }
                                        }}
                                    />
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

                            <div className="grid grid-cols-2 gap-5">
                                <div className="space-y-2 col-span-2 md:col-span-1">
                                    <Label htmlFor="onboarding-password" className="font-bold">{t('step2.password')}</Label>
                                    <div className="relative">
                                        <Input 
                                            id="onboarding-password" 
                                            {...register('password')} 
                                            type={showPassword ? 'text' : 'password'} 
                                            placeholder={t('step2.passwordPlaceholder')} 
                                            className="h-12 pr-12" 
                                            aria-invalid={!!errors.password} 
                                            autoComplete="new-password"
                                        />
                                        <button 
                                            type="button" 
                                            className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2 focus-visible:ring-offset-background rounded-md" 
                                            onClick={() => setShowPassword(!showPassword)}
                                            aria-label={showPassword ? 'Hide password' : 'Show password'}
                                        >
                                            {showPassword ? <EyeOff className="w-5 h-5" /> : <Eye className="w-5 h-5" />}
                                        </button>
                                    </div>
                                    {errors.password && <p className="text-red-500 text-xs font-bold" role="alert">{errors.password.message}</p>}
                                </div>
                                <div className="space-y-2 col-span-2 md:col-span-1">
                                    <Label htmlFor="onboarding-confirm-password" className="font-bold">{t('step2.confirmPassword')}</Label>
                                    <div className="relative">
                                        <Input 
                                            id="onboarding-confirm-password" 
                                            {...register('confirmPassword')} 
                                            type={showConfirmPassword ? 'text' : 'password'} 
                                            placeholder={t('step2.confirmPasswordPlaceholder')} 
                                            className="h-12 pr-12" 
                                            aria-invalid={!!errors.confirmPassword} 
                                            autoComplete="new-password"
                                        />
                                        <button 
                                            type="button" 
                                            className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2 focus-visible:ring-offset-background rounded-md" 
                                            onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                                            aria-label={showConfirmPassword ? 'Hide password' : 'Show password'}
                                        >
                                            {showConfirmPassword ? <EyeOff className="w-5 h-5" /> : <Eye className="w-5 h-5" />}
                                        </button>
                                    </div>
                                    {errors.confirmPassword && <p className="text-red-500 text-xs font-bold" role="alert">{errors.confirmPassword.message}</p>}
                                </div>
                            </div>
                            
                            <input type="hidden" value={`KTP-${stableExternalId}-${typeof crypto !== 'undefined' ? crypto.randomUUID().substring(0, 8) : 'rnd'}`} {...register('externalId')} />

                             <div className="pt-6 flex gap-4">
                                <Button type="button" variant="outline" onClick={() => setStep(1)} className="h-14 px-8">
                                    {t('step2.backButton')}
                                </Button>
                                <Button type="submit" className="flex-1 h-14 font-bold shadow-xl shadow-primary/20" disabled={mutation.isPending}>
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
                        <div className="w-24 h-24 bg-primary/10 rounded-full flex items-center justify-center mx-auto mb-6">
                            <CheckCircle2 className="w-12 h-12 text-primary" />
                        </div>
                        <div className="space-y-2">
                            <h2 id="onboarding-title" className="text-3xl font-bold text-foreground">{t('step3.title')}</h2>
                            <p className="text-muted-foreground max-w-xs mx-auto">
                                {t('step3.subtitle')}
                            </p>
                        </div>
                        <div className="pt-4">
                            <Loader2 className="w-6 h-6 text-primary animate-spin mx-auto" aria-label="Processing..." />
                        </div>
                    </motion.div>
                )}
            </AnimatePresence>
        </div>
      </main>
    </div>

  );
}
