'use client';

import type { ComponentType, CSSProperties } from 'react';
import { Card, Col, Row, Space, Typography } from 'antd';

export interface StatItem {
  label: string;
  value: string;
  icon: ComponentType<{ style?: CSSProperties }>;
}

const labelStyle: CSSProperties = {
  fontSize: 12,
  fontWeight: 700,
  color: 'var(--ant-color-text-secondary)',
  textTransform: 'uppercase',
  letterSpacing: '0.1em',
};

export default function StatCards({ stats }: { stats: StatItem[] }) {
  return (
    <Row gutter={[24, 24]}>
      {stats.map((stat, i) => (
        <Col key={i} xs={24} md={12} lg={6}>
          <Card>
            <Space size={20} align="center">
              <div
                style={{
                  width: 48,
                  height: 48,
                  borderRadius: 12,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  background: 'var(--ant-color-primary-bg)',
                  color: 'var(--ant-color-primary)',
                }}
              >
                <stat.icon style={{ fontSize: 24 }} />
              </div>
              <Space direction="vertical" size={2}>
                <Typography.Text strong style={labelStyle}>{stat.label}</Typography.Text>
                <Typography.Title level={3} style={{ margin: 0 }}>{stat.value}</Typography.Title>
              </Space>
            </Space>
          </Card>
        </Col>
      ))}
    </Row>
  );
}
