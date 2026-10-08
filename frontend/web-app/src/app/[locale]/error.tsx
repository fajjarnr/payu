'use client';

import { useEffect } from 'react';
import { Button, Card, Space, Typography, Alert } from 'antd';
import { AlertTriangle, RefreshCw, Home, ArrowLeft, Bug } from '@/components/icons';

interface ErrorProps {
  error: Error & { digest?: string };
  reset: () => void;
}

export default function Error({ error, reset }: ErrorProps) {
  useEffect(() => {
    console.error('[Route Error Boundary]', error);
  }, [error]);

  const handleGoHome = () => {
    const pathLocale = window.location.pathname.match(/^\/(en|id)(\/|$)/);
    const locale = pathLocale ? pathLocale[1] : 'id';
    window.location.href = `/${locale}/dashboard`;
  };

  const handleGoBack = () => {
    window.history.back();
  };

  const handleReset = () => {
    reset();
  };

  return (
    <div style={{ minHeight: '60vh', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 24 }}>
      <Card style={{ maxWidth: 448, width: '100%', textAlign: 'center' }}>
        <Space direction="vertical" size={16} style={{ width: '100%' }}>
          <AlertTriangle style={{ fontSize: 40, color: '#FF4757' }} aria-hidden="true" />
          <Typography.Title level={2}>Terjadi Kesalahan</Typography.Title>
          <Typography.Text type="secondary">
            Maaf, terjadi kesalahan yang tidak terduga saat memuat halaman ini. Silakan coba lagi atau kembali ke beranda.
          </Typography.Text>

          {process.env.NODE_ENV === 'development' && (
            <Alert
              type="error"
              message={
                <Space direction="vertical" size={4}>
                  <Space size={8}>
                    <Bug style={{ fontSize: 12 }} />
                    <Typography.Text strong style={{ fontSize: 12 }}>Detail Teknis</Typography.Text>
                  </Space>
                  <Typography.Text type="danger" style={{ fontSize: 12, wordBreak: 'break-word' }}>
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

          <Space direction="vertical" size={12} style={{ width: '100%' }}>
            <Button type="primary" block onClick={handleReset}>
              <RefreshCw style={{ marginRight: 8 }} aria-hidden="true" />
              Coba Lagi
            </Button>
            <Space size={12} style={{ width: '100%' }}>
              <Button block onClick={handleGoBack}>
                <ArrowLeft style={{ marginRight: 8 }} aria-hidden="true" />
                Kembali
              </Button>
              <Button block onClick={handleGoHome}>
                <Home style={{ marginRight: 8 }} aria-hidden="true" />
                Beranda
              </Button>
            </Space>
          </Space>

          <Typography.Text type="secondary" style={{ fontSize: 12, marginTop: 32, display: 'block' }}>
            Masalah berlanjut? Hubungi tim dukungan kami.
          </Typography.Text>
        </Space>
      </Card>
    </div>
  );
}
