'use client';

import { useTranslations } from 'next-intl';
import { usePathname } from 'next/navigation';
import { Link } from '@/lib/navigation';
import { FileQuestion } from '@/components/icons';
import { Button, Card, Result, Space, Typography } from 'antd';
import DashboardLayout from '@/components/DashboardLayout';
import { usesAppShell } from '@/lib/app-shell-routes';

/**
 * QAMVP-019: 404 page for unknown routes.
 */
export default function NotFoundPage() {
  const t = useTranslations('errors');
  const pathname = usePathname();

  const card = (
    <div style={{ minHeight: '60vh', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 32 }}>
      <Card style={{ width: '100%', maxWidth: 420 }}>
        <Result
          icon={<FileQuestion style={{ width: 64, height: 64, color: 'var(--ant-color-text-tertiary)' }} />}
          title={<Typography.Title level={2} style={{ margin: 0 }}>404</Typography.Title>}
          subTitle={<Typography.Text type="secondary">{t('notFound')}</Typography.Text>}
          extra={
            <Link href="/dashboard">
              <Button type="primary" size="large" block>
                Kembali ke Dasbor
              </Button>
            </Link>
          }
        />
      </Card>
    </div>
  );

  // Same reason as the error boundary: the 404 replaces the page, so without
  // the shell the app frame disappears on a mistyped URL.
  if (!usesAppShell(pathname)) return card;
  return <DashboardLayout>{card}</DashboardLayout>;
}
