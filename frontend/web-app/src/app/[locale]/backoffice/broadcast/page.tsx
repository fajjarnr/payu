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
import { useAuthStore } from '@/stores/authStore';
import { useNotifications } from '@/hooks/useNotifications';
import { Skeleton } from '@/components/ui/skeleton';


export default function BroadcastPage() {
  const [searchTerm, setSearchTerm] = useState('');
  const { accountId } = useAuthStore();
  const { data: notifications, isLoading, error } = useNotifications(accountId ?? '', 20);

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'SENT':
        return <Badge className="bg-emerald-500/10 text-emerald-500 border-emerald-500/20 px-3 py-1 uppercase tracking-widest text-xs gap-1.5 flex items-center justify-center w-fit">Sent</Badge>;
      case 'SCHEDULED':
        return <Badge className="bg-amber-500/10 text-amber-500 border-amber-500/20 px-3 py-1 uppercase tracking-widest text-xs gap-1.5 flex items-center justify-center w-fit">Scheduled</Badge>;
      case 'FAILED':
        return <Badge className="bg-rose-500/10 text-rose-500 border-rose-500/20 px-3 py-1 uppercase tracking-widest text-xs gap-1.5 flex items-center justify-center w-fit">Failed</Badge>;
      default:
        return <Badge variant="outline">{status}</Badge>;
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

  return (
    <div className="space-y-12">
      <StaggerContainer>
        {/* Header Stats */}
        <StaggerItem>
          <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
            {[
              { label: 'Broadcasts Sent', value: '—', color: 'bg-emerald-500', icon: Send },
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
        </StaggerItem>

        {/* Toolbar */}
        <StaggerItem>
          <div className="flex flex-col lg:flex-row justify-between items-start lg:items-center gap-6 bg-card border border-border p-5 sm:p-6 lg:p-8 rounded-2xl shadow-sm">
            <div className="flex items-center gap-4 w-full lg:w-auto">
              <div className="relative flex-1 lg:w-96">
                <Search className="absolute left-4 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                <Input 
                  placeholder="Search broadcasts..." 
                  className="pl-12 bg-muted/30 border-border h-12 rounded-xl text-xs font-bold uppercase tracking-widest"
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                />
              </div>
            </div>
            
            <div className="flex items-center gap-4 w-full lg:w-auto">
              <Button variant="outline" className="h-12 px-6 rounded-xl border-border bg-card text-xs font-bold tracking-widest uppercase gap-2">
                <Users className="h-4 w-4" />
                Targeting Rules
              </Button>
              <Button className="h-12 px-6 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white font-bold text-xs tracking-widest uppercase gap-2">
                <Plus className="h-4 w-4" />
                Create Broadcast
              </Button>
            </div>
          </div>
        </StaggerItem>

        {/* Broadcast Table */}
        <StaggerItem>
          <div className="bg-card border border-border rounded-2xl shadow-sm overflow-hidden">
            <Table>
              <TableHeader className="bg-muted/30">
                <TableRow className="border-border">
                  <TableHead className="w-[300px] text-xs font-bold uppercase tracking-widest p-6">Broadcast Title</TableHead>
                  <TableHead className="text-xs font-bold uppercase tracking-widest px-6">Channels</TableHead>
                  <TableHead className="text-xs font-bold uppercase tracking-widest px-6">Status</TableHead>
                  <TableHead className="text-xs font-bold uppercase tracking-widest px-6">Reach</TableHead>
                  <TableHead className="text-xs font-bold uppercase tracking-widest px-6">Engagement</TableHead>
                  <TableHead className="text-xs font-bold uppercase tracking-widest px-6 text-right">Date</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {isLoading ? (
                  <TableRow><TableCell colSpan={6} className="p-6 text-center"><Skeleton className="h-6 w-full" /></TableCell></TableRow>
                ) : error ? (
                  <TableRow><TableCell colSpan={6} className="p-6 text-center text-destructive">Failed to load broadcasts</TableCell></TableRow>
                ) : !notifications || notifications.length === 0 ? (
                  <TableRow><TableCell colSpan={6} className="p-6 text-center text-muted-foreground">No broadcasts found</TableCell></TableRow>
                ) : (
                  notifications.filter((bc: any) => bc.title?.toLowerCase().includes(searchTerm.toLowerCase())).map((bc: any) => (
                  <TableRow key={bc.id} className="border-border hover:bg-muted/10 transition-colors">
                    <TableCell className="p-6">
                      <div className="space-y-1">
                        <p className="font-bold text-foreground text-sm uppercase tracking-tight">{bc.title}</p>
                        <div className="flex items-center gap-2">
                          <Users className="h-3 w-3 text-muted-foreground" />
                          <span className="text-xs text-muted-foreground font-bold uppercase tracking-widest">{bc.audience}</span>
                        </div>
                      </div>
                    </TableCell>
                    <TableCell className="px-6">
                      <div className="flex items-center gap-1.5">
                        {bc.channels.map((ch: string) => (
                          <div key={ch} className="h-7 w-7 rounded-lg bg-muted flex items-center justify-center border border-border" title={ch}>
                            {getChannelIcon(ch)}
                          </div>
                        ))}
                      </div>
                    </TableCell>
                    <TableCell className="px-6">
                      {getStatusBadge(bc.status)}
                    </TableCell>
                    <TableCell className="px-6 text-xs font-bold text-foreground">
                      {bc.sentCount}
                    </TableCell>
                    <TableCell className="px-6 text-xs font-bold text-emerald-500">
                      {bc.openRate}
                    </TableCell>
                    <TableCell className="text-right p-6">
                      <p className="text-xs font-medium text-foreground">{new Date(bc.timestamp).toLocaleTimeString()}</p>
                      <p className="text-xs text-muted-foreground font-bold uppercase tracking-widester mt-0.5">{new Date(bc.timestamp).toLocaleDateString()}</p>
                    </TableCell>
                  </TableRow>
                )))}
              </TableBody>
            </Table>
            
            <div className="p-6 border-t border-border flex items-center justify-between">
              <p className="text-xs font-bold text-muted-foreground uppercase tracking-widest">
                Multi-channel Delivery Engine Active
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
