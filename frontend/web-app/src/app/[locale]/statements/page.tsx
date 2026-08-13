'use client';

import React from 'react';
import { useTranslations } from 'next-intl';
import DashboardLayout from '@/components/DashboardLayout';
import { PageTransition } from '@/components/ui/Motion';
import StatementDownloader from '@/components/settings/statement-downloader';

/**
 * QAMVP-019: dedicated statement page.
 * Reuses the statement downloader component (generation + history + download),
 * scoped to the authenticated user's own statements (backend-enforced).
 */
export default function StatementsPage() {
  const t = useTranslations('settings.statements');

  return (
    <DashboardLayout>
      <PageTransition>
        <div className="mx-auto max-w-5xl space-y-6 px-4 py-8">
          <header className="space-y-1">
            <h1 className="text-2xl font-semibold tracking-tight">
              {t('generator.title')}
            </h1>
            <p className="text-sm text-muted-foreground">
              {t('generator.subtitle')}
            </p>
          </header>
          <StatementDownloader />
        </div>
      </PageTransition>
    </DashboardLayout>
  );
}
