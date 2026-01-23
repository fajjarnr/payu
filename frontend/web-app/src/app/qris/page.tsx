'use client';

import React, { useState } from 'react';
import DashboardLayout from "@/components/DashboardLayout";
import { QrCode, Camera, History, Image as ImageIcon, ShieldCheck, Info } from 'lucide-react';
import clsx from 'clsx';

export default function QRISPage() {
  const [isScanning, setIsScanning] = useState(false);

  return (
    <DashboardLayout>
      <div className="space-y-12">
        <div className="flex justify-between items-end">
          <div>
            <h2 className="text-3xl font-black text-foreground ">Pembayaran QRIS</h2>
            <p className="text-sm text-gray-500 font-medium">Pindai kode QRIS merchant atau P2P untuk membayar secara instan.</p>
          </div>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-12 gap-10">
          {/* QR Scanner Area */}
          <div className="lg:col-span-8">
            <div className="bg-card rounded-xl border border-border overflow-hidden shadow-sm flex flex-col items-center justify-center p-12 min-h-[500px] relative group">
              {/* Decorative Background */}
              <div className="absolute top-0 right-0 w-64 h-64 bg-bank-green/5 rounded-full blur-3xl -z-0" />

              <div className="relative z-10 w-full max-w-sm space-y-12 text-center">
                <div className="aspect-square bg-gray-50 dark:bg-gray-900/50 rounded-xl border-2 border-dashed border-border flex flex-col items-center justify-center relative hover:border-bank-green transition-all duration-500">
                  <div className="absolute inset-8 border-2 border-bank-green/20 rounded-xl animate-pulse" />

                  <div className="w-24 h-24 bg-white dark:bg-gray-800 rounded-xl flex items-center justify-center mb-6 shadow-2xl group-hover:scale-110 transition-transform duration-500">
                    <Camera className="h-10 w-10 text-bank-green" />
                  </div>
                  <p className="text-[10px] font-black text-gray-400 tracking-[0.2em]">Posisikan kode QR di dalam bingkai</p>
                </div>

                <div className="flex gap-6">
                  <button className="flex-1 bg-foreground text-background py-6 rounded-xl font-black text-[10px] tracking-[0.2em] hover:bg-bank-green hover:text-white transition-all active:scale-95 flex items-center justify-center gap-3 shadow-xl">
                    <Camera className="h-4 w-4" /> Buka Kamera
                  </button>
                  <button className="flex-1 bg-gray-50 dark:bg-gray-900 text-foreground py-6 rounded-xl font-black text-[10px] tracking-[0.2em] border border-border hover:bg-gray-100 dark:hover:bg-gray-800 transition-all active:scale-95 flex items-center justify-center gap-3">
                    <ImageIcon className="h-4 w-4" /> Unggah Foto
                  </button>
                </div>
              </div>
            </div>
          </div>

          {/* Quick Info & History */}
          <div className="lg:col-span-4 space-y-8">
            <div className="bg-card rounded-xl p-10 shadow-sm border border-border">
              <h3 className="text-sm font-bold text-foreground mb-8 tracking-wide">Keamanan Pembayaran</h3>
              <div className="space-y-8">
                <div className="flex gap-5">
                  <div className="h-12 w-12 bg-bank-green/10 rounded-xl flex items-center justify-center shrink-0 border border-bank-green/10">
                    <ShieldCheck className="h-6 w-6 text-bank-green" />
                  </div>
                  <div>
                    <p className="text-xs font-bold text-foreground">Bayar Terenkripsi</p>
                    <p className="text-[10px] text-gray-400 font-bold leading-relaxed tracking-widest mt-1">Setiap transaksi ditandatangani dengan token perangkat unik.</p>
                  </div>
                </div>
                <div className="flex gap-5">
                  <div className="h-12 w-12 bg-bank-green/10 rounded-xl flex items-center justify-center shrink-0 border border-bank-green/10">
                    <Info className="h-6 w-6 text-bank-green" />
                  </div>
                  <div>
                    <p className="text-xs font-bold text-foreground">Standar OJK & BI</p>
                    <p className="text-[10px] text-gray-400 font-bold leading-relaxed tracking-widest mt-1">Patuh sepenuhnya pada protokol QRIS Bank Indonesia & ASPI.</p>
                  </div>
                </div>
              </div>
            </div>

            <div className="bg-gradient-to-br from-gray-900 to-gray-800 rounded-xl p-10 text-white relative overflow-hidden shadow-2xl group">
              <div className="relative z-10">
                <h4 className="font-bold text-xl mb-3 ">Kode QRIS Saya</h4>
                <p className="text-[10px] text-gray-400 font-bold tracking-widest mb-10 leading-relaxed">Terima dana instan dari aplikasi bank manapun menggunakan kode unik Anda.</p>
                <button className="w-full py-5 bg-white/10 rounded-xl font-black text-[10px] tracking-[0.2em] hover:bg-white/20 transition-all border border-white/10">Tampilkan Kode Saya</button>
              </div>
              <QrCode className="absolute bottom-[-30px] right-[-30px] h-48 w-48 text-white/5 rotate-12 group-hover:rotate-0 transition-transform duration-1000" />
            </div>
          </div>
        </div>

        {/* Recent Transactions Footer */}
        <div className="bg-card rounded-xl p-12 border border-border shadow-sm">
          <div className="flex justify-between items-center mb-10">
            <h3 className="text-xl font-black text-foreground ">Pembayaran QR Terakhir</h3>
            <button className="text-[10px] font-black text-bank-green tracking-[0.2em] border-b-2 border-bank-green/20 hover:border-bank-green transition-all pb-1">Lihat Semua Riwayat</button>
          </div>

          <div className="text-center py-16">
            <History className="h-16 w-16 text-gray-100 dark:text-gray-900 mx-auto mb-6" />
            <p className="text-[10px] font-black text-gray-400 tracking-[0.2em] leading-relaxed max-w-xs mx-auto">Tidak ada transaksi QRIS yang tercatat dalam 30 hari terakhir.</p>
          </div>
        </div>
      </div>
    </DashboardLayout>
  );
}
