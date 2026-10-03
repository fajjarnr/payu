'use client';

import { useState } from 'react';
import { Trash2, Building2, CreditCard, Plus, Loader2 } from '@/components/icons';
import { useTranslations } from 'next-intl';
import { Button, Input } from 'antd';


import { Card } from 'antd';
import { useBeneficiaries, useCreateBeneficiary, useDeleteBeneficiary } from '@/hooks/useBeneficiaries';
import { notify as toast } from '@/lib/notify';

interface BeneficiaryManagerProps {
  accountId: string;
  onSelect?: (accountNumber: string) => void;
}

export default function BeneficiaryManager({ accountId, onSelect }: BeneficiaryManagerProps) {
  const t = useTranslations('beneficiaries');
  const { data: beneficiaries, isLoading } = useBeneficiaries(accountId);
  const createMut = useCreateBeneficiary(accountId);
  const deleteMut = useDeleteBeneficiary(accountId);

  const [bankCode, setBankCode] = useState('');
  const [accountNumber, setAccountNumber] = useState('');
  const [nickname, setNickname] = useState('');
  const [errors, setErrors] = useState<Record<string,string>>({});

  const validate = () => {
    const e: Record<string,string> = {};
    if (!bankCode.trim() || bankCode.length > 10) e.bankCode = t('errBankCode');
    if (!/^\d{10,20}$/.test(accountNumber)) e.accountNumber = t('errAccount');
    if (nickname && nickname.length > 100) e.nickname = t('errNickname');
    setErrors(e);
    return Object.keys(e).length === 0;
  };

  const handleCreate = async () => {
    if (!validate()) return;
    try {
      await createMut.mutateAsync({ bankCode: bankCode.trim(), accountNumber: accountNumber.trim(), nickname: nickname.trim() || undefined });
      toast.success(t('added'));
      setBankCode(''); setAccountNumber(''); setNickname('');
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { error?: { message?: string } } }; message?: string })?.response?.data?.error?.message || (err as { message?: string })?.message || t('addFailed');
    }
  };

  const handleDelete = async (id: string) => {
    try {
      await deleteMut.mutateAsync(id);
      toast.success(t('removed'));
    } catch (err: unknown) {
      toast.error((err as { response?: { data?: { error?: { message?: string } } } })?.response?.data?.error?.message || t('deleteFailed'));
    }
  };

  return (
    <Card data-testid="beneficiary-manager" className="overflow-hidden" styles={{ body: { display: 'contents' } }}>
      <div className="flex flex-col space-y-1.5 p-6 pb-4">
        <h3 className="text-2xl font-bold leading-none tracking-tight flex items-center gap-2 text-base">
          <Building2 className="h-5 w-5 text-primary" aria-hidden="true" />
          {t('title')}
        </h3>
        <p className="text-xs text-muted-foreground font-medium uppercase tracking-[0.1em]">{t('description')}</p>
      </div>
      <div className="p-6 pt-0 space-y-6">
        {/* List */}
        <div className="space-y-3" role="list" aria-label="Beneficiary list">
          {isLoading ? (
            <div className="space-y-2" aria-busy="true">
              <div className="h-16 bg-muted animate-pulse rounded-xl" />
              <div className="h-16 bg-muted/50 animate-pulse rounded-xl" />
            </div>
          ) : !beneficiaries || beneficiaries.length === 0 ? (
            <p className="text-sm text-muted-foreground text-center py-6" data-testid="beneficiary-empty">{t('empty')}</p>
          ) : (
            beneficiaries.map((b) => (
              <div key={b.id} role="listitem" data-testid={`beneficiary-${b.id}`} className="flex items-center justify-between p-4 rounded-xl border border-border bg-card hover:bg-muted/30 transition-colors">
                <div className="flex items-center gap-3 min-w-0 flex-1">
                  <div className="h-10 w-10 rounded-xl bg-primary/10 flex items-center justify-center border border-primary/10 shrink-0">
                    <CreditCard className="h-5 w-5 text-primary" aria-hidden="true" />
                  </div>
                  <div className="min-w-0 flex-1">
                    <p className="text-sm font-bold truncate">{b.nickname || b.accountName} <span className="text-xs font-mono text-muted-foreground">({b.bankCode})</span></p>
                    <p className="text-xs font-mono text-muted-foreground truncate">{b.accountNumber}</p>
                  </div>
                </div>
                <div className="flex items-center gap-2 shrink-0">
                  {onSelect && (
                    <Button type="default" size="small" onClick={() => onSelect(b.accountNumber)} data-testid={`beneficiary-select-${b.id}`} className="cursor-pointer border-transparent bg-transparent hover:bg-muted hover:text-foreground hover:border-transparent active:bg-transparent active:border-transparent">
                      {t('use')}
                    </Button>
                  )}
                  <Button type="default" onClick={() => handleDelete(b.id)} disabled={deleteMut.isPending} aria-label={t('delete', { name: b.nickname || b.accountNumber })} data-testid={`beneficiary-delete-${b.id}`} className="h-11 w-11 cursor-pointer border-transparent bg-transparent hover:bg-muted hover:text-foreground hover:border-transparent active:bg-transparent active:border-transparent">
                    {deleteMut.isPending ? <Loader2 className="h-4 w-4 animate-spin" /> : <Trash2 className="h-4 w-4 text-destructive" />}
                  </Button>
                </div>
              </div>
            ))
          )}
        </div>

        {/* Create form */}
        <div className="bg-muted/30 p-4 sm:p-6 rounded-xl border border-border space-y-4" data-testid="beneficiary-form">
          <h4 className="text-xs font-bold tracking-[0.15em] uppercase text-muted-foreground">{t('addTitle')}</h4>
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div className="space-y-1.5">
              <label htmlFor="beneficiary-bankCode" className="text-sm font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70 text-xs font-bold uppercase tracking-widest">{t('bankCode')}</label>
              <Input id="beneficiary-bankCode" data-testid="beneficiary-bankCode" value={bankCode} onChange={(e) => setBankCode(e.target.value)} placeholder="014" maxLength={10} className="flex h-14 min-h-[44px] w-full rounded-xl border border-border bg-muted/20 px-6 py-3 text-sm font-bold text-foreground transition-all shadow-sm placeholder:text-muted-foreground/40 focus:bg-background focus:ring-4 focus:ring-primary/10 focus:border-primary outline-none disabled:cursor-not-allowed disabled:opacity-50 h-11" />
              {errors.bankCode && <p className="text-xs text-destructive" role="alert">{errors.bankCode}</p>}
            </div>
            <div className="space-y-1.5 sm:col-span-2">
              <label htmlFor="beneficiary-accountNumber" className="text-sm font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70 text-xs font-bold uppercase tracking-widest">{t('accountNumber')}</label>
              <Input id="beneficiary-accountNumber" data-testid="beneficiary-accountNumber" value={accountNumber} onChange={(e) => setAccountNumber(e.target.value.replace(/\D/g,''))} placeholder="1234567890" inputMode="numeric" className="flex h-14 min-h-[44px] w-full rounded-xl border border-border bg-muted/20 px-6 py-3 text-sm font-bold text-foreground transition-all shadow-sm placeholder:text-muted-foreground/40 focus:bg-background focus:ring-4 focus:ring-primary/10 focus:border-primary outline-none disabled:cursor-not-allowed disabled:opacity-50 h-11" />
              {errors.accountNumber && <p className="text-xs text-destructive" role="alert">{errors.accountNumber}</p>}
            </div>
          </div>
          <div className="space-y-1.5">
            <label htmlFor="beneficiary-nickname" className="text-sm font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70 text-xs font-bold uppercase tracking-widest">{t('nicknameOptional')}</label>
            <Input id="beneficiary-nickname" data-testid="beneficiary-nickname" value={nickname} onChange={(e) => setNickname(e.target.value)} placeholder="My BCA" maxLength={100} className="flex h-14 min-h-[44px] w-full rounded-xl border border-border bg-muted/20 px-6 py-3 text-sm font-bold text-foreground transition-all shadow-sm placeholder:text-muted-foreground/40 focus:bg-background focus:ring-4 focus:ring-primary/10 focus:border-primary outline-none disabled:cursor-not-allowed disabled:opacity-50 h-11" />
            {errors.nickname && <p className="text-xs text-destructive" role="alert">{errors.nickname}</p>}
          </div>
          <Button type="primary" size="large" onClick={handleCreate} disabled={createMut.isPending} data-testid="beneficiary-create" className="w-full sm:w-auto cursor-pointer">
            {createMut.isPending ? <Loader2 className="h-4 w-4 animate-spin mr-2" /> : <Plus className="h-4 w-4 mr-2" />}
            {t('add')}
          </Button>
        </div>
      </div>
    </Card>
  );
}
