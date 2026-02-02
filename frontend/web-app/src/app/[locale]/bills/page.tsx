'use client';

import { Smartphone, Zap, Droplets, Wifi, CreditCard, Heart, Tv, Gamepad2, Plus, ChevronRight, LifeBuoy } from "lucide-react";
import { useMutation, useQuery } from '@tanstack/react-query';
import { CreatePaymentRequest, PaymentResponse } from '@/types';
import api from '@/lib/api';
import { useState } from 'react';
import DashboardLayout from "@/components/DashboardLayout";
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { useUIStore } from '@/stores';
import { PageTransition, StaggerContainer, StaggerItem, ButtonMotion } from '@/components/ui/Motion';
import { cn } from '@/lib/utils';

export default function BillsPage() {
 const { addToast } = useUIStore();
 const [selectedBiller, setSelectedBiller] = useState<{ name: string; icon: React.ComponentType<{ className?: string }>; color: string; code: string } | null>(null);
 const [customerId, setCustomerId] = useState('');
 const [amount, setAmount] = useState('');

 const billers = [
  { name: 'Pulsa', icon: Smartphone, color: 'bg-blue-100 text-blue-600', code: 'PULSA' },
  { name: 'Listrik (PLN)', icon: Zap, color: 'bg-yellow-100 text-yellow-600', code: 'PLN' },
  { name: 'Air (PDAM)', icon: Droplets, color: 'bg-cyan-100 text-cyan-600', code: 'PDAM' },
  { name: 'Internet/TV', icon: Wifi, color: 'bg-indigo-100 text-indigo-600', code: 'INTERNET' },
  { name: 'Saldo Kartu', icon: CreditCard, color: 'bg-purple-100 text-purple-600', code: 'CARDS' },
  { name: 'BPJS', icon: Heart, color: 'bg-green-100 text-green-600', code: 'BPJS' },
  { name: 'TV Kabel', icon: Tv, color: 'bg-pink-100 text-pink-600', code: 'TV' },
  { name: 'Game Voucher', icon: Gamepad2, color: 'bg-orange-100 text-orange-600', code: 'VOUCHER' },
 ];

 const { data: recentPayments, isLoading } = useQuery({
  queryKey: ['recent-payments'],
  queryFn: async () => {
   const response = await api.get('/payments?size=5');
   return response.data;
  },
  enabled: false
 });

 const paymentMutation = useMutation({
  mutationFn: (data: CreatePaymentRequest) => {
   return api.post('/payments', data);
  },
  onSuccess: () => {
   addToast(`Pembayaran ${selectedBiller?.name} sebesar Rp ${parseFloat(amount).toLocaleString()} telah diproses.`, 'success');
   setSelectedBiller(null);
   setCustomerId('');
   setAmount('');
  },
  onError: (error) => {
   console.error('Pembayaran gagal:', error);
   addToast('Terjadi kesalahan saat memproses pembayaran. Silakan coba lagi.', 'error');
  }
 });

 const handlePay = () => {
  if (!selectedBiller || !customerId || !amount) {
   alert('Silakan isi semua bidang yang diperlukan');
   return;
  }

  const data: CreatePaymentRequest = {
   billerCode: selectedBiller.code,
   customerId,
   amount: parseFloat(amount),
   referenceNumber: `REF-${Date.now()}`,
  };

  paymentMutation.mutate(data);
 };

 const recentBills = recentPayments?.content || [];

 if (selectedBiller) {
  return (
   <DashboardLayout>
    <div className="space-y-12">
     <div className="flex items-center gap-4">
      <Button
       variant="outline"
       size="icon"
       onClick={() => setSelectedBiller(null)}
       className="h-12 w-12 rounded-xl"
      >
       <ChevronRight className="h-6 w-6 rotate-180" />
      </Button>
      <h2 className="text-2xl sm:text-3xl font-bold text-foreground tracking-tight">Bayar {selectedBiller.name}</h2>
     </div>

     <div className="bg-card rounded-2xl p-6 sm:p-10 border border-border relative overflow-hidden group shadow-sm">
      <div className={cn("absolute top-0 right-0 w-48 sm:w-64 h-48 sm:h-64 opacity-10 rounded-full blur-3xl -z-0", selectedBiller.color.split(' ')[0])} />

      <div className="relative z-10 flex items-center gap-4 sm:gap-6 mb-6 sm:mb-10 pb-6 sm:pb-10 border-b border-border">
       <div className={`w-16 h-16 sm:w-20 sm:h-20 rounded-xl ${selectedBiller.color} flex items-center justify-center shadow-xl transition-transform group-hover:scale-110`}>
        <selectedBiller.icon className="h-8 w-8 sm:h-10 sm:w-10" />
       </div>
       <div>
        <p className="text-xs text-gray-400 font-bold tracking-widest mb-1">Penyedia Layanan</p>
        <h3 className="text-xl sm:text-2xl font-bold text-foreground ">{selectedBiller.name}</h3>
        <p className="text-xs font-bold text-bank-green tracking-widest">Mitra Pembayaran Resmi</p>
       </div>
      </div>

      <div className="space-y-6 sm:space-y-12 relative z-10">
       <div className="group">
        <label className="text-xs font-bold text-muted-foreground tracking-[0.2em] uppercase ml-1 block mb-4 group-focus-within:text-primary transition-colors">ID Pelanggan / Nomor Rekening</label>
        <Input
         type="text"
         value={customerId}
         onChange={(e) => setCustomerId(e.target.value)}
         placeholder="Masukkan ID unik Anda"
         className="h-16 text-lg sm:text-xl"
        />
       </div>

       <div className="group">
        <label className="text-xs font-bold text-muted-foreground tracking-[0.2em] uppercase ml-1 block mb-4 group-focus-within:text-primary transition-colors">Jumlah Pembayaran (IDR)</label>
        <div className="relative">
         <div className="absolute left-6 top-1/2 -translate-y-1/2 text-xl sm:text-2xl font-bold text-muted-foreground/30 pointer-events-none">Rp</div>
         <Input
          type="number"
          value={amount}
          onChange={(e) => setAmount(e.target.value)}
          placeholder="0"
          className="h-20 pl-16 text-3xl sm:text-4xl"
         />
        </div>
       </div>
      </div>
     </div>

     <div className="flex flex-col gap-6">
      <ButtonMotion className="w-full">
       <Button
        onClick={handlePay}
        disabled={paymentMutation.isPending}
        className="w-full h-16 rounded-2xl shadow-xl shadow-emerald-500/20"
       >
        {paymentMutation.isPending ? 'Sedang Memproses...' : 'Konfirmasi & Bayar Sekarang'}
       </Button>
      </ButtonMotion>
      <p className="text-center text-xs text-muted-foreground font-bold tracking-widest uppercase opacity-60">Transaksi aman terenkripsi oleh Infrastruktur Protokol PayU</p>
     </div>
    </div>
   </DashboardLayout>
  );
 }

 return (
  <DashboardLayout>
   <div className="space-y-12">
    <div className="flex flex-col sm:flex-row justify-between items-start sm:items-end gap-4">
     <div>
      <h2 className="text-3xl font-bold text-foreground tracking-tight">Tagihan & Top-up</h2>
      <p className="text-sm text-muted-foreground font-medium mt-1">Bayar tagihan utilitas dan top up dompet digital Anda secara instan.</p>
     </div>
     <div className="bg-primary/10 px-6 py-3 rounded-full border border-primary/20 hidden md:block shadow-sm">
      <p className="text-xs font-bold text-primary tracking-widest uppercase animate-pulse">Penyelesaian Real-time 24/7</p>
     </div>
    </div>

    {/* Biller Grid */}
    <div className="bg-card rounded-2xl p-8 sm:p-12 border border-border relative overflow-hidden shadow-sm">
     <div className="absolute top-0 right-0 w-48 sm:w-64 h-48 sm:h-64 bg-primary/5 rounded-full blur-3xl" />
     <h3 className="text-xs font-bold text-muted-foreground tracking-[0.2em] uppercase mb-10 text-center opacity-60">Kategori Layanan</h3>
     <div className="grid grid-cols-3 sm:grid-cols-4 gap-4 sm:gap-12 relative z-10">
      {billers.map((item) => (
       <button
        key={item.name}
        onClick={() => setSelectedBiller(item)}
        className="flex flex-col items-center gap-4 transition-all group active:scale-95"
       >
        <div className={`w-16 h-16 sm:w-20 sm:h-20 rounded-2xl ${item.color} flex items-center justify-center shadow-lg transition-transform group-hover:scale-110`}>
         <item.icon className="h-7 w-7 sm:h-9 sm:w-9" />
        </div>
        <span className="text-[10px] sm:text-xs font-bold text-foreground tracking-widest uppercase">{item.name}</span>
       </button>
      ))}
      <button className="flex flex-col items-center gap-4 transition-all group active:scale-95">
       <div className="w-16 h-16 sm:w-20 sm:h-20 rounded-2xl bg-muted/50 flex items-center justify-center text-muted-foreground group-hover:text-primary transition-colors">
        <Plus className="h-7 w-7 sm:h-9 sm:w-9" />
       </div>
       <span className="text-[10px] sm:text-xs font-bold text-muted-foreground tracking-widest uppercase">Lainnya</span>
      </button>
     </div>
    </div>

    {/* Recent Bills */}
    <div className="space-y-8">
     <h3 className="text-xl font-bold text-foreground tracking-tight">Aktivitas Terakhir</h3>
     {!isLoading && recentBills.length > 0 ? (
      <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
       {recentBills.map((bill: PaymentResponse) => (
        <div key={bill.id} className="bg-card p-6 sm:p-8 rounded-2xl flex items-center justify-between border border-border hover:shadow-xl transition-all group shadow-sm">
         <div className="flex items-center gap-6">
          <div className="w-12 h-12 rounded-xl bg-primary/10 flex items-center justify-center text-primary transition-transform group-hover:scale-110">
           <Zap className="h-6 w-6" />
          </div>
          <div>
           <div className="font-bold text-foreground text-sm uppercase tracking-wider">{bill.billerCode}</div>
           <div className="text-[10px] font-bold text-muted-foreground tracking-[0.2em] uppercase mt-1">Ref: {bill.referenceNumber.slice(0, 10)}...</div>
          </div>
         </div>
         <div className="text-right">
          <div className="font-bold text-foreground text-base tabular-nums">Rp {bill.amount.toLocaleString('id-ID')}</div>
          <div className="text-[10px] font-bold text-primary tracking-[0.2em] uppercase mt-1">{bill.status}</div>
         </div>
        </div>
       ))}
      </div>
     ) : (
      <div className="bg-card rounded-2xl p-12 text-center border-2 border-dashed border-border flex flex-col items-center justify-center">
       <LifeBuoy className="h-16 w-16 text-muted/20 mb-6" />
       <p className="text-muted-foreground font-bold tracking-[0.2em] text-[10px] uppercase max-w-xs leading-relaxed opacity-60">Pembayaran tagihan terakhir Anda akan muncul di sini.</p>
      </div>
     )}
    </div>
   </div>
  </DashboardLayout>
 );
}
