'use client';

import React, { useState } from 'react';
import Image from 'next/image';
import { 
  Plus, 
  Search, 
  Filter, 
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
} from 'lucide-react';
import clsx from 'clsx';
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
import { type Content, type ContentType } from '@/services/CMSService';

// BUG-FE-098: Removed MOCK_CONTENT — should be fetched from CMS service API
const MOCK_CONTENT: Content[] = [];

export default function CMSPage() {
  const [searchTerm, setSearchTerm] = useState('');
  const [activeTab, setActiveTab] = useState<string>('ALL');

  const filteredContent = MOCK_CONTENT.filter(content => {
    const matchesSearch = content.title.toLowerCase().includes(searchTerm.toLowerCase()) || 
                          content.description.toLowerCase().includes(searchTerm.toLowerCase());
    const matchesTab = activeTab === 'ALL' || content.contentType === activeTab;
    return matchesSearch && matchesTab;
  });

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'ACTIVE':
        return <Badge className="bg-emerald-500/10 text-emerald-500 border-emerald-500/20 px-3 py-1 uppercase tracking-widest text-xs">Active</Badge>;
      case 'SCHEDULED':
        return <Badge className="bg-amber-500/10 text-amber-500 border-amber-500/20 px-3 py-1 uppercase tracking-widest text-xs">Scheduled</Badge>;
      case 'DRAFT':
        return <Badge className="bg-slate-500/10 text-slate-500 border-slate-500/20 px-3 py-1 uppercase tracking-widest text-xs">Draft</Badge>;
      case 'ARCHIVED':
        return <Badge className="bg-rose-500/10 text-rose-500 border-rose-500/20 px-3 py-1 uppercase tracking-widest text-xs">Archived</Badge>;
      default:
        return <Badge variant="outline">{status}</Badge>;
    }
  };

  const getContentTypeIcon = (type: string) => {
    switch (type) {
      case 'BANNER':
        return <ImageIcon className="h-4 w-4 text-blue-500" />;
      case 'PROMO':
        return <Gift className="h-4 w-4 text-emerald-500" />;
      case 'ALERT':
        return <AlertCircle className="h-4 w-4 text-rose-500" />;
      case 'POPUP':
        return <ExternalLink className="h-4 w-4 text-purple-500" />;
      default:
        return <FileText className="h-4 w-4 text-slate-500" />;
    }
  };

  return (
    <div className="space-y-12">
      <StaggerContainer>
        {/* Header Stats */}
        <StaggerItem>
          <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
            {[
              { label: 'Total Content', value: '—', color: 'bg-blue-500', icon: FileText },
              { label: 'Active Now', value: '—', color: 'bg-emerald-500', icon: CheckCircle2 },
              { label: 'Scheduled', value: '—', color: 'bg-amber-500', icon: Clock },
              { label: 'Pending Review', value: '—', color: 'bg-rose-500', icon: AlertCircle },
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
            <div className="flex flex-wrap items-center gap-3">
              {['ALL', 'BANNER', 'PROMO', 'ALERT', 'POPUP'].map((tab) => (
                <button
                  key={tab}
                  onClick={() => setActiveTab(tab)}
                  className={clsx(
                    "px-6 py-2.5 rounded-xl text-xs font-bold tracking-widest uppercase transition-all border",
                    activeTab === tab 
                      ? "bg-emerald-500 text-white border-emerald-500 shadow-lg shadow-emerald-500/20" 
                      : "bg-muted/30 text-muted-foreground border-border hover:bg-muted/50 hover:text-foreground"
                  )}
                >
                  {tab}
                </button>
              ))}
            </div>
            
            <div className="flex items-center gap-4 w-full lg:w-auto">
              <div className="relative flex-1 lg:w-80">
                <Search className="absolute left-4 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                <Input 
                  placeholder="Search content..." 
                  className="pl-12 bg-muted/30 border-border h-12 rounded-xl text-xs font-bold uppercase tracking-widest"
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                />
              </div>
              <Button className="h-12 px-6 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white font-bold text-xs tracking-widest uppercase gap-2">
                <Plus className="h-4 w-4" />
                New Content
              </Button>
            </div>
          </div>
        </StaggerItem>

        {/* Content Table */}
        <StaggerItem>
          <div className="bg-card border border-border rounded-2xl shadow-sm overflow-hidden">
            <Table>
              <TableHeader className="bg-muted/30">
                <TableRow className="border-border">
                  <TableHead className="w-[300px] text-xs font-bold uppercase tracking-widest p-6">Content</TableHead>
                  <TableHead className="text-xs font-bold uppercase tracking-widest px-6">Type</TableHead>
                  <TableHead className="text-xs font-bold uppercase tracking-widest px-6">Status</TableHead>
                  <TableHead className="text-xs font-bold uppercase tracking-widest px-6">Schedule</TableHead>
                  <TableHead className="text-xs font-bold uppercase tracking-widest px-6">Priority</TableHead>
                  <TableHead className="text-right text-xs font-bold uppercase tracking-widest p-6">Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {filteredContent.map((item) => (
                  <TableRow key={item.id} className="border-border hover:bg-muted/10 transition-colors">
                    <TableCell className="p-6">
                      <div className="flex items-start gap-4">
                        <div className="h-16 w-16 rounded-lg bg-muted flex-shrink-0 overflow-hidden border border-border relative">
                          {item.imageUrl ? (
                            <Image src={item.imageUrl} alt={item.title} fill sizes="64px" className="object-cover" />
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
                    </TableCell>
                    <TableCell className="px-6">
                      <div className="flex items-center gap-2">
                        {getContentTypeIcon(item.contentType)}
                        <span className="text-xs font-bold text-foreground uppercase tracking-widest">{item.contentType}</span>
                      </div>
                    </TableCell>
                    <TableCell className="px-6">
                      {getStatusBadge(item.status)}
                    </TableCell>
                    <TableCell className="px-6">
                      <div className="space-y-1 text-xs">
                        <p className="font-medium text-foreground">S: {new Date(item.startDate).toLocaleDateString()}</p>
                        <p className="text-muted-foreground font-medium">E: {new Date(item.endDate).toLocaleDateString()}</p>
                      </div>
                    </TableCell>
                    <TableCell className="px-6">
                      <div className="flex items-center gap-2">
                        <div className="w-12 bg-muted h-1.5 rounded-full overflow-hidden">
                          <div className="bg-emerald-500 h-full rounded-full" style={{ width: `${item.priority * 10}%` }} />
                        </div>
                        <span className="text-xs font-bold text-foreground">{item.priority}</span>
                      </div>
                    </TableCell>
                    <TableCell className="text-right p-6">
                      <div className="flex items-center justify-end gap-2">
                        <Button variant="ghost" size="icon" className="h-9 w-9 rounded-lg hover:bg-muted/50">
                          <Eye className="h-4 w-4" />
                        </Button>
                        <Button variant="ghost" size="icon" className="h-9 w-9 rounded-lg hover:bg-muted/50">
                          <Edit className="h-4 w-4" />
                        </Button>
                        <DropdownMenu>
                          <DropdownMenuTrigger asChild>
                            <Button variant="ghost" size="icon" className="h-9 w-9 rounded-lg hover:bg-muted/50">
                              <MoreHorizontal className="h-4 w-4" />
                            </Button>
                          </DropdownMenuTrigger>
                          <DropdownMenuContent align="end" className="bg-card border border-border rounded-xl shadow-xl w-48">
                            <DropdownMenuItem className="gap-2 text-xs font-bold uppercase tracking-widest p-3">
                              <CheckCircle2 className="h-4 w-4 text-emerald-500" />
                              Activate
                            </DropdownMenuItem>
                            <DropdownMenuItem className="gap-2 text-xs font-bold uppercase tracking-widest p-3">
                              <Clock className="h-4 w-4 text-amber-500" />
                              Pause
                            </DropdownMenuItem>
                            <DropdownMenuItem className="gap-2 text-xs font-bold uppercase tracking-widest p-3 text-rose-500">
                              <Trash2 className="h-4 w-4" />
                              Archive
                            </DropdownMenuItem>
                          </DropdownMenuContent>
                        </DropdownMenu>
                      </div>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
            
            {/* Pagination */}
            <div className="p-6 border-t border-border flex items-center justify-between">
              <p className="text-xs font-bold text-muted-foreground uppercase tracking-widest">
                Showing <span className="text-foreground">{filteredContent.length}</span> results
              </p>
              <div className="flex items-center gap-2">
                <Button variant="outline" size="icon" className="h-10 w-10 rounded-xl border-border hover:bg-muted/50">
                  <ChevronLeft className="h-4 w-4" />
                </Button>
                <div className="h-10 px-4 flex items-center justify-center rounded-xl bg-emerald-500 text-white font-bold text-xs">
                  1
                </div>
                <Button variant="outline" size="icon" className="h-10 w-10 rounded-xl border-border hover:bg-muted/50">
                  2
                </Button>
                <Button variant="outline" size="icon" className="h-10 w-10 rounded-xl border-border hover:bg-muted/50">
                  3
                </Button>
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
