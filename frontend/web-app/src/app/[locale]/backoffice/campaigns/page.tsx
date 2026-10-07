'use client';

import React, { useState } from 'react';
import { 
  Plus, 
  Search, 
  Gift, 
  Tag as TagIcon, 
  Users, 
  Calendar, 
  BarChart3, 
  MoreHorizontal, 
  Edit, 
  Copy, 
  CheckCircle2, 
  Timer,
  BadgePercent,
  ChevronLeft,
  ChevronRight
} from '@/components/icons';
import { Badge, Button, Dropdown, Input, Table } from 'antd';
import type { MenuProps, TableColumnsType } from 'antd';
import type { Promotion } from '@/services';
import { useActivePromotions } from '@/hooks/useRewards';


 export default function CampaignsPage() {
  const [searchTerm, setSearchTerm] = useState('');
  const { data: campaigns, isLoading, error } = useActivePromotions();
  const filteredCampaigns = (campaigns ?? []).filter((cmp) => cmp.name.toLowerCase().includes(searchTerm.toLowerCase()));
  const campaignMenuItems: MenuProps['items'] = [
    { key: 'analytics', label: (<span className="flex items-center gap-2 text-xs font-bold uppercase tracking-widest p-3"><BarChart3 className="h-4 w-4 text-primary" />View Analytics</span>) },
    { key: 'end', label: (<span className="flex items-center gap-2 text-xs font-bold uppercase tracking-widest p-3 text-error"><CheckCircle2 className="h-4 w-4" />End Campaign</span>) },
  ];
  const columns: TableColumnsType<Promotion> = [
    {
      key: 'name',
      title: 'Campaign Name',
      render: (_, cmp) => (
        <div className="space-y-1">
          <p className="font-bold text-foreground text-sm leading-tight">{cmp.name}</p>
          <p className="text-xs text-muted-foreground font-bold tracking-widest uppercase">{cmp.id}</p>
          <div className="flex items-center gap-2 mt-2">
            <Calendar className="h-3 w-3 text-muted-foreground" />
            <p className="text-xs text-muted-foreground font-medium">{new Date(cmp.startDate).toLocaleDateString()} - {new Date(cmp.endDate).toLocaleDateString()}</p>
          </div>
        </div>
      ),
    },
    {
      key: 'type',
      title: 'Type',
      render: (_, cmp) => (
        <div className="flex items-center gap-2">
          {getTypeIcon(cmp.type)}
          <span className="text-xs font-bold text-foreground uppercase tracking-widest">{cmp.type.replace('_', ' ')}</span>
        </div>
      ),
    },
    { key: 'status', title: 'Status', render: (_, cmp) => getStatusBadge(cmp.status) },
    { key: 'claims', title: 'Rewards Sent', render: (_, cmp) => <span className="text-xs font-bold text-foreground">{cmp.currentClaims.toLocaleString()}</span> },
    {
      key: 'budget',
      title: 'Budget Spent',
      render: (_, cmp) => (
        <div className="space-y-2">
          <div className="flex justify-between items-center text-xs font-bold uppercase tracking-widest">
            <span className="text-primary">{cmp.value}</span>
            <span className="text-muted-foreground opacity-40">/ {cmp.maxClaims ?? '∞'}</span>
          </div>
          <div className="w-32 bg-muted h-1 rounded-full overflow-hidden">
            <div className="bg-primary h-full" style={{ width: `${cmp.maxClaims ? Math.min(100, (cmp.currentClaims / cmp.maxClaims) * 100) : 0}%` }} />
          </div>
        </div>
      ),
    },
    {
      key: 'actions',
      title: 'Actions',
      align: 'right',
      render: () => (
        <div className="flex items-center justify-end gap-2">
          <Button type="text" className="min-h-[44px] min-w-[44px] rounded-lg hover:bg-muted/50 p-0" icon={<Edit className="h-4 w-4" />} aria-label="Edit kampanye" />
          <Button type="text" className="min-h-[44px] min-w-[44px] rounded-lg hover:bg-muted/50 p-0" icon={<Copy className="h-4 w-4" />} aria-label="Salin kampanye" />
          <Dropdown trigger={['click']} placement="bottomRight" menu={{ items: campaignMenuItems }}>
            <Button type="text" className="min-h-[44px] min-w-[44px] rounded-lg hover:bg-muted/50 p-0" icon={<MoreHorizontal className="h-4 w-4" />} aria-label="Aksi lainnya" />
          </Dropdown>
        </div>
      ),
    },
  ];

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'ACTIVE':
        return <Badge count="Active" color="green" className="uppercase tracking-widest text-xs [&_sup]:bg-primary/10 [&_sup]:text-primary [&_sup]:border [&_sup]:border-primary/20 [&_sup]:px-3 [&_sup]:py-1 [&_sup]:rounded-full [&_sup]:font-semibold" />;
      case 'PAUSED':
        return <Badge count="Paused" color="gold" className="uppercase tracking-widest text-xs [&_sup]:bg-warning/10 [&_sup]:text-warning [&_sup]:border [&_sup]:border-warning/20 [&_sup]:px-3 [&_sup]:py-1 [&_sup]:rounded-full [&_sup]:font-semibold" />;
      case 'DRAFT':
        return <Badge count="Draft" className="uppercase tracking-widest text-xs [&_sup]:bg-text-secondary/10 [&_sup]:text-text-secondary [&_sup]:border [&_sup]:border-text-secondary/20 [&_sup]:px-3 [&_sup]:py-1 [&_sup]:rounded-full [&_sup]:font-semibold" showZero />;
      default:
        return <span className="inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-semibold">{status}</span>;
    }
  };
  const getTypeIcon = (type: string) => {
    switch (type) {
      case 'CASHBACK': return <BadgePercent className="h-4 w-4 text-primary" />;
      case 'SIGNUP_BONUS': return <Gift className="h-4 w-4 text-accent" />;
      case 'REFERRAL': return <Users className="h-4 w-4 text-primary" />;
      default: return <TagIcon className="h-4 w-4 text-text-secondary" />;
    }
  };

   return (
    <div className="space-y-6 lg:space-y-8">
      <>
        <>
          <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
            {[
              { label: 'Total Campaigns', value: isLoading ? '…' : String(campaigns?.length ?? 0), color: 'bg-primary', icon: Gift },
              { label: 'Rewards Sent', value: isLoading ? '…' : String((campaigns ?? []).reduce((s, c) => s + (c.currentClaims ?? 0), 0)), color: 'bg-primary', icon: CheckCircle2 },
              { label: 'Active Campaigns', value: isLoading ? '…' : String((campaigns ?? []).filter((c) => c.status === 'ACTIVE').length), color: 'bg-secondary', icon: Timer },
              { label: 'Draft Campaigns', value: isLoading ? '…' : String((campaigns ?? []).filter((c) => c.status === 'DRAFT').length), color: 'bg-accent', icon: BarChart3 },
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

        {/* Toolbar */}
        <>
          <div className="flex flex-col lg:flex-row justify-between items-start lg:items-center gap-6 bg-card border border-border p-5 sm:p-6 lg:p-8 rounded-2xl shadow-sm">
            <div className="flex items-center gap-4 w-full lg:w-auto">
              <div className="relative flex-1 lg:w-96 flex items-center">
                <Search className="absolute left-4 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground z-10 pointer-events-none" />
                <Input
                  aria-label="Cari kampanye"
                  placeholder="Search campaigns..."
                  className="pl-12 bg-muted/30 border-border h-12 rounded-xl text-xs font-bold uppercase tracking-widest w-full"
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                />
              </div>
            </div>
            
            <div className="flex items-center gap-4 w-full lg:w-auto">
              <Button className="h-12 px-6 rounded-xl border-border bg-card text-xs font-bold tracking-widest uppercase gap-2">
                <BarChart3 className="h-4 w-4" />
                Performance Report
              </Button>
              <Button type="primary" className="h-12 px-6 rounded-xl bg-primary-dark hover:bg-primary text-surface font-bold text-xs tracking-widest uppercase gap-2">
                <Plus className="h-4 w-4" />
                Launch New Campaign
              </Button>
            </div>
          </div>
        </>

        {/* Campaign Table */}
        <>
          <div className="bg-card border border-border rounded-2xl shadow-sm overflow-hidden">
            <Table<Promotion>
              columns={columns}
              dataSource={filteredCampaigns}
              rowKey="id"
              pagination={false}
              loading={isLoading}
              locale={{ emptyText: error ? 'Failed to load campaigns' : 'No campaigns found' }}
            />
            
            <div className="p-6 border-t border-border flex items-center justify-between">
              <p className="text-xs font-bold text-muted-foreground uppercase tracking-widest">
                Showing <span className="text-foreground">{filteredCampaigns.length}</span> campaigns
              </p>
              <div className="flex items-center gap-2">
                <Button className="min-h-[44px] min-w-[44px] rounded-xl border-border hover:bg-muted/50 p-0 flex items-center justify-center" icon={<ChevronLeft className="h-4 w-4" />} aria-label="Halaman sebelumnya" />
                <div className="h-10 px-4 flex items-center justify-center rounded-xl bg-primary text-surface font-bold text-xs">
                  1
                </div>
                <Button className="min-h-[44px] min-w-[44px] rounded-xl border-border hover:bg-muted/50 p-0 flex items-center justify-center" icon={<ChevronRight className="h-4 w-4" />} aria-label="Halaman berikutnya" />
              </div>
            </div>
          </div>
        </>
      </>
    </div>
  );
}
