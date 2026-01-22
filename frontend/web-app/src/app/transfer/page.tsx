'use client';

import { Search, ChevronRight, PlusCircle, LifeBuoy } from "lucide-react";
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { transferSchema, TransferRequest } from '@/types';
import { useState } from 'react';
import { useInitiateTransfer } from '@/hooks';
import { useAuthStore } from '@/stores';
import { useUIStore } from '@/stores';
import DashboardLayout from "@/components/DashboardLayout";
import clsx from 'clsx';

export default function TransferPage() {
  const [selectedContact, setSelectedContact] = useState<string | null>(null);
  const [showReview, setShowReview] = useState(false);
  const accountId = useAuthStore((state) => state.accountId);
  const addToast = useUIStore((state) => state.addToast);
  const transferMutation = useInitiateTransfer();

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

  const handleContactSelect = (contact: { name: string; accountId: string }) => {
    setSelectedContact(contact.accountId);
    setValue('toAccountId', contact.accountId);
    setValue('fromAccountId', accountId || '');
  };

  const onSubmit = (data: TransferRequest) => {
    transferMutation.mutate(
      {
        senderAccountId: data.fromAccountId,
        recipientAccountNumber: data.toAccountId,
        amount: data.amount,
        description: data.description || '',
        type: 'INTERNAL_TRANSFER'
      },
      {
        onSuccess: () => {
          addToast('Transfer berhasil!', 'success');
          setShowReview(false);
          setSelectedContact(null);
          setValue('amount', 0);
          setValue('description', '');
        },
        onError: () => {
          addToast('Transfer gagal. Silakan coba lagi.', 'error');
        }
      }
    );
  };

  const handleReview = () => {
    const data = { ...watch(), fromAccountId: accountId || '' };
    if (!data.toAccountId || data.amount <= 0) {
      addToast('Silakan pilih penerima dan masukkan jumlah transfer', 'warning');
      return;
    }
    setShowReview(true);
  };

  if (showReview) {
    const selectedContactData = recentContacts.find(c => c.accountId === selectedContact);
    return (
      <DashboardLayout>
        <div className="max-w-2xl mx-auto space-y-8 sm:space-y-10">
          <div className="flex items-center gap-4">
            <button
              onClick={() => setShowReview(false)}
              className="p-3 bg-gray-50 dark:bg-gray-900 rounded-2xl border border-border hover:bg-gray-100 transition-all active:scale-95"
            >
              <ChevronRight className="h-5 w-5 sm:h-6 sm:w-6 rotate-180" />
            </button>
            <h2 className="text-2xl sm:text-3xl font-black text-foreground uppercase tracking-tight italic">Tinjau Transfer</h2>
          </div>

          <div className="bg-card rounded-[2rem] sm:rounded-[3rem] p-6 sm:p-10 shadow-sm border border-border relative overflow-hidden group">
            <div className="absolute top-0 right-0 w-48 sm:w-64 h-48 sm:h-64 bg-bank-green/5 rounded-full blur-3xl -z-0" />

            <div className="relative z-10 flex flex-col md:flex-row md:items-center justify-between gap-6 sm:gap-10 mb-8 sm:mb-10 pb-8 sm:pb-10 border-b border-border">
              <div className="flex items-center gap-4 sm:gap-6">
                <div className={`w-16 h-16 sm:w-20 sm:h-20 rounded-[1.5rem] ${selectedContactData?.color} flex items-center justify-center font-black text-2xl sm:text-3xl shadow-xl transition-transform group-hover:scale-110`}>
                  {selectedContactData?.initial}
                </div>
                <div>
                  <p className="text-[10px] text-gray-400 font-black uppercase tracking-widest mb-1">Kepada Penerima</p>
                  <h3 className="text-xl sm:text-2xl font-black text-foreground uppercase tracking-tight">{selectedContactData?.name}</h3>
                  <p className="text-xs font-bold text-bank-green uppercase tracking-widest">No. Rek: {selectedContact}</p>
                </div>
              </div>
              <div className="text-left md:text-right w-full md:w-auto">
                <p className="text-[10px] text-gray-400 font-black uppercase tracking-widest mb-1">Jumlah Transfer</p>
                <p className="text-3xl sm:text-4xl font-black text-foreground italic">Rp {amount.toLocaleString('id-ID')}</p>
                <p className="text-[10px] font-black text-gray-400 uppercase tracking-widest mt-1 italic">Mata Uang IDR</p>
              </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-6 sm:gap-8 relative z-10">
              <div className="bg-gray-50 dark:bg-gray-900/50 p-5 sm:p-8 rounded-[1.25rem] sm:rounded-[1.5rem] border border-border">
                <p className="text-[10px] font-black text-gray-400 uppercase tracking-widest mb-2">Kantong Pengirim</p>
                <p className="font-black text-foreground uppercase tracking-tight text-sm">Tabungan Utama Cair</p>
                <p className="text-[10px] font-black text-gray-400 uppercase tracking-widest mt-1">Saldo: Rp 86.353.000</p>
              </div>
              {watch('description') && (
                <div className="bg-gray-50 dark:bg-gray-900/50 p-5 sm:p-8 rounded-[1.25rem] sm:rounded-[1.5rem] border border-border">
                  <p className="text-[10px] font-black text-gray-400 uppercase tracking-widest mb-2">Catatan Transfer</p>
                  <p className="font-black text-foreground italic">&quot;{watch('description')}&quot;</p>
                </div>
              )}
            </div>
          </div>

          <button
            onClick={handleSubmit(onSubmit)}
            disabled={transferMutation.isPending}
            className="w-full bg-bank-green text-white py-5 sm:py-6 rounded-[1.25rem] sm:rounded-[1.5rem] font-black text-xs uppercase tracking-[0.2em] hover:bg-bank-emerald transition-all active:scale-[0.98] shadow-2xl shadow-bank-green/20 disabled:bg-bank-green/50 disabled:active:scale-100"
          >
            {transferMutation.isPending ? 'Sedang Memproses Transaksi...' : 'Selesaikan Transfer Sekarang'}
          </button>
        </div>
      </DashboardLayout>
    );
  }

  return (
    <DashboardLayout>
      <div className="max-w-4xl mx-auto space-y-8 sm:space-y-10 pb-10">
        <div>
          <h2 className="text-2xl sm:text-3xl font-black text-foreground uppercase tracking-tight italic">Transfer Instan</h2>
          <p className="text-sm text-gray-500 font-medium">Kirim uang dengan aman kepada siapa saja, di mana saja.</p>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 sm:gap-10">
          {/* Left Column: Form */}
          <div className="lg:col-span-8 space-y-6 sm:space-y-8">
            {/* Search / Recipient */}
            <div className="relative group">
              <Search className="absolute left-5 sm:left-6 top-1/2 -translate-y-1/2 h-5 w-5 sm:h-6 sm:w-6 text-gray-400 group-focus-within:text-bank-green transition-colors" />
              <input
                {...register('toAccountId')}
                type="text"
                placeholder="Cari nama, nomor rekening, atau pindai ID"
                className="w-full pl-14 sm:pl-16 pr-4 sm:pr-6 py-5 sm:py-8 rounded-[1.5rem] sm:rounded-[2rem] border-border bg-card shadow-sm focus:ring-4 focus:ring-bank-green/10 focus:border-bank-green transition-all text-base sm:text-lg font-black placeholder:text-gray-200 outline-none uppercase tracking-tight"
              />
              {errors.toAccountId && <p className="text-red-500 text-[10px] mt-4 pl-6 font-black uppercase tracking-widest">{errors.toAccountId.message}</p>}
            </div>

            {/* Amount Section */}
            <div className="bg-card rounded-[1.75rem] sm:rounded-[2.5rem] p-6 sm:p-10 shadow-sm border border-border relative overflow-hidden">
              <div className="absolute top-0 right-0 w-24 sm:w-32 h-24 sm:h-32 bg-bank-green/5 rounded-full blur-2xl" />
              <div className="flex justify-between items-center mb-6 sm:mb-8">
                <span className="text-[10px] font-black text-gray-400 uppercase tracking-widest">Masukkan Jumlah</span>
                <div className="flex items-center gap-2 bg-bank-green/10 px-3 sm:px-4 py-1.5 rounded-full">
                  <div className="h-1.5 w-1.5 bg-bank-green rounded-full animate-pulse" />
                  <span className="text-[10px] font-black text-bank-green uppercase tracking-widest tracking-[0.2em]">IDR FIXED</span>
                </div>
              </div>

              <div className="flex items-center gap-3 sm:gap-4 mb-6 sm:mb-10">
                <span className="text-3xl sm:text-4xl font-black text-gray-300">Rp</span>
                <input
                  {...register('amount', { valueAsNumber: true })}
                  type="number"
                  placeholder="0"
                  className="w-full bg-transparent border-0 p-0 focus:ring-0 placeholder:text-gray-100 text-4xl sm:text-6xl font-black outline-none tracking-tighter"
                />
              </div>
              {errors.amount && <p className="text-red-500 text-[10px] mb-6 sm:mb-8 font-black uppercase tracking-widest">{errors.amount.message}</p>}

              <div className="bg-gray-50 dark:bg-gray-900/50 p-5 sm:p-8 rounded-[1.25rem] sm:rounded-[1.5rem] border border-border">
                <p className="text-[10px] font-black text-gray-400 uppercase tracking-widest mb-3">Pesan (Opsional)</p>
                <input
                  {...register('description')}
                  type="text"
                  placeholder="Apa tujuan transfer ini?"
                  className="w-full text-sm sm:text-base font-black bg-transparent border-0 p-0 focus:ring-0 placeholder:text-gray-300 outline-none uppercase tracking-tight"
                />
              </div>
            </div>

            <button
              onClick={handleReview}
              className="w-full bg-foreground text-background py-5 sm:py-6 rounded-[1.25rem] sm:rounded-[1.5rem] font-black text-xs uppercase tracking-[0.2em] hover:bg-bank-green hover:text-white transition-all active:scale-[0.98] shadow-2xl flex items-center justify-center gap-3 group"
            >
              Tinjau Detail Transfer
              <ChevronRight className="h-5 w-5 group-hover:translate-x-1 transition-transform" />
            </button>
          </div>

          {/* Right Column: Favorites */}
          <div className="lg:col-span-4 space-y-6 sm:space-y-8">
            <div className="bg-card rounded-[1.75rem] sm:rounded-[2.5rem] p-6 sm:p-10 shadow-sm border border-border">
              <div className="flex justify-between items-center mb-6 sm:mb-10">
                <h3 className="text-[10px] font-black text-foreground uppercase tracking-[0.2em]">Penerima Favorit</h3>
                <div className="h-1.5 w-10 bg-bank-green/20 rounded-full" />
              </div>
              <div className="grid grid-cols-2 gap-4 sm:gap-6">
                {recentContacts.map((c) => (
                  <button
                    key={c.name}
                    onClick={() => handleContactSelect(c)}
                    className={clsx(
                      "flex flex-col items-center gap-3 sm:gap-4 p-4 sm:p-5 rounded-[1.5rem] sm:rounded-[2rem] border transition-all group",
                      selectedContact === c.accountId
                        ? "bg-bank-green/5 border-bank-green scale-105"
                        : "bg-gray-50 dark:bg-gray-900/50 border-transparent hover:border-border"
                    )}
                  >
                    <div className={`w-12 h-12 sm:w-14 sm:h-14 rounded-[1.25rem] ${c.color} flex items-center justify-center font-black text-xl sm:text-2xl shadow-sm group-hover:scale-110 transition-transform`}>
                      {c.initial}
                    </div>
                    <span className="text-[10px] font-black text-gray-400 uppercase tracking-widest">{c.name}</span>
                  </button>
                ))}
                <button className="flex flex-col items-center gap-3 sm:gap-4 p-4 sm:p-5 rounded-[1.5rem] sm:rounded-[2rem] border border-dashed border-gray-200 hover:border-bank-green hover:bg-bank-green/5 transition-all group">
                  <div className="w-12 h-12 sm:w-14 sm:h-14 rounded-[1.25rem] bg-gray-50 dark:bg-gray-800 flex items-center justify-center text-gray-300 group-hover:text-bank-green transition-colors">
                    <PlusCircle className="h-5 w-5 sm:h-6 sm:w-6" />
                  </div>
                  <span className="text-[10px] font-black text-gray-400 group-hover:text-bank-green uppercase tracking-widest">Baru</span>
                </button>
              </div>
            </div>

            <div className="bg-gradient-to-br from-gray-900 to-gray-800 rounded-[1.75rem] sm:rounded-[2.5rem] p-6 sm:p-10 text-white relative overflow-hidden shadow-2xl">
              <div className="relative z-10">
                <h4 className="font-black text-lg sm:text-xl mb-2 italic tracking-tight">Butuh Bantuan?</h4>
                <p className="text-[10px] text-gray-400 font-bold uppercase tracking-widest mb-6 sm:mb-8 leading-relaxed">Pelajari lebih lanjut tentang batas transfer dan fitur keamanan kami.</p>
                <button className="text-[10px] font-black uppercase tracking-[0.2em] bg-white/10 px-5 sm:px-6 py-2.5 sm:py-3 rounded-full border border-white/10 hover:bg-white/20 transition-all">Panduan Keamanan</button>
              </div>
              <LifeBuoy className="absolute bottom-[-20px] sm:bottom-[-30px] right-[-20px] sm:right-[-30px] h-36 w-36 sm:h-48 sm:w-48 text-white/5 rotate-12" />
            </div>
          </div>
        </div>
      </div>
    </DashboardLayout>
  );
}
