'use client';

import { theme, Typography } from 'antd';
import type { ReactNode } from 'react';

const { Title, Text } = Typography;

export interface PageHeaderProps {
  title: string;
  subtitle?: string;
  actions?: ReactNode;
}

/** Shared page heading: L1 title + optional subtitle + optional actions, stacked on mobile. */
export default function PageHeader({ title, subtitle, actions }: PageHeaderProps) {
  const { token } = theme.useToken();
  return (
    <div
      style={{
        display: 'flex',
        flexWrap: 'wrap',
        gap: token.marginSM,
        alignItems: 'flex-start',
        justifyContent: 'space-between',
        marginBottom: token.marginLG,
      }}
    >
      <div style={{ minWidth: 0, flex: '1 1 240px' }}>
        <Title level={1} style={{ margin: 0, fontSize: token.fontSizeHeading1 }}>
          {title}
        </Title>
        {subtitle ? <Text type="secondary">{subtitle}</Text> : null}
      </div>
      {actions ? (
        <div
          style={{
            display: 'flex',
            gap: token.marginSM,
            flexWrap: 'wrap',
            width: '100%',
          }}
          className="page-header-actions"
        >
          {actions}
        </div>
      ) : null}
      <style>{`@media (min-width: 640px) { .page-header-actions { width: auto !important; } }`}</style>
    </div>
  );
}
