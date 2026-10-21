'use client';

import { theme, Skeleton, Space, Spin } from 'antd';
import { useTranslations } from 'next-intl';

export interface LoadingStateProps {
  /** `skeleton` matches dashboard page shape; `spinner` matches legal "Memuat" shape. */
  variant?: 'skeleton' | 'spinner';
}

/** Shared loading UI: dashboard skeleton or legal spinner, both token-driven. */
export function LoadingState({ variant = 'skeleton' }: LoadingStateProps) {
  const { token } = theme.useToken();
  const t = useTranslations('common');
  if (variant === 'spinner') {
    return (
      <div
        role="status"
        aria-live="polite"
        style={{
          display: 'flex',
          minHeight: '60vh',
          alignItems: 'center',
          justifyContent: 'center',
        }}
      >
        <Space direction="vertical" size={token.margin} align="center">
          <Spin size="large" />
          <span style={{ color: token.colorTextSecondary }}>{t('loading')}</span>
        </Space>
      </div>
    );
  }
  return (
    // The shell renders its content in a 1200px column with 16/24px padding —
    // the skeleton uses the same box so the page does not jump on load.
    <div
      aria-busy="true"
      aria-live="polite"
      style={{
        display: 'flex',
        flexDirection: 'column',
        gap: token.margin,
        maxWidth: 1200,
        margin: '0 auto',
        padding: token.padding,
      }}
    >
      <Skeleton active paragraph={{ rows: 1, width: '40%' }} title={false} />
      <Skeleton.Button active block style={{ height: 120 }} />
      <Skeleton active paragraph={{ rows: 3 }} title={false} />
    </div>
  );
}

export default LoadingState;
