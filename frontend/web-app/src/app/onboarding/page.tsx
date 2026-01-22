'use client';

import { useMutation } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { registerUserSchema, RegisterUserRequest } from '@/types';
import api from '@/lib/api';
import { useRouter } from 'next/navigation';
import MobileHeader from '@/components/MobileHeader';
import { Camera, ChevronRight, CheckCircle2 } from 'lucide-react';
import { useState } from 'react';

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
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900 pb-10">
      <MobileHeader title="Create Account" />

      <div className="max-w-md mx-auto px-6 py-6">
        {/* Progress Steps */}
        <div className="flex justify-between items-center mb-8 px-2">
            {[1, 2, 3].map((s) => (
               <div key={s} className="flex flex-col items-center gap-2">
                  <div className={`w-8 h-8 rounded-full flex items-center justify-center font-bold text-sm transition-colors ${step >= s ? 'bg-green-600 text-white' : 'bg-gray-200 text-gray-500'}`}>
                     {s}
                  </div>
               </div>
            ))}
            <div className="absolute left-6 right-6 top-[88px] h-[2px] bg-gray-200 -z-10 mx-6">
               <div className="h-full bg-green-500 transition-all duration-300" style={{ width: `${((step-1)/2)*100}%` }}></div>
            </div>
        </div>

        {step === 1 && (
            <div className="space-y-6 animate-in slide-in-from-right duration-300">
               <div className="text-center mb-6">
                  <h2 className="text-xl font-bold text-gray-900 dark:text-white">eKYC Verification</h2>
                  <p className="text-gray-500 text-sm mt-2">Scan your ID card to continue</p>
               </div>
               
               <div className="aspect-video bg-gray-200 dark:bg-gray-800 rounded-2xl flex flex-col items-center justify-center border-2 border-dashed border-gray-300 dark:border-gray-700 cursor-pointer hover:bg-gray-100 transition-colors">
                  <div className="w-16 h-16 bg-white dark:bg-gray-700 rounded-full flex items-center justify-center mb-2 shadow-sm">
                     <Camera className="h-8 w-8 text-green-600" />
                  </div>
                  <span className="text-sm font-medium text-gray-600 dark:text-gray-300">Tap to scan KTP</span>
               </div>

               <button 
                  onClick={() => setStep(2)}
                  className="w-full bg-green-600 text-white py-4 rounded-2xl font-bold hover:bg-green-700 transition-colors shadow-lg shadow-green-200 mt-4 flex items-center justify-center gap-2"
               >
                  Continue <ChevronRight className="h-5 w-5" />
               </button>
            </div>
        )}

        {step === 2 && (
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-5 animate-in slide-in-from-right duration-300">
            <div className="text-center mb-6">
               <h2 className="text-xl font-bold text-gray-900 dark:text-white">Personal Details</h2>
               <p className="text-gray-500 text-sm mt-2">Fill in your information accurately</p>
            </div>

            <div className="space-y-4">
               <div>
                  <label className="text-xs font-semibold text-gray-500 ml-1">NIK</label>
                  <input 
                     {...register('nik')}
                     type="text" 
                     className="w-full rounded-2xl border-0 bg-white dark:bg-gray-800 p-4 text-gray-900 dark:text-white placeholder:text-gray-400 focus:ring-2 focus:ring-green-500 shadow-sm"
                     placeholder="16-digit NIK"
                  />
                  {errors.nik && <p className="text-red-500 text-xs mt-1 ml-1">{errors.nik.message}</p>}
               </div>
               
               <div>
                  <label className="text-xs font-semibold text-gray-500 ml-1">Full Name</label>
                  <input 
                     {...register('fullName')}
                     type="text" 
                     className="w-full rounded-2xl border-0 bg-white dark:bg-gray-800 p-4 text-gray-900 dark:text-white placeholder:text-gray-400 focus:ring-2 focus:ring-green-500 shadow-sm"
                     placeholder="Your name"
                  />
                  {errors.fullName && <p className="text-red-500 text-xs mt-1 ml-1">{errors.fullName.message}</p>}
               </div>

               <div>
                  <label className="text-xs font-semibold text-gray-500 ml-1">Email</label>
                  <input 
                     {...register('email')}
                     type="email" 
                     className="w-full rounded-2xl border-0 bg-white dark:bg-gray-800 p-4 text-gray-900 dark:text-white placeholder:text-gray-400 focus:ring-2 focus:ring-green-500 shadow-sm"
                     placeholder="email@example.com"
                  />
                  {errors.email && <p className="text-red-500 text-xs mt-1 ml-1">{errors.email.message}</p>}
               </div>

               <div>
                  <label className="text-xs font-semibold text-gray-500 ml-1">Username</label>
                  <input 
                     {...register('username')}
                     type="text" 
                     className="w-full rounded-2xl border-0 bg-white dark:bg-gray-800 p-4 text-gray-900 dark:text-white placeholder:text-gray-400 focus:ring-2 focus:ring-green-500 shadow-sm"
                     placeholder="Create username"
                  />
                  {errors.username && <p className="text-red-500 text-xs mt-1 ml-1">{errors.username.message}</p>}
               </div>
               
               <input 
                  {...register('externalId')}
                  type="hidden" 
                  defaultValue="KTP-123456" 
               />
            </div>

            <button 
              type="submit" 
              disabled={mutation.isPending}
              className="w-full bg-green-600 text-white py-4 rounded-2xl font-bold hover:bg-green-700 transition-colors shadow-lg shadow-green-200 mt-6"
            >
              {mutation.isPending ? 'Processing...' : 'Complete Registration'}
            </button>
          </form>
        )}

        {step === 3 && (
            <div className="flex flex-col items-center justify-center py-10 animate-in zoom-in duration-300">
               <div className="w-20 h-20 bg-green-100 rounded-full flex items-center justify-center mb-6">
                  <CheckCircle2 className="h-10 w-10 text-green-600" />
               </div>
               <h2 className="text-2xl font-bold text-gray-900 dark:text-white mb-2">Success!</h2>
               <p className="text-gray-500 text-center">Your account has been created successfully. Redirecting...</p>
            </div>
        )}
      </div>
    </div>
  );
}
