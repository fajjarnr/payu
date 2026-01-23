'use client';

import React from 'react';
import { Search, ChevronDown, MoreHorizontal, RotateCcw, ArrowRight, User } from 'lucide-react';

interface TransferItem {
  id: string;
  name: string;
  date: string;
  amount: number;
  category: string;
  account: string;
  avatar?: string;
}

export default function TransferActivity() {
  const transfers: TransferItem[] = [
    { id: '1', name: 'Alex Johnson', date: '22 Jan 2026, 09:30 AM', amount: -7500000, category: 'Transfer ke', account: 'Tabungan (****5678)' },
    { id: '2', name: 'Tagihan Netflix', date: '21 Jan 2026, 03:45 AM', amount: -159000, category: 'Langganan', account: 'Netflix' },
    { id: '3', name: 'John Doe', date: '20 Jan 2026, 11:10 AM', amount: -4500000, category: 'Transfer ke', account: 'Tabungan (****9876)' },
    { id: '4', name: 'Maria Garcia', date: '19 Jan 2026, 07:45 AM', amount: -350000, category: 'Transfer ke', account: 'Tabungan (****4321)' },
  ];

  return (
    <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 mt-12">
      {/* Recent Transfer Activity List */}
      <div className="lg:col-span-8 bg-card p-8 rounded-xl border border-border shadow-card">
        <div className="flex justify-between items-center mb-10">
          <h3 className="text-lg font-bold text-foreground ">Aktivitas Terakhir</h3>
          <div className="flex items-center gap-1.5 text-[10px] font-bold text-muted-foreground bg-muted/50 px-4 py-2 rounded-lg cursor-pointer hover:bg-muted transition-colors border border-transparent hover:border-border tracking-widest">
            Januari <ChevronDown className="h-3.5 w-3.5" />
          </div>
        </div>

        {/* Desktop Table View */}
        <div className="hidden md:block overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="text-left border-b border-border">
                <th className="pb-4 text-[10px] font-semibold text-muted-foreground tracking-wider">Tanggal</th>
                <th className="pb-4 text-[10px] font-semibold text-muted-foreground tracking-wider">Penerima</th>
                <th className="pb-4 text-[10px] font-semibold text-muted-foreground tracking-wider text-center">Rekening</th>
                <th className="pb-4 text-[10px] font-semibold text-muted-foreground tracking-wider text-right">Jumlah</th>
                <th className="pb-4"></th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border">
              {transfers.map((item) => (
                <tr key={item.id} className="group hover:bg-muted/30 transition-colors">
                  <td className="py-4 whitespace-nowrap text-xs text-muted-foreground font-medium">{item.date}</td>
                  <td className="py-4">
                    <div className="flex items-center gap-3">
                      <div className="h-10 w-10 rounded-xl bg-accent flex items-center justify-center border border-border group-hover:scale-105 transition-transform">
                        <User className="h-5 w-5 text-primary" />
                      </div>
                      <div>
                        <p className="text-[10px] text-muted-foreground font-semibold tracking-wider leading-none mb-1">{item.category}</p>
                        <p className="text-xs font-bold text-foreground">{item.name}</p>
                      </div>
                    </div>
                  </td>
                  <td className="py-4 text-center">
                    <p className="text-[10px] font-bold text-foreground tracking-wider bg-muted/50 px-2 py-1 rounded-md inline-block">{item.account}</p>
                  </td>
                  <td className="py-4 text-right">
                    <p className="text-sm font-bold text-foreground">Rp {Math.abs(item.amount).toLocaleString('id-ID')}</p>
                  </td>
                  <td className="py-4 text-right pl-4">
                    <button className="p-2 text-muted-foreground hover:text-foreground hover:bg-muted rounded-lg transition-colors">
                      <MoreHorizontal className="h-4 w-4" />
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {/* Mobile Card Layout */}
        <div className="md:hidden space-y-4">
          {transfers.map((item) => (
            <div key={item.id} className="bg-muted/30 p-4 rounded-xl border border-transparent hover:border-primary/20 transition-all">
              <div className="flex items-center justify-between mb-3">
                <div className="flex items-center gap-3">
                  <div className="h-10 w-10 rounded-xl bg-card flex items-center justify-center border border-border shadow-sm">
                    <User className="h-5 w-5 text-primary" />
                  </div>
                  <div>
                    <p className="text-xs font-bold text-foreground leading-tight">{item.name}</p>
                    <p className="text-[10px] font-semibold text-muted-foreground tracking-widest leading-none">{item.category}</p>
                  </div>
                </div>
                <p className="text-sm font-bold text-foreground">Rp {Math.abs(item.amount).toLocaleString('id-ID')}</p>
              </div>
              <div className="flex items-center justify-between pt-3 border-t border-border/50">
                <p className="text-[10px] font-medium text-muted-foreground">{item.date}</p>
                <div className="flex items-center gap-2">
                  <span className="text-[10px] font-bold text-foreground tracking-wider">{item.account}</span>
                  <button className="p-1.5 text-muted-foreground">
                    <MoreHorizontal className="h-4 w-4" />
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>

        <div className="mt-8 pt-6 border-t border-border flex flex-col sm:flex-row justify-between items-center gap-4">
          <button className="flex items-center gap-2 text-[10px] font-bold text-primary hover:text-primary/80 tracking-widest transition-colors">
            <RotateCcw className="h-3.5 w-3.5" /> Ulangi Transfer Terakhir
          </button>
          <button className="flex items-center gap-2 text-[10px] font-bold text-primary hover:underline tracking-widest transition-all">
            Riwayat Lengkap <ArrowRight className="h-3.5 w-3.5" />
          </button>
        </div>
      </div>

      {/* Quick Transfer Section */}
      <div className="lg:col-span-4 flex flex-col gap-6">
        <div className="bg-card p-8 rounded-xl border border-border shadow-card flex-1 relative overflow-hidden group">
          <div className="absolute top-0 right-0 w-32 h-32 bg-primary/5 rounded-full blur-3xl transition-transform group-hover:scale-125" />

          <div className="flex justify-between items-center mb-8">
            <h3 className="text-lg font-bold text-foreground ">Kirim Cepat</h3>
            <div className="p-2 bg-muted/50 rounded-lg hover:bg-muted transition-colors cursor-pointer border border-transparent hover:border-border">
              <Search className="h-4 w-4 text-muted-foreground" />
            </div>
          </div>

          <div className="space-y-12 relative z-10">
            <div>
              <p className="text-[10px] font-bold text-muted-foreground tracking-[0.2em] mb-6 text-center">Kategori Favorit</p>
              <div className="grid grid-cols-4 gap-4">
                {[
                  { icon: '🏦', label: 'Bank' },
                  { icon: '📱', label: 'E-Wallet' },
                  { icon: '📄', label: 'Tagihan' },
                  { icon: '•••', label: 'Lain' },
                ].map((item, i) => (
                  <button key={i} className="flex flex-col items-center gap-2.5 group/btn">
                    <div className="h-12 w-12 rounded-xl bg-muted/50 flex items-center justify-center text-xl group-hover/btn:bg-accent group-hover/btn:scale-105 transition-all border border-transparent group-hover/btn:border-primary/20 shadow-sm">
                      {item.icon}
                    </div>
                    <span className="text-[10px] font-bold text-muted-foreground group-hover/btn:text-primary ">{item.label}</span>
                  </button>
                ))}
              </div>
            </div>

            <div>
              <p className="text-[10px] font-bold text-muted-foreground tracking-[0.2em] mb-6 text-center">Kontak Terbaru</p>
              <div className="flex justify-between items-center px-1">
                {[1, 2, 3, 4, 5].map((i) => (
                  <div key={i} className="h-10 w-10 rounded-xl bg-accent border-2 border-card shadow-sm flex items-center justify-center overflow-hidden cursor-pointer hover:scale-110 active:scale-95 transition-all hover:ring-2 ring-primary group/avatar">
                    <User className="h-5 w-5 text-primary/60 group-hover/avatar:text-primary transition-colors" />
                  </div>
                ))}
              </div>
            </div>

            <button className="w-full bg-primary text-primary-foreground py-4 rounded-xl font-bold text-xs tracking-widest shadow-lg shadow-primary/20 hover:shadow-primary/30 hover:bg-bank-emerald transition-all active:scale-[0.98]">
              Kirim Sekarang
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
