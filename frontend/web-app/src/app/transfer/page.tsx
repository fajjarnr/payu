'use client';

import { Search, ChevronRight, PlusCircle, LifeBuoy, ArrowRight } from "lucide-react";
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { transferSchema, TransferRequest } from '@/types';
import { useState } from 'react';
import { useInitiateTransfer } from '@/hooks';
import { useAuthStore } from '@/stores';
import { useUIStore } from '@/stores';
import DashboardLayout from "@/components/DashboardLayout";
import clsx from 'clsx';
import { PageTransition, StaggerContainer, StaggerItem, ButtonMotion } from '@/components/ui/Motion';

export default function TransferPage() {
  const [selectedContact, setSelectedContact] = useState<string | null>(null);
  const [showReview, setShowReview] = useState(false);
  const accountId = useAuthStore((state) => state.accountId);
  const addToast = useUIStore((state) => state.addToast);
  const transferMutation = useInitiateTransfer();

  const recentContacts = [
    { name: 'Anya', initial: 'A', color: 'bg-primary/10 text-primary', accountId: 'acc-any123' },
    { name: 'Budi', initial: 'B', color: 'bg-blue-500/10 text-blue-600', accountId: 'acc-bud456' },
    { name: 'Citra', initial: 'C', color: 'bg-pink-500/10 text-pink-600', accountId: 'acc-cit789' },
    { name: 'Dodi', initial: 'D', color: 'bg-amber-500/10 text-amber-600', accountId: 'acc-dod012' },
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
        <PageTransition>
          <div className="space-y-12">
            <StaggerContainer>
              <StaggerItem>
                <div className="flex items-center gap-6">
                  <button
                    onClick={() => setShowReview(false)}
                    className="p-3 bg-muted rounded-xl border border-border hover:bg-muted font-bold transition-all active:scale-95"
                  >
                    <ChevronRight className="h-6 w-6 rotate-180" />
                  </button>
                  <h2 className="text-3xl font-black text-foreground">Tinjau Transfer</h2>
                </div>
              </StaggerItem>

              <StaggerItem>
                <div className="bg-card rounded-xl p-8 sm:p-12 shadow-card border border-border relative overflow-hidden group">
                  <div className="absolute top-0 right-0 w-80 h-80 bg-primary/5 rounded-full blur-3xl" />

                  <div className="relative z-10 flex flex-col md:flex-row md:items-center justify-between gap-10 mb-12 pb-12 border-b border-border">
                    <div className="flex items-center gap-6">
                      <div className={clsx("w-20 h-20 rounded-2xl flex items-center justify-center font-black text-3xl shadow-lg transition-transform group-hover:rotate-3", selectedContactData?.color)}>
                        {selectedContactData?.initial}
                      </div>
                      <div>
                        <p className="text-[10px] font-bold text-muted-foreground tracking-widest uppercase mb-1">Kepada Penerima</p>
                        <h3 className="text-2xl font-black text-foreground">{selectedContactData?.name}</h3>
                        <p className="text-xs font-bold text-primary tracking-tight">ID Akun: {selectedContact}</p>
                      </div>
                    </div>
                    <div className="text-left md:text-right">
                      <p className="text-[10px] font-bold text-muted-foreground tracking-widest uppercase mb-1">Jumlah Transfer</p>
                      <p className="text-4xl sm:text-5xl font-black text-foreground">Rp {amount.toLocaleString('id-ID')}</p>
                      <p className="text-[10px] font-bold text-muted-foreground tracking-widest uppercase mt-2">Mata Uang IDR</p>
                    </div>
                  </div>

                  <div className="grid grid-cols-1 md:grid-cols-2 gap-8 relative z-10">
                    <div className="bg-muted p-8 rounded-xl border border-border">
                      <p className="text-[10px] font-bold text-muted-foreground tracking-widest uppercase mb-2">Kantong Sumber</p>
                      <p className="font-black text-foreground text-lg">Kantong Utama Cair</p>
                      <p className="text-[10px] font-bold text-primary tracking-widest uppercase mt-2">Saldo: Rp 86.353.000</p>
                    </div>
                    {watch('description') && (
                      <div className="bg-muted p-8 rounded-xl border border-border">
                        <p className="text-[10px] font-bold text-muted-foreground tracking-widest uppercase mb-2">Pesan Konfirmasi</p>
                        <p className="font-bold text-foreground text-lg italic">&quot;{watch('description')}&quot;</p>
                      </div>
                    )}
                  </div>
                </div>
              </StaggerItem>

              <StaggerItem>
                <ButtonMotion>
                  <button
                    onClick={handleSubmit(onSubmit)}
                    disabled={transferMutation.isPending}
                    className="w-full bg-primary text-primary-foreground py-6 rounded-2xl font-bold text-xs tracking-widest uppercase shadow-2xl shadow-primary/20 disabled:bg-primary/50"
                  >
                    {transferMutation.isPending ? 'Memvalidasi Transaksi...' : 'Otorisasi Transfer Sekarang'}
                  </button>
                </ButtonMotion>
              </StaggerItem>
            </StaggerContainer>
          </div>
        </PageTransition>
      </DashboardLayout>
    );
  }

  return (
    <DashboardLayout>
      <PageTransition>
        <div className="space-y-12">
          <StaggerContainer>
            <StaggerItem>
              <div className="mb-8">
                <h2 className="text-3xl font-black text-foreground">Transfer Instan</h2>
                <p className="text-sm text-muted-foreground font-medium mt-1">Kirim dana secara aman dalam hitungan detik.</p>
              </div>
            </StaggerItem>

            <div className="grid grid-cols-1 lg:grid-cols-12 gap-10">
              <StaggerItem className="lg:col-span-8 space-y-8">
                <div className="relative group">
                  <Search className="absolute left-6 top-1/2 -translate-y-1/2 h-6 w-6 text-muted-foreground group-focus-within:text-primary transition-colors" />
                  <input
                    {...register('toAccountId')}
                    type="text"
                    placeholder="Masukkan ID Akun atau Nomor Rekening"
                    className="w-full pl-16 pr-8 py-8 rounded-2xl border border-border bg-card shadow-card focus:ring-4 focus:ring-primary/10 focus:border-primary transition-all text-lg font-bold placeholder:text-muted-foreground/40 outline-none"
                  />
                  {errors.toAccountId && <p className="text-destructive text-[10px] mt-4 ml-6 font-bold tracking-widest uppercase">{errors.toAccountId.message}</p>}
                </div>

                <div className="bg-card rounded-2xl p-10 border border-border shadow-card relative overflow-hidden">
                  <div className="absolute top-0 right-0 w-48 h-48 bg-primary/5 rounded-full blur-3xl -z-0" />

                  <div className="flex justify-between items-center mb-10 relative z-10">
                    <span className="text-[10px] font-bold text-muted-foreground tracking-widest uppercase">Nominal Transfer</span>
                    <div className="flex items-center gap-3 bg-success-light px-4 py-1.5 rounded-full border border-primary/10 shadow-sm">
                      <div className="h-1.5 w-1.5 bg-primary rounded-full animate-pulse" />
                      <span className="text-[10px] font-bold text-primary tracking-widest uppercase">Secured IDR</span>
                    </div>
                  </div>

                  <div className="flex items-center gap-6 mb-10 relative z-10">
                    <span className="text-4xl sm:text-5xl font-black text-muted-foreground/30">Rp</span>
                    <input
                      {...register('amount', { valueAsNumber: true })}
                      type="number"
                      placeholder="0"
                      className="w-full bg-transparent border-0 p-0 focus:ring-0 placeholder:text-muted-foreground/10 text-5xl sm:text-7xl font-black outline-none text-foreground"
                    />
                  </div>

                  {errors.amount && <p className="text-destructive text-[10px] mb-8 font-bold tracking-widest uppercase">{errors.amount.message}</p>}

                  <div className="bg-muted/50 p-8 rounded-xl border border-border relative z-10">
                    <p className="text-[10px] font-bold text-muted-foreground tracking-widest uppercase mb-3">Memo Transaksi</p>
                    <input
                      {...register('description')}
                      type="text"
                      placeholder="Apa tujuan transfer ini?"
                      className="w-full text-base font-bold bg-transparent border-0 p-0 focus:ring-0 placeholder:text-muted-foreground/40 outline-none"
                    />
                  </div>
                </div>

                <ButtonMotion>
                  <button
                    onClick={handleReview}
                    className="w-full bg-foreground text-background py-6 rounded-2xl font-bold text-xs tracking-widest uppercase shadow-2xl flex items-center justify-center gap-4 group"
                  >
                    Tinjau Ringkasan Transfer
                    <ArrowRight className="h-5 w-5 group-hover:translate-x-2 transition-transform" />
                  </button>
                </ButtonMotion>
              </StaggerItem>

              <StaggerItem className="lg:col-span-4 space-y-8">
                <div className="bg-card rounded-2xl p-10 border border-border shadow-card h-full flex flex-col">
                  <div className="flex justify-between items-center mb-10">
                    <h3 className="text-[10px] font-bold text-foreground tracking-widest uppercase">Penerima Favorit</h3>
                    <div className="h-1 w-8 bg-primary rounded-full" />
                  </div>

                  <div className="grid grid-cols-2 gap-6">
                    {recentContacts.map((c) => (
                      <button
                        key={c.name}
                        onClick={() => handleContactSelect(c)}
                        className={clsx(
                          "flex flex-col items-center gap-4 p-6 rounded-xl border transition-all group",
                          selectedContact === c.accountId
                            ? "bg-primary/5 border-primary shadow-lg shadow-primary/10"
                            : "bg-muted border-transparent hover:border-border hover:bg-card"
                        )}
                      >
                        <div className={`w-14 h-14 rounded-2xl ${c.color} flex items-center justify-center font-black text-2xl shadow-sm group-hover:scale-110 transition-transform`}>
                          {c.initial}
                        </div>
                        <span className="text-[10px] font-bold text-muted-foreground tracking-widest uppercase">{c.name}</span>
                      </button>
                    ))}
                    <button className="flex flex-col items-center gap-4 p-6 rounded-xl border border-dashed border-border hover:border-primary hover:bg-primary/5 transition-all group">
                      <div className="w-14 h-14 rounded-2xl bg-muted flex items-center justify-center text-muted-foreground group-hover:text-primary transition-colors">
                        <PlusCircle className="h-6 w-6" />
                      </div>
                      <span className="text-[10px] font-bold text-muted-foreground tracking-widest uppercase">Tambah</span>
                    </button>
                  </div>

                  <div className="mt-auto pt-10">
                    <div className="bg-gradient-to-br from-gray-900 to-gray-800 rounded-xl p-8 text-white relative overflow-hidden shadow-2xl group">
                      <div className="relative z-10">
                        <h4 className="font-black text-xl mb-2">Bantuan?</h4>
                        <p className="text-[10px] text-gray-400 font-bold tracking-widest uppercase mb-8 leading-relaxed">Proteksi & panduan transaksi aman.</p>
                        <button className="text-[10px] font-bold tracking-widest uppercase bg-white/10 px-6 py-3 rounded-xl border border-white/10 hover:bg-white/20 transition-all">Hubungi Kami</button>
                      </div>
                      <LifeBuoy className="absolute bottom-[-30px] right-[-30px] h-48 w-48 text-white/5 rotate-12" />
                    </div>
                  </div>
                </div>
              </StaggerItem>
            </div>
          </StaggerContainer>
        </div>
      </PageTransition>
    </DashboardLayout>
  );
}
