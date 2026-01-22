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
         alert('Registration failed. Please try again.');
      }
   });

   const onSubmit = (data: RegisterUserRequest) => {
      mutation.mutate(data);
   };

   return (
      <div className="min-h-screen bg-background flex flex-col items-center justify-center p-6">
         <div className="max-w-xl w-full">
            {/* Header */}
            <div className="mb-12 flex justify-between items-center">
               <Link href="/login" className="p-2 hover:bg-gray-100 dark:hover:bg-gray-800 rounded-xl transition-colors">
                  <ArrowLeft className="h-6 w-6" />
               </Link>
               <div className="flex items-center gap-2">
                  <div className="h-2 w-2 bg-bank-green rounded-full animate-pulse" />
                  <span className="text-[10px] font-black uppercase tracking-[0.2em] text-bank-green">Identity Protocol</span>
               </div>
               <div className="w-10" />
            </div>

            {/* Progress Tracker */}
            <div className="flex justify-between items-center mb-16 relative">
               <div className="absolute left-0 right-0 top-1/2 -translate-y-1/2 h-[2px] bg-gray-100 dark:bg-gray-800 -z-0" />
               <div className="absolute left-0 top-1/2 -translate-y-1/2 h-[2px] bg-bank-green transition-all duration-500 -z-0" style={{ width: `${((step - 1) / 2) * 100}%` }} />

               {[1, 2, 3].map((s) => (
                  <div key={s} className="relative z-10 flex flex-col items-center">
                     <div className={`w-12 h-12 rounded-2xl flex items-center justify-center font-black text-lg transition-all shadow-xl duration-500 ${step >= s ? 'bg-bank-green text-white scale-110 shadow-bank-green/20' : 'bg-card text-gray-300 border border-border'}`}>
                        {s}
                     </div>
                  </div>
               ))}
            </div>

            {/* Form Sections */}
            <div className="bg-card rounded-[2.5rem] p-10 border border-border shadow-sm min-h-[450px] flex flex-col justify-center relative overflow-hidden">
               <div className="absolute top-0 right-0 w-64 h-64 bg-bank-green/5 rounded-full blur-3xl" />

               {step === 1 && (
                  <div className="space-y-10 animate-in slide-in-from-right duration-500">
                     <div className="text-center">
                        <h2 className="text-3xl font-black text-foreground tracking-tight mb-3 italic">eKYC Verification.</h2>
                        <p className="text-[10px] font-black text-gray-400 uppercase tracking-widest leading-relaxed max-w-xs mx-auto">Upload your government ID (KTP) to initialize biological identity mapping.</p>
                     </div>

                     <div className="aspect-video bg-gray-50 dark:bg-gray-900/50 rounded-[2rem] flex flex-col items-center justify-center border-2 border-dashed border-border cursor-pointer hover:border-bank-green hover:bg-bank-green/5 transition-all group">
                        <div className="w-20 h-20 bg-white dark:bg-gray-800 rounded-3xl flex items-center justify-center mb-4 shadow-xl group-hover:scale-110 transition-transform">
                           <Camera className="h-10 w-10 text-bank-green" />
                        </div>
                        <span className="text-[10px] font-black text-gray-400 group-hover:text-bank-green uppercase tracking-widest">Capture ID Graphics</span>
                     </div>

                     <button
                        onClick={() => setStep(2)}
                        className="w-full bg-bank-green text-white py-6 rounded-3xl font-black text-xl hover:bg-bank-emerald transition-all active:scale-[0.98] shadow-2xl shadow-bank-green/20 flex items-center justify-center gap-3"
                     >
                        Initialize Onboarding <ChevronRight className="h-6 w-6" />
                     </button>
                     <div className="flex items-center justify-center gap-2 text-[10px] font-black text-gray-400 uppercase tracking-widest">
                        <ShieldCheck className="h-4 w-4" /> SECURE OJK COMPLIANT ENCRYPTION
                     </div>
                  </div>
               )}

               {step === 2 && (
                  <form onSubmit={handleSubmit(onSubmit)} className="space-y-8 animate-in slide-in-from-right duration-500">
                     <div className="text-center mb-4">
                        <h2 className="text-3xl font-black text-foreground tracking-tight mb-2 italic">Account Profile.</h2>
                        <p className="text-[10px] font-black text-gray-400 uppercase tracking-widest">Map your unique identity to our financial Ledger.</p>
                     </div>

                     <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                        <div className="group">
                           <label className="text-[10px] font-black text-gray-400 uppercase tracking-widest ml-1 block mb-2 group-focus-within:text-bank-green transition-colors">NIK (16 Digits)</label>
                           <input
                              {...register('nik')}
                              type="text"
                              className="w-full rounded-2xl border-border bg-gray-50 dark:bg-gray-900/50 p-4 text-foreground font-black placeholder:text-gray-200 focus:ring-4 focus:ring-bank-green/10 focus:border-bank-green transition-all outline-none"
                              placeholder="3200..."
                           />
                           {errors.nik && <p className="text-red-500 text-[10px] mt-2 pl-2 font-black uppercase tracking-widest">{errors.nik.message}</p>}
                        </div>

                        <div className="group">
                           <label className="text-[10px] font-black text-gray-400 uppercase tracking-widest ml-1 block mb-2 group-focus-within:text-bank-green transition-colors">Full Identity Name</label>
                           <input
                              {...register('fullName')}
                              type="text"
                              className="w-full rounded-2xl border-border bg-gray-50 dark:bg-gray-900/50 p-4 text-foreground font-black placeholder:text-gray-200 focus:ring-4 focus:ring-bank-green/10 focus:border-bank-green transition-all outline-none"
                              placeholder="Jane Doe"
                           />
                           {errors.fullName && <p className="text-red-500 text-[10px] mt-2 pl-2 font-black uppercase tracking-widest">{errors.fullName.message}</p>}
                        </div>

                        <div className="group">
                           <label className="text-[10px] font-black text-gray-400 uppercase tracking-widest ml-1 block mb-2 group-focus-within:text-bank-green transition-colors">Digital Email Address</label>
                           <input
                              {...register('email')}
                              type="email"
                              className="w-full rounded-2xl border-border bg-gray-50 dark:bg-gray-900/50 p-4 text-foreground font-black placeholder:text-gray-200 focus:ring-4 focus:ring-bank-green/10 focus:border-bank-green transition-all outline-none"
                              placeholder="name@domain.com"
                           />
                           {errors.email && <p className="text-red-500 text-[10px] mt-2 pl-2 font-black uppercase tracking-widest">{errors.email.message}</p>}
                        </div>

                        <div className="group">
                           <label className="text-[10px] font-black text-gray-400 uppercase tracking-widest ml-1 block mb-2 group-focus-within:text-bank-green transition-colors">Desired Username</label>
                           <input
                              {...register('username')}
                              type="text"
                              className="w-full rounded-2xl border-border bg-gray-50 dark:bg-gray-900/50 p-4 text-foreground font-black placeholder:text-gray-200 focus:ring-4 focus:ring-bank-green/10 focus:border-bank-green transition-all outline-none"
                              placeholder="unique_id"
                           />
                           {errors.username && <p className="text-red-500 text-[10px] mt-2 pl-2 font-black uppercase tracking-widest">{errors.username.message}</p>}
                        </div>
                     </div>

                     <input type="hidden" {...register('externalId')} defaultValue="KTP-PREMIUM" />

                     <div className="pt-4">
                        <button
                           type="submit"
                           disabled={mutation.isPending}
                           className="w-full bg-bank-green text-white py-6 rounded-3xl font-black text-xl hover:bg-bank-emerald transition-all active:scale-[0.98] shadow-2xl shadow-bank-green/20"
                        >
                           {mutation.isPending ? 'Propagating Identity...' : 'Confirm Account Creation'}
                        </button>
                     </div>
                  </form>
               )}

               {step === 3 && (
                  <div className="flex flex-col items-center justify-center py-10 animate-in zoom-in duration-500 text-center">
                     <div className="w-24 h-24 bg-bank-green/10 rounded-[2rem] flex items-center justify-center mb-8 shadow-2xl shadow-bank-green/5">
                        <CheckCircle2 className="h-12 w-12 text-bank-green" />
                     </div>
                     <h2 className="text-4xl font-black text-foreground mb-3 italic tracking-tighter">Registration Success.</h2>
                     <p className="text-[10px] font-black text-gray-400 uppercase tracking-widest max-w-xs leading-relaxed">Identity mapping complete. Initializing main pocket and secondary wallets. Redirecting to access terminal...</p>
                  </div>
               )}
            </div>
         </div>
      </div>
   );
}
