'use client';

import { Card } from 'antd';
import type { ReactNode } from 'react';

export interface DataTableCardProps {
  title?: ReactNode;
  extra?: ReactNode;
  children: ReactNode;
}

/** Shared backoffice table shell: zero body padding + max-content horizontal scroll. */
export default function DataTableCard({ title, extra, children }: DataTableCardProps) {
  return (
    <Card title={title} extra={extra} styles={{ body: { padding: 0, overflow: 'auto' } }}>
      <div style={{ overflowX: 'auto' }}>{children}</div>
    </Card>
  );
}
