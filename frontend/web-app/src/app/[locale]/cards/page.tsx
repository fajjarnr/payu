'use client';

/* eslint-disable no-restricted-syntax -- display percentage uses Number for chart width, not Money arithmetic (ADR-0047 display only) */

import React, { useState } from 'react';
import DashboardLayout from "@/components/DashboardLayout";
import { Eye, EyeOff, Lock, RefreshCw, Sliders, ShieldCheck, Zap, Plus, Loader2, Trash2, Settings } from 'lucide-react';
import clsx from 'clsx';
import { PageTransition, StaggerContainer, StaggerItem, ButtonMotion } from '@/components/ui/Motion';
import { Button } from '@/components/ui/button';
import { Switch } from '@/components/ui/switch';
import { Input } from '@/components/ui/input';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Alert, AlertDescription } from '@/components/ui/alert';
import {
  useCards,
  useFreezeCard,
  useUnfreezeCard,
  useCreateCard,
  useDeleteCard,
  useUpdateCard
} from '@/hooks';
import { useAuthStore } from '@/stores/authStore';
import { asMoney, parseCurrencyExact } from '@/lib/currency';
import type { VirtualCard } from '@/services/WalletService';

// Extended card properties that may come from backend but aren't in the base interface yet
interface ExtendedCardData extends VirtualCard {
  monthlyLimit?: number;
  dailySpent?: number;
  onlineEnabled?: boolean;
  internationalEnabled?: boolean;
  subscriptionEnabled?: boolean;
  atmEnabled?: boolean;
}

interface CardData {
  id: string;
  cardNumber: string;
  expiryDate: string;
  cardHolder: string;
  status: string;
}

