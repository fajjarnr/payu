'use client';

import React from 'react';
import DashboardLayout from "@/components/DashboardLayout";
import { TrendingUp, PieChart, Landmark, ArrowUpRight, ShieldCheck, Briefcase, Plus, Coins, BarChart3, Target } from 'lucide-react';
import clsx from 'clsx';
import { PageTransition, StaggerContainer, StaggerItem, ButtonMotion } from '@/components/ui/Motion';

export default function InvestmentsPage() {
  const investmentProducts = [
    { name: 'Suku Bunga Tetap Plus', type: 'Risiko Rendah', return: '5.5% p.a', icon: Landmark, color: 'text-blue-500', bg: 'bg-blue-500/10' },
    { name: 'Equity Growth Fund', type: 'Risiko Tinggi', return: '18.2% p.a', icon: TrendingUp, color: 'text-primary', bg: 'bg-success-light' },
    { name: 'Emas Digital (XAU)', type: 'Stabil', return: 'Harga Pasar', icon: Coins, color: 'text-amber-500', bg: 'bg-amber-500/10' },
  ];

  return (
    <DashboardLayout>
      <PageTransition>
        <div className="space-y-12">
          {/* Header */}
          <StaggerContainer>
            <StaggerItem>
              <div className="flex flex-col sm:flex-row justify-between items-start sm:items-end gap-6 mb-8">
                <div>
                  <h2 className="text-3xl font-black text-foreground">Manajemen Kekayaan</h2>
                  <p className="text-sm text-muted-foreground font-medium mt-1">Tumbuhkan aset Anda dengan produk investasi kelas institusi.</p>
                </div>
                <ButtonMotion>
                  <button className="bg-primary text-primary-foreground px-8 py-4 rounded-xl font-bold text-xs tracking-widest shadow-xl shadow-primary/20 flex items-center gap-2 hover:bg-bank-emerald transition-all">
                    <Plus className="h-4 w-4" /> Investasi Baru
                  </button>
                </ButtonMotion>
              </div>
            </StaggerItem>

            {/* Portfolio Overview */}
            <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
              <StaggerItem className="lg:col-span-8">
                <div className="bg-card rounded-xl p-8 sm:p-10 border border-border shadow-card h-full relative overflow-hidden group">
                  <div className="absolute top-0 right-0 w-80 h-80 bg-primary/5 rounded-full blur-3xl -z-0" />

                  <div className="relative z-10 flex flex-col h-full">
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-10 mb-10">
                      <div className="space-y-6">
                        <div>
                          <p className="text-[10px] font-bold text-muted-foreground tracking-widest uppercase mb-2">Total Portofolio Bersih</p>
                          <h3 className="text-4xl sm:text-5xl font-black text-foreground">Rp 152.800.000</h3>
                        </div>
                        <div className="flex flex-wrap gap-3">
                          <div className="bg-success-light px-4 py-2 rounded-xl flex items-center gap-2 border border-primary/10">
                            <TrendingUp className="h-4 w-4 text-primary" />
                            <span className="text-[10px] font-bold text-primary tracking-widest">+Rp 12,4 Jt (8.2%)</span>
                          </div>
                          <div className="bg-muted/50 px-4 py-2 rounded-xl flex items-center gap-2 border border-border">
                            <ShieldCheck className="h-4 w-4 text-muted-foreground" />
                            <span className="text-[10px] font-bold text-muted-foreground tracking-widest uppercase">Terjamin LPS</span>
                          </div>
                        </div>
                      </div>

                      <div className="flex items-end justify-end gap-2 h-32">
                        {[40, 55, 45, 70, 60, 90, 85, 100].map((h, i) => (
                          <div key={i} className="flex-1 bg-primary/20 rounded-t-lg relative group/bar transition-all duration-500" style={{ height: `${h}%` }}>
                            <div className="absolute top-0 left-0 w-full h-1 bg-primary rounded-full" />
                            <div className="absolute -top-10 left-1/2 -translate-x-1/2 bg-foreground text-background text-[8px] font-bold px-2 py-1 rounded-md opacity-0 group-hover/bar:opacity-100 transition-all shadow-xl whitespace-nowrap">
                              +{h / 10}%
                            </div>
                          </div>
                        ))}
                      </div>
                    </div>

                    <div className="mt-auto grid grid-cols-3 gap-6 pt-8 border-t border-border">
                      <div>
                        <p className="text-[10px] font-bold text-muted-foreground tracking-widest uppercase mb-1">Pasar Uang</p>
                        <p className="text-lg font-black text-foreground">45%</p>
                      </div>
                      <div>
                        <p className="text-[10px] font-bold text-muted-foreground tracking-widest uppercase mb-1">Saham</p>
                        <p className="text-lg font-black text-foreground">30%</p>
                      </div>
                      <div>
                        <p className="text-[10px] font-bold text-muted-foreground tracking-widest uppercase mb-1">Komoditas</p>
                        <p className="text-lg font-black text-foreground">25%</p>
                      </div>
                    </div>
                  </div>
                </div>
              </StaggerItem>

              <StaggerItem className="lg:col-span-4">
                <div className="bg-gradient-to-br from-gray-900 to-gray-800 rounded-xl p-8 sm:p-10 text-white h-full relative overflow-hidden flex flex-col justify-between shadow-2xl group">
                  <div className="relative z-10">
                    <div className="flex justify-between items-start mb-6">
                      <h3 className="text-lg font-bold">Profil Risiko</h3>
                      <div className="h-8 w-8 bg-white/10 rounded-lg flex items-center justify-center border border-white/10">
                        <BarChart3 className="h-4 w-4 text-bank-green" />
                      </div>
                    </div>
                    <div className="flex items-center gap-5 mb-8">
                      <div className="h-14 w-14 rounded-xl bg-white/10 flex items-center justify-center border border-white/10 shadow-xl group-hover:rotate-6 transition-transform duration-500">
                        <Briefcase className="h-7 w-7 text-bank-green" />
                      </div>
                      <div>
                        <p className="text-xl font-black">Moderat-Agresif</p>
                        <p className="text-[10px] text-gray-400 font-bold tracking-widest uppercase mt-0.5">ROI 15% / Thn</p>
                      </div>
                    </div>
                    <div className="space-y-2">
                      <div className="flex justify-between text-[10px] font-bold text-gray-400 tracking-widest uppercase">
                        <span>Konservatif</span>
                        <span>Agresif</span>
                      </div>
                      <div className="w-full bg-white/10 h-2 rounded-full overflow-hidden">
                        <div className="bg-bank-green h-full rounded-full" style={{ width: '75%' }} />
                      </div>
                    </div>
                  </div>
                  <button className="relative z-10 w-full py-4 bg-white/5 border border-white/10 rounded-xl font-bold text-[10px] tracking-widest uppercase hover:bg-white/10 transition-all mt-10">Optimasi Portofolio</button>
                  <PieChart className="absolute bottom-[-40px] right-[-40px] h-48 w-48 text-white/5 -rotate-12 group-hover:rotate-0 transition-transform duration-1000" />
                </div>
              </StaggerItem>
            </div>

            {/* Marketplace Grid */}
            <div className="space-y-8 mt-12">
              <div className="flex justify-between items-center">
                <h3 className="text-xl font-black text-foreground">Katalog Produk Terpilih</h3>
                <div className="flex gap-2">
                  <button className="px-4 py-2 bg-primary text-primary-foreground text-[10px] font-bold rounded-lg uppercase tracking-widest shadow-lg shadow-primary/20">Semua</button>
                  <button className="px-4 py-2 bg-muted/50 text-muted-foreground text-[10px] font-bold rounded-lg uppercase tracking-widest border border-border">Pasar Uang</button>
                  <button className="px-4 py-2 bg-muted/50 text-muted-foreground text-[10px] font-bold rounded-lg uppercase tracking-widest border border-border">Emas</button>
                </div>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                {investmentProducts.map((prod, i) => (
                  <div key={i} className="bg-card p-8 rounded-xl border border-border shadow-sm hover:shadow-card hover:-translate-y-1 transition-all group cursor-pointer active:scale-[0.98]">
                    <div className="flex justify-between items-start mb-10">
                      <div className={clsx(
                        "h-16 w-16 rounded-xl flex items-center justify-center shadow-lg transition-transform group-hover:scale-110",
                        prod.bg,
                        prod.color
                      )}>
                        <prod.icon className="h-8 w-8" />
                      </div>
                      <div className="h-8 w-8 bg-muted/30 rounded-lg flex items-center justify-center text-muted-foreground group-hover:bg-primary group-hover:text-primary-foreground transition-all">
                        <ArrowUpRight className="h-4 w-4" />
                      </div>
                    </div>
                    <div>
                      <p className="text-[10px] font-bold text-muted-foreground tracking-widest uppercase mb-2">{prod.type}</p>
                      <h4 className="text-xl font-black text-foreground mb-6 leading-tight group-hover:text-primary transition-colors">{prod.name}</h4>
                      <div className="flex items-center justify-between p-4 bg-muted/30 rounded-xl border border-border group-hover:border-primary/20 transition-all">
                        <div className="space-y-0.5">
                          <p className="text-[8px] font-bold text-muted-foreground tracking-widest uppercase">Imbal Hasil</p>
                          <span className="text-lg font-black text-primary">{prod.return}</span>
                        </div>
                        <ButtonMotion>
                          <button className="h-10 w-10 bg-primary text-primary-foreground rounded-lg flex items-center justify-center shadow-lg shadow-primary/20">
                            <Plus className="h-4 w-4" />
                          </button>
                        </ButtonMotion>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>

            {/* Smart Advice Section */}
            <div className="mt-12 bg-primary/5 rounded-xl border border-primary/10 p-8 sm:p-12 relative overflow-hidden group">
              <div className="relative z-10 flex flex-col md:flex-row items-center gap-10">
                <div className="h-20 w-20 bg-primary/20 rounded-2xl flex items-center justify-center shrink-0 border border-primary/20">
                  <Target className="h-10 w-10 text-primary animate-pulse" />
                </div>
                <div className="space-y-4">
                  <h3 className="text-2xl font-black text-foreground">Target Portofolio Hampir Tercapai.</h3>
                  <p className="text-sm text-muted-foreground font-medium leading-relaxed max-w-2xl">
                    Berdasarkan performa saat ini, Anda diprediksi akan mencapai target <span className="text-primary font-bold">Dana Pensiun</span> dalam 14 bulan lebih cepat. Pertimbangkan untuk merealokasi 5% aset ke produk yang lebih stabil untuk mengunci keuntungan.
                  </p>
                </div>
                <div className="md:ml-auto">
                  <button className="px-8 py-4 bg-primary text-primary-foreground rounded-xl font-bold text-xs tracking-widest uppercase shadow-xl shadow-primary/20 hover:bg-bank-emerald transition-all">Tinjau Strategi</button>
                </div>
              </div>
            </div>
          </StaggerContainer>
        </div>
      </PageTransition>
    </DashboardLayout>
  );
}
