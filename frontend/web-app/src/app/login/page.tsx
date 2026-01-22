'use client';

import { useMutation } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { loginSchema, LoginRequest } from '@/types';
import api from '@/lib/api';
import { useRouter } from 'next/navigation';
import Image from 'next/image';
import Link from 'next/link';

export default function LoginPage() {
  const router = useRouter();
  
  const { register, handleSubmit, formState: { errors } } = useForm<LoginRequest>({
    resolver: zodResolver(loginSchema)
  });

  const mutation = useMutation({
    mutationFn: (data: LoginRequest) => {
      // Pointing to auth-service via gateway
      return api.post('/auth/login', data);
    },
    onSuccess: (response) => {
      const { token } = response.data;
      localStorage.setItem('token', token);
      alert('Login successful!');
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
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900 flex flex-col items-center justify-center p-8">
      
      <div className="mb-8 flex items-center gap-3">
         <div className="relative h-12 w-12 rounded-xl overflow-hidden shadow-lg">
             <Image 
               src="/logo.png" 
               alt="PayU Logo" 
               fill 
               className="object-cover" 
             />
         </div>
         <span className="text-3xl font-bold text-green-600 dark:text-green-400 tracking-tight">PayU</span>
      </div>

      <div className="w-full max-w-md bg-white dark:bg-gray-800 rounded-2xl shadow-xl border border-gray-100 dark:border-gray-700 p-8">
        <div className="text-center mb-8">
           <h1 className="text-2xl font-bold text-gray-900 dark:text-white">Welcome Back</h1>
           <p className="text-sm text-gray-500 dark:text-gray-400 mt-2">Please sign in to your account</p>
        </div>
        
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Username</label>
            <input 
              {...register('username')}
              type="text" 
              className="w-full rounded-lg border border-gray-300 dark:border-gray-600 p-3 bg-gray-50 dark:bg-gray-900 focus:ring-2 focus:ring-green-500 focus:border-transparent outline-none transition-all"
              placeholder="Enter your username"
            />
            {errors.username && <p className="text-red-500 text-sm mt-1">{errors.username.message}</p>}
          </div>
          
          <div>
            <div className="flex justify-between items-center mb-1">
               <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">Password</label>
               <a href="#" className="text-xs text-green-600 hover:text-green-700 font-medium">Forgot?</a>
            </div>
            <input 
              {...register('password')}
              type="password" 
              className="w-full rounded-lg border border-gray-300 dark:border-gray-600 p-3 bg-gray-50 dark:bg-gray-900 focus:ring-2 focus:ring-green-500 focus:border-transparent outline-none transition-all"
              placeholder="Enter your password"
            />
            {errors.password && <p className="text-red-500 text-sm mt-1">{errors.password.message}</p>}
          </div>

          <button 
            type="submit" 
            disabled={mutation.isPending}
            className="w-full bg-green-600 text-white py-3 px-4 rounded-lg font-semibold hover:bg-green-700 transition-colors disabled:bg-green-400 shadow-lg hover:shadow-green-500/30"
          >
            {mutation.isPending ? 'Logging in...' : 'Sign In'}
          </button>
        </form>

        <div className="mt-8 text-center border-t border-gray-100 dark:border-gray-700 pt-6">
            <p className="text-sm text-gray-600 dark:text-gray-400">
               Don't have an account?{' '}
               <Link href="/onboarding" className="text-green-600 hover:text-green-700 font-semibold hover:underline">
                  Sign up now
               </Link>
            </p>
        </div>
      </div>
    </div>
  );
}
