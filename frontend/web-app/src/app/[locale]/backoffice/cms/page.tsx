'use client';

import React, { useState } from 'react';
import Image from 'next/image';
import { 
  Plus, 
  Search, 
  MoreHorizontal, 
  Edit, 
  Trash2, 
  Eye, 
  CheckCircle2, 
  Clock, 
  AlertCircle,
  Image as ImageIcon,
  ExternalLink,
  ChevronLeft,
  ChevronRight,
  FileText,
  Gift
} from '@/components/icons';
import { Badge, Button, Dropdown, Input, Select, Table } from 'antd';
import type { MenuProps, TableColumnsType } from 'antd';
import type { Content } from '@/services';
import { useActiveContent } from '@/hooks/useCMS';



export default function CMSPage() {
  const [searchTerm, setSearchTerm] = useState('');
  const [activeTab, setActiveTab] = useState<string>('ALL');
  const contentMenuItems: MenuProps['items'] = [
    { key: 'activate', label: (<span className="flex items-center gap-2 text-xs font-bold uppercase tracking-widest p-3"><CheckCircle2 className="h-4 w-4 text-primary" />Activate</span>) },
    { key: 'pause', label: (<span className="flex items-center gap-2 text-xs font-bold uppercase tracking-widest p-3"><Clock className="h-4 w-4 text-warning" />Pause</span>) },
    { key: 'archive', label: (<span className="flex items-center gap-2 text-xs font-bold uppercase tracking-widest p-3 text-error"><Trash2 className="h-4 w-4" />Archive</span>) },
  ];

  const { data: banners, isLoading: bannersLoading, error: bannersError } = useActiveContent('BANNER');
  const { data: promos, isLoading: promosLoading } = useActiveContent('PROMO');
  const { data: alerts, isLoading: alertsLoading } = useActiveContent('ALERT');
  const { data: popups, isLoading: popupsLoading } = useActiveContent('POPUP');
  const isLoading = bannersLoading || promosLoading || alertsLoading || popupsLoading;
  const error = bannersError;
  const allContent = [...(banners ?? []), ...(promos ?? []), ...(alerts ?? []), ...(popups ?? [])];
  const cmsData = allContent;
  const filteredContent = (cmsData ?? []).filter(content => {
    const matchesSearch = content.title.toLowerCase().includes(searchTerm.toLowerCase()) ||
                          content.description.toLowerCase().includes(searchTerm.toLowerCase());
    const matchesTab = activeTab === 'ALL' || content.contentType === activeTab;
    return matchesSearch && matchesTab;
  });
  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'ACTIVE':
        return <Badge count="Active" color="green" className="uppercase tracking-widest text-xs [&_sup]:bg-primary/10 [&_sup]:text-primary [&_sup]:border [&_sup]:border-primary/20 [&_sup]:px-3 [&_sup]:py-1 [&_sup]:rounded-full [&_sup]:font-semibold" />;
      case 'SCHEDULED':
        return <Badge count="Scheduled" color="gold" className="uppercase tracking-widest text-xs [&_sup]:bg-warning/10 [&_sup]:text-warning [&_sup]:border [&_sup]:border-warning/20 [&_sup]:px-3 [&_sup]:py-1 [&_sup]:rounded-full [&_sup]:font-semibold" />;
      case 'DRAFT':
        return <Badge count="Draft" className="uppercase tracking-widest text-xs [&_sup]:bg-text-secondary/10 [&_sup]:text-text-secondary [&_sup]:border [&_sup]:border-text-secondary/20 [&_sup]:px-3 [&_sup]:py-1 [&_sup]:rounded-full [&_sup]:font-semibold" showZero />;
      case 'ARCHIVED':
        return <Badge count="Archived" color="red" className="uppercase tracking-widest text-xs [&_sup]:bg-error/10 [&_sup]:text-error [&_sup]:border [&_sup]:border-error/20 [&_sup]:px-3 [&_sup]:py-1 [&_sup]:rounded-full [&_sup]:font-semibold" />;
      default:
        return <span className="inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-semibold">{status}</span>;
    }
  };

  const getContentTypeIcon = (type: string) => {
    switch (type) {
      case 'BANNER':
        return <ImageIcon className="h-4 w-4 text-primary" />;
      case 'PROMO':
        return <Gift className="h-4 w-4 text-primary" />;
      case 'ALERT':
        return <AlertCircle className="h-4 w-4 text-error" />;
      case 'POPUP':
        return <ExternalLink className="h-4 w-4 text-accent" />;
      default:
        return <FileText className="h-4 w-4 text-text-secondary" />;
    }
  };
  const columns: TableColumnsType<Content> = [
    {
      key: 'content',
      title: 'Content',
      render: (_, item) => (
        <div className="flex items-start gap-4">
          <div className="h-16 w-16 rounded-lg bg-muted flex-shrink-0 overflow-hidden border border-border relative">
            {item.imageUrl ? (
              <Image src={item.imageUrl} alt={item.title} fill sizes="64px" className="object-cover" loading="lazy" />
            ) : (
              <div className="h-full w-full flex items-center justify-center">
                {getContentTypeIcon(item.contentType)}
              </div>
            )}
          </div>
          <div className="space-y-1">
            <p className="font-bold text-foreground text-sm leading-tight">{item.title}</p>
            <p className="text-xs text-muted-foreground line-clamp-2 max-w-[200px]">{item.description}</p>
          </div>
        </div>
      ),
    },
    {
      key: 'type',
      title: 'Type',
      render: (_, item) => (
        <div className="flex items-center gap-2">
          {getContentTypeIcon(item.contentType)}
          <span className="text-xs font-bold text-foreground uppercase tracking-widest">{item.contentType}</span>
        </div>
      ),
    },
    { key: 'status', title: 'Status', render: (_, item) => getStatusBadge(item.status) },
    {
      key: 'schedule',
      title: 'Schedule',
      render: (_, item) => (
        <div className="space-y-1 text-xs">
          <p className="font-medium text-foreground">S: {new Date(item.startDate).toLocaleDateString()}</p>
          <p className="text-muted-foreground font-medium">E: {new Date(item.endDate).toLocaleDateString()}</p>
        </div>
      ),
    },
    {
      key: 'priority',
      title: 'Priority',
      render: (_, item) => (
        <div className="flex items-center gap-2">
          <div className="w-12 bg-muted h-1.5 rounded-full overflow-hidden">
            <div className="bg-primary h-full rounded-full" style={{ width: `${item.priority * 10}%` }} />
          </div>
          <span className="text-xs font-bold text-foreground">{item.priority}</span>
        </div>
      ),
    },
    {
      key: 'actions',
      title: 'Actions',
      align: 'right',
      render: () => (
        <div className="flex items-center justify-end gap-2">
          <Button type="text" className="min-h-[44px] min-w-[44px] rounded-lg hover:bg-muted/50 p-0" icon={<Eye className="h-4 w-4" />} />
          <Button type="text" className="min-h-[44px] min-w-[44px] rounded-lg hover:bg-muted/50 p-0" icon={<Edit className="h-4 w-4" />} />
          <Dropdown trigger={['click']} placement="bottomRight" menu={{ items: contentMenuItems }}>
            <Button type="text" className="min-h-[44px] min-w-[44px] rounded-lg hover:bg-muted/50 p-0" icon={<MoreHorizontal className="h-4 w-4" />} />
          </Dropdown>
        </div>
      ),
    },
  ];

  return (
    <div className="space-y-12">
      <>
        {/* Header Stats */}
        <>
          <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
            {[
              { label: 'Total Content', value: String(allContent.length), color: 'bg-primary', icon: FileText },
              { label: 'Active Now', value: String(allContent.filter((c) => c.status === 'ACTIVE').length), color: 'bg-primary', icon: CheckCircle2 },
              { label: 'Scheduled', value: String(allContent.filter((c) => c.status === 'SCHEDULED').length), color: 'bg-warning', icon: Clock },
              { label: 'Pending Review', value: String(allContent.filter((c) => c.status === 'DRAFT').length), color: 'bg-error', icon: AlertCircle },
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
            <div className="flex flex-wrap items-center gap-3">
              <Select
                aria-label="Pilih tab konten"
                value={activeTab}
                onChange={(value: string) => setActiveTab(value)}
                className="min-w-40"
                options={['ALL', 'BANNER', 'PROMO', 'ALERT', 'POPUP'].map((tab) => ({ value: tab, label: tab }))}
              />
            </div>
            
            <div className="flex items-center gap-4 w-full lg:w-auto">
              <div className="relative flex-1 lg:w-80 flex items-center">
                <Search className="absolute left-4 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground z-10 pointer-events-none" />
                <Input
                  aria-label="Cari konten"
                  placeholder="Search content..."
                  className="pl-12 bg-muted/30 border-border h-12 rounded-xl text-xs font-bold uppercase tracking-widest w-full"
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                />
              </div>
              <Button type="primary" className="h-12 px-6 rounded-xl bg-primary-dark hover:bg-primary text-surface font-bold text-xs tracking-widest uppercase gap-2">
                <Plus className="h-4 w-4" />
                New Content
              </Button>
            </div>
          </div>
        </>

        {/* Content Table */}
        <>
          <div className="bg-card border border-border rounded-2xl shadow-sm overflow-hidden">
            <Table<Content>
              columns={columns}
              dataSource={filteredContent}
              rowKey="id"
              pagination={false}
              loading={isLoading}
              locale={{ emptyText: error ? 'Failed to load content' : 'No content found' }}
            />

            {/* Pagination */}
            <div className="p-6 border-t border-border flex items-center justify-between">
              <p className="text-xs font-bold text-muted-foreground uppercase tracking-widest">
                Showing <span className="text-foreground">{filteredContent.length}</span> results
              </p>
              <div className="flex items-center gap-2">
                <Button className="min-h-[44px] min-w-[44px] rounded-xl border-border hover:bg-muted/50 p-0 flex items-center justify-center" icon={<ChevronLeft className="h-4 w-4" />} />
                <div className="h-10 px-4 flex items-center justify-center rounded-xl bg-primary text-surface font-bold text-xs">
                  1
                </div>
                <Button className="min-h-[44px] min-w-[44px] rounded-xl border-border hover:bg-muted/50 p-0 flex items-center justify-center">2</Button>
                <Button className="min-h-[44px] min-w-[44px] rounded-xl border-border hover:bg-muted/50 p-0 flex items-center justify-center">3</Button>
                <Button className="min-h-[44px] min-w-[44px] rounded-xl border-border hover:bg-muted/50 p-0 flex items-center justify-center" icon={<ChevronRight className="h-4 w-4" />} />
              </div>
            </div>
          </div>
        </>
      </>
    </div>
  );
}
