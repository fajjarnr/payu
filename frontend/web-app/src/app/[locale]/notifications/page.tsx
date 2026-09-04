'use client';

import React, { useState } from 'react';
import { 
  Search, 
  Trash2, 
  Clock, 
  MessageSquare, 
  Gift, 
  ShieldAlert, 
  ArrowRight,
  MoreVertical,
  Inbox,
} from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Badge } from '@/components/ui/badge';
import DashboardLayout from '@/components/DashboardLayout';
import { PageTransition, StaggerContainer, StaggerItem } from '@/components/ui/Motion';
import clsx from 'clsx';
import { useNotifications, useMarkNotificationRead } from '@/hooks';
import { useAuthStore } from '@/stores/authStore';
import { useRouter } from '@/lib/navigation';
import { toast } from 'sonner';

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
      case 'PROMO': return <Gift className="h-5 w-5 text-emerald-500" />;
      case 'ALERT': return <ShieldAlert className="h-5 w-5 text-rose-500" />;
      case 'SECURITY': return <Clock className="h-5 w-5 text-amber-500" />;
      default: return <MessageSquare className="h-5 w-5 text-blue-500" />;
    }
  };

  return (
    <DashboardLayout>
      <PageTransition>
        <div className="w-full space-y-6 lg:space-y-8">
          <StaggerContainer>
            {/* Header */}
            <StaggerItem>
              <div className="flex flex-col sm:flex-row justify-between items-start sm:items-end gap-4 mb-6">
                <div>
                  <h2 className="text-3xl font-bold text-foreground tracking-tight">Kotak Masuk</h2>
                  <p className="text-sm text-muted-foreground font-medium mt-1">Kelola notifikasi, promo, dan peringatan keamanan Anda.</p>
                </div>
                <div className="flex items-center gap-3">
                  <Button 
                    onClick={handleMarkAllRead}
                    variant="ghost" 
                    className="text-xs font-bold tracking-widest uppercase hover:text-emerald-500">
                    Tandai Semua Dibaca
                  </Button>
                  <Button 
                    onClick={handleClearAll}
                    variant="ghost" 
                    className="text-xs font-bold tracking-widest uppercase text-rose-500 hover:bg-rose-500/5">
                    Hapus Semua
                  </Button>
                </div>
              </div>
            </StaggerItem>

            {/* Toolbar */}
            <StaggerItem>
              <div className="flex flex-col md:flex-row gap-4 bg-card border border-border p-3 rounded-2xl shadow-sm">
                <div className="relative flex-1">
                  <Search className="absolute left-4 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                  <Input 
                    placeholder="Cari notifikasi..." 
                    className="pl-12 bg-transparent border-none focus-visible:ring-0 h-12 text-sm font-bold uppercase tracking-widest"
                    value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)}
                  />
                </div>
                <div className="flex items-center gap-2 pr-2">
                  {['ALL', 'UNREAD', 'PROMO', 'SECURITY'].map((f) => (
                    <button
                      key={f}
                      onClick={() => setFilter(f)}
                      className={clsx(
                        "px-4 py-2 rounded-xl text-xs font-bold tracking-widest uppercase transition-all",
                        filter === f 
                          ? "bg-emerald-500 text-white shadow-lg shadow-emerald-500/20" 
                          : "text-muted-foreground hover:bg-muted/50 hover:text-foreground"
                      )}
                    >
                      {f === 'ALL' ? 'Semua' : f === 'UNREAD' ? 'Belum Dibaca' : f}
                    </button>
                  ))}
                </div>
              </div>
            </StaggerItem>

            {/* Notifications List */}
            <StaggerItem>
              <div className="space-y-4 mt-8">
                {filteredNotifs.length === 0 ? (
                  <div className="text-center py-8 bg-card border border-border rounded-2xl">
                    <Inbox className="h-16 w-16 text-muted-foreground mx-auto opacity-20 mb-6" />
                    <p className="text-sm font-bold text-muted-foreground uppercase tracking-widest">Tidak ada notifikasi</p>
                  </div>
                ) : (
                  filteredNotifs.map((n) => (
                    <div 
                      key={n.id} 
                      className={clsx(
                        "group bg-card border border-border p-6 rounded-2xl shadow-sm transition-all hover:shadow-md hover:border-emerald-500/30 flex items-start gap-6 relative overflow-hidden",
                        !n.read && "border-l-4 border-l-emerald-500"
                      )}
                    >
                      {!n.read && (
                        <div className="absolute top-0 right-0 p-2">
                          <div className="h-2 w-2 bg-emerald-500 rounded-full" />
                        </div>
                      )}
                      
                      <div className="h-14 w-14 rounded-2xl bg-muted flex items-center justify-center border border-border shrink-0 text-xl shadow-sm group-hover:scale-110 transition-transform">
                        {getIcon(n.type)}
                      </div>

                      <div className="flex-1 space-y-2">
                        <div className="flex justify-between items-start">
                          <Badge variant="outline" className="text-xs font-bold uppercase tracking-widest px-2 py-0 border-emerald-500/20 text-emerald-500">
                            {n.type}
                          </Badge>
                          <span className="text-xs font-bold text-muted-foreground uppercase tracking-widest">
                            {new Date(n.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                          </span>
                        </div>
                        <h3 className={clsx("text-lg font-bold text-foreground", !n.read && "text-emerald-600")}>
                          {n.title}
                        </h3>
                        <p className="text-sm text-muted-foreground leading-relaxed">
                          {n.content}
                        </p>
                        <div className="pt-4 flex items-center justify-between">
                          <button
                            onClick={() => {
                              if (!n.read) {
                                markRead.mutate(n.id);
                              }
                              router.push(`/notifications/${n.id}`);
                            }}
                            className="text-xs font-bold text-emerald-600 uppercase tracking-widest flex items-center gap-2 group/btn"
                          >
                            Lihat Detail
                            <ArrowRight className="h-3 w-3 transition-transform group-hover/btn:translate-x-1" />
                          </button>
                          <div className="flex items-center gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
                            <Button variant="ghost" size="icon" className="h-8 w-8 rounded-lg text-muted-foreground hover:text-rose-500 hover:bg-rose-500/5">
                              <Trash2 className="h-4 w-4" />
                            </Button>
                            <Button variant="ghost" size="icon" className="h-8 w-8 rounded-lg text-muted-foreground">
                              <MoreVertical className="h-4 w-4" />
                            </Button>
                          </div>
                        </div>
                      </div>
                    </div>
                  ))
                )}
              </div>
            </StaggerItem>
          </StaggerContainer>
        </div>
      </PageTransition>
    </DashboardLayout>
  );
}
