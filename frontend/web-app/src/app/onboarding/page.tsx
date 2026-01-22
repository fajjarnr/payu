'use client';

import { useMutation } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { registerUserSchema, RegisterUserRequest } from '@/types';
import api from '@/lib/api';
import { useRouter } from 'next/navigation';
import { Camera, ChevronRight, CheckCircle2, ShieldCheck, ArrowLeft } from 'lucide-react';
import { useState } from 'react';
import Link from 'next/link';

export default function OnboardingPage() {
   const router = useRouter();
   const [step, setStep] = useState(1);

   const { register, handleSubmit, formState: { errors } } = useForm<RegisterUserRequest>({
      resolver: zodResolver(registerUserSchema)
   });

   const mutation = useMutation({
      mutationFn: (data: RegisterUserRequest) => {
         return api.post('/accounts/register', data);
      },
      onSuccess: () => {
         setStep(3); // Success step
         setTimeout(() => router.push('/login'), 2000);
      },
      onError: (error) => {
         console.error('Registration failed:', error);
         alert('Pendaftaran gagal. Silakan coba lagi.');
      }
   });

   const onSubmit = (data: RegisterUserRequest) => {
      mutation.mutate(data);
   };

   return (
      <div className="min-h-screen bg-background flex flex-col items-center justify-center p-6 relative overflow-hidden">
         {/* Background Decor */}
         <div className="absolute top-[-10%] right-[-10%] w-[40%] h-[40%] bg-bank-green/5 rounded-full blur-[120px]" />
         <div className="absolute bottom-[-10%] left-[-10%] w-[40%] h-[40%] bg-bank-emerald/5 rounded-full blur-[120px]" />

         <div className="max-w-xl w-full relative z-10">
            {/* Header */}
            <div className="mb-12 flex justify-between items-center">
               <Link href="/login" className="p-3 bg-card/80 backdrop-blur-md hover:bg-gray-100 dark:hover:bg-gray-800 rounded-2xl border border-border transition-all active:scale-95 shadow-sm">
                  <ArrowLeft className="h-6 w-6" />
               </Link>
               <div className="flex items-center gap-3 bg-bank-green/10 px-4 py-2 rounded-full border border-bank-green/20">
                  <div className="h-2 w-2 bg-bank-green rounded-full animate-pulse" />
                  <span className="text-[10px] font-black uppercase tracking-[0.2em] text-bank-green italic">Protokol Identitas</span>
               </div>
               <div className="w-12" />
            </div>

            {/* Progress Tracker */}
            <div className="flex justify-between items-center mb-16 relative px-4">
               <div className="absolute left-0 right-0 top-1/2 -translate-y-1/2 h-[2px] bg-gray-100 dark:bg-gray-900 -z-0" />
               <div className="absolute left-0 top-1/2 -translate-y-1/2 h-[2px] bg-bank-green transition-all duration-700 -z-0" style={{ width: `${((step - 1) / 2) * 100}%` }} />

               {[1, 2, 3].map((s) => (
                  <div key={s} className="relative z-10 flex flex-col items-center">
                     <div className={`w-14 h-14 rounded-2xl flex items-center justify-center font-black text-xl transition-all duration-700 shadow-2xl ${step >= s ? 'bg-bank-green text-white scale-110 shadow-bank-green/30' : 'bg-card text-gray-300 border border-border'}`}>
                        {s}
                     </div>
                  </div>
               ))}
            </div>

            {/* Form Sections */}
            <div className="bg-card/80 backdrop-blur-xl rounded-[3rem] p-10 border border-border shadow-2xl min-h-[450px] flex flex-col justify-center relative overflow-hidden group">
               <div className="absolute top-0 right-0 w-80 h-80 bg-bank-green/5 rounded-full blur-3xl" />

               {step === 1 && (
                  <div className="space-y-10 animate-in slide-in-from-right duration-700">
                     <div className="text-center">
                        <h2 className="text-4xl font-black text-foreground tracking-tighter mb-4 italic">Verifikasi eKYC.</h2>
                        <p className="text-[10px] font-black text-gray-400 uppercase tracking-[0.2em] leading-relaxed max-w-sm mx-auto">Unggah identitas resmi pemerintah (KTP) Anda untuk memulai pemetaan identitas biologis.</p>
                     </div>

                     <div className="aspect-video bg-gray-50 dark:bg-gray-900/50 rounded-[2.5rem] flex flex-col items-center justify-center border-2 border-dashed border-border cursor-pointer hover:border-bank-green hover:bg-bank-green/5 transition-all duration-500 group/upload">
                        <div className="w-24 h-24 bg-white dark:bg-gray-800 rounded-[2rem] flex items-center justify-center mb-6 shadow-2xl group-hover/upload:scale-110 transition-transform duration-500">
                           <Camera className="h-10 w-10 text-bank-green" />
                        </div>
                        <span className="text-[10px] font-black text-gray-400 group-hover/upload:text-bank-green uppercase tracking-[0.2em] italic">Ambil Gambar Identitas</span>
                     </div>

                     <button
                        onClick={() => setStep(2)}
                        className="w-full bg-bank-green text-white py-6 rounded-3xl font-black text-[10px] uppercase tracking-[0.2em] hover:bg-bank-emerald transition-all active:scale-[0.98] shadow-2xl shadow-bank-green/20 flex items-center justify-center gap-3 italic"
                     >
                        Mulai Proses Verifikasi <ChevronRight className="h-5 w-5" />
                     </button>
                     <div className="flex items-center justify-center gap-3 text-[10px] font-black text-gray-400 uppercase tracking-widest bg-gray-50 dark:bg-gray-900/50 py-3 rounded-xl border border-border">
                        <ShieldCheck className="h-4 w-4 text-bank-green" /> ENKRIPSI AMAN SESUAI STANDAR OJK & BI
                     </div>
                  </div>
               )}

               {step === 2 && (
                  <form onSubmit={handleSubmit(onSubmit)} className="space-y-10 animate-in slide-in-from-right duration-700">
                     <div className="text-center mb-4">
                        <h2 className="text-4xl font-black text-foreground tracking-tighter mb-3 italic">Profil Akun.</h2>
                        <p className="text-[10px] font-black text-gray-400 uppercase tracking-[0.2em]">Petakan identitas unik Anda ke dalam Buku Besar (Ledger) finansial kami.</p>
                     </div>

                     <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
                        <div className="group">
                           <label className="text-[10px] font-black text-gray-400 uppercase tracking-widest ml-1 block mb-3 group-focus-within:text-bank-green transition-colors">Nomor NIK (16 Digit)</label>
                           <input
                              {...register('nik')}
                              type="text"
                              className="w-full rounded-2xl border-border bg-gray-50 dark:bg-gray-900/50 p-5 text-foreground font-black placeholder:text-gray-200 focus:ring-4 focus:ring-bank-green/10 focus:border-bank-green transition-all outline-none uppercase tracking-tight"
                              placeholder="3200..."
                           />
                           {errors.nik && <p className="text-red-500 text-[10px] mt-3 pl-3 font-black uppercase tracking-widest">{errors.nik.message}</p>}
                        </div>

                        <div className="group">
                           <label className="text-[10px] font-black text-gray-400 uppercase tracking-widest ml-1 block mb-3 group-focus-within:text-bank-green transition-colors">Nama Lengkap Sesuai KTP</label>
                           <input
                              {...register('fullName')}
                              type="text"
                              className="w-full rounded-2xl border-border bg-gray-50 dark:bg-gray-900/50 p-5 text-foreground font-black placeholder:text-gray-200 focus:ring-4 focus:ring-bank-green/10 focus:border-bank-green transition-all outline-none uppercase tracking-tight"
                              placeholder="NAMA LENGKAP ANDA"
                           />
                           {errors.fullName && <p className="text-red-500 text-[10px] mt-3 pl-3 font-black uppercase tracking-widest">{errors.fullName.message}</p>}
                        </div>

                        <div className="group">
                           <label className="text-[10px] font-black text-gray-400 uppercase tracking-widest ml-1 block mb-3 group-focus-within:text-bank-green transition-colors">Alamat Email Digital</label>
                           <input
                              {...register('email')}
                              type="email"
                              className="w-full rounded-2xl border-border bg-gray-50 dark:bg-gray-900/50 p-5 text-foreground font-black placeholder:text-gray-200 focus:ring-4 focus:ring-bank-green/10 focus:border-bank-green transition-all outline-none italic"
                              placeholder="nama@domain.com"
                           />
                           {errors.email && <p className="text-red-500 text-[10px] mt-3 pl-3 font-black uppercase tracking-widest">{errors.email.message}</p>}
                        </div>

                        <div className="group">
                           <label className="text-[10px] font-black text-gray-400 uppercase tracking-widest ml-1 block mb-3 group-focus-within:text-bank-green transition-colors">Nama Pengguna (Username)</label>
                           <input
                              {...register('username')}
                              type="text"
                              className="w-full rounded-2xl border-border bg-gray-50 dark:bg-gray-900/50 p-5 text-foreground font-black placeholder:text-gray-200 focus:ring-4 focus:ring-bank-green/10 focus:border-bank-green transition-all outline-none uppercase tracking-tight"
                              placeholder="NAMA_PENGGUNA_UNIK"
                           />
                           {errors.username && <p className="text-red-500 text-[10px] mt-3 pl-3 font-black uppercase tracking-widest">{errors.username.message}</p>}
                        </div>
                     </div>

                     <input type="hidden" {...register('externalId')} defaultValue="KTP-PREMIUM" />

                     <div className="pt-6">
                        <button
                           type="submit"
                           disabled={mutation.isPending}
                           className="w-full bg-bank-green text-white py-6 rounded-[1.5rem] font-black text-[10px] uppercase tracking-[0.2em] hover:bg-bank-emerald transition-all active:scale-[0.98] shadow-2xl shadow-bank-green/20 italic"
                        >
                           {mutation.isPending ? 'Menyebarkan Identitas...' : 'Konfirmasi Pembuatan Akun'}
                        </button>
                     </div>
                  </form>
               )}

               {step === 3 && (
                  <div className="flex flex-col items-center justify-center py-16 animate-in zoom-in duration-700 text-center relative z-10">
                     <div className="w-28 h-28 bg-bank-green/10 rounded-[2.5rem] flex items-center justify-center mb-10 shadow-[0_0_50px_rgba(16,185,129,0.2)] border border-bank-green/20">
                        <CheckCircle2 className="h-14 w-14 text-bank-green" />
                     </div>
                     <h2 className="text-5xl font-black text-foreground mb-4 italic tracking-tighter">Pendaftaran Berhasil.</h2>
                     <p className="text-[10px] font-black text-gray-400 uppercase tracking-[0.2em] max-w-sm leading-relaxed mx-auto">Pemetaan identitas selesai. Menginisialisasi kantong utama dan dompet sekunder. Mengalihkan ke terminal akses...</p>
                  </div>
               )}
            </div>
         </div>
      </div>
   );
}
