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
    <div className="min-h-screen bg-white dark:bg-gray-900 flex flex-col p-6">
      <div className="flex-1 flex flex-col justify-center max-w-md mx-auto w-full">
        
        <div className="mb-12 text-center">
           <div className="relative h-16 w-16 mx-auto mb-4 bg-green-500 rounded-2xl flex items-center justify-center text-white text-2xl font-bold shadow-lg shadow-green-200">
               <span>U</span>
               <div className="absolute top-3 right-3 h-2 w-2 bg-white rounded-full opacity-50"></div>
           </div>
           <h1 className="text-3xl font-extrabold text-gray-900 dark:text-white mb-2">Welcome Back</h1>
           <p className="text-gray-500 dark:text-gray-400">Sign in to manage your finances</p>
        </div>
        
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
          <div className="space-y-4">
            <div>
              <input 
                {...register('username')}
                type="text" 
                className="w-full rounded-2xl border-0 bg-gray-50 dark:bg-gray-800 p-4 text-gray-900 dark:text-white placeholder:text-gray-400 focus:ring-2 focus:ring-green-500 transition-all font-medium"
                placeholder="Username"
              />
              {errors.username && <p className="text-red-500 text-xs mt-2 pl-2 font-medium">{errors.username.message}</p>}
            </div>
            
            <div>
              <input 
                {...register('password')}
                type="password" 
                className="w-full rounded-2xl border-0 bg-gray-50 dark:bg-gray-800 p-4 text-gray-900 dark:text-white placeholder:text-gray-400 focus:ring-2 focus:ring-green-500 transition-all font-medium"
                placeholder="Password"
              />
              {errors.password && <p className="text-red-500 text-xs mt-2 pl-2 font-medium">{errors.password.message}</p>}
            </div>

            <div className="flex justify-end">
               <a href="#" className="text-sm font-semibold text-green-600 hover:text-green-700">Forgot Password?</a>
            </div>
          </div>

          <button 
            type="submit" 
            disabled={mutation.isPending}
            className="w-full bg-green-600 text-white py-4 rounded-2xl font-bold text-lg hover:bg-green-700 active:scale-[0.98] transition-all shadow-xl shadow-green-200 disabled:bg-green-400 disabled:shadow-none"
          >
            {mutation.isPending ? 'Signing in...' : 'Sign In'}
          </button>
        </form>

        <div className="mt-12 text-center">
            <p className="text-gray-500 dark:text-gray-400 font-medium">
               New to PayU?{' '}
               <Link href="/onboarding" className="text-green-600 hover:text-green-700 font-bold">
                  Create Account
               </Link>
            </p>
        </div>
      </div>
    </div>
  );
}
