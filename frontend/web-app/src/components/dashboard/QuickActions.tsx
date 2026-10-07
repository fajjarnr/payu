import React, { useState } from 'react';
import {
  ArrowRightLeft,
  QrCode,
  Receipt,
  Wallet,
  CreditCard,
  Smartphone,
  MoreHorizontal,
  GripVertical,
  ChevronRight,
} from '@/components/icons';
import { useTranslations } from 'next-intl';
import clsx from 'clsx';
import { cn } from '@/lib/utils';
import { Button, Card } from 'antd';

interface QuickAction {
  id: string;
  label: string;
  icon: React.ElementType;
  href: string;
  color: string;
  bgColor: string;
  description?: string;
  ariaLabel: string;
}

interface QuickActionsProps {
  actions?: QuickAction[];
  maxActions?: number;
  className?: string;
  onReorder?: (actions: QuickAction[]) => void;
}

const defaultActions: QuickAction[] = [
  {
    id: 'transfer',
    label: 'Transfer',
    icon: ArrowRightLeft,
    href: '/transfer',
    color: 'text-primary',
    bgColor: 'bg-success-light',
    description: 'Kirim uang instan',
    ariaLabel: 'Transfer uang ke akun lain',
  },
  {
    id: 'qris',
    label: 'QRIS',
    icon: QrCode,
    href: '/qris',
    color: 'text-primary',
    bgColor: 'bg-chart-2',
    description: 'Scan QR untuk bayar',
    ariaLabel: 'Pembayaran QRIS',
  },
  {
    id: 'bills',
    label: 'Tagihan',
    icon: Receipt,
    href: '/bills',
    color: 'text-primary',
    bgColor: 'bg-chart-3',
    description: 'Bayar tagihan & isi ulang',
    ariaLabel: 'Bayar tagihan dan isi ulang',
  },
  {
    id: 'pockets',
    label: 'Kantong',
    icon: Wallet,
    href: '/pockets',
    color: 'text-primary',
    bgColor: 'bg-chart-green1',
    description: 'Kelola kantong uang',
    ariaLabel: 'Kelola kantong',
  },
  {
    id: 'cards',
    label: 'Kartu',
    icon: CreditCard,
    href: '/cards',
    color: 'text-primary',
    bgColor: 'bg-chart-green2',
    description: 'Kartu virtual',
    ariaLabel: 'Kelola kartu virtual',
  },
  {
    id: 'topup',
    label: 'Isi Ulang',
    icon: Smartphone,
    href: '/bills?category=pulsa',
    color: 'text-primary',
    bgColor: 'bg-chart-green3',
    description: 'Isi pulsa & paket data',
    ariaLabel: 'Isi ulang pulsa',
  },
];

export default function QuickActions({
  actions = defaultActions,
  maxActions = 6,
  className = '',
  onReorder,
}: QuickActionsProps) {
  const t = useTranslations('dashboard');
  const [items, setItems] = useState(actions.slice(0, maxActions));
  const [isEditMode, setIsEditMode] = useState(false);

  const moveItem = (id: string, dir: -1 | 1) => {
    setItems((prev) => {
      const idx = prev.findIndex((item) => item.id === id);
      const next = idx + dir;
      if (idx < 0 || next < 0 || next >= prev.length) return prev;
      const newItems = [...prev];
      [newItems[idx], newItems[next]] = [newItems[next], newItems[idx]];
      onReorder?.(newItems);
      return newItems;
    });
  };

  return (
    <Card
      data-testid="quick-actions-card"
      role="region"
      aria-labelledby="quick-actions-title"
      className={cn("relative overflow-hidden group", className)}
      styles={{ body: { display: 'contents' } }}
    >
      {/* Decorative background */}
      <div className="absolute -bottom-10 -right-10 w-40 h-40 bg-primary/5 rounded-full blur-3xl pointer-events-none" />

      <div className="flex flex-col space-y-1.5 p-6 flex flex-row items-start justify-between space-y-0 pb-6">
        <div>
          <h3 id="quick-actions-title" className="text-2xl font-bold leading-none tracking-tight text-base sm:text-lg font-bold text-foreground tracking-widest uppercase">
            {t('quickActionsTitle')}
          </h3>
          <p className="text-xs text-muted-foreground font-medium uppercase tracking-[0.1em] uppercase tracking-widest text-xs sm:text-xs font-bold opacity-60">
            {t('quickActionsSubtitle')}
          </p>
        </div>

        <Button
          type={isEditMode ? "primary" : "default"}
          size="small"
          data-testid="edit-quick-actions-button"
          onClick={() => setIsEditMode(!isEditMode)}
          aria-label={isEditMode ? 'Selesai mengedit' : 'Edit urutan aksi cepat'}
          aria-pressed={isEditMode}
          className="text-xs sm:text-xs px-4"
        >
          {isEditMode ? 'Selesai' : 'Edit'}
        </Button>
      </div>

      <div className="p-6 pt-0">
        {/* Reorder hint in edit mode */}
        {isEditMode && (
          <div
            className="mb-6 p-4 bg-primary/5 border border-primary/20 rounded-xl"
            role="status"
            aria-live="polite"
          >
            <p className="text-xs text-muted-foreground flex items-center gap-3">
              <GripVertical className="h-5 w-5" aria-hidden="true" />
              {t('quickActionsDragHint')} - Gunakan tombol panah atau tombol naik/turun untuk mengatur ulang
            </p>
          </div>
        )}

        {/* Actions Grid */}
        <div className="grid grid-cols-2 sm:grid-cols-4 lg:grid-cols-4 xl:grid-cols-6 gap-8">
          {items.map((action, index) => (
            <QuickActionItem
              key={action.id}
              action={action}
              isEditMode={isEditMode}
              isFirst={index === 0}
              isLast={index === items.length - 1}
              onMoveUp={() => moveItem(action.id, -1)}
              onMoveDown={() => moveItem(action.id, 1)}
            />
          ))}
        </div>
        {/* More Actions Link */}
        <div className="mt-8 pt-6 border-t border-border">
          <Button
            type="default"
            data-testid="view-all-features-button"
            aria-label="Lihat semua fitur"
            className="w-full text-xs sm:text-sm font-bold text-muted-foreground hover:text-foreground justify-center gap-3 h-12 border-transparent bg-transparent hover:bg-muted hover:border-transparent active:bg-transparent active:border-transparent"
          >
            <MoreHorizontal className="h-5 w-5" aria-hidden="true" />
            Lihat Semua Fitur
            <ChevronRight className="h-5 w-5" aria-hidden="true" />
          </Button>
        </div>
      </div>
    </Card>
  );
}

