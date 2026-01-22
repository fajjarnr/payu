'use client';

import { useMutation } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { loginSchema, LoginRequest } from '@/types';
import api from '@/lib/api';
import { useRouter } from 'next/navigation';
import Link from 'next/link';

export default function LoginPage() {
  const router = useRouter();

  const { register, handleSubmit, formState: { errors } } = useForm<LoginRequest>({
    resolver: zodResolver(loginSchema)
  });

  const mutation = useMutation({
    mutationFn: (data: LoginRequest) => {
      return api.post('/auth/login', data);
    },
    onSuccess: (response) => {
      const { token, user } = response.data;
      localStorage.setItem('token', token);
      localStorage.setItem('user', JSON.stringify(user));
      localStorage.setItem('accountId', user.id);
      router.push('/');
    },
    onError: (error) => {
      console.error('Login failed:', error);
      alert('Login failed. Please check your credentials.');
    }
  });

  const onSubmit = (data: LoginRequest) => {
    mutation.mutate(data);
  };

  return (
    <div className="min-h-screen bg-background flex flex-col p-6 items-center justify-center">
      <div className="max-w-md w-full space-y-12">

        <div className="text-center">
          <div className="relative h-20 w-20 mx-auto mb-8 bg-bank-green rounded-[1.5rem] flex items-center justify-center text-white text-3xl font-black shadow-2xl shadow-bank-green/30">
            <span>U</span>
            <div className="absolute top-4 right-4 h-2 w-2 bg-white rounded-full animate-pulse"></div>
          </div>
          <h1 className="text-4xl font-black text-foreground tracking-tighter mb-2 italic">Welcome Back.</h1>
          <p className="text-gray-500 font-bold uppercase tracking-widest text-[10px]">Secure Digital Banking Portal</p>
        </div>

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-8">
          <div className="space-y-6">
            <div className="group">
              <label className="text-[10px] font-black text-gray-400 uppercase tracking-widest ml-1 block mb-2 group-focus-within:text-bank-green transition-colors">Credential Identifier</label>
              <input
                {...register('username')}
                type="text"
                className="w-full rounded-2xl border-border bg-card p-5 text-foreground placeholder:text-gray-200 focus:ring-4 focus:ring-bank-green/10 focus:border-bank-green transition-all font-black text-lg outline-none"
                placeholder="Username or Account ID"
              />
              {errors.username && <p className="text-red-500 text-[10px] mt-2 pl-2 font-black uppercase tracking-widest">{errors.username.message}</p>}
            </div>

            <div className="group">
              <label className="text-[10px] font-black text-gray-400 uppercase tracking-widest ml-1 block mb-2 group-focus-within:text-bank-green transition-colors">Access Keyphrase</label>
              <input
                {...register('password')}
                type="password"
                className="w-full rounded-2xl border-border bg-card p-5 text-foreground placeholder:text-gray-200 focus:ring-4 focus:ring-bank-green/10 focus:border-bank-green transition-all font-black text-lg outline-none"
                placeholder="••••••••••••"
              />
              {errors.password && <p className="text-red-500 text-[10px] mt-2 pl-2 font-black uppercase tracking-widest">{errors.password.message}</p>}
            </div>

            <div className="flex justify-end">
              <a href="#" className="text-[10px] font-black text-bank-green uppercase tracking-widest hover:underline">Revoke / Reset Access ?</a>
            </div>
          </div>

          <div className="space-y-4 pt-4">
            <button
              type="submit"
              disabled={mutation.isPending}
              className="w-full bg-bank-green text-white py-6 rounded-3xl font-black text-xl hover:bg-bank-emerald active:scale-[0.98] transition-all shadow-2xl shadow-bank-green/20 disabled:bg-bank-green/50"
            >
              {mutation.isPending ? 'Validating Account...' : 'Initialize Access'}
            </button>
            <div className="h-0.5 w-full bg-gray-50 dark:bg-gray-900 rounded-full" />
            <p className="text-center text-gray-500 font-bold uppercase tracking-widest text-[10px]">
              Authentication Protocol v1.4.2
            </p>
          </div>
        </form>

        <div className="text-center">
          <p className="text-gray-400 font-bold text-xs">
            New to the platform?{' '}
            <Link href="/onboarding" className="text-bank-green hover:underline">
              Create Account
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
}
