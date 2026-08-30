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
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { StaggerContainer, StaggerItem } from '@/components/ui/Motion';
import { useActivePromotions } from '@/hooks/useRewards';
import { Skeleton } from '@/components/ui/skeleton';



export default function CampaignsPage() {
  const [searchTerm, setSearchTerm] = useState('');
  const { data: campaigns, isLoading, error } = useActivePromotions();

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'ACTIVE':
        return <Badge className="bg-emerald-500/10 text-emerald-500 border-emerald-500/20 px-3 py-1 uppercase tracking-widest text-xs">Active</Badge>;
      case 'PAUSED':
        return <Badge className="bg-amber-500/10 text-amber-500 border-amber-500/20 px-3 py-1 uppercase tracking-widest text-xs">Paused</Badge>;
      case 'DRAFT':
        return <Badge className="bg-slate-500/10 text-slate-500 border-slate-500/20 px-3 py-1 uppercase tracking-widest text-xs">Draft</Badge>;
      default:
        return <Badge variant="outline">{status}</Badge>;
    }
  };

  const getTypeIcon = (type: string) => {
    switch (type) {
      case 'CASHBACK': return <BadgePercent className="h-4 w-4 text-emerald-500" />;
      case 'SIGNUP_BONUS': return <Gift className="h-4 w-4 text-orange-500" />;
      case 'REFERRAL': return <Users className="h-4 w-4 text-blue-500" />;
      default: return <TagIcon className="h-4 w-4 text-slate-500" />;
    }
  };

  return (
    <div className="space-y-12">
      <StaggerContainer>
        {/* Header Stats */}
        <StaggerItem>
          <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
            {[
              { label: 'Total Budget', value: '—', color: 'bg-emerald-500', icon: Gift },
              { label: 'Total Rewards Sent', value: '—', color: 'bg-blue-500', icon: CheckCircle2 },
              { label: 'Active Campaigns', value: '—', color: 'bg-indigo-500', icon: Timer },
              { label: 'Conversion Lift', value: '—', color: 'bg-orange-500', icon: BarChart3 },
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
                  placeholder="Search campaigns..." 
                  className="pl-12 bg-muted/30 border-border h-12 rounded-xl text-xs font-bold uppercase tracking-widest"
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                />
              </div>
            </div>
            
            <div className="flex items-center gap-4 w-full lg:w-auto">
              <Button variant="outline" className="h-12 px-6 rounded-xl border-border bg-card text-xs font-bold tracking-widest uppercase gap-2">
                <BarChart3 className="h-4 w-4" />
                Performance Report
              </Button>
              <Button className="h-12 px-6 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white font-bold text-xs tracking-widest uppercase gap-2">
                <Plus className="h-4 w-4" />
                Launch New Campaign
              </Button>
            </div>
          </div>
        </StaggerItem>

        {/* Campaign Table */}
        <StaggerItem>
          <div className="bg-card border border-border rounded-2xl shadow-sm overflow-hidden">
            <Table>
              <TableHeader className="bg-muted/30">
                <TableRow className="border-border">
                  <TableHead className="w-[300px] text-xs font-bold uppercase tracking-widest p-6">Campaign Name</TableHead>
                  <TableHead className="text-xs font-bold uppercase tracking-widest px-6">Type</TableHead>
                  <TableHead className="text-xs font-bold uppercase tracking-widest px-6">Status</TableHead>
                  <TableHead className="text-xs font-bold uppercase tracking-widest px-6">Rewards Sent</TableHead>
                  <TableHead className="text-xs font-bold uppercase tracking-widest px-6">Budget Spent</TableHead>
                  <TableHead className="text-right text-xs font-bold uppercase tracking-widest p-6">Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {isLoading ? (
                  <TableRow><TableCell colSpan={7} className="p-6 text-center"><Skeleton className="h-6 w-full" /></TableCell></TableRow>
                ) : error ? (
                  <TableRow><TableCell colSpan={7} className="p-6 text-center text-destructive">Failed to load campaigns</TableCell></TableRow>
                ) : !campaigns || campaigns.length === 0 ? (
                  <TableRow><TableCell colSpan={7} className="p-6 text-center text-muted-foreground">No campaigns found</TableCell></TableRow>
                ) : (
                  campaigns.filter((cmp: { name: string; id: string }) => cmp.name.toLowerCase().includes(searchTerm.toLowerCase())).map((cmp) => (
                  <TableRow key={cmp.id} className="border-border hover:bg-muted/10 transition-colors">
                    <TableCell className="p-6">
                      <div className="space-y-1">
                        <p className="font-bold text-foreground text-sm leading-tight">{cmp.name}</p>
                        <p className="text-xs text-muted-foreground font-bold tracking-widest uppercase">{cmp.id}</p>
                        <div className="flex items-center gap-2 mt-2">
                          <Calendar className="h-3 w-3 text-muted-foreground" />
                          <p className="text-xs text-muted-foreground font-medium">{new Date(cmp.startDate).toLocaleDateString()} - {new Date(cmp.endDate).toLocaleDateString()}</p>
                        </div>
                      </div>
                    </TableCell>
                    <TableCell className="px-6">
                      <div className="flex items-center gap-2">
                        {getTypeIcon(cmp.type)}
                        <span className="text-xs font-bold text-foreground uppercase tracking-widest">{cmp.type.replace('_', ' ')}</span>
                      </div>
                    </TableCell>
                    <TableCell className="px-6">
                      {getStatusBadge(cmp.status)}
                    </TableCell>
                    <TableCell className="px-6 text-xs font-bold text-foreground">
                      {cmp.currentClaims.toLocaleString()}
                    </TableCell>
                    <TableCell className="px-6">
                      <div className="space-y-2">
                        <div className="flex justify-between items-center text-xs font-bold uppercase tracking-widest">
                          <span className="text-emerald-500">{cmp.value}</span>
                          <span className="text-muted-foreground opacity-40">/ {cmp.maxClaims ?? '∞'}</span>
                        </div>
                        <div className="w-32 bg-muted h-1 rounded-full overflow-hidden">
                          <div className="bg-emerald-500 h-full" style={{ width: `${cmp.maxClaims ? Math.min(100, (cmp.currentClaims / cmp.maxClaims) * 100) : 0}%` }} />
                        </div>
                      </div>
                    </TableCell>
                    <TableCell className="text-right p-6">
                      <div className="flex items-center justify-end gap-2">
                        <Button variant="ghost" size="icon" className="h-9 w-9 rounded-lg hover:bg-muted/50">
                          <Edit className="h-4 w-4" />
                        </Button>
                        <Button variant="ghost" size="icon" className="h-9 w-9 rounded-lg hover:bg-muted/50">
                          <Copy className="h-4 w-4" />
                        </Button>
                        <DropdownMenu>
                          <DropdownMenuTrigger asChild>
                            <Button variant="ghost" size="icon" className="h-9 w-9 rounded-lg hover:bg-muted/50">
                              <MoreHorizontal className="h-4 w-4" />
                            </Button>
                          </DropdownMenuTrigger>
                          <DropdownMenuContent align="end" className="bg-card border border-border rounded-xl shadow-xl w-48">
                            <DropdownMenuItem className="gap-2 text-xs font-bold uppercase tracking-widest p-3">
                              <BarChart3 className="h-4 w-4 text-emerald-500" />
                              View Analytics
                            </DropdownMenuItem>
                            <DropdownMenuItem className="gap-2 text-xs font-bold uppercase tracking-widest p-3 text-rose-500">
                              <CheckCircle2 className="h-4 w-4" />
                              End Campaign
                            </DropdownMenuItem>
                          </DropdownMenuContent>
                        </DropdownMenu>
                      </div>
                    </TableCell>
                  </TableRow>
                ))
                )}
              </TableBody>
            </Table>
            
            <div className="p-6 border-t border-border flex items-center justify-between">
              <p className="text-xs font-bold text-muted-foreground uppercase tracking-widest">
                Showing <span className="text-foreground">4</span> campaigns
              </p>
              <div className="flex items-center gap-2">
                <Button variant="outline" size="icon" className="h-10 w-10 rounded-xl border-border hover:bg-muted/50">
                  <ChevronLeft className="h-4 w-4" />
                </Button>
                <div className="h-10 px-4 flex items-center justify-center rounded-xl bg-emerald-500 text-white font-bold text-xs">
                  1
                </div>
                <Button variant="outline" size="icon" className="h-10 w-10 rounded-xl border-border hover:bg-muted/50">
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
