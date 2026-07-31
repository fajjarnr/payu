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

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!email) {
      toast.error('Email wajib diisi');
      return;
    }
    toast.info('Fitur reset password akan segera hadir');
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
