'use client';

import { useTranslations } from 'next-intl';
import { Link } from '@/lib/navigation';
import { Button } from '@/components/ui/button';
import { FileQuestion } from 'lucide-react';

/**
 * QAMVP-019: 404 page for unknown routes.
 */
export default function NotFoundPage() {
  const t = useTranslations('errors');

  return (
    <div className="min-h-screen w-full flex items-center justify-center bg-background p-8">
      <div className="w-full max-w-[420px] space-y-6 text-center">
        <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-muted">
          <FileQuestion className="h-8 w-8 text-muted-foreground" />
        </div>
        <div className="space-y-2">
          <h1 className="text-3xl font-bold tracking-tight">404</h1>
          <p className="text-muted-foreground">{t('notFound')}</p>
        </div>
        <Button asChild>
          <Link href="/dashboard">Kembali ke Dasbor</Link>
        </Button>
      </div>
    </div>
  );
}
