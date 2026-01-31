'use client';

import { useMutation } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { loginSchema, LoginRequest } from '@/types';
import api from '@/lib/api';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { useAuthStore } from '@/stores';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { CheckCircle2, ShieldCheck, ArrowRight, Loader2 } from 'lucide-react';
import { motion } from 'framer-motion';

export default function LoginPage() {
  const router = useRouter();
  const setAuth = useAuthStore((state) => state.setAuth);

  const { register, handleSubmit, formState: { errors } } = useForm<LoginRequest>({
    resolver: zodResolver(loginSchema)
  });

  const mutation = useMutation({
    mutationFn: (data: LoginRequest) => api.post('/auth/login', data),
    onSuccess: (response) => {
      const { user } = response.data;
      if (user) setAuth(user, user.id);
      router.push('/');
    },
    onError: (error) => {
      console.error('Login failed:', error);
      alert('Login gagal. Silakan periksa kembali kredensial Anda.');
    }
  });

  return (
    <div className="min-h-screen w-full flex bg-background font-inter">
      {/* Left Panel - Branding (Hidden on mobile) */}
      <div className="hidden lg:flex flex-col justify-between w-1/2 bg-zinc-900 border-r border-border/10 p-12 relative overflow-hidden text-white">
        {/* Background Effects */}
        <div className="absolute top-0 right-0 w-[800px] h-[800px] bg-emerald-500/10 rounded-full blur-[120px] -translate-y-1/2 translate-x-1/2" />
        <div className="absolute bottom-0 left-0 w-[600px] h-[600px] bg-bank-green/10 rounded-full blur-[100px] translate-y-1/2 -translate-x-1/4" />
        
        {/* Pattern Overlay */}
        <div className="absolute inset-0 opacity-[0.03] bg-[url('https://grainy-gradients.vercel.app/noise.svg')]" />

        <div className="relative z-10">
            <Link href="/" className="flex items-center gap-3 w-fit">
                <div className="w-10 h-10 bg-emerald-500 rounded-xl flex items-center justify-center shadow-lg shadow-emerald-500/20">
                    <span className="text-white font-bold text-xl">U</span>
                </div>
                <span className="text-2xl font-bold tracking-tight text-white">PayU</span>
            </Link>
        </div>

        <div className="relative z-10 max-w-lg space-y-6">
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-emerald-900/30 border border-emerald-500/20 text-emerald-400 text-xs font-bold tracking-widest uppercase mb-4">
                <ShieldCheck className="w-4 h-4" />
                <span>Enterprise Grade Security</span>
            </div>
            <h1 className="text-5xl font-bold leading-tight tracking-tight">
                Platform Perbankan Digital <span className="text-emerald-500">Masa Depan.</span>
            </h1>
            <p className="text-lg text-zinc-400 leading-relaxed">
                Kelola aset, investasi, dan transaksi harian Anda dalam satu ekosistem yang aman, terenkripsi, dan terintegrasi penuh.
            </p>

            <div className="pt-8 space-y-4">
                {[
                    "Enkripsi End-to-End Standar Militer",
                    "Monitoring Transaksi Real-time AI",
                    "Integrasi Pembayaran QRIS & BI-FAST"
                ].map((feature, i) => (
                    <div key={i} className="flex items-center gap-3 text-zinc-300">
                        <CheckCircle2 className="w-5 h-5 text-emerald-500" />
                        <span className="font-medium">{feature}</span>
                    </div>
                ))}
            </div>
        </div>

        <div className="relative z-10 text-zinc-500 text-xs font-mono">
            © 2026 PayU Financial Infrastructure. Secure Environment.
        </div>
      </div>

      {/* Right Panel - Form */}
      <div className="flex-1 flex items-center justify-center p-8 bg-background relative">
        <div className="w-full max-w-[420px] space-y-8">
            <div className="text-center lg:text-left space-y-2">
                <h2 className="text-3xl font-bold tracking-tight">Selamat Datang Kembali</h2>
                <p className="text-muted-foreground">Masuk ke dashboard finansial Anda</p>
            </div>

            <form onSubmit={handleSubmit((data) => mutation.mutate(data))} className="space-y-6">
                <div className="space-y-4">
                    <div className="space-y-2">
                        <Label htmlFor="username">Username</Label>
                        <Input
                            id="username"
                            placeholder="username123"
                            {...register('username')}
                            className="h-12 bg-muted/30"
                            disabled={mutation.isPending}
                        />
                         {errors.username && <p className="text-red-500 text-xs font-medium">{errors.username.message}</p>}
                    </div>
                    <div className="space-y-2">
                        <div className="flex items-center justify-between">
                            <Label htmlFor="password">Password</Label>
                            <Link href="#" className="text-xs font-medium text-emerald-600 hover:text-emerald-500 hover:underline">
                                Lupa password?
                            </Link>
                        </div>
                        <Input
                            id="password"
                            type="password"
                            placeholder="••••••••"
                            {...register('password')}
                            className="h-12 bg-muted/30"
                            disabled={mutation.isPending}
                        />
                        {errors.password && <p className="text-red-500 text-xs font-medium">{errors.password.message}</p>}
                    </div>
                </div>

                <Button 
                    type="submit" 
                    className="w-full h-12 bg-emerald-600 hover:bg-emerald-500 text-white font-bold text-base shadow-lg shadow-emerald-500/20 transition-all active:scale-[0.98]"
                    disabled={mutation.isPending}
                >
                    {mutation.isPending ? (
                        <>
                            <Loader2 className="mr-2 h-4 w-4 animate-spin" /> Masuk...
                        </>
                    ) : (
                        "Masuk ke Akun"
                    )}
                </Button>
            </form>

            <div className="relative">
                <div className="absolute inset-0 flex items-center">
                    <span className="w-full border-t border-border" />
                </div>
                <div className="relative flex justify-center text-xs uppercase">
                    <span className="bg-background px-4 text-muted-foreground font-medium">Atau</span>
                </div>
            </div>

            <div className="text-center text-sm">
                Belum memiliki akun?{" "}
                <Link href="/onboarding" className="font-bold text-emerald-600 hover:text-emerald-500 hover:underline inline-flex items-center">
                    Daftar Sekarang <ArrowRight className="ml-1 w-3 h-3" />
                </Link>
            </div>
        </div>
      </div>
    </div>
  );
}
