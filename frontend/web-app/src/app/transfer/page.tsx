'use client';

import { Search, ChevronRight, PlusCircle, LifeBuoy } from "lucide-react";
import { useMutation } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { transferSchema, TransferRequest } from '@/types';
import api from '@/lib/api';
import { useState } from 'react';
import TransactionService from '@/services/TransactionService';
import DashboardLayout from "@/components/DashboardLayout";
import clsx from 'clsx';

export default function TransferPage() {
  const [selectedContact, setSelectedContact] = useState<string | null>(null);
  const [showReview, setShowReview] = useState(false);
  const transactionService = TransactionService;

  const recentContacts = [
    { name: 'Anya', initial: 'A', color: 'bg-bank-green/10 text-bank-green', accountId: 'acc-any123' },
    { name: 'Budi', initial: 'B', color: 'bg-blue-100/10 text-blue-600', accountId: 'acc-bud456' },
    { name: 'Citra', initial: 'C', color: 'bg-pink-100/10 text-pink-600', accountId: 'acc-cit789' },
    { name: 'Dodi', initial: 'D', color: 'bg-yellow-100/10 text-yellow-600', accountId: 'acc-dod012' },
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
      return transactionService.initiateTransfer({
        senderAccountId: data.fromAccountId,
        recipientAccountNumber: data.toAccountId,
        amount: data.amount,
        description: data.description || ''
      });
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
      <DashboardLayout>
        <div className="max-w-2xl mx-auto space-y-8">
          <div className="flex items-center gap-4">
            <button
              onClick={() => setShowReview(false)}
              className="p-2 hover:bg-gray-100 dark:hover:bg-gray-800 rounded-xl transition-colors"
            >
              <ChevronRight className="h-6 w-6 rotate-180" />
            </button>
            <h2 className="text-3xl font-black text-foreground">Review Transfer</h2>
          </div>

          <div className="bg-card rounded-[2.5rem] p-10 shadow-sm border border-border relative overflow-hidden">
            <div className="absolute top-0 right-0 w-64 h-64 bg-bank-green/5 rounded-full blur-3xl -z-0" />

            <div className="relative z-10 flex flex-col md:flex-row md:items-center justify-between gap-8 mb-10 pb-10 border-b border-border">
              <div className="flex items-center gap-6">
                <div className={`w-20 h-20 rounded-3xl ${selectedContactData?.color} flex items-center justify-center font-black text-3xl shadow-xl`}>
                  {selectedContactData?.initial}
                </div>
                <div>
                  <p className="text-[10px] text-gray-400 font-black uppercase tracking-widest mb-1">To Recipient</p>
                  <h3 className="text-2xl font-black text-foreground">{selectedContactData?.name}</h3>
                  <p className="text-xs font-bold text-bank-green">Acc: {selectedContact}</p>
                </div>
              </div>
              <div className="text-left md:text-right">
                <p className="text-[10px] text-gray-400 font-black uppercase tracking-widest mb-1">Transfer Amount</p>
                <p className="text-4xl font-black text-foreground">Rp {amount.toLocaleString()}</p>
                <p className="text-[10px] font-bold text-gray-400 uppercase tracking-widest mt-1">IDR Currency</p>
              </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-8 relative z-10">
              <div className="bg-gray-50 dark:bg-gray-900/50 p-6 rounded-2xl border border-border">
                <p className="text-[10px] text-gray-400 font-black uppercase tracking-widest mb-2">Sender Pocket</p>
                <p className="font-black text-foreground">Main Savings Account</p>
                <p className="text-xs font-bold text-gray-500">Balance: Rp 86.353.000</p>
              </div>
              {watch('description') && (
                <div className="bg-gray-50 dark:bg-gray-900/50 p-6 rounded-2xl border border-border">
                  <p className="text-[10px] text-gray-400 font-black uppercase tracking-widest mb-2">Transfer Note</p>
                  <p className="font-bold text-foreground">"{watch('description')}"</p>
                </div>
              )}
            </div>
          </div>

          <button
            onClick={handleSubmit(onSubmit)}
            disabled={transferMutation.isPending}
            className="w-full bg-bank-green text-white py-6 rounded-3xl font-black text-xl hover:bg-bank-emerald transition-all active:scale-[0.98] shadow-2xl shadow-bank-green/20 disabled:bg-bank-green/50 disabled:active:scale-100"
          >
            {transferMutation.isPending ? 'Processing Transaction...' : 'Complete Transfer Now'}
          </button>
        </div>
      </DashboardLayout>
    );
  }

  return (
    <DashboardLayout>
      <div className="max-w-4xl mx-auto space-y-10 pb-10">
        <div>
          <h2 className="text-3xl font-black text-foreground uppercase tracking-tight">Instant Transfer</h2>
          <p className="text-sm text-gray-500 font-medium">Send money securely to anyone, anywhere.</p>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-12 gap-10">
          {/* Left Column: Form */}
          <div className="lg:col-span-8 space-y-8">
            {/* Search / Recipient */}
            <div className="relative group">
              <Search className="absolute left-6 top-1/2 -translate-y-1/2 h-6 w-6 text-gray-400 group-focus-within:text-bank-green transition-colors" />
              <input
                {...register('toAccountId')}
                type="text"
                placeholder="Search name, account number, or scan ID"
                className="w-full pl-16 pr-6 py-6 rounded-[2rem] border-border bg-card shadow-sm focus:ring-4 focus:ring-bank-green/10 focus:border-bank-green transition-all text-lg font-black placeholder:text-gray-300 outline-none"
              />
              {errors.toAccountId && <p className="text-red-500 text-xs mt-3 pl-6 font-bold uppercase tracking-widest">{errors.toAccountId.message}</p>}
            </div>

            {/* Amount Section */}
            <div className="bg-card rounded-[2.5rem] p-10 shadow-sm border border-border relative overflow-hidden">
              <div className="absolute top-0 right-0 w-32 h-32 bg-bank-green/5 rounded-full blur-2xl" />
              <div className="flex justify-between items-center mb-6">
                <span className="text-[10px] text-gray-400 font-black uppercase tracking-widest">Enter Amount</span>
                <div className="flex items-center gap-2 bg-bank-green/10 px-3 py-1 rounded-full">
                  <div className="h-1.5 w-1.5 bg-bank-green rounded-full animate-pulse" />
                  <span className="text-[10px] font-black text-bank-green uppercase tracking-widest">Fixed IDR</span>
                </div>
              </div>

              <div className="flex items-center gap-4 mb-8">
                <span className="text-4xl font-black text-gray-300">Rp</span>
                <input
                  {...register('amount', { valueAsNumber: true })}
                  type="number"
                  placeholder="0"
                  className="w-full bg-transparent border-0 p-0 focus:ring-0 placeholder:text-gray-100 text-6xl font-black outline-none tracking-tighter"
                />
              </div>
              {errors.amount && <p className="text-red-500 text-xs mb-6 font-bold uppercase tracking-widest">{errors.amount.message}</p>}

              <div className="bg-gray-50 dark:bg-gray-900/50 p-6 rounded-2xl border border-border">
                <p className="text-[10px] text-gray-400 font-black uppercase tracking-widest mb-2">Message (Optional)</p>
                <input
                  {...register('description')}
                  type="text"
                  placeholder="What's this transfer for?"
                  className="w-full text-base font-bold bg-transparent border-0 p-0 focus:ring-0 placeholder:text-gray-400 outline-none"
                />
              </div>
            </div>

            <button
              onClick={handleReview}
              className="w-full bg-foreground text-background py-6 rounded-3xl font-black text-xl hover:bg-bank-green hover:text-white transition-all active:scale-[0.98] shadow-2xl shadow-gray-200 dark:shadow-none flex items-center justify-center gap-3 group"
            >
              Review Transfer Details
              <ChevronRight className="h-6 w-6 group-hover:translate-x-1 transition-transform" />
            </button>
          </div>

          {/* Right Column: Favorites */}
          <div className="lg:col-span-4 space-y-8">
            <div className="bg-card rounded-[2.5rem] p-8 shadow-sm border border-border">
              <h3 className="text-lg font-black text-foreground mb-6 uppercase tracking-widest">Favorites</h3>
              <div className="grid grid-cols-2 gap-4">
                {recentContacts.map((c) => (
                  <button
                    key={c.name}
                    onClick={() => handleContactSelect(c)}
                    className={clsx(
                      "flex flex-col items-center gap-3 p-4 rounded-3xl border transition-all group",
                      selectedContact === c.accountId
                        ? "bg-bank-green/5 border-bank-green scale-105"
                        : "bg-gray-50 dark:bg-gray-900/50 border-transparent hover:border-gray-200"
                    )}
                  >
                    <div className={`w-14 h-14 rounded-2xl ${c.color} flex items-center justify-center font-black text-xl shadow-sm group-hover:scale-110 transition-transform`}>
                      {c.initial}
                    </div>
                    <span className="text-[10px] font-black text-gray-600 dark:text-gray-300 uppercase tracking-widest">{c.name}</span>
                  </button>
                ))}
                <button className="flex flex-col items-center gap-3 p-4 rounded-3xl border border-dashed border-gray-300 hover:border-bank-green hover:bg-bank-green/5 transition-all group">
                  <div className="w-14 h-14 rounded-2xl bg-gray-100 dark:bg-gray-800 flex items-center justify-center text-gray-400 group-hover:text-bank-green transition-colors">
                    <PlusCircle className="h-6 w-6" />
                  </div>
                  <span className="text-[10px] font-black text-gray-400 group-hover:text-bank-green uppercase tracking-widest">New</span>
                </button>
              </div>
            </div>

            <div className="bg-gradient-to-br from-gray-900 to-gray-800 rounded-[2.5rem] p-8 text-white relative overflow-hidden">
              <div className="relative z-10">
                <h4 className="font-black text-lg mb-2">Need Help?</h4>
                <p className="text-xs text-gray-400 font-medium mb-6">Learn more about our transfer limits and safety features.</p>
                <button className="text-[10px] font-black uppercase tracking-widest bg-white/10 px-4 py-2 rounded-full hover:bg-white/20 transition-colors">Safety Guide</button>
              </div>
              <LifeBuoy className="absolute bottom-[-20px] right-[-20px] h-32 w-32 text-white/5 rotate-12" />
            </div>
          </div>
        </div>
      </div>
    </DashboardLayout>
  );
}
