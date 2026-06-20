'use client';

import React, { useState } from 'react';
import { 
  Plus, 
  Search, 
  FlaskConical, 
  Play, 
  Pause, 
  CheckCircle2, 
  MoreHorizontal, 
  Settings, 
  Users, 
  Target, 
  BarChart2, 
  RefreshCw,
  ChevronRight,
  ChevronLeft
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
import { Progress } from "@/components/ui/progress";
import { type Experiment, ExperimentStatus, AllocationStrategy } from '@/services/ABTestingService'; // eslint-disable-line @typescript-eslint/no-unused-vars
import clsx from 'clsx';

// BUG-FE-096: Removed MOCK_EXPERIMENTS — should be fetched from AB Testing service API
const MOCK_EXPERIMENTS: Experiment[] = [];

export default function ABTestingPage() {
  const [searchTerm, setSearchTerm] = useState('');

  const filteredExperiments = MOCK_EXPERIMENTS.filter(exp => 
    exp.name.toLowerCase().includes(searchTerm.toLowerCase()) || 
    exp.key.toLowerCase().includes(searchTerm.toLowerCase())
  );

  const getStatusBadge = (status: ExperimentStatus) => {
    switch (status) {
      case ExperimentStatus.RUNNING:
        return <Badge className="bg-emerald-500/10 text-emerald-500 border-emerald-500/20 px-3 py-1 uppercase tracking-widest text-xs gap-1.5 flex items-center justify-center w-fit"><span className="h-1.5 w-1.5 bg-emerald-500 rounded-full animate-pulse" /> Running</Badge>;
      case ExperimentStatus.COMPLETED:
        return <Badge className="bg-slate-500/10 text-slate-500 border-slate-500/20 px-3 py-1 uppercase tracking-widest text-xs">Completed</Badge>;
      case ExperimentStatus.PAUSED:
        return <Badge className="bg-amber-500/10 text-amber-500 border-amber-500/20 px-3 py-1 uppercase tracking-widest text-xs">Paused</Badge>;
      case ExperimentStatus.DRAFT:
        return <Badge className="bg-blue-500/10 text-blue-500 border-blue-500/20 px-3 py-1 uppercase tracking-widest text-xs">Draft</Badge>;
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
              { label: 'Active Experiments', value: '—', color: 'bg-emerald-500', icon: Play },
              { label: 'Total Variations', value: '—', color: 'bg-blue-500', icon: FlaskConical },
              { label: 'Users Exposed', value: '—', color: 'bg-indigo-500', icon: Users },
              { label: 'Winning Variations', value: '—', color: 'bg-amber-500', icon: CheckCircle2 },
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
              {['ALL', 'ACTIVE', 'COMPLETED', 'DRAFT'].map((tab) => (
                <button
                  key={tab}
                  className={clsx(
                    "px-6 py-2.5 rounded-xl text-xs font-bold tracking-widest uppercase transition-all border",
                    tab === 'ALL' 
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
                  placeholder="Search experiments..." 
                  className="pl-12 bg-muted/30 border-border h-12 rounded-xl text-xs font-bold uppercase tracking-widest"
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                />
              </div>
              <Button className="h-12 px-6 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white font-bold text-xs tracking-widest uppercase gap-2">
                <Plus className="h-4 w-4" />
                Create Experiment
              </Button>
            </div>
          </div>
        </StaggerItem>

        {/* Experiment Table */}
        <StaggerItem>
          <div className="bg-card border border-border rounded-2xl shadow-sm overflow-hidden">
            <Table>
              <TableHeader className="bg-muted/30">
                <TableRow className="border-border">
                  <TableHead className="w-[300px] text-xs font-bold uppercase tracking-widest p-6">Experiment</TableHead>
                  <TableHead className="text-xs font-bold uppercase tracking-widest px-6">Status</TableHead>
                  <TableHead className="text-xs font-bold uppercase tracking-widest px-6">Traffic</TableHead>
                  <TableHead className="text-xs font-bold uppercase tracking-widest px-6">Audience</TableHead>
                  <TableHead className="text-xs font-bold uppercase tracking-widest px-6">Start Date</TableHead>
                  <TableHead className="text-right text-xs font-bold uppercase tracking-widest p-6">Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {filteredExperiments.map((exp) => (
                  <TableRow key={exp.id} className="border-border hover:bg-muted/10 transition-colors">
                    <TableCell className="p-6">
                      <div className="space-y-1">
                        <p className="font-bold text-foreground text-sm uppercase tracking-tight">{exp.name}</p>
                        <p className="text-xs font-mono font-bold text-emerald-500 tracking-wider">KEY: {exp.key}</p>
                        <p className="text-xs text-muted-foreground line-clamp-1 max-w-[250px] mt-2">{exp.description}</p>
                      </div>
                    </TableCell>
                    <TableCell className="px-6">
                      {getStatusBadge(exp.status)}
                    </TableCell>
                    <TableCell className="px-6">
                      <div className="space-y-2 max-w-[100px]">
                        <Progress value={exp.trafficPercentage} className="h-1.5" />
                        <span className="text-xs font-bold text-foreground">{exp.trafficPercentage}%</span>
                      </div>
                    </TableCell>
                    <TableCell className="px-6">
                      <div className="flex items-center gap-2">
                        <Target className="h-3.5 w-3.5 text-muted-foreground" />
                        <span className="text-xs font-bold text-foreground uppercase truncate max-w-[120px]">
                          {Object.entries(exp.targetAudience).map(([k, v]) => `${k}:${v}`).join(', ')}
                        </span>
                      </div>
                    </TableCell>
                    <TableCell className="px-6">
                      <p className="text-xs font-medium text-foreground">{new Date(exp.startDate).toLocaleDateString()}</p>
                      <p className="text-xs text-muted-foreground font-bold uppercase tracking-widester mt-1">Launched</p>
                    </TableCell>
                    <TableCell className="text-right p-6">
                      <div className="flex items-center justify-end gap-2">
                        <Button variant="ghost" size="icon" className="h-9 w-9 rounded-lg hover:bg-muted/50">
                          <BarChart2 className="h-4 w-4" />
                        </Button>
                        <Button variant="ghost" size="icon" className="h-9 w-9 rounded-lg hover:bg-muted/50">
                          <Settings className="h-4 w-4" />
                        </Button>
                        <DropdownMenu>
                          <DropdownMenuTrigger asChild>
                            <Button variant="ghost" size="icon" className="h-9 w-9 rounded-lg hover:bg-muted/50">
                              <MoreHorizontal className="h-4 w-4" />
                            </Button>
                          </DropdownMenuTrigger>
                          <DropdownMenuContent align="end" className="bg-card border border-border rounded-xl shadow-xl w-48">
                            <DropdownMenuItem className="gap-2 text-xs font-bold uppercase tracking-widest p-3">
                              <Play className="h-4 w-4 text-emerald-500" />
                              Start
                            </DropdownMenuItem>
                            <DropdownMenuItem className="gap-2 text-xs font-bold uppercase tracking-widest p-3">
                              <Pause className="h-4 w-4 text-amber-500" />
                              Pause
                            </DropdownMenuItem>
                            <DropdownMenuItem className="gap-2 text-xs font-bold uppercase tracking-widest p-3">
                              <RefreshCw className="h-4 w-4 text-blue-500" />
                              Duplicate
                            </DropdownMenuItem>
                            <DropdownMenuItem className="gap-2 text-xs font-bold uppercase tracking-widest p-3 text-rose-500">
                              <CheckCircle2 className="h-4 w-4" />
                              Stop & Finalize
                            </DropdownMenuItem>
                          </DropdownMenuContent>
                        </DropdownMenu>
                      </div>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
            
            <div className="p-6 border-t border-border flex items-center justify-between">
              <p className="text-xs font-bold text-muted-foreground uppercase tracking-widest">
                Showing <span className="text-foreground">3</span> experiments
              </p>
              <div className="flex items-center gap-2">
                <Button variant="outline" size="icon" className="h-10 w-10 rounded-xl border-border hover:bg-muted/50 disabled:opacity-30" disabled>
                  <ChevronLeft className="h-4 w-4" />
                </Button>
                <div className="h-10 px-4 flex items-center justify-center rounded-xl bg-emerald-500 text-white font-bold text-xs">
                  1
                </div>
                <Button variant="outline" size="icon" className="h-10 w-10 rounded-xl border-border hover:bg-muted/50 disabled:opacity-30" disabled>
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
