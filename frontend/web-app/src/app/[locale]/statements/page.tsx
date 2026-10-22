'use client';

import React from 'react';
import { useTranslations } from 'next-intl';
import { Space } from 'antd';
import DashboardLayout from '@/components/DashboardLayout';
import PageHeader from '@/components/PageHeader';
import StatementDownloader from '@/components/settings/statement-downloader';
import { SkipLink } from "@/lib/a11y";

/**
 * QAMVP-019: dedicated statement page.
 * Reuses the statement downloader component (generation + history + download),
 * scoped to the authenticated user's own statements (backend-enforced).
 */
export default function StatementsPage() {
  const t = useTranslations('settings.statements');

  return (
    <DashboardLayout>
      <SkipLink />
      <Space orientation="vertical" size={16} style={{ width: '100%' }}>
        <PageHeader title={t('pageTitle')} subtitle={t('pageSubtitle')} />
        <StatementDownloader />
      </Space>
    </DashboardLayout>
  );
}
