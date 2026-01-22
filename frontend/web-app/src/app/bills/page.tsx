'use client';

import MobileHeader from "@/components/MobileHeader";
import { Smartphone, Zap, Droplets, Wifi, CreditCard, Heart, Tv, Gamepad2, Plus } from "lucide-react";
import { useMutation, useQuery } from '@tanstack/react-query';
import { CreatePaymentRequest, PaymentResponse } from '@/types';
import api from '@/lib/api';
import { useState } from 'react';

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
      <div className="min-h-screen bg-gray-50 dark:bg-gray-900 pb-20">
        <MobileHeader title={`Pay ${selectedBiller.name}`} />

        <div className="max-w-md mx-auto p-6 space-y-6">
          <div className="bg-white dark:bg-gray-800 rounded-3xl p-6 shadow-sm">
            <div className="flex items-center gap-4 mb-6">
              <div className={`w-14 h-14 rounded-2xl ${selectedBiller.color} flex items-center justify-center shadow-sm`}>
                <selectedBiller.icon className="h-6 w-6" />
              </div>
              <div>
                <h3 className="font-bold text-gray-900 dark:text-white text-lg">{selectedBiller.name}</h3>
                <p className="text-gray-500 text-sm">Code: {selectedBiller.code}</p>
              </div>
            </div>

            <div className="space-y-4">
              <div>
                <label className="text-xs font-semibold text-gray-500 ml-1 block mb-2">Customer ID / Phone Number</label>
                <input
                  type="text"
                  value={customerId}
                  onChange={(e) => setCustomerId(e.target.value)}
                  placeholder="Enter ID"
                  className="w-full rounded-2xl border-0 bg-gray-50 dark:bg-gray-700 p-4 text-gray-900 dark:text-white placeholder:text-gray-400 focus:ring-2 focus:ring-green-500 shadow-sm"
                />
              </div>

              <div>
                <label className="text-xs font-semibold text-gray-500 ml-1 block mb-2">Amount (IDR)</label>
                <div className="relative">
                  <span className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400 font-medium">Rp</span>
                  <input
                    type="number"
                    value={amount}
                    onChange={(e) => setAmount(e.target.value)}
                    placeholder="0"
                    className="w-full rounded-2xl border-0 bg-gray-50 dark:bg-gray-700 p-4 pl-12 text-gray-900 dark:text-white placeholder:text-gray-400 focus:ring-2 focus:ring-green-500 shadow-sm font-medium"
                  />
                </div>
              </div>
            </div>
          </div>

          <div className="flex gap-4">
            <button
              onClick={() => setSelectedBiller(null)}
              className="flex-1 bg-gray-100 dark:bg-gray-800 text-gray-700 dark:text-gray-300 py-4 rounded-2xl font-bold hover:bg-gray-200 dark:hover:bg-gray-700 transition-colors"
            >
              Cancel
            </button>
            <button
              onClick={handlePay}
              disabled={paymentMutation.isPending}
              className="flex-1 bg-green-600 text-white py-4 rounded-2xl font-bold hover:bg-green-700 transition-colors shadow-lg shadow-green-200 disabled:bg-green-400"
            >
              {paymentMutation.isPending ? 'Processing...' : 'Pay Now'}
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900 pb-20">
      <MobileHeader title="Pay Bills" />

      <div className="max-w-md mx-auto p-6 space-y-8">

        {/* Biller Grid */}
        <div>
          <h3 className="text-sm font-bold text-gray-900 dark:text-white mb-4">Categories</h3>
          <div className="grid grid-cols-4 gap-4">
            {billers.map((item) => (
              <div key={item.name} onClick={() => setSelectedBiller(item)} className="flex flex-col items-center gap-2 cursor-pointer group">
                <div className={`w-14 h-14 rounded-2xl ${item.color} flex items-center justify-center shadow-sm group-hover:scale-105 transition-transform`}>
                  <item.icon className="h-6 w-6" />
                </div>
                <span className="text-xs font-medium text-gray-600 dark:text-gray-300 text-center">{item.name}</span>
              </div>
            ))}
            <div className="flex flex-col items-center gap-2 cursor-pointer group">
              <div className="w-14 h-14 rounded-2xl bg-gray-100 dark:bg-gray-800 text-gray-400 flex items-center justify-center shadow-sm group-hover:scale-105 transition-transform">
                <Plus className="h-6 w-6" />
              </div>
              <span className="text-xs font-medium text-gray-600 dark:text-gray-300 text-center">More</span>
            </div>
          </div>
        </div>

        {/* Recent Bills */}
        <div>
          <h3 className="text-sm font-bold text-gray-900 dark:text-white mb-4">Recent Payments</h3>
          {!isLoading && recentBills.length > 0 ? (
            <div className="space-y-3">
              {recentBills.map((bill: PaymentResponse) => (
                <div key={bill.id} className="bg-white dark:bg-gray-800 p-4 rounded-2xl flex items-center justify-between shadow-sm">
                  <div className="flex items-center gap-4">
                    <div className="w-10 h-10 rounded-full bg-blue-50 dark:bg-blue-900/20 flex items-center justify-center text-blue-600 dark:text-blue-400">
                      <Zap className="h-5 w-5" />
                    </div>
                    <div>
                      <div className="font-bold text-gray-900 dark:text-white text-sm">{bill.billerCode}</div>
                      <div className="text-xs text-gray-500">{bill.referenceNumber}</div>
                    </div>
                  </div>
                  <div className="text-right">
                    <div className="font-bold text-gray-900 dark:text-white text-sm">Rp {bill.amount.toLocaleString()}</div>
                    <div className="text-xs text-gray-400">{bill.status}</div>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div className="bg-white dark:bg-gray-800 p-8 rounded-2xl text-center">
              <p className="text-gray-500 text-sm">No recent payments</p>
            </div>
          )}
        </div>

      </div>
    </div>
  );
}
