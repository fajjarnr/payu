'use client';

import React, { useState } from 'react';
import DashboardLayout from "@/components/DashboardLayout";
import { CreditCard, TrendingUp, Calendar, ShieldCheck, Wallet, ArrowRight, Percent, CheckCircle, Clock, Plus, FileText } from 'lucide-react';
import clsx from 'clsx';
import { PageTransition, StaggerContainer, StaggerItem, ButtonMotion } from '@/components/ui/Motion';

export default function LendingPage() {
  const [activeTab, setActiveTab] = useState<'loans' | 'paylater'>('loans');

  const loanProducts = [
    {
      name: 'Pinjaman Personal',
      description: 'Pembiayaan fleksibel untuk kebutuhan pribadi dengan bunga kompetitif',
      minAmount: 2000000,
      maxAmount: 50000000,
      interestRate: '12.5% p.a',
      tenure: '6 - 36 bulan',
      processingTime: '1-2 hari kerja',
      icon: FileText,
      color: 'text-blue-500',
      bg: 'bg-blue-500/10'
    },
    {
      name: 'Pinjaman Multiguna',
      description: 'Gunakan aset Anda sebagai jaminan untuk limit pinjaman lebih tinggi',
      minAmount: 10000000,
      maxAmount: 200000000,
      interestRate: '10% p.a',
      tenure: '12 - 60 bulan',
      processingTime: '3-5 hari kerja',
      icon: TrendingUp,
      color: 'text-primary',
      bg: 'bg-success-light'
    }
  ];

  const payLaterStats = {
    creditLimit: 15000000,
    usedLimit: 4500000,
    availableLimit: 10500000,
    minimumPayment: 250000,
    dueDate: '25 Jan 2026',
    transactions: [
      { id: 1, merchant: 'TokoBapak', amount: 850000, date: '20 Jan 2026', status: 'paid' },
      { id: 2, merchant: 'Traveloka', amount: 3200000, date: '18 Jan 2026', status: 'pending' },
      { id: 3, merchant: 'Shopee', amount: 450000, date: '15 Jan 2026', status: 'paid' }
    ]
  };

  const creditScore = {
    score: 785,
    grade: 'A',
    maxScore: 850,
    lastUpdated: '20 Jan 2026',
    factors: ['Pembayaran tepat waktu', 'Rasio utang rendah', 'Histori kredit panjang']
  };

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR', minimumFractionDigits: 0 }).format(amount);
  };

  return (
    <DashboardLayout>
      <PageTransition>
        <div className="space-y-12">
          <StaggerContainer>
            <StaggerItem>
              <div className="flex flex-col sm:flex-row justify-between items-start sm:items-end gap-6 mb-8">
                <div>
                  <h2 className="text-3xl font-black text-foreground">Pinjaman & Kredit</h2>
                  <p className="text-sm text-muted-foreground font-medium mt-1">Solusi pembiayaan fleksibel sesuai kebutuhan Anda.</p>
                </div>
                {activeTab === 'paylater' && (
                  <ButtonMotion>
                    <button className="bg-primary text-primary-foreground px-8 py-4 rounded-xl font-bold text-xs tracking-widest shadow-xl shadow-primary/20 flex items-center gap-2 hover:bg-bank-emerald transition-all">
                      <Plus className="h-4 w-4" /> Aktifkan PayLater
                    </button>
                  </ButtonMotion>
                )}
              </div>

              <div className="flex gap-2 mb-8">
                <button
                  onClick={() => setActiveTab('loans')}
                  className={clsx(
                    'px-6 py-3 rounded-xl font-bold text-xs tracking-widest uppercase transition-all',
                    activeTab === 'loans' ? 'bg-primary text-primary-foreground shadow-lg shadow-primary/20' : 'bg-muted/50 text-muted-foreground border border-border'
                  )}
                >
                  Pinjaman
                </button>
                <button
                  onClick={() => setActiveTab('paylater')}
                  className={clsx(
                    'px-6 py-3 rounded-xl font-bold text-xs tracking-widest uppercase transition-all',
                    activeTab === 'paylater' ? 'bg-primary text-primary-foreground shadow-lg shadow-primary/20' : 'bg-muted/50 text-muted-foreground border border-border'
                  )}
                >
                  PayLater
                </button>
              </div>
            </StaggerItem>

            {activeTab === 'loans' ? (
              <>
                <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                  <StaggerItem className="lg:col-span-2">
                    <div className="bg-gradient-to-br from-gray-900 to-gray-800 rounded-xl p-8 sm:p-10 text-white relative overflow-hidden shadow-2xl">
                      <div className="relative z-10 flex items-start justify-between mb-8">
                        <div>
                          <p className="text-[10px] font-bold text-gray-400 tracking-widest uppercase mb-2">Skor Kredit Anda</p>
                          <div className="flex items-baseline gap-3">
                            <h3 className="text-5xl font-black">{creditScore.score}</h3>
                            <div className="flex items-center gap-2 bg-success-light/20 px-3 py-1 rounded-full border border-success-light/20">
                              <span className="text-lg font-bold text-success-light">Grade {creditScore.grade}</span>
                            </div>
                          </div>
                          <p className="text-[10px] text-gray-400 font-bold tracking-widest uppercase mt-3">Terakhir diperbarui: {creditScore.lastUpdated}</p>
                        </div>
                        <div className="h-16 w-16 bg-white/10 rounded-xl flex items-center justify-center border border-white/10">
                          <ShieldCheck className="h-8 w-8 text-bank-green" />
                        </div>
                      </div>

                      <div className="space-y-3 mb-8">
                        {creditScore.factors.map((factor, i) => (
                          <div key={i} className="flex items-center gap-3">
                            <div className="h-6 w-6 rounded-full bg-success-light/20 flex items-center justify-center">
                              <CheckCircle className="h-4 w-4 text-success-light" />
                            </div>
                            <span className="text-sm font-medium">{factor}</span>
                          </div>
                        ))}
                      </div>

                      <div className="w-full bg-white/10 h-2 rounded-full overflow-hidden">
                        <div className="bg-gradient-to-r from-success-light to-primary h-full rounded-full" style={{ width: `${(creditScore.score / creditScore.maxScore) * 100}%` }} />
                      </div>
                    </div>
                  </StaggerItem>

                  <StaggerItem>
                    <div className="bg-card rounded-xl p-8 sm:p-10 border border-border shadow-card h-full">
                      <div className="h-12 w-12 bg-primary/10 rounded-xl flex items-center justify-center mb-6 border border-primary/10">
                        <Wallet className="h-6 w-6 text-primary" />
                      </div>
                      <h3 className="text-lg font-black text-foreground mb-3">Total Limit Pinjaman</h3>
                      <p className="text-3xl font-black text-primary mb-2">{formatCurrency(50000000)}</p>
                      <p className="text-[10px] font-bold text-muted-foreground tracking-widest uppercase">Tersedia berdasarkan skor kredit</p>
                    </div>
                  </StaggerItem>
                </div>

                <div className="space-y-8 mt-12">
                  <h3 className="text-xl font-black text-foreground">Produk Pinjaman</h3>
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                    {loanProducts.map((product, i) => (
                      <StaggerItem key={i} className="bg-card p-8 rounded-xl border border-border shadow-sm hover:shadow-card hover:-translate-y-1 transition-all group cursor-pointer active:scale-[0.98]">
                        <div className="flex justify-between items-start mb-8">
                          <div className={clsx("h-16 w-16 rounded-xl flex items-center justify-center shadow-lg transition-transform group-hover:scale-110", product.bg, product.color)}>
                            <product.icon className="h-8 w-8" />
                          </div>
                          <div className="h-10 w-10 bg-muted/30 rounded-lg flex items-center justify-center text-muted-foreground">
                            <Percent className="h-5 w-5" />
                          </div>
                        </div>
                        <div>
                          <h4 className="text-xl font-black text-foreground mb-2 leading-tight">{product.name}</h4>
                          <p className="text-sm text-muted-foreground mb-6 leading-relaxed">{product.description}</p>
                          <div className="space-y-3 mb-6">
                            <div className="flex justify-between items-center text-sm">
                              <span className="text-muted-foreground font-medium">Limit Pinjaman</span>
                              <span className="font-bold text-foreground">{formatCurrency(product.minAmount)} - {formatCurrency(product.maxAmount)}</span>
                            </div>
                            <div className="flex justify-between items-center text-sm">
                              <span className="text-muted-foreground font-medium">Bunga</span>
                              <span className="font-bold text-primary">{product.interestRate}</span>
                            </div>
                            <div className="flex justify-between items-center text-sm">
                              <span className="text-muted-foreground font-medium">Tenor</span>
                              <span className="font-bold text-foreground">{product.tenure}</span>
                            </div>
                            <div className="flex justify-between items-center text-sm">
                              <span className="text-muted-foreground font-medium">Proses</span>
                              <span className="font-bold text-foreground">{product.processingTime}</span>
                            </div>
                          </div>
                          <ButtonMotion>
                            <button className="w-full py-4 bg-primary text-primary-foreground rounded-xl font-bold text-xs tracking-widest uppercase shadow-xl shadow-primary/20 hover:bg-bank-emerald transition-all flex items-center justify-center gap-2">
                              Ajukan Sekarang <ArrowRight className="h-4 w-4" />
                            </button>
                          </ButtonMotion>
                        </div>
                      </StaggerItem>
                    ))}
                  </div>
                </div>
              </>
            ) : (
              <>
                <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                  <StaggerItem className="lg:col-span-2">
                    <div className="bg-gradient-to-br from-primary to-primary/80 rounded-xl p-8 sm:p-10 text-white relative overflow-hidden shadow-2xl">
                      <div className="relative z-10 flex flex-col md:flex-row items-start md:items-center justify-between gap-6 mb-8">
                        <div>
                          <div className="flex items-center gap-3 mb-3">
                            <div className="h-12 w-12 bg-white/20 rounded-xl flex items-center justify-center border border-white/10">
                              <CreditCard className="h-6 w-6" />
                            </div>
                            <div>
                              <p className="text-[10px] font-bold text-white/80 tracking-widest uppercase">PayLater Limit</p>
                              <h3 className="text-2xl font-black">{formatCurrency(payLaterStats.availableLimit)}</h3>
                            </div>
                          </div>
                          <p className="text-sm text-white/80">Tersedia untuk belanja sekarang, bayar nanti</p>
                        </div>
                        <div className="text-right">
                          <p className="text-[10px] font-bold text-white/60 tracking-widest uppercase mb-1">Jatuh Tempo</p>
                          <div className="flex items-center gap-2 justify-end">
                            <Calendar className="h-4 w-4" />
                            <span className="font-bold">{payLaterStats.dueDate}</span>
                          </div>
                        </div>
                      </div>

                      <div className="space-y-4">
                        <div>
                          <div className="flex justify-between text-[10px] font-bold text-white/80 tracking-widest uppercase mb-2">
                            <span>Limit Terpakai</span>
                            <span>{formatCurrency(payLaterStats.usedLimit)} / {formatCurrency(payLaterStats.creditLimit)}</span>
                          </div>
                          <div className="w-full bg-white/20 h-3 rounded-full overflow-hidden">
                            <div className="bg-white h-full rounded-full transition-all" style={{ width: `${(payLaterStats.usedLimit / payLaterStats.creditLimit) * 100}%` }} />
                          </div>
                        </div>
                        <div className="flex items-center justify-between pt-4 border-t border-white/20">
                          <div>
                            <p className="text-[10px] font-bold text-white/60 tracking-widest uppercase">Pembayaran Minimum</p>
                            <p className="text-xl font-black">{formatCurrency(payLaterStats.minimumPayment)}</p>
                          </div>
                          <ButtonMotion>
                            <button className="px-6 py-3 bg-white text-primary rounded-xl font-bold text-xs tracking-widest uppercase hover:bg-white/90 transition-all shadow-lg">
                              Bayar Tagihan
                            </button>
                          </ButtonMotion>
                        </div>
                      </div>
                    </div>
                  </StaggerItem>

                  <StaggerItem>
                    <div className="bg-card rounded-xl p-8 sm:p-10 border border-border shadow-card h-full">
                      <div className="flex justify-between items-start mb-6">
                        <h3 className="text-lg font-black text-foreground">Ringkasan Transaksi</h3>
                        <div className="h-8 w-8 bg-primary/10 rounded-lg flex items-center justify-center border border-primary/10">
                          <TrendingUp className="h-4 w-4 text-primary" />
                        </div>
                      </div>
                      <div className="space-y-4">
                        <div className="flex justify-between items-center py-3 border-b border-border">
                          <span className="text-sm text-muted-foreground font-medium">Total Transaksi</span>
                          <span className="text-lg font-black text-foreground">3</span>
                        </div>
                        <div className="flex justify-between items-center py-3 border-b border-border">
                          <span className="text-sm text-muted-foreground font-medium">Pembayaran Berhasil</span>
                          <span className="text-lg font-black text-success-light">2</span>
                        </div>
                        <div className="flex justify-between items-center py-3">
                          <span className="text-sm text-muted-foreground font-medium">Menunggu Pembayaran</span>
                          <span className="text-lg font-black text-warning">1</span>
                        </div>
                      </div>
                    </div>
                  </StaggerItem>
                </div>

                <div className="space-y-8 mt-12">
                  <h3 className="text-xl font-black text-foreground">Riwayat Transaksi PayLater</h3>
                  <div className="bg-card rounded-xl border border-border shadow-sm overflow-hidden">
                    <div className="divide-y divide-border">
                      {payLaterStats.transactions.map((txn) => (
                        <div key={txn.id} className="p-6 hover:bg-muted/30 transition-colors">
                          <div className="flex items-center justify-between">
                            <div className="flex items-center gap-4">
                              <div className={clsx(
                                'h-12 w-12 rounded-xl flex items-center justify-center',
                                txn.status === 'paid' ? 'bg-success-light/10 text-success-light' : 'bg-warning/10 text-warning'
                              )}>
                                {txn.status === 'paid' ? <CheckCircle className="h-6 w-6" /> : <Clock className="h-6 w-6" />}
                              </div>
                              <div>
                                <h4 className="font-bold text-foreground">{txn.merchant}</h4>
                                <p className="text-[10px] text-muted-foreground font-bold tracking-widest uppercase">{txn.date}</p>
                              </div>
                            </div>
                            <div className="text-right">
                              <p className="text-lg font-black text-foreground">{formatCurrency(txn.amount)}</p>
                              <p className={clsx(
                                'text-[10px] font-bold tracking-widest uppercase flex items-center justify-end gap-1',
                                txn.status === 'paid' ? 'text-success-light' : 'text-warning'
                              )}>
                                {txn.status === 'paid' ? 'Dibayar' : 'Menunggu Pembayaran'}
                              </p>
                            </div>
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>
                </div>
              </>
            )}
          </StaggerContainer>
        </div>
      </PageTransition>
    </DashboardLayout>
  );
}
