'use client';

import { useEffect } from 'react';
import { theme, Alert, Button, Card, Space, Typography } from 'antd';
import { useTranslations } from 'next-intl';
import { ArrowLeft, Home, RefreshCw } from '@/components/icons';

export interface RouteErrorProps {
  error: Error & { digest?: string };
  reset: () => void;
}

/**
 * Shared route error fallback: one localized, token-driven screen for every
 * route segment (`error.tsx` re-exports this). Keeps the retry / back / home
 * affordances the previous per-route copies had, in one place.
 */
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
      <Card style={{ maxWidth: 448, width: '100%' }}>
        <Space direction="vertical" size={token.margin} style={{ textAlign: 'center', width: '100%' }}>
          <Typography.Title level={2} style={{ margin: 0 }}>
            {t('error')}
          </Typography.Title>
          <Typography.Text type="secondary">{t('errorDescription')}</Typography.Text>

          {process.env.NODE_ENV === 'development' && (
            <Alert
              type="error"
              message={
                <Space direction="vertical" size={4}>
                  <Typography.Text strong style={{ fontSize: 12 }}>
                    {t('errorDetails')}
                  </Typography.Text>
                  <Typography.Text
                    type="danger"
                    style={{ fontSize: 12, wordBreak: 'break-word' }}
                  >
                    {error.message}
                  </Typography.Text>
                  {error.digest && (
                    <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                      Digest: {error.digest}
                    </Typography.Text>
                  )}
                </Space>
              }
            />
          )}

          <Button type="primary" block onClick={reset}>
            <RefreshCw style={{ marginRight: 8 }} aria-hidden="true" />
            {t('retry')}
          </Button>
          <Space size={token.marginSM} style={{ width: '100%' }}>
            <Button block onClick={() => window.history.back()}>
              <ArrowLeft style={{ marginRight: 8 }} aria-hidden="true" />
              {t('back')}
            </Button>
            <Button
              block
              onClick={() => {
                const pathLocale = window.location.pathname.match(/^\/(en|id)(\/|$)/);
                const locale = pathLocale ? pathLocale[1] : 'id';
                window.location.href = `/${locale}/dashboard`;
              }}
            >
              <Home style={{ marginRight: 8 }} aria-hidden="true" />
              {t('home')}
            </Button>
          </Space>

          <Typography.Text type="secondary" style={{ fontSize: 12 }}>
            {t('errorSupport')}
          </Typography.Text>
        </Space>
      </Card>
    </div>
  );
}