export default function CardsPage() {
  const { accountId: authAccountId } = useAuthStore();
  const [showFullDetails, setShowFullDetails] = useState(false);
  const [isLimitModalOpen, setIsLimitModalOpen] = useState(false);
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [selectedCard, setSelectedCard] = useState<VirtualCard | null>(null);
  const [limitForm, setLimitForm] = useState({
    dailyLimit: 25000000,
    monthlyLimit: 100000000,
  });

  const { data: cardsData } = useCards(authAccountId ?? undefined);
  const freezeCard = useFreezeCard();
  const unfreezeCard = useUnfreezeCard();
  const createCard = useCreateCard();
  const deleteCard = useDeleteCard();
  const updateCard = useUpdateCard();

  const primaryCard = (cardsData as CardData[] | undefined)?.[0];
  const cardNumber = primaryCard?.cardNumber ?? '\u2022\u2022\u2022\u2022 \u2022\u2022\u2022\u2022 \u2022\u2022\u2022\u2022 \u2022\u2022\u2022\u2022';
  const cardExpiry = primaryCard?.expiryDate ?? '--/--';
  const cardOwner = primaryCard?.cardHolder ?? '\u2014';
  const cardLast4 = cardNumber.slice(-4);
  const isFrozen = primaryCard?.status === 'FROZEN';

  const handleOpenLimitModal = () => {
    if (cardsData && cardsData.length > 0) {
      const card = cardsData[0];
      setSelectedCard(card);
      setLimitForm({
        dailyLimit: card.dailyLimit ? Number(card.dailyLimit) : 25000000,
        monthlyLimit: (card as ExtendedCardData).monthlyLimit || 100000000,
      });
      setIsLimitModalOpen(true);
    }
  };

  const handleOpenDeleteModal = () => {
    if (cardsData && cardsData.length > 0) {
      setSelectedCard(cardsData[0]);
      setIsDeleteModalOpen(true);
    }
  };

  const handleUpdateLimit = async () => {
    if (!selectedCard || parseCurrencyExact(limitForm.dailyLimit) === '0') return;

    await updateCard.mutateAsync({
      cardId: selectedCard.id,
      data: {
        dailyLimit: parseCurrencyExact(limitForm.dailyLimit),
      },
    });

    setIsLimitModalOpen(false);
  };

  const handleDeleteCard = async () => {
    if (!selectedCard) return;

    await deleteCard.mutateAsync(selectedCard.id);
    setIsDeleteModalOpen(false);
  };

  return (
    <DashboardLayout>
      <PageTransition>
        <div className="space-y-6 lg:space-y-8">
          {/* Header */}
          <StaggerContainer>
            <StaggerItem>
              <div className="flex flex-col sm:flex-row justify-between items-start sm:items-end gap-4 mb-6">
                <div>
                  <h2 className="text-3xl font-bold text-foreground tracking-tight">Kartu Virtual</h2>
                  <p className="text-sm text-muted-foreground font-medium mt-1">Pembayaran online yang aman dengan rincian kartu instan.</p>
                </div>
                <ButtonMotion>
                  <Button
                    className="shadow-xl shadow-primary/20"
                    onClick={() => createCard.mutate({
                      accountId: authAccountId ?? '',
                      cardHolderName: cardOwner,
                      dailyLimit: asMoney('25000000.0000'),
                    })}
                    disabled={createCard.isPending}
                  >
                    {createCard.isPending ? (
                      <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                    ) : (
                      <Plus className="h-4 w-4 mr-2" />
                    )}
                    Kartu Baru
                  </Button>
                </ButtonMotion>
              </div>
            </StaggerItem>

            {/* Top Hero Section: Card Visualization & Limits (8/4 Split) */}
            <div className="grid grid-cols-1 md:grid-cols-12 lg:grid-cols-12 gap-6 items-stretch">
              {/* Left: Digital Card & Primary Actions (8 units) */}
              <div className="md:col-span-12 lg:col-span-8">
                <StaggerItem>
                  <div className="bg-card rounded-2xl border border-border shadow-sm p-5 sm:p-6 lg:p-8 h-full relative overflow-hidden group">
                    <div className="absolute top-0 left-0 w-[500px] h-[500px] bg-emerald-500/5 rounded-full blur-[120px] -z-0" />

                    <div className="relative z-10 flex flex-col items-center justify-center gap-6 h-full">
                      {/* Digital Card Visualization */}
                      <div className="w-full max-w-[440px] aspect-[1.586/1] rounded-2xl relative overflow-hidden shadow-2xl group-hover:scale-[1.01] transition-all duration-700 border border-white/10">
                        <div className="absolute inset-0 bg-gradient-to-br from-emerald-600 to-emerald-400" />
                        <div className="absolute inset-0 bg-white/5 backdrop-blur-md" />
                        <div className="absolute -top-8 -right-10 w-64 h-64 bg-white/20 rounded-full blur-3xl" />

                        <div className="relative z-10 h-full p-5 sm:p-6 lg:p-8 flex flex-col justify-between text-white">
                          <div className="flex justify-between items-start">
                            <div className="flex items-center gap-3">
                              <div className="h-10 w-10 bg-white/20 backdrop-blur-md rounded-xl flex items-center justify-center text-white font-bold text-xl border border-white/20">U</div>
                              <span className="text-xl font-bold tracking-tighter">PayU</span>
                            </div>
                            <div className="h-2 w-2 bg-white rounded-full animate-pulse shadow-[0_0_15px_rgba(255,255,255,1)]" />
                          </div>

                          <div className="space-y-4">
                            <div className="text-2xl sm:text-[1.75rem] font-bold tracking-[0.25em] font-mono leading-none drop-shadow-xl tabular-nums">
                              {showFullDetails ? cardNumber : `•••• •••• •••• ${cardLast4}`}
                            </div>
                            <div className="flex justify-between items-end">
                              <div className="space-y-1">
                                <p className="text-xs text-white/50 font-bold tracking-widest uppercase">Owner</p>
                                <p className="text-xs font-bold uppercase tracking-widest truncate max-w-[150px]">{cardOwner}</p>
                              </div>
                              <div className="text-right space-y-0.5">
                                <p className="text-xs text-white/50 font-bold tracking-widest uppercase">Exp</p>
                                <p className="font-mono font-bold text-xs">{cardExpiry}</p>
                              </div>
                            </div>
                          </div>
                        </div>
                      </div>

                      {/* Centered Actions */}
                      <div className="w-full max-w-[440px] grid grid-cols-2 gap-4">
                        <Button
                          onClick={() => setShowFullDetails(!showFullDetails)}
                          className="bg-gray-950 hover:bg-emerald-600 shadow-lg text-white"
                        >
                          {showFullDetails ? <EyeOff className="h-4 w-4 mr-2" /> : <Eye className="h-4 w-4 mr-2" />}
                          Detail Kartu
                        </Button>
                        <Button variant="outline" className="text-muted-foreground/60 hover:text-white hover:bg-destructive hover:border-destructive"
                          onClick={() => primaryCard?.id && (isFrozen ? unfreezeCard.mutate(primaryCard.id) : freezeCard.mutate(primaryCard.id))}
                        >
                          <Lock className="h-4 w-4 mr-2" /> {isFrozen ? 'Aktifkan' : 'Bekukan'}
                        </Button>
                        <Button variant="outline" onClick={handleOpenLimitModal}>
                          <Settings className="h-4 w-4 mr-2" /> Ubah Limit
                        </Button>
                        <Button variant="outline" className="text-red-500 hover:bg-red-500/10 hover:text-red-500"
                          onClick={handleOpenDeleteModal}
                          disabled={deleteCard.isPending}
                        >
                          {deleteCard.isPending ? (
                            <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                          ) : (
                            <Trash2 className="h-4 w-4 mr-2" />
                          )}
                          Hapus Kartu
                        </Button>
                      </div>
                    </div>
                  </div>
                </StaggerItem>
              </div>

              {/* Right: Daily Limit (4 units) Styled after Profil Risiko */}
              <div className="md:col-span-12 lg:col-span-4">
                <StaggerItem>
                  <div className="bg-slate-900 rounded-2xl p-5 sm:p-6 lg:p-8 text-white h-full relative overflow-hidden shadow-xl border border-white/5 flex flex-col justify-between min-h-[320px]">
                    <div className="absolute top-0 right-0 w-32 h-32 bg-emerald-500/10 rounded-full blur-[60px]" />

                    <div className="relative z-10">
                      <div className="flex justify-between items-center mb-6">
                        <h3 className="text-lg font-bold">Limit Harian</h3>
                        <Button size="icon" variant="outline" className="h-10 w-10 bg-white/5 border-white/10 hover:bg-white/10"
                          onClick={handleOpenLimitModal}
                        >
                          <Sliders className="h-4 w-4 text-emerald-400" />
                        </Button>
                      </div>

                      <div className="space-y-6">
                        <div>
                          <p className="text-xs text-white/40 font-bold tracking-widest uppercase mb-2">Terpakai Hari Ini</p>
                          <p className="text-3xl font-bold tabular-nums">{primaryCard ? `Rp ${((cardsData?.[0] as ExtendedCardData)?.dailySpent ?? 0).toLocaleString('id-ID')}` : '\u2014'}</p>
                        </div>

                        <div className="bg-white/5 rounded-xl p-4 border border-white/5 flex items-center gap-3">
                          <div className="h-8 w-8 bg-emerald-500/20 rounded-lg flex items-center justify-center">
                            <ShieldCheck className="h-4 w-4 text-emerald-400" />
                          </div>
                          <div>
                            <p className="text-xs font-bold text-emerald-400">Status Aktif</p>
                            <p className="text-xs text-white/40 font-medium tracking-tight">Terlindungi Protokol Keamanan</p>
                          </div>
                        </div>
                      </div>
                    </div>

                    <div className="relative z-10 space-y-4">
                        <div className="h-2 w-full bg-white/5 rounded-full overflow-hidden">
                        <div className="h-full bg-emerald-400 rounded-full shadow-[0_0_15px_rgba(52,211,153,0.5)]" style={{ width: `${limitForm.dailyLimit > 0 ? Math.min(100, (((cardsData?.[0] as ExtendedCardData)?.dailySpent ?? 0) / limitForm.dailyLimit) * 100) : 0}%` }} />
                      </div>
                      <div className="flex justify-between items-end">
                        <p className="text-xs font-bold text-emerald-400">{limitForm.dailyLimit > 0 ? Math.round((((cardsData?.[0] as ExtendedCardData)?.dailySpent ?? 0) / limitForm.dailyLimit) * 100) : 0}% Terpakai</p>
                        <p className="text-xs font-bold text-white/40 tabular-nums">Limit: Rp {(limitForm.dailyLimit / 1000000).toFixed(1)}jt</p>
                      </div>
                      <Button
                        variant="outline"
                        className="w-full bg-white/10 hover:bg-white/20 border-white/10 mt-4 text-white"
                        onClick={handleOpenLimitModal}
                      >
                        Ubah Batas Transaksi
                      </Button>
                    </div>
                  </div>
                </StaggerItem>
              </div>
            </div>

            {/* Mid Section: Catalog Style Operations (4 Columns Grid) */}
            <div className="space-y-6">
              <h3 className="text-lg font-bold text-foreground">Kontrol Operasional</h3>
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
                {[
                  { label: 'Transaksi Online', desc: 'Situs web & retail', icon: Zap, status: (cardsData?.[0] as ExtendedCardData)?.onlineEnabled ?? false, tag: 'REKOMENDASI' },
                  { label: 'Internasional', desc: 'Transaksi lintas negara', icon: ShieldCheck, status: (cardsData?.[0] as ExtendedCardData)?.internationalEnabled ?? false, tag: 'AMAN' },
                  { label: 'Langganan', desc: 'Merchant & auto-debit', icon: RefreshCw, status: (cardsData?.[0] as ExtendedCardData)?.subscriptionEnabled ?? false, tag: 'AKTIF' },
                  { label: 'Penarikan ATM', desc: 'Izin tarik tunai fisik', icon: Sliders, status: (cardsData?.[0] as ExtendedCardData)?.atmEnabled ?? false, tag: 'BLOKIR' },
                ].map((item, i) => (
                  <StaggerItem key={i}>
                    <div className="bg-card rounded-2xl border border-border p-6 shadow-sm hover:shadow-md transition-all group relative overflow-hidden">
                      <div className="flex justify-between items-start mb-6">
                        <div className={clsx(
                          "h-10 w-10 rounded-xl flex items-center justify-center border transition-all",
                          item.status ? "bg-emerald-500/10 border-emerald-500/20 text-emerald-600" : "bg-muted/50 border-border text-muted-foreground"
                        )}>
                          <item.icon className="h-5 w-5" />
                        </div>
                        <span className={clsx(
                          "text-xs font-bold px-2 py-0.5 rounded-full tracking-widest",
                          item.status ? "bg-emerald-500/10 text-emerald-600" : "bg-muted text-muted-foreground/60"
                        )}>{item.tag}</span>
                      </div>
                      <div>
                        <p className="text-xs text-muted-foreground font-bold tracking-widest uppercase mb-1">{item.status ? 'Aktif' : 'Non-aktif'}</p>
                        <h4 className="text-sm font-bold text-foreground mb-1">{item.label}</h4>
                        <p className="text-xs text-muted-foreground font-medium opacity-60 leading-tight">{item.desc}</p>
                      </div>
                      <div className="mt-6 pt-6 border-t border-border flex justify-between items-center">
                        <span className="text-xs font-bold text-emerald-600 tracking-widest uppercase">Atur Izin</span>
                        <Switch defaultChecked={item.status} />
                      </div>
                    </div>
                  </StaggerItem>
                ))}
              </div>
            </div>

            {/* Bottom Banner Area (Full Width) Styled after Target Portofolio Banner */}
            <StaggerItem>
              <div className="bg-emerald-500/5 border border-emerald-500/10 rounded-2xl p-5 sm:p-6 lg:p-8 flex flex-col md:flex-row items-center justify-between gap-6 relative overflow-hidden group">
                <div className="absolute top-0 left-0 w-64 h-64 bg-emerald-500/5 rounded-full blur-[80px]" />
                <div className="flex items-center gap-6 relative z-10 w-full md:w-auto">
                  <div className="h-14 w-14 bg-emerald-500/10 rounded-2xl flex items-center justify-center border border-emerald-500/20 shadow-inner">
                    <SecurityIcon className="h-7 w-7 text-emerald-500" />
                  </div>
                  <div className="space-y-1">
                    <h4 className="text-lg font-bold text-foreground">Protokol Keamanan Aktif.</h4>
                    <p className="text-sm text-muted-foreground font-medium opacity-80 max-w-2xl">
                      Sistem AI kami mendeteksi aktivitas mencurigakan secara real-time. Upgrade ke Premium untuk perlindungan asuransi saldo hingga Rp 50.000.000.
                    </p>
                  </div>
                </div>
                <Button className="shadow-xl shadow-emerald-500/10 whitespace-nowrap relative z-10 h-14">
                  Upgrade Sekarang
                </Button>
              </div>
            </StaggerItem>
          </StaggerContainer>
        </div>
      </PageTransition>

      {/* Limit Update Modal */}
      <Dialog open={isLimitModalOpen} onOpenChange={setIsLimitModalOpen}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>Ubah Batas Transaksi</DialogTitle>
            <DialogDescription>
              Sesuaikan limit harian dan bulanan untuk kartu {cardLast4}.
            </DialogDescription>
          </DialogHeader>

          {updateCard.isError && (
            <Alert className="bg-red-500/10 border-red-500/20">
              <AlertDescription className="text-red-500">
                Gagal mengubah limit. Silakan coba lagi.
              </AlertDescription>
            </Alert>
          )}

          <div className="grid gap-4 py-4">
            <div className="space-y-2">
              <label className="text-sm font-medium">Limit Harian (IDR)</label>
              <Input
                type="number"
                value={limitForm.dailyLimit}
                onChange={(e) => setLimitForm(prev => ({ ...prev, dailyLimit: parseInt(e.target.value) || 0 }))}
                placeholder="25000000"
              />
              <p className="text-xs text-muted-foreground">Maksimum transaksi per hari</p>
            </div>
            <div className="space-y-2">
              <label className="text-sm font-medium">Limit Bulanan (IDR)</label>
              <Input
                type="number"
                value={limitForm.monthlyLimit}
                onChange={(e) => setLimitForm(prev => ({ ...prev, monthlyLimit: parseInt(e.target.value) || 0 }))}
                placeholder="100000000"
              />
              <p className="text-xs text-muted-foreground">Maksimum transaksi per bulan</p>
            </div>
          </div>

          <DialogFooter>
            <Button variant="outline" onClick={() => setIsLimitModalOpen(false)}>
              Batal
            </Button>
            <Button
              onClick={handleUpdateLimit}
              disabled={updateCard.isPending}
            >
              {updateCard.isPending ? (
                <Loader2 className="h-4 w-4 mr-2 animate-spin" />
              ) : null}
              Simpan Perubahan
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Delete Confirmation Modal */}
      <Dialog open={isDeleteModalOpen} onOpenChange={setIsDeleteModalOpen}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle className="text-destructive">Hapus Kartu</DialogTitle>
            <DialogDescription>
              Apakah Anda yakin ingin menghapus kartu berakhiran {cardLast4}? Tindakan ini tidak dapat dibatalkan.
            </DialogDescription>
          </DialogHeader>

          {deleteCard.isError && (
            <Alert className="bg-red-500/10 border-red-500/20">
              <AlertDescription className="text-red-500">
                Gagal menghapus kartu. Silakan coba lagi.
              </AlertDescription>
            </Alert>
          )}

          <DialogFooter className="mt-4">
            <Button variant="outline" onClick={() => setIsDeleteModalOpen(false)}>
              Batal
            </Button>
            <Button
              variant="destructive"
              onClick={handleDeleteCard}
              disabled={deleteCard.isPending}
            >
              {deleteCard.isPending ? (
                <Loader2 className="h-4 w-4 mr-2 animate-spin" />
              ) : (
                <Trash2 className="h-4 w-4 mr-2" />
              )}
              Hapus Kartu
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </DashboardLayout>
  );
}

// Simple Security Icon for the banner
function SecurityIcon({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className={className}>
      <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10" />
      <path d="m9 12 2 2 4-4" />
    </svg>
  );
}
