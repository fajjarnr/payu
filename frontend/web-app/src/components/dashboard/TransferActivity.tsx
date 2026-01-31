'use client';

import React from 'react';
import { Search, ChevronDown, MoreHorizontal, RotateCcw, ArrowRight, User } from 'lucide-react';
import { cn } from '@/lib/utils';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Button } from '@/components/ui/button';

interface TransferItem {
  id: string;
  name: string;
  date: string;
  amount: number;
  category: string;
  account: string;
  avatar?: string;
}

interface TransferActivityProps {
  className?: string;
}

export default function TransferActivity({ className = '' }: TransferActivityProps) {
  const transfers: TransferItem[] = [
    { id: '1', name: 'Alex Johnson', date: '22 Jan 2026, 09:30 AM', amount: -7500000, category: 'Transfer ke', account: 'Tabungan (****5678)' },
    { id: '2', name: 'Tagihan Netflix', date: '21 Jan 2026, 03:45 AM', amount: -159000, category: 'Langganan', account: 'Netflix' },
    { id: '3', name: 'John Doe', date: '20 Jan 2026, 11:10 AM', amount: -4500000, category: 'Transfer ke', account: 'Tabungan (****9876)' },
    { id: '4', name: 'Maria Garcia', date: '19 Jan 2026, 07:45 AM', amount: -350000, category: 'Transfer ke', account: 'Tabungan (****4321)' },
  ];

  return (
    <div className={cn("grid grid-cols-1 md:grid-cols-2 lg:grid-cols-12 gap-6 lg:gap-8", className)}>
      {/* Recent Transfer Activity List */}
      <Card className="lg:col-span-8 overflow-hidden">
        <CardHeader className="flex flex-row items-center justify-between pb-8">
          <CardTitle className="text-sm font-black text-foreground tracking-widest uppercase">
            Aktivitas Terakhir
          </CardTitle>
          <div className="flex items-center gap-1.5 text-[10px] font-bold text-muted-foreground bg-muted/50 px-4 py-2 rounded-lg cursor-pointer hover:bg-muted transition-colors border border-transparent hover:border-border tracking-widest uppercase">
            Januari <ChevronDown className="h-3.5 w-3.5" />
          </div>
        </CardHeader>

        <CardContent>
          {/* Desktop Table View */}
          <div className="hidden md:block overflow-x-auto">
            <table className="w-full">
              <thead>
                <tr className="text-left border-b border-border">
                  <th className="pb-4 text-[10px] font-semibold text-muted-foreground tracking-wider uppercase">Tanggal</th>
                  <th className="pb-4 text-[10px] font-semibold text-muted-foreground tracking-wider uppercase">Penerima</th>
                  <th className="pb-4 text-[10px] font-semibold text-muted-foreground tracking-wider uppercase text-center">Rekening</th>
                  <th className="pb-4 text-[10px] font-semibold text-muted-foreground tracking-wider uppercase text-right">Jumlah</th>
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
                          <p className="text-[10px] text-muted-foreground font-semibold tracking-wider leading-none mb-1 uppercase">{item.category}</p>
                          <p className="text-xs font-black text-foreground">{item.name}</p>
                        </div>
                      </div>
                    </td>
                    <td className="py-4 text-center">
                      <p className="text-[10px] font-black text-foreground tracking-wider bg-muted/50 px-2 py-1 rounded-md inline-block uppercase">{item.account}</p>
                    </td>
                    <td className="py-4 text-right">
                      <p className="text-sm font-black text-foreground tabular-nums">Rp {Math.abs(item.amount).toLocaleString('id-ID')}</p>
                    </td>
                    <td className="py-4 text-right pl-4">
                      <Button variant="ghost" size="icon" className="h-8 w-8 text-muted-foreground hover:text-foreground hover:bg-muted">
                        <MoreHorizontal className="h-4 w-4" />
                      </Button>
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
                      <p className="text-xs font-black text-foreground leading-tight">{item.name}</p>
                      <p className="text-[10px] font-semibold text-muted-foreground tracking-widest leading-none uppercase">{item.category}</p>
                    </div>
                  </div>
                  <p className="text-sm font-black text-foreground tabular-nums">Rp {Math.abs(item.amount).toLocaleString('id-ID')}</p>
                </div>
                <div className="flex items-center justify-between pt-3 border-t border-border/50">
                  <p className="text-[10px] font-medium text-muted-foreground">{item.date}</p>
                  <div className="flex items-center gap-2">
                    <span className="text-[10px] font-black text-foreground tracking-wider uppercase">{item.account}</span>
                    <Button variant="ghost" size="icon" className="h-8 w-8 text-muted-foreground">
                      <MoreHorizontal className="h-4 w-4" />
                    </Button>
                  </div>
                </div>
              </div>
            ))}
          </div>

          <div className="mt-8 pt-6 border-t border-border flex flex-col sm:flex-row justify-between items-center gap-4">
            <Button variant="ghost" size="sm" className="flex items-center gap-2 text-[10px] font-bold text-primary hover:text-primary/80 tracking-widest transition-colors uppercase h-auto p-0">
              <RotateCcw className="h-3.5 w-3.5" /> Ulangi Transfer Terakhir
            </Button>
            <Button variant="ghost" size="sm" className="flex items-center gap-2 text-[10px] font-bold text-primary hover:underline tracking-widest transition-all uppercase h-auto p-0">
              Riwayat Lengkap <ArrowRight className="h-3.5 w-3.5" />
            </Button>
          </div>
        </CardContent>
      </Card>

      {/* Quick Transfer Section */}
      <Card className="lg:col-span-4 relative overflow-hidden group min-h-[400px]">
        {/* Decorative background */}
        <div className="absolute top-0 right-0 w-32 h-32 bg-primary/5 rounded-full blur-3xl transition-transform group-hover:scale-125 pointer-events-none" />

        <CardHeader className="flex flex-row items-center justify-between pb-8">
          <CardTitle className="text-sm font-black text-foreground tracking-widest uppercase">
            Kirim Cepat
          </CardTitle>
          <div className="p-2 bg-muted/50 rounded-lg hover:bg-muted transition-colors cursor-pointer border border-transparent hover:border-border">
            <Search className="h-4 w-4 text-muted-foreground" />
          </div>
        </CardHeader>

        <CardContent className="space-y-12 relative z-10 flex flex-col justify-between h-[calc(100%-100px)]">
          <div>
            <p className="text-[10px] font-black text-muted-foreground tracking-[0.2em] mb-6 text-center uppercase">Kategori Favorit</p>
            <div className="grid grid-cols-4 gap-4">
              {[
                { icon: '🏦', label: 'Bank' },
                { icon: '📱', label: 'E-Wallet' },
                { icon: '📄', label: 'Tagihan' },
                { icon: '•••', label: 'Lain' },
              ].map((item, i) => (
                <Button key={i} variant="ghost" className="flex flex-col items-center gap-2.5 group/btn h-auto p-0 hover:bg-transparent">
                  <div className="h-10 w-10 sm:h-12 sm:w-12 rounded-xl bg-muted/30 flex items-center justify-center text-xl group-hover/btn:bg-accent group-hover/btn:scale-105 transition-all border border-transparent group-hover/btn:border-primary/20 shadow-sm overflow-hidden">
                    {item.icon}
                  </div>
                  <span className="text-[10px] font-black text-muted-foreground group-hover/btn:text-primary uppercase tracking-tighter">{item.label}</span>
                </Button>
              ))}
            </div>
          </div>

          <div>
            <p className="text-[10px] font-black text-muted-foreground tracking-[0.2em] mb-6 text-center uppercase">Kontak Terbaru</p>
            <div className="flex justify-between items-center px-1">
              {[1, 2, 3, 4, 5].map((i) => (
                <div key={i} className="h-10 w-10 rounded-xl bg-accent border-2 border-background shadow-sm flex items-center justify-center overflow-hidden cursor-pointer hover:scale-110 active:scale-95 transition-all hover:ring-2 ring-primary group/avatar">
                  <User className="h-5 w-5 text-primary/60 group-hover/avatar:text-primary transition-colors" />
                </div>
              ))}
            </div>
          </div>

          <Button className="w-full uppercase tracking-widest font-black text-xs h-14 rounded-2xl">
            Kirim Sekarang
          </Button>
        </CardContent>
      </Card>
    </div>
  );
}
