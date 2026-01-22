'use client';

import { useMutation } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { registerUserSchema, RegisterUserRequest } from '@/types';
import api from '@/lib/api';
import { useRouter } from 'next/navigation';

export default function OnboardingPage() {
  const router = useRouter();
  
  const { register, handleSubmit, formState: { errors } } = useForm<RegisterUserRequest>({
    resolver: zodResolver(registerUserSchema)
  });

  const mutation = useMutation({
    mutationFn: (data: RegisterUserRequest) => {
      // In a real app, this would point to the gateway-service
      // which routes to account-service
      return api.post('/accounts', data);
    },
    onSuccess: () => {
      alert('Account created successfully!');
      router.push('/login');
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
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900 p-8">
      <div className="max-w-2xl mx-auto bg-white dark:bg-gray-800 rounded-xl shadow p-8">
        <h1 className="text-3xl font-bold mb-6 text-gray-900 dark:text-white">Account Opening</h1>
        
        <div className="space-y-6">
          <div className="bg-blue-50 dark:bg-blue-900/20 p-4 rounded-lg">
            <h3 className="font-semibold text-blue-700 dark:text-blue-300 mb-2">Step 1: eKYC Verification</h3>
            <p className="text-sm text-gray-600 dark:text-gray-400">Please prepare your KTP and ensure good lighting for selfie.</p>
          </div>

          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">NIK</label>
              <input 
                {...register('nik')}
                type="text" 
                className="w-full rounded-md border border-gray-300 dark:border-gray-600 p-2 bg-transparent"
                placeholder="Enter 16 digit NIK"
              />
              {errors.nik && <p className="text-red-500 text-sm mt-1">{errors.nik.message}</p>}
            </div>
            
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Full Name</label>
              <input 
                {...register('fullName')}
                type="text" 
                className="w-full rounded-md border border-gray-300 dark:border-gray-600 p-2 bg-transparent"
                placeholder="As shown on KTP"
              />
              {errors.fullName && <p className="text-red-500 text-sm mt-1">{errors.fullName.message}</p>}
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Email</label>
              <input 
                {...register('email')}
                type="email" 
                className="w-full rounded-md border border-gray-300 dark:border-gray-600 p-2 bg-transparent"
                placeholder="email@example.com"
              />
              {errors.email && <p className="text-red-500 text-sm mt-1">{errors.email.message}</p>}
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Username</label>
              <input 
                {...register('username')}
                type="text" 
                className="w-full rounded-md border border-gray-300 dark:border-gray-600 p-2 bg-transparent"
                placeholder="Choose a username"
              />
               {errors.username && <p className="text-red-500 text-sm mt-1">{errors.username.message}</p>}
            </div>
            
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">External ID (e.g. KTP Photo ID for now)</label>
              <input 
                {...register('externalId')}
                type="text" 
                className="w-full rounded-md border border-gray-300 dark:border-gray-600 p-2 bg-transparent"
                placeholder="Unique ID"
              />
               {errors.externalId && <p className="text-red-500 text-sm mt-1">{errors.externalId.message}</p>}
            </div>

            <button 
              type="submit" 
              disabled={mutation.isPending}
              className="w-full bg-blue-600 text-white py-2 px-4 rounded-md hover:bg-blue-700 transition-colors disabled:bg-blue-400"
            >
              {mutation.isPending ? 'Processing...' : 'Continue'}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}
