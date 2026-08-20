'use client';

import { useState } from 'react';
import { useTranslations } from 'next-intl';
import { Link } from '@/lib/navigation';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { ArrowLeft } from 'lucide-react';
import { toast } from 'sonner';

export default function ForgotPasswordPage() {
  const t = useTranslations('auth');
  const [email, setEmail] = useState('');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!email) {
      toast.error('Email wajib diisi');
      return;
    }
    try {
      // ponytail: OIDC PKCE + Keycloak execute-actions-email per ADR-0039, rate-limit IP + audit payu.auth.password-reset-requested.v1 handled by backend
      const res = await fetch('/api/auth/forgot-password', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'X-Idempotency-Key': crypto.randomUUID() },
        body: JSON.stringify({ email }),
      });
      if (res.ok) toast.success('Instruksi reset telah dikirim');
      else toast.error('Gagal mengirim instruksi');
    } catch {
      toast.error('Gagal mengirim instruksi');
    }
  };

  return (
    <div className="min-h-screen w-full flex items-center justify-center bg-background p-8">
      <div className="w-full max-w-[420px] space-y-8">
        <div className="space-y-2">
          <Link href="/login" className="inline-flex items-center gap-2 text-sm text-muted-foreground hover:text-foreground mb-4">
            <ArrowLeft className="w-4 h-4" />
            <span>Kembali ke login</span>
          </Link>
          <h1 className="text-3xl font-bold tracking-tight">{t('forgotPassword')}</h1>
          <p className="text-muted-foreground">Masukkan email Anda untuk menerima instruksi reset password.</p>
        </div>
        <form onSubmit={handleSubmit} className="space-y-6">
          <div className="space-y-2">
            <Label htmlFor="email">{t('email')}</Label>
            <Input
              id="email"
              type="email"
              placeholder="nama@email.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="h-12"
            />
          </div>
          <Button type="submit" className="w-full h-12 font-bold">
            Kirim Instruksi
          </Button>
        </form>
      </div>
    </div>
  );
}
