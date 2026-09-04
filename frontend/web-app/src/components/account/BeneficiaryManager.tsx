'use client';

import { useState } from 'react';
import { Trash2, Building2, CreditCard, Plus, Loader2 } from 'lucide-react';
import { useTranslations } from 'next-intl';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { useBeneficiaries, useCreateBeneficiary, useDeleteBeneficiary } from '@/hooks/useBeneficiaries';
import { toast } from 'sonner';

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
    <Card data-testid="beneficiary-manager" className="overflow-hidden">
      <CardHeader className="pb-4">
        <CardTitle className="flex items-center gap-2 text-base">
          <Building2 className="h-5 w-5 text-primary" aria-hidden="true" />
          {t('title')}
        </CardTitle>
        <CardDescription>{t('description')}</CardDescription>
      </CardHeader>
      <CardContent className="space-y-6">
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
                    <Button variant="ghost" size="sm" onClick={() => onSelect(b.accountNumber)} data-testid={`beneficiary-select-${b.id}`} className="cursor-pointer">
                      {t('use')}
                    </Button>
                  )}
                  <Button variant="ghost" size="icon" onClick={() => handleDelete(b.id)} disabled={deleteMut.isPending} aria-label={t('delete', { name: b.nickname || b.accountNumber })} data-testid={`beneficiary-delete-${b.id}`} className="h-11 w-11 cursor-pointer">
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
              <Label htmlFor="beneficiary-bankCode" className="text-xs font-bold uppercase tracking-widest">{t('bankCode')}</Label>
              <Input id="beneficiary-bankCode" data-testid="beneficiary-bankCode" value={bankCode} onChange={(e) => setBankCode(e.target.value)} placeholder="014" maxLength={10} className="h-11" />
              {errors.bankCode && <p className="text-xs text-destructive" role="alert">{errors.bankCode}</p>}
            </div>
            <div className="space-y-1.5 sm:col-span-2">
              <Label htmlFor="beneficiary-accountNumber" className="text-xs font-bold uppercase tracking-widest">{t('accountNumber')}</Label>
              <Input id="beneficiary-accountNumber" data-testid="beneficiary-accountNumber" value={accountNumber} onChange={(e) => setAccountNumber(e.target.value.replace(/\D/g,''))} placeholder="1234567890" inputMode="numeric" className="h-11" />
              {errors.accountNumber && <p className="text-xs text-destructive" role="alert">{errors.accountNumber}</p>}
            </div>
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="beneficiary-nickname" className="text-xs font-bold uppercase tracking-widest">{t('nicknameOptional')}</Label>
            <Input id="beneficiary-nickname" data-testid="beneficiary-nickname" value={nickname} onChange={(e) => setNickname(e.target.value)} placeholder="My BCA" maxLength={100} className="h-11" />
            {errors.nickname && <p className="text-xs text-destructive" role="alert">{errors.nickname}</p>}
          </div>
          <Button onClick={handleCreate} disabled={createMut.isPending} data-testid="beneficiary-create" className="w-full sm:w-auto cursor-pointer">
            {createMut.isPending ? <Loader2 className="h-4 w-4 animate-spin mr-2" /> : <Plus className="h-4 w-4 mr-2" />}
            {t('add')}
          </Button>
        </div>
      </CardContent>
    </Card>
  );
}