interface QuickActionItemProps {
  action: QuickAction;
  isEditMode: boolean;
  isFirst: boolean;
  isLast: boolean;
  onMoveUp: () => void;
  onMoveDown: () => void;
}
function QuickActionItem({ action, isEditMode, isFirst, isLast, onMoveUp, onMoveDown }: QuickActionItemProps) {
  const Icon = action.icon;

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (!isEditMode) return;
    if (e.key === 'ArrowUp' || e.key === 'ArrowLeft') {
      e.preventDefault();
      onMoveUp();
    } else if (e.key === 'ArrowDown' || e.key === 'ArrowRight') {
      e.preventDefault();
      onMoveDown();
    }
  };

  return (
    <div className="relative" onKeyDown={handleKeyDown}>
      <a
        href={action.href}
        data-testid={`quick-action-${action.id}`}
        className={clsx(
          'group relative p-6 rounded-2xl border transition-all flex flex-col items-center text-center',
          'focus:outline-none focus:ring-2 focus:ring-primary focus:ring-inset',
          isEditMode
            ? 'border-dashed border-primary/30 bg-muted/30'
            : 'border-border bg-card hover:border-primary/30 hover:shadow-xl hover:bg-primary/5 shadow-sm'
        )}
        aria-label={action.ariaLabel}
      >
        {isEditMode && (
          <div className="absolute top-4 right-4 flex items-center gap-1">
            <GripVertical className="h-5 w-5 text-muted-foreground" aria-hidden="true" />
            <span className="sr-only">Drag untuk mengatur ulang</span>
          </div>
        )}
        <div
          className={clsx(
            'h-16 w-16 rounded-2xl flex items-center justify-center mb-5 transition-transform group-hover:scale-110 shadow-lg',
            action.bgColor
          )}
        >
          <Icon className={clsx('h-8 w-8', action.color)} aria-hidden="true" />
        </div>
        <p className="text-sm font-bold text-foreground mb-1 shadow-sm">{action.label}</p>
        {action.description && (
          <p className="text-xs sm:text-xs text-muted-foreground font-medium line-clamp-1 opacity-80 uppercase tracking-[0.05em]">{action.description}</p>
        )}
      </a>
      {isEditMode && (
        <div className="mt-2 flex justify-center gap-2">
          <Button
            size="small"
            disabled={isFirst}
            onClick={onMoveUp}
            aria-label={`Pindahkan ${action.label} ke atas`}
            className="min-h-[44px] min-w-[44px]"
          >
            Naik
          </Button>
          <Button
            size="small"
            disabled={isLast}
            onClick={onMoveDown}
            aria-label={`Pindahkan ${action.label} ke bawah`}
            className="min-h-[44px] min-w-[44px]"
          >
            Turun
          </Button>
        </div>
      )}
    </div>
  );
}
