'use client';

import { Smartphone, Zap, Droplets, Wifi, CreditCard, Heart, Tv, Gamepad2, Plus, ChevronRight, LifeBuoy } from "lucide-react";
import { useMutation, useQuery } from '@tanstack/react-query';
import { CreatePaymentRequest, PaymentResponse } from '@/types';
import api from '@/lib/api';
import { useState } from 'react';
import DashboardLayout from "@/components/DashboardLayout";

export default function BillsPage() {
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
   alert('Pembayaran berhasil!');
   setSelectedBiller(null);
   setCustomerId('');
   setAmount('');
  },
  onError: (error) => {
   console.error('Pembayaran gagal:', error);
   alert('Pembayaran gagal. Silakan coba lagi.');
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
      <button
       onClick={() => setSelectedBiller(null)}
       className="p-3 bg-gray-50 dark:bg-gray-900 rounded-xl border border-border hover:bg-gray-100 transition-all active:scale-95"
      >
       <ChevronRight className="h-5 w-5 sm:h-6 sm:w-6 rotate-180" />
      </button>
      <h2 className="text-2xl sm:text-3xl font-black text-foreground ">Bayar {selectedBiller.name}</h2>
     </div>

     <div className="bg-card rounded-xl p-6 sm:p-10 shadow-sm border border-border relative overflow-hidden group">
      <div className={`absolute top-0 right-0 w-48 sm:w-64 h-48 sm:h-64 ${selectedBiller.color.split(' ')[0]} opacity-10 rounded-full blur-3xl -z-0`} />

      <div className="relative z-10 flex items-center gap-4 sm:gap-6 mb-6 sm:mb-10 pb-6 sm:pb-10 border-b border-border">
       <div className={`w-16 h-16 sm:w-20 sm:h-20 rounded-xl ${selectedBiller.color} flex items-center justify-center shadow-xl transition-transform group-hover:scale-110`}>
        <selectedBiller.icon className="h-8 w-8 sm:h-10 sm:w-10" />
       </div>
       <div>
        <p className="text-[10px] text-gray-400 font-black tracking-widest mb-1">Penyedia Layanan</p>
        <h3 className="text-xl sm:text-2xl font-black text-foreground ">{selectedBiller.name}</h3>
        <p className="text-xs font-bold text-bank-green tracking-widest">Mitra Pembayaran Resmi</p>
       </div>
      </div>

      <div className="space-y-6 sm:space-y-10 relative z-10">
       <div className="group">
        <label className="text-[10px] font-black text-gray-400 tracking-widest ml-1 block mb-3 group-focus-within:text-bank-green transition-colors">ID Pelanggan / Nomor Rekening</label>
        <input
         type="text"
         value={customerId}
         onChange={(e) => setCustomerId(e.target.value)}
         placeholder="Masukkan ID unik Anda"
         className="w-full rounded-xl border-border bg-gray-50 dark:bg-gray-900/50 p-4 sm:p-6 text-base sm:text-xl font-black text-foreground placeholder:text-gray-200 focus:ring-4 focus:ring-bank-green/10 focus:border-bank-green transition-all outline-none "
        />
       </div>

       <div className="group">
        <label className="text-[10px] font-black text-gray-400 tracking-widest ml-1 block mb-3 group-focus-within:text-bank-green transition-colors">Jumlah Pembayaran (IDR)</label>
        <div className="relative">
         <span className="absolute left-4 sm:left-6 top-1/2 -translate-y-1/2 text-xl sm:text-2xl font-black text-gray-300">Rp</span>
         <input
          type="number"
          value={amount}
          onChange={(e) => setAmount(e.target.value)}
          placeholder="0"
          className="w-full rounded-xl border-border bg-gray-50 dark:bg-gray-900/50 p-4 sm:p-6 pl-14 sm:pl-16 text-2xl sm:text-3xl font-black text-foreground placeholder:text-gray-200 focus:ring-4 focus:ring-bank-green/10 focus:border-bank-green transition-all outline-none "
         />
        </div>
       </div>
      </div>
     </div>

     <div className="flex flex-col gap-6">
      <button
       onClick={handlePay}
       disabled={paymentMutation.isPending}
       className="w-full bg-bank-green text-white py-5 sm:py-6 rounded-xl font-black text-xs tracking-[0.2em] hover:bg-bank-emerald transition-all active:scale-[0.98] shadow-2xl shadow-bank-green/20 disabled:bg-bank-green/50"
      >
       {paymentMutation.isPending ? 'Sedang Memproses Pembayaran...' : 'Konfirmasi & Bayar Sekarang'}
      </button>
      <p className="text-center text-[10px] text-gray-400 font-black tracking-widest leading-relaxed">Transaksi aman terenkripsi oleh Infrastruktur Protokol PayU</p>
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
      <h2 className="text-2xl sm:text-3xl font-black text-foreground ">Tagihan & Top-up</h2>
      <p className="text-sm text-gray-500 font-medium">Bayar tagihan utilitas dan top up dompet digital Anda secara instan.</p>
     </div>
     <div className="bg-bank-green/10 px-4 sm:px-5 py-2.5 rounded-full border border-bank-green/20 hidden md:block shadow-sm">
      <p className="text-[10px] font-black text-bank-green tracking-widest animate-pulse">Penyelesaian Real-time 24/7</p>
     </div>
    </div>

    {/* Biller Grid */}
    <div className="bg-card rounded-xl p-6 sm:p-10 shadow-sm border border-border relative overflow-hidden">
     <div className="absolute top-0 right-0 w-48 sm:w-64 h-48 sm:h-64 bg-bank-green/5 rounded-full blur-3xl" />
     <h3 className="text-[10px] font-black text-gray-400 tracking-[0.2em] mb-6 sm:mb-10 text-center">Kategori Layanan</h3>
     <div className="grid grid-cols-3 sm:grid-cols-4 gap-4 sm:gap-10 relative z-10">
      {billers.map((item) => (
       <button
        key={item.name}
        onClick={() => setSelectedBiller(item)}
        className="flex flex-col items-center gap-3 sm:gap-5 transition-all group active:scale-95"
       >
        <div className={`w-14 h-14 sm:w-20 sm:h-20 rounded-xl ${item.color} flex items-center justify-center shadow-lg shadow-black/5 group-hover:scale-110 group-hover:shadow-xl group-hover:border group-hover:border-white/20 transition-all duration-300`}>
         <item.icon className="h-6 w-6 sm:h-8 sm:w-8" />
        </div>
        <span className="text-[9px] sm:text-[10px] font-black text-foreground tracking-widest">{item.name}</span>
       </button>
      ))}
      <button className="flex flex-col items-center gap-3 sm:gap-5 transition-all group active:scale-95">
       <div className="w-14 h-14 sm:w-20 sm:h-20 rounded-xl bg-gray-50 dark:bg-gray-900 shadow-inner flex items-center justify-center text-gray-300 group-hover:text-bank-green transition-colors">
        <Plus className="h-6 w-6 sm:h-8 sm:w-8" />
       </div>
       <span className="text-[9px] sm:text-[10px] font-black text-gray-400 tracking-widest">Lainnya</span>
      </button>
     </div>
    </div>

    {/* Recent Bills */}
    <div className="space-y-6 sm:space-y-8">
     <h3 className="text-lg sm:text-xl font-black text-foreground ">Aktivitas Terakhir</h3>
     {!isLoading && recentBills.length > 0 ? (
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6 sm:gap-8">
       {recentBills.map((bill: PaymentResponse) => (
        <div key={bill.id} className="bg-card p-5 sm:p-8 rounded-xl flex items-center justify-between border border-border hover:shadow-xl transition-all group">
         <div className="flex items-center gap-4 sm:gap-6">
          <div className="w-10 h-10 sm:w-12 sm:h-12 rounded-xl bg-bank-green/10 flex items-center justify-center text-bank-green transition-transform group-hover:scale-110">
           <Zap className="h-5 w-5 sm:h-6 sm:w-6" />
          </div>
          <div>
           <div className="font-black text-foreground text-xs sm:text-sm ">{bill.billerCode}</div>
           <div className="text-[10px] font-bold text-gray-400 tracking-widest mt-1">Ref: {bill.referenceNumber.slice(0, 10)}...</div>
          </div>
         </div>
         <div className="text-right">
          <div className="font-black text-foreground text-sm sm:text-base">Rp {bill.amount.toLocaleString('id-ID')}</div>
          <div className="text-[10px] font-black text-bank-green tracking-widest mt-1">{bill.status}</div>
         </div>
        </div>
       ))}
      </div>
     ) : (
      <div className="bg-card rounded-xl p-10 sm:p-16 text-center border border-border border-dashed border-2 flex flex-col items-center justify-center">
       <LifeBuoy className="h-12 w-12 sm:h-16 sm:w-16 text-gray-100 dark:text-gray-900 mb-4 sm:mb-6" />
       <p className="text-gray-400 font-black tracking-[0.2em] text-[10px] max-w-xs leading-relaxed">Pembayaran tagihan terakhir Anda akan muncul di sini.</p>
      </div>
     )}
    </div>
   </div>
  </DashboardLayout>
 );
}
