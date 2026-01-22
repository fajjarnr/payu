'use client';

import MobileHeader from "@/components/MobileHeader";
import { Plus, Target, Lock, TrendingUp } from "lucide-react";

export default function PocketsPage() {
  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900 pb-20">
      <MobileHeader title="Pockets" />
      
      <div className="max-w-md mx-auto p-6 space-y-8">
        
        {/* Main Pockets */}
        <div className="flex gap-4 overflow-x-auto pb-4 no-scrollbar snap-x">
           <div className="snap-center shrink-0 w-[85%] bg-gradient-to-br from-blue-600 to-blue-800 rounded-3xl p-6 text-white shadow-lg relative overflow-hidden">
               <div className="absolute top-0 right-0 w-32 h-32 bg-white/10 rounded-full -translate-y-1/2 translate-x-1/2 blur-2xl"></div>
               <div className="relative z-10">
                  <div className="text-blue-200 text-sm mb-1">Main Pocket</div>
                  <div className="text-3xl font-bold mb-8">Rp 15.000.000</div>
                  <div className="flex justify-between items-end">
                     <div className="text-xs bg-white/20 px-2 py-1 rounded">Daily</div>
                     <div className="text-xs text-blue-200">**** 8829</div>
                  </div>
               </div>
           </div>

           <div className="snap-center shrink-0 w-[85%] bg-white dark:bg-gray-800 rounded-3xl p-6 shadow-sm border border-gray-100 dark:border-gray-700 flex flex-col items-center justify-center text-gray-400 gap-2 border-dashed border-2">
               <div className="w-12 h-12 bg-gray-100 dark:bg-gray-700 rounded-full flex items-center justify-center">
                  <Plus className="h-6 w-6 text-gray-500" />
               </div>
               <span className="font-medium text-sm">Create Pocket</span>
           </div>
        </div>

        {/* Saving Goals */}
        <div>
           <div className="flex justify-between items-center mb-4">
               <h3 className="text-sm font-bold text-gray-900 dark:text-white">Saving Goals</h3>
               <button className="text-green-600 text-xs font-bold bg-green-50 px-3 py-1 rounded-full">+ New Goal</button>
           </div>
           
           <div className="space-y-4">
               <div className="bg-white dark:bg-gray-800 p-5 rounded-3xl shadow-sm border border-gray-100 dark:border-gray-700">
                  <div className="flex justify-between items-start mb-4">
                     <div className="flex items-center gap-3">
                        <div className="w-10 h-10 bg-orange-100 text-orange-600 rounded-xl flex items-center justify-center">
                           <Target className="h-5 w-5" />
                        </div>
                        <div>
                           <div className="font-bold text-gray-900 dark:text-white">Holiday Trip</div>
                           <div className="text-xs text-gray-400">Target: Rp 10.000.000</div>
                        </div>
                     </div>
                     <span className="font-bold text-green-600">25%</span>
                  </div>
                  <div className="w-full bg-gray-100 dark:bg-gray-700 h-2 rounded-full overflow-hidden">
                     <div className="bg-green-500 h-full rounded-full" style={{ width: '25%' }}></div>
                  </div>
                  <div className="flex justify-between mt-2 text-xs text-gray-500">
                     <span>Rp 2.500.000</span>
                     <span>Left: Rp 7.500.000</span>
                  </div>
               </div>

               <div className="bg-white dark:bg-gray-800 p-5 rounded-3xl shadow-sm border border-gray-100 dark:border-gray-700">
                  <div className="flex justify-between items-start mb-4">
                     <div className="flex items-center gap-3">
                        <div className="w-10 h-10 bg-purple-100 text-purple-600 rounded-xl flex items-center justify-center">
                           <Lock className="h-5 w-5" />
                        </div>
                        <div>
                           <div className="font-bold text-gray-900 dark:text-white">Emergency Fund</div>
                           <div className="text-xs text-gray-400">Locked Deposit</div>
                        </div>
                     </div>
                     <span className="text-xs bg-yellow-100 text-yellow-700 px-2 py-1 rounded font-bold">4.5% p.a</span>
                  </div>
                  <div className="font-bold text-xl text-gray-900 dark:text-white mb-1">Rp 50.000.000</div>
                  <div className="text-xs text-gray-400">Matures in 3 months</div>
               </div>
           </div>
        </div>

        {/* Investment Teaser */}
        <div className="bg-gradient-to-r from-gray-900 to-gray-800 rounded-3xl p-5 text-white flex items-center justify-between">
            <div className="flex items-center gap-4">
               <div className="w-10 h-10 bg-white/10 rounded-xl flex items-center justify-center">
                  <TrendingUp className="h-5 w-5 text-green-400" />
               </div>
               <div>
                  <div className="font-bold">Start Investing</div>
                  <div className="text-xs text-gray-400">Coming soon</div>
               </div>
            </div>
            <ChevronRight className="h-5 w-5 text-gray-400" />
        </div>

      </div>
    </div>
  );
}

function ChevronRight({ className }: { className?: string }) {
   return (
      <svg className={className} xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
         <polyline points="9 18 15 12 9 6"></polyline>
      </svg>
   );
}
