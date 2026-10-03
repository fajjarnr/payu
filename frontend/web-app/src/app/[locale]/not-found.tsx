'use client';

import { useTranslations } from 'next-intl';
import { Link } from '@/lib/navigation';
import { FileQuestion } from '@/components/icons';

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
        <Link href="/dashboard" className="inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-xl text-xs font-bold uppercase tracking-[0.15em] transition-all focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:pointer-events-none disabled:opacity-50 active:scale-95 cursor-pointer h-12 px-6 py-2 bg-primary text-primary-foreground shadow-lg shadow-primary/20 hover:bg-primary/90 hover:shadow-primary/30">Kembali ke Dasbor</Link>
      </div>
    </div>
  );
}
