'use client';

import React, { useState } from 'react';
import {
  Search,
  Trash2,
  Clock,
  MessageSquare,
  Gift,
  ShieldAlert,
  MoreVertical,
} from '@/components/icons';
import { Button, Input, Tag, Card, Space, Row, Col, Typography, List, Empty, Avatar, Segmented } from 'antd';
import DashboardLayout from '@/components/DashboardLayout';
import { useNotifications, useMarkNotificationRead } from '@/hooks';
import { useAuthStore } from '@/stores/authStore';
import { useRouter } from '@/lib/navigation';
import { notify as toast } from '@/lib/notify';

const { Title, Text } = Typography;

export default function NotificationsPage() {
  const { user } = useAuthStore();
  const router = useRouter();
  const userId = user?.id ?? '';
  const [searchTerm, setSearchTerm] = useState('');
  const [filter, setFilter] = useState('ALL');
  const { data: notificationsData } = useNotifications(userId);
  const markRead = useMarkNotificationRead();

  const rawNotifications = Array.isArray(notificationsData) ? notificationsData : [];
  // BUG-CROSS-032: Map backend field names (body/sentAt) to frontend display fields (content/timestamp)
  const notifications = rawNotifications.map((n) => ({
    id: n.id,
    title: n.title ?? '',
    content: n.body ?? '',
    type: n.channel ?? 'IN_APP',
    read: !!n.readAt,
    timestamp: n.sentAt ?? n.createdAt ?? '',
  }));

  const filteredNotifs = notifications.filter((n: { title: string; content: string; type: string; read: boolean }) => {
    const matchesSearch = n.title.toLowerCase().includes(searchTerm.toLowerCase()) ||
                          n.content.toLowerCase().includes(searchTerm.toLowerCase());
    const matchesFilter = filter === 'ALL' || n.type === filter || (filter === 'UNREAD' && !n.read);
    return matchesSearch && matchesFilter;
  });

  const handleMarkAllRead = () => {
    notifications.filter(n => !n.read).forEach(n => markRead.mutate(n.id));
    toast.success('Semua notifikasi telah ditandai dibaca');
  };

  const handleClearAll = () => {
    toast.info('Riwayat notifikasi telah dibersihkan');
  };

  const getIcon = (type: string) => {
    switch (type) {
      case 'PROMO': return <Gift style={{ width: 20, height: 20, color: 'hsl(var(--primary))' }} />;
      case 'ALERT': return <ShieldAlert style={{ width: 20, height: 20, color: 'var(--color-error)' }} />;
      case 'SECURITY': return <Clock style={{ width: 20, height: 20, color: 'var(--color-warning)' }} />;
      default: return <MessageSquare style={{ width: 20, height: 20, color: 'hsl(var(--primary))' }} />;
    }
  };

  return (
    <DashboardLayout>
      <Space direction="vertical" size={24} style={{ width: '100%' }}>
        {/* Header */}
        <Card>
          <Row justify="space-between" align="middle" gutter={[16, 16]}>
            <Col>
              <Space direction="vertical" size={4}>
                <Title level={2} style={{ fontSize: 24, fontWeight: 700, letterSpacing: '-0.025em', margin: 0 }}>Kotak Masuk</Title>
                <Text type="secondary" style={{ fontSize: 14, fontWeight: 500 }}>Kelola notifikasi, promo, dan peringatan keamanan Anda.</Text>
              </Space>
            </Col>
            <Col>
              <Space size={16}>
                <Button
                  type="text"
                  onClick={handleMarkAllRead}
                  style={{ fontSize: 12, fontWeight: 700, letterSpacing: '0.1em', textTransform: 'uppercase' }}
                >
                  Tandai Semua Dibaca
                </Button>
                <Button
                  type="text"
                  danger
                  onClick={handleClearAll}
                  style={{ fontSize: 12, fontWeight: 700, letterSpacing: '0.1em', textTransform: 'uppercase' }}
                >
                  Hapus Semua
                </Button>
              </Space>
            </Col>
          </Row>
        </Card>

        {/* Toolbar */}
        <Card>
          <Row gutter={[16, 16]} align="middle">
            <Col flex="auto">
              <Input
                aria-label="Cari notifikasi"
                placeholder="Cari notifikasi..."
                prefix={<Search style={{ width: 16, height: 16, color: 'hsl(var(--muted-foreground))' }} />}
                style={{ height: 48, fontWeight: 700, letterSpacing: '0.1em', textTransform: 'uppercase' }}
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
              />
            </Col>
            <Col>
              <Segmented
                value={filter}
                onChange={(value) => setFilter(value as string)}
                options={[
                  { label: 'Semua', value: 'ALL' },
                  { label: 'Belum Dibaca', value: 'UNREAD' },
                  { label: 'PROMO', value: 'PROMO' },
                  { label: 'SECURITY', value: 'SECURITY' },
                ]}
              />
            </Col>
          </Row>
        </Card>

        {/* Notifications List */}
        {filteredNotifs.length === 0 ? (
          <Card>
            <Empty
              image={Empty.PRESENTED_IMAGE_SIMPLE}
              description={
                <Text type="secondary" style={{ fontSize: 14, fontWeight: 700, letterSpacing: '0.1em', textTransform: 'uppercase' }}>
                  Tidak ada notifikasi
                </Text>
              }
            />
          </Card>
        ) : (
          <List
            dataSource={filteredNotifs}
            renderItem={(n) => (
              <List.Item
                key={n.id}
                style={{
                  background: 'hsl(var(--card))',
                  border: '1px solid hsl(var(--border))',
                  borderRadius: 12,
                  padding: 24,
                  marginBottom: 16,
                  borderLeft: !n.read ? '4px solid hsl(var(--primary))' : undefined,
                }}
                actions={[
                  <Button
                    key="view"
                    type="link"
                    onClick={() => {
                      if (!n.read) {
                        markRead.mutate(n.id);
                      }
                      router.push(`/notifications/${n.id}`);
                    }}
                    style={{ fontSize: 12, fontWeight: 700, color: 'var(--color-primary-dark)', letterSpacing: '0.1em', textTransform: 'uppercase' }}
                  >
                    Lihat Detail
                  </Button>,
                  <Button
                    key="delete"
                    type="text"
                    shape="circle"
                    size="small"
                    icon={<Trash2 style={{ width: 16, height: 16 }} />}
                    aria-label="Hapus notifikasi"
                  />,
                  <Button
                    key="more"
                    type="text"
                    shape="circle"
                    size="small"
                    icon={<MoreVertical style={{ width: 16, height: 16 }} />}
                    aria-label="Opsi notifikasi"
                  />,
                ]}
              >
                <List.Item.Meta
                  avatar={
                    <Avatar
                      size={56}
                      style={{
                        background: 'hsl(var(--muted))',
                        border: '1px solid hsl(var(--border))',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                      }}
                    >
                      {getIcon(n.type)}
                    </Avatar>
                  }
                  title={
                    <Space size={12} align="center">
                      <Tag bordered={false} color="green" style={{ fontSize: 12, fontWeight: 700, letterSpacing: '0.1em', textTransform: 'uppercase', padding: '0 8px', border: '1px solid hsl(var(--primary) / 0.2)', color: 'hsl(var(--primary))' }}>
                        {n.type}
                      </Tag>
                      <Text type="secondary" style={{ fontSize: 12, fontWeight: 700, letterSpacing: '0.1em', textTransform: 'uppercase' }}>
                        {new Date(n.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                      </Text>
                    </Space>
                  }
                  description={
                    <Space direction="vertical" size={4}>
                      <Text strong style={{ fontSize: 18, color: !n.read ? 'var(--color-primary-dark)' : undefined }}>
                        {n.title}
                      </Text>
                      <Text type="secondary" style={{ fontSize: 14, lineHeight: 1.6 }}>
                        {n.content}
                      </Text>
                    </Space>
                  }
                />
              </List.Item>
            )}
          />
        )}
      </Space>
    </DashboardLayout>
  );
}
