'use client';

import MobileHeader from "@/components/MobileHeader";
import { Smartphone, Zap, Droplets, Wifi, CreditCard, Heart, Tv, Gamepad2, Plus } from "lucide-react";

export default function BillsPage() {
  const billers = [
    { name: 'Pulsa', icon: Smartphone, color: 'bg-blue-100 text-blue-600' },
    { name: 'Listrik', icon: Zap, color: 'bg-yellow-100 text-yellow-600' },
    { name: 'PDAM', icon: Droplets, color: 'bg-cyan-100 text-cyan-600' },
    { name: 'Internet', icon: Wifi, color: 'bg-indigo-100 text-indigo-600' },
    { name: 'Cards', icon: CreditCard, color: 'bg-purple-100 text-purple-600' },
    { name: 'BPJS', icon: Heart, color: 'bg-green-100 text-green-600' },
    { name: 'Cable TV', icon: Tv, color: 'bg-pink-100 text-pink-600' },
    { name: 'Voucher', icon: Gamepad2, color: 'bg-orange-100 text-orange-600' },
  ];

  const recentBills = [
    { name: 'PLN Token', id: '123456789', amount: 'Rp 100.000', date: 'Yesterday' },
    { name: 'Telkomsel Data', id: '08123456789', amount: 'Rp 150.000', date: '2 days ago' },
  ];

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900 pb-20">
      <MobileHeader title="Pay Bills" />
      
      <div className="max-w-md mx-auto p-6 space-y-8">
        
        {/* Biller Grid */}
        <div>
           <h3 className="text-sm font-bold text-gray-900 dark:text-white mb-4">Categories</h3>
           <div className="grid grid-cols-4 gap-4">
              {billers.map((item) => (
                 <div key={item.name} className="flex flex-col items-center gap-2 cursor-pointer group">
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
           <div className="space-y-3">
              {recentBills.map((bill) => (
                 <div key={bill.name + bill.date} className="bg-white dark:bg-gray-800 p-4 rounded-2xl flex items-center justify-between shadow-sm">
                    <div className="flex items-center gap-4">
                       <div className="w-10 h-10 rounded-full bg-blue-50 dark:bg-blue-900/20 flex items-center justify-center text-blue-600 dark:text-blue-400">
                          <Zap className="h-5 w-5" />
                       </div>
                       <div>
                          <div className="font-bold text-gray-900 dark:text-white text-sm">{bill.name}</div>
                          <div className="text-xs text-gray-500">{bill.id}</div>
                       </div>
                    </div>
                    <div className="text-right">
                       <div className="font-bold text-gray-900 dark:text-white text-sm">{bill.amount}</div>
                       <div className="text-xs text-gray-400">{bill.date}</div>
                    </div>
                 </div>
              ))}
           </div>
        </div>

      </div>
    </div>
  );
}
