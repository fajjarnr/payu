'use client';

import MobileHeader from "@/components/MobileHeader";
import { Plus, Target, Lock, TrendingUp, ChevronRight } from "lucide-react";
import { useQuery } from '@tanstack/react-query';
import { BalanceResponse, Transaction } from '@/types';
import api from '@/lib/api';

export default function PocketsPage() {
  const accountId = localStorage.getItem('accountId') || '';

  const { data: balance, isLoading: balanceLoading } = useQuery({
    queryKey: ['wallet-balance', accountId],
    queryFn: async () => {
      const response = await api.get<BalanceResponse>(`/wallets/${accountId}/balance`);
      return response.data;
    },
    enabled: !!accountId
  });

  const { data: transactions, isLoading: transactionsLoading } = useQuery({
    queryKey: ['wallet-transactions', accountId],
    queryFn: async () => {
      const response = await api.get<Transaction[]>(`/wallets/${accountId}/transactions`);
      return response.data;
    },
    enabled: !!accountId
  });

  const pockets = balance ? [
    {
      id: 'main',
      name: 'Main Pocket',
      balance: balance.balance,
      type: 'MAIN',
      color: 'from-blue-600 to-blue-800'
    }
  ] : [];

  const savingGoals = [
    {
      id: 1,
      name: 'Holiday Trip',
      target: 10000000,
      current: 2500000,
      color: 'bg-orange-100 text-orange-600',
      icon: Target
    },
    {
      id: 2,
      name: 'Emergency Fund',
      target: 50000000,
      current: 50000000,
      color: 'bg-purple-100 text-purple-600',
      icon: Lock,
      interestRate: '4.5% p.a',
      locked: true
    }
  ];

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900 pb-20">
      <MobileHeader title="Pockets" />

      <div className="max-w-md mx-auto p-6 space-y-8">

        {/* Main Pockets */}
        <div className="flex gap-4 overflow-x-auto pb-4 no-scrollbar snap-x">
          {balanceLoading ? (
            <div className="snap-center shrink-0 w-[85%] bg-gray-100 dark:bg-gray-800 rounded-3xl p-6 h-48 animate-pulse"></div>
          ) : pockets.length > 0 ? (
            pockets.map((pocket) => (
              <div key={pocket.id} className={`snap-center shrink-0 w-[85%] bg-gradient-to-br ${pocket.color} rounded-3xl p-6 text-white shadow-lg relative overflow-hidden`}>
                <div className="absolute top-0 right-0 w-32 h-32 bg-white/10 rounded-full -translate-y-1/2 translate-x-1/2 blur-2xl"></div>
                <div className="relative z-10">
                  <div className="text-white/80 text-sm mb-1">Main Pocket</div>
                  <div className="text-3xl font-bold mb-8">Rp {balance?.balance.toLocaleString()}</div>
                  <div className="flex justify-between items-end">
                    <div className="text-xs bg-white/20 px-2 py-1 rounded">Daily</div>
                    <div className="text-xs text-white/80">**** 8829</div>
                  </div>
                </div>
              </div>
            ))
          ) : (
            <div className="snap-center shrink-0 w-[85%] bg-gray-100 dark:bg-gray-800 rounded-3xl p-6 text-center text-gray-500">
              <p>No account found</p>
            </div>
          )}
          <div className="snap-center shrink-0 w-[85%] bg-white dark:bg-gray-800 rounded-3xl p-6 shadow-sm border border-gray-100 dark:border-gray-700 flex flex-col items-center justify-center text-gray-400 gap-2 border-dashed border-2">
            <div className="w-12 h-12 bg-gray-100 dark:bg-gray-700 rounded-full flex items-center justify-center">
              <Plus className="h-6 w-6 text-gray-500" />
            </div>
            <span className="font-medium text-sm">Create Pocket</span>
          </div>
        </div>

        {/* Balance Details */}
        {balance && (
          <div className="bg-white dark:bg-gray-800 rounded-3xl p-5 shadow-sm border border-gray-100 dark:border-gray-700">
            <h4 className="font-bold text-gray-900 dark:text-white mb-4">Balance Details</h4>
            <div className="space-y-3">
              <div className="flex justify-between">
                <span className="text-gray-500 text-sm">Available Balance</span>
                <span className="font-bold text-gray-900 dark:text-white">Rp {balance.availableBalance.toLocaleString()}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-500 text-sm">Reserved Balance</span>
                <span className="font-bold text-gray-900 dark:text-white">Rp {balance.reservedBalance.toLocaleString()}</span>
              </div>
            </div>
          </div>
        )}

        {/* Saving Goals */}
        <div>
          <div className="flex justify-between items-center mb-4">
            <h3 className="text-sm font-bold text-gray-900 dark:text-white">Saving Goals</h3>
            <button className="text-green-600 text-xs font-bold bg-green-50 px-3 py-1 rounded-full">+ New Goal</button>
          </div>

          <div className="space-y-4">
            {savingGoals.map((goal) => {
              const percentage = Math.round((goal.current / goal.target) * 100);
              const Icon = goal.icon;

              return (
                <div key={goal.id} className="bg-white dark:bg-gray-800 p-5 rounded-3xl shadow-sm border border-gray-100 dark:border-gray-700">
                  <div className="flex justify-between items-start mb-4">
                    <div className="flex items-center gap-3">
                      <div className={`w-10 h-10 ${goal.color} rounded-xl flex items-center justify-center`}>
                        <Icon className="h-5 w-5" />
                      </div>
                      <div>
                        <div className="font-bold text-gray-900 dark:text-white">{goal.name}</div>
                        <div className="text-xs text-gray-400">Target: Rp {goal.target.toLocaleString()}</div>
                      </div>
                    </div>
                    {goal.locked ? (
                      <span className="text-xs bg-yellow-100 text-yellow-700 px-2 py-1 rounded font-bold">{goal.interestRate}</span>
                    ) : (
                      <span className="font-bold text-green-600">{percentage}%</span>
                    )}
                  </div>
                  {!goal.locked && (
                    <>
                      <div className="w-full bg-gray-100 dark:bg-gray-700 h-2 rounded-full overflow-hidden">
                        <div className="bg-green-500 h-full rounded-full" style={{ width: `${percentage}%` }}></div>
                      </div>
                      <div className="flex justify-between mt-2 text-xs text-gray-500">
                        <span>Rp {goal.current.toLocaleString()}</span>
                        <span>Left: Rp {(goal.target - goal.current).toLocaleString()}</span>
                      </div>
                    </>
                  )}
                  {goal.locked && (
                    <>
                      <div className="font-bold text-xl text-gray-900 dark:text-white mb-1">Rp {goal.current.toLocaleString()}</div>
                      <div className="text-xs text-gray-400">Fully funded</div>
                    </>
                  )}
                </div>
              );
            })}
          </div>
        </div>

        {/* Recent Transactions */}
        <div className="bg-white dark:bg-gray-800 rounded-3xl p-5 shadow-sm border border-gray-100 dark:border-gray-700">
          <div className="flex justify-between items-center mb-4">
            <h4 className="font-bold text-gray-900 dark:text-white">Recent Transactions</h4>
            {transactions && transactions.length > 0 && (
              <span className="text-xs text-gray-400">Last {transactions.length}</span>
            )}
          </div>

          {transactionsLoading ? (
            <div className="space-y-3">
              {[1, 2, 3].map((i) => (
                <div key={i} className="flex items-center gap-4 animate-pulse">
                  <div className="w-10 h-10 bg-gray-100 dark:bg-gray-700 rounded-full"></div>
                  <div className="flex-1">
                    <div className="h-4 bg-gray-100 dark:bg-gray-700 rounded w-32 mb-2"></div>
                    <div className="h-3 bg-gray-100 dark:bg-gray-700 rounded w-24"></div>
                  </div>
                  <div className="h-4 bg-gray-100 dark:bg-gray-700 rounded w-20"></div>
                </div>
              ))}
            </div>
          ) : transactions && transactions.length > 0 ? (
            <div className="space-y-3">
              {transactions.map((tx) => (
                <div key={tx.id} className="flex items-center justify-between">
                  <div className="flex items-center gap-4">
                    <div className={`w-10 h-10 rounded-full ${tx.type === 'CREDIT' ? 'bg-green-100 text-green-600' : 'bg-red-100 text-red-600'} flex items-center justify-center`}>
                      {tx.type === 'CREDIT' ? '+' : '-'}
                    </div>
                    <div>
                      <p className="font-medium text-gray-900 dark:text-white text-sm">{tx.description}</p>
                      <p className="text-xs text-gray-500">{new Date(tx.createdAt).toLocaleDateString()}</p>
                    </div>
                  </div>
                  <span className={`font-bold text-sm ${tx.type === 'CREDIT' ? 'text-green-600' : 'text-gray-900 dark:text-white'}`}>
                    {tx.type === 'CREDIT' ? '+' : '-'} Rp {tx.amount.toLocaleString()}
                  </span>
                </div>
              ))}
            </div>
          ) : (
            <p className="text-gray-500 text-sm text-center py-4">No transactions yet</p>
          )}
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
