'use client';

import MobileHeader from "@/components/MobileHeader";
import { Search, ChevronRight } from "lucide-react";
import { useMutation } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { transferSchema, TransferRequest } from '@/types';
import api from '@/lib/api';
import { useState } from 'react';

export default function TransferPage() {
  const [selectedContact, setSelectedContact] = useState<string | null>(null);
  const [showReview, setShowReview] = useState(false);

  const recentContacts = [
    { name: 'Anya', initial: 'A', color: 'bg-purple-100 text-purple-600', accountId: 'acc-any123' },
    { name: 'Budi', initial: 'B', color: 'bg-blue-100 text-blue-600', accountId: 'acc-bud456' },
    { name: 'Citra', initial: 'C', color: 'bg-pink-100 text-pink-600', accountId: 'acc-cit789' },
    { name: 'Dodi', initial: 'D', color: 'bg-yellow-100 text-yellow-600', accountId: 'acc-dod012' },
  ];

  const { register, handleSubmit, formState: { errors }, watch, setValue } = useForm<TransferRequest>({
    resolver: zodResolver(transferSchema),
    defaultValues: {
      amount: 0,
    }
  });

  const amount = watch('amount');

  const transferMutation = useMutation({
    mutationFn: (data: TransferRequest) => {
      return api.post('/transactions/transfer', data);
    },
    onSuccess: () => {
      alert('Transfer successful!');
      setShowReview(false);
      setSelectedContact(null);
      setValue('amount', 0);
      setValue('description', '');
    },
    onError: (error) => {
      console.error('Transfer failed:', error);
      alert('Transfer failed. Please try again.');
    }
  });

  const handleContactSelect = (contact: { name: string; accountId: string }) => {
    setSelectedContact(contact.accountId);
    setValue('toAccountId', contact.accountId);
    setValue('fromAccountId', localStorage.getItem('accountId') || '');
  };

  const onSubmit = (data: TransferRequest) => {
    transferMutation.mutate(data);
  };

  const handleReview = () => {
    const data = { ...watch(), fromAccountId: localStorage.getItem('accountId') || '' };
    if (!data.toAccountId || data.amount <= 0) {
      alert('Please select a recipient and enter an amount');
      return;
    }
    setShowReview(true);
  };

  if (showReview) {
    const selectedContactData = recentContacts.find(c => c.accountId === selectedContact);
    return (
      <div className="min-h-screen bg-gray-50 dark:bg-gray-900">
        <MobileHeader title="Review Transfer" />

        <div className="max-w-md mx-auto p-6 space-y-6">
          <div className="bg-white dark:bg-gray-800 rounded-3xl p-6 shadow-sm">
            <div className="flex items-center gap-4 mb-6">
              <div className={`w-14 h-14 rounded-full ${selectedContactData?.color} flex items-center justify-center font-bold text-xl`}>
                {selectedContactData?.initial}
              </div>
              <div>
                <h3 className="font-bold text-gray-900 dark:text-white text-lg">{selectedContactData?.name}</h3>
                <p className="text-gray-500 text-sm">Account ID: {selectedContact}</p>
              </div>
            </div>

            <div className="border-t border-gray-100 dark:border-gray-700 pt-6 space-y-4">
              <div className="flex justify-between">
                <span className="text-gray-500">Amount</span>
                <span className="font-bold text-gray-900 dark:text-white text-xl">Rp {amount.toLocaleString()}</span>
              </div>
              {watch('description') && (
                <div className="flex justify-between">
                  <span className="text-gray-500">Note</span>
                  <span className="text-gray-900 dark:text-white">{watch('description')}</span>
                </div>
              )}
            </div>
          </div>

          <div className="flex gap-4">
            <button
              onClick={() => setShowReview(false)}
              className="flex-1 bg-gray-100 dark:bg-gray-800 text-gray-700 dark:text-gray-300 py-4 rounded-2xl font-bold hover:bg-gray-200 dark:hover:bg-gray-700 transition-colors"
            >
              Back
            </button>
            <button
              onClick={handleSubmit(onSubmit)}
              disabled={transferMutation.isPending}
              className="flex-1 bg-green-600 text-white py-4 rounded-2xl font-bold hover:bg-green-700 transition-colors shadow-lg shadow-green-200 disabled:bg-green-400"
            >
              {transferMutation.isPending ? 'Processing...' : 'Confirm'}
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900">
      <MobileHeader title="Transfer" />

      <div className="max-w-md mx-auto p-6 space-y-8">

        {/* Search */}
        <div className="relative">
          <Search className="absolute left-4 top-1/2 -translate-y-1/2 h-5 w-5 text-gray-400" />
          <input
            {...register('toAccountId')}
            type="text"
            placeholder="Search name or ID"
            className="w-full pl-12 pr-4 py-4 rounded-2xl border-0 bg-white dark:bg-gray-800 shadow-sm focus:ring-2 focus:ring-green-500 transition-all text-sm font-medium"
          />
          {errors.toAccountId && <p className="text-red-500 text-xs mt-2 pl-2">{errors.toAccountId.message}</p>}
        </div>

        {/* Recent */}
        <div>
          <h3 className="text-sm font-bold text-gray-900 dark:text-white mb-4">Recent</h3>
          <div className="flex gap-4 overflow-x-auto pb-4 no-scrollbar">
            <div className="flex flex-col items-center gap-2 min-w-[64px]">
              <div className="w-14 h-14 rounded-full border-2 border-dashed border-gray-300 flex items-center justify-center text-gray-400 hover:border-green-500 hover:text-green-500 transition-colors cursor-pointer">
                <span className="text-2xl">+</span>
              </div>
              <span className="text-xs font-medium text-gray-500">New</span>
            </div>
            {recentContacts.map((c) => (
              <div key={c.name} onClick={() => handleContactSelect(c)} className="flex flex-col items-center gap-2 min-w-[64px] cursor-pointer group">
                <div className={`w-14 h-14 rounded-full ${c.color} flex items-center justify-center font-bold text-lg shadow-sm group-hover:scale-105 transition-transform ${selectedContact === c.accountId ? 'ring-2 ring-green-500 ring-offset-2' : ''}`}>
                  {c.initial}
                </div>
                <span className="text-xs font-medium text-gray-600 dark:text-gray-300">{c.name}</span>
              </div>
            ))}
          </div>
        </div>

        {/* Amount Input */}
        <div className="bg-white dark:bg-gray-800 rounded-3xl p-6 shadow-sm">
          <div className="flex justify-between items-center mb-4">
            <span className="text-sm text-gray-500 font-medium">Amount</span>
            <span className="text-xs font-bold text-green-600 bg-green-50 px-2 py-1 rounded-lg">IDR</span>
          </div>
          <div className="text-4xl font-bold text-gray-900 dark:text-white mb-2 flex items-center">
            <span className="text-gray-300 mr-2">Rp</span>
            <input
              {...register('amount', { valueAsNumber: true })}
              type="number"
              placeholder="0"
              className="w-full bg-transparent border-0 p-0 focus:ring-0 placeholder:text-gray-200 outline-none"
            />
          </div>
          {errors.amount && <p className="text-red-500 text-xs mt-2">{errors.amount.message}</p>}
          <div className="h-[1px] w-full bg-gray-100 dark:bg-gray-700 my-4"></div>
          <input
            {...register('description')}
            type="text"
            placeholder="Add a note (optional)"
            className="w-full text-sm font-medium bg-transparent border-0 p-0 focus:ring-0 placeholder:text-gray-400 outline-none"
          />
        </div>

        <button
          onClick={handleReview}
          className="w-full bg-black dark:bg-white text-white dark:text-black py-4 rounded-2xl font-bold text-lg shadow-xl shadow-gray-200 dark:shadow-none hover:scale-[1.02] transition-transform flex items-center justify-center gap-2"
        >
          Review Transfer <ChevronRight className="h-5 w-5" />
        </button>

      </div>
    </div>
  );
}
