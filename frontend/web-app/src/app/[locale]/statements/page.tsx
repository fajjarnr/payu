'use client';

import React from 'react';
import { useTranslations } from 'next-intl';
import { Space, Typography } from 'antd';
import DashboardLayout from '@/components/DashboardLayout';
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
      <Space direction="vertical" size={16} style={{ maxWidth: 1024, margin: '0 auto', padding: '32px 16px', width: '100%' }}>
        <Space direction="vertical" size={4}>
          <Typography.Title level={2} style={{ marginBottom: 0 }}>
            {t('generator.title')}
          </Typography.Title>
          <Typography.Text type="secondary">
            {t('generator.subtitle')}
          </Typography.Text>
        </Space>
        <StatementDownloader />
      </Space>
    </DashboardLayout>
  );
}
