'use client';

import { Smartphone, Zap, Droplets, Wifi, CreditCard, Heart, Tv, Gamepad2, Plus, ChevronRight, LifeBuoy } from "lucide-react";
import { useMutation, useQuery } from '@tanstack/react-query';
import { CreatePaymentRequest, PaymentResponse } from '@/types';
import api from '@/lib/api';
import { useState } from 'react';
import DashboardLayout from "@/components/DashboardLayout";
import clsx from 'clsx';

export default function BillsPage() {
  const [selectedBiller, setSelectedBiller] = useState<{ name: string; icon: React.ComponentType<{ className?: string }>; color: string; code: string } | null>(null);
  const [customerId, setCustomerId] = useState('');
  const [amount, setAmount] = useState('');

  const billers = [
    { name: 'Pulsa', icon: Smartphone, color: 'bg-blue-100 text-blue-600', code: 'PULSA' },
    { name: 'Listrik', icon: Zap, color: 'bg-yellow-100 text-yellow-600', code: 'PLN' },
    { name: 'PDAM', icon: Droplets, color: 'bg-cyan-100 text-cyan-600', code: 'PDAM' },
    { name: 'Internet', icon: Wifi, color: 'bg-indigo-100 text-indigo-600', code: 'INTERNET' },
    { name: 'Cards', icon: CreditCard, color: 'bg-purple-100 text-purple-600', code: 'CARDS' },
    { name: 'BPJS', icon: Heart, color: 'bg-green-100 text-green-600', code: 'BPJS' },
    { name: 'Cable TV', icon: Tv, color: 'bg-pink-100 text-pink-600', code: 'TV' },
    { name: 'Voucher', icon: Gamepad2, color: 'bg-orange-100 text-orange-600', code: 'VOUCHER' },
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
      alert('Payment successful!');
      setSelectedBiller(null);
      setCustomerId('');
      setAmount('');
    },
    onError: (error) => {
      console.error('Payment failed:', error);
      alert('Payment failed. Please try again.');
    }
  });

  const handlePay = () => {
    if (!selectedBiller || !customerId || !amount) {
      alert('Please fill in all fields');
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
        <div className="max-w-2xl mx-auto space-y-8 pb-10">
          <div className="flex items-center gap-4">
            <button
              onClick={() => setSelectedBiller(null)}
              className="p-2 hover:bg-gray-100 dark:hover:bg-gray-800 rounded-xl transition-colors"
            >
              <ChevronRight className="h-6 w-6 rotate-180" />
            </button>
            <h2 className="text-3xl font-black text-foreground">Pay {selectedBiller.name}</h2>
          </div>

          <div className="bg-card rounded-[2.5rem] p-10 shadow-sm border border-border relative overflow-hidden">
            <div className={`absolute top-0 right-0 w-64 h-64 ${selectedBiller.color.split(' ')[0]} opacity-10 rounded-full blur-3xl -z-0`} />

            <div className="relative z-10 flex items-center gap-6 mb-10 pb-10 border-b border-border">
              <div className={`w-20 h-20 rounded-3xl ${selectedBiller.color} flex items-center justify-center shadow-xl`}>
                <selectedBiller.icon className="h-10 w-10" />
              </div>
              <div>
                <p className="text-[10px] text-gray-400 font-black uppercase tracking-widest mb-1">Service Provider</p>
                <h3 className="text-2xl font-black text-foreground">{selectedBiller.name}</h3>
                <p className="text-xs font-bold text-bank-green">Official Payment Partner</p>
              </div>
            </div>

            <div className="space-y-8 relative z-10">
              <div className="group">
                <label className="text-[10px] font-black text-gray-400 uppercase tracking-widest ml-1 block mb-3 group-focus-within:text-bank-green transition-colors">Customer ID / Account Number</label>
                <input
                  type="text"
                  value={customerId}
                  onChange={(e) => setCustomerId(e.target.value)}
                  placeholder="Enter your unique ID"
                  className="w-full rounded-2xl border-border bg-gray-50 dark:bg-gray-900/50 p-6 text-xl font-black text-foreground placeholder:text-gray-200 focus:ring-4 focus:ring-bank-green/10 focus:border-bank-green transition-all outline-none"
                />
              </div>

              <div className="group">
                <label className="text-[10px] font-black text-gray-400 uppercase tracking-widest ml-1 block mb-3 group-focus-within:text-bank-green transition-colors">Payment Amount (IDR)</label>
                <div className="relative">
                  <span className="absolute left-6 top-1/2 -translate-y-1/2 text-2xl font-black text-gray-300">Rp</span>
                  <input
                    type="number"
                    value={amount}
                    onChange={(e) => setAmount(e.target.value)}
                    placeholder="0"
                    className="w-full rounded-2xl border-border bg-gray-50 dark:bg-gray-900/50 p-6 pl-16 text-3xl font-black text-foreground placeholder:text-gray-200 focus:ring-4 focus:ring-bank-green/10 focus:border-bank-green transition-all outline-none"
                  />
                </div>
              </div>
            </div>
          </div>

          <div className="flex flex-col gap-4">
            <button
              onClick={handlePay}
              disabled={paymentMutation.isPending}
              className="w-full bg-bank-green text-white py-6 rounded-3xl font-black text-xl hover:bg-bank-emerald transition-all active:scale-[0.98] shadow-2xl shadow-bank-green/20 disabled:bg-bank-green/50"
            >
              {paymentMutation.isPending ? 'Processing Payment...' : 'Confirm & Pay Now'}
            </button>
            <p className="text-center text-[10px] text-gray-400 font-bold uppercase tracking-widest">Secure encrypted transaction by PayU Infrastructure</p>
          </div>
        </div>
      </DashboardLayout>
    );
  }

  return (
    <DashboardLayout>
      <div className="max-w-4xl mx-auto space-y-10 pb-10">
        <div className="flex justify-between items-end">
          <div>
            <h2 className="text-3xl font-black text-foreground uppercase tracking-tight">Bills & Top-up</h2>
            <p className="text-sm text-gray-500 font-medium">Pay your utility bills and top up your digital wallets instantly.</p>
          </div>
          <div className="bg-bank-green/10 px-4 py-2 rounded-full border border-bank-green/20 hidden md:block">
            <p className="text-[10px] font-black text-bank-green uppercase tracking-widest">24/7 Real-time Settlement</p>
          </div>
        </div>

        {/* Biller Grid */}
        <div className="bg-card rounded-[2.5rem] p-10 shadow-sm border border-border">
          <h3 className="text-sm font-black text-gray-400 uppercase tracking-widest mb-8">Service Categories</h3>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-8">
            {billers.map((item) => (
              <button
                key={item.name}
                onClick={() => setSelectedBiller(item)}
                className="flex flex-col items-center gap-4 transition-all group active:scale-95"
              >
                <div className={`w-20 h-20 rounded-[1.5rem] ${item.color} flex items-center justify-center shadow-lg shadow-black/5 group-hover:scale-110 group-hover:shadow-xl transition-all`}>
                  <item.icon className="h-8 w-8" />
                </div>
                <span className="text-xs font-black text-foreground uppercase tracking-widest">{item.name}</span>
              </button>
            ))}
            <button className="flex flex-col items-center gap-4 transition-all group active:scale-95">
              <div className="w-20 h-20 rounded-[1.5rem] bg-gray-50 dark:bg-gray-900 shadow-inner flex items-center justify-center text-gray-300 group-hover:text-bank-green transition-colors">
                <Plus className="h-8 w-8" />
              </div>
              <span className="text-xs font-bold text-gray-400 uppercase tracking-widest">More</span>
            </button>
          </div>
        </div>

        {/* Recent Bills */}
        <div className="space-y-6">
          <h3 className="text-xl font-black text-foreground uppercase tracking-tight">Recent Activity</h3>
          {!isLoading && recentBills.length > 0 ? (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              {recentBills.map((bill: PaymentResponse) => (
                <div key={bill.id} className="bg-card p-6 rounded-3xl flex items-center justify-between border border-border hover:shadow-lg transition-all">
                  <div className="flex items-center gap-5">
                    <div className="w-12 h-12 rounded-2xl bg-bank-green/10 flex items-center justify-center text-bank-green transition-colors">
                      <Zap className="h-6 w-6" />
                    </div>
                    <div>
                      <div className="font-black text-foreground text-sm uppercase">{bill.billerCode}</div>
                      <div className="text-[10px] font-bold text-gray-400 uppercase tracking-widest">Ref: {bill.referenceNumber.slice(0, 10)}...</div>
                    </div>
                  </div>
                  <div className="text-right">
                    <div className="font-black text-foreground">Rp {bill.amount.toLocaleString()}</div>
                    <div className="text-[10px] font-bold text-bank-green uppercase tracking-widest">{bill.status}</div>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div className="bg-card rounded-[2rem] p-12 text-center border border-border border-dashed border-2">
              <LifeBuoy className="h-12 w-12 text-gray-100 dark:text-gray-800 mx-auto mb-4" />
              <p className="text-gray-400 font-bold uppercase tracking-widest text-[10px]">Your recent bill payments will appear here</p>
            </div>
          )}
        </div>
      </div>
    </DashboardLayout>
  );
}
