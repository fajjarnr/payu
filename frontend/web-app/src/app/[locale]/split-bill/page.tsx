'use client';

import React, { useState } from 'react';
import DashboardLayout from '@/components/DashboardLayout';
import {
  Users,
  Plus,
  DollarSign,
  CheckCircle2,
  Clock,
  XCircle,
  UserPlus,
  Receipt,
  Loader2,
} from 'lucide-react';
import { PageTransition, StaggerContainer, StaggerItem, ButtonMotion } from '@/components/ui/Motion';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Badge } from '@/components/ui/badge';
import {
  useSplitBills,
  useCreateSplitBill,
  useSettleSplitBill,
  useAddParticipant,
  useActivateSplitBill,
} from '@/hooks';
import { useAuthStore } from '@/stores/authStore';
import { addCurrency, divideCurrency, formatExactDecimal, parseCurrencyExact, type Money } from '@/lib/currency';
import type { SplitBillParticipant } from '@/services/TransactionService';

export default function SplitBillPage() {
  const { accountId } = useAuthStore();
  const acctId = accountId ?? '';
  const { data: splitBillsData, isLoading } = useSplitBills(acctId);
  const createSplitBill = useCreateSplitBill();
  const settleBill = useSettleSplitBill();
  const addParticipant = useAddParticipant();
  const activateBill = useActivateSplitBill();

  const [showCreateModal, setShowCreateModal] = useState(false);
  const [newBillName, setNewBillName] = useState('');
  const [newBillAmount, setNewBillAmount] = useState('');
  const [participants, setParticipants] = useState<Array<{ accountId: string; accountNumber: string; accountName: string }>>([
    { accountId: '', accountNumber: '', accountName: '' },
  ]);

  const updateParticipant = (index: number, field: 'accountId' | 'accountNumber' | 'accountName', value: string) => {
    setParticipants((prev) => prev.map((p, i) => (i === index ? { ...p, [field]: value } : p)));
  };

  const addParticipantRow = () => {
    setParticipants((prev) => [...prev, { accountId: '', accountNumber: '', accountName: '' }]);
  };

  const removeParticipantRow = (index: number) => {
    setParticipants((prev) => prev.filter((_, i) => i !== index));
  };

  const splitBills = ((Array.isArray(splitBillsData) ? splitBillsData : []) as unknown as Array<{
    id: string;
    description: string;
    totalAmount: Money;
    currency: string;
    status: string;
    createdAt: string;
    participants: Array<{
      id: string;
      accountId: string;
      name: string;
      amount: Money;
      status: string;
      paidAmount: Money;
    }>;
  }>);

  const formatCurrency = (amount: Money | number) =>
    formatExactDecimal(amount, 0, 'id-ID');

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'ACTIVE':
        return <Badge className="bg-emerald-500/10 text-emerald-500 border-emerald-500/20 px-3 py-1 text-xs uppercase tracking-widest">Aktif</Badge>;
      case 'SETTLED':
        return <Badge className="bg-blue-500/10 text-blue-500 border-blue-500/20 px-3 py-1 text-xs uppercase tracking-widest">Lunas</Badge>;
      case 'PENDING':
        return <Badge className="bg-amber-500/10 text-amber-500 border-amber-500/20 px-3 py-1 text-xs uppercase tracking-widest">Menunggu</Badge>;
      case 'CANCELLED':
        return <Badge className="bg-rose-500/10 text-rose-500 border-rose-500/20 px-3 py-1 text-xs uppercase tracking-widest">Dibatalkan</Badge>;
      default:
        return <Badge variant="outline">{status}</Badge>;
    }
  };

  const handleCreate = () => {
    if (!newBillName || !newBillAmount) return;
    const validParticipants = participants
      .map((p) => ({ ...p, accountId: p.accountId.trim(), accountNumber: p.accountNumber.trim(), accountName: p.accountName.trim() }))
      .filter((p) => p.accountId && p.accountNumber && p.accountName);
    if (validParticipants.length === 0) return;
    const totalAmount = parseCurrencyExact(newBillAmount);
    const perHead = divideCurrency(totalAmount, validParticipants.length);
    createSplitBill.mutate(
      {
        title: newBillName,
        totalAmount,
        currency: 'IDR',
        creatorAccountId: acctId,
        splitType: 'EQUAL',
        participants: validParticipants.map((p) => ({ ...p, amountOwed: perHead })),
      },
      {
        onSuccess: () => {
          setShowCreateModal(false);
          setNewBillName('');
          setNewBillAmount('');
          setParticipants([{ accountId: '', accountNumber: '', accountName: '' }]);
        },
      }
    );
  };

  const activeBills = splitBills.filter((b) => b.status === 'ACTIVE' || b.status === 'PENDING');
  const settledBills = splitBills.filter((b) => b.status === 'SETTLED');

  return (
    <DashboardLayout>
      <PageTransition>
        <div className="space-y-12">
          <StaggerContainer>
            {/* Header */}
            <StaggerItem>
              <div className="flex flex-col sm:flex-row justify-between items-start sm:items-end gap-6 mb-8">
                <div>
                  <h2 className="text-3xl font-bold text-foreground">Split Bill</h2>
                  <p className="text-sm text-muted-foreground font-medium mt-1">
                    Bagi tagihan dengan teman, keluarga, atau rekan kerja secara adil.
                  </p>
                </div>
                <ButtonMotion>
                  <Button
                    className="h-14 px-8 shadow-xl shadow-primary/20 flex items-center gap-2"
                    onClick={() => setShowCreateModal(true)}
                  >
                    <Plus className="h-4 w-4" /> Split Bill Baru
                  </Button>
                </ButtonMotion>
              </div>
            </StaggerItem>

            {/* Stats */}
            <StaggerItem>
              <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                <div className="bg-card border border-border p-6 rounded-2xl shadow-sm flex items-center gap-5">
                  <div className="bg-emerald-500 h-12 w-12 rounded-xl flex items-center justify-center text-white shadow-lg">
                    <Receipt className="h-6 w-6" />
                  </div>
                  <div>
                    <p className="text-xs font-bold text-muted-foreground uppercase tracking-widest">Aktif</p>
                    <p className="text-2xl font-bold text-foreground mt-0.5">{activeBills.length}</p>
                  </div>
                </div>
                <div className="bg-card border border-border p-6 rounded-2xl shadow-sm flex items-center gap-5">
                  <div className="bg-blue-500 h-12 w-12 rounded-xl flex items-center justify-center text-white shadow-lg">
                    <CheckCircle2 className="h-6 w-6" />
                  </div>
                  <div>
                    <p className="text-xs font-bold text-muted-foreground uppercase tracking-widest">Lunas</p>
                    <p className="text-2xl font-bold text-foreground mt-0.5">{settledBills.length}</p>
                  </div>
                </div>
                <div className="bg-card border border-border p-6 rounded-2xl shadow-sm flex items-center gap-5">
                  <div className="bg-indigo-500 h-12 w-12 rounded-xl flex items-center justify-center text-white shadow-lg">
                    <DollarSign className="h-6 w-6" />
                  </div>
                  <div>
                    <p className="text-xs font-bold text-muted-foreground uppercase tracking-widest">Total</p>
                    <p className="text-2xl font-bold text-foreground mt-0.5">
                      {formatCurrency(splitBills.reduce((sum, b) => addCurrency(sum, b.totalAmount), '0'))}
                    </p>
                  </div>
                </div>
              </div>
            </StaggerItem>

            {/* Create Modal */}
            {showCreateModal && (
              <StaggerItem>
                <div className="bg-card border border-border rounded-2xl p-8 shadow-card space-y-6">
                  <h3 className="text-xl font-bold text-foreground">Buat Split Bill Baru</h3>
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                    <div>
                      <label className="text-xs font-bold text-muted-foreground uppercase tracking-widest mb-2 block">
                        Deskripsi
                      </label>
                      <Input
                        placeholder="Makan siang, nonton bareng..."
                        value={newBillName}
                        onChange={(e) => setNewBillName(e.target.value)}
                        className="h-12"
                      />
                    </div>
                    <div>
                      <label className="text-xs font-bold text-muted-foreground uppercase tracking-widest mb-2 block">
                        Total Tagihan
                      </label>
                      <Input
                        type="number"
                        placeholder="150000"
                        value={newBillAmount}
                        onChange={(e) => setNewBillAmount(e.target.value)}
                        className="h-12"
                      />
                    </div>
                  </div>
                  <div className="space-y-3">
                    <div className="flex items-center justify-between">
                      <label className="text-xs font-bold text-muted-foreground uppercase tracking-widest">
                        Peserta (min. 1)
                      </label>
                      <Button type="button" variant="outline" size="sm" onClick={addParticipantRow} className="h-9 gap-1 text-xs font-bold uppercase tracking-widest">
                        <UserPlus className="h-3 w-3" /> Tambah Peserta
                      </Button>
                    </div>
                    {participants.map((p, i) => (
                      <div key={i} className="grid grid-cols-1 md:grid-cols-[1fr_1fr_1.4fr_auto] gap-3 items-center">
                        <Input
                          placeholder="Account ID"
                          value={p.accountId}
                          onChange={(e) => updateParticipant(i, 'accountId', e.target.value)}
                          className="h-11"
                        />
                        <Input
                          placeholder="No. Rekening"
                          value={p.accountNumber}
                          onChange={(e) => updateParticipant(i, 'accountNumber', e.target.value)}
                          className="h-11"
                        />
                        <Input
                          placeholder="Nama"
                          value={p.accountName}
                          onChange={(e) => updateParticipant(i, 'accountName', e.target.value)}
                          className="h-11"
                        />
                        <Button
                          type="button"
                          variant="ghost"
                          size="sm"
                          disabled={participants.length === 1}
                          onClick={() => removeParticipantRow(i)}
                          className="h-11 text-rose-500"
                        >
                          Hapus
                        </Button>
                      </div>
                    ))}
                    <p className="text-xs text-muted-foreground">
                      Jumlah tiap peserta dibagi rata (split rata).
                    </p>
                  </div>
                  <div className="flex gap-4">
                    <Button onClick={handleCreate} disabled={createSplitBill.isPending} className="h-12 px-8">
                      {createSplitBill.isPending ? <Loader2 className="h-4 w-4 animate-spin mr-2" /> : null}
                      Buat
                    </Button>
                    <Button variant="outline" onClick={() => setShowCreateModal(false)} className="h-12 px-8">
                      Batal
                    </Button>
                  </div>
                </div>
              </StaggerItem>
            )}

            {/* Active Split Bills */}
            {activeBills.length > 0 && (
              <div className="space-y-6">
                <h3 className="text-xl font-bold text-foreground">Split Bill Aktif</h3>
                <div className="space-y-4">
                  {activeBills.map((bill) => (
                    <StaggerItem key={bill.id}>
                      <div className="bg-card border border-border rounded-2xl p-6 shadow-sm hover:shadow-md transition-all">
                        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-6">
                          <div className="flex items-center gap-4">
                            <div className="h-12 w-12 bg-primary/10 rounded-xl flex items-center justify-center">
                              <Users className="h-6 w-6 text-primary" />
                            </div>
                            <div>
                              <h4 className="font-bold text-foreground">{bill.description}</h4>
                              <p className="text-xs text-muted-foreground font-bold tracking-widest uppercase">
                                {new Date(bill.createdAt).toLocaleDateString(undefined)}
                              </p>
                            </div>
                          </div>
                          <div className="flex items-center gap-3">
                            {getStatusBadge(bill.status)}
                            <span className="text-lg font-bold text-foreground">
                              {formatCurrency(bill.totalAmount)}
                            </span>
                          </div>
                        </div>

                        {/* Participants */}
                        {bill.participants?.length > 0 && (
                          <div className="space-y-3 mb-4">
                            {bill.participants.map((p) => (
                              <div key={p.id} className="flex items-center justify-between p-3 bg-muted/30 rounded-xl">
                                <div className="flex items-center gap-3">
                                  <div className="h-8 w-8 bg-primary/10 rounded-full flex items-center justify-center text-xs font-bold text-primary">
                                    {p.name?.charAt(0) ?? '?'}
                                  </div>
                                  <span className="text-sm font-medium text-foreground">{p.name}</span>
                                </div>
                                <div className="flex items-center gap-3">
                                  <span className="text-sm font-bold text-foreground">
                                    {formatCurrency(p.amount)}
                                  </span>
                                  {p.status === 'PAID' ? (
                                    <CheckCircle2 className="h-4 w-4 text-emerald-500" />
                                  ) : p.status === 'DECLINED' ? (
                                    <XCircle className="h-4 w-4 text-rose-500" />
                                  ) : (
                                    <Clock className="h-4 w-4 text-amber-500" />
                                  )}
                                </div>
                              </div>
                            ))}
                          </div>
                        )}

                        {/* Actions */}
                        <div className="flex flex-wrap gap-2">
                          <Button
                            size="sm"
                            variant="outline"
                            onClick={() =>
                              addParticipant.mutate({
                                id: bill.id,
                                participant: { accountId: '', accountName: 'Teman Baru', amountOwed: divideCurrency(bill.totalAmount, 2), status: 'PENDING' } as SplitBillParticipant,
                              })
                            }
                            className="text-xs font-bold tracking-widest uppercase gap-1"
                          >
                            <UserPlus className="h-3 w-3" /> Tambah Peserta
                          </Button>
                          {bill.status === 'PENDING' && (
                            <Button
                              size="sm"
                              onClick={() => activateBill.mutate(bill.id)}
                              className="text-xs font-bold tracking-widest uppercase"
                            >
                              Aktifkan
                            </Button>
                          )}
                          <Button
                            size="sm"
                            variant="outline"
                            onClick={() => settleBill.mutate(bill.id)}
                            className="text-xs font-bold tracking-widest uppercase text-emerald-600 border-emerald-500/20 hover:bg-emerald-500/5"
                          >
                            Selesaikan
                          </Button>
                        </div>
                      </div>
                    </StaggerItem>
                  ))}
                </div>
              </div>
            )}

            {/* Settled Bills */}
            {settledBills.length > 0 && (
              <div className="space-y-6">
                <h3 className="text-xl font-bold text-foreground">Riwayat</h3>
                <div className="space-y-4">
                  {settledBills.map((bill) => (
                    <StaggerItem key={bill.id}>
                      <div className="bg-card border border-border rounded-2xl p-6 shadow-sm opacity-80">
                        <div className="flex justify-between items-center">
                          <div className="flex items-center gap-4">
                            <div className="h-12 w-12 bg-muted rounded-xl flex items-center justify-center">
                              <CheckCircle2 className="h-6 w-6 text-muted-foreground" />
                            </div>
                            <div>
                              <h4 className="font-bold text-foreground">{bill.description}</h4>
                              <p className="text-xs text-muted-foreground font-bold tracking-widest uppercase">
                                {bill.participants?.length ?? 0} peserta
                              </p>
                            </div>
                          </div>
                          <span className="text-lg font-bold text-muted-foreground">
                            {formatCurrency(bill.totalAmount)}
                          </span>
                        </div>
                      </div>
                    </StaggerItem>
                  ))}
                </div>
              </div>
            )}

            {/* Empty State */}
            {!isLoading && splitBills.length === 0 && (
              <StaggerItem>
                <div className="text-center py-20 bg-card border border-border rounded-2xl">
                  <Users className="h-16 w-16 text-muted-foreground mx-auto opacity-20 mb-6" />
                  <h3 className="text-lg font-bold text-foreground mb-2">Belum ada Split Bill</h3>
                  <p className="text-sm text-muted-foreground font-medium mb-6">
                    Buat split bill pertama Anda untuk membagi tagihan bersama teman.
                  </p>
                  <ButtonMotion>
                    <Button onClick={() => setShowCreateModal(true)} className="h-12 px-8 shadow-xl shadow-primary/20">
                      <Plus className="h-4 w-4 mr-2" /> Mulai Split Bill
                    </Button>
                  </ButtonMotion>
                </div>
              </StaggerItem>
            )}

            {isLoading && (
              <StaggerItem>
                <div className="flex items-center justify-center py-20">
                  <Loader2 className="h-8 w-8 animate-spin text-primary" />
                </div>
              </StaggerItem>
            )}
          </StaggerContainer>
        </div>
      </PageTransition>
    </DashboardLayout>
  );
}
