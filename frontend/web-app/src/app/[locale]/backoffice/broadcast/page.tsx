'use client';

import React, { useState } from 'react';
import {
  Send,
  Search,
  Users,
  MessageSquare,
  Mail,
  Smartphone,
  Bell,
  CheckCircle2,
  AlertCircle,
  ChevronLeft,
  ChevronRight,
  Plus
} from '@/components/icons';
import { Badge, Button, Input, Table } from 'antd';
import type { TableColumnsType } from 'antd';
import type { Notification } from '@/services';
import { useAuthStore } from '@/stores/authStore';
import { useNotifications } from '@/hooks/useNotifications';

export default function BroadcastPage() {
  const [searchTerm, setSearchTerm] = useState('');
  const { accountId } = useAuthStore();
  const { data: notifications, isLoading, error } = useNotifications(accountId ?? '', 20);

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'SENT':
        return <Badge count="Sent" color="green" className="px-3 py-1 uppercase tracking-widest text-xs [&_sup]:bg-emerald-500/10 [&_sup]:text-emerald-500 [&_sup]:border [&_sup]:border-emerald-500/20 [&_sup]:px-3 [&_sup]:py-1 [&_sup]:rounded-full [&_sup]:font-semibold" />;
      case 'SCHEDULED':
        return <Badge count="Scheduled" color="gold" className="px-3 py-1 uppercase tracking-widest text-xs [&_sup]:bg-amber-500/10 [&_sup]:text-amber-500 [&_sup]:border [&_sup]:border-amber-500/20 [&_sup]:px-3 [&_sup]:py-1 [&_sup]:rounded-full [&_sup]:font-semibold" />;
      case 'FAILED':
        return <Badge count="Failed" color="red" className="px-3 py-1 uppercase tracking-widest text-xs [&_sup]:bg-rose-500/10 [&_sup]:text-rose-500 [&_sup]:border [&_sup]:border-rose-500/20 [&_sup]:px-3 [&_sup]:py-1 [&_sup]:rounded-full [&_sup]:font-semibold" />;
      default:
        return <span className="inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-semibold">{status}</span>;
    }
  };

  const getChannelIcon = (channel: string) => {
    switch (channel) {
      case 'PUSH': return <Smartphone className="h-3 w-3" />;
      case 'SMS': return <MessageSquare className="h-3 w-3" />;
      case 'EMAIL': return <Mail className="h-3 w-3" />;
      case 'WHATSAPP': return <Bell className="h-3 w-3 text-emerald-500" />;
      default: return null;
    }
  };

  const filteredBroadcasts = (notifications ?? []).filter((bc) =>
    (bc.title ?? '').toLowerCase().includes(searchTerm.toLowerCase()),
  );

  const columns: TableColumnsType<Notification> = [
    {
      key: 'title',
      title: 'Broadcast Title',
      render: (_, bc) => (
        <div className="space-y-1">
          <p className="font-bold text-foreground text-sm uppercase tracking-tight">{bc.title}</p>
          <div className="flex items-center gap-2">
            <Users className="h-3 w-3 text-muted-foreground" />
            <span className="text-xs text-muted-foreground font-bold uppercase tracking-widest">{bc.recipient}</span>
          </div>
        </div>
      ),
    },
    {
      key: 'channels',
      title: 'Channels',
      render: (_, bc) => (
        <div className="flex items-center gap-1.5">
          <div className="h-7 w-7 rounded-lg bg-muted flex items-center justify-center border border-border" title={bc.channel}>
            {getChannelIcon(bc.channel)}
          </div>
        </div>
      ),
    },
    {
      key: 'status',
      title: 'Status',
      render: (_, bc) => getStatusBadge(bc.status),
    },
    {
      key: 'reach',
      title: 'Reach',
      render: () => <span className="text-xs font-bold text-foreground">—</span>,
    },
    {
      key: 'engagement',
      title: 'Engagement',
      render: () => <span className="text-xs font-bold text-emerald-500">—</span>,
    },
    {
      key: 'date',
      title: 'Date',
      align: 'right',
      render: (_, bc) => (
        <div>
          <p className="text-xs font-medium text-foreground">{new Date(bc.createdAt).toLocaleTimeString()}</p>
          <p className="text-xs text-muted-foreground font-bold uppercase tracking-widester mt-0.5">{new Date(bc.createdAt).toLocaleDateString()}</p>
        </div>
      ),
    },
  ];

  return (
    <div className="space-y-6 lg:space-y-8">
      <>
        {/* Header Stats */}
        <>
          <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
            {[
              { label: 'Broadcasts Sent', value: isLoading ? '…' : String(notifications?.length ?? 0), color: 'bg-emerald-500', icon: Send },
              { label: 'Total Messages', value: '—', color: 'bg-blue-500', icon: Smartphone },
              { label: 'Avg Open Rate', value: '—', color: 'bg-indigo-500', icon: CheckCircle2 },
              { label: 'Unsubscribe Rate', value: '—', color: 'bg-rose-500', icon: AlertCircle },
            ].map((stat, i) => (
              <div key={i} className="bg-card border border-border p-6 rounded-2xl shadow-sm flex items-center gap-5">
                <div className={`${stat.color} h-12 w-12 rounded-xl flex items-center justify-center text-white shadow-lg`}>
                  <stat.icon className="h-6 w-6" />
                </div>
                <div>
                  <p className="text-xs font-bold text-muted-foreground uppercase tracking-widest">{stat.label}</p>
                  <p className="text-2xl font-bold text-foreground mt-0.5">{stat.value}</p>
                </div>
              </div>
            ))}
          </div>
        </>

        {/* Toolbar */}
        <>
          <div className="flex flex-col lg:flex-row justify-between items-start lg:items-center gap-6 bg-card border border-border p-5 sm:p-6 lg:p-8 rounded-2xl shadow-sm">
            <div className="flex items-center gap-4 w-full lg:w-auto">
              <div className="relative flex-1 lg:w-96 flex items-center">
                <Search className="absolute left-4 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground z-10 pointer-events-none" />
                <Input
                  placeholder="Search broadcasts..."
                  className="pl-12 bg-muted/30 border-border h-12 rounded-xl text-xs font-bold uppercase tracking-widest w-full"
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                />
              </div>
            </div>

            <div className="flex items-center gap-4 w-full lg:w-auto">
              <Button className="h-12 px-6 rounded-xl border-border bg-card text-xs font-bold tracking-widest uppercase gap-2">
                <Users className="h-4 w-4" />
                Targeting Rules
              </Button>
              <Button type="primary" className="h-12 px-6 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white font-bold text-xs tracking-widest uppercase gap-2">
                <Plus className="h-4 w-4" />
                Create Broadcast
              </Button>
            </div>
          </div>
        </>

        {/* Broadcast Table */}
        <>
          <div className="bg-card border border-border rounded-2xl shadow-sm overflow-hidden">
            <Table<Notification>
              columns={columns}
              dataSource={filteredBroadcasts}
              rowKey="id"
              pagination={false}
              loading={isLoading}
              locale={{ emptyText: error ? 'Failed to load broadcasts' : 'No broadcasts found' }}
            />

            <div className="p-6 border-t border-border flex items-center justify-between">
              <p className="text-xs font-bold text-muted-foreground uppercase tracking-widest">
                Multi-channel Delivery Engine Active
              </p>
              <div className="flex items-center gap-2">
                <Button className="h-10 w-10 rounded-xl border-border hover:bg-muted/50 p-0 flex items-center justify-center" disabled icon={<ChevronLeft className="h-4 w-4" />} />
                <div className="h-10 px-4 flex items-center justify-center rounded-xl bg-emerald-500 text-white font-bold text-xs">
                  1
                </div>
                <Button className="h-10 w-10 rounded-xl border-border hover:bg-muted/50 p-0 flex items-center justify-center" disabled icon={<ChevronRight className="h-4 w-4" />} />
              </div>
            </div>
          </div>
        </>
      </>
    </div>
  );
}
