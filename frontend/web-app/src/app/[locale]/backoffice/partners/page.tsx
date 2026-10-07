'use client';

import React, { useState } from 'react';
import {
  Store,
  Search,
  Plus,
  CheckCircle2,
  AlertCircle,
  Globe,
  ShieldCheck,
  Key,
  Settings,
  MoreHorizontal,
  ChevronLeft,
  ChevronRight,
  ExternalLink,
} from '@/components/icons';
import { Badge, Button, Input, Table } from 'antd';
import type { TableColumnsType } from 'antd';
import { usePartners } from '@/hooks';
import type { Partner } from '@/services';

type PartnerRow = {
  id: number;
  name: string;
  type: string;
  status: 'ACTIVE' | 'UNDER_REVIEW';
  apiLevel: string;
  transactions: string;
  volume: string;
};

function toPartnerRow(partner: Partner): PartnerRow {
  return {
    id: partner.id,
    name: partner.name,
    type: partner.type,
    status: partner.active ? 'ACTIVE' : 'UNDER_REVIEW',
    apiLevel: partner.publicKey ? 'SNAP BI Ready' : 'Pending Setup',
    transactions: '--',
    volume: 'N/A',
  };
}

export default function PartnersPage() {
  const [searchTerm, setSearchTerm] = useState('');
  const { data: partnersData, isLoading } = usePartners();

  const partners = (Array.isArray(partnersData) ? partnersData.map(toPartnerRow) : []).filter((p) => {
    if (!searchTerm) return true;
    const term = searchTerm.toLowerCase();
    return p.name?.toLowerCase().includes(term) || String(p.id ?? '').toLowerCase().includes(term);
  });

  const activeCount = partners.filter(p => p.status === 'ACTIVE').length;
  const pendingCount = partners.filter(p => p.status === 'UNDER_REVIEW').length;

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'ACTIVE':
        return <Badge count="Active" color="green" className="uppercase tracking-widest text-xs [&_sup]:bg-primary/10 [&_sup]:text-primary [&_sup]:border [&_sup]:border-primary/20 [&_sup]:px-3 [&_sup]:py-1 [&_sup]:rounded-full [&_sup]:font-semibold" />;
      case 'UNDER_REVIEW':
        return <Badge count="Reviewing" color="gold" className="uppercase tracking-widest text-xs [&_sup]:bg-warning/10 [&_sup]:text-warning [&_sup]:border [&_sup]:border-warning/20 [&_sup]:px-3 [&_sup]:py-1 [&_sup]:rounded-full [&_sup]:font-semibold" />;
      case 'SUSPENDED':
        return <Badge count="Suspended" color="red" className="uppercase tracking-widest text-xs [&_sup]:bg-error/10 [&_sup]:text-error [&_sup]:border [&_sup]:border-error/20 [&_sup]:px-3 [&_sup]:py-1 [&_sup]:rounded-full [&_sup]:font-semibold" />;
      default:
        return <span className="inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-semibold">{status}</span>;
    }
  };
  const columns: TableColumnsType<PartnerRow> = [
    {
      key: 'org',
      title: 'Partner Org',
      render: (_, partner) => (
        <div className="flex items-center gap-3">
          <div className="h-10 w-10 rounded-xl bg-muted flex items-center justify-center border border-border">
            <Store className="h-5 w-5 text-muted-foreground" />
          </div>
          <div>
            <p className="font-bold text-foreground text-sm leading-tight">{partner.name}</p>
            <p className="text-xs text-muted-foreground font-bold tracking-widest uppercase">{partner.id}</p>
          </div>
        </div>
      ),
    },
    {
      key: 'type',
      title: 'Type',
      render: (_, partner) => <span className="text-xs font-bold text-foreground uppercase tracking-widest">{partner.type}</span>,
    },
    { key: 'status', title: 'Status', render: (_, partner) => getStatusBadge(partner.status) },
    {
      key: 'api',
      title: 'API Integration',
      render: (_, partner) => (
        <span className="inline-flex items-center rounded-full border px-2 py-0.5 text-xs font-semibold border-primary/20 text-primary font-bold uppercase tracking-widest gap-1.5">
          <ShieldCheck className="h-3 w-3" />
          {partner.apiLevel}
        </span>
      ),
    },
    {
      key: 'volume',
      title: 'Volume',
      render: (_, partner) => (
        <div>
          <p className="text-xs font-bold text-foreground">{partner.volume}</p>
          <p className="text-xs text-muted-foreground font-medium">{partner.transactions} txns</p>
        </div>
      ),
    },
    {
      key: 'actions',
      title: 'Actions',
      align: 'right',
      render: () => (
        <div className="flex items-center justify-end gap-2">
          <Button type="text" className="min-h-[44px] min-w-[44px] rounded-lg hover:bg-muted/50 p-0" icon={<ExternalLink className="h-4 w-4" />} aria-label="Buka tautan eksternal" />
          <Button type="text" className="min-h-[44px] min-w-[44px] rounded-lg hover:bg-muted/50 p-0" icon={<Settings className="h-4 w-4" />} aria-label="Pengaturan partner" />
          <Button type="text" className="min-h-[44px] min-w-[44px] rounded-lg hover:bg-muted/50 p-0" icon={<MoreHorizontal className="h-4 w-4" />} aria-label="Aksi lainnya" />
        </div>
      ),
    },
  ];

  return (
    <div className="space-y-6 lg:space-y-8">
      <>
        <>
          <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
            {[
              { label: 'Total Partners', value: isLoading ? '…' : String(partners.length), color: 'bg-primary', icon: Store },
              { label: 'Active Merchants', value: isLoading ? '…' : String(activeCount), color: 'bg-primary', icon: CheckCircle2 },
              { label: 'Pending Apps', value: isLoading ? '…' : String(pendingCount), color: 'bg-warning', icon: AlertCircle },
              { label: 'SNAP BI Volume', value: '—', color: 'bg-secondary', icon: Globe },
            ].map((stat, i) => (
              <div key={i} className="bg-card border border-border p-6 rounded-2xl shadow-sm flex items-center gap-5">
                <div className={`${stat.color} h-12 w-12 rounded-xl flex items-center justify-center text-surface shadow-lg`}>
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

        <>
          <div className="flex flex-col lg:flex-row justify-between items-start lg:items-center gap-6 bg-card border border-border p-5 sm:p-6 lg:p-8 rounded-2xl shadow-sm">
            <div className="flex items-center gap-4 w-full lg:w-auto">
              <div className="relative flex-1 lg:w-96 flex items-center">
                <Search className="absolute left-4 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground z-10 pointer-events-none" />
                <Input
                  aria-label="Cari mitra berdasarkan nama atau ID"
                  placeholder="Search partners by name or ID..."
                  className="pl-12 bg-muted/30 border-border h-12 rounded-xl text-xs font-bold uppercase tracking-widest w-full"
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                />
              </div>
            </div>

            <div className="flex items-center gap-4 w-full lg:w-auto">
              <Button className="h-12 px-6 rounded-xl border-border bg-card text-xs font-bold tracking-widest uppercase gap-2">
                <Key className="h-4 w-4" />
                Manage API Keys
              </Button>
              <Button type="primary" className="h-12 px-6 rounded-xl bg-primary-dark hover:bg-primary text-surface font-bold text-xs tracking-widest uppercase gap-2">
                <Plus className="h-4 w-4" />
                Register New Partner
              </Button>
            </div>
          </div>
        </>

        <>
          <div className="bg-card border border-border rounded-2xl shadow-sm overflow-hidden">
            <Table<PartnerRow>
              columns={columns}
              dataSource={partners}
              rowKey="id"
              pagination={false}
              loading={isLoading}
              locale={{ emptyText: 'No partners found' }}
            />

            <div className="p-6 border-t border-border flex items-center justify-between">
              <p className="text-xs font-bold text-muted-foreground uppercase tracking-widest">
                Partner Portal & SNAP BI Registry Syncing
              </p>
              <div className="flex items-center gap-2">
                <Button className="min-h-[44px] min-w-[44px] rounded-xl border-border hover:bg-muted/50 p-0 flex items-center justify-center" disabled icon={<ChevronLeft className="h-4 w-4" />} aria-label="Halaman sebelumnya" />
                <div className="h-10 px-4 flex items-center justify-center rounded-xl bg-primary text-surface font-bold text-xs">
                  1
                </div>
                <Button className="min-h-[44px] min-w-[44px] rounded-xl border-border hover:bg-muted/50 p-0 flex items-center justify-center" disabled icon={<ChevronRight className="h-4 w-4" />} aria-label="Halaman berikutnya" />
              </div>
            </div>
          </div>
        </>
      </>
    </div>
  );
}
