'use client';

import MobileHeader from "@/components/MobileHeader";
import { Search, ChevronRight } from "lucide-react";

export default function TransferPage() {
  const recentContacts = [
    { name: 'Anya', initial: 'A', color: 'bg-purple-100 text-purple-600' },
    { name: 'Budi', initial: 'B', color: 'bg-blue-100 text-blue-600' },
    { name: 'Citra', initial: 'C', color: 'bg-pink-100 text-pink-600' },
    { name: 'Dodi', initial: 'D', color: 'bg-yellow-100 text-yellow-600' },
  ];

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900">
      <MobileHeader title="Transfer" />
      
      <div className="max-w-md mx-auto p-6 space-y-8">
        
        {/* Search */}
        <div className="relative">
           <Search className="absolute left-4 top-1/2 -translate-y-1/2 h-5 w-5 text-gray-400" />
           <input 
             type="text" 
             placeholder="Search name or ID"
             className="w-full pl-12 pr-4 py-4 rounded-2xl border-0 bg-white dark:bg-gray-800 shadow-sm focus:ring-2 focus:ring-green-500 transition-all text-sm font-medium"
           />
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
                 <div key={c.name} className="flex flex-col items-center gap-2 min-w-[64px] cursor-pointer group">
                    <div className={`w-14 h-14 rounded-full ${c.color} flex items-center justify-center font-bold text-lg shadow-sm group-hover:scale-105 transition-transform`}>
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
                type="number" 
                placeholder="0"
                className="w-full bg-transparent border-0 p-0 focus:ring-0 placeholder:text-gray-200 outline-none"
              />
           </div>
           <div className="h-[1px] w-full bg-gray-100 dark:bg-gray-700 my-4"></div>
           <input 
             type="text" 
             placeholder="Add a note (optional)"
             className="w-full text-sm font-medium bg-transparent border-0 p-0 focus:ring-0 placeholder:text-gray-400 outline-none"
           />
        </div>

        <button className="w-full bg-black dark:bg-white text-white dark:text-black py-4 rounded-2xl font-bold text-lg shadow-xl shadow-gray-200 dark:shadow-none hover:scale-[1.02] transition-transform flex items-center justify-center gap-2">
           Review Transfer <ChevronRight className="h-5 w-5" />
        </button>

      </div>
    </div>
  );
}
