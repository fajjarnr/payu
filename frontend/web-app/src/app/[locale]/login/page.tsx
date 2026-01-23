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
   alert('Login gagal. Silakan periksa kembali kredensial Anda.');
  }
 });

 const onSubmit = (data: LoginRequest) => {
  mutation.mutate(data);
 };

 return (
  <div className="min-h-screen bg-background flex flex-col p-6 items-center justify-center relative overflow-hidden">
   {/* Background Decor */}
   <div className="absolute top-[-10%] right-[-10%] w-[40%] h-[40%] bg-bank-green/5 rounded-full blur-[120px]" />
   <div className="absolute bottom-[-10%] left-[-10%] w-[40%] h-[40%] bg-bank-emerald/5 rounded-full blur-[120px]" />

   <div className="max-w-md w-full space-y-12 relative z-10">
    <div className="text-center">
     <div className="relative h-24 w-24 mx-auto mb-10 bg-bank-green rounded-xl flex items-center justify-center text-white text-4xl font-black shadow-2xl shadow-bank-green/30 transition-transform hover:rotate-12 duration-500">
      <span>U</span>
      <div className="absolute top-4 right-4 h-2.5 w-2.5 bg-white rounded-full animate-pulse shadow-[0_0_10px_white]"></div>
     </div>
     <h1 className="text-5xl font-black text-foreground mb-3">Selamat Datang.</h1>
     <p className="text-gray-400 font-bold tracking-[0.2em] text-[10px]">Portal Perbankan Digital Aman</p>
    </div>

    <form onSubmit={handleSubmit(onSubmit)} className="space-y-12">
     <div className="space-y-8">
      <div className="group">
       <label className="text-[10px] font-black text-gray-400 tracking-widest ml-1 block mb-3 group-focus-within:text-bank-green transition-colors">Pengenal Kredensial (Username)</label>
       <input
        {...register('username')}
        type="text"
        className="w-full rounded-xl border-border bg-card/50 backdrop-blur-sm p-6 text-foreground placeholder:text-gray-200 focus:ring-4 focus:ring-bank-green/10 focus:border-bank-green transition-all font-bold text-lg outline-none"
        placeholder="Username atau ID Akun"
       />
       {errors.username && <p className="text-red-500 text-[10px] mt-3 pl-3 font-black tracking-widest">{errors.username.message}</p>}
      </div>

      <div className="group">
       <label className="text-[10px] font-black text-gray-400 tracking-widest ml-1 block mb-3 group-focus-within:text-bank-green transition-colors">Kunci Kata Sandi (Password)</label>
       <input
        {...register('password')}
        type="password"
        className="w-full rounded-xl border-border bg-card/50 backdrop-blur-sm p-6 text-foreground placeholder:text-gray-200 focus:ring-4 focus:ring-bank-green/10 focus:border-bank-green transition-all font-bold text-lg outline-none"
        placeholder="••••••••••••"
       />
       {errors.password && <p className="text-red-500 text-[10px] mt-3 pl-3 font-black tracking-widest">{errors.password.message}</p>}
      </div>

      <div className="flex justify-end pr-2">
       <a href="#" className="text-[10px] font-black text-bank-green tracking-[0.2em] hover:underline">Lupa / Riset Akses ?</a>
      </div>
     </div>

     <div className="space-y-6 pt-4">
      <button
       type="submit"
       disabled={mutation.isPending}
       className="w-full bg-bank-green text-white py-6 rounded-xl font-black text-xs tracking-[0.2em] hover:bg-bank-emerald active:scale-[0.98] transition-all shadow-2xl shadow-bank-green/20 disabled:bg-bank-green/50"
      >
       {mutation.isPending ? 'Memvalidasi Akun...' : 'Inisialisasi Akses'}
      </button>
      <div className="h-0.5 w-full bg-gray-100 dark:bg-gray-900 rounded-full" />
      <p className="text-center text-gray-400 font-bold tracking-[0.2em] text-[8px]">
       Protokol Autentikasi v1.4.2-IND
      </p>
     </div>
    </form>

    <div className="text-center pt-6">
     <p className="text-gray-400 font-black tracking-widest text-[10px]">
      Baru di platform ini?{' '}
      <Link href="/onboarding" className="text-bank-green hover:underline">
       Buat Akun Baru
      </Link>
     </p>
    </div>
   </div>
  </div>
 );
}
