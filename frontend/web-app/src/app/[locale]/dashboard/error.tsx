'use client';

import { useEffect } from 'react';
import { Button, Card, Space, Typography } from 'antd';

export default function Error({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    console.error('[ErrorBoundary: dashboard]', error);
  }, [error]);

  return (
    <div style={{ display: 'flex', minHeight: '60vh', alignItems: 'center', justifyContent: 'center', padding: 16 }}>
      <Card>
        <Space direction="vertical" size={16} style={{ textAlign: 'center', width: '100%' }}>
          <Typography.Title level={2}>Something went wrong</Typography.Title>
          <Typography.Text type="secondary">
            {error.message || 'An unexpected error occurred. Please try again.'}
          </Typography.Text>
          <Button type="primary" block onClick={() => reset()}>
            Try again
          </Button>
        </Space>
      </Card>
    </div>
  );
}
