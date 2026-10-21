'use client';

import { useEffect } from 'react';
import { theme, Button, Card, Space, Typography } from 'antd';
import { useTranslations } from 'next-intl';

export interface RouteErrorProps {
  error: Error & { digest?: string };
  reset: () => void;
}

/** Shared error-boundary fallback: same shape as dashboard/error.tsx, token-driven. */
export default function RouteError({ error, reset }: RouteErrorProps) {
  const { token } = theme.useToken();
  const t = useTranslations('common');
  useEffect(() => {
    // eslint-disable-next-line no-console
    console.error('[RouteError]', error);
  }, [error]);
  return (
    <div
      style={{
        display: 'flex',
        minHeight: '60vh',
        alignItems: 'center',
        justifyContent: 'center',
        padding: token.paddingLG,
      }}
    >
      <Card>
        <Space direction="vertical" size={token.margin} style={{ textAlign: 'center', width: '100%' }}>
          <Typography.Title level={2}>{t('error')}</Typography.Title>
          <Typography.Text type="secondary">
            {error.message || t('error')}
          </Typography.Text>
          <Button type="primary" block onClick={reset}>
            {t('retry')}
          </Button>
        </Space>
      </Card>
    </div>
  );
}
