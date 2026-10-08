'use client';

import { useTranslations } from 'next-intl';
import { Link } from '@/lib/navigation';
import { FileQuestion } from '@/components/icons';
import { Button, Card, Result, Space, Typography } from 'antd';

/**
 * QAMVP-019: 404 page for unknown routes.
 */
export default function NotFoundPage() {
  const t = useTranslations('errors');

  return (
    <div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 32 }}>
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
}
