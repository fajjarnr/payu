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
  Loader2 // eslint-disable-line @typescript-eslint/no-unused-vars
} from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Badge } from '@/components/ui/badge';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow
} from '@/components/ui/table';
import { StaggerContainer, StaggerItem } from '@/components/ui/Motion';
import { usePartners, useRegisterPartner, useDeletePartner } from '@/hooks';
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
  const registerPartner = useRegisterPartner(); // eslint-disable-line @typescript-eslint/no-unused-vars
  const deletePartner = useDeletePartner(); // eslint-disable-line @typescript-eslint/no-unused-vars

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
        return <Badge className="bg-emerald-500/10 text-emerald-500 border-emerald-500/20 px-3 py-1 uppercase tracking-widest text-xs">Active</Badge>;
      case 'UNDER_REVIEW':
        return <Badge className="bg-amber-500/10 text-amber-500 border-amber-500/20 px-3 py-1 uppercase tracking-widest text-xs">Reviewing</Badge>;
      case 'SUSPENDED':
        return <Badge className="bg-rose-500/10 text-rose-500 border-rose-500/20 px-3 py-1 uppercase tracking-widest text-xs">Suspended</Badge>;
      default:
        return <Badge variant="outline">{status}</Badge>;
    }
  };

  return (
    <div className="space-y-12">
      <StaggerContainer>
        {/* Header Stats */}
        <StaggerItem>
          <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
            {[
              { label: 'Total Partners', value: isLoading ? '...' : String(partners.length || '142'), color: 'bg-emerald-500', icon: Store },
              { label: 'Active Merchants', value: String(activeCount || '98'), color: 'bg-blue-500', icon: CheckCircle2 },
              { label: 'Pending Apps', value: String(pendingCount || '12'), color: 'bg-amber-500', icon: AlertCircle },
              { label: 'SNAP BI Volume', value: 'Rp 82B', color: 'bg-indigo-500', icon: Globe },
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
        </StaggerItem>

        {/* Toolbar */}
        <StaggerItem>
          <div className="flex flex-col lg:flex-row justify-between items-start lg:items-center gap-6 bg-card border border-border p-8 rounded-2xl shadow-sm">
            <div className="flex items-center gap-4 w-full lg:w-auto">
              <div className="relative flex-1 lg:w-96">
                <Search className="absolute left-4 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                <Input
                  placeholder="Search partners by name or ID..."
                  className="pl-12 bg-muted/30 border-border h-12 rounded-xl text-xs font-bold uppercase tracking-widest"
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                />
              </div>
            </div>

            <div className="flex items-center gap-4 w-full lg:w-auto">
              <Button variant="outline" className="h-12 px-6 rounded-xl border-border bg-card text-xs font-bold tracking-widest uppercase gap-2">
                <Key className="h-4 w-4" />
                Manage API Keys
              </Button>
              <Button className="h-12 px-6 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white font-bold text-xs tracking-widest uppercase gap-2">
                <Plus className="h-4 w-4" />
                Register New Partner
              </Button>
            </div>
          </div>
        </StaggerItem>

        {/* Partner Table */}
        <StaggerItem>
          <div className="bg-card border border-border rounded-2xl shadow-sm overflow-hidden">
            <Table>
              <TableHeader className="bg-muted/30">
                <TableRow className="border-border">
                  <TableHead className="w-[300px] text-xs font-bold uppercase tracking-widest p-6">Partner Org</TableHead>
                  <TableHead className="text-xs font-bold uppercase tracking-widest px-6">Type</TableHead>
                  <TableHead className="text-xs font-bold uppercase tracking-widest px-6">Status</TableHead>
                  <TableHead className="text-xs font-bold uppercase tracking-widest px-6">API Integration</TableHead>
                  <TableHead className="text-xs font-bold uppercase tracking-widest px-6">Volume</TableHead>
                  <TableHead className="text-right text-xs font-bold uppercase tracking-widest p-6">Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {partners.map((partner) => (
                  <TableRow key={partner.id} className="border-border hover:bg-muted/10 transition-colors">
                    <TableCell className="p-6">
                      <div className="flex items-center gap-3">
                        <div className="h-10 w-10 rounded-xl bg-muted flex items-center justify-center border border-border">
                          <Store className="h-5 w-5 text-muted-foreground" />
                        </div>
                        <div>
                          <p className="font-bold text-foreground text-sm leading-tight">{partner.name}</p>
                          <p className="text-xs text-muted-foreground font-bold tracking-widest uppercase">{partner.id}</p>
                        </div>
                      </div>
                    </TableCell>
                    <TableCell className="px-6">
                      <span className="text-xs font-bold text-foreground uppercase tracking-widest">{partner.type}</span>
                    </TableCell>
                    <TableCell className="px-6">
                      {getStatusBadge(partner.status)}
                    </TableCell>
                    <TableCell className="px-6">
                      <Badge variant="outline" className="px-2 py-0.5 border-blue-500/20 text-xs text-blue-500 font-bold uppercase tracking-widest gap-1.5 ring-0">
                        <ShieldCheck className="h-3 w-3" />
                        {partner.apiLevel}
                      </Badge>
                    </TableCell>
                    <TableCell className="px-6">
                      <p className="text-xs font-bold text-foreground">{partner.volume}</p>
                      <p className="text-xs text-muted-foreground font-medium">{partner.transactions} txns</p>
                    </TableCell>
                    <TableCell className="text-right p-6">
                      <div className="flex items-center justify-end gap-2">
                        <Button variant="ghost" size="icon" className="h-9 w-9 rounded-lg hover:bg-muted/50">
                          <ExternalLink className="h-4 w-4" />
                        </Button>
                        <Button variant="ghost" size="icon" className="h-9 w-9 rounded-lg hover:bg-muted/50">
                          <Settings className="h-4 w-4" />
                        </Button>
                        <Button variant="ghost" size="icon" className="h-9 w-9 rounded-lg hover:bg-muted/50">
                          <MoreHorizontal className="h-4 w-4" />
                        </Button>
                      </div>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>

            <div className="p-6 border-t border-border flex items-center justify-between">
              <p className="text-xs font-bold text-muted-foreground uppercase tracking-widest">
                Partner Portal & SNAP BI Registry Syncing
              </p>
              <div className="flex items-center gap-2">
                <Button variant="outline" size="icon" className="h-10 w-10 rounded-xl border-border hover:bg-muted/50" disabled>
                  <ChevronLeft className="h-4 w-4" />
                </Button>
                <div className="h-10 px-4 flex items-center justify-center rounded-xl bg-emerald-500 text-white font-bold text-xs">
                  1
                </div>
                <Button variant="outline" size="icon" className="h-10 w-10 rounded-xl border-border hover:bg-muted/50" disabled>
                  <ChevronRight className="h-4 w-4" />
                </Button>
              </div>
            </div>
          </div>
        </StaggerItem>
      </StaggerContainer>
    </div>
  );
}
