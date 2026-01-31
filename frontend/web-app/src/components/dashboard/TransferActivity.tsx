'use client';

import React from 'react';
import { Search, ChevronDown, MoreHorizontal, RotateCcw, ArrowRight, User, Landmark, Smartphone, ReceiptText, MoreHorizontal as MoreIcon } from 'lucide-react';
import { cn } from '@/lib/utils';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';

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
      {/* Quick Transfer Section - Now on the Left */}
      <Card className="lg:col-span-4 relative overflow-hidden group min-h-[400px]">
        {/* Decorative background */}
        <div className="absolute top-0 right-0 w-32 h-32 bg-primary/5 rounded-full blur-3xl transition-transform group-hover:scale-125 pointer-events-none" />

        <CardHeader className="flex flex-row items-center justify-between pb-10">
          <CardTitle className="text-base sm:text-lg font-bold text-foreground tracking-widest uppercase">
            Kirim Cepat
          </CardTitle>
          <div className="p-3 bg-muted/50 rounded-xl hover:bg-muted transition-colors cursor-pointer border border-transparent hover:border-border shadow-sm">
            <Search className="h-5 w-5 text-muted-foreground" />
          </div>
        </CardHeader>

        <CardContent className="space-y-12 py-6 relative z-10 flex flex-col justify-between h-full">
          <div>
            <p className="text-xs sm:text-xs font-bold text-muted-foreground tracking-[0.25em] mb-8 text-center uppercase opacity-60">Kategori Favorit</p>
            <div className="grid grid-cols-4 gap-4 sm:gap-6 px-1">
              {[
                { icon: Landmark, label: 'Bank' },
                { icon: Smartphone, label: 'E-Wallet' },
                { icon: ReceiptText, label: 'Tagihan' },
                { icon: MoreIcon, label: 'Lain' },
              ].map((item, i) => (
                <Button key={i} variant="ghost" className="flex flex-col items-center gap-4 group/btn h-auto p-0 hover:bg-transparent">
                  <div className="h-14 w-14 sm:h-16 sm:w-16 rounded-2xl bg-emerald-500/[0.03] flex items-center justify-center group-hover/btn:bg-emerald-500/10 group-hover/btn:scale-105 transition-all border border-emerald-500/10 group-hover/btn:border-emerald-500/30 shadow-sm overflow-hidden">
                    <item.icon className="h-6 w-6 sm:h-7 sm:w-7 text-emerald-500" />
                  </div>
                  <span className="text-xs sm:text-xs font-bold text-muted-foreground group-hover/btn:text-primary uppercase tracking-tighter">{item.label}</span>
                </Button>
              ))}
            </div>
          </div>

          <div>
            <p className="text-xs sm:text-xs font-bold text-muted-foreground tracking-[0.25em] mb-8 text-center uppercase opacity-60">Kontak Terbaru</p>
            <div className="flex justify-between items-center px-2">
              {[1, 2, 3, 4, 5].map((i) => (
                <div key={i} className="h-12 w-12 sm:h-14 sm:w-14 rounded-2xl bg-accent border-2 border-background shadow-md flex items-center justify-center overflow-hidden cursor-pointer hover:scale-110 active:scale-95 transition-all hover:ring-2 ring-primary/30 group/avatar">
                  <User className="h-6 w-6 sm:h-7 sm:w-7 text-primary/60 group-hover/avatar:text-primary transition-colors" />
                </div>
              ))}
            </div>
          </div>

          <Button className="w-full uppercase tracking-[0.2em] font-bold text-xs h-14 sm:h-16 rounded-2xl shadow-xl shadow-primary/10 hover:shadow-primary/20 transition-all mt-4">
            Kirim Sekarang
          </Button>
        </CardContent>
      </Card>

      {/* Recent Transfer Activity List - Now on the Right */}
      <Card className="lg:col-span-8 overflow-hidden">
        <CardHeader className="flex flex-row items-center justify-between pb-10">
          <CardTitle className="text-base sm:text-lg font-bold text-foreground tracking-widest uppercase">
            Aktivitas Terakhir
          </CardTitle>
          <div className="flex items-center gap-2 text-xs font-bold text-muted-foreground bg-muted/50 px-5 py-2.5 rounded-xl cursor-pointer hover:bg-muted transition-colors border border-transparent hover:border-border tracking-widest uppercase shadow-sm">
            Januari <ChevronDown className="h-4 w-4" />
          </div>
        </CardHeader>

        <CardContent>
          <div className="hidden md:block">
            <Table>
              <TableHeader>
                <TableRow className="border-b border-border/50 hover:bg-transparent">
                  <TableHead className="h-14 text-xs sm:text-xs font-bold text-muted-foreground tracking-[0.2em] uppercase">Tanggal</TableHead>
                  <TableHead className="h-14 text-xs sm:text-xs font-bold text-muted-foreground tracking-[0.2em] uppercase">Penerima</TableHead>
                  <TableHead className="h-14 text-xs sm:text-xs font-bold text-muted-foreground tracking-[0.2em] uppercase text-center">Rekening</TableHead>
                  <TableHead className="h-14 text-xs sm:text-xs font-bold text-muted-foreground tracking-[0.2em] uppercase text-right">Jumlah</TableHead>
                  <TableHead className="h-12 w-[50px]"></TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {transfers.map((item) => (
                  <TableRow key={item.id} className="group border-b border-border/30 hover:bg-muted/30 transition-colors">
                    <TableCell className="py-8 sm:py-10 whitespace-nowrap text-xs sm:text-xs text-muted-foreground font-bold tabular-nums uppercase tracking-tighter opacity-70">
                      {item.date}
                    </TableCell>
                    <TableCell className="py-8 sm:py-10">
                      <div className="flex items-center gap-4">
                        <div className="h-12 w-12 rounded-xl bg-accent flex items-center justify-center border border-border group-hover:scale-110 transition-transform shadow-sm">
                          <User className="h-6 w-6 text-primary" />
                        </div>
                        <div>
                          <p className="text-xs sm:text-xs text-muted-foreground font-bold tracking-widest leading-none mb-2 uppercase opacity-60">{item.category}</p>
                          <p className="text-sm font-bold text-foreground uppercase tracking-tight">{item.name}</p>
                        </div>
                      </div>
                    </TableCell>
                    <TableCell className="py-8 sm:py-10 text-center">
                      <span className="text-xs sm:text-xs font-bold text-foreground tracking-widest bg-muted/60 px-4 py-2 rounded-xl inline-block uppercase border border-border/20 group-hover:border-primary/40 transition-colors shadow-sm">
                        {item.account}
                      </span>
                    </TableCell>
                    <TableCell className="py-8 sm:py-10 text-right">
                      <p className="text-sm sm:text-base font-bold text-foreground tabular-nums tracking-tight">
                        Rp {Math.abs(item.amount).toLocaleString('id-ID')}
                      </p>
                    </TableCell>
                    <TableCell className="py-6 text-right">
                      <Button variant="ghost" size="icon" className="h-9 w-9 text-muted-foreground hover:text-foreground hover:bg-muted rounded-xl">
                        <MoreHorizontal className="h-4 w-4" />
                      </Button>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
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
                      <p className="text-xs font-semibold text-muted-foreground tracking-widest leading-none uppercase">{item.category}</p>
                    </div>
                  </div>
                  <p className="text-sm font-bold text-foreground tabular-nums">Rp {Math.abs(item.amount).toLocaleString('id-ID')}</p>
                </div>
                <div className="flex items-center justify-between pt-3 border-t border-border/50">
                  <p className="text-xs font-medium text-muted-foreground">{item.date}</p>
                  <div className="flex items-center gap-2">
                    <span className="text-xs font-bold text-foreground tracking-wider uppercase">{item.account}</span>
                    <Button variant="ghost" size="icon" className="h-8 w-8 text-muted-foreground">
                      <MoreHorizontal className="h-4 w-4" />
                    </Button>
                  </div>
                </div>
              </div>
            ))}
          </div>

          <div className="mt-10 pt-8 border-t border-border flex flex-col sm:flex-row justify-between items-center gap-6">
            <Button variant="ghost" size="sm" className="flex items-center gap-3 text-xs font-bold text-primary hover:text-primary/80 tracking-widest transition-colors uppercase h-auto p-0 hover:bg-transparent">
              <RotateCcw className="h-4 w-4" /> Ulangi Transfer Terakhir
            </Button>
            <Button variant="ghost" size="sm" className="flex items-center gap-3 text-xs font-bold text-primary hover:underline tracking-widest transition-all uppercase h-auto p-0 hover:bg-transparent">
              Riwayat Lengkap <ArrowRight className="h-4 w-4" />
            </Button>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
