'use client';

import { useMutation } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { loginSchema, LoginRequest } from '@/types';
import api from '@/lib/api';
import { useRouter } from 'next/navigation';

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
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900 flex items-center justify-center p-8">
      <div className="w-full max-w-md bg-white dark:bg-gray-800 rounded-xl shadow p-8">
        <h1 className="text-3xl font-bold mb-6 text-center text-gray-900 dark:text-white">Welcome Back</h1>
        
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Username</label>
            <input 
              {...register('username')}
              type="text" 
              className="w-full rounded-md border border-gray-300 dark:border-gray-600 p-2 bg-transparent"
              placeholder="Enter your username"
            />
            {errors.username && <p className="text-red-500 text-sm mt-1">{errors.username.message}</p>}
          </div>
          
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Password</label>
            <input 
              {...register('password')}
              type="password" 
              className="w-full rounded-md border border-gray-300 dark:border-gray-600 p-2 bg-transparent"
              placeholder="Enter your password"
            />
            {errors.password && <p className="text-red-500 text-sm mt-1">{errors.password.message}</p>}
          </div>

          <button 
            type="submit" 
            disabled={mutation.isPending}
            className="w-full bg-blue-600 text-white py-2 px-4 rounded-md hover:bg-blue-700 transition-colors disabled:bg-blue-400"
          >
            {mutation.isPending ? 'Logging in...' : 'Login'}
          </button>
        </form>

        <div className="mt-4 text-center">
            <a href="/onboarding" className="text-sm text-blue-600 hover:underline">Don't have an account? Sign up</a>
        </div>
      </div>
    </div>
  );
}
