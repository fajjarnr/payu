'use client';

import { theme, Button, Empty, Typography } from 'antd';
import type { ReactNode } from 'react';

export interface EmptyStateProps {
  title: string;
  description: string;
  actionLabel?: string;
  onAction?: () => void;
  icon?: ReactNode;
}

/** Shared empty state: 64px icon + h3 title + body-secondary description + optional CTA. */
export default function EmptyState({
  title,
  description,
  actionLabel,
  onAction,
  icon,
}: EmptyStateProps) {
  const { token } = theme.useToken();
  return (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        textAlign: 'center',
        gap: token.marginSM,
        padding: `${token.paddingLG}px ${token.padding}px`,
      }}
    >
      <div style={{ fontSize: 64, lineHeight: 1, color: token.colorTextSecondary }} aria-hidden="true">
        {icon ?? <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description={false} />}
      </div>
      <Typography.Title level={3} style={{ margin: 0 }}>
        {title}
      </Typography.Title>
      <Typography.Text type="secondary">{description}</Typography.Text>
      {actionLabel && onAction ? (
        <Button type="primary" onClick={onAction}>
          {actionLabel}
        </Button>
      ) : null}
    </div>
  );
}
